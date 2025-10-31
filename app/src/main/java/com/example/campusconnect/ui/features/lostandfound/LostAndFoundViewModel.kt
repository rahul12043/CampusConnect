package com.example.campusconnect.ui.features.lostandfound

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.campusconnect.data.LostFoundItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class LostFoundState(
    val items: List<LostFoundItem> = emptyList(),
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false
)

class LostAndFoundViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val _state = MutableStateFlow(LostFoundState())
    val state = _state.asStateFlow()

    init {
        firestore.collection("lostAndFoundItems")
            .whereEqualTo("status", "ACTIVE")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    _state.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }
                val items = snapshots?.toObjects(LostFoundItem::class.java) ?: emptyList()
                _state.update { it.copy(items = items, isLoading = false) }
            }
    }

    fun reportItem(
        name: String,
        description: String?,
        location: String,
        imageUri: Uri?,
        onComplete: (Boolean) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onComplete(false)
            return
        }

        _state.update { it.copy(isSubmitting = true) }

        if (imageUri == null) {
            viewModelScope.launch {
                saveItemToFirestore(userId, name, description, location, null, onComplete)
            }
            return
        }

        MediaManager.get().upload(imageUri)
            .unsigned("YOUR_UNSIGNED_UPLOAD_PRESET")
            .option("resource_type", "auto")
            .option("folder", "lost_and_found")
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val downloadUrl = resultData["url"] as? String
                    viewModelScope.launch {
                        saveItemToFirestore(userId, name, description, location, downloadUrl, onComplete)
                    }
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    android.util.Log.e("L&F_ViewModel", "Cloudinary Error: ${error.description}")
                    _state.update { it.copy(isSubmitting = false) }
                    onComplete(false)
                }
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    private suspend fun saveItemToFirestore(
        userId: String,
        name: String,
        description: String?,
        location: String,
        imageUrl: String?,
        onComplete: (Boolean) -> Unit
    ) {
        try {
            val document = firestore.collection("lostAndFoundItems").document()
            // --- THIS IS THE FIX ---
            // We now correctly pass Timestamp.now() to the data class constructor.
            val newItem = LostFoundItem(
                id = document.id,
                name = name,
                description = description,
                location = location,
                status = "PENDING_REVIEW",
                reporterId = userId,
                imageUrl = imageUrl,
                timestamp = Timestamp.now() // Ensure timestamp is never null
            )
            document.set(newItem).await()
            onComplete(true)
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(false)
        } finally {
            _state.update { it.copy(isSubmitting = false) }
        }
    }
}
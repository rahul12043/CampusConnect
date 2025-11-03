package com.example.campusconnect.ui.features.lostandfound

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.LostFoundItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * NEW: This State class is for the reporting screen.
 * It provides the `isSubmitting` flag for your UI.
 */
data class ReportItemState(
    val isSubmitting: Boolean = false
)

/**
 * NEW: This ViewModel is for the ReportItemScreen.
 * Its only job is to handle the logic for creating and uploading a new item.
 */
class ReportItemViewModel : ViewModel() {

    private val firestore = Firebase.firestore
    private val storage = Firebase.storage
    private val auth = Firebase.auth

    private val _state = MutableStateFlow(ReportItemState())
    val state = _state.asStateFlow()

    fun reportItem(
        type: String,
        title: String,
        description: String?,
        location: String,
        imageUri: Uri?,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }

            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e("ReportItemVM", "User is not logged in.")
                _state.update { it.copy(isSubmitting = false) }
                onComplete(false)
                return@launch
            }

            try {
                var imageUrl: String? = null
                if (imageUri != null) {
                    val storageRef = storage.reference.child("lost_found_images/${UUID.randomUUID()}.jpg")
                    storageRef.putFile(imageUri).await()
                    imageUrl = storageRef.downloadUrl.await().toString()
                }

                val collectionRef = firestore.collection("lostAndFoundItems")
                val itemId = collectionRef.document().id

                val newItem = LostFoundItem(
                    id = itemId,
                    type = type,
                    title = title,
                    description = description,
                    location = location,
                    imageUrl = imageUrl,
                    status = "open",
                    postedBy = userId,
                    timestamp = Timestamp.now()
                )

                collectionRef.document(itemId).set(newItem).await()
                _state.update { it.copy(isSubmitting = false) }
                onComplete(true)

            } catch (e: Exception) {
                Log.e("ReportItemVM", "Error reporting item", e)
                _state.update { it.copy(isSubmitting = false) }
                onComplete(false)
            }
        }
    }
}
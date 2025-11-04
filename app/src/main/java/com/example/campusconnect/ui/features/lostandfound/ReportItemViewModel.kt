package com.example.campusconnect.ui.features.lostandfound

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.campusconnect.data.LostFoundItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

data class ReportItemState(
    val isSubmitting: Boolean = false
)

class ReportItemViewModel : ViewModel() {

    private val firestore = Firebase.firestore
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
                // --- If no image, just create document directly ---
                if (imageUri == null) {
                    saveLostFoundItem(null, userId, type, title, description, location, onComplete)
                    return@launch
                }

                // --- If image is provided, upload to Cloudinary ---
                val publicId = "lost_found_${UUID.randomUUID()}"
                val options = mapOf(
                    "resource_type" to "image",
                    "public_id" to publicId,
                    "upload_preset" to "campus_connect_unsigned"
                )

                MediaManager.get().upload(imageUri)
                    .options(options)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String?) {
                            Log.d("ReportItemVM", "Cloudinary upload started...")
                        }

                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                            val progress = (bytes * 100) / totalBytes
                            Log.d("ReportItemVM", "Upload progress: $progress%")
                        }

                        override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                            val imageUrl = resultData["secure_url"] as String
                            viewModelScope.launch {
                                saveLostFoundItem(imageUrl, userId, type, title, description, location, onComplete)
                            }
                        }

                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            Log.e("ReportItemVM", "Cloudinary upload failed: ${error?.description}")
                            _state.update { it.copy(isSubmitting = false) }
                            onComplete(false)
                        }

                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                            Log.w("ReportItemVM", "Cloudinary upload rescheduled: ${error?.description}")
                        }
                    })
                    .dispatch()

            } catch (e: Exception) {
                Log.e("ReportItemVM", "Error reporting item", e)
                _state.update { it.copy(isSubmitting = false) }
                onComplete(false)
            }
        }
    }

    // --- Helper function to save to Firestore ---
    private suspend fun saveLostFoundItem(
        imageUrl: String?,
        userId: String,
        type: String,
        title: String,
        description: String?,
        location: String,
        onComplete: (Boolean) -> Unit
    ) {
        try {
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
            Log.d("ReportItemVM", "Item saved successfully in Firestore.")

            _state.update { it.copy(isSubmitting = false) }
            onComplete(true)

        } catch (e: Exception) {
            Log.e("ReportItemVM", "Error saving to Firestore", e)
            _state.update { it.copy(isSubmitting = false) }
            onComplete(false)
        }
    }
}

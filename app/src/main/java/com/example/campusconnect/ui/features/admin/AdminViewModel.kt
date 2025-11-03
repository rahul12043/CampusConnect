package com.example.campusconnect.ui.features.admin

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.campusconnect.data.Announcement
import com.example.campusconnect.data.FacultyMember
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class AdminState(
    val facultyList: List<FacultyMember> = emptyList(),
    val isLoading: Boolean = false,
    val announcements: List<Announcement> = emptyList(),
    val isUploading: Boolean = false
)

class AdminViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val _state = MutableStateFlow(AdminState())
    val state = _state.asStateFlow()

    init {
        fetchFacultyMembers()
        fetchAnnouncements()
    }

    // --- Announcement Functions (Unchanged) ---

    fun updateAnnouncement(
        announcementId: String,
        title: String,
        message: String,
        isUrgent: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                firestore.collection("announcements").document(announcementId)
                    .update(mapOf(
                        "title" to title,
                        "message" to message,
                        "isUrgent" to isUrgent,
                        "timestamp" to FieldValue.serverTimestamp()
                    )).await()
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    private fun fetchAnnouncements() {
        firestore.collection("announcements")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, _ ->
                val list = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(Announcement::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                _state.update { it.copy(announcements = list) }
            }
    }

    fun addAnnouncement(title: String, message: String, isUrgent: Boolean, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val document = firestore.collection("announcements").document()
                val newAnnouncement = Announcement(
                    id = document.id,
                    title = title,
                    message = message,
                    isUrgent = isUrgent,
                    timestamp = Timestamp.now()
                )
                document.set(newAnnouncement).await()
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun deleteAnnouncement(announcementId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("announcements").document(announcementId).delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Faculty Functions ---

    private fun fetchFacultyMembers() {
        _state.update { it.copy(isLoading = true) }
        firestore.collection("facultyMembers").addSnapshotListener { snapshots, error ->
            if (error != null) {
                _state.update { it.copy(isLoading = false) }
                return@addSnapshotListener
            }
            val list = snapshots?.documents?.mapNotNull { doc ->
                doc.toObject(FacultyMember::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            _state.update { it.copy(facultyList = list, isLoading = false) }
        }
    }

    /**
     * MODIFIED: Now includes the 'email' parameter.
     */
    fun addFacultyMember(
        name: String,
        department: String,
        officeLocation: String,
        email: String, // Added email
        timetable: Map<String, String>,
        imageUri: Uri,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true) }
            try {
                val imageUrl = uploadImageToCloudinary(imageUri)
                if (imageUrl == null) {
                    Log.e("AdminViewModel", "Cloudinary upload failed.")
                    onComplete(false)
                    _state.update { it.copy(isUploading = false) }
                    return@launch
                }

                val document = firestore.collection("facultyMembers").document()
                val newMember = FacultyMember(
                    id = document.id,
                    name = name,
                    department = department,
                    officeLocation = officeLocation,
                    email = email, // Added email
                    imageUrl = imageUrl,
                    timetable = timetable
                )
                document.set(newMember).await()
                onComplete(true)
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error adding faculty member", e)
                onComplete(false)
            } finally {
                _state.update { it.copy(isUploading = false) }
            }
        }
    }

    // Helper function to upload images to Cloudinary
    private suspend fun uploadImageToCloudinary(imageUri: Uri): String? = suspendCoroutine { continuation ->
        MediaManager.get().upload(imageUri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val url = resultData?.get("secure_url") as? String
                    continuation.resume(url)
                }
                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e("AdminViewModel", "Cloudinary Error: ${error?.description}")
                    continuation.resume(null)
                }
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }

    /**
     * MODIFIED: This function now accepts a complete FacultyMember object,
     * which is cleaner and supports updating all fields at once.
     */
    fun updateFacultyMember(facultyMember: FacultyMember, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("facultyMembers").document(facultyMember.id)
                    .set(facultyMember) // .set() overwrites the document with the new data
                    .await()
                onComplete(true)
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error updating faculty member", e)
                onComplete(false)
            }
        }
    }

    fun deleteFacultyMember(facultyId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("facultyMembers").document(facultyId).delete().await()
                onComplete(true)
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error deleting faculty member", e)
                onComplete(false)
            }
        }
    }
}
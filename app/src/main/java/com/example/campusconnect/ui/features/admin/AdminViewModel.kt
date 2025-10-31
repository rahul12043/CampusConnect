package com.example.campusconnect.ui.features.admin

import android.net.Uri
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
    // Firebase Storage is no longer needed for this view model
    // private val storage = Firebase.storage
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

    // --- Faculty Functions (Updated for Cloudinary) ---

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

    fun addFacultyMember(
        name: String,
        department: String,
        officeLocation: String,
        timetable: Map<String, String>,
        imageUri: Uri,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true) }
            try {
                // 1. Upload the image to Cloudinary and wait for the URL
                val imageUrl = uploadImageToCloudinary(imageUri)

                if (imageUrl == null) {
                    // Handle image upload failure
                    println("Cloudinary upload failed.")
                    onComplete(false)
                    _state.update { it.copy(isUploading = false) }
                    return@launch
                }

                // 2. Save faculty member to Firestore with the Cloudinary URL
                val document = firestore.collection("facultyMembers").document()
                val newMember = FacultyMember(
                    id = document.id,
                    name = name,
                    department = department,
                    officeLocation = officeLocation,
                    imageUrl = imageUrl, // Use the URL from Cloudinary
                    timetable = timetable
                )
                document.set(newMember).await()
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                _state.update { it.copy(isUploading = false) }
            }
        }
    }

    // New helper function to wrap Cloudinary's callback in a coroutine
    private suspend fun uploadImageToCloudinary(imageUri: Uri): String? = suspendCoroutine { continuation ->
        MediaManager.get().upload(imageUri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val url = resultData?.get("secure_url") as? String
                    continuation.resume(url) // Return the secure URL
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    println("Cloudinary Error: ${error?.description}")
                    continuation.resume(null) // Return null on failure
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }

    fun updateFacultyMember(
        facultyId: String,
        name: String,
        department: String,
        office: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                firestore.collection("facultyMembers").document(facultyId)
                    .update(mapOf(
                        "name" to name,
                        "department" to department,
                        "officeLocation" to office
                    )).await()
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
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
                e.printStackTrace()
                onComplete(false)
            }
        }
    }
}
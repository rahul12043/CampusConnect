package com.example.campusconnect.ui.features.note_sharing

import android.content.ContentResolver
import android.icu.text.IDNA
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.campusconnect.data.User
import com.example.campusconnect.data.NotePost
import com.example.campusconnect.data.Subject
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

data class NoteSharingState(
    val allPosts: List<NotePost> = emptyList(),
    val displayedPosts: List<NotePost> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val selectedSubject: String? = null,
    val isLoading: Boolean = true,
    val isUploading: Boolean = false
)

class NoteSharingViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val _state = MutableStateFlow(NoteSharingState())
    val state = _state.asStateFlow()

    init {
        fetchSubjects()

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            // This listener fetches ALL notes ONCE and keeps the list updated in the background.
            firestore.collection("notePosts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(NotePost::class.java)
                }
                .catch { exception ->
                    Log.e("NoteSharingVM", "Error listening for notes", exception)
                    emit(emptyList())
                }
                .collect { allPosts ->
                    _state.update { it.copy(allPosts = allPosts) }
                    applyFilter()
                }
        }
    }

    private fun applyFilter() {
        val currentState = _state.value
        val filteredPosts = if (currentState.selectedSubject == null) {
            currentState.allPosts
        } else {
            currentState.allPosts.filter { it.subject == currentState.selectedSubject }
        }
        _state.update {
            it.copy(
                displayedPosts = filteredPosts,
                isLoading = false
            )
        }
    }

    fun filterBySubject(subject: String?) {
        _state.update { it.copy(selectedSubject = subject) }
        applyFilter()
    }

    private fun fetchSubjects() {
        firestore.collection("subjects").orderBy("name").addSnapshotListener { snapshots, _ ->
            val subjects = snapshots?.toObjects(Subject::class.java) ?: emptyList()
            _state.update { it.copy(subjects = subjects) }
        }
    }

    private suspend fun createSubjectIfNotExist(subjectName: String) {
        if (subjectName.isBlank()) return
        val formattedSubjectName = subjectName.trim().uppercase(Locale.getDefault())
        val subjectRef = firestore.collection("subjects").document(formattedSubjectName)
        val document = subjectRef.get().await()
        if (!document.exists()) {
            subjectRef.set(Subject(name = formattedSubjectName)).await()
        }
    }

    // --- THIS IS THE UPDATED FUNCTION ---
    fun uploadNote(
        user: User,
        title: String,
        description: String?,
        subject: String,
        fileUri: Uri,
        fileType: String,
        contentResolver: ContentResolver,
        onComplete: (Boolean) -> Unit
    ) {
        _state.update { it.copy(isUploading = true) }

        // 1. Determine the resource type
        val resourceType = if (fileType.startsWith("image")) "image" else "raw"

        // 2. Get the original file extension (e.g., ".pdf", ".jpg")
        val fileExtension = getFileExtension(fileUri, contentResolver)

        // 3. Create a unique public ID that INCLUDES the file extension
        val publicId = "${UUID.randomUUID()}$fileExtension"

        // 4. --- THIS IS THE CRITICAL FIX ---
        // We create a Map of all our upload options. This is the most reliable method.
        val options = mapOf(
            "resource_type" to resourceType,
            "public_id" to publicId,
            "upload_preset" to "campus_connect_unsigned" // The preset must be included for unsigned uploads
        )

        // 5. Build and dispatch the upload request using the options map
        MediaManager.get().upload(fileUri)
            .options(options) // Use the .options() method with the map
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val downloadUrl = resultData["secure_url"] as String
                    viewModelScope.launch {
                        try {
                            // ... This Firestore saving logic is correct and remains the same ...
                            val formattedSubject = subject.trim().uppercase(Locale.getDefault())
                            createSubjectIfNotExist(formattedSubject)
                            val document = firestore.collection("notePosts").document()
                            val newPost = NotePost(
                                id = document.id, title = title, description = description, fileUrl = downloadUrl,
                                fileType = fileType, authorId = user.uid, authorSapId = user.specializedId,
                                authorName = user.fullName, subject = formattedSubject, timestamp = Timestamp.now()
                            )
                            document.set(newPost).await()
                            onComplete(true)
                        } catch (e: Exception) {
                            Log.e("NoteSharingVM", "Error saving post to Firestore", e)
                            onComplete(false)
                        } finally {
                            _state.update { it.copy(isUploading = false) }
                        }
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("NoteSharingVM", "Cloudinary upload failed: ${error.description}")
                    _state.update { it.copy(isUploading = false) }
                    onComplete(false)
                }
            }).dispatch()
    }// --- NEW HELPER FUNCTION TO GET THE FILE EXTENSION ---
    private fun getFileExtension(uri: Uri, contentResolver: ContentResolver): String {
        var extension = ""
        // Query the content resolver for the file's display name
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    val fileName = cursor.getString(nameIndex)
                    val dotIndex = fileName.lastIndexOf('.')
                    if (dotIndex > 0) {
                        extension = ".${fileName.substring(dotIndex + 1)}"
                    }
                }
            }
        }
        return extension
    }

    fun toggleUpvote(post: NotePost, userId: String) {
        if (userId.isBlank() || post.id.isBlank()) return
        viewModelScope.launch {
            try {
                val postRef = firestore.collection("notePosts").document(post.id)
                if (post.upvotedBy.contains(userId)) {
                    postRef.update("upvoteCount", FieldValue.increment(-1), "upvotedBy", FieldValue.arrayRemove(userId)).await()
                } else {
                    postRef.update("upvoteCount", FieldValue.increment(1), "upvotedBy", FieldValue.arrayUnion(userId)).await()
                }
            } catch (e: Exception) {
                Log.e("NoteSharingVM", "Error toggling upvote", e)
            }
        }
    }

    fun deleteNoteAndCleanupSubject(notePost: NotePost) {
        viewModelScope.launch {
            try {
                firestore.collection("notePosts").document(notePost.id).delete().await()
                Log.d("NoteSharingVM", "Deleted note ${notePost.id}")

                val remainingNotes = firestore.collection("notePosts")
                    .whereEqualTo("subject", notePost.subject)
                    .limit(1)
                    .get()
                    .await()

                if (remainingNotes.isEmpty) {
                    firestore.collection("subjects").document(notePost.subject).delete().await()
                    Log.d("NoteSharingVM", "Cleaned up empty subject: ${notePost.subject}")
                }

            } catch (e: Exception) {
                Log.e("NoteSharingVM", "Error deleting note or cleaning up subject", e)
            }
        }
    }
}
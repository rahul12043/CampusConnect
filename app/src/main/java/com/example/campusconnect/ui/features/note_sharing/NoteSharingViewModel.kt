package com.example.campusconnect.ui.features.note_sharing

import android.net.Uri
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
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

data class NoteSharingState(
    val posts: List<NotePost> = emptyList(),
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
        listenForNotes()
        fetchSubjects()
    }

    private fun listenForNotes() {
        _state.update { it.copy(isLoading = true) }
        var query: Query = firestore.collection("notePosts").orderBy("timestamp", Query.Direction.DESCENDING)
        _state.value.selectedSubject?.let { subject ->
            if (subject.isNotBlank()) {
                query = query.whereEqualTo("subject", subject)
            }
        }
        query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                _state.update { it.copy(isLoading = false) }
                return@addSnapshotListener
            }
            val posts = snapshots?.documents?.mapNotNull { doc -> doc.toObject(NotePost::class.java)?.copy(id = doc.id) } ?: emptyList()
            _state.update { it.copy(posts = posts, isLoading = false) }
        }
    }

    private fun fetchSubjects() {
        firestore.collection("subjects").orderBy("name").addSnapshotListener { snapshots, _ ->
            val subjects = snapshots?.toObjects(Subject::class.java) ?: emptyList()
            _state.update { it.copy(subjects = subjects) }
        }
    }

    fun filterBySubject(subject: String?) {
        _state.update { it.copy(selectedSubject = subject) }
        listenForNotes()
    }

    private suspend fun createSubjectIfNotExist(subjectName: String) {
        if (subjectName.isBlank()) return
        val formattedSubjectName = subjectName.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val subjectRef = firestore.collection("subjects").document(formattedSubjectName)
        val document = subjectRef.get().await()
        if (!document.exists()) {
            subjectRef.set(Subject(name = formattedSubjectName)).await()
        }
    }

    fun toggleUpvote(post: NotePost, userId: String) {
        if (userId.isBlank() || post.id.isBlank()) return
        viewModelScope.launch {
            val postRef = firestore.collection("notePosts").document(post.id)
            if (post.upvotedBy.contains(userId)) {
                postRef.update("upvoteCount", FieldValue.increment(-1), "upvotedBy", FieldValue.arrayRemove(userId)).await()
            } else {
                postRef.update("upvoteCount", FieldValue.increment(1), "upvotedBy", FieldValue.arrayUnion(userId)).await()
            }
        }
    }

    fun uploadNote(
        user: User,
        title: String,
        description: String?,
        subject: String,
        fileUri: Uri,
        fileType: String,
        onComplete: (Boolean) -> Unit
    ) {
        _state.update { it.copy(isUploading = true) }

        MediaManager.get().upload(fileUri)
            .unsigned("tpxtrxjo")
            .option("resource_type", "auto")
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val downloadUrl = resultData["url"] as String
                    viewModelScope.launch {
                        try {
                            val formattedSubject = subject.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            createSubjectIfNotExist(formattedSubject)

                            val document = firestore.collection("notePosts").document()

                            // --- THIS IS THE FIX ---
                            // Ensure all fields match their data types.
                            val newPost = NotePost(
                                id = document.id,
                                title = title,
                                description = description,
                                fileUrl = downloadUrl,
                                fileType = fileType,
                                authorId = user.uid,
                                authorSapId = user.specializedId, // Correctly use specializedId
                                authorName = user.fullName,         // Correctly use name
                                subject = formattedSubject,
                                timestamp = Timestamp.now()
                            )
                            document.set(newPost).await()
                            onComplete(true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onComplete(false)
                        } finally {
                            _state.update { it.copy(isUploading = false) }
                        }
                    }
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    _state.update { it.copy(isUploading = false) }
                    onComplete(false)
                }
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }
}
package com.example.campusconnect.ui.features

import androidx.lifecycle.ViewModel
import com.example.campusconnect.data.Announcement
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeState(
    val announcements: List<Announcement> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        fetchAnnouncements()
    }

    private fun fetchAnnouncements() {
        firestore.collection("announcements")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(5)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    // Log the error for debugging
                    android.util.Log.w("HomeViewModel", "Listen failed.", error)
                    _state.value = _state.value.copy(isLoading = false)
                    return@addSnapshotListener
                }

                val announcements = snapshots?.toObjects(Announcement::class.java) ?: emptyList()
                _state.value = HomeState(announcements = announcements, isLoading = false)
            }
    }
}
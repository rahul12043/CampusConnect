package com.example.campusconnect.ui.features.lostandfound

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.campusconnect.data.LostFoundItem
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * NEW: This State class is for the list screen.
 * It matches exactly what your LostAndFoundScreen needs: `items` and `isLoading`.
 */
data class LostFoundListState(
    val items: List<LostFoundItem> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * NEW: This ViewModel is for the LostAndFoundScreen (the list view).
 * Its only job is to fetch and provide the list of verified items.
 */
class LostFoundListViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val _state = MutableStateFlow(LostFoundListState())
    val state = _state.asStateFlow()

    init {
        // This listener fetches all items with the status "verified" for students to see.
        firestore.collection("lostAndFoundItems")
            .whereEqualTo("status", "verified")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("L&F_List_VM", "Error listening for items", error)
                    _state.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }

                val items = snapshots?.toObjects(LostFoundItem::class.java) ?: emptyList()
                _state.update { it.copy(items = items, isLoading = false) }
            }
    }
}
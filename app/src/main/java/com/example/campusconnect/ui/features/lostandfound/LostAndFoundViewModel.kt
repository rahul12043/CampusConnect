package com.example.campusconnect.ui.features.lostandfound

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.LostFoundItem
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LostFoundListState(
    val items: List<LostFoundItem> = emptyList(),
    val isLoading: Boolean = true
)

class LostFoundListViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth // Add Firebase Auth instance
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

    // --- NEW FUNCTION TO HANDLE ITEM CLAIMS ---
    fun claimItem(itemId: String, onComplete: (success: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                onComplete(false, "You must be logged in to claim an item.")
                return@launch
            }

            try {
                val itemRef = firestore.collection("lostAndFoundItems").document(itemId)
                // Update the status and add the ID of the user who claimed it
                itemRef.update(
                    mapOf(
                        "status" to "claim_pending",
                        "claimedBy" to userId
                    )
                ).await() // Using await for cleaner async handling
                onComplete(true, "Claim submitted! The admin has been notified.")
            } catch (e: Exception) {
                Log.e("L&F_List_VM", "Error claiming item", e)
                onComplete(false, "Failed to submit claim. Please try again.")
            }
        }
    }
}
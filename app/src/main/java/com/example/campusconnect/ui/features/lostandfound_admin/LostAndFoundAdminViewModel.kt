package com.example.campusconnect.ui.features.lostandfound_admin

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.campusconnect.data.LostFoundItem
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

data class AdminLostFoundState(
    val pendingItems: List<LostFoundItem> = emptyList(),
    val isLoading: Boolean = true
)

class LostAndFoundAdminViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val _state = MutableStateFlow(AdminLostFoundState())
    val state = _state.asStateFlow()

    init {
        // This listener correctly removes items from the UI automatically
        // once their status is no longer "PENDING_REVIEW".
        firestore.collection("lostAndFoundItems")
            .whereEqualTo("status", "PENDING_REVIEW")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("L&F_AdminVM", "Error listening for pending items", error)
                    _state.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }
                val items = snapshots?.toObjects(LostFoundItem::class.java) ?: emptyList()
                // Use .update for thread-safe state modification
                _state.update { it.copy(pendingItems = items, isLoading = false) }
            }
    }

    /**
     * --- THIS IS THE FIX ---
     * This is now a suspend function that awaits the result from Firestore.
     * It returns 'true' on success and 'false' on failure.
     *
     * @param itemId The ID of the document to update.
     * @param newStatus The new status string ("ACTIVE" or "REJECTED").
     * @return Boolean indicating the outcome of the operation.
     */
    suspend fun updateItemStatus(itemId: String, newStatus: String): Boolean {
        if (itemId.isBlank()) return false
        return try {
            firestore.collection("lostAndFoundItems").document(itemId)
                .update("status", newStatus)
                .await() // Wait for the operation to complete
            Log.d("L&F_AdminVM", "Successfully updated item $itemId to $newStatus")
            true
        } catch (e: Exception) {
            Log.e("L&F_AdminVM", "Failed to update status for item $itemId", e)
            false
        }
    }
}
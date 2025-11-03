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

/**
 * MODIFIED: State now holds separate lists for pending and verified items.
 */
data class AdminLostFoundState(
    val pendingItems: List<LostFoundItem> = emptyList(),
    val verifiedItems: List<LostFoundItem> = emptyList(), // For marking as resolved/claimed
    val isLoading: Boolean = true
)

class LostAndFoundAdminViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val itemsCollection = firestore.collection("lostAndFoundItems")

    private val _state = MutableStateFlow(AdminLostFoundState())
    val state = _state.asStateFlow()

    init {
        // Listener for new items awaiting review (status = "open")
        itemsCollection
            .whereEqualTo("status", "open")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("L&F_AdminVM", "Error listening for pending items", error)
                    return@addSnapshotListener
                }
                val pending = snapshots?.toObjects(LostFoundItem::class.java) ?: emptyList()
                _state.update { it.copy(pendingItems = pending, isLoading = false) }
            }

        // Listener for items that are verified and can be marked as "resolved"
        itemsCollection
            .whereEqualTo("status", "verified")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("L&F_AdminVM", "Error listening for verified items", error)
                    return@addSnapshotListener
                }
                val verified = snapshots?.toObjects(LostFoundItem::class.java) ?: emptyList()
                _state.update { it.copy(verifiedItems = verified, isLoading = false) }
            }
    }

    /**
     * MODIFIED: This generic function can now update the status to various states
     * like "verified", "rejected", or "resolved".
     */
    suspend fun updateItemStatus(itemId: String, newStatus: String): Boolean {
        if (itemId.isBlank()) return false
        return try {
            itemsCollection.document(itemId)
                .update("status", newStatus)
                .await()
            Log.d("L&F_AdminVM", "Successfully updated item $itemId to $newStatus")
            true
        } catch (e: Exception) {
            Log.e("L&F_AdminVM", "Failed to update status for item $itemId", e)
            false
        }
    }
}
package com.example.campusconnect.ui.features.lostandfound_admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.LostFoundItem
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * MODIFIED: The state now includes a list for items with a pending claim.
 */
data class AdminLostFoundState(
    val pendingItems: List<LostFoundItem> = emptyList(),      // Status: "open"
    val verifiedItems: List<LostFoundItem> = emptyList(),     // Status: "verified"
    val pendingClaims: List<LostFoundItem> = emptyList(),     // Status: "claim_pending"
    val isLoading: Boolean = true
)

class LostAndFoundAdminViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val itemsCollection = firestore.collection("lostAndFoundItems")

    private val _state = MutableStateFlow(AdminLostFoundState())
    val state = _state.asStateFlow()

    init {
        // --- FIX: USE A SINGLE REAL-TIME LISTENER FOR ALL ITEMS ---
        // This is more efficient and ensures the UI always has the latest data for all categories.
        listenForAllItems()
    }

    private fun listenForAllItems() {
        itemsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("L&F_AdminVM", "Error listening for item changes", error)
                _state.update { it.copy(isLoading = false) }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val allItems = snapshot.toObjects(LostFoundItem::class.java)
                // Filter the single source of truth into the correct categories
                _state.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        pendingItems = allItems.filter { it.status == "open" }.sortedByDescending { it.timestamp },
                        verifiedItems = allItems.filter { it.status == "verified" }.sortedByDescending { it.timestamp },
                        pendingClaims = allItems.filter { it.status == "claim_pending" }.sortedByDescending { it.timestamp }
                    )
                }
            }
        }
    }

    /**
     * This generic function updates the status for approving ("verified") or rejecting ("rejected") items.
     */
    suspend fun updateItemStatus(itemId: String, newStatus: String): Boolean {
        if (itemId.isBlank()) return false
        return try {
            itemsCollection.document(itemId).update("status", newStatus).await()
            Log.d("L&F_AdminVM", "Successfully updated item $itemId to $newStatus")
            true
        } catch (e: Exception) {
            Log.e("L&F_AdminVM", "Failed to update status for item $itemId", e)
            false
        }
    }

    /**
     * NEW: Confirms a claim, marking the item as resolved.
     */
    suspend fun confirmResolution(itemId: String): Boolean {
        return updateItemStatus(itemId, "resolved")
    }

    /**
     * NEW: Denies a claim, returning the item to the "verified" pool for others to see.
     */
    suspend fun denyClaim(itemId: String): Boolean {
        if (itemId.isBlank()) return false
        return try {
            // This requires updating two fields: the status and clearing the claimedBy field.
            itemsCollection.document(itemId).update(
                mapOf(
                    "status" to "verified",
                    "claimedBy" to null
                )
            ).await()
            Log.d("L&F_AdminVM", "Successfully denied claim for item $itemId")
            true
        } catch (e: Exception) {
            Log.e("L&F_AdminVM", "Failed to deny claim for item $itemId", e)
            false
        }
    }
}
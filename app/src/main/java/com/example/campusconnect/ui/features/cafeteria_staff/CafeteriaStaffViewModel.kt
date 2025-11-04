package com.example.campusconnect.ui.features.cafeteria_staff

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.campusconnect.data.MenuItem
import com.example.campusconnect.data.Order
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
// import com.google.firebase.storage.ktx.storage // No longer needed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

data class StaffState(
    val newOrders: List<Order> = emptyList(),
    val preparingOrders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val isUploading: Boolean = false // Keep this for the "Add Menu Item" screen
)

class CafeteriaStaffViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    // private val storage = Firebase.storage // No longer needed
    private val _state = MutableStateFlow(StaffState())
    val state = _state.asStateFlow()

    init {
        // Your existing order listeners are perfect and remain unchanged
        listenForNewOrders()
        listenForPreparingOrders()
    }

    // --- THIS IS THE NEW, UPDATED FUNCTION ---
    fun addMenuItem(name: String, description: String, price: Double, imageUri: Uri, onComplete: (Boolean) -> Unit) {
        _state.update { it.copy(isUploading = true) }

        // Use the Cloudinary logic from your NoteSharingViewModel
        MediaManager.get().upload(imageUri)
            .unsigned("tpxtrxjo") // Assuming this is your unsigned upload preset
            .option("resource_type", "auto")
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as String
                    viewModelScope.launch {
                        try {
                            val document = firestore.collection("menuItems").document()
                            val newItem = MenuItem(
                                id = document.id,
                                name = name,
                                description = description,
                                price = price,
                                imageUrl = imageUrl // Use the URL from Cloudinary
                            )
                            document.set(newItem).await()
                            onComplete(true)
                        } catch (e: Exception) {
                            Log.e("CafeteriaStaffVM", "Error saving menu item to Firestore", e)
                            onComplete(false)
                        } finally {
                            _state.update { it.copy(isUploading = false) }
                        }
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("CafeteriaStaffVM", "Cloudinary upload failed: ${error.description}")
                    _state.update { it.copy(isUploading = false) }
                    onComplete(false)
                }
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    // --- ALL YOUR ORDER MANAGEMENT FUNCTIONS REMAIN UNCHANGED ---

    private fun listenForNewOrders() {
        firestore.collection("orders")
            .whereEqualTo("status", "PLACED")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    _state.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }
                val orders = value?.toObjects(Order::class.java) ?: emptyList()
                _state.update { currentState ->
                    currentState.copy(newOrders = orders, isLoading = false) // Also update loading state here
                }
            }
    }

    private fun listenForPreparingOrders() {
        firestore.collection("orders")
            .whereEqualTo("status", "PREPARING")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    _state.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }
                val orders = value?.toObjects(Order::class.java) ?: emptyList()
                _state.update { currentState ->
                    currentState.copy(preparingOrders = orders, isLoading = false) // And here
                }
            }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        if (orderId.isNotBlank()) {
            firestore.collection("orders").document(orderId).update("status", newStatus)
        }
    }
}
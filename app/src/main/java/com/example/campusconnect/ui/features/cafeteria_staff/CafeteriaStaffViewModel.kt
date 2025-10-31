package com.example.campusconnect.ui.features.cafeteria_staff

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.MenuItem
import com.example.campusconnect.data.Order
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
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
    val isUploading: Boolean = false
)

class CafeteriaStaffViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage
    private val _state = MutableStateFlow(StaffState())
    val state = _state.asStateFlow()

    init {
        listenForNewOrders()
        listenForPreparingOrders()
    }

    fun addMenuItem(name: String, description: String, price: Double, imageUri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true) }
            try {
                val fileName = "menu_items/${UUID.randomUUID()}.jpg"
                val storageRef = storage.reference.child(fileName)
                storageRef.putFile(imageUri).await()
                val imageUrl = storageRef.downloadUrl.await().toString()

                val document = firestore.collection("menuItems").document()
                val newItem = MenuItem(
                    id = document.id,
                    name = name,
                    description = description,
                    price = price,
                    imageUrl = imageUrl
                )
                document.set(newItem).await()
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                _state.update { it.copy(isUploading = false) }
            }
        }
    }

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
                    currentState.copy(newOrders = orders)
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
                    currentState.copy(preparingOrders = orders)
                }
            }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        if (orderId.isNotBlank()) {
            firestore.collection("orders").document(orderId).update("status", newStatus)
        }
    }
}
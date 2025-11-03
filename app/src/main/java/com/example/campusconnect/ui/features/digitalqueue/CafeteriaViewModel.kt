package com.example.campusconnect.ui.features.digitalqueue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.User
import com.example.campusconnect.data.MenuItem
import com.example.campusconnect.data.Order
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CafeteriaState(
    val menuItems: List<MenuItem> = emptyList(),
    val cart: Map<MenuItem, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val orderPlacedSuccessfully: Boolean? = null
)

class CafeteriaViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private val _state = MutableStateFlow(CafeteriaState())
    val state = _state.asStateFlow()

    init {
        firestore.collection("menuItems").get()
            .addOnSuccessListener { result ->
                val items = result.toObjects(MenuItem::class.java)
                _state.value = _state.value.copy(menuItems = items, isLoading = false)
            }
            .addOnFailureListener {
                _state.value = _state.value.copy(isLoading = false)
            }
    }

    fun addToCart(item: MenuItem) {
        _state.update { currentState ->
            val newCart = currentState.cart.toMutableMap()
            newCart[item] = (newCart[item] ?: 0) + 1
            currentState.copy(cart = newCart)
        }
    }

    fun removeFromCart(item: MenuItem) {
        _state.update { currentState ->
            val newCart = currentState.cart.toMutableMap()
            val currentCount = newCart[item] ?: 0
            if (currentCount > 1) {
                newCart[item] = currentCount - 1
            } else {
                newCart.remove(item)
            }
            currentState.copy(cart = newCart)
        }
    }

    fun placeOrder(currentUser: User) {
        val cart = _state.value.cart
        if (cart.isEmpty()) return

        val userId = currentUser.uid
        val userName = currentUser.specializedId

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val document = firestore.collection("orders").document()
                val newOrder = Order(
                    orderId = document.id,
                    userId = userId,
                    userName = userName,
                    items = cart.keys.map { "${it.name} x${cart[it]}" },
                    totalPrice = cart.entries.sumOf { (item, quantity) -> item.price * quantity },
                    status = "PLACED",
                    timestamp = Timestamp.now()
                )
                document.set(newOrder).await()
                _state.value = CafeteriaState(menuItems = _state.value.menuItems, isLoading = false, orderPlacedSuccessfully = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, orderPlacedSuccessfully = false)
            }
        }
    }

    fun resetOrderSuccessStatus() {
        _state.value = _state.value.copy(orderPlacedSuccessfully = null)
    }
}
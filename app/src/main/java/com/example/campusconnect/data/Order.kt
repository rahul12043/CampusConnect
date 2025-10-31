package com.example.campusconnect.data
import com.google.firebase.Timestamp
// Data model for a single cafeteria order
data class Order(
    val orderId: String = "",
    val userId: String = "",
    val userName: String = "", // e.g., the student's SAP ID
    val items: List<String> = emptyList(), // List of item names
    val totalPrice: Double = 0.0,
    val status: String = "PLACED", // PLACED, PREPARING, READY_FOR_PICKUP, COMPLETED
    val timestamp: Timestamp? = null
)
package com.example.campusconnect.data
// FIX: Added default values to all properties to prevent crashing
// when Firestore deserializes the data.
data class MenuItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = ""
)
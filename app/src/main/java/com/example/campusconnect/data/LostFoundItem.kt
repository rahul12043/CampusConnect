package com.example.campusconnect.data

import com.google.firebase.Timestamp

data class LostFoundItem(
    val id: String = "",
    val name: String = "",
    // CHANGE: Description is now nullable to make it optional
    val description: String? = null,
    val location: String = "",
    val status: String = "PENDING_REVIEW",
    val reporterId: String = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp? = null
)
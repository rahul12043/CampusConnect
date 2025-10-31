package com.example.campusconnect.data

import com.google.firebase.Timestamp

data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val isUrgent: Boolean = false,
    // FIX: Ensure Timestamp is nullable and has a default value
    val timestamp: Timestamp? = null
)
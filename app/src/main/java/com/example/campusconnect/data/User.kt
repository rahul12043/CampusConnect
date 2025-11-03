package com.example.campusconnect.data

/**
 * The single, definitive data model for a user in the application.
 * This class will be used everywhere.
 */
data class User(
    val uid: String = "",
    val specializedId: String = "",
    val contactEmail: String = "",
    val role: String = "",
    val fullName: String = "" // <-- The missing property is now here
)
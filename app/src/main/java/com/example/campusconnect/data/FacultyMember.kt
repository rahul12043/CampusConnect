package com.example.campusconnect.data

// Data model for a single faculty member
data class FacultyMember(
    val id: String = "",
    val name: String = "",
    val department: String = "",
    val officeLocation: String = "",
    val imageUrl: String = "",
    val email: String = "",
    // The timetable is a map where the key is the day (e.g., "Monday")
    // and the value is a string describing their availability.
    val timetable: Map<String, String> = emptyMap()
)

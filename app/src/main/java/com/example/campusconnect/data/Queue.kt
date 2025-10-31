package com.example.campusconnect.data

data class Queue(
    val id: String,
    val name: String,
    val location: String,
    val currentWaitTime: Int, // in minutes
    val peopleInQueue: Int
)
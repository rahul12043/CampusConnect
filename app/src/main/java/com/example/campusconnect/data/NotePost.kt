package com.example.campusconnect.data

import com.google.firebase.Timestamp

data class NotePost(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val fileUrl: String = "",
    val fileType: String = "",
    val authorId: String = "",
    val authorSapId: String = "",
    val authorName: String = "",
    val upvoteCount: Int = 0,
    val upvotedBy: List<String> = emptyList(),
    val subject: String = "",
    val timestamp: Timestamp? = null
)
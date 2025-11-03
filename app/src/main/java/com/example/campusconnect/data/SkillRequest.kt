package com.example.campusconnect.data

import com.google.firebase.Timestamp

/**
 * Data model for a single skill request posted by a student who wants to learn something.
 * This is the main document stored in the "skillRequests" collection.
 */
data class SkillRequest(
    val requestId: String = "",
    val skillName: String = "",
    val description: String = "",

    val postedByUid: String = "",
    val postedByName: String = "",
    val postedBySapId: String = "",

    val status: String = "OPEN",
    val preferredTimeSlots: String = "",

    // This list will contain all the offers from students who can help.
    val offers: List<HelperOffer> = emptyList(),

    val timestamp: Timestamp? = null
)

/**
 * Data model representing an offer from one student (the helper) to another.
 * This object is nested inside the 'offers' array in a SkillRequest document.
 */
data class HelperOffer(
    val helperUid: String = "",
    val helperName: String = "",
    val helperContactEmail: String = ""
)
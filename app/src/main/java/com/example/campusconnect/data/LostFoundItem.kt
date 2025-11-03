package com.example.campusconnect.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * MODIFIED: This data class now aligns with the system design specification.
 *
 * Changes:
 * - `name` renamed to `title`.
 * - `type` ("lost" or "found") field added.
 * - `reporterId` renamed to `posted_by` for clarity.
 * - `status` default value changed to "open" as per the flow.
 * - `claimed_by` field added to track who claims an item.
 * - @PropertyName annotations used for Firestore field name consistency.
 */
data class LostFoundItem(
    val id: String = "",
    val type: String = "", // "lost" or "found"
    val title: String = "",
    val description: String? = null,

    @get:PropertyName("image_url") @set:PropertyName("image_url")
    var imageUrl: String? = null,

    val status: String = "open", // open | verified | matched | resolved | rejected

    @get:PropertyName("posted_by") @set:PropertyName("posted_by")
    var postedBy: String = "",

    @get:PropertyName("claimed_by") @set:PropertyName("claimed_by")
    var claimedBy: String? = null,

    val timestamp: Timestamp? = null,

    // This field was in your original code and is useful for display.
    val location: String = ""
)
package com.example.campusconnect.ui.features.peerskill

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.HelperOffer
import com.example.campusconnect.data.SkillRequest
import com.example.campusconnect.data.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PeerSkillState(
    val skillRequests: List<SkillRequest> = emptyList(),
    val isLoading: Boolean = true
)

class PeerSkillViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _state = MutableStateFlow(PeerSkillState())
    val state = _state.asStateFlow()

    init {
        fetchSkillRequests()
    }

    private fun fetchSkillRequests() {
        _state.update { it.copy(isLoading = true) }
        db.collection("skillRequests")
            .whereEqualTo("status", "OPEN")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("PeerSkillVM", "Error listening for skill requests", error)
                    _state.update { it.copy(isLoading = false) }
                    return@addSnapshotListener
                }

                val requests = snapshots?.toObjects(SkillRequest::class.java) ?: emptyList()
                _state.update { it.copy(skillRequests = requests, isLoading = false) }
            }
    }

    fun createSkillRequest(
        skillName: String,
        description: String,
        preferredTimes: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val user = auth.currentUser ?: run {
                onComplete(false)
                return@launch
            }

            val userProfile = try {
                db.collection("users").document(user.uid).get().await().toObject(User::class.java)
            } catch (e: Exception) {
                Log.e("PeerSkillVM", "Error fetching user profile for request creation", e)
                null
            }

            if (userProfile == null) {
                onComplete(false)
                return@launch
            }

            val collectionRef = db.collection("skillRequests")
            val requestId = collectionRef.document().id

            val newRequest = SkillRequest(
                requestId = requestId,
                skillName = skillName,
                description = description,
                preferredTimeSlots = preferredTimes,
                postedByUid = userProfile.uid,
                postedByName = userProfile.fullName,
                postedBySapId = userProfile.specializedId,
                status = "OPEN",
                offers = emptyList(),
                timestamp = Timestamp.now()
            )

            try {
                collectionRef.document(requestId).set(newRequest).await()
                onComplete(true)
            } catch (e: Exception) {
                Log.e("PeerSkillVM", "Failed to create skill request", e)
                onComplete(false)
            }
        }
    }

    fun offerHelp(request: SkillRequest, helper: User) {
        viewModelScope.launch {
            if (request.postedByUid == helper.uid) {
                Log.d("PeerSkillVM", "User cannot offer help on their own request.")
                return@launch
            }

            val docRef = db.collection("skillRequests").document(request.requestId)

            val newOffer = HelperOffer(
                helperUid = helper.uid,
                helperName = helper.fullName,
                helperContactEmail = helper.contactEmail
            )

            try {
                docRef.update("offers", FieldValue.arrayUnion(newOffer)).await()
                Log.d("PeerSkillVM", "Successfully offered help for request: ${request.requestId}")
            } catch (e: Exception) {
                Log.e("PeerSkillVM", "Error offering help", e)
            }
        }
    }

    fun markAsResolved(request: SkillRequest) {
        viewModelScope.launch {
            if (request.postedByUid != auth.currentUser?.uid) {
                Log.w("PeerSkillVM", "Unauthorized attempt to resolve request.")
                return@launch
            }

            try {
                db.collection("skillRequests").document(request.requestId)
                    .update("status", "RESOLVED").await()
                Log.d("PeerSkillVM", "Successfully marked request as resolved: ${request.requestId}")
            } catch (e: Exception) {
                Log.e("PeerSkillVM", "Error resolving request", e)
            }
        }
    }
}
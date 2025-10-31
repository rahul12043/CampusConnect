package com.example.campusconnect.ui.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.User // Re-use the User data class
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserManagementState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = true
)

class UserManagementViewModel : ViewModel() {
    private val firestore = Firebase.firestore

    private val _state = MutableStateFlow(UserManagementState())
    val state = _state.asStateFlow()

    init {
        fetchUsers()
    }

    private fun fetchUsers() {
        _state.update { it.copy(isLoading = true) }

        // Listen for real-time updates to the users collection
        firestore.collection("users").addSnapshotListener { snapshots, error ->
            if (error != null) {
                println("Error fetching users: ${error.message}")
                _state.update { it.copy(isLoading = false) }
                return@addSnapshotListener
            }

            val userList = snapshots?.documents?.mapNotNull { doc ->
                // Automatically map the Firestore document to our User data class
                doc.toObject(User::class.java)
            } ?: emptyList()

            _state.update { it.copy(users = userList, isLoading = false) }
        }
    }

    fun updateUserRole(userId: String, newRole: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("users").document(userId)
                    .update("role", newRole)
                    .await()
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun deleteUser(userId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Note: This only deletes the Firestore user document, not the
                // Firebase Auth entry. Deleting the auth entry requires Firebase Functions
                // or the Admin SDK on a backend for security reasons.
                firestore.collection("users").document(userId).delete().await()
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }
}
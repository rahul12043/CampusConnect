package com.example.campusconnect.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// This AuthState sealed class is correct and complete.
sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

// This User data class is now in the correct context and will compile.
data class User(
    val uid: String = "",
    val specializedId: String = "",
    val contactEmail: String = "",
    val name: String = "",
    val role: String = "student"
)

// The complete and correct ViewModel class definition.
class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                fetchUserProfile(firebaseUser.uid)
            } else {
                _authState.value = AuthState.Idle
                _currentUser.value = null
            }
        }
    }

    private fun fetchUserProfile(uid: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error("User profile not found.")
                }
            }
            .addOnFailureListener { exception ->
                _authState.value = AuthState.Error("Failed to load profile: ${exception.message}")
            }
    }

    fun login(specializedId: String, password: String) {
        if (specializedId.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("ID and password cannot be empty.")
            return
        }
        val authEmail = "$specializedId@nmims.campusconnect"
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(authEmail, password).await()
                // The AuthStateListener will handle fetching the user profile
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unknown login error occurred")
            }
        }
    }

    fun register(specializedId: String, contactEmail: String, name: String, password: String) {
        if (specializedId.isBlank() || contactEmail.isBlank() || name.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("All fields are required.")
            return
        }

        val authEmail = "$specializedId@nmims.campusconnect"

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(authEmail, password).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val newUser = User(
                        uid = firebaseUser.uid,
                        specializedId = specializedId,
                        contactEmail = contactEmail,
                        name = name,
                        role = "student"
                    )
                    firestore.collection("users").document(firebaseUser.uid).set(newUser).await()
                    // The AuthStateListener will handle the rest
                } else {
                    _authState.value = AuthState.Error("Failed to create user profile.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unknown registration error occurred")
            }
        }
    }
}
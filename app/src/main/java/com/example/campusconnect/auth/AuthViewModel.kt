package com.example.campusconnect.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.User // <-- FIX: Import the correct User class from the data package
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    // --- FIX: This StateFlow now holds the correct data.User object ---
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
        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(uid).get().await()
                if (document != null && document.exists()) {
                    // --- FIX: Map the Firestore document to the correct data.User class ---
                    val user = document.toObject(User::class.java)
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error("User profile not found.")
                }
            } catch (exception: Exception) {
                _authState.value = AuthState.Error("Failed to load profile: ${exception.message}")
            }
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
                // The AuthStateListener will automatically handle fetching the user profile
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unknown login error occurred")
            }
        }
    }

    fun register(specializedId: String, contactEmail: String, fullName: String, password: String) {
        if (specializedId.isBlank() || contactEmail.isBlank() || fullName.isBlank() || password.isBlank()) {
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
                    // --- FIX: Create an instance of the new data.User object ---
                    val newUser = User(
                        uid = firebaseUser.uid,
                        specializedId = specializedId,
                        contactEmail = contactEmail,
                        fullName = fullName, // Use 'fullName' to match the data class
                        role = "student"
                    )
                    firestore.collection("users").document(firebaseUser.uid).set(newUser).await()
                    // The AuthStateListener will handle setting the state
                } else {
                    _authState.value = AuthState.Error("Failed to create user profile.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unknown registration error occurred")
            }
        }
    }
}
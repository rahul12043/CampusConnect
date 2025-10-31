package com.example.campusconnect.ui.features.faculty_connect

import androidx.lifecycle.ViewModel
import com.example.campusconnect.data.FacultyMember
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FacultyState(
    val facultyList: List<FacultyMember> = emptyList(),
    val isLoading: Boolean = true
)

class FacultyViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val _state = MutableStateFlow(FacultyState())
    val state = _state.asStateFlow()

    init {
        fetchFacultyMembers()
    }

    private fun fetchFacultyMembers() {
        firestore.collection("facultyMembers")
            .get()
            .addOnSuccessListener { result ->
                // --- THIS IS THE FIX ---
                // We must manually iterate through the documents to get their IDs.
                // .toObjects() does not provide the document ID.
                val facultyList = result.documents.mapNotNull { document ->
                    // Convert the document's data to our FacultyMember class
                    val facultyMember = document.toObject(FacultyMember::class.java)
                    // Manually set the 'id' field using the document's unique ID
                    facultyMember?.copy(id = document.id)
                }

                _state.value = FacultyState(facultyList = facultyList, isLoading = false)
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("FacultyViewModel", "Error fetching faculty", exception)
                _state.value = _state.value.copy(isLoading = false)
            }
    }
}
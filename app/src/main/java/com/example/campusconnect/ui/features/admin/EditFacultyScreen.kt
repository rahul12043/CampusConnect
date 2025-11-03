package com.example.campusconnect.ui.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campusconnect.data.FacultyMember
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFacultyScreen(
    navController: NavController,
    facultyId: String,
    viewModel: AdminViewModel
) {
    val state by viewModel.state.collectAsState()
    val facultyMember = state.facultyList.find { it.id == facultyId }

    // --- STATE MANAGEMENT FOR ALL EDITABLE FIELDS ---
    var name by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var officeLocation by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    // State to hold the timetable. Using mutableStateMapOf to ensure Compose recomposes on changes.
    var timetable by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    var showDeleteDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // This effect will run once when facultyMember is found, populating all the states.
    LaunchedEffect(facultyMember) {
        facultyMember?.let {
            name = it.name
            department = it.department
            officeLocation = it.officeLocation
            email = it.email
            timetable = it.timetable
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (facultyMember == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // Show a more persistent loading indicator or message
                CircularProgressIndicator()
                Text("Loading details...")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp) // Add horizontal padding
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(16.dp))

                // --- CORE DETAILS SECTION ---
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = officeLocation, onValueChange = { officeLocation = it }, label = { Text("Office Location") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Contact Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Divider(modifier = Modifier.padding(vertical = 24.dp))

                // --- TIMETABLE SECTION ---
                Text("Weekly Timetable", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                daysOfWeek.forEach { day ->
                    OutlinedTextField(
                        value = timetable[day] ?: "",
                        onValueChange = { newAvailability ->
                            // Update the map with the new value for the specific day
                            timetable = timetable.toMutableMap().apply { this[day] = newAvailability }
                        },
                        label = { Text(day) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // --- ACTIONS SECTION ---
                Button(
                    onClick = {
                        // Create an updated FacultyMember object to pass to the ViewModel
                        val updatedFaculty = FacultyMember(
                            id = facultyId, // ID is not changed
                            name = name,
                            department = department,
                            officeLocation = officeLocation,
                            email = email,
                            timetable = timetable,
                            imageUrl = facultyMember.imageUrl // Image URL is not changed here
                        )
                        viewModel.updateFacultyMember(updatedFaculty) { success ->
                            if (success) {
                                navController.popBackStack()
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Failed to update details.") }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Save Changes")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Faculty Member")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // --- DELETE CONFIRMATION DIALOG ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this faculty member? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFacultyMember(facultyId) { success ->
                            if (success) {
                                showDeleteDialog = false
                                navController.popBackStack()
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Failed to delete.") }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
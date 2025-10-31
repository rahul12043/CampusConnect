package com.example.campusconnect.ui.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFacultyScreen(
    navController: NavController,
    facultyId: String,
    viewModel: AdminViewModel // Receives the shared ViewModel
) {
    val state by viewModel.state.collectAsState()
    val facultyMember = state.facultyList.find { it.id == facultyId }

    var name by remember { mutableStateOf(facultyMember?.name ?: "") }
    var department by remember { mutableStateOf(facultyMember?.department ?: "") }
    var officeLocation by remember { mutableStateOf(facultyMember?.officeLocation ?: "") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(facultyMember) {
        facultyMember?.let {
            name = it.name
            department = it.department
            officeLocation = it.officeLocation
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (facultyMember == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading faculty details...")
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(2.dp).verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = officeLocation, onValueChange = { officeLocation = it }, label = { Text("Office Location") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.updateFacultyMember(facultyId, name, department, officeLocation) { success ->
                            if (success) {
                                navController.popBackStack()
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Failed to update.") }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Save Changes")
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { showDeleteDialog = true }, // This will now show your dialog
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Text("Delete Faculty Member")
                }
            }
        }
    }

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
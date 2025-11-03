package com.example.campusconnect.ui.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScreen(
    navController: NavController,
    userId: String,
    viewModel: UserManagementViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val user = state.users.find { it.uid == userId }

    // State for the UI
    var selectedRole by remember { mutableStateOf(user?.role ?: "") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // List of available roles
    val roles = listOf("student", "admin", "cafeteria_staff")

    // Update the local state if the user data changes
    LaunchedEffect(user) {
        user?.let {
            selectedRole = it.role
        }
    }

    Scaffold(
    ) { padding ->
        if (user == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading user details...")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(2.dp)
            ) {
                Text("Name: ${user.fullName}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(24.dp))
                Text("ID: ${user.specializedId}", style = MaterialTheme.typography.bodyLarge)
                Text("Email: ${user.contactEmail}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(24.dp))

                // Role selection dropdown
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedRole,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role) },
                                onClick = {
                                    selectedRole = role
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.updateUserRole(userId, selectedRole) { success ->
                            if (success) {
                                navController.popBackStack()
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Failed to update role.") }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { showDeleteDialog = true }, // This triggers the confirmation dialog
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete User")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this user's data? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(userId) { success ->
                            if (success) {
                                showDeleteDialog = false
                                navController.popBackStack()
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Failed to delete user.") }
                                showDeleteDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },

        )
    }
}
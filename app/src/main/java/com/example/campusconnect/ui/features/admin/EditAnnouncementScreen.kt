package com.example.campusconnect.ui.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAnnouncementScreen(
    navController: NavController,
    announcementId: String,
    viewModel: AdminViewModel
) {
    val state by viewModel.state.collectAsState()
    val announcement = state.announcements.find { it.id == announcementId }

    var title by remember { mutableStateOf(announcement?.title ?: "") }
    var message by remember { mutableStateOf(announcement?.message ?: "") }
    var isUrgent by remember { mutableStateOf(announcement?.isUrgent ?: false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(announcement) {
        announcement?.let {
            title = it.title
            message = it.message
            isUrgent = it.isUrgent
        }
    }

    Scaffold(

    ) { padding ->
        if (announcement == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading announcement details...")
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(2.dp).verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth().height(150.dp))
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isUrgent, onCheckedChange = { isUrgent = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Mark as Urgent")
                }
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        // Call the new ViewModel function
                        viewModel.updateAnnouncement(announcementId, title, message, isUrgent) { success ->
                            if (success) {
                                navController.popBackStack()
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Failed to update announcement.") }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    // Enable the button now that the function exists
                    enabled = true
                ) {
                    Text("Save Changes")
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { showDeleteDialog = true }, // This will now show your dialog
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Text("Delete Announcement")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this announcement? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAnnouncement(announcementId)
                        showDeleteDialog = false
                        navController.popBackStack()
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
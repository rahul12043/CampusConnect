package com.example.campusconnect.ui.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAnnouncementScreen(
    // onAnnouncementAdded: () -> Unit, // <-- 1. THIS PARAMETER IS REMOVED
    navController: NavController,
    viewModel: AdminViewModel
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isUrgent, onCheckedChange = { isUrgent = it })
                Spacer(Modifier.width(8.dp))
                Text("Mark as Urgent")
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (title.isNotBlank() && message.isNotBlank()) {
                        viewModel.addAnnouncement(title, message, isUrgent) { success ->
                            if (success) {
                                // 2. The screen now handles its own navigation back
                                navController.popBackStack()
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Failed to add announcement.") }
                            }
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Title and message cannot be empty.") }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Add Announcement")
            }
        }
    }
}
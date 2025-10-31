package com.example.campusconnect.ui.features.lostandfound_admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.campusconnect.data.LostFoundItem
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostAndFoundAdminScreen(onLogout: () -> Unit) {
    val viewModel: LostAndFoundAdminViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    // --- FIX: Add coroutine scope and snackbar state for user feedback ---
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedItem by remember { mutableStateOf<LostFoundItem?>(null) }
    var isUpdating by remember { mutableStateOf(false) } // To disable buttons during update

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("L&F Admin Panel") },
                actions = {
                    IconButton(onClick = {
                        Firebase.auth.signOut()
                        onLogout()
                    }) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout") }
                }
            )
        },
        // --- FIX: Add the SnackbarHost to the Scaffold ---
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Text("Items Awaiting Review", style = MaterialTheme.typography.headlineSmall) }
                if (state.pendingItems.isEmpty()) {
                    item { Text("No items are currently pending review.") }
                }
                items(state.pendingItems) { item ->
                    AdminItemCard(
                        item = item,
                        onClick = { selectedItem = item }
                    )
                }
            }
        }
    }

    if (selectedItem != null) {
        AlertDialog(
            onDismissRequest = { if (!isUpdating) selectedItem = null },
            title = { Text(selectedItem!!.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Show image in the dialog for better context
                    selectedItem!!.imageUrl?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = "Item image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Text("Description: ${selectedItem!!.description ?: "N/A"}")
                    Text("Location: ${selectedItem!!.location}")
                }
            },
            confirmButton = {
                Button(
                    // --- FIX: Call the suspend function from a coroutine ---
                    onClick = {
                        scope.launch {
                            isUpdating = true
                            val success = viewModel.updateItemStatus(selectedItem!!.id, "ACTIVE")
                            val message = if (success) "Item approved successfully" else "Failed to approve item"
                            snackbarHostState.showSnackbar(message)
                            isUpdating = false
                            selectedItem = null // Dismiss dialog after operation
                        }
                    },
                    enabled = !isUpdating // Disable button while updating
                ) { Text("Accept") }
            },
            dismissButton = {
                OutlinedButton(
                    // --- FIX: Call the suspend function from a coroutine ---
                    onClick = {
                        scope.launch {
                            isUpdating = true
                            val success = viewModel.updateItemStatus(selectedItem!!.id, "REJECTED")
                            val message = if (success) "Item rejected successfully" else "Failed to reject item"
                            snackbarHostState.showSnackbar(message)
                            isUpdating = false
                            selectedItem = null // Dismiss dialog after operation
                        }
                    },
                    enabled = !isUpdating // Disable button while updating
                ) { Text("Reject") }
            }
        )
    }
}

@Composable
fun AdminItemCard(item: LostFoundItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            item.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = item.name,
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Item: ${item.name}", style = MaterialTheme.typography.titleLarge)
                Text("Location: ${item.location}")
            }
        }
    }
}
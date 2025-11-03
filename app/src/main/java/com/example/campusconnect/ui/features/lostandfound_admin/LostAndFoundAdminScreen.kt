package com.example.campusconnect.ui.features.lostandfound_admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.LocalIndication
// import androidx.compose.foundation.border // Not needed for the final fix
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedPendingItem by remember { mutableStateOf<LostFoundItem?>(null) }
    var isUpdating by remember { mutableStateOf(false) }

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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading && state.pendingItems.isEmpty() && state.verifiedItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("Items Awaiting Review", style = MaterialTheme.typography.headlineSmall) }
                if (state.pendingItems.isEmpty()) {
                    item { Text("No items are currently pending review.", modifier = Modifier.padding(bottom = 16.dp)) }
                }
                items(state.pendingItems) { item ->
                    AdminItemCard(
                        item = item,
                        onClick = { selectedPendingItem = item }
                    )
                }

                item { Divider(modifier = Modifier.padding(vertical = 16.dp)) }

                item { Text("Verified Items", style = MaterialTheme.typography.headlineSmall) }
                if (state.verifiedItems.isEmpty()) {
                    item { Text("No items are currently verified.") }
                }
                items(state.verifiedItems) { item ->
                    VerifiedItemCard(
                        item = item,
                        onMarkResolved = {
                            scope.launch {
                                val success = viewModel.updateItemStatus(item.id, "resolved")
                                val message = if (success) "Item marked as resolved." else "Failed to update item."
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    )
                }
            }
        }
    }

    if (selectedPendingItem != null) {
        val item = selectedPendingItem!!
        AlertDialog(
            onDismissRequest = { if (!isUpdating) selectedPendingItem = null },
            title = { Text(item.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.imageUrl?.let {
                        AsyncImage(model = it, contentDescription = "Item image", modifier = Modifier.fillMaxWidth().height(150.dp), contentScale = ContentScale.Fit)
                    }
                    Text("Type: ${item.type.uppercase()}", fontWeight = FontWeight.Bold)
                    Text("Description: ${item.description ?: "N/A"}")
                    Text("Location: ${item.location}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isUpdating = true
                            val success = viewModel.updateItemStatus(item.id, "verified")
                            val message = if (success) "Item approved successfully" else "Failed to approve item"
                            snackbarHostState.showSnackbar(message)
                            isUpdating = false
                            selectedPendingItem = null
                        }
                    },
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Approve") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isUpdating = true
                            val success = viewModel.updateItemStatus(item.id, "rejected")
                            val message = if (success) "Item rejected successfully" else "Failed to reject item"
                            snackbarHostState.showSnackbar(message)
                            isUpdating = false
                            selectedPendingItem = null
                        }
                    },
                    enabled = !isUpdating
                ) { Text("Reject") }
            }
        )
    }
}

// --- THIS IS THE CORRECTED CODE ---
@Composable
fun AdminItemCard(item: LostFoundItem, onClick: () -> Unit) {
    Card(
        // The modifier is now correct for a simple clickable list item.
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick // Use the 'onClick' that was passed into this function
            ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            item.imageUrl?.let {
                AsyncImage(model = it, contentDescription = item.title, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleLarge)
                Text("Location: ${item.location}")
                Text("Type: ${item.type.uppercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

// Card for items in the VERIFIED list, with a button to resolve them
@Composable
fun VerifiedItemCard(item: LostFoundItem, onMarkResolved: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.imageUrl?.let {
                    AsyncImage(model = it, contentDescription = item.title, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleLarge)
                    Text("Location: ${item.location}")
                    Text("Type: ${item.type.uppercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onMarkResolved,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mark as Resolved / Claimed")
            }
        }
    }
}
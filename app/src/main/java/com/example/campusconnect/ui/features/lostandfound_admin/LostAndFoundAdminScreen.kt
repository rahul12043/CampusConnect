package com.example.campusconnect.ui.features.lostandfound_admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
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

    var selectedItem by remember { mutableStateOf<LostFoundItem?>(null) }
    var itemToDelete by remember { mutableStateOf<LostFoundItem?>(null) }
    var isUpdating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "L&F Admin Panel",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = {
                        Firebase.auth.signOut()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading && state.pendingItems.isEmpty() && state.verifiedItems.isEmpty() && state.pendingClaims.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Text("Pending Claims", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary) }
                if (state.pendingClaims.isEmpty()) {
                    item { Text("No items have been claimed yet.") }
                }
                items(state.pendingClaims) { item ->
                    AdminItemCard(item = item, onClick = { selectedItem = item })
                }

                item { Divider() }

                item { Text("Items Awaiting Review", style = MaterialTheme.typography.headlineSmall) }
                if (state.pendingItems.isEmpty()) {
                    item { Text("No items are currently pending review.") }
                }
                items(state.pendingItems) { item ->
                    AdminItemCard(item = item, onClick = { selectedItem = item })
                }

                item { Divider() }

                item { Text("Verified Items", style = MaterialTheme.typography.headlineSmall) }
                if (state.verifiedItems.isEmpty()) {
                    item { Text("No items are currently verified.") }
                }
                items(state.verifiedItems) { item ->
                    VerifiedItemCard(
                        item = item,
                        onDeleteClick = { itemToDelete = item }
                    )
                }
            }
        }
    }

    selectedItem?.let { item ->
        when (item.status) {
            "open" -> ReviewItemDialog(
                item = item,
                isUpdating = isUpdating,
                onDismiss = { if (!isUpdating) selectedItem = null },
                onApprove = {
                    scope.launch {
                        isUpdating = true
                        viewModel.updateItemStatus(item.id, "verified").also { success ->
                            val message = if (success) "Item approved successfully" else "Failed to approve item"
                            snackbarHostState.showSnackbar(message)
                        }
                        isUpdating = false
                        selectedItem = null
                    }
                },
                onReject = {
                    scope.launch {
                        isUpdating = true
                        viewModel.updateItemStatus(item.id, "rejected").also { success ->
                            val message = if (success) "Item rejected successfully" else "Failed to reject item"
                            snackbarHostState.showSnackbar(message)
                        }
                        isUpdating = false
                        selectedItem = null
                    }
                }
            )
            "claim_pending" -> ClaimReviewDialog(
                item = item,
                isUpdating = isUpdating,
                onDismiss = { if (!isUpdating) selectedItem = null },
                onConfirm = {
                    scope.launch {
                        isUpdating = true
                        viewModel.confirmResolution(item.id).also { success ->
                            val message = if (success) "Claim confirmed and item resolved" else "Failed to confirm claim"
                            snackbarHostState.showSnackbar(message)
                        }
                        isUpdating = false
                        selectedItem = null
                    }
                },
                onDeny = {
                    scope.launch {
                        isUpdating = true
                        viewModel.denyClaim(item.id).also { success ->
                            val message = if (success) "Claim denied successfully" else "Failed to deny claim"
                            snackbarHostState.showSnackbar(message)
                        }
                        isUpdating = false
                        selectedItem = null
                    }
                }
            )
        }
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to permanently delete the item '${item.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.deleteItem(item.id).also { success ->
                                val message = if (success) "Item deleted successfully." else "Failed to delete item."
                                snackbarHostState.showSnackbar(message)
                            }
                            itemToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminItemCard(item: LostFoundItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick
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
                if(item.status == "claim_pending") {
                    Text("CLAIMED BY: ${item.claimedBy ?: "Unknown"}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun VerifiedItemCard(
    item: LostFoundItem,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            item.imageUrl?.let {
                AsyncImage(model = it, contentDescription = item.title, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text("Location: ${item.location}", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Item",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ReviewItemDialog(
    item: LostFoundItem,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.imageUrl?.let { AsyncImage(model = it, contentDescription = "Item image", modifier = Modifier.fillMaxWidth().height(150.dp), contentScale = ContentScale.Fit) }
                Text("Type: ${item.type.uppercase()}", fontWeight = FontWeight.Bold)
                Text("Description: ${item.description ?: "N/A"}")
                Text("Location: ${item.location}")
                Text("Posted By: ${item.postedBy}", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onApprove, enabled = !isUpdating, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("Approve")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onReject, enabled = !isUpdating) {
                Text("Reject")
            }
        }
    )
}

@Composable
fun ClaimReviewDialog(
    item: LostFoundItem,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Claim: ${item.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.imageUrl?.let { AsyncImage(model = it, contentDescription = "Item image", modifier = Modifier.fillMaxWidth().height(150.dp), contentScale = ContentScale.Fit) }
                Text("Item was posted by user: \n${item.postedBy}", style = MaterialTheme.typography.bodySmall)
                Divider(Modifier.padding(vertical = 4.dp))
                Text("Item is being claimed by user: \n${item.claimedBy}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isUpdating, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("Confirm Resolution")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDeny, enabled = !isUpdating) {
                Text("Deny Claim")
            }
        }
    )
}
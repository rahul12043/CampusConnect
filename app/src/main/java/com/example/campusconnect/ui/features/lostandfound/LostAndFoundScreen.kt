package com.example.campusconnect.ui.features.lostandfound

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.campusconnect.data.LostFoundItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostAndFoundScreen(
    onReportItemClick: () -> Unit,
    viewModel: LostFoundListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Add SnackbarHost
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onReportItemClick,
                text = { Text("Report Item") },
                icon = { /* Add an Icon if you want */ }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.items.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No lost or found items have been reported yet.")
                        }
                    }
                }

                items(state.items) { item ->
                    LostFoundItemCard(
                        item = item,
                        // Connect the claim button click to the ViewModel function
                        onClaimClicked = { itemId ->
                            viewModel.claimItem(itemId) { success, message ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LostFoundItemCard(
    item: LostFoundItem,
    onClaimClicked: (String) -> Unit // Add the callback function as a parameter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))
            }

            Text(item.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("Status: ${item.type.uppercase()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Location: ${item.location}")
            if (item.description != null) {
                Text("Description: ${item.description}")
            }

            // The button is only shown for items that have been verified by an admin
            if (item.status == "verified") {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onClaimClicked(item.id) }, // Call the callback with the item's ID
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("This is Mine! (Claim Item)")
                }
            }
        }
    }
}
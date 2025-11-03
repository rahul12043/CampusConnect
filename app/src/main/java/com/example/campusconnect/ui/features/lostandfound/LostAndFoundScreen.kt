package com.example.campusconnect.ui.features.lostandfound

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Correct import
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.campusconnect.data.LostFoundItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostAndFoundScreen(
    onReportItemClick: () -> Unit,
    // --- FIX: Use the new ViewModel for this screen ---
    viewModel: LostFoundListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onReportItemClick,
                text = { Text("Report Item") },
                icon = { /* Add an Icon if you want */ }
            )
        }
    ) { padding ->
        // --- FIX: Correctly check the isLoading state ---
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- FIX: Check if the list is empty with parentheses ---
                if (state.items.isEmpty()) {
                    item {
                        Text("No lost or found items have been reported yet.")
                    }
                }

                // --- FIX: Correctly iterate over items ---
                items(state.items) { item ->
                    LostFoundItemCard(item = item)
                }
            }
        }
    }
}

@Composable
fun LostFoundItemCard(item: LostFoundItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title, // --- FIX: Use item.title ---
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))
            }
            // --- FIX: Use item.title ---
            Text(item.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("Status: ${item.type.uppercase()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Location: ${item.location}")
            if (item.description != null) {
                Text("Description: ${item.description}")
            }
        }
    }
}
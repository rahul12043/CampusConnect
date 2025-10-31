package com.example.campusconnect.ui.features.lostandfound

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.campusconnect.data.LostFoundItem
import com.example.campusconnect.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostAndFoundScreen(
    navController: NavController,
    viewModel: LostAndFoundViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(Screen.ReportLostFoundItem.route)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Report Item")
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Text("Active Items", style = MaterialTheme.typography.headlineSmall) }
                if (state.items.isEmpty()) {
                    item { Text("No active lost or found items.") }
                }
                items(state.items) { item ->
                    StudentItemCard(item = item)
                }
            }
        }
    }
}

@Composable
fun StudentItemCard(item: LostFoundItem) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column {
            item.imageUrl?.let {
                AsyncImage(model = it, contentDescription = item.name, modifier = Modifier.height(180.dp).fillMaxWidth(), contentScale = ContentScale.Crop)
            }
            Column(Modifier.padding(12.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleLarge)
                // CHANGE: Handle optional description
                if (!item.description.isNullOrBlank()) {
                    Text(item.description)
                }
                Spacer(Modifier.height(8.dp))
                Text("Location: ${item.location}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
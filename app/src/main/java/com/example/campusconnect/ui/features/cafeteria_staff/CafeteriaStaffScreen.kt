package com.example.campusconnect.ui.features.cafeteria_staff

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.campusconnect.data.Order
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeteriaStaffScreen(onLogout: () -> Unit) {
    // This NavController is now local to the Cafeteria Staff's workflow
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cafeteria Orders") },
                actions = {
                    IconButton(onClick = {
                        Firebase.auth.signOut()
                        onLogout()
                    }) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout") }
                }
            )
        },
        // --- NEW: Floating Action Button to navigate to the "add item" screen ---
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_item") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Menu Item")
            }
        }
    ) { padding ->
        // --- NEW: NavHost to manage the staff's screens (Order List vs. Add Item Form) ---
        NavHost(
            navController = navController,
            startDestination = "order_list",
            modifier = Modifier.padding(padding)
        ) {
            composable("order_list") {
                // The original order list UI is now its own composable
                OrderListScreen()
            }
            composable("add_item") {
                // The new screen for adding items
                AddMenuItemScreen(onItemAdded = {
                    // Go back to the order list after an item is successfully added
                    navController.popBackStack()
                })
            }
        }
    }
}

// --- NEW: The original LazyColumn is extracted into its own composable ---
@Composable
fun OrderListScreen(viewModel: CafeteriaStaffViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("New Orders", style = MaterialTheme.typography.headlineSmall) }
        if (state.newOrders.isEmpty()) {
            item { Text("No new orders.") }
        }
        items(state.newOrders) { order ->
            OrderCard(
                order = order,
                actionText = "Start Preparing",
                onActionClick = { viewModel.updateOrderStatus(order.orderId, "PREPARING") }
            )
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text("Currently Preparing", style = MaterialTheme.typography.headlineSmall)
        }
        if (state.preparingOrders.isEmpty()) {
            item { Text("No orders are being prepared.") }
        }
        items(state.preparingOrders) { order ->
            OrderCard(
                order = order,
                actionText = "Ready for Pickup",
                onActionClick = { viewModel.updateOrderStatus(order.orderId, "READY_FOR_PICKUP") }
            )
        }
    }
}

// This composable remains unchanged, it's perfect as is.
@Composable
fun OrderCard(order: Order, actionText: String, onActionClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Order for: ${order.userName}", style = MaterialTheme.typography.titleLarge)
            Text("Total: ₹${order.totalPrice}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            order.items.forEach { Text("- $it") }
            Button(onClick = onActionClick, modifier = Modifier.padding(top = 8.dp)) {
                Text(actionText)
            }
        }
    }
}
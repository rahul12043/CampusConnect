package com.example.campusconnect.ui.features.cafeteria_staff

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.campusconnect.data.Order
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeteriaStaffScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val title = when (currentRoute) {
        "order_list" -> "Cafeteria Orders"
        "add_item" -> "Add New Menu Item"
        else -> "Cafeteria Panel"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    // Show back button only on the "add_item" screen
                    if (currentRoute == "add_item") {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Firebase.auth.signOut()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            // Only show the FAB on the order list screen
            if (currentRoute == "order_list") {
                FloatingActionButton(onClick = { navController.navigate("add_item") }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Menu Item")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "order_list",
            modifier = Modifier.padding(padding)
        ) {
            composable("order_list") {
                OrderListScreen() // Pass the shared ViewModel implicitly
            }
            composable("add_item") {
                // Pass the NavController so the screen can navigate itself back on success
                AddMenuItemScreen(navController = navController)
            }
        }
    }
}


@Composable
fun OrderListScreen(viewModel: CafeteriaStaffViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    if (state.isLoading && state.newOrders.isEmpty() && state.preparingOrders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
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
                Divider(modifier = Modifier.padding(vertical = 16.dp))
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
}


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
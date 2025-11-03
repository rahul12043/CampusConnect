package com.example.campusconnect.ui.features.digitalqueue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
// import androidx.lifecycle.viewmodel.compose.viewModel // <-- THIS IMPORT IS NO LONGER NEEDED
import coil.compose.AsyncImage
import com.example.campusconnect.auth.AuthViewModel
import com.example.campusconnect.data.MenuItem
import com.example.campusconnect.data.User // <-- You might need to import your data.User class

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeteriaMenuScreen(
    // --- FIX: The ViewModels are now passed in as parameters ---
    viewModel: CafeteriaViewModel,
    authViewModel: AuthViewModel
) {
    // The line creating the ViewModel is now gone. We use the one passed in.
    val state by viewModel.state.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.orderPlacedSuccessfully) {
        when (state.orderPlacedSuccessfully) {
            true -> {
                snackbarHostState.showSnackbar("Order placed successfully!")
                viewModel.resetOrderSuccessStatus()
            }
            false -> {
                snackbarHostState.showSnackbar("Failed to place order.")
                viewModel.resetOrderSuccessStatus()
            }
            null -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(visible = state.cart.isNotEmpty()) {
                CartBottomBar(
                    cart = state.cart,
                    // The 'placeOrder' call is now safer
                    onPlaceOrder = { currentUser?.let { user -> viewModel.placeOrder(user) } }
                )
            }
        }
    ) { paddingValues ->
        if (state.isLoading && state.menuItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.menuItems) { item ->
                    MenuItemCard(
                        menuItem = item,
                        cartCount = state.cart[item] ?: 0,
                        onAddToCart = { viewModel.addToCart(item) },
                        onRemoveFromCart = { viewModel.removeFromCart(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun MenuItemCard(
    menuItem: MenuItem,
    cartCount: Int,
    onAddToCart: () -> Unit,
    onRemoveFromCart: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = menuItem.imageUrl,
                contentDescription = menuItem.name,
                modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(menuItem.name, style = MaterialTheme.typography.titleLarge)
                Text(menuItem.description, style = MaterialTheme.typography.bodyMedium)
                Text("₹${"%.2f".format(menuItem.price)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
            }
            if (cartCount == 0) {
                Button(onClick = onAddToCart) { Text("Add") }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onRemoveFromCart, modifier = Modifier.size(32.dp)) { Text("-") }
                    Text("$cartCount", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onAddToCart, modifier = Modifier.size(32.dp)) { Text("+") }
                }
            }
        }
    }
}

@Composable
fun CartBottomBar(cart: Map<MenuItem, Int>, onPlaceOrder: () -> Unit) {
    val totalPrice = cart.entries.sumOf { (item, quantity) -> item.price * quantity }

    Surface(tonalElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Total", style = MaterialTheme.typography.bodyMedium)
                Text("₹${"%.2f".format(totalPrice)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onPlaceOrder) {
                Text("Place Order")
            }
        }
    }
}
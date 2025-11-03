package com.example.campusconnect.ui.features.digitalqueue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.campusconnect.auth.AuthViewModel
import com.example.campusconnect.data.MenuItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeteriaMenuScreen(
    viewModel: CafeteriaViewModel,
    authViewModel: AuthViewModel
) {
    val state by viewModel.state.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentSuccessful by remember { mutableStateOf(false) }

    // This will now show the toast AFTER the success overlay is dismissed and the order is placed.
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
                    onPlaceOrder = { showPaymentDialog = true }
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

        // 🧾 Payment Dialog Simulation
        if (showPaymentDialog) {
            MockPaymentDialog(
                onPaymentDone = {
                    // --- FIX 1: ---
                    // The ONLY thing we do when payment is done is show the success overlay.
                    // We DO NOT place the order yet.
                    showPaymentDialog = false
                    paymentSuccessful = true
                },
                onDismiss = { showPaymentDialog = false }
            )
        }

        // 🎉 Success Overlay
        if (paymentSuccessful) {
            PaymentSuccessOverlay(
                onDismiss = {
                    // --- FIX 2: ---
                    // NOW, after the user dismisses the success overlay, we place the order.
                    paymentSuccessful = false
                    currentUser?.let { user -> viewModel.placeOrder(user) }
                }
            )
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
                Text(
                    "₹${"%.2f".format(menuItem.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (cartCount == 0) {
                Button(onClick = onAddToCart) { Text("Add") }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onRemoveFromCart, modifier = Modifier.size(32.dp)) {
                        Text("-")
                    }
                    Text("$cartCount", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onAddToCart, modifier = Modifier.size(32.dp)) {
                        Text("+")
                    }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Total", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "₹${"%.2f".format(totalPrice)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(onClick = onPlaceOrder) {
                Text("Proceed to Pay")
            }
        }
    }
}

/* 🧾 Payment Simulation Dialog */
@Composable
fun MockPaymentDialog(onPaymentDone: () -> Unit, onDismiss: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        repeat(100) {
            delay(25)
            progress += 0.01f
        }
        delay(500)
        onPaymentDone()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Processing Payment") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(progress = progress)
                Spacer(Modifier.height(8.dp))
                Text("Please wait while we complete your payment...")
            }
        }
    )
}

/* 🎉 Success Overlay after Payment */
@Composable
fun PaymentSuccessOverlay(onDismiss: () -> Unit) {
    val scaleAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.spring(),
        label = ""
    )

    Box(
        Modifier.fillMaxSize()
            .background(Color(0xAA000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // No ripple effect for a background scrim
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .scale(scaleAnim)
                .padding(32.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✅ Payment Successful!", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Your order has been placed.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onDismiss) { Text("OK") }
            }
        }
    }
}

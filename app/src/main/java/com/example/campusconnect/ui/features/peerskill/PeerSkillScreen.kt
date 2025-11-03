package com.example.campusconnect.ui.features.peerskill

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.campusconnect.auth.AuthViewModel
import com.example.campusconnect.data.SkillRequest
import com.example.campusconnect.data.User
import com.example.campusconnect.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerSkillScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    skillViewModel: PeerSkillViewModel = viewModel()
) {
    val state by skillViewModel.state.collectAsState()
    val user by authViewModel.currentUser.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.CreateSkillRequest.route) }) {
                Icon(Icons.Default.Add, "Request Help")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                SkillRequestList(
                    requests = state.skillRequests,
                    currentUser = user,
                    viewModel = skillViewModel
                )
            }
        }
    }
}

@Composable
fun SkillRequestList(
    requests: List<SkillRequest>,
    currentUser: User?,
    viewModel: PeerSkillViewModel
) {
    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No one has requested help yet. Be the first!")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requests) { request ->
                SkillRequestCard(
                    request = request,
                    currentUser = currentUser,
                    onOfferHelp = {
                        // Pass the full User object to the ViewModel
                        currentUser?.let { user ->
                            viewModel.offerHelp(request, user)
                        }
                    },
                    onMarkResolved = { viewModel.markAsResolved(request) }
                )
            }
        }
    }
}

// --- THIS COMPOSABLE IS FULLY UPDATED ---
@Composable
fun SkillRequestCard(
    request: SkillRequest,
    currentUser: User?,
    onOfferHelp: () -> Unit,
    onMarkResolved: () -> Unit
) {
    val isOwnRequest = request.postedByUid == currentUser?.uid
    // The check is now cleaner and type-safe
    val hasAlreadyOffered = request.offers.any { it.helperUid == currentUser?.uid }
    val context = LocalContext.current

    Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(request.skillName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Requested by: ${request.postedByName} (${request.postedBySapId})", style = MaterialTheme.typography.bodySmall)
            Text("Preferred Time: ${request.preferredTimeSlots}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

            if (request.description.isNotBlank()) {
                Text(request.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(12.dp))

            if (isOwnRequest) {
                // This is your own post, show who has offered to help
                Text("Helpers who have offered:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (request.offers.isEmpty()) {
                    Text("No one has offered to help yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    // Iterate through the type-safe list of HelperOffer objects
                    request.offers.forEach { offer ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Direct property access is cleaner and safer
                            Text(offer.helperName)
                            Button(onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    // Direct property access here as well
                                    data = Uri.parse("mailto:${offer.helperContactEmail}")
                                }
                                context.startActivity(intent)
                            }) {
                                Text("Contact")
                            }
                        }
                    }
                }
                OutlinedButton(onClick = onMarkResolved, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Default.Done, "Mark as Resolved")
                    Spacer(Modifier.width(8.dp))
                    Text("Mark as Resolved")
                }

            } else {
                // This is someone else's post, show a button to offer help
                Button(
                    onClick = onOfferHelp,
                    enabled = !hasAlreadyOffered
                ) {
                    Text(if (hasAlreadyOffered) "You offered to help" else "I can help")
                }
            }
        }
    }
}
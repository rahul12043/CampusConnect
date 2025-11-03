package com.example.campusconnect.ui.features.admin

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val adminViewModel: AdminViewModel = viewModel()
    // This ViewModel is now required because you are uncommenting the routes that use it.
    val userManagementViewModel: UserManagementViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val title = when {
        currentRoute == "admin_home" -> "Admin Panel"
        currentRoute == "manage_faculty" -> "Manage Faculty"
        currentRoute == "add_faculty" -> "Add Faculty"
        currentRoute?.startsWith("edit_faculty/") == true -> "Edit Faculty"
        currentRoute == "manage_users" -> "Manage Users"
        currentRoute?.startsWith("edit_user/") == true -> "Edit User"
        currentRoute == "manage_announcements" -> "Manage Announcements"
        currentRoute == "add_announcement" -> "Add Announcement"
        currentRoute?.startsWith("edit_announcement/") == true -> "Edit Announcement"
        else -> "Admin Panel"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    if (currentRoute != "admin_home") {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Firebase.auth.signOut()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
                    }
                }
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "admin_home",
            modifier = Modifier.padding(padding)
        ) {
            composable("admin_home") { AdminHome(navController = navController) }
            composable("manage_faculty") {
                ManageFacultyScreen(navController = navController, viewModel = adminViewModel)
            }
            composable("add_faculty") {
                AddFacultyScreen(
                    navController = navController,
                    viewModel = adminViewModel
                )
            }
            composable("edit_faculty/{facultyId}", listOf(navArgument("facultyId") { type = NavType.StringType })) {
                val facultyId = it.arguments?.getString("facultyId")
                if (facultyId != null) {
                    EditFacultyScreen(navController, facultyId, adminViewModel)
                }
            }

            // --- THIS IS THE FIX: UNCOMMENT THESE ROUTES ---
            composable("manage_users") {
                UserManagementScreen(
                    navController = navController,
                    viewModel = userManagementViewModel
                )
            }
            composable("edit_user/{userId}", listOf(navArgument("userId") { type = NavType.StringType })) {
                val userId = it.arguments?.getString("userId")
                if (userId != null) {
                    EditUserScreen(
                        navController = navController,
                        userId = userId,
                        viewModel = userManagementViewModel
                    )
                }
            }
            // --- END OF FIX ---

            composable("manage_announcements") {
                ManageAnnouncementsScreen(navController, adminViewModel)
            }
            composable("add_announcement") {
                AddAnnouncementScreen(
                    navController = navController,
                    viewModel = adminViewModel
                )
            }
            composable("edit_announcement/{announcementId}", listOf(navArgument("announcementId") { type = NavType.StringType })) {
                val announcementId = it.arguments?.getString("announcementId")
                if (announcementId != null) {
                    EditAnnouncementScreen(navController, announcementId, adminViewModel)
                }
            }
        }
    }
}


@Composable
fun AdminHome(navController: NavController) {
    Column(modifier = Modifier.padding(16.dp)) {
        AdminFeatureCard(
            feature = AdminFeature("Manage Faculty", Icons.Default.Person),
            onClick = { navController.navigate("manage_faculty") }
        )
        Spacer(Modifier.height(16.dp))
        AdminFeatureCard(
            feature = AdminFeature("Manage Announcements", Icons.Default.Campaign),
            onClick = { navController.navigate("manage_announcements") }
        )
        Spacer(Modifier.height(16.dp))
        AdminFeatureCard(
            feature = AdminFeature("Manage Users", Icons.Default.ManageAccounts),
            onClick = { navController.navigate("manage_users") }
        )
    }
}

@Composable
fun AdminFeatureCard(feature: AdminFeature, onClick: () -> Unit) {
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
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(feature.icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Text(feature.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
    }
}

data class AdminFeature(val name: String, val icon: ImageVector)
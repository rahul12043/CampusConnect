package com.example.campusconnect.ui.features.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import com.example.campusconnect.ui.theme.CampusConnectTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onLogout: () -> Unit) {
    CampusConnectTheme {
        val navController = rememberNavController()
        val adminViewModel: AdminViewModel = viewModel()
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
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
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
                        onFacultyAdded = { navController.popBackStack() },
                        viewModel = adminViewModel
                    )
                }
                composable(
                    route = "edit_faculty/{facultyId}",
                    arguments = listOf(navArgument("facultyId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val facultyId = backStackEntry.arguments?.getString("facultyId")
                    if (facultyId != null) {
                        EditFacultyScreen(
                            navController = navController,
                            facultyId = facultyId,
                            viewModel = adminViewModel
                        )
                    }
                }
                composable("manage_users") {
                    UserManagementScreen(
                        navController = navController,
                        viewModel = userManagementViewModel
                    )
                }
                composable(
                    route = "edit_user/{userId}",
                    arguments = listOf(navArgument("userId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId")
                    if (userId != null) {
                        EditUserScreen(
                            navController = navController,
                            userId = userId,
                            viewModel = userManagementViewModel
                        )
                    }
                }
                composable("manage_announcements") {
                    ManageAnnouncementsScreen(
                        navController = navController,
                        viewModel = adminViewModel
                    )
                }
                composable("add_announcement") {
                    AddAnnouncementScreen(
                        navController = navController,
                        onAnnouncementAdded = { navController.popBackStack() },
                        viewModel = adminViewModel
                    )
                }
                composable(
                    route = "edit_announcement/{announcementId}",
                    arguments = listOf(navArgument("announcementId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val announcementId = backStackEntry.arguments?.getString("announcementId")
                    if (announcementId != null) {
                        EditAnnouncementScreen(
                            navController = navController,
                            announcementId = announcementId,
                            viewModel = adminViewModel
                        )
                    }
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
            .clickable(onClick = onClick),
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
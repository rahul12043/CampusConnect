package com.example.campusconnect.ui.features

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
// Explicit imports for clarity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.example.campusconnect.auth.AuthViewModel
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import com.example.campusconnect.ui.features.note_sharing.CreateNoteScreen
import com.example.campusconnect.ui.features.note_sharing.NoteSharingScreen
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.campusconnect.navigation.Screen
import com.example.campusconnect.ui.features.digitalqueue.CafeteriaMenuScreen
import com.example.campusconnect.ui.features.digitalqueue.CafeteriaViewModel
import com.example.campusconnect.ui.features.faculty_connect.FacultyDetailScreen
import com.example.campusconnect.ui.features.faculty_connect.FacultyListScreen
import com.example.campusconnect.ui.features.faculty_connect.FacultyViewModel
import com.example.campusconnect.ui.features.lostandfound.LostAndFoundScreen
import com.example.campusconnect.ui.features.lostandfound.LostFoundListViewModel
import com.example.campusconnect.ui.features.lostandfound.ReportItemScreen
import com.example.campusconnect.ui.features.note_sharing.NoteSharingViewModel
import com.example.campusconnect.ui.features.peerskill.CreateSkillRequestScreen
import com.example.campusconnect.ui.features.peerskill.PeerSkillScreen
import com.example.campusconnect.ui.features.peerskill.PeerSkillViewModel
import com.example.campusconnect.ui.features.utility.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val authViewModel: AuthViewModel = viewModel()

    val title = when {
        currentRoute == Screen.Home.route -> "Campus Connect+"
        currentRoute == Screen.DigitalQueue.route -> "Cafeteria Menu"
        currentRoute == Screen.LostAndFound.route -> "Lost & Found"
        currentRoute == Screen.ReportLostFoundItem.route -> "Report an Item"
        currentRoute == Screen.NoteSharing.route -> "Peer Help"
        currentRoute == Screen.PeerSkill.route -> "PeerSkill Hub"
        currentRoute == Screen.CreateSkillRequest.route -> "Request Help"
        currentRoute == Screen.IdeaIncubator.route -> "Idea Incubator"
        currentRoute == Screen.FacultyConnect.route -> "Faculty Directory"
        currentRoute == Screen.FlashcardGenerator.route -> "AI Flashcard Generator"
        currentRoute?.startsWith("faculty_detail/") == true -> "Faculty Details"
        else -> "Campus Connect+"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    if (currentRoute != Screen.Home.route) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Firebase.auth.signOut()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController = navController) }

            // --- THIS BLOCK IS NOW CORRECT ---
            composable(Screen.DigitalQueue.route) {
                val cafeteriaViewModel: CafeteriaViewModel = viewModel() // Hoisted
                CafeteriaMenuScreen(
                    viewModel = cafeteriaViewModel,
                    authViewModel = authViewModel // Correctly passing the parameter
                )
            }

            composable(Screen.LostAndFound.route) {
                val lostFoundListViewModel: LostFoundListViewModel = viewModel() // Hoisted
                LostAndFoundScreen(
                    onReportItemClick = { navController.navigate(Screen.ReportLostFoundItem.route) },
                    viewModel = lostFoundListViewModel
                )
            }
            composable(Screen.ReportLostFoundItem.route) {
                ReportItemScreen(onItemReported = { navController.popBackStack() })
            }

            composable(Screen.NoteSharing.route) {
                val noteSharingViewModel: NoteSharingViewModel = viewModel() // Hoisted
                NoteSharingScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    noteViewModel = noteSharingViewModel
                )
            }
            composable(Screen.CreateNote.route) {
                CreateNoteScreen(navController = navController, authViewModel = authViewModel)
            }

            composable(Screen.PeerSkill.route) {
                val peerSkillViewModel: PeerSkillViewModel = viewModel() // Hoisted
                PeerSkillScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    skillViewModel = peerSkillViewModel
                )
            }
            composable(Screen.CreateSkillRequest.route) {
                val peerSkillViewModel: PeerSkillViewModel = viewModel(
                    navController.getBackStackEntry(Screen.PeerSkill.route)
                )
                CreateSkillRequestScreen(
                    onSuccess = { navController.popBackStack() },
                    viewModel = peerSkillViewModel
                )
            }

            composable(Screen.FacultyConnect.route) {
                val facultyViewModel: FacultyViewModel = viewModel() // Hoisted
                FacultyListScreen(navController = navController, viewModel = facultyViewModel)
            }
            composable(
                route = Screen.FacultyDetail.route,
                arguments = listOf(navArgument("facultyId") { type = NavType.StringType })
            ) { backStackEntry ->
                val facultyId = backStackEntry.arguments?.getString("facultyId")
                if (facultyId != null) {
                    FacultyDetailScreen(facultyId = facultyId)
                }
            }

            composable(Screen.MindMingle.route) { MindMingleScreen(navController) }
            composable(Screen.IdeaIncubator.route) { IdeaIncubatorScreen(navController) }
        }
    }
}
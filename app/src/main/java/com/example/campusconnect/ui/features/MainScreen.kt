package com.example.campusconnect.ui.features

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.campusconnect.auth.AuthViewModel
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import com.example.campusconnect.ui.features.note_sharing.CreateNoteScreen
import com.example.campusconnect.ui.features.note_sharing.NoteSharingScreen
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.example.campusconnect.navigation.Screen
import com.example.campusconnect.data.Flashcard
import com.example.campusconnect.ui.features.digitalqueue.CafeteriaMenuScreen
import com.example.campusconnect.ui.features.faculty_connect.FacultyDetailScreen
import com.example.campusconnect.ui.features.faculty_connect.FacultyListScreen
import com.example.campusconnect.ui.features.lostandfound.LostAndFoundScreen
import com.example.campusconnect.ui.features.lostandfound.ReportItemScreen
import com.example.campusconnect.ui.features.note_sharing.CreateNoteScreen
import com.example.campusconnect.ui.features.note_sharing.NoteSharingScreen
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
    var generatedFlashcards by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    // --- FIX: Updated the title logic to handle the new routes ---
    val title = when {
        currentRoute == Screen.Home.route -> "Campus Connect+"
        currentRoute == Screen.DigitalQueue.route -> "Cafeteria Menu"
        currentRoute == Screen.LostAndFound.route -> "Lost & Found"
        currentRoute == Screen.ReportLostFoundItem.route -> "Report an Item"
        currentRoute == Screen.NoteSharing.route -> "Peer Help"
        currentRoute == Screen.PeerSkill.route -> "PeerSkill Hub"
        currentRoute == Screen.IdeaIncubator.route -> "Idea Incubator"
        currentRoute == Screen.FacultyConnect.route -> "Faculty Directory"
        currentRoute == Screen.FlashcardGenerator.route -> "AI Flashcard Generator"
        currentRoute == Screen.FlashcardViewer.route -> "Review Flashcards"

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
            composable(Screen.DigitalQueue.route) { CafeteriaMenuScreen() }
            composable(Screen.LostAndFound.route) { LostAndFoundScreen(navController = navController) }
            composable(Screen.ReportLostFoundItem.route) {
                ReportItemScreen(onItemReported = { navController.popBackStack() })
            }
            composable(Screen.MindMingle.route) { MindMingleScreen(navController) }
            composable(Screen.PeerSkill.route) { PeerSkillScreen(navController) }
            composable(Screen.IdeaIncubator.route) { IdeaIncubatorScreen(navController) }

            // --- FIX: Replaced the old FacultyConnect route with the TWO new ones ---
            composable(Screen.FacultyConnect.route) {
                FacultyListScreen(navController = navController)
            }
            composable(
                route = Screen.FacultyDetail.route,
                arguments = listOf(navArgument("facultyId") { type = NavType.StringType })
            ) { backStackEntry ->
                val facultyId = backStackEntry.arguments?.getString("facultyId")
                // A null check is good practice to prevent crashes
                if (facultyId != null) {
                    FacultyDetailScreen(facultyId = facultyId)
                }
            }
            composable(Screen.NoteSharing.route) {
                NoteSharingScreen(navController = navController, authViewModel = authViewModel)
            }
            composable(Screen.CreateNote.route) {
                CreateNoteScreen(navController = navController, authViewModel = authViewModel)
            }
        }
    }
}
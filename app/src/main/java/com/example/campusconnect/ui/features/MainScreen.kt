package com.example.campusconnect.ui.features

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.campusconnect.auth.AuthViewModel
import com.example.campusconnect.navigation.Screen
import com.example.campusconnect.ui.features.digitalqueue.CafeteriaMenuScreen
import com.example.campusconnect.ui.features.digitalqueue.CafeteriaViewModel
import com.example.campusconnect.ui.features.faculty_connect.FacultyDetailScreen
import com.example.campusconnect.ui.features.faculty_connect.FacultyListScreen
import com.example.campusconnect.ui.features.faculty_connect.FacultyViewModel
import com.example.campusconnect.ui.features.lostandfound.LostAndFoundScreen
import com.example.campusconnect.ui.features.lostandfound.LostFoundListViewModel
import com.example.campusconnect.ui.features.lostandfound.ReportItemScreen
import com.example.campusconnect.ui.features.note_sharing.CreateNoteScreen
import com.example.campusconnect.ui.features.note_sharing.NoteSharingScreen
import com.example.campusconnect.ui.features.note_sharing.NoteSharingViewModel
import com.example.campusconnect.ui.features.peerskill.CreateSkillRequestScreen
import com.example.campusconnect.ui.features.peerskill.PeerSkillScreen
import com.example.campusconnect.ui.features.peerskill.PeerSkillViewModel
import com.example.campusconnect.ui.features.aiflashcard.AiFlashcardScreen
import com.example.campusconnect.ui.features.aiflashcard.AiFromFileScreen
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val authViewModel: AuthViewModel = viewModel()

    val title = when {
        currentRoute == Screen.Home.route -> "NM360"
        currentRoute == Screen.DigitalQueue.route -> "Cafeteria Menu"
        currentRoute == Screen.LostAndFound.route -> "Lost & Found"
        currentRoute == Screen.ReportLostFoundItem.route -> "Report an Item"
        currentRoute == Screen.NoteSharing.route -> "Peer Help"
        currentRoute == Screen.PeerSkill.route -> "PeerSkill Hub"
        currentRoute == Screen.CreateSkillRequest.route -> "Request Help"
        currentRoute == Screen.FacultyConnect.route -> "Faculty Directory"
        currentRoute == Screen.FlashcardGenerator.route -> "AI Flashcard Generator" // Title for the first AI screen
        currentRoute == Screen.AiFromFile.route -> "AI From Document"
        currentRoute?.startsWith("faculty_detail/") == true -> "Faculty Details"
        else -> "Campus Connect+"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (currentRoute == Screen.Home.route) {
                        AnimatedHomeTitle()
                    } else {
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    if (currentRoute != Screen.Home.route) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onPrimary
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
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
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

            composable(Screen.DigitalQueue.route) {
                val cafeteriaViewModel: CafeteriaViewModel = viewModel()
                CafeteriaMenuScreen(viewModel = cafeteriaViewModel, authViewModel = authViewModel)
            }

            composable(Screen.LostAndFound.route) {
                val lostFoundListViewModel: LostFoundListViewModel = viewModel()
                LostAndFoundScreen(
                    onReportItemClick = { navController.navigate(Screen.ReportLostFoundItem.route) },
                    viewModel = lostFoundListViewModel
                )
            }
            composable(Screen.ReportLostFoundItem.route) {
                ReportItemScreen(onItemReported = { navController.popBackStack() })
            }

            composable(Screen.NoteSharing.route) {
                val noteSharingViewModel: NoteSharingViewModel = viewModel()
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
                val peerSkillViewModel: PeerSkillViewModel = viewModel()
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
                val facultyViewModel: FacultyViewModel = viewModel()
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

            composable(Screen.FlashcardGenerator.route) {
                AiFlashcardScreen() // The ViewModel is created internally
            }
            composable(Screen.AiFromFile.route) {
                AiFromFileScreen()
            }
        }
    }
}

@Composable
fun AnimatedHomeTitle() {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(800)),
        exit = fadeOut()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Campus",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            )
            Text(
                text = " Connect",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )

            )
        }
    }
}

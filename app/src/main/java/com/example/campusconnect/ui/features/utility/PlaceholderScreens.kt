package com.example.campusconnect.ui.features.utility

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(title: String, navController: NavController) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }
        )
    }) {
        Box(modifier = Modifier.fillMaxSize().padding(it), contentAlignment = Alignment.Center) {
            Text("$title Feature Coming Soon")
        }
    }
}

// Update all placeholder screens to use the new template
@Composable fun MindMingleScreen(navController: NavController) = PlaceholderScreen("Mind Mingle", navController)
@Composable fun PeerSkillScreen(navController: NavController) = PlaceholderScreen("PeerSkill Hub", navController)
@Composable fun IdeaIncubatorScreen(navController: NavController) = PlaceholderScreen("Idea Incubator", navController)
@Composable fun FacultyConnectScreen(navController: NavController) = PlaceholderScreen("Faculty Connect", navController)
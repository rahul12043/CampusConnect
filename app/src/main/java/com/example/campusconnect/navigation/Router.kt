package com.example.campusconnect.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.campusconnect.auth.AuthViewModel
import com.example.campusconnect.auth.AuthState

@Composable
fun Router(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val user by authViewModel.currentUser.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(user, authState) {
        if (authState is AuthState.Authenticated && user != null) {
            val destination = when (user?.role) {
                "student" -> "student_dashboard"
                "lost_and_found_admin" -> "lostandfound_admin_dashboard"
                "cafeteria_staff" -> "cafeteria_staff_dashboard"
                "admin"->"admin_dashboard"
                else -> Screen.Login.route
            }
            navController.navigate(destination) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
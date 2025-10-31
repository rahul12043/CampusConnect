package com.example.campusconnect.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.campusconnect.auth.LoginScreen
import com.example.campusconnect.auth.RegisterScreen
import com.example.campusconnect.ui.features.MainScreen
import com.example.campusconnect.ui.features.cafeteria_staff.CafeteriaStaffScreen
import com.example.campusconnect.ui.features.lostandfound_admin.LostAndFoundAdminScreen
import com.example.campusconnect.ui.features.admin.AdminDashboardScreen
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("router") {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("router") {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("router") {
            Router(navController = navController)
        }

        composable("student_dashboard") {
            MainScreen(onLogout = {
                navController.navigate(Screen.Login.route) {
                    popUpTo("student_dashboard") { inclusive = true }
                }
            })
        }

        composable("lostandfound_admin_dashboard") {
            LostAndFoundAdminScreen(onLogout = {
                navController.navigate(Screen.Login.route) {
                    popUpTo("lostandfound_admin_dashboard") { inclusive = true }
                }
            })
        }
        composable("cafeteria_staff_dashboard") {
            CafeteriaStaffScreen(onLogout = {
                navController.navigate(Screen.Login.route) {
                    popUpTo("cafeteria_staff_dashboard") { inclusive = true }
                }
            })
        }
        composable("admin_dashboard") {
            AdminDashboardScreen(onLogout = {
                navController.navigate(Screen.Login.route) {
                    popUpTo("admin_dashboard") { inclusive = true }
                }
            })
        }
    }
}
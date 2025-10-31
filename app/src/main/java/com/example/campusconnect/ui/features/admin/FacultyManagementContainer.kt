package com.example.campusconnect.ui.features.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FacultyManagementContainer(
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel()
) {
    // This state drives the animation.
    // null = show list, a faculty ID = show details for that ID.
    var selectedFacultyId by remember { mutableStateOf<String?>(null) }

    // The SharedTransitionLayout wraps the entire animated feature.
    SharedTransitionLayout {
        // AnimatedContent switches between the list and detail views.
        AnimatedContent(
            targetState = selectedFacultyId,
            label = "faculty_transition"
        ) { targetId ->
            if (targetId == null) {
                // State: Show the list of faculty members.
                ManageFacultyScreen(
                    viewModel = adminViewModel,
                    onFacultyClick = { facultyId ->
                        selectedFacultyId = facultyId // Change state to show details
                    },
                    onAddClick = { navController.navigate("add_faculty") },
                    onBack = { navController.popBackStack() }, // Go back from the entire feature
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent
                )
            } else {
                // State: Show the details for the selected faculty member.
                EditFacultyScreen(
                    facultyId = targetId,
                    viewModel = adminViewModel,
                    onBack = {
                        selectedFacultyId = null // Change state back to show the list
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent
                )
            }
        }
    }
}
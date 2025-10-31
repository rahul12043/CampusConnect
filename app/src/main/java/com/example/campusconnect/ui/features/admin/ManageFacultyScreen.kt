package com.example.campusconnect.ui.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campusconnect.ui.features.faculty_connect.FacultyCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageFacultyScreen(
    navController: NavController,
    viewModel: AdminViewModel // Receives the shared ViewModel
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_faculty") }) {
                Icon(Icons.Default.Add, "Add Faculty")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(2.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.facultyList) { faculty ->
                    FacultyCard(
                        facultyMember = faculty,
                        onClick = { navController.navigate("edit_faculty/${faculty.id}") }
                    )
                }
            }

        }
    }
}
package com.example.campusconnect.ui.features.faculty_connect

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.campusconnect.data.FacultyMember

@Composable
fun FacultyDetailScreen(
    facultyId: String,
    viewModel: FacultyViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val facultyMember = state.facultyList.find { it.id == facultyId }

    if (facultyMember == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Faculty member not found.")
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = facultyMember.imageUrl,
                contentDescription = "Photo of ${facultyMember.name}",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(facultyMember.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(facultyMember.department, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Office: ${facultyMember.officeLocation}", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(24.dp))

            TimetableSection(timetable = facultyMember.timetable)

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { /* TODO: Implement meeting request logic */ }) {
                Text("Request Meeting")
            }
        }
    }
}

@Composable
fun TimetableSection(timetable: Map<String, String>) {
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Weekly Timetable", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

        daysOfWeek.forEach { day ->
            val availability = timetable[day] ?: "Not specified"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(day, fontWeight = FontWeight.Bold)
                Text(availability)
            }
            HorizontalDivider()
        }
    }
}
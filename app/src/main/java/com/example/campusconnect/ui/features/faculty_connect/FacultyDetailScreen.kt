package com.example.campusconnect.ui.features.faculty_connect

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    // Get the current context, which is needed to launch the email intent
    val context = LocalContext.current
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

            // --- NEW: DISPLAY EMAIL ADDRESS ---
            Text("Office: ${facultyMember.officeLocation}", style = MaterialTheme.typography.bodyLarge)
            Text("Email: ${facultyMember.email}", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(24.dp))

            // --- NEW: CONTACT FACULTY BUTTON ---
            Button(
                onClick = {
                    // Create an Intent to send an email
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:") // Only email apps should handle this
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(facultyMember.email))
                        putExtra(Intent.EXTRA_SUBJECT, "Inquiry from CampusConnect App")
                    }
                    // Create a chooser to let the user pick their email app
                    val chooser = Intent.createChooser(emailIntent, "Send Email To ${facultyMember.name}")
                    context.startActivity(chooser)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email Icon",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Contact Faculty")
            }

            Spacer(modifier = Modifier.height(24.dp))

            TimetableSection(timetable = facultyMember.timetable)
        }
    }
}

@Composable
fun TimetableSection(timetable: Map<String, String>) {
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Weekly Availability", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

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
            // Use Divider instead of HorizontalDivider for Material3
            Divider()
        }
    }
}
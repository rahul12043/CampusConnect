package com.example.campusconnect.ui.features.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import androidx.compose.foundation.LocalIndication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFacultyScreen(
    navController: NavController,
    viewModel: AdminViewModel
) {
    var name by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var officeLocation by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") } // <-- ADDED STATE FOR EMAIL
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    // The timetable can be empty by default
    var timetable by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> imageUri = uri }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            // Image Picker Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = { imagePickerLauncher.launch("image/*") }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Selected Faculty Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Tap to select an image")
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = officeLocation, onValueChange = { officeLocation = it }, label = { Text("Office Location") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            // --- ADDED EMAIL TEXT FIELD ---
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Contact Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(Modifier.height(24.dp))

            if (state.isUploading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = {
                        val localImageUri = imageUri
                        if (name.isNotBlank() && department.isNotBlank() && email.isNotBlank() && localImageUri != null) {
                            // --- THIS IS THE CORRECTED FUNCTION CALL ---
                            viewModel.addFacultyMember(
                                name = name,
                                department = department,
                                officeLocation = officeLocation,
                                email = email, // Pass the new email state
                                timetable = timetable, // Pass the (potentially empty) timetable
                                imageUri = localImageUri // Pass the image URI
                            ) { success ->
                                if (success) {
                                    navController.popBackStack()
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Failed to add faculty member.") }
                                }
                            }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Please fill all fields and select an image.") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Add Faculty Member")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
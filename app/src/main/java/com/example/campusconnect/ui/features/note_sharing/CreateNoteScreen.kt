package com.example.campusconnect.ui.features.note_sharing

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.campusconnect.auth.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoteScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    noteViewModel: NoteSharingViewModel = viewModel()
) {

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("No file selected") }

    val user by authViewModel.currentUser.collectAsState()
    val state by noteViewModel.state.collectAsState()
    val context = LocalContext.current
    val contentResolver = context.contentResolver // Get the content resolver
    val scope = rememberCoroutineScope()
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Simplified file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            fileUri = uri
            // Get the filename directly from the Uri
            fileName = uri?.let { getFileName(it, contentResolver) } ?: "No file selected"
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Share a New Note") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title*") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (Optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it; isDropdownExpanded = true },
                    label = { Text("Subject* (Type new or select existing)") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) }
                )

                val filteredSubjects = state.subjects.filter { it.name.contains(subject, ignoreCase = true) }
                if (filteredSubjects.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        filteredSubjects.forEach { subjectOption ->
                            DropdownMenuItem(
                                text = { Text(subjectOption.name) },
                                onClick = {
                                    subject = subjectOption.name
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = { filePickerLauncher.launch(arrayOf("application/pdf", "image/*", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) }) {
                Text(if (fileUri == null) "Select File*" else "Change File")
            }
            Text(fileName, style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(24.dp))

            if (state.isUploading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        val currentUser = user
                        val currentFileUri = fileUri
                        if (title.isBlank() || subject.isBlank() || currentUser == null || currentFileUri == null) {
                            scope.launch { snackbarHostState.showSnackbar("Please fill all required (*) fields.") }
                            return@Button
                        }

                        // --- THIS IS THE FIX ---
                        // 1. Get the MIME type directly.
                        val fileType = contentResolver.getType(currentFileUri) ?: "application/octet-stream"

                        // 2. Call the ViewModel with all required parameters.
                        noteViewModel.uploadNote(
                            user = currentUser,
                            title = title,
                            description = description.takeIf { it.isNotBlank() },
                            subject = subject,
                            fileUri = currentFileUri,
                            fileType = fileType,
                            contentResolver = contentResolver, // Pass the contentResolver
                            onComplete = { success ->
                                if (success) {
                                    navController.popBackStack()
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Upload failed. Please try again.") }
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Share Note")
                }
            }
        }
    }
}

// Helper function to get the original filename from a content Uri
private fun getFileName(uri: Uri, contentResolver: ContentResolver): String {
    var name = "File selected"
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }
    }
    return name
}
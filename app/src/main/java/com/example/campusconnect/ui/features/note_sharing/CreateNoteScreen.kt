package com.example.campusconnect.ui.features.note_sharing

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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
    var localFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("No file selected") }

    val user by authViewModel.currentUser.collectAsState()
    val state by noteViewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                scope.launch {
                    val tempFileUri = createTempFileFromUri(context, it)
                    if (tempFileUri != null) {
                        localFileUri = tempFileUri
                        fileName = it.pathSegments.lastOrNull()?.substringAfterLast('/') ?: "File selected"
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Could not access the selected file.")
                        }
                    }
                }
            }
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title*") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
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
                Text("Select File*")
            }
            Text(fileName, style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(24.dp))

            if (state.isUploading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        val currentUser = user
                        val currentFileUri = localFileUri
                        if (title.isBlank() || subject.isBlank() || currentUser == null || currentFileUri == null) {
                            scope.launch { snackbarHostState.showSnackbar("Please fill all required (*) fields.") }
                            return@Button
                        }

                        val fileType = getMimeType(context, currentFileUri) ?: "file"

                        noteViewModel.uploadNote(
                            user = currentUser,
                            title = title,
                            description = description.takeIf { it.isNotBlank() },
                            subject = subject,
                            fileUri = currentFileUri,
                            fileType = fileType
                        ) { success ->
                            if (success) {
                                navController.popBackStack()
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Upload failed. Please check your connection and try again.") }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Share Note")
                }
            }
        }
    }
}

private suspend fun createTempFileFromUri(context: Context, uri: Uri): Uri? {
    return withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("upload_", ".tmp", context.cacheDir)
            outputStream = FileOutputStream(tempFile)

            inputStream?.copyTo(outputStream)

            Uri.fromFile(tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }
}

private fun getMimeType(context: Context, uri: Uri): String? {
    return context.contentResolver.getType(uri)?.let { mimeType ->
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    }
}
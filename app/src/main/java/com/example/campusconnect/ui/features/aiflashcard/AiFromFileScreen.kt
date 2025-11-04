package com.example.campusconnect.ui.features.aiflashcard

import android.net.Uri
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFromFileScreen(
    viewModel: AiFromFileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold{ padding ->
        // The main content area will change based on the UI state
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is AiFromFileUiState.Idle -> {
                    // Initial state: Show the file picker and prompt UI
                    IdleUI(
                        onProcessFile = { uri, mimeType, prompt ->
                            viewModel.processFile(context, uri, mimeType, prompt)
                        }
                    )
                }
                is AiFromFileUiState.Processing -> {
                    // Loading state
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(state.status, textAlign = TextAlign.Center)
                    }
                }
                is AiFromFileUiState.Success -> {
                    // Result state
                    ResultUI(
                        outputText = state.output,
                        onRestart = { viewModel.resetState() }
                    )
                }
                is AiFromFileUiState.Error -> {
                    // Error state
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetState() }) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}

// Composable for the initial screen state
@Composable
private fun IdleUI(onProcessFile: (uri: Uri, mimeType: String?, prompt: String) -> Unit) {
    val context = LocalContext.current
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileType by remember { mutableStateOf<String?>(null) }
    var userPrompt by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        selectedFileUri = uri
        selectedFileType = uri?.let { context.contentResolver.getType(it) }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("1. Select a file to analyze", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            // Launch the file picker for images AND PDFs
            filePickerLauncher.launch(arrayOf("image/*", "application/pdf"))
        }) {
            Text("Select Image or PDF")
        }

        if (selectedFileUri != null) {
            Spacer(Modifier.height(24.dp))
            Text(
                "File Selected: ${selectedFileUri?.path?.substringAfterLast('/')}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))

            Text("2. Tell the AI what to do", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = userPrompt,
                onValueChange = { userPrompt = it },
                label = { Text("e.g., 'Summarize this' or 'Create flashcards'") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onProcessFile(selectedFileUri!!, selectedFileType, userPrompt) },
                enabled = userPrompt.isNotBlank()
            ) {
                Text("Process Document")
            }
        }
    }
}

// Composable for displaying the final result
@Composable
private fun ResultUI(outputText: String, onRestart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("AI Generated Result", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(outputText, modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRestart) {
            Text("Analyze Another Document")
        }
    }
}
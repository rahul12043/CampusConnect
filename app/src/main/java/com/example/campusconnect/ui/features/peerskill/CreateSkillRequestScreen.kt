package com.example.campusconnect.ui.features.peerskill
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSkillRequestScreen(
    onSuccess: () -> Unit,
    viewModel: PeerSkillViewModel = viewModel()
) {
    var skillName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var preferredTimes by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold{ padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("What skill do you need help with?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = skillName,
                onValueChange = { skillName = it },
                label = { Text("Skill Name (e.g., Python, Canva)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Describe what you need help with") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = preferredTimes,
                onValueChange = { preferredTimes = it },
                label = { Text("Preferred Time Slots (e.g., Weekends)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = {
                        if (skillName.isNotBlank() && description.isNotBlank()) {
                            isLoading = true
                            viewModel.createSkillRequest(skillName, description, preferredTimes) { success ->
                                if (success) {
                                    onSuccess()
                                } else {
                                    isLoading = false
                                    scope.launch { snackbarHostState.showSnackbar("Failed to post request.") }
                                }
                            }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Skill name and description are required.") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Post Request")
                }
            }
        }
    }
}
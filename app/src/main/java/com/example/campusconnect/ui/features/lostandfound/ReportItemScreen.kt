package com.example.campusconnect.ui.features.lostandfound

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication // <-- ADD THIS IMPORT
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource // <-- ADD THIS IMPORT
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportItemScreen(
    onItemReported: () -> Unit,
    viewModel: ReportItemViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val state by viewModel.state.collectAsState()

    val itemTypes = listOf("lost", "found")
    var selectedType by remember { mutableStateOf(itemTypes[0]) }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> imageUri = uri }
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Fields marked with * are required.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))

            Text("What is the item type?*", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth()) {
                itemTypes.forEach { type ->
                    // The Row is now the single source of truth for selection clicks
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = (type == selectedType),
                                onClick = { selectedType = type },
                                role = Role.RadioButton, // Important for accessibility
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = true) // A bounded ripple is perfect for radio buttons
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (type == selectedType),
                            // Set onClick to null here because the parent Row now handles the click
                            onClick = null
                        )
                        Text(
                            text = type.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item Title*") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (Optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Last Seen Location*") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

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
                    Image(painter = rememberAsyncImagePainter(imageUri), contentDescription = "Selected Image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text("Tap to select an image (Optional)")
                }
            }
            // --- END OF FIX ---

            Spacer(Modifier.height(24.dp))

            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = {
                        if (title.isNotBlank() && location.isNotBlank()) {
                            viewModel.reportItem(
                                type = selectedType,
                                title = title,
                                description = description.takeIf { it.isNotBlank() },
                                location = location,
                                imageUri = imageUri
                            ) { success ->
                                if (success) {
                                    onItemReported()
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to submit report. Please try again.")
                                    }
                                }
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please fill out all required (*) fields.")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Submit Report")
                }
            }
        }
    }
}
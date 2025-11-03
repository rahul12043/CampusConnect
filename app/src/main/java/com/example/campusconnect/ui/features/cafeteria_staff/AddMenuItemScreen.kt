package com.example.campusconnect.ui.features.cafeteria_staff

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMenuItemScreen(
    onItemAdded: () -> Unit,
    viewModel: CafeteriaStaffViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val state by viewModel.state.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> imageUri = uri }
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add New Menu Item") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (e.g., 60.0)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))        .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = { imagePickerLauncher.launch("image/*") }
                ),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Image(painter = rememberAsyncImagePainter(imageUri), "Selected Image", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text("Tap to select an image")
                }
            }
            Spacer(Modifier.height(24.dp))

            if (state.isUploading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = {
                        // --- THIS IS THE FIX ---
                        // Create a local, non-state variable to enable smart casting.
                        val localImageUri = imageUri
                        val priceDouble = price.toDoubleOrNull()

                        if (name.isNotBlank() && description.isNotBlank() && priceDouble != null && localImageUri != null) {
                            viewModel.addMenuItem(name, description, priceDouble, localImageUri) { success ->
                                if (success) {
                                    onItemAdded()
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Failed to add item.") }
                                }
                            }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Please fill all fields and select an image.") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Add Item to Menu")
                }
            }
        }
    }
}
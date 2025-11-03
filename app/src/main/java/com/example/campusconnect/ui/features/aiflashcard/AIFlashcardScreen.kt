package com.example.campusconnect.ui.features.aiflashcard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFlashcardScreen(
    viewModel: AiFlashcardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            // The UI will change based on the state from the ViewModel
            when (val state = uiState) {
                is AiFlashcardUiState.Idle -> IdleScreen(onGenerateClicked = { topic -> viewModel.generateFlashcards(topic) })
                is AiFlashcardUiState.Loading -> CircularProgressIndicator()
                is AiFlashcardUiState.Success -> FlashcardPager(
                    flashcards = state.flashcards,
                    onGenerateNew = { viewModel.resetState() }
                )
                is AiFlashcardUiState.Error -> ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.resetState() }
                )
            }
        }
    }
}

// Screen shown at the start
@Composable
fun IdleScreen(onGenerateClicked: (String) -> Unit) {
    var topic by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("What topic do you want to study today?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("e.g., 'The Krebs Cycle' or 'Java Interfaces'") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onGenerateClicked(topic) },
            enabled = topic.isNotBlank()
        ) {
            Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Generate Flashcards")
        }
    }
}

// Screen for displaying the swipeable flashcards
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlashcardPager(flashcards: List<Flashcard>, onGenerateNew: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { flashcards.size })

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(32.dp)
        ) { page ->
            FlippableFlashcard(flashcard = flashcards[page])
        }
        Text("${pagerState.currentPage + 1} / ${pagerState.pageCount}")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onGenerateNew) {
            Text("Create New Set")
        }
        Spacer(Modifier.height(16.dp))
    }
}

// The interactive, flippable card composable
@Composable
fun FlippableFlashcard(flashcard: Flashcard) {
    var isFlipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(500),
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 8 * density
            }
            // THIS IS THE FIX:
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // We set indication to null because the flip is the visual feedback
                onClick = { isFlipped = !isFlipped }
            ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (rotation < 90f) {
                // Show Question (Front)
                Text(
                    text = flashcard.question,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
            } else {
                // Show Answer (Back) - needs to be flipped back
                Text(
                    text = flashcard.answer,
                    modifier = Modifier
                        .padding(16.dp)
                        .graphicsLayer { rotationY = 180f }, // Flip the text back
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
// Screen for showing errors
@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}
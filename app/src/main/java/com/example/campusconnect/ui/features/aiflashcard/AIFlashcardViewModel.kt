package com.example.campusconnect.ui.features.aiflashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 1. A simple data class for a single flashcard
data class Flashcard(
    val question: String,
    val answer: String
)

// 2. A more robust UI state to handle different scenarios
sealed interface AiFlashcardUiState {
    // The initial state before the user has done anything
    object Idle : AiFlashcardUiState

    // State when the AI is processing the request
    object Loading : AiFlashcardUiState

    // State when the AI has successfully returned flashcards
    data class Success(val flashcards: List<Flashcard>) : AiFlashcardUiState

    // State for handling any errors
    data class Error(val message: String) : AiFlashcardUiState
}

class AiFlashcardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AiFlashcardUiState>(AiFlashcardUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        // Add a safety setting to reduce the chance of the AI refusing a prompt
        // safetySettings = listOf(SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH))
    )

    fun generateFlashcards(topic: String) {
        if (topic.isBlank()) {
            _uiState.update { AiFlashcardUiState.Error("Topic cannot be empty.") }
            return
        }

        _uiState.update { AiFlashcardUiState.Loading }

        // We create a more specific prompt for the AI to ensure better formatting
        val prompt = """
            Generate 5 flashcards about the following topic: "$topic".
            For each flashcard, provide a question and a concise answer.
            Use the following format exactly for each card, with no extra text before or after:
            1. Q: [Your Question] A: [Your Answer]
            2. Q: [Your Question] A: [Your Answer]
            ...
        """.trimIndent()

        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                val parsedFlashcards = parseFlashcardResponse(response.text ?: "")

                if (parsedFlashcards.isEmpty()) {
                    _uiState.update { AiFlashcardUiState.Error("The AI couldn't generate flashcards for this topic. Please try being more specific.") }
                } else {
                    _uiState.update { AiFlashcardUiState.Success(parsedFlashcards) }
                }

            } catch (e: Exception) {
                _uiState.update { AiFlashcardUiState.Error("An error occurred: ${e.localizedMessage}") }
            }
        }
    }

    // This function uses regular expressions to reliably parse the AI's output
    private fun parseFlashcardResponse(responseText: String): List<Flashcard> {
        val flashcards = mutableListOf<Flashcard>()
        // Regex to find patterns like "1. Q: ... A: ..."
        val pattern = Regex("""\d+\.\s*Q:\s*(.*?)\s*A:\s*(.*)""")
        val matches = pattern.findAll(responseText)

        for (match in matches) {
            val question = match.groups[1]?.value?.trim() ?: ""
            val answer = match.groups[2]?.value?.trim() ?: ""
            if (question.isNotEmpty() && answer.isNotEmpty()) {
                flashcards.add(Flashcard(question, answer))
            }
        }
        return flashcards
    }

    // Function to reset the screen back to the initial state
    fun resetState() {
        _uiState.update { AiFlashcardUiState.Idle }
    }
}
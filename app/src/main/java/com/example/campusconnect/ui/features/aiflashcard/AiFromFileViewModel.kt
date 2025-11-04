package com.example.campusconnect.ui.features.aiflashcard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Sealed Interface to represent the different states of the UI
sealed interface AiFromFileUiState {
    object Idle : AiFromFileUiState // Waiting for user input
    data class Processing(val status: String) : AiFromFileUiState // Working
    data class Success(val output: String) : AiFromFileUiState // Got a result
    data class Error(val message: String) : AiFromFileUiState // Something went wrong
}

class AiFromFileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AiFromFileUiState>(AiFromFileUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // Model for multimodal input (text + image)
    private val visionModel = GenerativeModel("gemini-2.5-pro", BuildConfig.GEMINI_API_KEY)

    // Efficient model for pure text input
    private val textModel = GenerativeModel("gemini-2.5-flash-lite", BuildConfig.GEMINI_API_KEY)

    fun processFile(context: Context, uri: Uri, mimeType: String?, userPrompt: String) {
        viewModelScope.launch {
            try {
                when {
                    // --- PATH 1: IMAGE (Use Vision AI) ---
                    mimeType?.startsWith("image/") == true -> {
                        _uiState.update { AiFromFileUiState.Processing("Analyzing image with Vision AI...") }
                        val bitmap = uriToBitmap(context, uri)

                        val inputContent = content {
                            image(bitmap)
                            // MODIFIED PROMPT: Ask for Markdown
                            text("Based on the image provided (which could contain text, diagrams, or both), fulfill this request: '$userPrompt'. " +
                                    "Format your response using Markdown for clear, hierarchical formatting " +
                                    "(e.g., use headings with '#', bold with '**', and lists with '-').")
                        }

                        val response = visionModel.generateContent(inputContent)
                        _uiState.update { AiFromFileUiState.Success(response.text ?: "No response text.") }
                    }

                    // --- PATH 2: PDF (Extract Text First) ---
                    mimeType == "application/pdf" -> {
                        _uiState.update { AiFromFileUiState.Processing("Extracting text from PDF...") }
                        val extractedText = extractTextFromPdf(context, uri)

                        if (extractedText.isBlank()) {
                            _uiState.update { AiFromFileUiState.Error("Could not extract any text from the PDF.") }
                            return@launch
                        }

                        _uiState.update { AiFromFileUiState.Processing("Sending extracted text to AI...") }

                        // MODIFIED PROMPT: Ask for Markdown
                        val finalPrompt = """
                            CONTEXT from PDF:
                            ---
                            $extractedText
                            ---
                            
                            USER'S REQUEST: '$userPrompt'
                            
                            Based only on the context provided, fulfill the user's request. 
                            Format your response using Markdown for clear, hierarchical formatting 
                            (e.g., use headings with '#', bold with '**', and lists with '-').
                        """.trimIndent()

                        val response = textModel.generateContent(finalPrompt)
                        _uiState.update { AiFromFileUiState.Success(response.text ?: "No response text.") }
                    }

                    // --- FALLBACK for unsupported types ---
                    else -> {
                        _uiState.update { AiFromFileUiState.Error("Unsupported file type. Please select an Image or PDF.") }
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e("AiFromFileVM", "Processing failed", e)
                _uiState.update { AiFromFileUiState.Error("An error occurred: ${e.localizedMessage}") }
            }
        }
    }

    // --- HELPER FUNCTIONS ---

    private suspend fun uriToBitmap(context: Context, uri: Uri): Bitmap {
        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        }
    }

    private suspend fun extractTextFromPdf(context: Context, uri: Uri): String {
        return withContext(Dispatchers.IO) {
            PDFBoxResourceLoader.init(context)
            val inputStream = context.contentResolver.openInputStream(uri)
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            text
        }
    }

    fun resetState() {
        _uiState.update { AiFromFileUiState.Idle }
    }
}
package com.adhdcompanion.focus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdcompanion.focus.di.AppContainer
import com.adhdcompanion.focus.model.MicroStep
import com.adhdcompanion.focus.model.UnblockerResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class StuckUiState(
    val inputText: String = "",
    val isListening: Boolean = false,
    val isLoading: Boolean = false,
    val response: UnblockerResponse? = null,
    val errorMessage: String? = null
)

class StuckViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(StuckUiState())
    val uiState: StateFlow<StuckUiState> = _uiState.asStateFlow()

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text, errorMessage = null)
    }

    fun startVoiceInput() {
        if (!container.speechRecognitionManager.isAvailable()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Voice input isn't available on this device.")
            return
        }
        container.speechRecognitionManager.startListening(
            onResult = { text ->
                _uiState.value = _uiState.value.copy(inputText = text, isListening = false)
            },
            onError = { message ->
                _uiState.value = _uiState.value.copy(errorMessage = message, isListening = false)
            },
            onListeningStateChanged = { listening ->
                _uiState.value = _uiState.value.copy(isListening = listening)
            }
        )
    }

    fun stopVoiceInput() {
        container.speechRecognitionManager.stopListening()
        _uiState.value = _uiState.value.copy(isListening = false)
    }

    fun submit() {
        val text = _uiState.value.inputText
        if (text.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Type or speak a few words about what's going on.")
            return
        }
        if (!container.prefs.hasApiKey()) {
            _uiState.value = _uiState.value.copy(
                response = localFallbackResponse(),
                errorMessage = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val minute = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
            val currentWhy = runCatching {
                container.blockRepository.getCurrentBlock(minute)?.why
            }.getOrNull()

            container.geminiRepository.generateUnblockerResponse(text, currentWhy)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(isLoading = false, response = result)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        response = localFallbackResponse(),
                        errorMessage = "Couldn't reach the AI, so here's a steady fallback instead."
                    )
                }
        }
    }

    fun toggleStep(stepId: Int) {
        val response = _uiState.value.response ?: return
        val updatedSteps = response.microSteps.map {
            if (it.id == stepId) it.copy(isChecked = !it.isChecked) else it
        }
        _uiState.value = _uiState.value.copy(response = response.copy(microSteps = updatedSteps))
    }

    fun reset() {
        _uiState.value = StuckUiState()
    }

    private fun localFallbackResponse(): UnblockerResponse = UnblockerResponse(
        calmMessage = "This is a totally normal ADHD moment, not a character flaw. You don't need to force anything.",
        whyReminder = "You started this because future-you will be glad you did, even a tiny bit.",
        microSteps = listOf(
            MicroStep(0, "Take one slow breath."),
            MicroStep(1, "Stand up and stretch for 10 seconds."),
            MicroStep(2, "Sit back down at your workspace."),
            MicroStep(3, "Open just the one thing you need — nothing else."),
            MicroStep(4, "Do one sentence, one line, or one problem. That's the whole step.")
        )
    )
}

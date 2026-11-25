package com.adhdcompanion.focus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdcompanion.focus.di.AppContainer
import com.adhdcompanion.focus.model.DayBlock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class NowUiState(
    val isLoading: Boolean = true,
    val currentBlock: DayBlock? = null,
    val nextBlock: DayBlock? = null,
    val briefingScript: String? = null,
    val isSpeaking: Boolean = false,
    /** Seconds left on the 5-minute micro-step timer; null when no timer is running. */
    val microStepSecondsRemaining: Int? = null,
    val errorMessage: String? = null
)

class NowViewModel(private val container: AppContainer) : ViewModel() {

    companion object {
        const val MICRO_STEP_SECONDS = 300
    }

    private var microStepTimerJob: Job? = null

    private val _uiState = MutableStateFlow(NowUiState())
    val uiState: StateFlow<NowUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    private fun nowMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val minute = nowMinuteOfDay()
            val current = container.blockRepository.getCurrentBlock(minute)
            val next = container.blockRepository.getNextBlock(minute)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentBlock = current,
                nextBlock = next
            )
        }
    }

    fun speakBriefing() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isSpeaking = true, errorMessage = null)

            val script = if (!container.prefs.hasApiKey()) {
                fallbackScript(state.currentBlock, state.nextBlock)
            } else {
                container.geminiRepository
                    .generateBriefingScript(state.currentBlock, state.nextBlock)
                    .getOrElse { fallbackScript(state.currentBlock, state.nextBlock) }
            }

            _uiState.value = _uiState.value.copy(briefingScript = script)
            container.speechManager.speak(script) {
                _uiState.value = _uiState.value.copy(isSpeaking = false)
            }
        }
    }

    fun speakMicroStepOnly() {
        val step = _uiState.value.currentBlock?.microStep ?: return
        _uiState.value = _uiState.value.copy(isSpeaking = true)
        container.speechManager.speak("Your tiny first step: $step") {
            _uiState.value = _uiState.value.copy(isSpeaking = false)
        }
    }

    /**
     * Speaks the first micro-step, then runs a live 300-second countdown.
     * Speaks a short completion cue when it reaches zero. Does NOT mark the
     * block complete — that stays on its own button.
     */
    fun startFiveMinuteMicroStep() {
        val block = _uiState.value.currentBlock ?: return
        microStepTimerJob?.cancel()

        val step = block.microStep.ifBlank { "Just open what you need and look at it. That's it." }
        _uiState.value = _uiState.value.copy(
            isSpeaking = true,
            microStepSecondsRemaining = MICRO_STEP_SECONDS
        )
        container.speechManager.speak("Your tiny first step: $step. I'll time five minutes. Just start.") {
            _uiState.value = _uiState.value.copy(isSpeaking = false)
        }

        microStepTimerJob = viewModelScope.launch {
            var remaining = MICRO_STEP_SECONDS
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _uiState.value = _uiState.value.copy(microStepSecondsRemaining = remaining)
            }
            _uiState.value = _uiState.value.copy(microStepSecondsRemaining = null, isSpeaking = true)
            container.speechManager.speak(
                "Five minutes are up. Nice work. You can keep going, or take a breath."
            ) {
                _uiState.value = _uiState.value.copy(isSpeaking = false)
            }
        }
    }

    fun cancelMicroStepTimer() {
        microStepTimerJob?.cancel()
        microStepTimerJob = null
        container.speechManager.stop()
        _uiState.value = _uiState.value.copy(microStepSecondsRemaining = null, isSpeaking = false)
    }

    override fun onCleared() {
        microStepTimerJob?.cancel()
        super.onCleared()
    }

    fun markCurrentBlockStarted() {
        // Reserved for future "start micro-step timer" behavior; refresh for now.
        refresh()
    }

    fun markCurrentBlockCompleted() {
        viewModelScope.launch {
            val block = _uiState.value.currentBlock ?: return@launch
            microStepTimerJob?.cancel()
            microStepTimerJob = null
            _uiState.value = _uiState.value.copy(microStepSecondsRemaining = null)
            container.blockRepository.markCompleted(block)
            refresh()
        }
    }

    private fun fallbackScript(current: DayBlock?, next: DayBlock?): String {
        return if (current == null) {
            "Nothing is scheduled right now. Open the Planner tab whenever you're ready to set up your day."
        } else {
            buildString {
                append("Right now: ${current.title}. ")
                if (current.why.isNotBlank()) append("${current.why} ")
                if (current.microStep.isNotBlank()) append("First tiny step: ${current.microStep}. ")
                if (next != null) append("Up next: ${next.title}.")
            }
        }
    }
}

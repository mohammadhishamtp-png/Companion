package com.adhdcompanion.focus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdcompanion.focus.di.AppContainer
import com.adhdcompanion.focus.model.DayBlock
import com.adhdcompanion.focus.model.EnergyLevel
import com.adhdcompanion.focus.model.PlannerInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlannerUiState(
    val availableHours: String = "",
    val energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
    val goalsAndBlocks: String = "",
    val isGenerating: Boolean = false,
    val errorMessage: String? = null
)

class PlannerViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    val todaysBlocks: StateFlow<List<DayBlock>> = container.blockRepository
        .observeBlocksForDay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateHours(value: String) {
        _uiState.value = _uiState.value.copy(availableHours = value)
    }

    fun updateEnergy(value: EnergyLevel) {
        _uiState.value = _uiState.value.copy(energyLevel = value)
    }

    fun updateGoals(value: String) {
        _uiState.value = _uiState.value.copy(goalsAndBlocks = value)
    }

    fun generatePlan() {
        val state = _uiState.value
        if (state.availableHours.isBlank() || state.goalsAndBlocks.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Fill in your available hours and goals first.")
            return
        }
        if (!container.prefs.hasApiKey()) {
            _uiState.value = state.copy(errorMessage = "Add your Gemini API key in Settings first.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isGenerating = true, errorMessage = null)
            val input = PlannerInput(
                availableHours = state.availableHours,
                energyLevel = state.energyLevel,
                goalsAndBlocks = state.goalsAndBlocks
            )
            container.geminiRepository.generateDayPlan(input)
                .onSuccess { blocks ->
                    container.blockRepository.replaceDayPlan(blocks)
                    _uiState.value = _uiState.value.copy(isGenerating = false)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        errorMessage = error.message ?: "Couldn't generate a plan. Check your connection and API key."
                    )
                }
        }
    }

    fun deleteBlock(block: DayBlock) {
        viewModelScope.launch { container.blockRepository.deleteBlock(block) }
    }

    fun toggleCompleted(block: DayBlock) {
        viewModelScope.launch {
            if (!block.isCompleted) {
                container.blockRepository.markCompleted(block)
            } else {
                container.blockRepository.upsertBlock(block.copy(isCompleted = false))
            }
        }
    }
}

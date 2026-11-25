package com.adhdcompanion.focus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhdcompanion.focus.di.AppContainer
import com.adhdcompanion.focus.model.BrainDumpEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BrainDumpViewModel(private val container: AppContainer) : ViewModel() {

    private val _draftText = MutableStateFlow("")
    val draftText: StateFlow<String> = _draftText

    val entries: StateFlow<List<BrainDumpEntry>> = container.brainDumpRepository
        .observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateDraft(text: String) {
        _draftText.value = text
    }

    fun capture() {
        val text = _draftText.value
        if (text.isBlank()) return
        viewModelScope.launch {
            container.brainDumpRepository.quickCapture(text)
            _draftText.value = ""
        }
    }

    fun sendToPlanner(entry: BrainDumpEntry) {
        viewModelScope.launch {
            container.brainDumpRepository.markSentToPlanner(entry)
        }
    }

    fun delete(entry: BrainDumpEntry) {
        viewModelScope.launch { container.brainDumpRepository.delete(entry) }
    }
}

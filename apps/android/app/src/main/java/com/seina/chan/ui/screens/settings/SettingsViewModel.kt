package com.seina.chan.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seina.chan.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val pageSize: Int = 20,
    val showToolCalls: Boolean = true,
    val showReasoning: Boolean = true,
    val themeMode: String = "system",
    val showTimestamps: Boolean = false,
    val autoExpandReasoning: Boolean = false,
    val autoExpandTools: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepository.pageSize.collect { value ->
                _uiState.update { it.copy(pageSize = value) }
            }
        }
        viewModelScope.launch {
            settingsRepository.showToolCalls.collect { value ->
                _uiState.update { it.copy(showToolCalls = value) }
            }
        }
        viewModelScope.launch {
            settingsRepository.showReasoning.collect { value ->
                _uiState.update { it.copy(showReasoning = value) }
            }
        }
        viewModelScope.launch {
            settingsRepository.themeMode.collect { value ->
                _uiState.update { it.copy(themeMode = value) }
            }
        }
        viewModelScope.launch {
            settingsRepository.showTimestamps.collect { value ->
                _uiState.update { it.copy(showTimestamps = value) }
            }
        }
        viewModelScope.launch {
            settingsRepository.autoExpandReasoning.collect { value ->
                _uiState.update { it.copy(autoExpandReasoning = value) }
            }
        }
        viewModelScope.launch {
            settingsRepository.autoExpandTools.collect { value ->
                _uiState.update { it.copy(autoExpandTools = value) }
            }
        }
    }

    fun setPageSize(value: Int) {
        viewModelScope.launch {
            settingsRepository.setPageSize(value)
        }
    }

    fun setShowToolCalls(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowToolCalls(value)
        }
    }

    fun setShowReasoning(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowReasoning(value)
        }
    }

    fun setThemeMode(value: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(value)
        }
    }

    fun setShowTimestamps(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowTimestamps(value)
        }
    }

    fun setAutoExpandReasoning(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoExpandReasoning(value)
        }
    }

    fun setAutoExpandTools(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoExpandTools(value)
        }
    }
}

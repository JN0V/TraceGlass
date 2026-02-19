package io.github.jn0v.traceglass.feature.tracing.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jn0v.traceglass.core.session.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        repository.settingsData
            .onEach { data ->
                _uiState.update {
                    it.copy(
                        audioFeedbackEnabled = data.audioFeedbackEnabled,
                        breakReminderEnabled = data.breakReminderEnabled,
                        breakReminderIntervalMinutes = data.breakReminderIntervalMinutes
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onAudioFeedbackToggled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAudioFeedbackEnabled(enabled)
        }
    }

    fun onBreakReminderToggled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBreakReminderEnabled(enabled)
        }
    }

    fun onBreakIntervalChanged(minutes: Int) {
        val clamped = minutes.coerceIn(MIN_INTERVAL, MAX_INTERVAL)
        viewModelScope.launch {
            repository.setBreakReminderIntervalMinutes(clamped)
        }
    }

    companion object {
        const val MIN_INTERVAL = 5
        const val MAX_INTERVAL = 60
    }
}

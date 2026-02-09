package io.github.jn0v.traceglass.feature.tracing.settings

import io.github.jn0v.traceglass.core.session.SettingsData
import io.github.jn0v.traceglass.core.session.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSettingsRepository : SettingsRepository {

    private val _settingsData = MutableStateFlow(SettingsData())
    override val settingsData: Flow<SettingsData> = _settingsData.asStateFlow()

    var audioFeedbackSetCount: Int = 0
        private set
    var breakReminderSetCount: Int = 0
        private set
    var breakIntervalSetCount: Int = 0
        private set

    override suspend fun setAudioFeedbackEnabled(enabled: Boolean) {
        audioFeedbackSetCount++
        _settingsData.value = _settingsData.value.copy(audioFeedbackEnabled = enabled)
    }

    override suspend fun setBreakReminderEnabled(enabled: Boolean) {
        breakReminderSetCount++
        _settingsData.value = _settingsData.value.copy(breakReminderEnabled = enabled)
    }

    override suspend fun setBreakReminderIntervalMinutes(minutes: Int) {
        breakIntervalSetCount++
        _settingsData.value = _settingsData.value.copy(breakReminderIntervalMinutes = minutes)
    }
}

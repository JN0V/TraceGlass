package io.github.jn0v.traceglass.core.session

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsData: Flow<SettingsData>
    suspend fun setAudioFeedbackEnabled(enabled: Boolean)
    suspend fun setBreakReminderEnabled(enabled: Boolean)
    suspend fun setBreakReminderIntervalMinutes(minutes: Int)
    suspend fun setPerspectiveCorrectionEnabled(enabled: Boolean)
}

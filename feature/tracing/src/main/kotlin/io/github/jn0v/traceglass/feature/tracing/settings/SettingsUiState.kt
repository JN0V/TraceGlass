package io.github.jn0v.traceglass.feature.tracing.settings

data class SettingsUiState(
    val audioFeedbackEnabled: Boolean = false,
    val breakReminderEnabled: Boolean = false,
    val breakReminderIntervalMinutes: Int = 30,
    val perspectiveCorrectionEnabled: Boolean = true
)

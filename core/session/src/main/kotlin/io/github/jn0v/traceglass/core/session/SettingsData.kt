package io.github.jn0v.traceglass.core.session

data class SettingsData(
    val audioFeedbackEnabled: Boolean = false,
    val breakReminderEnabled: Boolean = false,
    val breakReminderIntervalMinutes: Int = 30
)

package io.github.jn0v.traceglass.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val settingsData: Flow<SettingsData> = dataStore.data.map { prefs ->
        SettingsData(
            audioFeedbackEnabled = prefs[KEY_AUDIO_FEEDBACK] ?: false,
            breakReminderEnabled = prefs[KEY_BREAK_REMINDER] ?: false,
            breakReminderIntervalMinutes = prefs[KEY_BREAK_INTERVAL] ?: 30
        )
    }

    override suspend fun setAudioFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_AUDIO_FEEDBACK] = enabled }
    }

    override suspend fun setBreakReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BREAK_REMINDER] = enabled }
    }

    override suspend fun setBreakReminderIntervalMinutes(minutes: Int) {
        dataStore.edit { it[KEY_BREAK_INTERVAL] = minutes }
    }

    companion object {
        private val KEY_AUDIO_FEEDBACK = booleanPreferencesKey("settings_audio_feedback")
        private val KEY_BREAK_REMINDER = booleanPreferencesKey("settings_break_reminder")
        private val KEY_BREAK_INTERVAL = intPreferencesKey("settings_break_interval")
    }
}

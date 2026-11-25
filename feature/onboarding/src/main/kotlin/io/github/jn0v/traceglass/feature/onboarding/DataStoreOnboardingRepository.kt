package io.github.jn0v.traceglass.feature.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import io.github.jn0v.traceglass.core.cv.DetectionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreOnboardingRepository(
    private val dataStore: DataStore<Preferences>
) : OnboardingRepository {

    private val safeData: Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is IOException) emit(emptyPreferences()) else throw exception
    }

    override val isOnboardingCompleted: Flow<Boolean> =
        safeData.map { prefs -> prefs[KEY_COMPLETED] ?: false }

    override val isTooltipShown: Flow<Boolean> =
        safeData.map { prefs -> prefs[KEY_TOOLTIP_SHOWN] ?: false }

    override val selectedTier: Flow<SetupTier?> =
        safeData.map { prefs ->
            prefs[KEY_SELECTED_TIER]?.let { name ->
                SetupTier.entries.firstOrNull { it.name == name }
            }
        }

    override val detectionMode: Flow<DetectionMode> =
        safeData.map { prefs ->
            DetectionMode.entries.firstOrNull { it.name == prefs[KEY_DETECTION_MODE] }
                ?: DetectionMode.ARUCO
        }

    override suspend fun setOnboardingCompleted() {
        dataStore.edit { it[KEY_COMPLETED] = true }
    }

    override suspend fun setTooltipShown() {
        dataStore.edit { it[KEY_TOOLTIP_SHOWN] = true }
    }

    override suspend fun setSelectedTier(tier: SetupTier) {
        dataStore.edit { it[KEY_SELECTED_TIER] = tier.name }
    }

    override suspend fun setDetectionMode(mode: DetectionMode) {
        dataStore.edit { it[KEY_DETECTION_MODE] = mode.name }
    }

    override suspend fun resetOnboarding() {
        dataStore.edit {
            it[KEY_COMPLETED] = false
            it[KEY_TOOLTIP_SHOWN] = false
            it.remove(KEY_SELECTED_TIER)
            it.remove(KEY_DETECTION_MODE)
        }
    }

    companion object {
        private val KEY_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_TOOLTIP_SHOWN = booleanPreferencesKey("tooltip_shown")
        private val KEY_SELECTED_TIER = stringPreferencesKey("selected_tier")
        private val KEY_DETECTION_MODE = stringPreferencesKey("detection_mode")
    }
}

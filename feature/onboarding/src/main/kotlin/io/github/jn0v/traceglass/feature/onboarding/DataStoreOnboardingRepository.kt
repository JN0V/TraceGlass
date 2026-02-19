package io.github.jn0v.traceglass.feature.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreOnboardingRepository(
    private val dataStore: DataStore<Preferences>
) : OnboardingRepository {

    override val isOnboardingCompleted: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[KEY_COMPLETED] ?: false }

    override val isTooltipShown: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[KEY_TOOLTIP_SHOWN] ?: false }

    override val selectedTier: Flow<SetupTier?> =
        dataStore.data.map { prefs ->
            prefs[KEY_SELECTED_TIER]?.let { name ->
                SetupTier.entries.firstOrNull { it.name == name }
            }
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

    override suspend fun resetOnboarding() {
        dataStore.edit {
            it[KEY_COMPLETED] = false
            it[KEY_TOOLTIP_SHOWN] = false
            it.remove(KEY_SELECTED_TIER)
        }
    }

    companion object {
        private val KEY_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_TOOLTIP_SHOWN = booleanPreferencesKey("tooltip_shown")
        private val KEY_SELECTED_TIER = stringPreferencesKey("selected_tier")
    }
}

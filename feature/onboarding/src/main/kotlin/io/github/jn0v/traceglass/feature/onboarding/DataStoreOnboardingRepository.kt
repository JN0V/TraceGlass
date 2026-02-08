package io.github.jn0v.traceglass.feature.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreOnboardingRepository(
    private val dataStore: DataStore<Preferences>
) : OnboardingRepository {

    override val isOnboardingCompleted: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[KEY_COMPLETED] ?: false }

    override suspend fun setOnboardingCompleted() {
        dataStore.edit { it[KEY_COMPLETED] = true }
    }

    override suspend fun resetOnboarding() {
        dataStore.edit { it[KEY_COMPLETED] = false }
    }

    companion object {
        private val KEY_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}

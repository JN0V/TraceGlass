package io.github.jn0v.traceglass.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSessionRepository(
    private val dataStore: DataStore<Preferences>
) : SessionRepository {

    override val sessionData: Flow<SessionData> = dataStore.data.map { prefs ->
        SessionData(
            imageUri = prefs[KEY_IMAGE_URI],
            overlayOffsetX = prefs[KEY_OFFSET_X] ?: 0f,
            overlayOffsetY = prefs[KEY_OFFSET_Y] ?: 0f,
            overlayScale = prefs[KEY_SCALE] ?: 1f,
            overlayOpacity = prefs[KEY_OPACITY] ?: 0.5f,
            colorTint = prefs[KEY_COLOR_TINT] ?: "NONE",
            isInvertedMode = prefs[KEY_INVERTED] ?: false,
            isSessionActive = prefs[KEY_SESSION_ACTIVE] ?: false,
            timelapseSnapshotCount = prefs[KEY_TIMELAPSE_COUNT] ?: 0
        )
    }

    override suspend fun save(data: SessionData) {
        dataStore.edit { prefs ->
            if (data.imageUri != null) {
                prefs[KEY_IMAGE_URI] = data.imageUri
            } else {
                prefs.remove(KEY_IMAGE_URI)
            }
            prefs[KEY_OFFSET_X] = data.overlayOffsetX
            prefs[KEY_OFFSET_Y] = data.overlayOffsetY
            prefs[KEY_SCALE] = data.overlayScale
            prefs[KEY_OPACITY] = data.overlayOpacity
            prefs[KEY_COLOR_TINT] = data.colorTint
            prefs[KEY_INVERTED] = data.isInvertedMode
            prefs[KEY_SESSION_ACTIVE] = data.isSessionActive
            prefs[KEY_TIMELAPSE_COUNT] = data.timelapseSnapshotCount
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    companion object {
        private val KEY_IMAGE_URI = stringPreferencesKey("session_image_uri")
        private val KEY_OFFSET_X = floatPreferencesKey("session_offset_x")
        private val KEY_OFFSET_Y = floatPreferencesKey("session_offset_y")
        private val KEY_SCALE = floatPreferencesKey("session_scale")
        private val KEY_OPACITY = floatPreferencesKey("session_opacity")
        private val KEY_COLOR_TINT = stringPreferencesKey("session_color_tint")
        private val KEY_INVERTED = booleanPreferencesKey("session_inverted")
        private val KEY_SESSION_ACTIVE = booleanPreferencesKey("session_active")
        private val KEY_TIMELAPSE_COUNT = intPreferencesKey("session_timelapse_count")
    }
}

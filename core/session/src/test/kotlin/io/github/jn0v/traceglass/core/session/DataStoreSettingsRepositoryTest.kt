package io.github.jn0v.traceglass.core.session

import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataStoreSettingsRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private var repoCounter = 0

    private fun TestScope.createRepo(): DataStoreSettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope
        ) { File(tempDir, "test_settings_${repoCounter++}.preferences_pb") }
        return DataStoreSettingsRepository(dataStore)
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    // Given a break interval of 0 minutes saved
    // When the settings are read
    // Then the interval is 5 minutes
    @Test
    fun `break interval of 0 is read as 5`() = runTest {
        val repo = createRepo()
        repo.setBreakReminderIntervalMinutes(0)

        assertEquals(5, repo.settingsData.first().breakReminderIntervalMinutes)
    }

    // Given a break interval of 500 minutes saved
    // When the settings are read
    // Then the interval is 60 minutes
    @Test
    fun `break interval of 500 is read as 60`() = runTest {
        val repo = createRepo()
        repo.setBreakReminderIntervalMinutes(500)

        assertEquals(60, repo.settingsData.first().breakReminderIntervalMinutes)
    }
}

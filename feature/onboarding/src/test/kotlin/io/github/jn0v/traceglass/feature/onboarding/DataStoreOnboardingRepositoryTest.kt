package io.github.jn0v.traceglass.feature.onboarding

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class DataStoreOnboardingRepositoryTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun failingDataStore(error: Throwable): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw error }

            override suspend fun updateData(
                transform: suspend (t: Preferences) -> Preferences
            ): Preferences = throw error
        }

    // Given the onboarding store throws an IOException on read,
    // When "onboarding completed" is read,
    // Then it is false
    @Test
    fun `unreadable store gives onboarding not completed`() = runTest {
        val repo = DataStoreOnboardingRepository(
            failingDataStore(IOException("corrupt onboarding file"))
        )

        assertFalse(repo.isOnboardingCompleted.first())
    }

    // Given the same store (throws an IOException on read),
    // When the chosen tier is read,
    // Then there is none
    @Test
    fun `unreadable store gives no selected tier`() = runTest {
        val repo = DataStoreOnboardingRepository(
            failingDataStore(IOException("corrupt onboarding file"))
        )

        assertNull(repo.selectedTier.first())
    }
}

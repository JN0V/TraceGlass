package io.github.jn0v.traceglass.feature.onboarding

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.github.jn0v.traceglass.core.cv.DetectionMode
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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

    private fun inMemoryDataStore(): DataStore<Preferences> =
        object : DataStore<Preferences> {
            private val state = MutableStateFlow(emptyPreferences())
            override val data: Flow<Preferences> = state

            override suspend fun updateData(
                transform: suspend (t: Preferences) -> Preferences
            ): Preferences = transform(state.value).also { state.value = it }
        }

    // Given nothing stored, When the detection mode is read, Then it is ArUco
    @Test
    fun `detection mode defaults to aruco`() = runTest {
        val repo = DataStoreOnboardingRepository(inMemoryDataStore())

        assertEquals(DetectionMode.ARUCO, repo.detectionMode.first())
    }

    // Given custom shapes are chosen, When the mode is read back, Then it is custom shapes
    @Test
    fun `chosen detection mode is stored`() = runTest {
        val repo = DataStoreOnboardingRepository(inMemoryDataStore())

        repo.setDetectionMode(DetectionMode.CUSTOM_SHAPES)

        assertEquals(DetectionMode.CUSTOM_SHAPES, repo.detectionMode.first())
    }

    // Given custom shapes are chosen, When onboarding is reset, Then the mode is ArUco again
    @Test
    fun `reset restores aruco`() = runTest {
        val repo = DataStoreOnboardingRepository(inMemoryDataStore())
        repo.setDetectionMode(DetectionMode.CUSTOM_SHAPES)

        repo.resetOnboarding()

        assertEquals(DetectionMode.ARUCO, repo.detectionMode.first())
    }

    // Given an unreadable store, When the mode is read, Then it is ArUco
    @Test
    fun `unreadable store gives aruco`() = runTest {
        val repo = DataStoreOnboardingRepository(
            failingDataStore(IOException("corrupt onboarding file"))
        )

        assertEquals(DetectionMode.ARUCO, repo.detectionMode.first())
    }
}

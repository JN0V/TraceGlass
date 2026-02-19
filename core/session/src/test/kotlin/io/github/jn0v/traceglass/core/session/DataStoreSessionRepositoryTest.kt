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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataStoreSessionRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private var repoCounter = 0

    private fun TestScope.createRepo(): DataStoreSessionRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope
        ) { File(tempDir, "test_prefs_${repoCounter++}.preferences_pb") }
        return DataStoreSessionRepository(dataStore)
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Nested
    inner class SaveAndRead {
        @Test
        fun `save persists all fields and read restores them`() = runTest {
            val repo = createRepo()
            val data = SessionData(
                imageUri = "file:///data/ref.jpg",
                overlayOffsetX = 10.5f,
                overlayOffsetY = -20.3f,
                overlayScale = 2.0f,
                overlayRotation = 45.0f,
                overlayOpacity = 0.8f,
                colorTint = "RED",
                isInvertedMode = true,
                isSessionActive = true,
                timelapseSnapshotCount = 42,
                isTimelapseRecording = true,
                isTimelapsePaused = false,
                isOverlayLocked = true,
                viewportZoom = 3.0f,
                viewportPanX = 100f,
                viewportPanY = -50f
            )

            repo.save(data)
            val restored = repo.sessionData.first()

            assertEquals("file:///data/ref.jpg", restored.imageUri)
            assertEquals(10.5f, restored.overlayOffsetX)
            assertEquals(-20.3f, restored.overlayOffsetY)
            assertEquals(2.0f, restored.overlayScale)
            assertEquals(45.0f, restored.overlayRotation)
            assertEquals(0.8f, restored.overlayOpacity)
            assertEquals("RED", restored.colorTint)
            assertTrue(restored.isInvertedMode)
            assertTrue(restored.isSessionActive)
            assertEquals(42, restored.timelapseSnapshotCount)
            assertTrue(restored.isTimelapseRecording)
            assertFalse(restored.isTimelapsePaused)
            assertTrue(restored.isOverlayLocked)
            assertEquals(3.0f, restored.viewportZoom)
            assertEquals(100f, restored.viewportPanX)
            assertEquals(-50f, restored.viewportPanY)
        }

        @Test
        fun `save with null imageUri removes the key`() = runTest {
            val repo = createRepo()
            repo.save(SessionData(imageUri = "file:///img.jpg"))
            assertEquals("file:///img.jpg", repo.sessionData.first().imageUri)

            repo.save(SessionData(imageUri = null))
            assertNull(repo.sessionData.first().imageUri)
        }

        @Test
        fun `default values when no data saved`() = runTest {
            val repo = createRepo()
            val data = repo.sessionData.first()

            assertNull(data.imageUri)
            assertEquals(0f, data.overlayOffsetX)
            assertEquals(0f, data.overlayOffsetY)
            assertEquals(1f, data.overlayScale)
            assertEquals(0f, data.overlayRotation)
            assertEquals(0.5f, data.overlayOpacity)
            assertEquals("NONE", data.colorTint)
            assertFalse(data.isInvertedMode)
            assertFalse(data.isSessionActive)
            assertEquals(0, data.timelapseSnapshotCount)
            assertFalse(data.isTimelapseRecording)
            assertFalse(data.isTimelapsePaused)
            assertFalse(data.isOverlayLocked)
            assertEquals(1f, data.viewportZoom)
            assertEquals(0f, data.viewportPanX)
            assertEquals(0f, data.viewportPanY)
        }
    }

    @Nested
    inner class Clear {
        @Test
        fun `clear removes only session keys`() = runTest {
            val repo = createRepo()
            repo.save(
                SessionData(
                    imageUri = "file:///img.jpg",
                    overlayOpacity = 0.9f,
                    isSessionActive = true,
                    viewportZoom = 2.5f
                )
            )

            repo.clear()
            val data = repo.sessionData.first()

            assertNull(data.imageUri)
            assertEquals(0.5f, data.overlayOpacity)
            assertFalse(data.isSessionActive)
            assertEquals(1f, data.viewportZoom)
        }

        @Test
        fun `clear then save works correctly`() = runTest {
            val repo = createRepo()
            repo.save(SessionData(imageUri = "file:///old.jpg"))
            repo.clear()
            repo.save(SessionData(imageUri = "file:///new.jpg", overlayScale = 3f))

            val data = repo.sessionData.first()
            assertEquals("file:///new.jpg", data.imageUri)
            assertEquals(3f, data.overlayScale)
        }
    }

    @Nested
    inner class HasSavedOverlay {
        @Test
        fun `hasSavedOverlay is false with no image`() = runTest {
            val repo = createRepo()
            val data = repo.sessionData.first()
            assertFalse(data.hasSavedOverlay)
        }

        @Test
        fun `hasSavedOverlay is true with image`() = runTest {
            val repo = createRepo()
            repo.save(SessionData(imageUri = "file:///img.png"))
            assertTrue(repo.sessionData.first().hasSavedOverlay)
        }
    }

    @Nested
    inner class OverwriteBehavior {
        @Test
        fun `second save overwrites first`() = runTest {
            val repo = createRepo()
            repo.save(SessionData(imageUri = "file:///first.jpg", overlayOpacity = 0.3f))
            repo.save(SessionData(imageUri = "file:///second.jpg", overlayOpacity = 0.7f))

            val data = repo.sessionData.first()
            assertEquals("file:///second.jpg", data.imageUri)
            assertEquals(0.7f, data.overlayOpacity)
        }
    }

    @Nested
    inner class SessionKeysSync {
        @Test
        fun `clear resets ALL fields to defaults when all were non-default`() = runTest {
            val repo = createRepo()
            // Save with every field set to a non-default value
            repo.save(
                SessionData(
                    imageUri = "file:///img.jpg",
                    overlayOffsetX = 99f,
                    overlayOffsetY = 99f,
                    overlayScale = 5f,
                    overlayRotation = 180f,
                    overlayOpacity = 0.1f,
                    colorTint = "SEPIA",
                    isInvertedMode = true,
                    isSessionActive = true,
                    timelapseSnapshotCount = 100,
                    isTimelapseRecording = true,
                    isTimelapsePaused = true,
                    isOverlayLocked = true,
                    viewportZoom = 4f,
                    viewportPanX = 200f,
                    viewportPanY = 300f
                )
            )

            repo.clear()
            val data = repo.sessionData.first()
            val defaults = SessionData()

            // If SESSION_KEYS is missing a key, the corresponding field
            // will retain the non-default value after clear().
            assertEquals(defaults.imageUri, data.imageUri, "imageUri not cleared")
            assertEquals(defaults.overlayOffsetX, data.overlayOffsetX, "overlayOffsetX not cleared")
            assertEquals(defaults.overlayOffsetY, data.overlayOffsetY, "overlayOffsetY not cleared")
            assertEquals(defaults.overlayScale, data.overlayScale, "overlayScale not cleared")
            assertEquals(defaults.overlayRotation, data.overlayRotation, "overlayRotation not cleared")
            assertEquals(defaults.overlayOpacity, data.overlayOpacity, "overlayOpacity not cleared")
            assertEquals(defaults.colorTint, data.colorTint, "colorTint not cleared")
            assertEquals(defaults.isInvertedMode, data.isInvertedMode, "isInvertedMode not cleared")
            assertEquals(defaults.isSessionActive, data.isSessionActive, "isSessionActive not cleared")
            assertEquals(defaults.timelapseSnapshotCount, data.timelapseSnapshotCount, "timelapseSnapshotCount not cleared")
            assertEquals(defaults.isTimelapseRecording, data.isTimelapseRecording, "isTimelapseRecording not cleared")
            assertEquals(defaults.isTimelapsePaused, data.isTimelapsePaused, "isTimelapsePaused not cleared")
            assertEquals(defaults.isOverlayLocked, data.isOverlayLocked, "isOverlayLocked not cleared")
            assertEquals(defaults.viewportZoom, data.viewportZoom, "viewportZoom not cleared")
            assertEquals(defaults.viewportPanX, data.viewportPanX, "viewportPanX not cleared")
            assertEquals(defaults.viewportPanY, data.viewportPanY, "viewportPanY not cleared")
        }
    }
}

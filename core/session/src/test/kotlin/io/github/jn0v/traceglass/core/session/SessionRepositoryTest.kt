package io.github.jn0v.traceglass.core.session

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SessionRepositoryTest {

    @Nested
    inner class SessionDataDefaults {
        @Test
        fun `default SessionData has null image URI`() {
            val data = SessionData()
            assertNull(data.imageUri)
        }

        @Test
        fun `default SessionData has no active session`() {
            val data = SessionData()
            assertFalse(data.hasSavedOverlay)
        }

        @Test
        fun `SessionData with image URI has active session`() {
            val data = SessionData(imageUri = "content://test")
            assertTrue(data.hasSavedOverlay)
        }

        @Test
        fun `default overlay values`() {
            val data = SessionData()
            assertEquals(0f, data.overlayOffsetX)
            assertEquals(0f, data.overlayOffsetY)
            assertEquals(1f, data.overlayScale)
            assertEquals(0.5f, data.overlayOpacity)
            assertEquals("NONE", data.colorTint)
            assertFalse(data.isInvertedMode)
            assertFalse(data.isSessionActive)
            assertEquals(0, data.timelapseSnapshotCount)
        }
    }

    @Nested
    inner class FakeRepositoryBehavior {
        @Test
        fun `save persists data`() = runTest {
            val repo = FakeSessionRepository()
            val data = SessionData(
                imageUri = "content://image/123",
                overlayOffsetX = 10f,
                overlayOffsetY = 20f,
                overlayScale = 1.5f,
                overlayOpacity = 0.7f,
                colorTint = "RED",
                isInvertedMode = true,
                isSessionActive = true,
                timelapseSnapshotCount = 42
            )

            repo.save(data)

            val restored = repo.sessionData.first()
            assertEquals("content://image/123", restored.imageUri)
            assertEquals(10f, restored.overlayOffsetX)
            assertEquals(20f, restored.overlayOffsetY)
            assertEquals(1.5f, restored.overlayScale)
            assertEquals(0.7f, restored.overlayOpacity)
            assertEquals("RED", restored.colorTint)
            assertTrue(restored.isInvertedMode)
            assertTrue(restored.isSessionActive)
            assertEquals(42, restored.timelapseSnapshotCount)
        }

        @Test
        fun `save increments counter`() = runTest {
            val repo = FakeSessionRepository()
            assertEquals(0, repo.saveCount)
            repo.save(SessionData())
            assertEquals(1, repo.saveCount)
            repo.save(SessionData())
            assertEquals(2, repo.saveCount)
        }

        @Test
        fun `clear resets to defaults`() = runTest {
            val repo = FakeSessionRepository()
            repo.save(SessionData(imageUri = "content://test", overlayOpacity = 0.8f))
            repo.clear()

            val restored = repo.sessionData.first()
            assertNull(restored.imageUri)
            assertEquals(0.5f, restored.overlayOpacity)
        }

        @Test
        fun `clear increments counter`() = runTest {
            val repo = FakeSessionRepository()
            repo.clear()
            assertEquals(1, repo.clearCount)
        }
    }
}

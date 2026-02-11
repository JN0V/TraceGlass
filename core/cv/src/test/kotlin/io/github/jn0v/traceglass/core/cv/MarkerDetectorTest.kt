package io.github.jn0v.traceglass.core.cv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class MarkerDetectorTest {

    @Nested
    inner class MarkerResultTests {
        @Test
        fun `empty result is not tracking`() {
            val result = MarkerResult(emptyList(), 10L)
            assertFalse(result.isTracking)
            assertEquals(0, result.markerCount)
        }

        @Test
        fun `result with markers is tracking`() {
            val marker = DetectedMarker(
                id = 1,
                centerX = 100f,
                centerY = 200f,
                corners = listOf(
                    90f to 190f, 110f to 190f,
                    110f to 210f, 90f to 210f
                ),
                confidence = 0.95f
            )
            val result = MarkerResult(listOf(marker), 5L)
            assertTrue(result.isTracking)
            assertEquals(1, result.markerCount)
        }

        @Test
        fun `detection time is recorded`() {
            val result = MarkerResult(emptyList(), 42L)
            assertEquals(42L, result.detectionTimeMs)
        }
    }

    @Nested
    inner class FakeMarkerDetectorTests {
        @Test
        fun `fake detector returns configured result`() {
            val detector = FakeMarkerDetector()
            val marker = DetectedMarker(
                id = 0, centerX = 50f, centerY = 50f,
                corners = emptyList(), confidence = 1f
            )
            detector.resultToReturn = MarkerResult(listOf(marker), 3L)

            val buffer = ByteBuffer.allocate(100)
            val result = detector.detect(buffer, 640, 480, 0, 0)

            assertTrue(result.isTracking)
            assertEquals(1, result.markerCount)
            assertEquals(3L, result.detectionTimeMs)
        }

        @Test
        fun `fake detector counts calls`() {
            val detector = FakeMarkerDetector()
            val buffer = ByteBuffer.allocate(100)

            assertEquals(0, detector.detectCallCount)
            detector.detect(buffer, 640, 480, 0, 0)
            detector.detect(buffer, 640, 480, 0, 0)
            assertEquals(2, detector.detectCallCount)
        }

        @Test
        fun `fake detector default returns no markers`() {
            val detector = FakeMarkerDetector()
            val buffer = ByteBuffer.allocate(100)
            val result = detector.detect(buffer, 640, 480, 0, 0)

            assertFalse(result.isTracking)
            assertEquals(0, result.markerCount)
        }
    }

    @Nested
    inner class DetectedMarkerTests {
        @Test
        fun `marker stores position and confidence`() {
            val marker = DetectedMarker(
                id = 42,
                centerX = 320f,
                centerY = 240f,
                corners = listOf(
                    310f to 230f, 330f to 230f,
                    330f to 250f, 310f to 250f
                ),
                confidence = 0.87f
            )

            assertEquals(42, marker.id)
            assertEquals(320f, marker.centerX)
            assertEquals(240f, marker.centerY)
            assertEquals(4, marker.corners.size)
            assertEquals(0.87f, marker.confidence)
        }
    }
}

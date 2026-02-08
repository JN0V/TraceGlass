package io.github.jn0v.traceglass.core.overlay

import io.github.jn0v.traceglass.core.cv.DetectedMarker
import io.github.jn0v.traceglass.core.cv.MarkerResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TrackingStateManagerTest {

    private fun marker(id: Int = 0) =
        DetectedMarker(id, 100f, 200f, emptyList(), 1f)

    private fun trackingResult() = MarkerResult(listOf(marker()), 5L)
    private fun emptyResult() = MarkerResult(emptyList(), 5L)

    @Nested
    inner class InitialState {
        @Test
        fun `starts as INACTIVE`() {
            val manager = TrackingStateManager()
            assertEquals(TrackingStatus.INACTIVE, manager.status)
        }

        @Test
        fun `stays INACTIVE when no markers detected and never tracked`() {
            val manager = TrackingStateManager()
            manager.onMarkerResult(emptyResult())
            assertEquals(TrackingStatus.INACTIVE, manager.status)
        }
    }

    @Nested
    inner class TrackingTransitions {
        @Test
        fun `transitions to TRACKING when markers detected`() {
            val manager = TrackingStateManager()
            manager.onMarkerResult(trackingResult())
            assertEquals(TrackingStatus.TRACKING, manager.status)
        }

        @Test
        fun `stays TRACKING briefly after markers lost`() {
            var time = 0L
            val manager = TrackingStateManager(
                lostTimeoutMs = 500L,
                timeProvider = { time }
            )
            manager.onMarkerResult(trackingResult())
            time = 200L
            manager.onMarkerResult(emptyResult())
            assertEquals(TrackingStatus.TRACKING, manager.status)
        }

        @Test
        fun `transitions to LOST after timeout`() {
            var time = 0L
            val manager = TrackingStateManager(
                lostTimeoutMs = 500L,
                timeProvider = { time }
            )
            manager.onMarkerResult(trackingResult())
            time = 600L
            manager.onMarkerResult(emptyResult())
            assertEquals(TrackingStatus.LOST, manager.status)
        }

        @Test
        fun `recovers to TRACKING after being LOST`() {
            var time = 0L
            val manager = TrackingStateManager(
                lostTimeoutMs = 500L,
                timeProvider = { time }
            )
            manager.onMarkerResult(trackingResult())
            time = 600L
            manager.onMarkerResult(emptyResult())
            assertEquals(TrackingStatus.LOST, manager.status)

            time = 700L
            manager.onMarkerResult(trackingResult())
            assertEquals(TrackingStatus.TRACKING, manager.status)
        }

        @Test
        fun `partial occlusion with remaining markers stays TRACKING`() {
            val manager = TrackingStateManager()
            val twoMarkers = MarkerResult(listOf(marker(0), marker(1)), 5L)
            manager.onMarkerResult(twoMarkers)

            val oneMarker = MarkerResult(listOf(marker(0)), 5L)
            manager.onMarkerResult(oneMarker)
            assertEquals(TrackingStatus.TRACKING, manager.status)
        }
    }

    @Nested
    inner class Reset {
        @Test
        fun `reset returns to INACTIVE`() {
            val manager = TrackingStateManager()
            manager.onMarkerResult(trackingResult())
            assertEquals(TrackingStatus.TRACKING, manager.status)
            manager.reset()
            assertEquals(TrackingStatus.INACTIVE, manager.status)
        }

        @Test
        fun `after reset empty result stays INACTIVE`() {
            val manager = TrackingStateManager()
            manager.onMarkerResult(trackingResult())
            manager.reset()
            manager.onMarkerResult(emptyResult())
            assertEquals(TrackingStatus.INACTIVE, manager.status)
        }
    }
}

package io.github.jn0v.traceglass.core.overlay

import io.github.jn0v.traceglass.core.cv.MarkerResult

enum class TrackingStatus {
    INACTIVE,
    TRACKING,
    LOST
}

class TrackingStateManager(
    private val lostTimeoutMs: Long = 500L,
    private val timeProvider: () -> Long = { System.nanoTime() / 1_000_000 }
) {
    private var lastTrackingTime: Long = 0L
    private var hasEverTracked: Boolean = false

    var status: TrackingStatus = TrackingStatus.INACTIVE
        private set

    fun onMarkerResult(result: MarkerResult): TrackingStatus {
        val now = timeProvider()

        if (result.isTracking) {
            lastTrackingTime = now
            hasEverTracked = true
            status = TrackingStatus.TRACKING
        } else if (hasEverTracked) {
            val elapsed = now - lastTrackingTime
            status = if (elapsed >= lostTimeoutMs) {
                TrackingStatus.LOST
            } else {
                TrackingStatus.TRACKING
            }
        }

        return status
    }

    fun reset() {
        status = TrackingStatus.INACTIVE
        lastTrackingTime = 0L
        hasEverTracked = false
    }
}

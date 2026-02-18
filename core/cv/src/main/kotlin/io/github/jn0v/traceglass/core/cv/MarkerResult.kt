package io.github.jn0v.traceglass.core.cv

data class MarkerResult(
    val markers: List<DetectedMarker>,
    val detectionTimeMs: Long,
    val frameWidth: Int = 0,
    val frameHeight: Int = 0
) {
    val isTracking: Boolean get() = markers.isNotEmpty()
    val markerCount: Int get() = markers.size
}

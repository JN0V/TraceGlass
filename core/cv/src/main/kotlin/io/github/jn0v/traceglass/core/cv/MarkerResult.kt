package io.github.jn0v.traceglass.core.cv

data class MarkerResult(
    val markers: List<DetectedMarker>,
    val detectionTimeMs: Long
) {
    val isTracking: Boolean get() = markers.isNotEmpty()
    val markerCount: Int get() = markers.size
}

data class DetectedMarker(
    val id: Int,
    val centerX: Float,
    val centerY: Float,
    val corners: List<Pair<Float, Float>>,
    val confidence: Float
)

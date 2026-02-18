package io.github.jn0v.traceglass.core.cv

data class DetectedMarker(
    val id: Int,
    val centerX: Float,
    val centerY: Float,
    val corners: List<Pair<Float, Float>>,
    val confidence: Float
)

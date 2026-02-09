package io.github.jn0v.traceglass.core.overlay

data class OverlayTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f
) {
    companion object {
        val IDENTITY = OverlayTransform()
    }
}

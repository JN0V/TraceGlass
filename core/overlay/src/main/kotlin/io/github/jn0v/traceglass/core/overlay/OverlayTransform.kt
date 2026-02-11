package io.github.jn0v.traceglass.core.overlay

/**
 * Overlay transform result from marker detection.
 *
 * When 4 (or 3+estimated) paper corner markers are available, [paperCornersFrame]
 * contains the smoothed corner positions in camera frame space [TL, TR, BR, BL].
 * The ViewModel uses these to compute a homography that maps the overlay image
 * directly onto the paper quadrilateral.
 *
 * When fewer markers are available, [offsetX]/[offsetY]/[scale]/[rotation] provide
 * an affine fallback (translate + uniform scale + rotation).
 */
data class OverlayTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val paperCornersFrame: List<Pair<Float, Float>>? = null,
    val paperAspectRatio: Float = 0f
) {
    companion object {
        val IDENTITY = OverlayTransform()
    }
}

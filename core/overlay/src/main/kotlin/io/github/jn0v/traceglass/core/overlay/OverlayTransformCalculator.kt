package io.github.jn0v.traceglass.core.overlay

import io.github.jn0v.traceglass.core.cv.MarkerResult
import kotlin.math.sqrt

class OverlayTransformCalculator(
    private val smoothingFactor: Float = 0.3f
) {
    private var referenceSpacing: Float? = null

    fun compute(result: MarkerResult, frameWidth: Float, frameHeight: Float): OverlayTransform {
        if (!result.isTracking) return OverlayTransform.IDENTITY

        val markers = result.markers
        val centerX = markers.map { it.centerX }.average().toFloat()
        val centerY = markers.map { it.centerY }.average().toFloat()

        val offsetX = centerX - frameWidth / 2f
        val offsetY = centerY - frameHeight / 2f

        val scale = if (markers.size >= 2) {
            val spacing = computeMaxSpacing(markers)
            if (referenceSpacing == null) {
                referenceSpacing = spacing
            }
            spacing / referenceSpacing!!
        } else {
            1f
        }

        return OverlayTransform(offsetX, offsetY, scale)
    }

    fun computeSmoothed(
        result: MarkerResult,
        frameWidth: Float,
        frameHeight: Float,
        previous: OverlayTransform
    ): OverlayTransform {
        val target = compute(result, frameWidth, frameHeight)
        return OverlayTransform(
            offsetX = lerp(previous.offsetX, target.offsetX, smoothingFactor),
            offsetY = lerp(previous.offsetY, target.offsetY, smoothingFactor),
            scale = lerp(previous.scale, target.scale, smoothingFactor)
        )
    }

    fun resetReferenceSpacing() {
        referenceSpacing = null
    }

    private fun computeMaxSpacing(
        markers: List<io.github.jn0v.traceglass.core.cv.DetectedMarker>
    ): Float {
        var maxDist = 0f
        for (i in markers.indices) {
            for (j in i + 1 until markers.size) {
                val dx = markers[i].centerX - markers[j].centerX
                val dy = markers[i].centerY - markers[j].centerY
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > maxDist) maxDist = dist
            }
        }
        return maxDist
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float =
        start + (end - start) * fraction
}

package io.github.jn0v.traceglass.core.overlay

import io.github.jn0v.traceglass.core.cv.DetectedMarker
import io.github.jn0v.traceglass.core.cv.MarkerResult
import kotlin.math.atan2
import kotlin.math.sqrt

class OverlayTransformCalculator(
    private val smoothingFactor: Float = 0.12f
) {
    private var referenceSpacing: Float? = null
    private var referenceAngle: Float? = null
    private var referenceEdgeLength: Float? = null
    private var referenceAngleSingle: Float? = null

    // Per-marker offset from midpoint, used to reconstruct virtual midpoint
    // when one marker is lost
    private val markerOffsetFromMidpoint = mutableMapOf<Int, Pair<Float, Float>>()

    private var lastTransform: OverlayTransform = OverlayTransform.IDENTITY

    fun compute(result: MarkerResult, frameWidth: Float, frameHeight: Float): OverlayTransform {
        if (!result.isTracking) return lastTransform

        val markers = result.markers

        val centerX: Float
        val centerY: Float

        if (markers.size >= 2) {
            // True midpoint from all markers
            centerX = markers.map { it.centerX }.average().toFloat()
            centerY = markers.map { it.centerY }.average().toFloat()

            // Store each marker's offset from midpoint for future single-marker recovery
            for (m in markers) {
                markerOffsetFromMidpoint[m.id] = Pair(m.centerX - centerX, m.centerY - centerY)
            }
        } else {
            val marker = markers[0]
            val storedOffset = markerOffsetFromMidpoint[marker.id]
            if (storedOffset != null) {
                // Reconstruct virtual midpoint: marker.center - its known offset from midpoint
                centerX = marker.centerX - storedOffset.first
                centerY = marker.centerY - storedOffset.second
            } else {
                // No previous 2-marker data, use marker center directly
                centerX = marker.centerX
                centerY = marker.centerY
            }
        }

        val offsetX = centerX - frameWidth / 2f
        val offsetY = centerY - frameHeight / 2f

        val scale: Float
        val rotation: Float

        if (markers.size >= 2) {
            val spacing = computeMaxSpacing(markers)
            if (referenceSpacing == null) {
                referenceSpacing = spacing
            }
            scale = spacing / referenceSpacing!!

            val angle = computeAngle(markers)
            if (referenceAngle == null) {
                referenceAngle = angle
            }
            rotation = angle - referenceAngle!!
        } else {
            val marker = markers[0]
            if (marker.corners.size >= 2) {
                val edgeLength = computeEdgeLength(marker)
                if (referenceEdgeLength == null) {
                    referenceEdgeLength = edgeLength
                }
                scale = edgeLength / referenceEdgeLength!!

                val angle = computeSingleMarkerAngle(marker)
                if (referenceAngleSingle == null) {
                    referenceAngleSingle = angle
                }
                rotation = angle - referenceAngleSingle!!
            } else {
                scale = lastTransform.scale
                rotation = lastTransform.rotation
            }
        }

        val transform = OverlayTransform(offsetX, offsetY, scale, rotation)
        lastTransform = transform
        return transform
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
            scale = lerp(previous.scale, target.scale, smoothingFactor),
            rotation = lerp(previous.rotation, target.rotation, smoothingFactor)
        )
    }

    fun resetReference() {
        referenceSpacing = null
        referenceAngle = null
        referenceEdgeLength = null
        referenceAngleSingle = null
        markerOffsetFromMidpoint.clear()
        lastTransform = OverlayTransform.IDENTITY
    }

    private fun computeSingleMarkerAngle(marker: DetectedMarker): Float {
        val dx = marker.corners[1].first - marker.corners[0].first
        val dy = marker.corners[1].second - marker.corners[0].second
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    private fun computeEdgeLength(marker: DetectedMarker): Float {
        val dx = marker.corners[1].first - marker.corners[0].first
        val dy = marker.corners[1].second - marker.corners[0].second
        return sqrt(dx * dx + dy * dy)
    }

    private fun computeAngle(markers: List<DetectedMarker>): Float {
        val sorted = markers.sortedBy { it.id }
        val dx = sorted[1].centerX - sorted[0].centerX
        val dy = sorted[1].centerY - sorted[0].centerY
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    private fun computeMaxSpacing(markers: List<DetectedMarker>): Float {
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

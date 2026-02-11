package io.github.jn0v.traceglass.core.overlay

import io.github.jn0v.traceglass.core.cv.DetectedMarker
import io.github.jn0v.traceglass.core.cv.MarkerResult
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computes overlay transform from detected ArUco markers.
 *
 * Once all 4 markers (IDs 0-3) have been seen at least once, the calculator
 * always returns [paperCornersFrame] — never falls back to affine.
 *
 * Degradation uses frame-to-frame delta estimation:
 * - 4 markers: smooth detected outer corners
 * - 3 markers: compute affine delta (prev→current) from 3 visible, apply to hidden
 * - 2 markers: compute similarity delta from 2 visible, apply to hidden
 * - 1 marker: compute translation delta from 1 visible, apply to hidden
 * - 0 markers: hold last known corners
 */
class OverlayTransformCalculator(
    private val smoothingFactor: Float = 0.12f
) {
    companion object {
        val A4_PAPER_CORNERS_MM = listOf(
            Pair(0f, 0f), Pair(210f, 0f), Pair(210f, 297f), Pair(0f, 297f)
        )
    }

    // Reference geometry: set on first 4-marker detection
    private var referenceCorners: Map<Int, Pair<Float, Float>>? = null
    private var smoothedCorners: MutableMap<Int, Pair<Float, Float>>? = null
    private var referencePaperAR: Float = 0f
    private var calibratedFocalLength: Float? = null

    // Previous frame's smooth values for delta-based estimation
    private var prevSmooth: Map<Int, Pair<Float, Float>>? = null

    // Affine fallback state (only used before reference is established)
    private var referenceSpacing: Float? = null
    private var referenceAngle: Float? = null
    private var referenceEdgeLength: Float? = null
    private var referenceAngleSingle: Float? = null
    private val markerOffsetFromMidpoint = mutableMapOf<Int, Pair<Float, Float>>()

    private var lastTransform: OverlayTransform = OverlayTransform.IDENTITY

    fun compute(
        result: MarkerResult,
        frameWidth: Float,
        frameHeight: Float
    ): OverlayTransform {
        if (!result.isTracking) return lastTransform

        val markers = result.markers
        val markerById = markers.associateBy { it.id }
        val cornerIds = markers.mapNotNull { if (it.id in 0..3) it.id else null }.toSet()

        // If reference is established, always use paper corners path
        if (referenceCorners != null) {
            return computeWithPaperCorners(markerById, cornerIds, frameWidth, frameHeight)
        }

        // First time seeing all 4 → initialize reference
        if (cornerIds.size == 4) {
            return computeWithPaperCorners(markerById, cornerIds, frameWidth, frameHeight)
        }

        // No reference yet, <4 markers → affine fallback
        return computeAffine(markers, frameWidth, frameHeight)
    }

    private fun computeWithPaperCorners(
        markerById: Map<Int, DetectedMarker>,
        cornerIds: Set<Int>,
        frameWidth: Float,
        frameHeight: Float
    ): OverlayTransform {
        val detected = extractOuterCorners(markerById)

        // Initialize reference on first 4-marker detection
        if (referenceCorners == null) {
            if (cornerIds.size < 4) {
                return computeAffine(markerById.values.toList(), frameWidth, frameHeight)
            }
            referenceCorners = detected.toMap()
            smoothedCorners = detected.toMutableMap()

            // Estimate focal length from 4-marker calibration
            val detectedList = (0..3).mapNotNull { detected[it] }
            if (detectedList.size == 4) {
                calibratedFocalLength = HomographySolver.estimateFocalLength(
                    A4_PAPER_CORNERS_MM,
                    detectedList,
                    frameWidth / 2f, frameHeight / 2f
                )
            }

            // Compute paper AR from reference (fixed, never recomputed)
            val tl = detected[0]!!; val tr = detected[1]!!
            val br = detected[2]!!; val bl = detected[3]!!
            val w = (dist(tl, tr) + dist(bl, br)) / 2f
            val h = (dist(tl, bl) + dist(tr, br)) / 2f
            referencePaperAR = if (h > 0.001f) w / h else 1f
        }

        val smooth = smoothedCorners!!
        val alpha = if (smoothingFactor >= 1f) 1f else smoothingFactor

        // Snapshot previous smooth BEFORE updating (for delta estimation)
        val prev = prevSmooth ?: smooth.toMap()

        // Update smoothed positions for all detected corners
        for ((id, corner) in detected) {
            val p = smooth[id] ?: corner
            smooth[id] = Pair(
                lerp(p.first, corner.first, alpha),
                lerp(p.second, corner.second, alpha)
            )
        }

        // Estimate missing corners using frame-to-frame delta
        val missingIds = setOf(0, 1, 2, 3) - cornerIds
        if (missingIds.isNotEmpty() && missingIds.size < 4) {
            estimateFromDelta(cornerIds.sorted(), missingIds, prev, smooth, frameWidth, frameHeight)
        }

        // Store current smooth for next frame's delta
        prevSmooth = smooth.toMap()

        // Build paper corners [TL, TR, BR, BL] from smoothed
        val paperCorners = (0..3).mapNotNull { smooth[it] }
        if (paperCorners.size < 4) {
            return lastTransform
        }

        val centerX = paperCorners.map { it.first }.average().toFloat()
        val centerY = paperCorners.map { it.second }.average().toFloat()

        val transform = OverlayTransform(
            offsetX = centerX - frameWidth / 2f,
            offsetY = centerY - frameHeight / 2f,
            scale = 1f,
            rotation = 0f,
            paperCornersFrame = paperCorners,
            paperAspectRatio = referencePaperAR
        )
        lastTransform = transform
        return transform
    }

    /**
     * Delta-based estimation: compute how visible corners moved since the previous
     * frame (prev→current), then apply the same transform to hidden corners.
     *
     * Frame-to-frame deltas are tiny, so affine/similarity estimation is very accurate.
     *
     * - 3 visible: affine delta (translation + rotation + scale + shear)
     * - 2 visible: similarity delta (translation + rotation + scale)
     * - 1 visible: translation delta
     */
    private fun estimateFromDelta(
        visibleIds: List<Int>,
        missingIds: Set<Int>,
        prev: Map<Int, Pair<Float, Float>>,
        smooth: MutableMap<Int, Pair<Float, Float>>,
        frameWidth: Float = 0f,
        frameHeight: Float = 0f
    ) {
        when {
            visibleIds.size >= 3 -> {
                // Try perspective-correct estimation using paper geometry + focal length
                val f = calibratedFocalLength
                if (f != null && frameWidth > 0f && frameHeight > 0f &&
                    estimateFromPaperGeometry(visibleIds, missingIds, smooth, f, frameWidth, frameHeight)) {
                    return
                }

                // Fallback: affine from prev[visible] → smooth[visible], apply to prev[missing]
                val ids = visibleIds.take(3)
                val srcPts = ids.map { prev[it] ?: return }
                val dstPts = ids.map { smooth[it] ?: return }
                val affine = HomographySolver.solveAffine(srcPts, dstPts) ?: return

                for (id in missingIds) {
                    val p = prev[id] ?: smooth[id] ?: return
                    smooth[id] = Pair(
                        affine[0] * p.first + affine[1] * p.second + affine[2],
                        affine[3] * p.first + affine[4] * p.second + affine[5]
                    )
                }
            }
            visibleIds.size == 2 -> {
                // Similarity from 2 point pairs
                val id0 = visibleIds[0]; val id1 = visibleIds[1]
                val pr0 = prev[id0] ?: return; val pr1 = prev[id1] ?: return
                val cr0 = smooth[id0] ?: return; val cr1 = smooth[id1] ?: return

                val sim = computeSimilarityParams(pr0, pr1, cr0, cr1) ?: return

                for (id in missingIds) {
                    val p = prev[id] ?: smooth[id] ?: return
                    smooth[id] = applySimilarity(sim, p)
                }
            }
            visibleIds.size == 1 -> {
                // Translation only
                val id0 = visibleIds[0]
                val pr = prev[id0] ?: return
                val cr = smooth[id0] ?: return
                val tx = cr.first - pr.first
                val ty = cr.second - pr.second

                for (id in missingIds) {
                    val p = prev[id] ?: smooth[id] ?: return
                    smooth[id] = Pair(p.first + tx, p.second + ty)
                }
            }
        }
    }

    /**
     * Estimate missing corners using known A4 paper geometry + calibrated focal length.
     * Solves a constrained homography from 3 visible paper→frame correspondences,
     * then projects the missing corners through H.
     *
     * @return true if estimation succeeded, false to fall back to affine delta
     */
    private fun estimateFromPaperGeometry(
        visibleIds: List<Int>,
        missingIds: Set<Int>,
        smooth: MutableMap<Int, Pair<Float, Float>>,
        f: Float,
        frameWidth: Float,
        frameHeight: Float
    ): Boolean {
        if (visibleIds.size < 3) return false

        val ids = visibleIds.take(3)
        val paperCoords3 = ids.map { A4_PAPER_CORNERS_MM[it] }
        val frameCoords3 = ids.map { smooth[it] ?: return false }

        val H = HomographySolver.solveConstrainedHomography(
            paperCoords3, frameCoords3, f, frameWidth / 2f, frameHeight / 2f
        ) ?: return false

        for (id in missingIds) {
            val (px, py) = A4_PAPER_CORNERS_MM[id]
            val w = H[6] * px + H[7] * py + H[8]
            if (kotlin.math.abs(w) < 1e-6f) return false
            smooth[id] = Pair(
                (H[0] * px + H[1] * py + H[2]) / w,
                (H[3] * px + H[4] * py + H[5]) / w
            )
        }
        return true
    }

    private data class SimilarityParams(
        val scale: Float, val cosA: Float, val sinA: Float,
        val tx: Float, val ty: Float
    )

    private fun computeSimilarityParams(
        r0: Pair<Float, Float>, r1: Pair<Float, Float>,
        c0: Pair<Float, Float>, c1: Pair<Float, Float>
    ): SimilarityParams? {
        val refMidX = (r0.first + r1.first) / 2f
        val refMidY = (r0.second + r1.second) / 2f
        val curMidX = (c0.first + c1.first) / 2f
        val curMidY = (c0.second + c1.second) / 2f

        val refDx = r1.first - r0.first; val refDy = r1.second - r0.second
        val curDx = c1.first - c0.first; val curDy = c1.second - c0.second

        val refDist = sqrt(refDx * refDx + refDy * refDy)
        val curDist = sqrt(curDx * curDx + curDy * curDy)
        if (refDist < 0.001f) return null
        val scale = curDist / refDist

        val dAngle = atan2(curDy, curDx) - atan2(refDy, refDx)
        val cosA = cos(dAngle); val sinA = sin(dAngle)

        val tx = curMidX - scale * (cosA * refMidX - sinA * refMidY)
        val ty = curMidY - scale * (sinA * refMidX + cosA * refMidY)

        return SimilarityParams(scale, cosA, sinA, tx, ty)
    }

    private fun applySimilarity(s: SimilarityParams, p: Pair<Float, Float>): Pair<Float, Float> {
        return Pair(
            s.scale * (s.cosA * p.first - s.sinA * p.second) + s.tx,
            s.scale * (s.sinA * p.first + s.cosA * p.second) + s.ty
        )
    }

    /**
     * Affine fallback: only used before 4 markers have been seen.
     */
    private fun computeAffine(
        markers: List<DetectedMarker>,
        frameWidth: Float,
        frameHeight: Float
    ): OverlayTransform {
        if (markers.isEmpty()) return lastTransform

        val centerX: Float
        val centerY: Float

        if (markers.size >= 2) {
            centerX = markers.map { it.centerX }.average().toFloat()
            centerY = markers.map { it.centerY }.average().toFloat()
            for (m in markers) {
                markerOffsetFromMidpoint[m.id] = Pair(m.centerX - centerX, m.centerY - centerY)
            }
        } else {
            val marker = markers[0]
            val storedOffset = markerOffsetFromMidpoint[marker.id]
            if (storedOffset != null) {
                centerX = marker.centerX - storedOffset.first
                centerY = marker.centerY - storedOffset.second
            } else {
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
            if (referenceSpacing == null) referenceSpacing = spacing
            scale = spacing / referenceSpacing!!

            val angle = computeAngle(markers)
            if (referenceAngle == null) referenceAngle = angle
            rotation = angle - referenceAngle!!
        } else {
            val marker = markers[0]
            if (marker.corners.size >= 2) {
                val edgeLength = computeEdgeLength(marker)
                if (referenceEdgeLength == null) referenceEdgeLength = edgeLength
                scale = edgeLength / referenceEdgeLength!!

                val angle = computeSingleMarkerAngle(marker)
                if (referenceAngleSingle == null) referenceAngleSingle = angle
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
        if (smoothingFactor >= 1f) return target

        return OverlayTransform(
            offsetX = lerp(previous.offsetX, target.offsetX, smoothingFactor),
            offsetY = lerp(previous.offsetY, target.offsetY, smoothingFactor),
            scale = lerp(previous.scale, target.scale, smoothingFactor),
            rotation = lerp(previous.rotation, target.rotation, smoothingFactor),
            paperCornersFrame = target.paperCornersFrame,
            paperAspectRatio = target.paperAspectRatio
        )
    }

    fun resetReference() {
        referenceSpacing = null
        referenceAngle = null
        referenceEdgeLength = null
        referenceAngleSingle = null
        markerOffsetFromMidpoint.clear()
        referenceCorners = null
        smoothedCorners = null
        referencePaperAR = 0f
        calibratedFocalLength = null
        prevSmooth = null
        lastTransform = OverlayTransform.IDENTITY
    }

    private fun extractOuterCorners(markerById: Map<Int, DetectedMarker>): Map<Int, Pair<Float, Float>> {
        val corners = mutableMapOf<Int, Pair<Float, Float>>()
        for (id in 0..3) {
            val marker = markerById[id] ?: continue
            if (marker.corners.size < 4) continue
            corners[id] = marker.corners[id]
        }
        return corners
    }

    private fun dist(a: Pair<Float, Float>, b: Pair<Float, Float>): Float {
        val dx = b.first - a.first
        val dy = b.second - a.second
        return sqrt(dx * dx + dy * dy)
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

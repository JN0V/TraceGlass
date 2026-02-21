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
 *
 * All methods must be called from the main thread (composable `LaunchedEffect`
 * collecting `StateFlow`). Focal length is auto-estimated from marker geometry
 * when the reference is near fronto-parallel.
 */
class OverlayTransformCalculator(
    private val smoothingFactor: Float = 0.12f
) {
    // Reference geometry: set on first 4-marker detection
    private var referenceCorners: Map<Int, Pair<Float, Float>>? = null
    private var smoothedCorners: MutableMap<Int, Pair<Float, Float>>? = null
    private var referencePaperAR: Float = 0f
    private var calibratedFocalLength: Float? = null
    // Rectangular paper coords for constrained homography (paper-size agnostic)
    private var calibratedPaperCorners: List<Pair<Float, Float>>? = null
    private var needsRebuildPaperCoords: Boolean = false
    // True when reference was near fronto-parallel (paper coords approximate a rectangle).
    // Auto-f-estimation only works when this is true.
    private var isReferenceRectangular: Boolean = false

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

            val tl = detected[0]!!; val tr = detected[1]!!
            val br = detected[2]!!; val bl = detected[3]!!
            calibratedPaperCorners = listOf(tl, tr, br, bl)
            calibratedFocalLength = null

            val w = (dist(tl, tr) + dist(bl, br)) / 2f
            val h = (dist(tl, bl) + dist(tr, br)) / 2f
            referencePaperAR = if (h > 0.001f) w / h else 1f

            // Check if reference is near fronto-parallel (top/bottom and left/right
            // edges approximately equal). Auto-f-estimation only works in this case
            // because the paper coords approximate a real physical rectangle.
            val topEdge = dist(tl, tr); val bottomEdge = dist(bl, br)
            val leftEdge = dist(tl, bl); val rightEdge = dist(tr, br)
            val hRatio = if (bottomEdge > 0.1f) topEdge / bottomEdge else 1f
            val vRatio = if (rightEdge > 0.1f) leftEdge / rightEdge else 1f
            // Threshold 0.08 allows up to ~5° tilt. Beyond that, the paper coords
            // are too distorted for reliable focal length auto-estimation.
            isReferenceRectangular = kotlin.math.abs(hRatio - 1f) < 0.08f &&
                                    kotlin.math.abs(vRatio - 1f) < 0.08f
        }

        // Auto-estimate focal length when reference is rectangular and all 4 visible
        if (calibratedFocalLength == null && isReferenceRectangular && cornerIds.size == 4) {
            val paper = calibratedPaperCorners
            val allDetected = (0..3).mapNotNull { detected[it] }
            if (paper != null && allDetected.size == 4) {
                val estimatedF = HomographySolver.estimateFocalLength(
                    paper, allDetected, frameWidth / 2f, frameHeight / 2f
                )
                if (estimatedF != null) {
                    calibratedFocalLength = estimatedF
                    needsRebuildPaperCoords = true
                }
            }
        }

        // Rebuild paper coords as proper rectangle when f becomes available
        if (needsRebuildPaperCoords) {
            rebuildPaperCoords(frameWidth, frameHeight)
        }

        val smooth = smoothedCorners ?: return lastTransform
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
        frameWidth: Float,
        frameHeight: Float
    ) {
        // Estimated positions are collected into a temporary map first, then applied
        // to [smooth] only if ALL missing corners were computed successfully.
        // This prevents partial state corruption when a lookup fails mid-loop.
        val estimated = mutableMapOf<Int, Pair<Float, Float>>()

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
                    estimated[id] = Pair(
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
                    estimated[id] = applySimilarity(sim, p)
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
                    estimated[id] = Pair(p.first + tx, p.second + ty)
                }
            }
            // 0 visible: hold last known positions (smooth unchanged)
        }

        // Batch apply: write all estimated positions only if all succeeded.
        // Not thread-safe (single-threaded caller assumed), but avoids partial writes.
        smooth.putAll(estimated)
    }

    /**
     * Estimate missing corners using calibrated rectangle geometry + focal length.
     * Works with any paper size — the rectangle is computed from the initial 4-marker detection.
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
        val paper = calibratedPaperCorners ?: return false

        val ids = visibleIds.take(3)
        val paperCoords3 = ids.map { paper[it] }
        val frameCoords3 = ids.map { smooth[it] ?: return false }

        val H = HomographySolver.solveConstrainedHomography(
            paperCoords3, frameCoords3, f, frameWidth / 2f, frameHeight / 2f
        ) ?: return false

        // Compute all missing corners into a temp map before writing to smooth,
        // so a mid-loop failure leaves smooth unchanged.
        val estimated = mutableMapOf<Int, Pair<Float, Float>>()
        for (id in missingIds) {
            val (px, py) = paper[id]
            val w = H[6] * px + H[7] * py + H[8]
            if (kotlin.math.abs(w) < 1e-6f) return false
            estimated[id] = Pair(
                (H[0] * px + H[1] * py + H[2]) / w,
                (H[3] * px + H[4] * py + H[5]) / w
            )
        }
        smooth.putAll(estimated)
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
            val refSpacing = referenceSpacing ?: spacing
            scale = if (refSpacing > 0.001f) spacing / refSpacing else 1f

            val angle = computeAngle(markers)
            if (referenceAngle == null) referenceAngle = angle
            rotation = angle - (referenceAngle ?: angle)
        } else {
            val marker = markers[0]
            if (marker.corners.size >= 2) {
                val edgeLength = computeEdgeLength(marker)
                if (referenceEdgeLength == null) referenceEdgeLength = edgeLength
                val refEdge = referenceEdgeLength ?: edgeLength
                scale = if (refEdge > 0.001f) edgeLength / refEdge else 1f

                val angle = computeSingleMarkerAngle(marker)
                if (referenceAngleSingle == null) referenceAngleSingle = angle
                rotation = angle - (referenceAngleSingle ?: angle)
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

        // Paper corners path: corners are already smoothed via internal EMA in compute().
        // Skip outer EMA to avoid double-smoothing; rendering uses paperCornersFrame
        // homography, not the fallback offset/scale/rotation values.
        if (target.paperCornersFrame != null) return target

        return OverlayTransform(
            offsetX = lerp(previous.offsetX, target.offsetX, smoothingFactor),
            offsetY = lerp(previous.offsetY, target.offsetY, smoothingFactor),
            scale = lerp(previous.scale, target.scale, smoothingFactor),
            rotation = lerp(previous.rotation, target.rotation, smoothingFactor),
            paperCornersFrame = target.paperCornersFrame,
            paperAspectRatio = target.paperAspectRatio
        )
    }

    /**
     * Rebuild paper coords as a proper rectangle using the known focal length.
     * The initial detected positions are perspectively distorted; using them as-is
     * violates the planar assumption in solveConstrainedHomography.
     * With known f, we can correct the aspect ratio to recover the true rectangle.
     */
    private fun rebuildPaperCoords(frameWidth: Float, frameHeight: Float) {
        needsRebuildPaperCoords = false
        val f = calibratedFocalLength ?: return
        val ref = referenceCorners ?: return
        val corners = (0..3).mapNotNull { ref[it] }
        if (corners.size != 4) return

        val tl = corners[0]; val tr = corners[1]
        val br = corners[2]; val bl = corners[3]

        // Edge-length rectangle matching detected shape
        val w = (dist(tl, tr) + dist(bl, br)) / 2f
        val h = (dist(tl, bl) + dist(tr, br)) / 2f
        if (w < 1f || h < 1f) return

        val centX = (tl.first + tr.first + br.first + bl.first) / 4f
        val centY = (tl.second + tr.second + br.second + bl.second) / 4f

        val edgeRect = listOf(
            Pair(centX - w / 2, centY - h / 2), Pair(centX + w / 2, centY - h / 2),
            Pair(centX + w / 2, centY + h / 2), Pair(centX - w / 2, centY + h / 2)
        )

        // Correct AR using known f (norm ratio of K⁻¹·H columns)
        val correctedAR = HomographySolver.correctAspectRatio(
            edgeRect, listOf(tl, tr, br, bl), f, frameWidth / 2f, frameHeight / 2f
        )

        if (correctedAR != null && correctedAR > 0.1f && correctedAR < 10f) {
            val correctedH = w / correctedAR
            calibratedPaperCorners = listOf(
                Pair(centX - w / 2, centY - correctedH / 2),
                Pair(centX + w / 2, centY - correctedH / 2),
                Pair(centX + w / 2, centY + correctedH / 2),
                Pair(centX - w / 2, centY + correctedH / 2)
            )
            referencePaperAR = correctedAR
        }
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
        calibratedPaperCorners = null
        isReferenceRectangular = false
        prevSmooth = null
        lastTransform = OverlayTransform.IDENTITY
        calibratedFocalLength = null
        needsRebuildPaperCoords = false
    }

    /**
     * Extracts one outer corner per paper-corner marker.
     *
     * Convention: marker ID matches its paper position (TL=0, TR=1, BR=2, BL=3).
     * ArUco returns corners in the same order, so `marker.corners[id]` is the
     * physical outer corner closest to the paper corner where the marker sits.
     *
     * The `corners.size < 4` guard ensures the index is always in bounds.
     */
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

    // Note: max pairwise distance is used for scale. If the visible marker set changes
    // (e.g. markers 0,1 → markers 0,3), the scale may jump because the max distance
    // differs. This is acceptable for the affine fallback path (pre-reference only).
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

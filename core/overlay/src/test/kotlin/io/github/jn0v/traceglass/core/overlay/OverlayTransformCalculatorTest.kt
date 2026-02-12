package io.github.jn0v.traceglass.core.overlay

import io.github.jn0v.traceglass.core.cv.DetectedMarker
import io.github.jn0v.traceglass.core.cv.MarkerResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class OverlayTransformCalculatorTest {

    private val calculator = OverlayTransformCalculator()

    /** Creates a marker without corners (legacy helper for offset-only tests). */
    private fun marker(id: Int, cx: Float, cy: Float): DetectedMarker =
        DetectedMarker(id, cx, cy, emptyList(), 1f)

    /**
     * Creates a marker with realistic 4-corner geometry.
     * @param id marker id
     * @param cx center x
     * @param cy center y
     * @param size edge length in pixels
     * @param angleDeg rotation in degrees (0 = horizontal top edge)
     */
    private fun markerWithCorners(
        id: Int, cx: Float, cy: Float,
        size: Float = 50f, angleDeg: Float = 0f
    ): DetectedMarker {
        val halfSize = size / 2f
        val rad = Math.toRadians(angleDeg.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        // Corners: top-left, top-right, bottom-right, bottom-left (ArUco order)
        val corners = listOf(
            Pair(cx - halfSize * cosA + halfSize * sinA, cy - halfSize * sinA - halfSize * cosA), // TL
            Pair(cx + halfSize * cosA + halfSize * sinA, cy + halfSize * sinA - halfSize * cosA), // TR
            Pair(cx + halfSize * cosA - halfSize * sinA, cy + halfSize * sinA + halfSize * cosA), // BR
            Pair(cx - halfSize * cosA - halfSize * sinA, cy - halfSize * sinA + halfSize * cosA)  // BL
        )
        return DetectedMarker(id, cx, cy, corners, 1f)
    }

    @Nested
    inner class NoMarkers {
        @Test
        fun `returns identity when no markers ever detected`() {
            val result = MarkerResult(emptyList(), 5L)
            val transform = calculator.compute(result, 1080f, 1920f)
            assertEquals(OverlayTransform.IDENTITY, transform)
        }

        @Test
        fun `returns last known transform when markers lost after tracking`() {
            val calc = OverlayTransformCalculator()
            // First: track a marker
            val tracked = MarkerResult(
                listOf(markerWithCorners(0, 300f, 500f)), 5L, 1080, 1920
            )
            val first = calc.compute(tracked, 1080f, 1920f)
            assertTrue(first.offsetX != 0f || first.offsetY != 0f)

            // Then: lose all markers
            val lost = MarkerResult(emptyList(), 5L)
            val holdTransform = calc.compute(lost, 1080f, 1920f)

            // Should hold last transform, not reset to IDENTITY
            assertEquals(first.offsetX, holdTransform.offsetX, 0.01f)
            assertEquals(first.offsetY, holdTransform.offsetY, 0.01f)
            assertEquals(first.scale, holdTransform.scale, 0.01f)
            assertEquals(first.rotation, holdTransform.rotation, 0.01f)
        }
    }

    @Nested
    inner class SingleMarker {
        @Test
        fun `centers overlay on single marker position`() {
            val result = MarkerResult(listOf(marker(0, 300f, 500f)), 5L)
            val transform = calculator.compute(result, 1080f, 1920f)
            assertEquals(300f - 1080f / 2f, transform.offsetX, 0.01f)
            assertEquals(500f - 1920f / 2f, transform.offsetY, 0.01f)
        }

        @Test
        fun `single marker with no corners keeps last scale and rotation`() {
            val result = MarkerResult(listOf(marker(0, 540f, 960f)), 5L)
            val transform = calculator.compute(result, 1080f, 1920f)
            // No corners, so falls back to lastTransform defaults
            assertEquals(1f, transform.scale, 0.01f)
            assertEquals(0f, transform.rotation, 0.01f)
        }

        @Test
        fun `single marker with corners computes rotation`() {
            val calc = OverlayTransformCalculator()
            // Marker rotated 30 degrees
            val m = markerWithCorners(0, 540f, 960f, 50f, 30f)
            val result = MarkerResult(listOf(m), 5L, 1080, 1920)
            val first = calc.compute(result, 1080f, 1920f)
            // First detection sets reference → rotation = 0
            assertEquals(0f, first.rotation, 0.1f)

            // Now rotate to 60 degrees
            val m2 = markerWithCorners(0, 540f, 960f, 50f, 60f)
            val second = calc.compute(MarkerResult(listOf(m2), 5L, 1080, 1920), 1080f, 1920f)
            // Should be ~30 degrees relative to reference
            assertEquals(30f, second.rotation, 1f)
        }

        @Test
        fun `single marker with corners computes scale from edge length`() {
            val calc = OverlayTransformCalculator()
            // First detection: size 50
            val m1 = markerWithCorners(0, 540f, 960f, 50f)
            calc.compute(MarkerResult(listOf(m1), 5L, 1080, 1920), 1080f, 1920f)

            // Second: size 100 (double)
            val m2 = markerWithCorners(0, 540f, 960f, 100f)
            val transform = calc.compute(MarkerResult(listOf(m2), 5L, 1080, 1920), 1080f, 1920f)
            assertEquals(2f, transform.scale, 0.1f)
        }

        @Test
        fun `first single-marker detection sets reference scale to 1`() {
            val calc = OverlayTransformCalculator()
            val m = markerWithCorners(0, 540f, 960f, 50f)
            val transform = calc.compute(MarkerResult(listOf(m), 5L, 1080, 1920), 1080f, 1920f)
            assertEquals(1f, transform.scale, 0.01f)
        }
    }

    @Nested
    inner class TwoMarkers {
        @Test
        fun `centers on midpoint of two markers`() {
            val m1 = marker(0, 200f, 400f)
            val m2 = marker(1, 600f, 800f)
            val result = MarkerResult(listOf(m1, m2), 5L)
            val transform = calculator.compute(result, 1080f, 1920f)
            assertEquals(400f - 1080f / 2f, transform.offsetX, 0.01f)
            assertEquals(600f - 1920f / 2f, transform.offsetY, 0.01f)
        }

        @Test
        fun `scale is proportional to marker spacing`() {
            val m1 = marker(0, 100f, 100f)
            val m2 = marker(1, 500f, 100f)
            val result = MarkerResult(listOf(m1, m2), 5L)
            val transform = calculator.compute(result, 1080f, 1920f)

            val m1b = marker(0, 100f, 100f)
            val m2b = marker(1, 900f, 100f)
            val resultB = MarkerResult(listOf(m1b, m2b), 5L)
            val transformB = calculator.compute(resultB, 1080f, 1920f)

            assertEquals(transformB.scale / transform.scale, 2f, 0.01f)
        }

        @Test
        fun `reference spacing set on first detection`() {
            val calc = OverlayTransformCalculator()
            val m1 = marker(0, 100f, 100f)
            val m2 = marker(1, 500f, 100f)
            val result = MarkerResult(listOf(m1, m2), 5L)
            val transform = calc.compute(result, 1080f, 1920f)
            assertEquals(1f, transform.scale, 0.01f)
        }
    }

    @Nested
    inner class Rotation {
        @Test
        fun `first detection sets reference angle with zero rotation`() {
            val calc = OverlayTransformCalculator()
            val m1 = marker(0, 100f, 100f)
            val m2 = marker(1, 500f, 100f) // horizontal
            val result = MarkerResult(listOf(m1, m2), 5L)
            val transform = calc.compute(result, 1080f, 1920f)
            assertEquals(0f, transform.rotation, 0.01f)
        }

        @Test
        fun `rotation changes relative to reference angle`() {
            val calc = OverlayTransformCalculator()
            // First: horizontal (0 degrees)
            val m1 = marker(0, 100f, 500f)
            val m2 = marker(1, 500f, 500f)
            calc.compute(MarkerResult(listOf(m1, m2), 5L), 1080f, 1920f)

            // Second: 45 degrees
            val m1b = marker(0, 100f, 100f)
            val m2b = marker(1, 500f, 500f)
            val transform = calc.compute(MarkerResult(listOf(m1b, m2b), 5L), 1080f, 1920f)
            assertEquals(45f, transform.rotation, 0.1f)
        }

        @Test
        fun `resetReference clears rotation reference`() {
            val calc = OverlayTransformCalculator()
            val m1 = marker(0, 100f, 100f)
            val m2 = marker(1, 500f, 500f)
            calc.compute(MarkerResult(listOf(m1, m2), 5L), 1080f, 1920f)

            calc.resetReference()

            // After reset, next detection becomes new reference (rotation = 0)
            val transform = calc.compute(MarkerResult(listOf(m1, m2), 5L), 1080f, 1920f)
            assertEquals(0f, transform.rotation, 0.01f)
        }
    }

    @Nested
    inner class MarkerTransitions {
        @Test
        fun `2-to-1 marker transition preserves approximate rotation`() {
            val calc = OverlayTransformCalculator()
            // Start with 2 markers: horizontal
            val m0 = markerWithCorners(0, 200f, 500f, 50f, 0f)
            val m1 = markerWithCorners(1, 800f, 500f, 50f, 0f)
            val twoResult = MarkerResult(listOf(m0, m1), 5L, 1080, 1920)
            val twoTransform = calc.compute(twoResult, 1080f, 1920f)

            // Lose marker 1, keep marker 0
            val oneResult = MarkerResult(listOf(m0), 5L, 1080, 1920)
            val oneTransform = calc.compute(oneResult, 1080f, 1920f)

            // Rotation should be close to 0 (first single-marker detection sets its own reference)
            assertEquals(0f, oneTransform.rotation, 1f)
            // No large jump — both should be ~0
            assertTrue(abs(twoTransform.rotation - oneTransform.rotation) < 5f,
                "Rotation jump too large: ${twoTransform.rotation} -> ${oneTransform.rotation}")
        }

        @Test
        fun `2-to-1 marker transition preserves offset via virtual midpoint`() {
            val calc = OverlayTransformCalculator()
            // 2 markers: m0 at x=200, m1 at x=800 → midpoint at x=500
            val m0 = markerWithCorners(0, 200f, 500f, 50f, 0f)
            val m1 = markerWithCorners(1, 800f, 500f, 50f, 0f)
            val twoTransform = calc.compute(
                MarkerResult(listOf(m0, m1), 5L, 1080, 1920), 1080f, 1920f
            )

            // Lose marker 1 → should reconstruct midpoint from m0 + stored offset
            val oneTransform = calc.compute(
                MarkerResult(listOf(m0), 5L, 1080, 1920), 1080f, 1920f
            )

            // Offset should NOT jump: virtual midpoint = m0.center - storedOffset = 200 - (-300) = 500
            assertEquals(twoTransform.offsetX, oneTransform.offsetX, 1f,
                "Offset X jumped on 2→1 transition: ${twoTransform.offsetX} -> ${oneTransform.offsetX}")
            assertEquals(twoTransform.offsetY, oneTransform.offsetY, 1f,
                "Offset Y jumped on 2→1 transition")
        }

        @Test
        fun `2-to-1-to-2 round trip returns approximately same transform`() {
            val calc = OverlayTransformCalculator()
            // Start: 2 markers
            val m0 = markerWithCorners(0, 200f, 500f, 50f, 0f)
            val m1 = markerWithCorners(1, 800f, 500f, 50f, 0f)
            val initial = calc.compute(
                MarkerResult(listOf(m0, m1), 5L, 1080, 1920), 1080f, 1920f
            )

            // Drop to 1 marker
            calc.compute(
                MarkerResult(listOf(m0), 5L, 1080, 1920), 1080f, 1920f
            )

            // Return to 2 markers (same positions)
            val recovered = calc.compute(
                MarkerResult(listOf(m0, m1), 5L, 1080, 1920), 1080f, 1920f
            )

            assertEquals(initial.offsetX, recovered.offsetX, 1f)
            assertEquals(initial.offsetY, recovered.offsetY, 1f)
            assertEquals(initial.scale, recovered.scale, 0.1f)
            assertEquals(initial.rotation, recovered.rotation, 1f)
        }

        @Test
        fun `0-to-1 marker recovery returns valid transform`() {
            val calc = OverlayTransformCalculator()
            // Start with no markers
            val empty = calc.compute(MarkerResult(emptyList(), 5L), 1080f, 1920f)
            assertEquals(OverlayTransform.IDENTITY, empty)

            // Then detect a marker
            val m = markerWithCorners(0, 300f, 700f, 50f, 15f)
            val recovered = calc.compute(
                MarkerResult(listOf(m), 5L, 1080, 1920), 1080f, 1920f
            )

            // Should have valid offset
            assertEquals(300f - 540f, recovered.offsetX, 0.01f)
            assertEquals(700f - 960f, recovered.offsetY, 0.01f)
            // First detection: reference set, so rotation and scale = initial values
            assertEquals(0f, recovered.rotation, 0.1f)
            assertEquals(1f, recovered.scale, 0.01f)
        }

        @Test
        fun `all markers lost holds last transform not identity`() {
            val calc = OverlayTransformCalculator()
            val m = markerWithCorners(0, 300f, 700f, 50f)
            val tracked = calc.compute(
                MarkerResult(listOf(m), 5L, 1080, 1920), 1080f, 1920f
            )
            assertTrue(tracked.offsetX != 0f)

            val held = calc.compute(MarkerResult(emptyList(), 5L), 1080f, 1920f)
            assertEquals(tracked, held)

            // Multiple empty frames should keep holding
            val held2 = calc.compute(MarkerResult(emptyList(), 5L), 1080f, 1920f)
            assertEquals(tracked, held2)
        }
    }

    @Nested
    inner class Smoothing {
        @Test
        fun `smoothed transform converges to target over iterations`() {
            val calc = OverlayTransformCalculator(smoothingFactor = 0.5f)
            val m = markerWithCorners(0, 800f, 960f, 50f)
            val result = MarkerResult(listOf(m), 5L, 1080, 1920)

            var transform = OverlayTransform.IDENTITY
            repeat(20) {
                transform = calc.computeSmoothed(result, 1080f, 1920f, transform)
            }

            val targetX = 800f - 1080f / 2f
            assertEquals(targetX, transform.offsetX, 1f)
        }

        @Test
        fun `smoothing factor 1 gives instant tracking`() {
            val calc = OverlayTransformCalculator(smoothingFactor = 1f)
            val m = markerWithCorners(0, 800f, 960f, 50f)
            val result = MarkerResult(listOf(m), 5L, 1080, 1920)

            val transform = calc.computeSmoothed(result, 1080f, 1920f, OverlayTransform.IDENTITY)

            val targetX = 800f - 1080f / 2f
            assertEquals(targetX, transform.offsetX, 0.01f)
        }

        @Test
        fun `smoothed transform does not overshoot`() {
            val calc = OverlayTransformCalculator(smoothingFactor = 0.12f)
            val m = markerWithCorners(0, 800f, 960f, 50f)
            val result = MarkerResult(listOf(m), 5L, 1080, 1920)
            val targetX = 800f - 1080f / 2f

            var prev = OverlayTransform.IDENTITY
            repeat(50) {
                val next = calc.computeSmoothed(result, 1080f, 1920f, prev)
                // Should always be between previous and target (no overshoot)
                if (targetX > 0) {
                    assertTrue(next.offsetX >= prev.offsetX,
                        "Overshoot detected at iteration $it: ${prev.offsetX} -> ${next.offsetX}")
                    assertTrue(next.offsetX <= targetX + 1f,
                        "Overshoot past target at iteration $it: ${next.offsetX} > $targetX")
                }
                prev = next
            }
        }
    }

    /**
     * Tests for delta-based corner estimation when markers are hidden.
     * Verifies that the 4th (or 3rd/2nd) corner can be accurately estimated
     * from the remaining visible markers using frame-to-frame delta transforms.
     *
     * Convention: marker IDs 0-3 map to paper corners TL, TR, BR, BL.
     * extractOuterCorners uses marker.corners[id] as the paper corner position.
     */
    @Nested
    inner class CornerEstimation {

        private val frameW = 1280f
        private val frameH = 720f
        private val paperW = 400f
        private val paperH = 566f // 400 * 297/210 ≈ A4 ratio

        /** A4 paper corners centered in frame: [TL, TR, BR, BL]. */
        private fun a4Corners(): List<Pair<Float, Float>> {
            val cx = frameW / 2f; val cy = frameH / 2f
            return listOf(
                Pair(cx - paperW / 2, cy - paperH / 2),
                Pair(cx + paperW / 2, cy - paperH / 2),
                Pair(cx + paperW / 2, cy + paperH / 2),
                Pair(cx - paperW / 2, cy + paperH / 2)
            )
        }

        /**
         * Creates a marker where corners[id] is exactly at [paperCorner].
         * The marker is axis-aligned with the given size.
         */
        private fun markerAtPaperCorner(
            id: Int,
            paperCorner: Pair<Float, Float>,
            markerSize: Float = 30f
        ): DetectedMarker {
            val s = markerSize / 2f
            val offsets = listOf(
                Pair(-s, -s), Pair(+s, -s), Pair(+s, +s), Pair(-s, +s)
            )
            val cx = paperCorner.first - offsets[id].first
            val cy = paperCorner.second - offsets[id].second
            val corners = offsets.map { (dx, dy) -> Pair(cx + dx, cy + dy) }
            return DetectedMarker(id, cx, cy, corners, 1f)
        }

        private fun markersForCorners(corners: List<Pair<Float, Float>>): List<DetectedMarker> =
            (0..3).map { markerAtPaperCorner(it, corners[it]) }

        private fun rotateCorners(
            corners: List<Pair<Float, Float>>, angleDeg: Float
        ): List<Pair<Float, Float>> {
            val cx = corners.map { it.first }.average().toFloat()
            val cy = corners.map { it.second }.average().toFloat()
            val rad = Math.toRadians(angleDeg.toDouble())
            val c = cos(rad).toFloat(); val s = sin(rad).toFloat()
            return corners.map { (x, y) ->
                val dx = x - cx; val dy = y - cy
                Pair(cx + dx * c - dy * s, cy + dx * s + dy * c)
            }
        }

        /**
         * Simulates perspective tilt: paper rotated around its horizontal center axis.
         * Positive [tiltDeg] tilts the paper top away from camera (top shrinks, bottom grows).
         */
        private fun perspectiveTilt(
            corners: List<Pair<Float, Float>>,
            tiltDeg: Float,
            focalLength: Float = 800f
        ): List<Pair<Float, Float>> {
            val cx = corners.map { it.first }.average().toFloat()
            val cy = corners.map { it.second }.average().toFloat()
            val rad = Math.toRadians(tiltDeg.toDouble())
            val cosT = cos(rad).toFloat(); val sinT = sin(rad).toFloat()
            return corners.map { (x, y) ->
                val dx = x - cx; val dy = y - cy
                val y3d = dy * cosT
                val z3d = -dy * sinT
                val depth = focalLength + z3d
                if (depth < 1f) Pair(x, y)
                else Pair(cx + dx * focalLength / depth, cy + y3d * focalLength / depth)
            }
        }

        private fun errorPx(a: Pair<Float, Float>, b: Pair<Float, Float>): Float {
            val dx = a.first - b.first; val dy = a.second - b.second
            return sqrt(dx * dx + dy * dy)
        }

        private fun f(v: Float) = "%.2f".format(v)

        private fun assertCornerClose(
            expected: Pair<Float, Float>, actual: Pair<Float, Float>,
            tolerance: Float, msg: String = ""
        ) {
            val dx = abs(expected.first - actual.first)
            val dy = abs(expected.second - actual.second)
            assertTrue(dx < tolerance && dy < tolerance,
                "$msg expected (${f(expected.first)}, ${f(expected.second)}), " +
                    "got (${f(actual.first)}, ${f(actual.second)}), " +
                    "delta=(${f(dx)}, ${f(dy)}) tolerance=$tolerance")
        }

        /** Feeds all 4 markers for 2 frames to establish reference + prevSmooth. */
        private fun initCalculator(corners: List<Pair<Float, Float>>): OverlayTransformCalculator {
            val calc = OverlayTransformCalculator(smoothingFactor = 1f)
            val mr = MarkerResult(markersForCorners(corners), 0L, frameW.toInt(), frameH.toInt())
            calc.compute(mr, frameW, frameH) // frame 1: set reference
            calc.compute(mr, frameW, frameH) // frame 2: set prevSmooth
            return calc
        }

        /**
         * Calibrates in two phases:
         * 1. Fronto-parallel 4 markers → correct rectangle geometry
         * 2. Tilted 4 markers → focal length estimation
         * This matches real usage: phone starts roughly overhead, then tilts.
         */
        private fun initCalibratedCalculator(
            corners: List<Pair<Float, Float>>,
            calibrationTiltDeg: Float = 10f
        ): OverlayTransformCalculator {
            val calc = OverlayTransformCalculator(smoothingFactor = 1f)
            // Frame 1: fronto-parallel → sets reference + correct rectangle, f=null
            val flat = MarkerResult(markersForCorners(corners), 0L, frameW.toInt(), frameH.toInt())
            calc.compute(flat, frameW, frameH)
            // Frame 2: tilted with all 4 → re-estimates focal length
            val tilted = perspectiveTilt(corners, calibrationTiltDeg)
            val tiltedMr = MarkerResult(markersForCorners(tilted), 0L, frameW.toInt(), frameH.toInt())
            calc.compute(tiltedMr, frameW, frameH)
            // Frame 3: same tilted → sets prevSmooth
            calc.compute(tiltedMr, frameW, frameH)
            return calc
        }

        /**
         * Realistic scenario: first detection is TILTED (no fronto-parallel warmup).
         * This is what happens on a real device — the phone is already at an angle
         * when markers are first detected.
         */
        private fun initTiltedCalculator(
            corners: List<Pair<Float, Float>>,
            tiltDeg: Float = 15f
        ): OverlayTransformCalculator {
            val calc = OverlayTransformCalculator(smoothingFactor = 1f)
            val tilted = perspectiveTilt(corners, tiltDeg)
            val mr = MarkerResult(markersForCorners(tilted), 0L, frameW.toInt(), frameH.toInt())
            calc.compute(mr, frameW, frameH) // frame 1: tilted → sets reference
            calc.compute(mr, frameW, frameH) // frame 2: same → prevSmooth
            return calc
        }

        // ── 3-marker affine estimation ──────────────────────────────

        @Test
        fun `3 markers - static scene hides each corner in turn`() {
            val corners = a4Corners()
            for (hiddenId in 0..3) {
                val calc = initCalculator(corners)
                val visible = markersForCorners(corners).filter { it.id != hiddenId }
                val result = calc.compute(
                    MarkerResult(visible, 0L, frameW.toInt(), frameH.toInt()), frameW, frameH
                )
                assertCornerClose(
                    corners[hiddenId], result.paperCornersFrame!![hiddenId], 0.1f,
                    "Static, hidden=$hiddenId:"
                )
            }
        }

        @Test
        fun `3 markers - pure translation estimates exactly`() {
            val corners = a4Corners()
            val moved = corners.map { (x, y) -> Pair(x + 50f, y + 30f) }
            for (hiddenId in 0..3) {
                val calc = initCalculator(corners)
                val visible = markersForCorners(moved).filter { it.id != hiddenId }
                val result = calc.compute(
                    MarkerResult(visible, 0L, frameW.toInt(), frameH.toInt()), frameW, frameH
                )
                assertCornerClose(
                    moved[hiddenId], result.paperCornersFrame!![hiddenId], 0.5f,
                    "Translation, hidden=$hiddenId:"
                )
            }
        }

        @Test
        fun `3 markers - rotation 15 degrees estimates exactly`() {
            val corners = a4Corners()
            val rotated = rotateCorners(corners, 15f)
            for (hiddenId in 0..3) {
                val calc = initCalculator(corners)
                val visible = markersForCorners(rotated).filter { it.id != hiddenId }
                val result = calc.compute(
                    MarkerResult(visible, 0L, frameW.toInt(), frameH.toInt()), frameW, frameH
                )
                assertCornerClose(
                    rotated[hiddenId], result.paperCornersFrame!![hiddenId], 1f,
                    "Rotation 15°, hidden=$hiddenId:"
                )
            }
        }

        @Test
        fun `3 markers - perspective tilt 20 degrees single step`() {
            // With perspective-correct estimation using paper geometry + focal length,
            // the 4th corner is projected through the constrained homography.
            // Error drops from ~100px (affine) to <5px (perspective-correct).
            // Calibration at 10° tilt provides the focal length estimate.
            val corners = a4Corners()
            val tilted = perspectiveTilt(corners, 20f)
            for (hiddenId in 0..3) {
                val calc = initCalibratedCalculator(corners)
                val visible = markersForCorners(tilted).filter { it.id != hiddenId }
                val result = calc.compute(
                    MarkerResult(visible, 0L, frameW.toInt(), frameH.toInt()), frameW, frameH
                )
                val err = errorPx(tilted[hiddenId], result.paperCornersFrame!![hiddenId])
                assertTrue(err < 5f,
                    "Tilt 20° single-step, hidden=$hiddenId: error=${f(err)}px")
            }
        }

        @Test
        fun `3 markers - gradual perspective tilt vs single step`() {
            val corners = a4Corners()
            val totalTilt = 20f
            val steps = 20
            val hiddenId = 2 // hide BR corner

            // Gradual: 20 steps of 1°, always hiding marker 2
            val calcGradual = initCalibratedCalculator(corners)
            for (step in 1..steps) {
                val tilted = perspectiveTilt(corners, step.toFloat())
                val visible = markersForCorners(tilted).filter { it.id != hiddenId }
                calcGradual.compute(
                    MarkerResult(visible, 0L, frameW.toInt(), frameH.toInt()), frameW, frameH
                )
            }
            val finalTilted = perspectiveTilt(corners, totalTilt)
            val gradualResult = calcGradual.compute(
                MarkerResult(
                    markersForCorners(finalTilted).filter { it.id != hiddenId },
                    0L, frameW.toInt(), frameH.toInt()
                ), frameW, frameH
            )
            val gradualError = errorPx(
                finalTilted[hiddenId], gradualResult.paperCornersFrame!![hiddenId]
            )

            // Single step: jump from calibration to 20° in one frame
            val calcSingle = initCalibratedCalculator(corners)
            val singleResult = calcSingle.compute(
                MarkerResult(
                    markersForCorners(finalTilted).filter { it.id != hiddenId },
                    0L, frameW.toInt(), frameH.toInt()
                ), frameW, frameH
            )
            val singleError = errorPx(
                finalTilted[hiddenId], singleResult.paperCornersFrame!![hiddenId]
            )

            // With perspective-correct estimation, both should be tight.
            assertTrue(gradualError < 5f,
                "Gradual 20° tilt: error=${f(gradualError)}px")
            assertTrue(singleError < 5f,
                "Single 20° tilt: error=${f(singleError)}px")
        }

        @Test
        fun `3 markers - tilted first detection then hide marker`() {
            // REALISTIC: phone is already tilted when first seeing all 4 markers.
            // No fronto-parallel warmup. This is the real device scenario.
            val corners = a4Corners()
            for (tiltDeg in listOf(10f, 15f, 20f)) {
                val calc = initTiltedCalculator(corners, tiltDeg)
                val tilted = perspectiveTilt(corners, tiltDeg)
                for (hiddenId in 0..3) {
                    val visible = markersForCorners(tilted).filter { it.id != hiddenId }
                    val result = calc.compute(
                        MarkerResult(visible, 0L, frameW.toInt(), frameH.toInt()), frameW, frameH
                    )
                    val err = errorPx(tilted[hiddenId], result.paperCornersFrame!![hiddenId])
                    assertTrue(err < 10f,
                        "Tilted init ${tiltDeg}°, hidden=$hiddenId: error=${f(err)}px (should be <10)")
                }
            }
        }

        @Test
        fun `3 markers - tilted first detection then gradual tilt without f`() {
            // Without focal length: affine delta accumulates perspective error.
            // At 30fps with ~10°/s tilt, each frame changes by ~0.3°.
            // Using 0.1° steps simulates realistic frame rate. The last frame's
            // delta is only 0.1°, so affine error is negligible.
            // Fresh calculator per hiddenId — hiding a corner mutates state.
            val corners = a4Corners()
            val tilted20 = perspectiveTilt(corners, 20f)

            for (hiddenId in 0..3) {
                val calc = initTiltedCalculator(corners, 10f)
                // Gradual tilt from 10.1° to 19.9° in 0.1° steps, all 4 visible
                var deg = 10.1f
                while (deg < 20f) {
                    calc.compute(
                        MarkerResult(markersForCorners(perspectiveTilt(corners, deg)), 0L, frameW.toInt(), frameH.toInt()),
                        frameW, frameH
                    )
                    deg += 0.1f
                }

                val visible = markersForCorners(tilted20).filter { it.id != hiddenId }
                val result = calc.compute(
                    MarkerResult(visible, 0L, frameW.toInt(), frameH.toInt()), frameW, frameH
                )
                val err = errorPx(tilted20[hiddenId], result.paperCornersFrame!![hiddenId])
                assertTrue(err < 5f,
                    "Gradual 10°→20° no f, hidden=$hiddenId: error=${f(err)}px (should be <5)")
            }
        }

        @Test
        fun `3 markers - tilted first detection with setFocalLength`() {
            // With focal length provided (from CameraX), perspective correction works
            // even when init was tilted. Error drops from ~23px to <5px.
            val corners = a4Corners()
            val calc = initTiltedCalculator(corners, 10f)
            calc.setFocalLength(800f)

            val tilted20 = perspectiveTilt(corners, 20f)
            for (hiddenId in 0..3) {
                val visible = markersForCorners(tilted20).filter { it.id != hiddenId }
                val result = calc.compute(
                    MarkerResult(visible, 0L, frameW.toInt(), frameH.toInt()), frameW, frameH
                )
                val err = errorPx(tilted20[hiddenId], result.paperCornersFrame!![hiddenId])
                assertTrue(err < 5f,
                    "Tilted init + setFocalLength, hidden=$hiddenId: error=${f(err)}px (should be <5)")
            }
        }

        @Test
        fun `3 markers - perspective tilt uses paper geometry when calibrated`() {
            // Verify that after calibration (4 markers with tilt), hiding one marker at
            // various tilt angles gives perspective-correct estimation.
            // Each frame is independently estimated through constrained homography.
            val corners = a4Corners()

            for (tiltDeg in listOf(5f, 10f, 15f, 20f)) {
                val calc = initCalibratedCalculator(corners)
                val tilted = perspectiveTilt(corners, tiltDeg)
                val hiddenId = 3 // BL corner
                val visible = markersForCorners(tilted).filter { it.id != hiddenId }
                val result = calc.compute(
                    MarkerResult(visible, 0L, frameW.toInt(), frameH.toInt()), frameW, frameH
                )
                val err = errorPx(tilted[hiddenId], result.paperCornersFrame!![hiddenId])
                assertTrue(err < 5f,
                    "Paper geometry tilt=${tiltDeg}°, hidden=$hiddenId: error=${f(err)}px")
            }
        }

        // ── 2-marker similarity estimation ──────────────────────────

        @Test
        fun `2 markers - pure translation estimates exactly`() {
            val corners = a4Corners()
            val moved = corners.map { (x, y) -> Pair(x + 40f, y - 20f) }
            val calc = initCalculator(corners)
            val visible = markersForCorners(moved).filter { it.id in setOf(0, 1) }
            val result = calc.compute(
                MarkerResult(visible, 0L, frameW.toInt(), frameH.toInt()), frameW, frameH
            )
            val estimated = result.paperCornersFrame!!
            for (hiddenId in listOf(2, 3)) {
                assertCornerClose(
                    moved[hiddenId], estimated[hiddenId], 1f,
                    "2-marker translation, hidden=$hiddenId:"
                )
            }
        }

        @Test
        fun `2 markers - rotation with diagonal pair`() {
            val corners = a4Corners()
            val rotated = rotateCorners(corners, 10f)
            val calc = initCalculator(corners)
            // Diagonal pair: markers 0 (TL) and 2 (BR)
            val visible = markersForCorners(rotated).filter { it.id in setOf(0, 2) }
            val result = calc.compute(
                MarkerResult(visible, 0L, frameW.toInt(), frameH.toInt()), frameW, frameH
            )
            val estimated = result.paperCornersFrame!!
            for (hiddenId in listOf(1, 3)) {
                val err = errorPx(rotated[hiddenId], estimated[hiddenId])
                assertTrue(err < 15f,
                    "2-marker rotation 10°, hidden=$hiddenId: error=${f(err)}px")
            }
        }

        // ── 1-marker translation estimation ─────────────────────────

        @Test
        fun `1 marker - pure translation estimates exactly`() {
            val corners = a4Corners()
            val moved = corners.map { (x, y) -> Pair(x + 30f, y + 15f) }
            val calc = initCalculator(corners)
            val visible = markersForCorners(moved).filter { it.id == 0 }
            val result = calc.compute(
                MarkerResult(visible, 0L, frameW.toInt(), frameH.toInt()), frameW, frameH
            )
            val estimated = result.paperCornersFrame!!
            for (hiddenId in 1..3) {
                assertCornerClose(
                    moved[hiddenId], estimated[hiddenId], 1f,
                    "1-marker translation, hidden=$hiddenId:"
                )
            }
        }

        @Test
        fun `1 marker - rotation causes drift proportional to distance`() {
            val corners = a4Corners()
            val rotated = rotateCorners(corners, 5f)
            val calc = initCalculator(corners)
            val visible = markersForCorners(rotated).filter { it.id == 0 }
            val result = calc.compute(
                MarkerResult(visible, 0L, frameW.toInt(), frameH.toInt()), frameW, frameH
            )
            val estimated = result.paperCornersFrame!!
            val err1 = errorPx(rotated[1], estimated[1])
            val err2 = errorPx(rotated[2], estimated[2])
            val err3 = errorPx(rotated[3], estimated[3])

            // Translation-only can't handle rotation; error grows with distance from visible marker.
            // Marker 2 (BR) is diagonal from marker 0 (TL) → largest distance → most error.
            // For 5° rotation of a ~690px diagonal, expected drift ≈ 690 * sin(5°) ≈ 60px.
            assertTrue(err2 >= err1 || err2 >= err3,
                "Diagonal corner should have comparable or larger error: " +
                    "err1=${f(err1)}, err2=${f(err2)}, err3=${f(err3)}")
            assertTrue(err2 < 70f,
                "1-marker rotation 5°, diagonal: error=${f(err2)}px")
        }

        // ── Multi-frame stability ────────────────────────────────────

        @Test
        fun `recover full accuracy when all 4 markers return`() {
            val corners = a4Corners()
            val calc = initCalculator(corners)

            // Hide marker 2 for several frames with movement
            for (step in 1..5) {
                val moved = corners.map { (x, y) -> Pair(x + step * 10f, y) }
                val visible = markersForCorners(moved).filter { it.id != 2 }
                calc.compute(
                    MarkerResult(visible, 0L, frameW.toInt(), frameH.toInt()), frameW, frameH
                )
            }

            // Return all 4 markers at a new position
            val finalCorners = corners.map { (x, y) -> Pair(x + 80f, y + 20f) }
            val result = calc.compute(
                MarkerResult(
                    markersForCorners(finalCorners), 0L, frameW.toInt(), frameH.toInt()
                ), frameW, frameH
            )
            val estimated = result.paperCornersFrame!!
            for (id in 0..3) {
                assertCornerClose(
                    finalCorners[id], estimated[id], 0.1f,
                    "Recovery with all 4, corner=$id:"
                )
            }
        }
    }
}

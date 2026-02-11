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
}

package io.github.jn0v.traceglass.core.overlay

import io.github.jn0v.traceglass.core.cv.DetectedMarker
import io.github.jn0v.traceglass.core.cv.MarkerResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class OverlayTransformCalculatorTest {

    private val calculator = OverlayTransformCalculator()

    private fun marker(id: Int, cx: Float, cy: Float): DetectedMarker =
        DetectedMarker(id, cx, cy, emptyList(), 1f)

    @Nested
    inner class NoMarkers {
        @Test
        fun `returns identity when no markers detected`() {
            val result = MarkerResult(emptyList(), 5L)
            val transform = calculator.compute(result, 1080f, 1920f)
            assertEquals(OverlayTransform.IDENTITY, transform)
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
        fun `single marker keeps default scale`() {
            val result = MarkerResult(listOf(marker(0, 540f, 960f)), 5L)
            val transform = calculator.compute(result, 1080f, 1920f)
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
        fun `single marker returns zero rotation`() {
            val calc = OverlayTransformCalculator()
            val result = MarkerResult(listOf(marker(0, 300f, 300f)), 5L)
            val transform = calc.compute(result, 1080f, 1920f)
            assertEquals(0f, transform.rotation, 0.01f)
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
    inner class Smoothing {
        @Test
        fun `smoothed transform converges to target over iterations`() {
            val calc = OverlayTransformCalculator(smoothingFactor = 0.5f)
            val m = marker(0, 800f, 960f)
            val result = MarkerResult(listOf(m), 5L)

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
            val m = marker(0, 800f, 960f)
            val result = MarkerResult(listOf(m), 5L)

            val transform = calc.computeSmoothed(result, 1080f, 1920f, OverlayTransform.IDENTITY)

            val targetX = 800f - 1080f / 2f
            assertEquals(targetX, transform.offsetX, 0.01f)
        }
    }
}

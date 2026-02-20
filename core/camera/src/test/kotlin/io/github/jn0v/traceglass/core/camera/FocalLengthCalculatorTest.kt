package io.github.jn0v.traceglass.core.camera

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FocalLengthCalculatorTest {

    @Test
    fun `computes focal length in analysis pixels for OnePlus 5T`() {
        // OnePlus 5T: f=4.103mm, sensor=5.64mm wide, analysis=1280px
        val result = FocalLengthCalculator.computePixels(
            focalLengthMm = 4.103f,
            sensorWidthMm = 5.64f,
            analysisWidth = 1280,
            zoomRatio = 1f
        )
        // Expected: 4.103 * 1280 / 5.64 ≈ 930.7
        assertEquals(930.7f, result!!, 1f)
    }

    @Test
    fun `computes focal length with zoom factor`() {
        val result = FocalLengthCalculator.computePixels(
            focalLengthMm = 4.103f,
            sensorWidthMm = 5.64f,
            analysisWidth = 1280,
            zoomRatio = 0.5f
        )
        // Expected: 4.103 * 1280 / 5.64 * 0.5 ≈ 465.4
        assertEquals(465.4f, result!!, 1f)
    }

    @Test
    fun `returns null for zero sensor width`() {
        val result = FocalLengthCalculator.computePixels(
            focalLengthMm = 4.103f,
            sensorWidthMm = 0f,
            analysisWidth = 1280,
            zoomRatio = 1f
        )
        assertNull(result)
    }

    @Test
    fun `returns null for negative focal length`() {
        val result = FocalLengthCalculator.computePixels(
            focalLengthMm = -1f,
            sensorWidthMm = 5.64f,
            analysisWidth = 1280,
            zoomRatio = 1f
        )
        assertNull(result)
    }

    @Test
    fun `returns null for zero analysis width`() {
        val result = FocalLengthCalculator.computePixels(
            focalLengthMm = 4.103f,
            sensorWidthMm = 5.64f,
            analysisWidth = 0,
            zoomRatio = 1f
        )
        assertNull(result)
    }

    @Test
    fun `returns null for negative sensor width`() {
        val result = FocalLengthCalculator.computePixels(
            focalLengthMm = 4.103f,
            sensorWidthMm = -1f,
            analysisWidth = 1280,
            zoomRatio = 1f
        )
        assertNull(result)
    }

    @Test
    fun `returns null for NaN focal length`() {
        val result = FocalLengthCalculator.computePixels(
            focalLengthMm = Float.NaN,
            sensorWidthMm = 5.64f,
            analysisWidth = 1280,
            zoomRatio = 1f
        )
        assertNull(result)
    }

    @Test
    fun `returns null for infinite focal length`() {
        val result = FocalLengthCalculator.computePixels(
            focalLengthMm = Float.POSITIVE_INFINITY,
            sensorWidthMm = 5.64f,
            analysisWidth = 1280,
            zoomRatio = 1f
        )
        assertNull(result)
    }
}

package io.github.jn0v.traceglass.core.overlay

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MatrixUtilsTest {

    private val eps = 1e-5f

    private fun assertMatrixEquals(expected: FloatArray, actual: FloatArray, tolerance: Float = eps) {
        assertEquals(9, expected.size)
        assertEquals(9, actual.size)
        for (i in 0..8) {
            assertEquals(expected[i], actual[i], tolerance,
                "Mismatch at index $i: expected=${expected[i]}, actual=${actual[i]}")
        }
    }

    private fun transformPoint(m: FloatArray, x: Float, y: Float): Pair<Float, Float> {
        val w = m[6] * x + m[7] * y + m[8]
        val px = (m[0] * x + m[1] * y + m[2]) / w
        val py = (m[3] * x + m[4] * y + m[5]) / w
        return Pair(px, py)
    }

    @Nested
    inner class Identity {
        @Test
        fun `identity is 3x3 unit matrix`() {
            val id = MatrixUtils.identity()
            assertMatrixEquals(
                floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
                id
            )
        }

        @Test
        fun `identity transforms point unchanged`() {
            val id = MatrixUtils.identity()
            val (px, py) = transformPoint(id, 42f, 17f)
            assertEquals(42f, px, eps)
            assertEquals(17f, py, eps)
        }
    }

    @Nested
    inner class Translate {
        @Test
        fun `translate matrix has correct structure`() {
            val t = MatrixUtils.translate(10f, -5f)
            assertMatrixEquals(
                floatArrayOf(1f, 0f, 10f, 0f, 1f, -5f, 0f, 0f, 1f),
                t
            )
        }

        @Test
        fun `translate moves point by offset`() {
            val t = MatrixUtils.translate(100f, 200f)
            val (px, py) = transformPoint(t, 5f, 10f)
            assertEquals(105f, px, eps)
            assertEquals(210f, py, eps)
        }

        @Test
        fun `translate zero is identity`() {
            val t = MatrixUtils.translate(0f, 0f)
            assertMatrixEquals(MatrixUtils.identity(), t)
        }
    }

    @Nested
    inner class Scale {
        @Test
        fun `uniform scale matrix has correct structure`() {
            val s = MatrixUtils.scale(2f)
            assertMatrixEquals(
                floatArrayOf(2f, 0f, 0f, 0f, 2f, 0f, 0f, 0f, 1f),
                s
            )
        }

        @Test
        fun `non-uniform scale matrix`() {
            val s = MatrixUtils.scale(3f, 0.5f)
            assertMatrixEquals(
                floatArrayOf(3f, 0f, 0f, 0f, 0.5f, 0f, 0f, 0f, 1f),
                s
            )
        }

        @Test
        fun `scale transforms point correctly`() {
            val s = MatrixUtils.scale(2f, 3f)
            val (px, py) = transformPoint(s, 10f, 20f)
            assertEquals(20f, px, eps)
            assertEquals(60f, py, eps)
        }

        @Test
        fun `scale 1 is identity`() {
            val s = MatrixUtils.scale(1f, 1f)
            assertMatrixEquals(MatrixUtils.identity(), s)
        }
    }

    @Nested
    inner class Rotate {
        @Test
        fun `rotate 0 degrees is identity`() {
            val r = MatrixUtils.rotate(0f)
            assertMatrixEquals(MatrixUtils.identity(), r)
        }

        @Test
        fun `rotate 90 degrees`() {
            val r = MatrixUtils.rotate(90f)
            val (px, py) = transformPoint(r, 1f, 0f)
            assertEquals(0f, px, eps)
            assertEquals(1f, py, eps)
        }

        @Test
        fun `rotate 180 degrees`() {
            val r = MatrixUtils.rotate(180f)
            val (px, py) = transformPoint(r, 1f, 0f)
            assertEquals(-1f, px, eps)
            assertEquals(0f, py, 1e-4f)
        }

        @Test
        fun `rotate 270 degrees`() {
            val r = MatrixUtils.rotate(270f)
            val (px, py) = transformPoint(r, 1f, 0f)
            assertEquals(0f, px, 1e-4f)
            assertEquals(-1f, py, eps)
        }

        @Test
        fun `rotate 45 degrees gives known values`() {
            val r = MatrixUtils.rotate(45f)
            val c = cos(Math.toRadians(45.0)).toFloat()
            val s = sin(Math.toRadians(45.0)).toFloat()
            assertMatrixEquals(
                floatArrayOf(c, -s, 0f, s, c, 0f, 0f, 0f, 1f),
                r
            )
        }

        @Test
        fun `rotate 360 degrees returns to identity`() {
            val r = MatrixUtils.rotate(360f)
            assertMatrixEquals(MatrixUtils.identity(), r, 1e-4f)
        }
    }

    @Nested
    inner class Multiply {
        @Test
        fun `multiply by identity returns same matrix`() {
            val t = MatrixUtils.translate(5f, 10f)
            val result = MatrixUtils.multiply(t, MatrixUtils.identity())
            assertMatrixEquals(t, result)
        }

        @Test
        fun `identity multiplied by matrix returns same matrix`() {
            val s = MatrixUtils.scale(3f, 2f)
            val result = MatrixUtils.multiply(MatrixUtils.identity(), s)
            assertMatrixEquals(s, result)
        }

        @Test
        fun `translate then scale composes correctly`() {
            val t = MatrixUtils.translate(10f, 20f)
            val s = MatrixUtils.scale(2f)
            // S * T means: first translate, then scale
            val result = MatrixUtils.multiply(s, t)
            val (px, py) = transformPoint(result, 0f, 0f)
            assertEquals(20f, px, eps)
            assertEquals(40f, py, eps)
        }

        @Test
        fun `scale then translate composes correctly`() {
            val t = MatrixUtils.translate(10f, 20f)
            val s = MatrixUtils.scale(2f)
            // T * S means: first scale, then translate
            val result = MatrixUtils.multiply(t, s)
            val (px, py) = transformPoint(result, 5f, 5f)
            assertEquals(20f, px, eps)
            assertEquals(30f, py, eps)
        }

        @Test
        fun `two translations sum offsets`() {
            val t1 = MatrixUtils.translate(3f, 7f)
            val t2 = MatrixUtils.translate(10f, 20f)
            val result = MatrixUtils.multiply(t1, t2)
            assertMatrixEquals(MatrixUtils.translate(13f, 27f), result)
        }

        @Test
        fun `two scales multiply factors`() {
            val s1 = MatrixUtils.scale(2f, 3f)
            val s2 = MatrixUtils.scale(4f, 5f)
            val result = MatrixUtils.multiply(s1, s2)
            assertMatrixEquals(MatrixUtils.scale(8f, 15f), result)
        }
    }

    @Nested
    inner class Compose {
        @Test
        fun `compose single matrix returns same matrix`() {
            val t = MatrixUtils.translate(5f, 10f)
            val result = MatrixUtils.compose(t)
            assertMatrixEquals(t, result)
        }

        @Test
        fun `compose two matrices equals multiply`() {
            val t = MatrixUtils.translate(5f, 10f)
            val s = MatrixUtils.scale(2f)
            val composed = MatrixUtils.compose(t, s)
            val multiplied = MatrixUtils.multiply(t, s)
            assertMatrixEquals(multiplied, composed)
        }

        @Test
        fun `compose three matrices is left-to-right`() {
            val t = MatrixUtils.translate(10f, 0f)
            val s = MatrixUtils.scale(2f)
            val r = MatrixUtils.rotate(90f)

            // compose(T, S, R) = T * S * R
            val composed = MatrixUtils.compose(t, s, r)
            val manual = MatrixUtils.multiply(MatrixUtils.multiply(t, s), r)
            assertMatrixEquals(manual, composed)
        }

        @Test
        fun `compose translate-scale-rotate transforms point correctly`() {
            // Scale 2x then translate (10, 0): point (1,0) → scale → (2,0) → translate → (12,0)
            val s = MatrixUtils.scale(2f)
            val t = MatrixUtils.translate(10f, 0f)
            val m = MatrixUtils.compose(t, s)
            val (px, py) = transformPoint(m, 1f, 0f)
            assertEquals(12f, px, eps)
            assertEquals(0f, py, eps)
        }
    }
}

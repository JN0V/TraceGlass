package io.github.jn0v.traceglass.core.overlay

import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure Kotlin 3x3 matrix utilities (row-major FloatArray(9)).
 * Layout: [m0 m1 m2; m3 m4 m5; m6 m7 m8]
 * Compatible with android.graphics.Matrix.setValues() layout.
 */
object MatrixUtils {

    fun identity(): FloatArray = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )

    fun translate(tx: Float, ty: Float): FloatArray = floatArrayOf(
        1f, 0f, tx,
        0f, 1f, ty,
        0f, 0f, 1f
    )

    fun scale(sx: Float, sy: Float = sx): FloatArray = floatArrayOf(
        sx, 0f, 0f,
        0f, sy, 0f,
        0f, 0f, 1f
    )

    fun rotate(degrees: Float): FloatArray {
        val rad = Math.toRadians(degrees.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        return floatArrayOf(
            c, -s, 0f,
            s, c, 0f,
            0f, 0f, 1f
        )
    }

    /**
     * Multiply two 3x3 matrices: result = a * b
     */
    fun multiply(a: FloatArray, b: FloatArray): FloatArray {
        val r = FloatArray(9)
        for (row in 0..2) {
            for (col in 0..2) {
                r[row * 3 + col] =
                    a[row * 3 + 0] * b[0 * 3 + col] +
                    a[row * 3 + 1] * b[1 * 3 + col] +
                    a[row * 3 + 2] * b[2 * 3 + col]
            }
        }
        return r
    }

    /**
     * Compose multiple matrices left-to-right: compose(A, B, C) = A * B * C
     */
    fun compose(vararg matrices: FloatArray): FloatArray {
        return matrices.reduce { acc, m -> multiply(acc, m) }
    }
}

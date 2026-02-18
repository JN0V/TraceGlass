package io.github.jn0v.traceglass.core.overlay

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure Kotlin 4-point DLT homography solver.
 * Computes H such that dst = H * src for 4 point correspondences.
 * Returns FloatArray(9) in row-major order (android.graphics.Matrix layout),
 * or null if degenerate.
 */
object HomographySolver {

    fun solveHomography(
        src: List<Pair<Float, Float>>,
        dst: List<Pair<Float, Float>>
    ): FloatArray? {
        if (src.size != 4 || dst.size != 4) return null

        // Build 8x8 system Ah = b, with h9 = 1
        val A = Array(8) { DoubleArray(8) }
        val b = DoubleArray(8)

        for (i in 0..3) {
            val (xi, yi) = src[i]
            val (xp, yp) = dst[i]
            val row0 = i * 2
            val row1 = row0 + 1

            A[row0][0] = xi.toDouble(); A[row0][1] = yi.toDouble(); A[row0][2] = 1.0
            A[row0][3] = 0.0;           A[row0][4] = 0.0;           A[row0][5] = 0.0
            A[row0][6] = -(xi * xp).toDouble(); A[row0][7] = -(yi * xp).toDouble()
            b[row0] = xp.toDouble()

            A[row1][0] = 0.0;           A[row1][1] = 0.0;           A[row1][2] = 0.0
            A[row1][3] = xi.toDouble(); A[row1][4] = yi.toDouble(); A[row1][5] = 1.0
            A[row1][6] = -(xi * yp).toDouble(); A[row1][7] = -(yi * yp).toDouble()
            b[row1] = yp.toDouble()
        }

        val h = gaussianElimination(A, b) ?: return null

        val result = FloatArray(9)
        for (i in 0..7) result[i] = h[i].toFloat()
        result[8] = 1f

        // Determinant sanity check
        val det = determinant3x3(result)
        if (det.isNaN() || det.isInfinite()) return null
        val absDet = kotlin.math.abs(det)
        if (absDet < 0.01f || absDet > 100f) return null

        return result
    }

    /**
     * 3-point affine solver. Given 3 correspondences (src_i -> dst_i),
     * solves for affine matrix [a,b,c; d,e,f; 0,0,1].
     * Returns FloatArray(9) in row-major order, or null if degenerate.
     *
     * System:
     *   x'_i = a*x_i + b*y_i + c
     *   y'_i = d*x_i + e*y_i + f
     * → 6 equations, 6 unknowns (a,b,c,d,e,f).
     */
    fun solveAffine(
        src: List<Pair<Float, Float>>,
        dst: List<Pair<Float, Float>>
    ): FloatArray? {
        if (src.size != 3 || dst.size != 3) return null

        // Build 6x6 system
        val A = Array(6) { DoubleArray(6) }
        val b = DoubleArray(6)

        for (i in 0..2) {
            val (xi, yi) = src[i]
            val (xp, yp) = dst[i]

            // Row for x': a*xi + b*yi + c = xp
            A[i * 2][0] = xi.toDouble()
            A[i * 2][1] = yi.toDouble()
            A[i * 2][2] = 1.0
            A[i * 2][3] = 0.0
            A[i * 2][4] = 0.0
            A[i * 2][5] = 0.0
            b[i * 2] = xp.toDouble()

            // Row for y': d*xi + e*yi + f = yp
            A[i * 2 + 1][0] = 0.0
            A[i * 2 + 1][1] = 0.0
            A[i * 2 + 1][2] = 0.0
            A[i * 2 + 1][3] = xi.toDouble()
            A[i * 2 + 1][4] = yi.toDouble()
            A[i * 2 + 1][5] = 1.0
            b[i * 2 + 1] = yp.toDouble()
        }

        val solution = gaussianEliminationN(A, b, 6) ?: return null

        val result = FloatArray(9)
        result[0] = solution[0].toFloat() // a
        result[1] = solution[1].toFloat() // b
        result[2] = solution[2].toFloat() // c
        result[3] = solution[3].toFloat() // d
        result[4] = solution[4].toFloat() // e
        result[5] = solution[5].toFloat() // f
        result[6] = 0f
        result[7] = 0f
        result[8] = 1f

        // Sanity check: 2x2 sub-determinant (a*e - b*d) should be reasonable
        val det2x2 = result[0] * result[4] - result[1] * result[3]
        if (det2x2.isNaN() || det2x2.isInfinite()) return null
        val absDet = kotlin.math.abs(det2x2)
        if (absDet < 0.01f || absDet > 100f) return null

        return result
    }

    /**
     * Estimate camera focal length from a known planar rectangle observed in the image.
     *
     * Given 4 paper coordinates (known metric rectangle, e.g. A4 corners in mm) and
     * their observed image positions, computes H (paper→image) and extracts the focal
     * length using the rotation column orthogonality constraint:
     *   h1^T · ω · h2 = 0, where ω = K^{-T} K^{-1}, K = [f 0 cx; 0 f cy; 0 0 1]
     *
     * @param paperCoords 4 known rectangle corners (e.g. mm)
     * @param frameCoords 4 corresponding image positions (pixels)
     * @param cx principal point x (typically frameWidth/2)
     * @param cy principal point y (typically frameHeight/2)
     * @return estimated focal length in pixels, or null if degenerate
     */
    fun estimateFocalLength(
        paperCoords: List<Pair<Float, Float>>,
        frameCoords: List<Pair<Float, Float>>,
        cx: Float,
        cy: Float
    ): Float? {
        if (paperCoords.size != 4 || frameCoords.size != 4) return null

        val H = solveHomography(paperCoords, frameCoords) ?: return null

        // H columns: h1 = [H[0], H[3], H[6]], h2 = [H[1], H[4], H[7]]
        // With K = [f 0 cx; 0 f cy; 0 0 1], K^{-1} = [1/f 0 -cx/f; 0 1/f -cy/f; 0 0 1]
        // Let v_i = K^{-1} * h_i. The constraint is v1^T · v2 = 0.
        //
        // v1 = [(H[0] - cx*H[6])/f, (H[3] - cy*H[6])/f, H[6]]
        // v2 = [(H[1] - cx*H[7])/f, (H[4] - cy*H[7])/f, H[7]]
        //
        // v1·v2 = 0:
        // (H[0]-cx*H[6])*(H[1]-cx*H[7])/f² + (H[3]-cy*H[6])*(H[4]-cy*H[7])/f² + H[6]*H[7] = 0
        //
        // Solve for f²:
        // f² = -[(H[0]-cx*H[6])*(H[1]-cx*H[7]) + (H[3]-cy*H[6])*(H[4]-cy*H[7])] / (H[6]*H[7])

        val h = DoubleArray(9) { H[it].toDouble() }
        val cxd = cx.toDouble()
        val cyd = cy.toDouble()

        val a1 = h[0] - cxd * h[6]
        val a2 = h[1] - cxd * h[7]
        val b1 = h[3] - cyd * h[6]
        val b2 = h[4] - cyd * h[7]

        val numerator = a1 * a2 + b1 * b2
        val denominator = h[6] * h[7]

        if (abs(denominator) < 1e-12) {
            // Near-fronto-parallel: h6≈0 and/or h7≈0, perspective is negligible.
            // Use the second constraint |v1| = |v2| to estimate f instead.
            // |v1|² = (a1/f)² + (b1/f)² + h6² = (a2/f)² + (b2/f)² + h7²
            // f² = (a1²+b1² - a2²-b2²) / (h7² - h6²)
            val num2 = a1 * a1 + b1 * b1 - a2 * a2 - b2 * b2
            val den2 = h[7] * h[7] - h[6] * h[6]
            if (abs(den2) < 1e-12) return null
            val f2 = num2 / den2
            if (f2 < 100.0) return null // unreasonable
            return sqrt(f2).toFloat()
        }

        val f2 = -numerator / denominator
        if (f2 < 100.0) return null // unreasonable (f < 10px)
        return sqrt(f2).toFloat()
    }

    /**
     * Solve for a full perspective homography H (paper→frame) using only 3 point
     * correspondences + the known camera focal length.
     *
     * 3 correspondences give 6 linear equations for h1..h8 (with h9=1).
     * The focal length provides 2 additional quadratic constraints from
     * rotation column orthogonality/equal-norm. We solve the 6×8 linear system
     * to express h1..h6 as functions of (h7, h8), then use Newton-Raphson on
     * the 2 quadratic constraints.
     *
     * @param paperCoords3 3 known paper corners
     * @param frameCoords3 3 corresponding image positions
     * @param f focal length in pixels
     * @param cx principal point x
     * @param cy principal point y
     * @return FloatArray(9) homography or null if solver fails
     */
    fun solveConstrainedHomography(
        paperCoords3: List<Pair<Float, Float>>,
        frameCoords3: List<Pair<Float, Float>>,
        f: Float,
        cx: Float,
        cy: Float
    ): FloatArray? {
        if (paperCoords3.size != 3 || frameCoords3.size != 3) return null
        if (f <= 0f) return null

        val fd = f.toDouble()
        val cxd = cx.toDouble()
        val cyd = cy.toDouble()

        // Build 6x8 system: for each correspondence (xi,yi) -> (xp,yp):
        //   h1*xi + h2*yi + h3 - h7*xi*xp - h8*yi*xp = xp   (with h9=1)
        //   h4*xi + h5*yi + h6 - h7*xi*yp - h8*yi*yp = yp
        // Rearrange to 6×6 in [h1..h6] with h7,h8 on RHS:
        //   A6 * [h1..h6]^T = b6 + C * [h7, h8]^T
        val A6 = Array(6) { DoubleArray(6) }
        val b6 = DoubleArray(6)
        val C = Array(6) { DoubleArray(2) }

        for (i in 0..2) {
            val (xi, yi) = paperCoords3[i].let { it.first.toDouble() to it.second.toDouble() }
            val (xp, yp) = frameCoords3[i].let { it.first.toDouble() to it.second.toDouble() }
            val r0 = i * 2
            val r1 = r0 + 1

            // x-equation: h1*xi + h2*yi + h3 = xp + h7*xi*xp + h8*yi*xp
            A6[r0][0] = xi; A6[r0][1] = yi; A6[r0][2] = 1.0
            A6[r0][3] = 0.0; A6[r0][4] = 0.0; A6[r0][5] = 0.0
            b6[r0] = xp
            C[r0][0] = xi * xp; C[r0][1] = yi * xp

            // y-equation: h4*xi + h5*yi + h6 = yp + h7*xi*yp + h8*yi*yp
            A6[r1][0] = 0.0; A6[r1][1] = 0.0; A6[r1][2] = 0.0
            A6[r1][3] = xi; A6[r1][4] = yi; A6[r1][5] = 1.0
            b6[r1] = yp
            C[r1][0] = xi * yp; C[r1][1] = yi * yp
        }

        // Solve A6 * x = b6 to get particular solution, and A6 * x = C[:,j] for null contributions
        // h_vec = p0 + h7*d7 + h8*d8
        val p0 = gaussianEliminationN(A6.map { it.copyOf() }.toTypedArray(), b6.copyOf(), 6) ?: return null
        val d7 = gaussianEliminationN(A6.map { it.copyOf() }.toTypedArray(), DoubleArray(6) { C[it][0] }, 6) ?: return null
        val d8 = gaussianEliminationN(A6.map { it.copyOf() }.toTypedArray(), DoubleArray(6) { C[it][1] }, 6) ?: return null

        // h(h7,h8) = [p0[0]+h7*d7[0]+h8*d8[0], ..., p0[5]+h7*d7[5]+h8*d8[5], h7, h8, 1]
        // Quadratic constraints from K^{-1}*h_col:
        // Let v1 = K^{-1} * [h1,h4,h7]^T, v2 = K^{-1} * [h2,h5,h8]^T
        // F1: v1·v2 = 0 (orthogonality)
        // F2: |v1|² - |v2|² = 0 (equal norm)

        // Newton-Raphson on (h7, h8), starting from (0, 0) = affine approximation
        var h7 = 0.0
        var h8 = 0.0

        for (iter in 0 until 50) {
            val h = reconstructH(p0, d7, d8, h7, h8)
            val (f1, f2) = evalConstraints(h, fd, cxd, cyd)

            if (abs(f1) < 1e-10 && abs(f2) < 1e-10) break

            // Jacobian: df1/dh7, df1/dh8, df2/dh7, df2/dh8
            val eps = 1e-8
            val hP7 = reconstructH(p0, d7, d8, h7 + eps, h8)
            val hP8 = reconstructH(p0, d7, d8, h7, h8 + eps)
            val (f1p7, f2p7) = evalConstraints(hP7, fd, cxd, cyd)
            val (f1p8, f2p8) = evalConstraints(hP8, fd, cxd, cyd)

            val j11 = (f1p7 - f1) / eps
            val j12 = (f1p8 - f1) / eps
            val j21 = (f2p7 - f2) / eps
            val j22 = (f2p8 - f2) / eps

            val det = j11 * j22 - j12 * j21
            if (abs(det) < 1e-20) return null

            val dh7 = -(j22 * f1 - j12 * f2) / det
            val dh8 = -(-j21 * f1 + j11 * f2) / det

            h7 += dh7
            h8 += dh8

            // Divergence check
            if (abs(h7) > 0.1 || abs(h8) > 0.1) return null
        }

        val hFinal = reconstructH(p0, d7, d8, h7, h8)
        val (f1, f2) = evalConstraints(hFinal, fd, cxd, cyd)
        if (abs(f1) > 1e-4 || abs(f2) > 1e-4) return null

        val result = FloatArray(9)
        for (i in 0..7) result[i] = hFinal[i].toFloat()
        result[8] = 1f
        return result
    }

    private fun reconstructH(
        p0: DoubleArray, d7: DoubleArray, d8: DoubleArray,
        h7: Double, h8: Double
    ): DoubleArray {
        return doubleArrayOf(
            p0[0] + h7 * d7[0] + h8 * d8[0], // h1
            p0[1] + h7 * d7[1] + h8 * d8[1], // h2
            p0[2] + h7 * d7[2] + h8 * d8[2], // h3
            p0[3] + h7 * d7[3] + h8 * d8[3], // h4
            p0[4] + h7 * d7[4] + h8 * d8[4], // h5
            p0[5] + h7 * d7[5] + h8 * d8[5], // h6
            h7, h8, 1.0
        )
    }

    /** Returns (F1=v1·v2, F2=|v1|²-|v2|²) for the rotation constraints. */
    private fun evalConstraints(
        h: DoubleArray, f: Double, cx: Double, cy: Double
    ): Pair<Double, Double> {
        // v1 = K^{-1} * col1 = [(h1-cx*h7)/f, (h4-cy*h7)/f, h7]
        // v2 = K^{-1} * col2 = [(h2-cx*h8)/f, (h5-cy*h8)/f, h8]
        val v1x = (h[0] - cx * h[6]) / f
        val v1y = (h[3] - cy * h[6]) / f
        val v1z = h[6]
        val v2x = (h[1] - cx * h[7]) / f
        val v2y = (h[4] - cy * h[7]) / f
        val v2z = h[7]

        val dot = v1x * v2x + v1y * v2y + v1z * v2z
        val norm1sq = v1x * v1x + v1y * v1y + v1z * v1z
        val norm2sq = v2x * v2x + v2y * v2y + v2z * v2z

        return dot to (norm1sq - norm2sq)
    }

    /**
     * Correct the aspect ratio of an assumed rectangle by analyzing perspective distortion.
     *
     * Given H mapping assumedPaperCoords → detectedCorners, the true AR is:
     *   AR_true = AR_assumed * |K⁻¹·h₁| / |K⁻¹·h₂|
     * where h₁, h₂ are H's columns (representing the x and y basis in the image).
     *
     * The orthogonality constraint (used to estimate f) is invariant to paper scale,
     * so f estimated from the wrong AR is still valid for this correction.
     *
     * @param assumedPaperCoords 4 assumed rectangle corners (same scale as detected)
     * @param detectedCorners 4 detected positions in pixels
     * @param f focal length in pixels
     * @param cx principal point x
     * @param cy principal point y
     * @return corrected aspect ratio (width/height), or null if degenerate
     */
    fun correctAspectRatio(
        assumedPaperCoords: List<Pair<Float, Float>>,
        detectedCorners: List<Pair<Float, Float>>,
        f: Float,
        cx: Float,
        cy: Float
    ): Float? {
        if (assumedPaperCoords.size != 4 || detectedCorners.size != 4) return null
        val H = solveHomography(assumedPaperCoords, detectedCorners) ?: return null
        val fd = f.toDouble()
        val cxd = cx.toDouble()
        val cyd = cy.toDouble()
        val h = DoubleArray(9) { H[it].toDouble() }

        // v1 = K⁻¹ * col1, v2 = K⁻¹ * col2
        val v1x = (h[0] - cxd * h[6]) / fd
        val v1y = (h[3] - cyd * h[6]) / fd
        val v1z = h[6]
        val v2x = (h[1] - cxd * h[7]) / fd
        val v2y = (h[4] - cyd * h[7]) / fd
        val v2z = h[7]

        val norm1 = sqrt(v1x * v1x + v1y * v1y + v1z * v1z)
        val norm2 = sqrt(v2x * v2x + v2y * v2y + v2z * v2z)
        if (norm1 < 1e-10 || norm2 < 1e-10) return null

        // AR_true = AR_assumed * norm1/norm2
        val assumedW = assumedPaperCoords[1].first - assumedPaperCoords[0].first
        val assumedH = assumedPaperCoords[3].second - assumedPaperCoords[0].second
        if (assumedH < 0.001f || assumedW < 0.001f) return null
        val assumedAR = assumedW / assumedH

        val corrected = (assumedAR * norm1 / norm2).toFloat()
        if (corrected < 0.1f || corrected > 10f) return null
        return corrected
    }

    /**
     * General NxN Gaussian elimination with partial pivoting.
     */
    private fun gaussianEliminationN(A: Array<DoubleArray>, b: DoubleArray, n: Int): DoubleArray? {
        val aug = Array(n) { i -> DoubleArray(n + 1).also { row ->
            for (j in 0 until n) row[j] = A[i][j]
            row[n] = b[i]
        }}

        for (col in 0 until n) {
            var maxRow = col
            var maxVal = kotlin.math.abs(aug[col][col])
            for (row in col + 1 until n) {
                val v = kotlin.math.abs(aug[row][col])
                if (v > maxVal) { maxVal = v; maxRow = row }
            }
            if (maxVal < 1e-12) return null
            if (maxRow != col) {
                val tmp = aug[col]; aug[col] = aug[maxRow]; aug[maxRow] = tmp
            }

            val pivot = aug[col][col]
            for (j in col..n) aug[col][j] /= pivot

            for (row in 0 until n) {
                if (row == col) continue
                val factor = aug[row][col]
                for (j in col..n) aug[row][j] -= factor * aug[col][j]
            }
        }

        return DoubleArray(n) { aug[it][n] }
    }

    private fun gaussianElimination(A: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        return gaussianEliminationN(A, b, 8)
    }

    private fun determinant3x3(h: FloatArray): Float {
        return h[0] * (h[4] * h[8] - h[5] * h[7]) -
               h[1] * (h[3] * h[8] - h[5] * h[6]) +
               h[2] * (h[3] * h[7] - h[4] * h[6])
    }
}

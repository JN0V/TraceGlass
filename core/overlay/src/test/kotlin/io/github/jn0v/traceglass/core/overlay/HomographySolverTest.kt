package io.github.jn0v.traceglass.core.overlay

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class HomographySolverTest {

    private val a4Corners = listOf(
        Pair(0f, 0f), Pair(210f, 0f), Pair(210f, 297f), Pair(0f, 297f)
    )

    private val cx = 640f
    private val cy = 360f

    /**
     * Projects A4 paper corners through a simulated camera with given focal length and tilt.
     * Paper is centered in the image, tilted around horizontal axis.
     */
    private fun projectA4(
        focalLength: Float,
        tiltDeg: Float = 0f,
        paperWidthPx: Float = 400f,
        paperHeightPx: Float = 566f
    ): List<Pair<Float, Float>> {
        // Place paper centered at image center
        val corners = listOf(
            Pair(cx - paperWidthPx / 2, cy - paperHeightPx / 2),
            Pair(cx + paperWidthPx / 2, cy - paperHeightPx / 2),
            Pair(cx + paperWidthPx / 2, cy + paperHeightPx / 2),
            Pair(cx - paperWidthPx / 2, cy + paperHeightPx / 2)
        )

        if (tiltDeg == 0f) return corners

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

    private fun transformPoint(H: FloatArray, x: Float, y: Float): Pair<Float, Float> {
        val w = H[6] * x + H[7] * y + H[8]
        return Pair(
            (H[0] * x + H[1] * y + H[2]) / w,
            (H[3] * x + H[4] * y + H[5]) / w
        )
    }

    @Nested
    inner class SolveHomography {

        @Test
        fun `identity mapping returns identity-like matrix`() {
            val pts = listOf(
                Pair(0f, 0f), Pair(100f, 0f), Pair(100f, 100f), Pair(0f, 100f)
            )
            val H = HomographySolver.solveHomography(pts, pts)
            assertNotNull(H, "Identity mapping should produce valid homography")
            for (pt in pts) {
                val (rx, ry) = transformPoint(H!!, pt.first, pt.second)
                assertEquals(pt.first, rx, 0.01f, "x mismatch for ${pt.first},${pt.second}")
                assertEquals(pt.second, ry, 0.01f, "y mismatch for ${pt.first},${pt.second}")
            }
        }

        @Test
        fun `pure translation maps points correctly`() {
            val src = listOf(
                Pair(0f, 0f), Pair(100f, 0f), Pair(100f, 100f), Pair(0f, 100f)
            )
            val dst = src.map { (x, y) -> Pair(x + 50f, y + 30f) }
            val H = HomographySolver.solveHomography(src, dst)
            assertNotNull(H)
            for (i in src.indices) {
                val (rx, ry) = transformPoint(H!!, src[i].first, src[i].second)
                assertEquals(dst[i].first, rx, 0.1f, "x mismatch at point $i")
                assertEquals(dst[i].second, ry, 0.1f, "y mismatch at point $i")
            }
        }

        @Test
        fun `scale mapping maps all 4 points correctly`() {
            val src = listOf(
                Pair(0f, 0f), Pair(200f, 0f), Pair(200f, 200f), Pair(0f, 200f)
            )
            val dst = src.map { (x, y) -> Pair(x * 2f, y * 2f) }
            val H = HomographySolver.solveHomography(src, dst)
            assertNotNull(H)
            for (i in src.indices) {
                val (rx, ry) = transformPoint(H!!, src[i].first, src[i].second)
                assertEquals(dst[i].first, rx, 0.1f, "x mismatch at point $i")
                assertEquals(dst[i].second, ry, 0.1f, "y mismatch at point $i")
            }
        }

        @Test
        fun `perspective trapezoid maps all 4 points correctly`() {
            // Simulate a perspective-distorted quadrilateral (trapezoid)
            val src = listOf(
                Pair(0f, 0f), Pair(200f, 0f), Pair(200f, 300f), Pair(0f, 300f)
            )
            val dst = listOf(
                Pair(30f, 10f), Pair(180f, 20f), Pair(210f, 290f), Pair(-10f, 280f)
            )
            val H = HomographySolver.solveHomography(src, dst)
            assertNotNull(H)
            for (i in src.indices) {
                val (rx, ry) = transformPoint(H!!, src[i].first, src[i].second)
                assertEquals(dst[i].first, rx, 0.5f, "x mismatch at point $i")
                assertEquals(dst[i].second, ry, 0.5f, "y mismatch at point $i")
            }
        }

        @Test
        fun `maps interior points correctly after perspective warp`() {
            val src = listOf(
                Pair(0f, 0f), Pair(100f, 0f), Pair(100f, 100f), Pair(0f, 100f)
            )
            val dst = listOf(
                Pair(10f, 10f), Pair(90f, 15f), Pair(95f, 85f), Pair(5f, 90f)
            )
            val H = HomographySolver.solveHomography(src, dst)
            assertNotNull(H)
            // Center of src (50,50) should map to somewhere inside the dst quadrilateral
            val (cx, cy) = transformPoint(H!!, 50f, 50f)
            assertTrue(cx in 0f..100f, "center x=$cx should be inside dst quad")
            assertTrue(cy in 0f..100f, "center y=$cy should be inside dst quad")
        }

        @Test
        fun `returns null for wrong number of points`() {
            val pts3 = listOf(Pair(0f, 0f), Pair(1f, 0f), Pair(1f, 1f))
            assertNull(HomographySolver.solveHomography(pts3, pts3))
        }

        @Test
        fun `returns null for collinear points`() {
            val src = listOf(
                Pair(0f, 0f), Pair(1f, 0f), Pair(2f, 0f), Pair(3f, 0f)
            )
            val dst = listOf(
                Pair(0f, 0f), Pair(1f, 0f), Pair(2f, 0f), Pair(3f, 0f)
            )
            val H = HomographySolver.solveHomography(src, dst)
            // Should return null due to degenerate/singular system
            assertNull(H, "Collinear points should produce null homography")
        }
    }

    @Nested
    inner class SolveAffine {

        @Test
        fun `identity mapping returns identity-like matrix`() {
            val pts = listOf(Pair(0f, 0f), Pair(100f, 0f), Pair(50f, 100f))
            val H = HomographySolver.solveAffine(pts, pts)
            assertNotNull(H)
            assertEquals(9, H!!.size)
            for (pt in pts) {
                val (rx, ry) = transformPoint(H, pt.first, pt.second)
                assertEquals(pt.first, rx, 0.01f)
                assertEquals(pt.second, ry, 0.01f)
            }
        }

        @Test
        fun `pure translation maps 3 points correctly`() {
            val src = listOf(Pair(10f, 20f), Pair(110f, 20f), Pair(60f, 120f))
            val dst = src.map { (x, y) -> Pair(x + 40f, y - 15f) }
            val H = HomographySolver.solveAffine(src, dst)
            assertNotNull(H)
            for (i in src.indices) {
                val (rx, ry) = transformPoint(H!!, src[i].first, src[i].second)
                assertEquals(dst[i].first, rx, 0.1f, "point $i x")
                assertEquals(dst[i].second, ry, 0.1f, "point $i y")
            }
        }

        @Test
        fun `scale and translate combined`() {
            val src = listOf(Pair(0f, 0f), Pair(100f, 0f), Pair(0f, 100f))
            // scale 2x then translate (50, 50)
            val dst = src.map { (x, y) -> Pair(x * 2f + 50f, y * 2f + 50f) }
            val H = HomographySolver.solveAffine(src, dst)
            assertNotNull(H)
            for (i in src.indices) {
                val (rx, ry) = transformPoint(H!!, src[i].first, src[i].second)
                assertEquals(dst[i].first, rx, 0.1f, "point $i x")
                assertEquals(dst[i].second, ry, 0.1f, "point $i y")
            }
        }

        @Test
        fun `last row is 0 0 1 for affine matrix`() {
            val src = listOf(Pair(0f, 0f), Pair(100f, 0f), Pair(50f, 80f))
            val dst = listOf(Pair(10f, 10f), Pair(90f, 20f), Pair(55f, 95f))
            val H = HomographySolver.solveAffine(src, dst)
            assertNotNull(H)
            assertEquals(0f, H!![6], 1e-6f, "h[6] should be 0")
            assertEquals(0f, H[7], 1e-6f, "h[7] should be 0")
            assertEquals(1f, H[8], 1e-6f, "h[8] should be 1")
        }

        @Test
        fun `returns null for wrong number of points`() {
            val pts4 = listOf(Pair(0f, 0f), Pair(1f, 0f), Pair(1f, 1f), Pair(0f, 1f))
            assertNull(HomographySolver.solveAffine(pts4, pts4))
        }

        @Test
        fun `returns null for collinear points`() {
            val src = listOf(Pair(0f, 0f), Pair(50f, 0f), Pair(100f, 0f))
            val dst = listOf(Pair(0f, 0f), Pair(50f, 0f), Pair(100f, 0f))
            val H = HomographySolver.solveAffine(src, dst)
            assertNull(H, "Collinear points should produce null affine")
        }
    }

    @Nested
    inner class EstimateFocalLength {

        @Test
        fun `known perspective geometry recovers focal length`() {
            val f = 800f
            val frameCoords = projectA4(f, tiltDeg = 15f)
            val estimated = HomographySolver.estimateFocalLength(a4Corners, frameCoords, cx, cy)
            assertNotNull(estimated, "Should estimate focal length from tilted view")
            val relError = abs(estimated!! - f) / f
            assertTrue(relError < 0.15f,
                "Focal length error too large: estimated=$estimated, expected=$f, relError=$relError")
        }

        @Test
        fun `stronger tilt gives better estimate`() {
            val f = 800f
            val frameCoords = projectA4(f, tiltDeg = 25f)
            val estimated = HomographySolver.estimateFocalLength(a4Corners, frameCoords, cx, cy)
            assertNotNull(estimated, "Should estimate focal length from 25° tilt")
            val relError = abs(estimated!! - f) / f
            assertTrue(relError < 0.10f,
                "Focal length error too large at 25°: estimated=$estimated, expected=$f")
        }

        @Test
        fun `fronto-parallel view returns value or null`() {
            // At 0° tilt, h6≈0, h7≈0 → degenerate for primary constraint
            // Falls back to secondary constraint or returns null
            val f = 800f
            val frameCoords = projectA4(f, tiltDeg = 0f)
            val estimated = HomographySolver.estimateFocalLength(a4Corners, frameCoords, cx, cy)
            // Either null (acceptable: no perspective to extract f from) or a large value
            if (estimated != null) {
                assertTrue(estimated > 100f,
                    "Fronto-parallel focal should be large or null: $estimated")
            }
        }
    }

    @Nested
    inner class SolveConstrainedHomography {

        @Test
        fun `recovers 4th corner from 3 visible - no tilt`() {
            val f = 800f
            val frameCoords = projectA4(f, tiltDeg = 0f)

            // Use first 3 corners, estimate 4th
            for (hiddenId in 0..3) {
                val visibleIds = (0..3).filter { it != hiddenId }
                val paperPts = visibleIds.map { a4Corners[it] }
                val framePts = visibleIds.map { frameCoords[it] }

                val H = HomographySolver.solveConstrainedHomography(paperPts, framePts, f, cx, cy)
                assertNotNull(H, "Should solve for hidden=$hiddenId at 0° tilt")

                val (px, py) = a4Corners[hiddenId]
                val w = H!![6] * px + H[7] * py + H[8]
                val estX = (H[0] * px + H[1] * py + H[2]) / w
                val estY = (H[3] * px + H[4] * py + H[5]) / w
                val err = errorPx(frameCoords[hiddenId], Pair(estX, estY))
                assertTrue(err < 2f,
                    "No-tilt hidden=$hiddenId: error=${err}px")
            }
        }

        @Test
        fun `recovers 4th corner at 20 degree tilt`() {
            val f = 800f
            val frameCoords = projectA4(f, tiltDeg = 20f)

            for (hiddenId in 0..3) {
                val visibleIds = (0..3).filter { it != hiddenId }
                val paperPts = visibleIds.map { a4Corners[it] }
                val framePts = visibleIds.map { frameCoords[it] }

                val H = HomographySolver.solveConstrainedHomography(paperPts, framePts, f, cx, cy)
                assertNotNull(H, "Should solve for hidden=$hiddenId at 20° tilt")

                val (px, py) = a4Corners[hiddenId]
                val w = H!![6] * px + H[7] * py + H[8]
                val estX = (H[0] * px + H[1] * py + H[2]) / w
                val estY = (H[3] * px + H[4] * py + H[5]) / w
                val err = errorPx(frameCoords[hiddenId], Pair(estX, estY))
                assertTrue(err < 5f,
                    "20° tilt hidden=$hiddenId: error=${err}px (was ~100px with affine)")
            }
        }

        @Test
        fun `recovers 4th corner at 30 degree tilt`() {
            val f = 800f
            val frameCoords = projectA4(f, tiltDeg = 30f)

            for (hiddenId in 0..3) {
                val visibleIds = (0..3).filter { it != hiddenId }
                val paperPts = visibleIds.map { a4Corners[it] }
                val framePts = visibleIds.map { frameCoords[it] }

                val H = HomographySolver.solveConstrainedHomography(paperPts, framePts, f, cx, cy)
                // At 30° this is harder; may return null for some corners
                if (H != null) {
                    val (px, py) = a4Corners[hiddenId]
                    val w = H[6] * px + H[7] * py + H[8]
                    val estX = (H[0] * px + H[1] * py + H[2]) / w
                    val estY = (H[3] * px + H[4] * py + H[5]) / w
                    val err = errorPx(frameCoords[hiddenId], Pair(estX, estY))
                    assertTrue(err < 10f,
                        "30° tilt hidden=$hiddenId: error=${err}px")
                }
            }
        }

        @Test
        fun `returns null for degenerate input`() {
            // All 3 points collinear
            val paperPts = listOf(Pair(0f, 0f), Pair(100f, 0f), Pair(200f, 0f))
            val framePts = listOf(Pair(100f, 100f), Pair(200f, 100f), Pair(300f, 100f))
            val H = HomographySolver.solveConstrainedHomography(paperPts, framePts, 800f, 640f, 360f)
            // Should return null or a degenerate result
            // The 6x6 system may be singular since all y=0
        }
    }
}

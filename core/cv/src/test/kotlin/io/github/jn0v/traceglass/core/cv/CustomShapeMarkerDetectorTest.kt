package io.github.jn0v.traceglass.core.cv

import io.github.jn0v.traceglass.core.cv.impl.CustomShapeMarkerDetector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class CustomShapeMarkerDetectorTest {

    private val detector = CustomShapeMarkerDetector()

    /** White sheet with solid dark squares of side [size] centred at the given points. */
    private class Sheet(val width: Int, val height: Int, val stride: Int = width) {
        val pixels = ByteArray(stride * height) { 0xF0.toByte() }

        fun square(cx: Int, cy: Int, size: Int, value: Int = 0x20) {
            for (y in cy - size / 2 until cy + size / 2) {
                for (x in cx - size / 2 until cx + size / 2) {
                    pixels[y * stride + x] = value.toByte()
                }
            }
        }

        fun fourCorners(size: Int = 30) {
            square(80, 60, size)
            square(width - 80, 60, size)
            square(width - 80, height - 60, size)
            square(80, height - 60, size)
        }

        fun detect(d: CustomShapeMarkerDetector, rotation: Int = 0) =
            d.detect(ByteBuffer.wrap(pixels), width, height, stride, rotation)
    }

    @Test
    fun `four drawn shapes are reported as ids 0 to 3 in TL TR BR BL order`() {
        val sheet = Sheet(640, 480).also { it.fourCorners() }

        val result = sheet.detect(detector)

        assertEquals(4, result.markerCount)
        assertEquals(listOf(0, 1, 2, 3), result.markers.map { it.id })
        val (tl, tr, br, bl) = result.markers
        assertEquals(80f, tl.centerX, 4f); assertEquals(60f, tl.centerY, 4f)
        assertEquals(560f, tr.centerX, 4f); assertEquals(60f, tr.centerY, 4f)
        assertEquals(560f, br.centerX, 4f); assertEquals(420f, br.centerY, 4f)
        assertEquals(80f, bl.centerX, 4f); assertEquals(420f, bl.centerY, 4f)
        assertEquals(640, result.frameWidth)
        assertEquals(480, result.frameHeight)
    }

    @Test
    fun `each marker carries its outer paper corner at corners of its id`() {
        val sheet = Sheet(640, 480).also { it.fourCorners() }

        val result = sheet.detect(detector)

        result.markers.forEach { assertEquals(4, it.corners.size) }
        val (tl, tr, br, bl) = result.markers
        // The outer corner of a 30 px square centred on (80, 60) is (65, 45).
        assertEquals(65f, tl.corners[0].first, 6f); assertEquals(45f, tl.corners[0].second, 6f)
        assertEquals(575f, tr.corners[1].first, 6f); assertEquals(45f, tr.corners[1].second, 6f)
        assertEquals(575f, br.corners[2].first, 6f); assertEquals(435f, br.corners[2].second, 6f)
        assertEquals(65f, bl.corners[3].first, 6f); assertEquals(435f, bl.corners[3].second, 6f)
    }

    @Test
    fun `row stride padding is respected`() {
        val sheet = Sheet(640, 480, stride = 704).also { it.fourCorners() }

        assertEquals(4, sheet.detect(detector).markerCount)
    }

    @Test
    fun `rotation maps shapes into the rotated frame`() {
        val sheet = Sheet(640, 480).also { it.fourCorners() }

        val result = sheet.detect(detector, rotation = 90)

        assertEquals(4, result.markerCount)
        assertEquals(480, result.frameWidth)
        assertEquals(640, result.frameHeight)
        // 90 degrees clockwise: source top-left (80, 60) lands at (480 - 60, 80).
        val tr = result.markers[1]
        assertEquals(420f, tr.centerX, 4f); assertEquals(80f, tr.centerY, 4f)
    }

    @Test
    fun `three shapes do not lock`() {
        val sheet = Sheet(640, 480)
        sheet.square(80, 60, 30); sheet.square(560, 60, 30); sheet.square(560, 420, 30)

        assertFalse(sheet.detect(detector).isTracking)
    }

    @Test
    fun `an extra mark of similar size is ambiguous`() {
        val sheet = Sheet(640, 480).also { it.fourCorners() }
        sheet.square(320, 240, 30)

        assertFalse(sheet.detect(detector).isTracking)
    }

    @Test
    fun `small specks next to the shapes are ignored`() {
        val sheet = Sheet(640, 480).also { it.fourCorners() }
        sheet.square(320, 240, 4)

        assertEquals(4, sheet.detect(detector).markerCount)
    }

    @Test
    fun `a blank sheet does not lock`() {
        assertFalse(Sheet(640, 480).detect(detector).isTracking)
    }

    @Test
    fun `low contrast marks do not lock`() {
        val sheet = Sheet(640, 480)
        listOf(80 to 60, 560 to 60, 560 to 420, 80 to 420).forEach { (x, y) ->
            sheet.square(x, y, 30, value = 0xD0)
        }

        assertFalse(sheet.detect(detector).isTracking)
    }

    @Test
    fun `a dark band touching the frame edge is not a shape`() {
        val sheet = Sheet(640, 480).also { it.fourCorners() }
        for (y in 0 until 480) for (x in 0 until 10) sheet.pixels[y * 640 + x] = 0x10

        assertEquals(4, sheet.detect(detector).markerCount)
    }

    @Test
    fun `a buffer smaller than the frame yields no markers`() {
        val result = detector.detect(ByteBuffer.allocate(100), 640, 480, 640, 0)

        assertTrue(result.markers.isEmpty())
    }

    @Test
    fun `invalid dimensions yield no markers`() {
        val result = detector.detect(ByteBuffer.allocate(100), 0, 0, 0, 0)

        assertTrue(result.markers.isEmpty())
    }
}

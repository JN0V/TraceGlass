package io.github.jn0v.traceglass.core.cv

import io.github.jn0v.traceglass.core.cv.impl.ModeSwitchingMarkerDetector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class ModeSwitchingMarkerDetectorTest {

    private val aruco = FakeMarkerDetector()
    private val shapes = FakeMarkerDetector()
    private val selector = DetectionModeSelector()
    private val detector = ModeSwitchingMarkerDetector(selector, aruco, shapes)
    private val buffer = ByteBuffer.allocate(16)

    @Test
    fun `aruco is the default mode`() {
        assertEquals(DetectionMode.ARUCO, selector.mode)
    }

    @Test
    fun `aruco mode uses only the aruco detector`() {
        detector.detect(buffer, 4, 4, 4, 0)

        assertEquals(1, aruco.detectCallCount)
        assertEquals(0, shapes.detectCallCount)
    }

    @Test
    fun `custom shapes mode uses only the shape detector`() {
        selector.mode = DetectionMode.CUSTOM_SHAPES

        detector.detect(buffer, 4, 4, 4, 0)

        assertEquals(0, aruco.detectCallCount)
        assertEquals(1, shapes.detectCallCount)
    }

    @Test
    fun `switching mode takes effect on the next frame`() {
        detector.detect(buffer, 4, 4, 4, 0)
        selector.mode = DetectionMode.CUSTOM_SHAPES
        detector.detect(buffer, 4, 4, 4, 0)
        selector.mode = DetectionMode.ARUCO
        detector.detect(buffer, 4, 4, 4, 0)

        assertEquals(2, aruco.detectCallCount)
        assertEquals(1, shapes.detectCallCount)
    }
}

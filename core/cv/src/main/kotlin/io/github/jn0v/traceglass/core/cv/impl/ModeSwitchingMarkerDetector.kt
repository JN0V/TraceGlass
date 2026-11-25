package io.github.jn0v.traceglass.core.cv.impl

import io.github.jn0v.traceglass.core.cv.DetectionMode
import io.github.jn0v.traceglass.core.cv.DetectionModeSelector
import io.github.jn0v.traceglass.core.cv.MarkerDetector
import io.github.jn0v.traceglass.core.cv.MarkerResult
import java.nio.ByteBuffer

/** Sends each frame to the detector of the mode chosen in [selector]. */
class ModeSwitchingMarkerDetector(
    private val selector: DetectionModeSelector,
    private val aruco: MarkerDetector,
    private val customShapes: MarkerDetector
) : MarkerDetector {

    override fun detect(
        frameBuffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        rotation: Int
    ): MarkerResult {
        val detector = when (selector.mode) {
            DetectionMode.ARUCO -> aruco
            DetectionMode.CUSTOM_SHAPES -> customShapes
        }
        return detector.detect(frameBuffer, width, height, rowStride, rotation)
    }
}

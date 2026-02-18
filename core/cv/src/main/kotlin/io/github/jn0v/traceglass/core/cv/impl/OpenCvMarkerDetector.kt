package io.github.jn0v.traceglass.core.cv.impl

import io.github.jn0v.traceglass.core.cv.MarkerDetector
import io.github.jn0v.traceglass.core.cv.MarkerResult
import java.nio.ByteBuffer

class OpenCvMarkerDetector : MarkerDetector {

    companion object {
        init {
            System.loadLibrary("traceglass_cv")
        }
    }

    override fun detect(
        frameBuffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        rotation: Int
    ): MarkerResult {
        return nativeDetect(frameBuffer, width, height, rowStride, rotation)
    }

    private external fun nativeDetect(
        byteBuffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        rotation: Int
    ): MarkerResult
}

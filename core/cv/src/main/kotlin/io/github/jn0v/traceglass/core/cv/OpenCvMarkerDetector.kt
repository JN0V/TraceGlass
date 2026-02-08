package io.github.jn0v.traceglass.core.cv

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
        rotation: Int
    ): MarkerResult {
        return nativeDetect(frameBuffer, width, height, rotation)
    }

    private external fun nativeDetect(
        byteBuffer: ByteBuffer,
        width: Int,
        height: Int,
        rotation: Int
    ): MarkerResult
}

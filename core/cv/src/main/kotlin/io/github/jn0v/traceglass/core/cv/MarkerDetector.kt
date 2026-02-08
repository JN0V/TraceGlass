package io.github.jn0v.traceglass.core.cv

import java.nio.ByteBuffer

interface MarkerDetector {
    fun detect(
        frameBuffer: ByteBuffer,
        width: Int,
        height: Int,
        rotation: Int
    ): MarkerResult
}

package io.github.jn0v.traceglass.core.cv

import java.nio.ByteBuffer

class FakeMarkerDetector : MarkerDetector {
    var resultToReturn: MarkerResult = MarkerResult(emptyList(), 0L)
    var detectCallCount: Int = 0
        private set

    override fun detect(
        frameBuffer: ByteBuffer,
        width: Int,
        height: Int,
        rotation: Int
    ): MarkerResult {
        detectCallCount++
        return resultToReturn
    }
}

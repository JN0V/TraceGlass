package io.github.jn0v.traceglass.feature.tracing

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import android.util.Log
import io.github.jn0v.traceglass.core.cv.MarkerDetector
import io.github.jn0v.traceglass.core.cv.MarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FrameAnalyzer(
    private val markerDetector: MarkerDetector
) : ImageAnalysis.Analyzer {

    private val _latestResult = MutableStateFlow(MarkerResult(emptyList(), 0L))
    val latestResult: StateFlow<MarkerResult> = _latestResult.asStateFlow()

    private var frameCount = 0L

    override fun analyze(image: ImageProxy) {
        try {
            val buffer = image.planes[0].buffer
            if (frameCount++ % 60 == 0L) {
                Log.d(TAG, "Frame #$frameCount: ${image.width}x${image.height} " +
                    "format=${image.format} rot=${image.imageInfo.rotationDegrees} " +
                    "bufSize=${buffer.remaining()} " +
                    "rowStride=${image.planes[0].rowStride} " +
                    "pixelStride=${image.planes[0].pixelStride}")
            }
            val result = markerDetector.detect(
                frameBuffer = buffer,
                width = image.width,
                height = image.height,
                rowStride = image.planes[0].rowStride,
                rotation = image.imageInfo.rotationDegrees
            )
            if (frameCount % 60 == 1L) {
                Log.d(TAG, "Detection: markers=${result.markers.size} " +
                    "frame=${result.frameWidth}x${result.frameHeight} " +
                    "time=${result.detectionTimeMs}ms")
            }
            _latestResult.value = result
        } finally {
            image.close()
        }
    }

    companion object {
        private const val TAG = "FrameAnalyzer"
    }
}

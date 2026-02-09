package io.github.jn0v.traceglass.feature.tracing

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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

    override fun analyze(image: ImageProxy) {
        try {
            val buffer = image.planes[0].buffer
            val result = markerDetector.detect(
                frameBuffer = buffer,
                width = image.width,
                height = image.height,
                rotation = image.imageInfo.rotationDegrees
            )
            _latestResult.value = result.copy(
                frameWidth = image.width,
                frameHeight = image.height
            )
        } finally {
            image.close()
        }
    }
}

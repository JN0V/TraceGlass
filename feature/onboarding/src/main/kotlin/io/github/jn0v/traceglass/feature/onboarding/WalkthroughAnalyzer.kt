package io.github.jn0v.traceglass.feature.onboarding

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import io.github.jn0v.traceglass.core.cv.MarkerDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WalkthroughAnalyzer(
    private val markerDetector: MarkerDetector
) : ImageAnalysis.Analyzer {

    private val _markersFound = MutableStateFlow(false)
    val markersFound: StateFlow<Boolean> = _markersFound.asStateFlow()

    override fun analyze(image: ImageProxy) {
        try {
            val buffer = image.planes[0].buffer
            val result = markerDetector.detect(
                frameBuffer = buffer,
                width = image.width,
                height = image.height,
                rowStride = image.planes[0].rowStride,
                rotation = image.imageInfo.rotationDegrees
            )
            if (result.markers.isNotEmpty()) {
                _markersFound.value = true
            }
        } catch (_: Exception) {
            // Ignore detection failures during walkthrough
        } finally {
            image.close()
        }
    }
}

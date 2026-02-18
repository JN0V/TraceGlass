package io.github.jn0v.traceglass.feature.tracing

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import io.github.jn0v.traceglass.core.cv.MarkerDetector
import io.github.jn0v.traceglass.core.cv.MarkerResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference

class FrameAnalyzer(
    private val markerDetector: MarkerDetector,
    snapshotDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ImageAnalysis.Analyzer {

    private val _latestResult = MutableStateFlow(MarkerResult(emptyList(), 0L))
    val latestResult: StateFlow<MarkerResult> = _latestResult.asStateFlow()

    private var frameCount = 0L
    private val snapshotScope = CoroutineScope(snapshotDispatcher + SupervisorJob())

    /**
     * One-shot snapshot callback. When non-null, the next frame will be encoded
     * to JPEG and delivered via this callback, then the callback is cleared
     * atomically. Set this from TimelapseSession's onCapture timer.
     */
    private val _snapshotCallback = AtomicReference<((ByteArray) -> Unit)?>(null)
    var snapshotCallback: ((ByteArray) -> Unit)?
        get() = _snapshotCallback.get()
        set(value) { _snapshotCallback.set(value) }

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
            if (result.detectionTimeMs > DETECTION_TIME_WARN_MS) {
                Log.w(TAG, "Detection exceeded ${DETECTION_TIME_WARN_MS}ms NFR: " +
                    "${result.detectionTimeMs}ms (frame #$frameCount)")
            }
            _latestResult.value = result

            // Snapshot capture: one-shot, encode JPEG on snapshot dispatcher
            val callback = _snapshotCallback.getAndSet(null)
            if (callback != null) {
                // toBitmap() handles rowStride padding correctly (CameraX 1.3+)
                val bitmap = image.toBitmap()
                snapshotScope.launch {
                    try {
                        val buffer = ByteArrayOutputStream(512 * 1024)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, buffer)
                        callback(buffer.toByteArray())
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Detection failed", e)
        } finally {
            image.close()
        }
    }

    fun close() {
        snapshotScope.cancel()
    }

    companion object {
        private const val TAG = "FrameAnalyzer"
        private const val DETECTION_TIME_WARN_MS = 50L
    }
}

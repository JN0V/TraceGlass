package io.github.jn0v.traceglass.core.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface CameraManager : Closeable {
    val isCameraReady: StateFlow<Boolean>
    val cameraError: StateFlow<String?>
    fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        imageAnalyzer: ImageAnalysis.Analyzer? = null
    )
    fun unbind()
    fun reapplyZoom()
}

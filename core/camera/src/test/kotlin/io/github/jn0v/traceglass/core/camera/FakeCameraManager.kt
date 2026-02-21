package io.github.jn0v.traceglass.core.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeCameraManager : CameraManager {

    private val _isCameraReady = MutableStateFlow(false)
    override val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    private val _cameraError = MutableStateFlow<String?>(null)
    override val cameraError: StateFlow<String?> = _cameraError.asStateFlow()

    override fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        imageAnalyzer: ImageAnalysis.Analyzer?
    ) {
        _isCameraReady.value = true
    }

    override fun unbind() {
        _isCameraReady.value = false
    }

    override fun reapplyZoom() {}

    var closeCalled = false
        private set

    override fun close() {
        closeCalled = true
        unbind()
    }
}

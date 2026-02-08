package io.github.jn0v.traceglass.core.camera.impl

import android.content.Context
import android.content.pm.PackageManager
import java.util.concurrent.Executors
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import io.github.jn0v.traceglass.core.camera.CameraManager
import io.github.jn0v.traceglass.core.camera.FlashlightController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraXManager(private val context: Context) : CameraManager, FlashlightController {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private val _isTorchOn = MutableStateFlow(false)
    override val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    override val hasFlashlight: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

    override fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        imageAnalyzer: ImageAnalysis.Analyzer?
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = surfaceProvider
            }

            val useCases = mutableListOf<androidx.camera.core.UseCase>(preview)

            if (imageAnalyzer != null) {
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, imageAnalyzer) }
                useCases.add(analysis)
            }

            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                *useCases.toTypedArray()
            )
            cameraProvider = provider
        }, ContextCompat.getMainExecutor(context))
    }

    override fun unbind() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        camera = null
        _isTorchOn.value = false
    }

    override fun toggleTorch() {
        val cam = camera ?: return
        if (!cam.cameraInfo.hasFlashUnit()) return
        val newState = !_isTorchOn.value
        cam.cameraControl.enableTorch(newState)
        _isTorchOn.value = newState
    }
}

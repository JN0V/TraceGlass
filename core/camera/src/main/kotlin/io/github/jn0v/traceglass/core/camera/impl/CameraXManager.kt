package io.github.jn0v.traceglass.core.camera.impl

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import android.util.Size
import java.util.concurrent.Executors
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import io.github.jn0v.traceglass.core.camera.CameraManager
import io.github.jn0v.traceglass.core.camera.FlashlightController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
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

            val cameraSelector = buildWideAngleSelector(provider)

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = surfaceProvider
            }

            val useCases = mutableListOf<androidx.camera.core.UseCase>(preview)

            if (imageAnalyzer != null) {
                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                        )
                    )
                    .build()

                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, imageAnalyzer) }
                useCases.add(analysis)
            }

            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *useCases.toTypedArray()
            )

            logSelectedCamera(camera)
            setWidestZoom(camera)

            cameraProvider = provider
        }, ContextCompat.getMainExecutor(context))
    }

    override fun unbind() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        camera = null
        _isTorchOn.value = false
        analysisExecutor.shutdown()
    }

    override fun toggleTorch() {
        val cam = camera ?: run {
            Log.w(TAG, "toggleTorch called but camera not yet bound")
            return
        }
        if (!cam.cameraInfo.hasFlashUnit()) return
        val newState = !_isTorchOn.value
        val future = cam.cameraControl.enableTorch(newState)
        future.addListener({
            try {
                future.get()
                _isTorchOn.value = newState
            } catch (e: Exception) {
                Log.w(TAG, "Failed to toggle torch", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Selects the back-facing camera with the widest field of view
     * (shortest focal length). Falls back to DEFAULT_BACK_CAMERA
     * if Camera2 interop info is unavailable.
     */

    private fun buildWideAngleSelector(provider: ProcessCameraProvider): CameraSelector {
        try {
            val backCameras = provider.availableCameraInfos.filter {
                it.lensFacing == CameraSelector.LENS_FACING_BACK
            }

            if (backCameras.size <= 1) {
                return CameraSelector.DEFAULT_BACK_CAMERA
            }

            val widest = backCameras.minByOrNull { cameraInfo ->
                val cam2Info = Camera2CameraInfo.from(cameraInfo)
                val focalLengths = cam2Info.getCameraCharacteristic(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                )
                val minFocal = focalLengths?.minOrNull() ?: Float.MAX_VALUE
                Log.d(TAG, "Camera: focal=${minFocal}mm, id=${cam2Info.cameraId}")
                minFocal
            }

            if (widest != null) {
                val cam2Info = Camera2CameraInfo.from(widest)
                val cameraId = cam2Info.cameraId
                Log.d(TAG, "Selected widest camera: id=$cameraId")
                return CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .addCameraFilter { cameras ->
                        cameras.filter { info ->
                            Camera2CameraInfo.from(info).cameraId == cameraId
                        }
                    }
                    .build()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to select wide-angle camera, using default", e)
        }

        return CameraSelector.DEFAULT_BACK_CAMERA
    }


    private fun logSelectedCamera(camera: Camera?) {
        camera ?: return
        try {
            val cam2Info = Camera2CameraInfo.from(camera.cameraInfo)
            val focalLengths = cam2Info.getCameraCharacteristic(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            )
            val sensorSize = cam2Info.getCameraCharacteristic(
                CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
            )
            Log.d(TAG, "Active camera: id=${cam2Info.cameraId}, " +
                "focal=${focalLengths?.contentToString()}, " +
                "sensor=${sensorSize?.width}x${sensorSize?.height}mm")
        } catch (e: Exception) {
            Log.w(TAG, "Could not log camera info", e)
        }
    }

    private fun setWidestZoom(camera: Camera?) {
        camera ?: return
        try {
            val zoomState = camera.cameraInfo.zoomState.value
            val minZoom = zoomState?.minZoomRatio ?: 1f
            if (minZoom < 1f) {
                camera.cameraControl.setZoomRatio(minZoom)
                Log.d(TAG, "Zoom set to min=$minZoom for widest FOV")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set zoom", e)
        }
    }

    override fun reapplyZoom() {
        setWidestZoom(camera)
    }

    companion object {
        private const val TAG = "CameraXManager"
    }
}

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
import io.github.jn0v.traceglass.core.camera.FocalLengthCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
class CameraXManager(private val context: Context) : CameraManager, FlashlightController {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private val _isCameraReady = MutableStateFlow(false)
    override val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    private val _cameraError = MutableStateFlow<String?>(null)
    override val cameraError: StateFlow<String?> = _cameraError.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    override val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _focalLengthPixels = MutableStateFlow<Float?>(null)
    override val focalLengthPixels: StateFlow<Float?> = _focalLengthPixels.asStateFlow()

    // Stored sensor data for re-computing focal length on zoom change
    private var storedFocalMm: Float? = null
    private var storedSensorWidthMm: Float? = null
    // Actual analysis resolution granted by CameraX (may differ from ANALYSIS_TARGET_WIDTH)
    private var actualAnalysisWidth: Int = ANALYSIS_TARGET_WIDTH

    override val hasFlashlight: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)


    override fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        imageAnalyzer: ImageAnalysis.Analyzer?
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        _cameraError.value = null
        future.addListener({
            try {
                val provider = future.get()

                val cameraSelector = buildWideAngleSelector(provider)

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = surfaceProvider
                }

                val useCases = mutableListOf<androidx.camera.core.UseCase>(preview)

                var analysisUseCase: ImageAnalysis? = null
                if (imageAnalyzer != null) {
                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                            )
                        )
                        .build()

                    analysisUseCase = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(analysisExecutor, imageAnalyzer) }
                    useCases.add(analysisUseCase)
                }

                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    *useCases.toTypedArray()
                )

                // Query the actual granted analysis resolution (may differ from
                // ANALYSIS_TARGET_WIDTH due to device fallback rules).
                // CameraX reports dimensions in sensor orientation (landscape),
                // so width is always the longer edge, matching sensorWidthMm.
                actualAnalysisWidth = analysisUseCase?.resolutionInfo?.resolution?.width
                    ?: ANALYSIS_TARGET_WIDTH

                logSelectedCamera(camera)
                val zoomRatio = setWidestZoom(camera)
                emitFocalLength(camera, zoomRatio)

                cameraProvider = provider
                _isCameraReady.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                _cameraError.value = e.message ?: "Camera initialization failed"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun unbind() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        camera = null
        _isCameraReady.value = false
        _isTorchOn.value = false
        _focalLengthPixels.value = null
        storedFocalMm = null
        storedSensorWidthMm = null
        actualAnalysisWidth = ANALYSIS_TARGET_WIDTH
        // Don't shutdown analysisExecutor — it's reused across bind/unbind cycles
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

    /**
     * Sets zoom to widest FOV and returns the effective zoom ratio.
     */
    private fun setWidestZoom(camera: Camera?): Float {
        camera ?: return 1f
        try {
            val zoomState = camera.cameraInfo.zoomState.value
            val minZoom = zoomState?.minZoomRatio ?: 1f
            if (minZoom < 1f) {
                camera.cameraControl.setZoomRatio(minZoom)
                Log.d(TAG, "Zoom set to min=$minZoom for widest FOV")
                return minZoom
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set zoom", e)
        }
        return 1f
    }

    override fun reapplyZoom() {
        val zoomRatio = setWidestZoom(camera)
        reemitFocalLength(zoomRatio)
    }

    private fun emitFocalLength(camera: Camera?, zoomRatio: Float) {
        camera ?: return
        try {
            val cam2Info = Camera2CameraInfo.from(camera.cameraInfo)
            val focalLengths = cam2Info.getCameraCharacteristic(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            )
            val sensorSize = cam2Info.getCameraCharacteristic(
                CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
            )

            // minOrNull() picks the shortest available focal length, matching our camera
            // selection (buildWideAngleSelector also picks the shortest). For fixed-lens
            // phones (the vast majority), this array has exactly one element.
            val focalMm = focalLengths?.minOrNull()
            val sensorWidthMm = sensorSize?.width

            if (focalMm == null || sensorWidthMm == null) {
                Log.w(TAG, "Missing focal length or sensor size data, skipping f injection")
                _focalLengthPixels.value = null
                return
            }

            storedFocalMm = focalMm
            storedSensorWidthMm = sensorWidthMm

            val fPixels = FocalLengthCalculator.computePixels(
                focalLengthMm = focalMm,
                sensorWidthMm = sensorWidthMm,
                analysisWidth = actualAnalysisWidth,
                zoomRatio = zoomRatio
            )

            _focalLengthPixels.value = fPixels
            Log.d(TAG, "Focal length: ${focalMm}mm, sensor=${sensorWidthMm}mm, " +
                "analysis=${actualAnalysisWidth}px, zoom=${zoomRatio}, f_pixels=$fPixels")
        } catch (e: Exception) {
            Log.w(TAG, "Could not compute focal length", e)
            _focalLengthPixels.value = null
        }
    }

    private fun reemitFocalLength(zoomRatio: Float) {
        val fMm = storedFocalMm ?: return
        val sWMm = storedSensorWidthMm ?: return
        val fPixels = FocalLengthCalculator.computePixels(
            focalLengthMm = fMm,
            sensorWidthMm = sWMm,
            analysisWidth = actualAnalysisWidth,
            zoomRatio = zoomRatio
        )
        _focalLengthPixels.value = fPixels
        Log.d(TAG, "Focal length re-emitted: zoom=$zoomRatio, f_pixels=$fPixels")
    }

    companion object {
        private const val TAG = "CameraXManager"
        private const val ANALYSIS_TARGET_WIDTH = 1280
    }
}

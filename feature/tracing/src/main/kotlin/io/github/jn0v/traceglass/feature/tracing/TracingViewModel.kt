package io.github.jn0v.traceglass.feature.tracing

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jn0v.traceglass.core.camera.FlashlightController
import io.github.jn0v.traceglass.core.cv.MarkerResult
import io.github.jn0v.traceglass.core.overlay.OverlayTransform
import io.github.jn0v.traceglass.core.overlay.OverlayTransformCalculator
import io.github.jn0v.traceglass.core.overlay.TrackingStateManager
import io.github.jn0v.traceglass.core.overlay.TrackingStatus
import io.github.jn0v.traceglass.core.session.SessionData
import io.github.jn0v.traceglass.core.session.SessionRepository
import io.github.jn0v.traceglass.core.session.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class TracingViewModel(
    private val flashlightController: FlashlightController,
    private val transformCalculator: OverlayTransformCalculator = OverlayTransformCalculator(),
    private val trackingStateManager: TrackingStateManager = TrackingStateManager(),
    private val sessionRepository: SessionRepository? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TracingUiState(hasFlashlight = flashlightController.hasFlashlight)
    )
    val uiState: StateFlow<TracingUiState> = _uiState.asStateFlow()

    private var breakReminderEnabled = false
    private var breakReminderIntervalMinutes = 30
    private var audioFeedbackEnabled = false
    private var breakTimerJob: Job? = null

    private var previousTransform: OverlayTransform = OverlayTransform.IDENTITY
    private var manualOffset: Offset = Offset.Zero
    private var manualScaleFactor: Float = 1f
    private var manualRotation: Float = 0f

    // View dimensions for frame→screen coordinate scaling
    private var viewWidth: Float = 1080f
    private var viewHeight: Float = 1920f
    private var lastFrameWidth: Float = 1080f
    private var lastFrameHeight: Float = 1920f

    init {
        flashlightController.isTorchOn
            .onEach { torchOn -> _uiState.update { it.copy(isTorchOn = torchOn) } }
            .launchIn(viewModelScope)

        settingsRepository?.settingsData
            ?.onEach { data ->
                val intervalChanged = breakReminderIntervalMinutes != data.breakReminderIntervalMinutes
                val enabledChanged = breakReminderEnabled != data.breakReminderEnabled
                breakReminderEnabled = data.breakReminderEnabled
                breakReminderIntervalMinutes = data.breakReminderIntervalMinutes
                audioFeedbackEnabled = data.audioFeedbackEnabled
                _uiState.update { it.copy(audioFeedbackEnabled = data.audioFeedbackEnabled) }
                if (intervalChanged || enabledChanged) {
                    restartBreakTimer()
                }
            }
            ?.launchIn(viewModelScope)
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                permissionState = if (granted) PermissionState.GRANTED else PermissionState.DENIED
            )
        }
    }

    fun onToggleTorch() {
        flashlightController.toggleTorch()
    }

    fun onImageSelected(uri: Uri?) {
        uri ?: return
        _uiState.update { it.copy(overlayImageUri = uri) }
    }

    fun onOpacityChanged(opacity: Float) {
        _uiState.update { it.copy(overlayOpacity = opacity.coerceIn(0f, 1f)) }
    }

    fun onToggleOpacitySlider() {
        _uiState.update { it.copy(isOpacitySliderVisible = !it.isOpacitySliderVisible) }
    }

    fun onColorTintChanged(tint: ColorTint) {
        _uiState.update { it.copy(colorTint = tint) }
    }

    fun onToggleInvertedMode() {
        _uiState.update { it.copy(isInvertedMode = !it.isInvertedMode) }
    }

    fun onOverlayDrag(delta: Offset) {
        manualOffset += delta
        updateOverlayFromCombined()
    }

    fun onOverlayScale(scaleFactor: Float) {
        manualScaleFactor *= scaleFactor
        updateOverlayFromCombined()
    }

    fun onOverlayRotate(angleDelta: Float) {
        manualRotation += angleDelta
        updateOverlayFromCombined()
    }

    fun setViewDimensions(width: Float, height: Float) {
        if (width > 0f && height > 0f) {
            viewWidth = width
            viewHeight = height
        }
    }

    fun onToggleSession() {
        _uiState.update { it.copy(isSessionActive = !it.isSessionActive) }
        restartBreakTimer()
        viewModelScope.launch { saveSession() }
    }

    fun onToggleControlsVisibility() {
        _uiState.update { it.copy(areControlsVisible = !it.areControlsVisible) }
    }

    suspend fun saveSession() {
        val repo = sessionRepository ?: return
        val state = _uiState.value
        repo.save(
            SessionData(
                imageUri = state.overlayImageUri?.toString(),
                overlayOffsetX = state.overlayOffset.x,
                overlayOffsetY = state.overlayOffset.y,
                overlayScale = state.overlayScale,
                overlayOpacity = state.overlayOpacity,
                colorTint = state.colorTint.name,
                isInvertedMode = state.isInvertedMode,
                isSessionActive = state.isSessionActive
            )
        )
    }

    suspend fun restoreSession() {
        val repo = sessionRepository ?: return
        val data = repo.sessionData.first()
        if (!data.hasActiveSession) return
        _uiState.update {
            it.copy(
                overlayImageUri = data.imageUri?.let { uri -> Uri.parse(uri) },
                overlayOffset = Offset(data.overlayOffsetX, data.overlayOffsetY),
                overlayScale = data.overlayScale,
                overlayOpacity = data.overlayOpacity,
                colorTint = ColorTint.entries.find { t -> t.name == data.colorTint } ?: ColorTint.NONE,
                isInvertedMode = data.isInvertedMode,
                isSessionActive = data.isSessionActive
            )
        }
    }

    fun onBreakReminderDismissed() {
        _uiState.update { it.copy(showBreakReminder = false) }
        restartBreakTimer()
    }

    private fun restartBreakTimer() {
        breakTimerJob?.cancel()
        if (!breakReminderEnabled || !_uiState.value.isSessionActive) return
        breakTimerJob = viewModelScope.launch {
            delay(breakReminderIntervalMinutes * 60_000L)
            _uiState.update { it.copy(showBreakReminder = true) }
        }
    }

    fun onMarkerResultReceived(result: MarkerResult) {
        val status = trackingStateManager.onMarkerResult(result)
        val trackingState = when (status) {
            TrackingStatus.INACTIVE -> TrackingState.INACTIVE
            TrackingStatus.TRACKING -> TrackingState.TRACKING
            TrackingStatus.LOST -> TrackingState.LOST
        }

        val frameW = result.frameWidth.toFloat().takeIf { it > 0f } ?: lastFrameWidth
        val frameH = result.frameHeight.toFloat().takeIf { it > 0f } ?: lastFrameHeight
        lastFrameWidth = frameW
        lastFrameHeight = frameH

        val transform = transformCalculator.computeSmoothed(
            result, frameW, frameH, previousTransform
        )
        previousTransform = transform

        _uiState.update {
            it.copy(
                trackingState = trackingState,
                detectedMarkerCount = result.markerCount
            )
        }
        updateOverlayFromCombined()
    }

    /**
     * Computes the preview scale factor (FILL_CENTER behavior):
     * the camera preview is scaled uniformly to fill the view,
     * so frame offsets must be multiplied by this factor to match screen pixels.
     */
    private fun previewScale(): Float {
        return maxOf(viewWidth / lastFrameWidth, viewHeight / lastFrameHeight)
    }

    private fun updateOverlayFromCombined() {
        val scale = previewScale()
        _uiState.update {
            it.copy(
                overlayOffset = Offset(
                    previousTransform.offsetX * scale + manualOffset.x,
                    previousTransform.offsetY * scale + manualOffset.y
                ),
                overlayScale = previousTransform.scale * manualScaleFactor,
                overlayRotation = previousTransform.rotation + manualRotation
            )
        }
    }
}

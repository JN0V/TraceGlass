package io.github.jn0v.traceglass.feature.tracing

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jn0v.traceglass.core.camera.FlashlightController
import io.github.jn0v.traceglass.core.cv.MarkerResult
import io.github.jn0v.traceglass.core.overlay.OverlayTransformCalculator
import io.github.jn0v.traceglass.core.overlay.TrackingStateManager
import io.github.jn0v.traceglass.core.overlay.TrackingStatus
import io.github.jn0v.traceglass.core.session.SessionData
import io.github.jn0v.traceglass.core.session.SessionRepository
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
    private val sessionRepository: SessionRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TracingUiState(hasFlashlight = flashlightController.hasFlashlight)
    )
    val uiState: StateFlow<TracingUiState> = _uiState.asStateFlow()

    init {
        flashlightController.isTorchOn
            .onEach { torchOn -> _uiState.update { it.copy(isTorchOn = torchOn) } }
            .launchIn(viewModelScope)
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
        _uiState.update {
            it.copy(overlayOffset = it.overlayOffset + delta)
        }
    }

    fun onOverlayScale(scaleFactor: Float) {
        _uiState.update {
            it.copy(overlayScale = it.overlayScale * scaleFactor)
        }
    }

    fun onToggleSession() {
        _uiState.update { it.copy(isSessionActive = !it.isSessionActive) }
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

    fun onMarkerResultReceived(
        result: MarkerResult,
        frameWidth: Float = 1080f,
        frameHeight: Float = 1920f
    ) {
        val status = trackingStateManager.onMarkerResult(result)
        val trackingState = when (status) {
            TrackingStatus.INACTIVE -> TrackingState.INACTIVE
            TrackingStatus.TRACKING -> TrackingState.TRACKING
            TrackingStatus.LOST -> TrackingState.LOST
        }

        if (result.isTracking) {
            val transform = transformCalculator.compute(result, frameWidth, frameHeight)
            _uiState.update {
                it.copy(
                    trackingState = trackingState,
                    overlayOffset = Offset(transform.offsetX, transform.offsetY),
                    overlayScale = transform.scale
                )
            }
        } else {
            _uiState.update { it.copy(trackingState = trackingState) }
        }
    }
}

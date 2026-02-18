package io.github.jn0v.traceglass.feature.tracing

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

data class TracingUiState(
    val permissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val isTorchOn: Boolean = false,
    val hasFlashlight: Boolean = true,
    val overlayImageUri: Uri? = null,
    val overlayOpacity: Float = 0.5f,
    val isOpacitySliderVisible: Boolean = false,
    val colorTint: ColorTint = ColorTint.NONE,
    val isInvertedMode: Boolean = false,
    val overlayOffset: Offset = Offset.Zero,
    val overlayScale: Float = 1f,
    val overlayRotation: Float = 0f,
    val isSessionActive: Boolean = false,
    val areControlsVisible: Boolean = true,
    val trackingState: TrackingState = TrackingState.INACTIVE,
    val detectedMarkerCount: Int = 0,
    val showBreakReminder: Boolean = false,
    val audioFeedbackEnabled: Boolean = false,
    val showResumeSessionDialog: Boolean = false,
    val isOverlayLocked: Boolean = false,
    val viewportZoom: Float = 1f,
    val viewportPanX: Float = 0f,
    val viewportPanY: Float = 0f,
    val showUnlockConfirmDialog: Boolean = false,
    val showLockSnackbar: Boolean = false
) {
    val effectiveOpacity: Float
        get() = if (isInvertedMode) 1f - overlayOpacity else overlayOpacity
}

enum class TrackingState {
    INACTIVE,
    TRACKING,
    LOST
}

enum class ColorTint {
    NONE,
    RED,
    GREEN,
    BLUE,
    GRAYSCALE;

    fun toColorFilter(): ColorFilter? = when (this) {
        NONE -> null
        RED -> ColorFilter.tint(
            Color.Red.copy(alpha = 0.5f),
            BlendMode.Modulate
        )
        GREEN -> ColorFilter.tint(
            Color.Green.copy(alpha = 0.5f),
            BlendMode.Modulate
        )
        BLUE -> ColorFilter.tint(
            Color.Blue.copy(alpha = 0.5f),
            BlendMode.Modulate
        )
        GRAYSCALE -> ColorFilter.colorMatrix(
            ColorMatrix().apply { setToSaturation(0f) }
        )
    }
}

enum class PermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED
}

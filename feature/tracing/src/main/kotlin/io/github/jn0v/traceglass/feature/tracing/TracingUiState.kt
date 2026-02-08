package io.github.jn0v.traceglass.feature.tracing

import android.net.Uri

data class TracingUiState(
    val permissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val isTorchOn: Boolean = false,
    val hasFlashlight: Boolean = true,
    val overlayImageUri: Uri? = null,
    val overlayOpacity: Float = 0.5f,
    val isOpacitySliderVisible: Boolean = false,
    val colorTint: ColorTint = ColorTint.NONE,
    val isInvertedMode: Boolean = false
)

enum class ColorTint {
    NONE,
    RED,
    GREEN,
    BLUE,
    GRAYSCALE;

    fun toColorFilter(): androidx.compose.ui.graphics.ColorFilter? = when (this) {
        NONE -> null
        RED -> androidx.compose.ui.graphics.ColorFilter.tint(
            androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.5f),
            androidx.compose.ui.graphics.BlendMode.Modulate
        )
        GREEN -> androidx.compose.ui.graphics.ColorFilter.tint(
            androidx.compose.ui.graphics.Color.Green.copy(alpha = 0.5f),
            androidx.compose.ui.graphics.BlendMode.Modulate
        )
        BLUE -> androidx.compose.ui.graphics.ColorFilter.tint(
            androidx.compose.ui.graphics.Color.Blue.copy(alpha = 0.5f),
            androidx.compose.ui.graphics.BlendMode.Modulate
        )
        GRAYSCALE -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
        )
    }
}

enum class PermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED
}

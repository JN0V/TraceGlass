package io.github.jn0v.traceglass.feature.tracing

import android.net.Uri

data class TracingUiState(
    val permissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val isTorchOn: Boolean = false,
    val hasFlashlight: Boolean = true,
    val overlayImageUri: Uri? = null,
    val overlayOpacity: Float = 0.5f
)

enum class PermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED
}

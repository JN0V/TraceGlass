package io.github.jn0v.traceglass.feature.tracing

data class TracingUiState(
    val permissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val isTorchOn: Boolean = false,
    val hasFlashlight: Boolean = true
)

enum class PermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED
}

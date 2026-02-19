package io.github.jn0v.traceglass.core.session

data class SessionData(
    val imageUri: String? = null,
    val overlayOffsetX: Float = 0f,
    val overlayOffsetY: Float = 0f,
    val overlayScale: Float = 1f,
    val overlayRotation: Float = 0f,
    val overlayOpacity: Float = 0.5f,
    val colorTint: String = "NONE",
    val isInvertedMode: Boolean = false,
    val isSessionActive: Boolean = false,
    val timelapseSnapshotCount: Int = 0,
    val isTimelapseRecording: Boolean = false,
    val isTimelapsePaused: Boolean = false,
    val isOverlayLocked: Boolean = false,
    val viewportZoom: Float = 1f,
    val viewportPanX: Float = 0f,
    val viewportPanY: Float = 0f
) {
    val hasSavedOverlay: Boolean
        get() = imageUri != null
}

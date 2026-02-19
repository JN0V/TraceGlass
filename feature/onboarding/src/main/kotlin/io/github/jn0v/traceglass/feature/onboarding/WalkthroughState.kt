package io.github.jn0v.traceglass.feature.onboarding

enum class WalkthroughStep {
    DETECTING_MARKERS,
    MARKERS_FOUND,
    PICK_IMAGE,
    SHOW_TOOLTIP,
    COMPLETED
}

data class WalkthroughUiState(
    val step: WalkthroughStep = WalkthroughStep.DETECTING_MARKERS,
    val elapsedSeconds: Int = 0,
    val markersDetected: Boolean = false,
    val showGuidance: Boolean = false,
    val imageUri: String? = null
)

package io.github.jn0v.traceglass.feature.onboarding

import io.github.jn0v.traceglass.core.cv.DetectionMode

data class OnboardingUiState(
    val currentPage: Int = 0,
    val selectedTier: SetupTier = SetupTier.FULL_DIY,
    val detectionMode: DetectionMode = DetectionMode.ARUCO,
    val isCompleted: Boolean = false,
    val wasSkipped: Boolean = false,
    val isReopened: Boolean = false
)

enum class SetupTier {
    FULL_DIY,
    SEMI_EQUIPPED,
    FULL_KIT
}

enum class OnboardingMode {
    FIRST_TIME,
    REOPENED
}

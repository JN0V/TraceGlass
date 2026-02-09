package io.github.jn0v.traceglass.feature.onboarding

data class OnboardingUiState(
    val currentPage: Int = 0,
    val selectedTier: SetupTier = SetupTier.FULL_DIY,
    val isCompleted: Boolean = false
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

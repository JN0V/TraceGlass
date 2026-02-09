package io.github.jn0v.traceglass.feature.onboarding

data class SetupGuideUiState(
    val selectedSection: SetupGuideSection = SetupGuideSection.MARKER_GUIDE
)

enum class SetupGuideSection {
    MARKER_GUIDE,
    MACGYVER_GUIDE
}

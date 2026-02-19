package io.github.jn0v.traceglass.feature.onboarding

import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val isOnboardingCompleted: Flow<Boolean>
    val isTooltipShown: Flow<Boolean>
    val selectedTier: Flow<SetupTier?>
    suspend fun setOnboardingCompleted()
    suspend fun setTooltipShown()
    suspend fun setSelectedTier(tier: SetupTier)
    suspend fun resetOnboarding()
}

package io.github.jn0v.traceglass.feature.onboarding

import io.github.jn0v.traceglass.core.cv.DetectionMode
import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val isOnboardingCompleted: Flow<Boolean>
    val isTooltipShown: Flow<Boolean>
    val selectedTier: Flow<SetupTier?>
    val detectionMode: Flow<DetectionMode>
    suspend fun setOnboardingCompleted()
    suspend fun setTooltipShown()
    suspend fun setSelectedTier(tier: SetupTier)
    suspend fun setDetectionMode(mode: DetectionMode)
    suspend fun resetOnboarding()
}

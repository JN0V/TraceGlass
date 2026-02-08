package io.github.jn0v.traceglass.feature.onboarding

import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val isOnboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted()
    suspend fun resetOnboarding()
}

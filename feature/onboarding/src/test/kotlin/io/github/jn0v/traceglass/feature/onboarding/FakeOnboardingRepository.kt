package io.github.jn0v.traceglass.feature.onboarding

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeOnboardingRepository : OnboardingRepository {

    private val _completed = MutableStateFlow(false)
    override val isOnboardingCompleted: Flow<Boolean> = _completed.asStateFlow()

    var setCompletedCount: Int = 0
        private set
    var resetCount: Int = 0
        private set

    override suspend fun setOnboardingCompleted() {
        setCompletedCount++
        _completed.value = true
    }

    override suspend fun resetOnboarding() {
        resetCount++
        _completed.value = false
    }
}

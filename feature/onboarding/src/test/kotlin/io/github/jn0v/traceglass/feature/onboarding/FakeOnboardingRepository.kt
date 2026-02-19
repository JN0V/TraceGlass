package io.github.jn0v.traceglass.feature.onboarding

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeOnboardingRepository : OnboardingRepository {

    private val _completed = MutableStateFlow(false)
    override val isOnboardingCompleted: Flow<Boolean> = _completed.asStateFlow()

    private val _tooltipShown = MutableStateFlow(false)
    override val isTooltipShown: Flow<Boolean> = _tooltipShown.asStateFlow()

    private val _selectedTier = MutableStateFlow<SetupTier?>(null)
    override val selectedTier: Flow<SetupTier?> = _selectedTier.asStateFlow()

    var setCompletedCount: Int = 0
        private set
    var setTooltipShownCount: Int = 0
        private set
    var setTierCount: Int = 0
        private set
    var resetCount: Int = 0
        private set

    override suspend fun setOnboardingCompleted() {
        setCompletedCount++
        _completed.value = true
    }

    override suspend fun setTooltipShown() {
        setTooltipShownCount++
        _tooltipShown.value = true
    }

    override suspend fun setSelectedTier(tier: SetupTier) {
        setTierCount++
        _selectedTier.value = tier
    }

    override suspend fun resetOnboarding() {
        resetCount++
        _completed.value = false
        _tooltipShown.value = false
        _selectedTier.value = null
    }
}

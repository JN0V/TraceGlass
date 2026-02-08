package io.github.jn0v.traceglass.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onPageChanged(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun onTierSelected(tier: SetupTier) {
        _uiState.update { it.copy(selectedTier = tier) }
    }

    fun onNextPage() {
        _uiState.update { it.copy(currentPage = (it.currentPage + 1).coerceAtMost(2)) }
    }

    fun onComplete() {
        viewModelScope.launch {
            repository.setOnboardingCompleted()
            _uiState.update { it.copy(isCompleted = true) }
        }
    }

    fun onSkip() {
        onComplete()
    }
}

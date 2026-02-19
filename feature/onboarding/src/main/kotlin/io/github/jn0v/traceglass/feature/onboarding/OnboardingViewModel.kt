package io.github.jn0v.traceglass.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedTier = repository.selectedTier.first()
            if (savedTier != null) {
                _uiState.update { it.copy(selectedTier = savedTier) }
            }
        }
    }

    fun onPageChanged(page: Int) {
        val clamped = page.coerceIn(0, PAGE_COUNT - 1)
        _uiState.update { it.copy(currentPage = clamped) }
    }

    fun onTierSelected(tier: SetupTier) {
        _uiState.update { it.copy(selectedTier = tier) }
        viewModelScope.launch { repository.setSelectedTier(tier) }
    }

    fun onNextPage() {
        _uiState.update { it.copy(currentPage = (it.currentPage + 1).coerceAtMost(PAGE_COUNT - 1)) }
    }

    fun onPreviousPage() {
        _uiState.update { it.copy(currentPage = (it.currentPage - 1).coerceAtLeast(0)) }
    }

    fun onReopen() {
        _uiState.update { it.copy(currentPage = 0, isReopened = true) }
    }

    fun onComplete() {
        viewModelScope.launch {
            if (!_uiState.value.isReopened) {
                repository.setOnboardingCompleted()
            }
            _uiState.update { it.copy(isCompleted = true, wasSkipped = false) }
        }
    }

    fun onSkip() {
        viewModelScope.launch {
            if (!_uiState.value.isReopened) {
                repository.setOnboardingCompleted()
            }
            _uiState.update { it.copy(isCompleted = true, wasSkipped = true) }
        }
    }

    companion object {
        const val PAGE_COUNT = 3
    }
}

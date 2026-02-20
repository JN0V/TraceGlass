package io.github.jn0v.traceglass.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jn0v.traceglass.core.session.SessionData
import io.github.jn0v.traceglass.core.session.SessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WalkthroughViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalkthroughUiState())
    val uiState: StateFlow<WalkthroughUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private val guidanceTimeoutSeconds = 10

    fun startDetection() {
        _uiState.update { it.copy(step = WalkthroughStep.DETECTING_MARKERS, elapsedSeconds = 0) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (!_uiState.value.markersDetected) {
                delay(1000)
                _uiState.update { state ->
                    val elapsed = state.elapsedSeconds + 1
                    state.copy(
                        elapsedSeconds = elapsed,
                        showGuidance = elapsed >= guidanceTimeoutSeconds && !state.markersDetected
                    )
                }
            }
        }
    }

    fun onMarkersDetected() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                step = WalkthroughStep.MARKERS_FOUND,
                markersDetected = true,
                showGuidance = false
            )
        }
    }

    fun onProceedToPickImage() {
        _uiState.update { it.copy(step = WalkthroughStep.PICK_IMAGE) }
    }

    fun onImagePicked(imageUri: String) {
        viewModelScope.launch {
            sessionRepository.save(SessionData(imageUri = imageUri))
        }
        _uiState.update { it.copy(step = WalkthroughStep.SHOW_TOOLTIP, imageUri = imageUri) }
    }

    fun onTooltipDismissed() {
        viewModelScope.launch {
            onboardingRepository.setTooltipShown()
        }
        _uiState.update { it.copy(step = WalkthroughStep.COMPLETED) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

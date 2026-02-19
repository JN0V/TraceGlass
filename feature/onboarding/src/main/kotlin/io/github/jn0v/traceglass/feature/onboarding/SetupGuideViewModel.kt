package io.github.jn0v.traceglass.feature.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SetupGuideViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SetupGuideUiState())
    val uiState: StateFlow<SetupGuideUiState> = _uiState.asStateFlow()

    fun onSectionSelected(section: SetupGuideSection) {
        _uiState.update { it.copy(selectedSection = section) }
    }

    companion object {
        const val MARKER_SHEET_URL =
            "https://github.com/jn0v/traceglass/releases/latest/download/markers.pdf"
        const val STAND_MODEL_URL =
            "https://github.com/jn0v/traceglass/releases/latest/download/phone-stand.stl"
    }
}

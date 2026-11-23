package io.github.jn0v.traceglass.feature.tracing

import androidx.compose.ui.geometry.Offset
import io.github.jn0v.traceglass.core.camera.FlashlightController
import io.github.jn0v.traceglass.feature.tracing.settings.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TracingViewModelZoomPivotTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class StubFlashlight : FlashlightController {
        private val _isTorchOn = MutableStateFlow(false)
        override val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()
        override val hasFlashlight: Boolean = true
        override fun toggleTorch() {
            _isTorchOn.value = !_isTorchOn.value
        }
    }

    private fun createViewModel(): TracingViewModel =
        TracingViewModel(
            flashlightController = StubFlashlight(),
            sessionRepository = FakeSessionRepository(),
            settingsRepository = FakeSettingsRepository(),
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

    // Given an overlay offset of (300, 0) on a 1080x1920 view
    // When I zoom 2x at (840, 960), the image's current centre
    // Then the offset is still (300, 0)
    @Test
    fun `zoom after a drag keeps the offset`() {
        val viewModel = createViewModel()
        viewModel.setViewDimensions(1080f, 1920f)
        viewModel.onOverlayDrag(Offset(300f, 0f))
        assertEquals(300f, viewModel.uiState.value.overlayOffset.x, 0.1f)

        viewModel.onOverlayGesture(Offset(840f, 960f), Offset.Zero, 2f, 0f)

        assertEquals(300f, viewModel.uiState.value.overlayOffset.x, 0.1f)
        assertEquals(0f, viewModel.uiState.value.overlayOffset.y, 0.1f)
        assertEquals(2f, viewModel.uiState.value.overlayScale, 0.001f)
    }
}

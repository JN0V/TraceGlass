package io.github.jn0v.traceglass.feature.onboarding

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WalkthroughViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var vm: WalkthroughViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        vm = WalkthroughViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Wraps [runTest] and cancels [vm]'s viewModelScope in a finally block.
     * Prevents runTest from hanging when the ViewModel has infinite coroutines
     * (e.g. the detection timer's while(true) loop).
     */
    private fun runVmTest(block: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
        try {
            block()
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Nested
    inner class Detection {
        @Test
        fun `initial step is DETECTING_MARKERS`() {
            assertEquals(WalkthroughStep.DETECTING_MARKERS, vm.uiState.value.step)
        }

        @Test
        fun `startDetection resets state`() = runVmTest {
            vm.startDetection()
            advanceTimeBy(100)
            assertEquals(WalkthroughStep.DETECTING_MARKERS, vm.uiState.value.step)
            assertEquals(0, vm.uiState.value.elapsedSeconds)
        }

        @Test
        fun `timer increments elapsed seconds`() = runVmTest {
            vm.startDetection()
            advanceTimeBy(3100)
            assertEquals(3, vm.uiState.value.elapsedSeconds)
        }

        @Test
        fun `shows guidance after 10 seconds without markers`() = runVmTest {
            vm.startDetection()
            advanceTimeBy(10100)
            assertTrue(vm.uiState.value.showGuidance)
        }

        @Test
        fun `no guidance before 10 seconds`() = runVmTest {
            vm.startDetection()
            advanceTimeBy(9100)
            assertFalse(vm.uiState.value.showGuidance)
        }
    }

    @Nested
    inner class MarkerDetection {
        @Test
        fun `onMarkersDetected transitions to MARKERS_FOUND`() = runVmTest {
            vm.startDetection()
            advanceTimeBy(2000)
            vm.onMarkersDetected()
            assertEquals(WalkthroughStep.MARKERS_FOUND, vm.uiState.value.step)
            assertTrue(vm.uiState.value.markersDetected)
        }

        @Test
        fun `onMarkersDetected hides guidance`() = runVmTest {
            vm.startDetection()
            advanceTimeBy(11000)
            assertTrue(vm.uiState.value.showGuidance)
            vm.onMarkersDetected()
            assertFalse(vm.uiState.value.showGuidance)
        }
    }

    @Nested
    inner class Flow {
        @Test
        fun `onProceedToPickImage transitions to PICK_IMAGE`() {
            vm.onProceedToPickImage()
            assertEquals(WalkthroughStep.PICK_IMAGE, vm.uiState.value.step)
        }

        @Test
        fun `onImagePicked transitions to SHOW_TOOLTIP`() {
            vm.onImagePicked()
            assertEquals(WalkthroughStep.SHOW_TOOLTIP, vm.uiState.value.step)
        }

        @Test
        fun `onTooltipDismissed transitions to COMPLETED`() {
            vm.onTooltipDismissed()
            assertEquals(WalkthroughStep.COMPLETED, vm.uiState.value.step)
        }
    }
}

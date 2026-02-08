package io.github.jn0v.traceglass.feature.tracing

import io.github.jn0v.traceglass.feature.tracing.FakeFlashlightController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class TracingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeFlashlight: FakeFlashlightController

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeFlashlight = FakeFlashlightController()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        flashlightController: FakeFlashlightController = fakeFlashlight
    ) = TracingViewModel(flashlightController)

    @Nested
    inner class Permission {
        @Test
        fun `initial state has permission not requested`() {
            val viewModel = createViewModel()
            assertEquals(PermissionState.NOT_REQUESTED, viewModel.uiState.value.permissionState)
        }

        @Test
        fun `when permission granted then state is GRANTED`() {
            val viewModel = createViewModel()
            viewModel.onPermissionResult(granted = true)
            assertEquals(PermissionState.GRANTED, viewModel.uiState.value.permissionState)
        }

        @Test
        fun `when permission denied then state is DENIED`() {
            val viewModel = createViewModel()
            viewModel.onPermissionResult(granted = false)
            assertEquals(PermissionState.DENIED, viewModel.uiState.value.permissionState)
        }

        @Test
        fun `when permission denied then granted on retry then state is GRANTED`() {
            val viewModel = createViewModel()
            viewModel.onPermissionResult(granted = false)
            viewModel.onPermissionResult(granted = true)
            assertEquals(PermissionState.GRANTED, viewModel.uiState.value.permissionState)
        }
    }

    @Nested
    inner class Flashlight {
        @Test
        fun `initial torch state is off`() {
            val viewModel = createViewModel()
            assertFalse(viewModel.uiState.value.isTorchOn)
        }

        @Test
        fun `toggle torch turns it on`() = runTest {
            val viewModel = createViewModel()
            viewModel.onToggleTorch()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isTorchOn)
        }

        @Test
        fun `toggle torch twice turns it off`() = runTest {
            val viewModel = createViewModel()
            viewModel.onToggleTorch()
            viewModel.onToggleTorch()
            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isTorchOn)
        }

        @Test
        fun `hasFlashlight reflects controller capability`() {
            val viewModel = createViewModel()
            assertTrue(viewModel.uiState.value.hasFlashlight)
        }

        @Test
        fun `device without flashlight hides the button`() {
            val noFlash = FakeFlashlightController(hasFlashlight = false)
            val viewModel = createViewModel(flashlightController = noFlash)
            assertFalse(viewModel.uiState.value.hasFlashlight)
        }
    }
}

package io.github.jn0v.traceglass.feature.tracing

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import io.github.jn0v.traceglass.feature.tracing.FakeFlashlightController
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
    inner class ImageImport {
        @Test
        fun `initial state has no overlay image`() {
            val viewModel = createViewModel()
            assertNull(viewModel.uiState.value.overlayImageUri)
        }

        @Test
        fun `default overlay opacity is 50 percent`() {
            val viewModel = createViewModel()
            assertEquals(0.5f, viewModel.uiState.value.overlayOpacity)
        }

        @Test
        fun `when image selected then overlayImageUri is set`() {
            val viewModel = createViewModel()
            val uri = mockk<Uri>()
            viewModel.onImageSelected(uri)
            assertEquals(uri, viewModel.uiState.value.overlayImageUri)
        }

        @Test
        fun `when image selection cancelled then state unchanged`() {
            val viewModel = createViewModel()
            viewModel.onImageSelected(null)
            assertNull(viewModel.uiState.value.overlayImageUri)
        }

        @Test
        fun `when new image selected it replaces the previous one`() {
            val viewModel = createViewModel()
            val uri1 = mockk<Uri>(name = "image1")
            val uri2 = mockk<Uri>(name = "image2")
            viewModel.onImageSelected(uri1)
            viewModel.onImageSelected(uri2)
            assertEquals(uri2, viewModel.uiState.value.overlayImageUri)
        }
    }

    @Nested
    inner class OverlayPositioning {
        @Test
        fun `initial overlay offset is zero`() {
            val viewModel = createViewModel()
            assertEquals(Offset.Zero, viewModel.uiState.value.overlayOffset)
        }

        @Test
        fun `initial overlay scale is 1`() {
            val viewModel = createViewModel()
            assertEquals(1f, viewModel.uiState.value.overlayScale)
        }

        @Test
        fun `onOverlayDrag adds delta to offset`() {
            val viewModel = createViewModel()
            viewModel.onOverlayDrag(Offset(100f, 50f))
            assertEquals(Offset(100f, 50f), viewModel.uiState.value.overlayOffset)
        }

        @Test
        fun `onOverlayDrag accumulates multiple drags`() {
            val viewModel = createViewModel()
            viewModel.onOverlayDrag(Offset(100f, 50f))
            viewModel.onOverlayDrag(Offset(-30f, 20f))
            assertEquals(Offset(70f, 70f), viewModel.uiState.value.overlayOffset)
        }

        @Test
        fun `onOverlayScale multiplies scale factor`() {
            val viewModel = createViewModel()
            viewModel.onOverlayScale(2f)
            assertEquals(2f, viewModel.uiState.value.overlayScale)
        }

        @Test
        fun `onOverlayScale accumulates`() {
            val viewModel = createViewModel()
            viewModel.onOverlayScale(2f)
            viewModel.onOverlayScale(0.5f)
            assertEquals(1f, viewModel.uiState.value.overlayScale, 0.001f)
        }
    }

    @Nested
    inner class VisualModes {
        @Test
        fun `initial color tint is NONE`() {
            val viewModel = createViewModel()
            assertEquals(ColorTint.NONE, viewModel.uiState.value.colorTint)
        }

        @Test
        fun `onColorTintChanged updates tint`() {
            val viewModel = createViewModel()
            viewModel.onColorTintChanged(ColorTint.RED)
            assertEquals(ColorTint.RED, viewModel.uiState.value.colorTint)
        }

        @Test
        fun `initial inverted mode is off`() {
            val viewModel = createViewModel()
            assertFalse(viewModel.uiState.value.isInvertedMode)
        }

        @Test
        fun `onToggleInvertedMode flips state`() {
            val viewModel = createViewModel()
            viewModel.onToggleInvertedMode()
            assertTrue(viewModel.uiState.value.isInvertedMode)
        }

        @Test
        fun `onToggleInvertedMode twice returns to original`() {
            val viewModel = createViewModel()
            viewModel.onToggleInvertedMode()
            viewModel.onToggleInvertedMode()
            assertFalse(viewModel.uiState.value.isInvertedMode)
        }
    }

    @Nested
    inner class Opacity {
        @Test
        fun `onOpacityChanged updates overlay opacity`() {
            val viewModel = createViewModel()
            viewModel.onOpacityChanged(0.75f)
            assertEquals(0.75f, viewModel.uiState.value.overlayOpacity)
        }

        @Test
        fun `opacity is clamped between 0 and 1`() {
            val viewModel = createViewModel()
            viewModel.onOpacityChanged(-0.1f)
            assertEquals(0f, viewModel.uiState.value.overlayOpacity)
            viewModel.onOpacityChanged(1.5f)
            assertEquals(1f, viewModel.uiState.value.overlayOpacity)
        }

        @Test
        fun `onToggleOpacitySlider opens slider`() {
            val viewModel = createViewModel()
            assertFalse(viewModel.uiState.value.isOpacitySliderVisible)
            viewModel.onToggleOpacitySlider()
            assertTrue(viewModel.uiState.value.isOpacitySliderVisible)
        }

        @Test
        fun `onToggleOpacitySlider twice closes slider`() {
            val viewModel = createViewModel()
            viewModel.onToggleOpacitySlider()
            viewModel.onToggleOpacitySlider()
            assertFalse(viewModel.uiState.value.isOpacitySliderVisible)
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

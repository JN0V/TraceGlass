package io.github.jn0v.traceglass.feature.tracing

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import io.github.jn0v.traceglass.core.camera.FlashlightController
import io.github.jn0v.traceglass.core.cv.DetectedMarker
import io.github.jn0v.traceglass.core.cv.MarkerResult
import io.github.jn0v.traceglass.core.overlay.OverlayTransformCalculator
import io.github.jn0v.traceglass.core.overlay.TrackingStateManager
import io.github.jn0v.traceglass.core.session.SessionData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.github.jn0v.traceglass.feature.tracing.settings.FakeSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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
        flashlightController: FakeFlashlightController = fakeFlashlight,
        trackingStateManager: TrackingStateManager? = null,
        sessionRepository: FakeSessionRepository = FakeSessionRepository(),
        transformCalculator: OverlayTransformCalculator? = null
    ) = TracingViewModel(
        flashlightController = flashlightController,
        trackingStateManager = trackingStateManager ?: TrackingStateManager(),
        sessionRepository = sessionRepository,
        transformCalculator = transformCalculator ?: OverlayTransformCalculator(smoothingFactor = 1f)
    )

    private fun markerWithCorners(
        id: Int, cx: Float, cy: Float,
        size: Float = 50f, angleDeg: Float = 0f
    ): DetectedMarker {
        val halfSize = size / 2f
        val rad = Math.toRadians(angleDeg.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val corners = listOf(
            Pair(cx - halfSize * cosA + halfSize * sinA, cy - halfSize * sinA - halfSize * cosA),
            Pair(cx + halfSize * cosA + halfSize * sinA, cy + halfSize * sinA - halfSize * cosA),
            Pair(cx + halfSize * cosA - halfSize * sinA, cy + halfSize * sinA + halfSize * cosA),
            Pair(cx - halfSize * cosA - halfSize * sinA, cy - halfSize * sinA + halfSize * cosA)
        )
        return DetectedMarker(id, cx, cy, corners, 1f)
    }

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
    inner class SessionControls {
        @Test
        fun `initial session is not active`() {
            val viewModel = createViewModel()
            assertFalse(viewModel.uiState.value.isSessionActive)
        }

        @Test
        fun `onToggleSession starts session`() {
            val viewModel = createViewModel()
            viewModel.onToggleSession()
            assertTrue(viewModel.uiState.value.isSessionActive)
        }

        @Test
        fun `onToggleSession twice stops session`() {
            val viewModel = createViewModel()
            viewModel.onToggleSession()
            viewModel.onToggleSession()
            assertFalse(viewModel.uiState.value.isSessionActive)
        }

        @Test
        fun `isSessionActive drives wake lock state`() {
            // Wake lock (FLAG_KEEP_SCREEN_ON) is wired via DisposableEffect in Compose.
            // This test verifies the ViewModel state that drives it.
            val viewModel = createViewModel()
            assertFalse(viewModel.uiState.value.isSessionActive, "Wake lock off initially")

            viewModel.onToggleSession()
            assertTrue(viewModel.uiState.value.isSessionActive, "Wake lock on when session active")

            viewModel.onToggleSession()
            assertFalse(viewModel.uiState.value.isSessionActive, "Wake lock off when session stopped")
        }

        @Test
        fun `controls are visible by default`() {
            val viewModel = createViewModel()
            assertTrue(viewModel.uiState.value.areControlsVisible)
        }

        @Test
        fun `onToggleControlsVisibility hides controls`() {
            val viewModel = createViewModel()
            viewModel.onToggleControlsVisibility()
            assertFalse(viewModel.uiState.value.areControlsVisible)
        }

        @Test
        fun `onToggleControlsVisibility twice shows controls again`() {
            val viewModel = createViewModel()
            viewModel.onToggleControlsVisibility()
            viewModel.onToggleControlsVisibility()
            assertTrue(viewModel.uiState.value.areControlsVisible)
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
        fun `onOverlayDrag adds delta to manual offset`() {
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

        @Test
        fun `onOverlayRotate adds rotation delta`() {
            val viewModel = createViewModel()
            viewModel.onOverlayRotate(15f)
            assertEquals(15f, viewModel.uiState.value.overlayRotation, 0.001f)
        }

        @Test
        fun `onOverlayRotate accumulates`() {
            val viewModel = createViewModel()
            viewModel.onOverlayRotate(15f)
            viewModel.onOverlayRotate(-5f)
            assertEquals(10f, viewModel.uiState.value.overlayRotation, 0.001f)
        }

        @Test
        fun `onOverlayGesture applies drag scale and rotate simultaneously`() {
            val viewModel = createViewModel()
            // Centroid at view center — no offset correction needed for scale
            val centroid = Offset(540f, 960f) // default view center (1080x1920)
            viewModel.onOverlayGesture(centroid, Offset(30f, 20f), 1.5f, 10f)
            assertEquals(30f, viewModel.uiState.value.overlayOffset.x, 0.1f)
            assertEquals(20f, viewModel.uiState.value.overlayOffset.y, 0.1f)
            assertEquals(1.5f, viewModel.uiState.value.overlayScale, 0.001f)
            assertEquals(10f, viewModel.uiState.value.overlayRotation, 0.001f)
        }

        @Test
        fun `onOverlayGesture with off-center centroid adjusts offset for scale pivot`() {
            val viewModel = createViewModel()
            // Centroid at (340, 460) — offset from view center (540, 960) by (-200, -500)
            val centroid = Offset(340f, 460f)
            val zoom = 2f
            // Offset correction: (1 - 2) * (340-540, 460-960) = (-1)*(-200,-500) = (200, 500)
            viewModel.onOverlayGesture(centroid, Offset.Zero, zoom, 0f)
            assertEquals(200f, viewModel.uiState.value.overlayOffset.x, 0.1f)
            assertEquals(500f, viewModel.uiState.value.overlayOffset.y, 0.1f)
            assertEquals(2f, viewModel.uiState.value.overlayScale, 0.001f)
        }

        @Test
        fun `onOverlayGesture accumulates across multiple calls`() {
            val viewModel = createViewModel()
            val center = Offset(540f, 960f)
            viewModel.onOverlayGesture(center, Offset(10f, 5f), 2f, 15f)
            viewModel.onOverlayGesture(center, Offset(20f, 10f), 0.5f, -5f)
            // Offset: 10+20=30, 5+10=15
            assertEquals(30f, viewModel.uiState.value.overlayOffset.x, 0.1f)
            assertEquals(15f, viewModel.uiState.value.overlayOffset.y, 0.1f)
            // Scale: 2 * 0.5 = 1
            assertEquals(1f, viewModel.uiState.value.overlayScale, 0.001f)
            // Rotation: 15 + (-5) = 10
            assertEquals(10f, viewModel.uiState.value.overlayRotation, 0.001f)
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

        @Test
        fun `effectiveOpacity equals overlayOpacity when inverted mode off`() {
            val state = TracingUiState(overlayOpacity = 0.7f, isInvertedMode = false)
            assertEquals(0.7f, state.effectiveOpacity, 0.001f)
        }

        @Test
        fun `effectiveOpacity is inverted when inverted mode on`() {
            val state = TracingUiState(overlayOpacity = 0.7f, isInvertedMode = true)
            assertEquals(0.3f, state.effectiveOpacity, 0.001f)
        }

        @Test
        fun `effectiveOpacity at 0 inverts to 1`() {
            val state = TracingUiState(overlayOpacity = 0f, isInvertedMode = true)
            assertEquals(1f, state.effectiveOpacity, 0.001f)
        }

        @Test
        fun `effectiveOpacity at 1 inverts to 0`() {
            val state = TracingUiState(overlayOpacity = 1f, isInvertedMode = true)
            assertEquals(0f, state.effectiveOpacity, 0.001f)
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
    inner class MarkerTracking {
        @Test
        fun `initial tracking state is INACTIVE`() {
            val viewModel = createViewModel()
            assertEquals(TrackingState.INACTIVE, viewModel.uiState.value.trackingState)
        }

        @Test
        fun `onMarkerResultReceived with markers sets TRACKING`() {
            val viewModel = createViewModel()
            val result = MarkerResult(
                listOf(markerWithCorners(0, 100f, 200f)), 5L, 1080, 1920
            )
            viewModel.onMarkerResultReceived(result)
            assertEquals(TrackingState.TRACKING, viewModel.uiState.value.trackingState)
        }

        @Test
        fun `onMarkerResultReceived with no markers after timeout sets LOST`() {
            var time = 0L
            val manager = TrackingStateManager(lostTimeoutMs = 500L, timeProvider = { time })
            val viewModel = createViewModel(trackingStateManager = manager)
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 100f, 200f)), 5L, 1080, 1920)
            )
            time = 600L
            viewModel.onMarkerResultReceived(MarkerResult(emptyList(), 5L))
            assertEquals(TrackingState.LOST, viewModel.uiState.value.trackingState)
        }

        @Test
        fun `brief marker loss stays TRACKING within grace period`() {
            var time = 0L
            val manager = TrackingStateManager(lostTimeoutMs = 500L, timeProvider = { time })
            val viewModel = createViewModel(trackingStateManager = manager)
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 100f, 200f)), 5L, 1080, 1920)
            )
            time = 200L
            viewModel.onMarkerResultReceived(MarkerResult(emptyList(), 5L))
            assertEquals(TrackingState.TRACKING, viewModel.uiState.value.trackingState)
        }

        @Test
        fun `tracking updates overlay offset from marker center`() {
            // Use smoothing=1 for instant tracking
            val viewModel = createViewModel()
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 540f, 960f)), 5L, 1080, 1920)
            )
            assertEquals(0f, viewModel.uiState.value.overlayOffset.x, 1f)
            assertEquals(0f, viewModel.uiState.value.overlayOffset.y, 1f)
        }

        @Test
        fun `tracking with off-center marker shifts overlay`() {
            val viewModel = createViewModel()
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 800f, 960f)), 5L, 1080, 1920)
            )
            assertTrue(viewModel.uiState.value.overlayOffset.x > 0f)
        }

        @Test
        fun `tracking with two markers computes rotation`() {
            val viewModel = createViewModel()
            // First result: horizontal markers (sets reference)
            val m1 = markerWithCorners(0, 100f, 500f)
            val m2 = markerWithCorners(1, 500f, 500f)
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(m1, m2), 5L, 1080, 1920)
            )
            assertEquals(0f, viewModel.uiState.value.overlayRotation, 0.1f)
        }

        @Test
        fun `lost tracking holds last known transform`() {
            var time = 0L
            val manager = TrackingStateManager(lostTimeoutMs = 500L, timeProvider = { time })
            val viewModel = createViewModel(trackingStateManager = manager)

            // Track a marker at off-center position
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 300f, 700f)), 5L, 1080, 1920)
            )
            val trackedOffset = viewModel.uiState.value.overlayOffset

            // Lose markers after timeout
            time = 600L
            viewModel.onMarkerResultReceived(MarkerResult(emptyList(), 5L))

            // Should hold last offset, not reset to zero
            assertEquals(trackedOffset.x, viewModel.uiState.value.overlayOffset.x, 1f)
            assertEquals(trackedOffset.y, viewModel.uiState.value.overlayOffset.y, 1f)
        }
    }

    @Nested
    inner class ManualAndMarkerInteraction {
        @Test
        fun `manual drag preserved after marker update`() {
            val viewModel = createViewModel()

            // Manual drag first
            viewModel.onOverlayDrag(Offset(50f, 30f))

            // Then marker detection
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 540f, 960f)), 5L, 1080, 1920)
            )

            // Manual offset should be additive (marker at center = 0 offset, plus manual 50,30)
            assertEquals(50f, viewModel.uiState.value.overlayOffset.x, 2f)
            assertEquals(30f, viewModel.uiState.value.overlayOffset.y, 2f)
        }

        @Test
        fun `manual scale preserved after marker update`() {
            val viewModel = createViewModel()

            viewModel.onOverlayScale(2f)

            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 540f, 960f)), 5L, 1080, 1920)
            )

            // marker scale=1 (first detection), manual=2 → combined=2
            assertEquals(2f, viewModel.uiState.value.overlayScale, 0.1f)
        }

        @Test
        fun `manual rotation preserved after marker update`() {
            val viewModel = createViewModel()

            viewModel.onOverlayRotate(25f)

            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 540f, 960f)), 5L, 1080, 1920)
            )

            // marker rotation=0 (first detection sets reference), manual=25 → combined=25
            assertEquals(25f, viewModel.uiState.value.overlayRotation, 1f)
        }

        @Test
        fun `previewScale scales marker offset to screen coordinates`() {
            val viewModel = createViewModel()

            // View is 2x the frame size → previewScale = 2.0
            viewModel.setViewDimensions(2160f, 3840f)

            // Marker 100px right of center in frame coords
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 640f, 960f)), 5L, 1080, 1920)
            )

            // offset in frame = 640-540 = 100px, scaled by 2 = 200px
            assertEquals(200f, viewModel.uiState.value.overlayOffset.x, 2f)
        }

        @Test
        fun `marker update does not reset manual offset`() {
            val viewModel = createViewModel()

            viewModel.onOverlayDrag(Offset(100f, 0f))

            // Multiple marker updates
            repeat(5) {
                viewModel.onMarkerResultReceived(
                    MarkerResult(listOf(markerWithCorners(0, 540f, 960f)), 5L, 1080, 1920)
                )
            }

            // Manual offset 100 should still be there
            assertEquals(100f, viewModel.uiState.value.overlayOffset.x, 2f)
        }
    }

    @Nested
    inner class LandscapeOrientation {
        @Test
        fun `landscape view dimensions produce correct preview scale`() {
            val viewModel = createViewModel()
            // Landscape view showing a landscape camera frame
            viewModel.setViewDimensions(1920f, 1080f)
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 960f, 540f)), 5L, 1920, 1080)
            )
            // Marker at center of landscape frame → offset ~0
            assertEquals(0f, viewModel.uiState.value.overlayOffset.x, 2f)
            assertEquals(0f, viewModel.uiState.value.overlayOffset.y, 2f)
        }

        @Test
        fun `landscape off-center marker scales correctly`() {
            val viewModel = createViewModel()
            viewModel.setViewDimensions(1920f, 1080f)
            // Marker 100px right of center in landscape frame
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 1060f, 540f)), 5L, 1920, 1080)
            )
            // pvScale=1.0 for matching dimensions, offset = 100px
            assertEquals(100f, viewModel.uiState.value.overlayOffset.x, 2f)
            assertEquals(0f, viewModel.uiState.value.overlayOffset.y, 2f)
        }

        @Test
        fun `switching from portrait to landscape recomputes correctly`() {
            val viewModel = createViewModel()

            // Start in portrait
            viewModel.setViewDimensions(1080f, 1920f)
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 640f, 960f)), 5L, 1080, 1920)
            )
            val portraitOffset = viewModel.uiState.value.overlayOffset

            // Switch to landscape (new frame dimensions too)
            viewModel.setViewDimensions(1920f, 1080f)
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 1060f, 540f)), 5L, 1920, 1080)
            )
            val landscapeOffset = viewModel.uiState.value.overlayOffset

            // Both should show ~100px right of center, but in their respective coordinate spaces
            assertEquals(100f, portraitOffset.x, 2f)
            assertEquals(100f, landscapeOffset.x, 2f)
        }
    }

    @Nested
    inner class SmoothedTracking {
        @Test
        fun `smoothed tracking converges without large jumps`() {
            // Use a realistic smoothing factor
            val calc = OverlayTransformCalculator(smoothingFactor = 0.3f)
            val viewModel = createViewModel(transformCalculator = calc)

            // First frame at center
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 540f, 960f)), 5L, 1080, 1920)
            )
            val first = viewModel.uiState.value.overlayOffset

            // Suddenly jump marker far away
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerWithCorners(0, 900f, 960f)), 5L, 1080, 1920)
            )
            val second = viewModel.uiState.value.overlayOffset

            // The full jump would be 360px (900-540). With smoothing 0.3, first step should be ~108px
            val targetDelta = 360f
            val actualDelta = second.x - first.x
            assertTrue(actualDelta > 0, "Should move in the right direction")
            assertTrue(actualDelta < targetDelta * 0.5f,
                "Smoothing should dampen the jump: got $actualDelta expected less than ${targetDelta * 0.5f}")
        }

        @Test
        fun `marker recovery after loss is smoothed`() {
            var time = 0L
            val manager = TrackingStateManager(lostTimeoutMs = 500L, timeProvider = { time })
            val calc = OverlayTransformCalculator(smoothingFactor = 0.3f)
            val viewModel = createViewModel(trackingStateManager = manager, transformCalculator = calc)

            // Track at position A
            val markerA = markerWithCorners(0, 300f, 700f)
            repeat(20) {
                viewModel.onMarkerResultReceived(
                    MarkerResult(listOf(markerA), 5L, 1080, 1920)
                )
            }
            val posA = viewModel.uiState.value.overlayOffset

            // Lose markers
            time = 600L
            viewModel.onMarkerResultReceived(MarkerResult(emptyList(), 5L))
            val held = viewModel.uiState.value.overlayOffset
            assertEquals(posA.x, held.x, 2f)

            // Recover at position B (different position)
            time = 700L
            val markerB = markerWithCorners(0, 700f, 700f)
            viewModel.onMarkerResultReceived(
                MarkerResult(listOf(markerB), 5L, 1080, 1920)
            )
            val afterRecovery = viewModel.uiState.value.overlayOffset

            // Should NOT have snapped fully to B; should be smoothed between A and B
            val fullJump = abs(700f - 300f) // 400px difference
            val actualJump = abs(afterRecovery.x - held.x)
            assertTrue(actualJump < fullJump * 0.5f,
                "Recovery should be smoothed: jumped $actualJump of $fullJump")
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

    @Nested
    inner class BreakReminder {
        private fun createViewModelWithSettings(
            settingsRepo: FakeSettingsRepository = FakeSettingsRepository()
        ) = TracingViewModel(
            flashlightController = fakeFlashlight,
            settingsRepository = settingsRepo
        )

        @Test
        fun `break reminder not shown by default`() {
            val viewModel = createViewModelWithSettings()
            assertFalse(viewModel.uiState.value.showBreakReminder)
        }

        @Test
        fun `timer fires after interval when session active and reminders enabled`() = runTest(testDispatcher) {
            val settings = FakeSettingsRepository()
            settings.setBreakReminderEnabled(true)
            settings.setBreakReminderIntervalMinutes(30)
            val viewModel = createViewModelWithSettings(settings)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onToggleSession() // start session
            testDispatcher.scheduler.advanceTimeBy(30 * 60_000L + 1)

            assertTrue(viewModel.uiState.value.showBreakReminder)
        }

        @Test
        fun `timer does NOT fire when reminders disabled`() = runTest(testDispatcher) {
            val settings = FakeSettingsRepository()
            // reminders disabled by default
            val viewModel = createViewModelWithSettings(settings)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onToggleSession()
            testDispatcher.scheduler.advanceTimeBy(60 * 60_000L)

            assertFalse(viewModel.uiState.value.showBreakReminder)
        }

        @Test
        fun `timer does NOT fire when session inactive`() = runTest(testDispatcher) {
            val settings = FakeSettingsRepository()
            settings.setBreakReminderEnabled(true)
            val viewModel = createViewModelWithSettings(settings)
            testDispatcher.scheduler.advanceUntilIdle()

            // session NOT started
            testDispatcher.scheduler.advanceTimeBy(60 * 60_000L)

            assertFalse(viewModel.uiState.value.showBreakReminder)
        }

        @Test
        fun `onBreakReminderDismissed resets flag and restarts timer`() = runTest(testDispatcher) {
            val settings = FakeSettingsRepository()
            settings.setBreakReminderEnabled(true)
            settings.setBreakReminderIntervalMinutes(10)
            val viewModel = createViewModelWithSettings(settings)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onToggleSession()
            testDispatcher.scheduler.advanceTimeBy(10 * 60_000L + 1)
            assertTrue(viewModel.uiState.value.showBreakReminder)

            viewModel.onBreakReminderDismissed()
            assertFalse(viewModel.uiState.value.showBreakReminder)

            // timer should restart, fire again after another interval
            testDispatcher.scheduler.advanceTimeBy(10 * 60_000L + 1)
            assertTrue(viewModel.uiState.value.showBreakReminder)
        }

        @Test
        fun `timer resets when interval changes`() = runTest(testDispatcher) {
            val settings = FakeSettingsRepository()
            settings.setBreakReminderEnabled(true)
            settings.setBreakReminderIntervalMinutes(30)
            val viewModel = createViewModelWithSettings(settings)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onToggleSession()
            testDispatcher.scheduler.advanceTimeBy(20 * 60_000L) // 20min elapsed

            // change interval to 10min — timer resets
            settings.setBreakReminderIntervalMinutes(10)
            testDispatcher.scheduler.runCurrent() // process Flow emission without advancing time

            // should NOT have fired yet (timer just reset with new 10min interval)
            assertFalse(viewModel.uiState.value.showBreakReminder)

            // advance 10min for new interval
            testDispatcher.scheduler.advanceTimeBy(10 * 60_000L + 1)
            assertTrue(viewModel.uiState.value.showBreakReminder)
        }

        @Test
        fun `stopping session cancels timer`() = runTest(testDispatcher) {
            val settings = FakeSettingsRepository()
            settings.setBreakReminderEnabled(true)
            settings.setBreakReminderIntervalMinutes(30)
            val viewModel = createViewModelWithSettings(settings)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onToggleSession() // start
            testDispatcher.scheduler.advanceTimeBy(20 * 60_000L)

            viewModel.onToggleSession() // stop
            testDispatcher.scheduler.advanceTimeBy(30 * 60_000L)

            assertFalse(viewModel.uiState.value.showBreakReminder)
        }
    }

    @Nested
    inner class OverlayLock {
        @Test
        fun `initial lock state is false`() {
            val viewModel = createViewModel()
            assertFalse(viewModel.uiState.value.isOverlayLocked)
        }

        @Test
        fun `initial viewport zoom is 1`() {
            val viewModel = createViewModel()
            assertEquals(1f, viewModel.uiState.value.viewportZoom)
        }

        @Test
        fun `initial viewport pan is zero`() {
            val viewModel = createViewModel()
            assertEquals(0f, viewModel.uiState.value.viewportPanX)
            assertEquals(0f, viewModel.uiState.value.viewportPanY)
        }

        @Test
        fun `initial unlock confirm dialog is not shown`() {
            val viewModel = createViewModel()
            assertFalse(viewModel.uiState.value.showUnlockConfirmDialog)
        }

        @Test
        fun `onToggleLock sets isOverlayLocked to true`() {
            val viewModel = createViewModel()
            viewModel.onToggleLock()
            assertTrue(viewModel.uiState.value.isOverlayLocked)
        }

        @Test
        fun `lock freezes overlay — drag after lock does NOT change overlay offset`() {
            val viewModel = createViewModel()
            viewModel.onOverlayDrag(Offset(50f, 30f))
            val offsetBefore = viewModel.uiState.value.overlayOffset

            viewModel.onToggleLock()
            viewModel.onOverlayDrag(Offset(100f, 100f))

            assertEquals(offsetBefore.x, viewModel.uiState.value.overlayOffset.x, 0.001f)
            assertEquals(offsetBefore.y, viewModel.uiState.value.overlayOffset.y, 0.001f)
        }

        @Test
        fun `lock freezes overlay — scale after lock does NOT change overlay scale`() {
            val viewModel = createViewModel()
            viewModel.onOverlayScale(2f)
            val scaleBefore = viewModel.uiState.value.overlayScale

            viewModel.onToggleLock()
            viewModel.onOverlayScale(3f)

            assertEquals(scaleBefore, viewModel.uiState.value.overlayScale, 0.001f)
        }

        @Test
        fun `lock freezes overlay — rotate after lock does NOT change overlay rotation`() {
            val viewModel = createViewModel()
            viewModel.onOverlayRotate(15f)
            val rotationBefore = viewModel.uiState.value.overlayRotation

            viewModel.onToggleLock()
            viewModel.onOverlayRotate(45f)

            assertEquals(rotationBefore, viewModel.uiState.value.overlayRotation, 0.001f)
        }

        @Test
        fun `lock freezes overlay — gesture after lock does NOT change overlay state`() {
            val viewModel = createViewModel()
            val center = Offset(540f, 960f)
            viewModel.onOverlayGesture(center, Offset(50f, 30f), 1.5f, 10f)
            val stateBefore = viewModel.uiState.value

            viewModel.onToggleLock()
            viewModel.onOverlayGesture(center, Offset(100f, 100f), 2f, 20f)

            assertEquals(stateBefore.overlayOffset, viewModel.uiState.value.overlayOffset)
            assertEquals(stateBefore.overlayScale, viewModel.uiState.value.overlayScale, 0.001f)
            assertEquals(stateBefore.overlayRotation, viewModel.uiState.value.overlayRotation, 0.001f)
        }

        @Test
        fun `viewport zoom only changes when locked`() {
            val viewModel = createViewModel()
            viewModel.setViewDimensions(1080f, 1920f)
            // Before lock — viewport zoom should stay at 1
            viewModel.onViewportZoom(2f)
            assertEquals(1f, viewModel.uiState.value.viewportZoom)

            // After lock — viewport zoom should change
            viewModel.onToggleLock()
            viewModel.onViewportZoom(2f)
            assertEquals(2f, viewModel.uiState.value.viewportZoom)
        }

        @Test
        fun `viewport pan only changes when locked`() {
            val viewModel = createViewModel()
            viewModel.setViewDimensions(1080f, 1920f)
            // Before lock — viewport pan should stay at 0
            viewModel.onViewportPan(Offset(50f, 30f))
            assertEquals(0f, viewModel.uiState.value.viewportPanX)
            assertEquals(0f, viewModel.uiState.value.viewportPanY)

            // After lock — viewport pan should change
            viewModel.onToggleLock()
            viewModel.onViewportZoom(2f) // must zoom first to have pan room
            viewModel.onViewportPan(Offset(50f, 30f))
            assertTrue(viewModel.uiState.value.viewportPanX != 0f || viewModel.uiState.value.viewportPanY != 0f)
        }

        @Test
        fun `viewport zoom clamped between 1 and 5`() {
            val viewModel = createViewModel()
            viewModel.setViewDimensions(1080f, 1920f)
            viewModel.onToggleLock()

            viewModel.onViewportZoom(10f)
            assertEquals(5f, viewModel.uiState.value.viewportZoom)

            viewModel.onViewportZoom(0.01f) // 5 * 0.01 = 0.05 → clamped to 1
            assertEquals(1f, viewModel.uiState.value.viewportZoom)
        }

        @Test
        fun `unlock resets viewport to 1x zoom and zero pan`() {
            val viewModel = createViewModel()
            viewModel.setViewDimensions(1080f, 1920f)
            viewModel.onToggleLock()
            viewModel.onViewportZoom(3f)
            viewModel.onViewportPan(Offset(50f, 30f))

            viewModel.onRequestUnlock()
            assertTrue(viewModel.uiState.value.showUnlockConfirmDialog)

            viewModel.onConfirmUnlock()
            assertFalse(viewModel.uiState.value.isOverlayLocked)
            assertEquals(1f, viewModel.uiState.value.viewportZoom)
            assertEquals(0f, viewModel.uiState.value.viewportPanX)
            assertEquals(0f, viewModel.uiState.value.viewportPanY)
            assertFalse(viewModel.uiState.value.showUnlockConfirmDialog)
        }

        @Test
        fun `unlock requires confirmation dialog flow`() {
            val viewModel = createViewModel()
            viewModel.onToggleLock()
            assertTrue(viewModel.uiState.value.isOverlayLocked)

            viewModel.onRequestUnlock()
            assertTrue(viewModel.uiState.value.showUnlockConfirmDialog)
            assertTrue(viewModel.uiState.value.isOverlayLocked) // still locked

            viewModel.onDismissUnlockDialog()
            assertFalse(viewModel.uiState.value.showUnlockConfirmDialog)
            assertTrue(viewModel.uiState.value.isOverlayLocked) // still locked after dismiss
        }

        @Test
        fun `opacity works regardless of lock state`() {
            val viewModel = createViewModel()
            viewModel.onToggleLock()
            viewModel.onOpacityChanged(0.8f)
            assertEquals(0.8f, viewModel.uiState.value.overlayOpacity)
        }

        @Test
        fun `color tint works regardless of lock state`() {
            val viewModel = createViewModel()
            viewModel.onToggleLock()
            viewModel.onColorTintChanged(ColorTint.RED)
            assertEquals(ColorTint.RED, viewModel.uiState.value.colorTint)
        }

        @Test
        fun `inverted mode works regardless of lock state`() {
            val viewModel = createViewModel()
            viewModel.onToggleLock()
            viewModel.onToggleInvertedMode()
            assertTrue(viewModel.uiState.value.isInvertedMode)
        }

        @Test
        fun `onToggleLock sets showLockSnackbar to true`() {
            val viewModel = createViewModel()
            assertFalse(viewModel.uiState.value.showLockSnackbar)
            viewModel.onToggleLock()
            assertTrue(viewModel.uiState.value.showLockSnackbar)
        }

        @Test
        fun `onLockSnackbarShown resets flag`() {
            val viewModel = createViewModel()
            viewModel.onToggleLock()
            assertTrue(viewModel.uiState.value.showLockSnackbar)
            viewModel.onLockSnackbarShown()
            assertFalse(viewModel.uiState.value.showLockSnackbar)
        }

        @Test
        fun `session restore does NOT set showLockSnackbar`() = runTest {
            val fakeUri = mockk<Uri>()
            mockkStatic(Uri::class)
            every { Uri.parse(any()) } returns fakeUri
            every { fakeUri.toString() } returns "file:///data/ref"

            val repo = FakeSessionRepository()
            repo.save(SessionData(imageUri = "file:///data/ref", isOverlayLocked = true))

            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.restoreSession()
            viewModel.onResumeSessionAccepted()

            assertTrue(viewModel.uiState.value.isOverlayLocked)
            assertFalse(viewModel.uiState.value.showLockSnackbar)

            unmockkStatic(Uri::class)
        }

        @Test
        fun `zoom out re-clamps pan to new bounds`() {
            val viewModel = createViewModel()
            viewModel.setViewDimensions(1080f, 1920f)
            viewModel.onToggleLock()

            // Zoom to 5x and pan to max
            viewModel.onViewportZoom(5f)
            viewModel.onViewportPan(Offset(99999f, 99999f))
            val maxPanAt5x = (5f - 1f) * 1080f / 2f // 2160
            assertEquals(maxPanAt5x, viewModel.uiState.value.viewportPanX, 1f)

            // Zoom out to 2x — pan should be re-clamped
            viewModel.onViewportZoom(2f / 5f) // 5 * 0.4 = 2
            val maxPanAt2x = (2f - 1f) * 1080f / 2f // 540
            assertTrue(viewModel.uiState.value.viewportPanX <= maxPanAt2x + 1f,
                "Pan should be clamped to $maxPanAt2x, got ${viewModel.uiState.value.viewportPanX}")
        }
    }

    @Nested
    inner class SessionPersistence {

        @Test
        fun `saveSession is blocked until restoreSession completes`() = runTest {
            val repo = FakeSessionRepository()
            val viewModel = createViewModel(sessionRepository = repo)
            // saveSession before restoreSession should be no-op
            viewModel.saveSession()
            assertEquals(0, repo.saveCount)

            // After restore completes, save should work
            viewModel.restoreSession()
            viewModel.saveSession()
            assertEquals(1, repo.saveCount)
        }

        @Test
        fun `saveSession persists all fields including rotation`() = runTest {
            val repo = FakeSessionRepository()
            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.restoreSession()
            val uri = mockk<Uri>()
            every { uri.toString() } returns "file:///test"
            viewModel.onImageSelected(uri)
            testDispatcher.scheduler.advanceUntilIdle() // let auto-save from onImageSelected complete
            val countAfterSelect = repo.saveCount

            viewModel.onOpacityChanged(0.8f)
            viewModel.onOverlayDrag(Offset(30f, 40f))
            viewModel.onOverlayScale(1.5f)
            viewModel.onOverlayRotate(45f)
            viewModel.onColorTintChanged(ColorTint.BLUE)
            viewModel.onToggleInvertedMode()
            testDispatcher.scheduler.advanceUntilIdle() // let debounced saves complete

            // Verify saved data has all fields
            val saved = repo.lastSavedData()
            assertEquals("file:///test", saved.imageUri)
            assertEquals(0.8f, saved.overlayOpacity)
            assertTrue(saved.overlayScale > 1f) // 1.0 * 1.5
            assertTrue(saved.overlayRotation != 0f) // 45 degrees
            assertEquals("BLUE", saved.colorTint)
            assertTrue(saved.isInvertedMode)
            assertTrue(saved.overlayOffsetX != 0f || saved.overlayOffsetY != 0f)
        }

        @Test
        fun `full save-kill-restore cycle preserves all state`() = runTest {
            val fakeUri = mockk<Uri>()
            mockkStatic(Uri::class)
            every { Uri.parse(any()) } returns fakeUri
            every { fakeUri.toString() } returns "file:///data/ref"

            // --- Session 1: user sets up overlay ---
            val repo = FakeSessionRepository()
            val vm1 = createViewModel(sessionRepository = repo)
            vm1.restoreSession() // no prior data
            vm1.onImageSelected(fakeUri)
            vm1.onOpacityChanged(0.7f)
            vm1.onOverlayDrag(Offset(100f, 200f))
            vm1.onOverlayScale(2f)
            vm1.onOverlayRotate(30f)
            vm1.onColorTintChanged(ColorTint.RED)
            vm1.onToggleInvertedMode()
            testDispatcher.scheduler.advanceUntilIdle()

            // Force a final save (simulates ON_STOP)
            vm1.saveSession()
            val saved = repo.lastSavedData()

            // --- Process death: new ViewModel reads same repo ---
            val vm2 = createViewModel(sessionRepository = repo)
            vm2.restoreSession()

            // Dialog should appear
            assertTrue(vm2.uiState.value.showResumeSessionDialog)
            // State should still be defaults
            assertEquals(0.5f, vm2.uiState.value.overlayOpacity)
            assertNull(vm2.uiState.value.overlayImageUri)

            // User accepts
            vm2.onResumeSessionAccepted()

            // All state restored
            assertFalse(vm2.uiState.value.showResumeSessionDialog)
            assertEquals(fakeUri, vm2.uiState.value.overlayImageUri)
            assertEquals(saved.overlayOpacity, vm2.uiState.value.overlayOpacity)
            assertEquals(saved.overlayScale, vm2.uiState.value.overlayScale, 0.01f)
            assertEquals(saved.overlayRotation, vm2.uiState.value.overlayRotation, 0.01f)
            assertEquals(ColorTint.RED, vm2.uiState.value.colorTint)
            assertTrue(vm2.uiState.value.isInvertedMode)
            // Offset is transformed by pvScale but with defaults (1f), should match
            assertTrue(abs(vm2.uiState.value.overlayOffset.x - saved.overlayOffsetX) < 1f)
            assertTrue(abs(vm2.uiState.value.overlayOffset.y - saved.overlayOffsetY) < 1f)

            unmockkStatic(Uri::class)
        }

        @Test
        fun `decline restore clears saved session`() = runTest {
            val repo = FakeSessionRepository()
            repo.save(SessionData(imageUri = "file:///img", overlayOpacity = 0.8f))

            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.restoreSession()
            assertTrue(viewModel.uiState.value.showResumeSessionDialog)

            viewModel.onResumeSessionDeclined()
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showResumeSessionDialog)
            assertEquals(1, repo.clearCount)
            // Defaults preserved
            assertEquals(0.5f, viewModel.uiState.value.overlayOpacity)
            assertNull(viewModel.uiState.value.overlayImageUri)
        }

        @Test
        fun `restoreSession with no saved data keeps defaults`() = runTest {
            val repo = FakeSessionRepository()
            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.restoreSession()

            assertNull(viewModel.uiState.value.overlayImageUri)
            assertEquals(0.5f, viewModel.uiState.value.overlayOpacity)
            assertEquals(1f, viewModel.uiState.value.overlayScale)
            assertFalse(viewModel.uiState.value.showResumeSessionDialog)
        }

        @Test
        fun `restoreSession only runs once`() = runTest {
            val repo = FakeSessionRepository()
            repo.save(SessionData(imageUri = "file:///img"))

            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.restoreSession()
            assertTrue(viewModel.uiState.value.showResumeSessionDialog)

            viewModel.onResumeSessionDeclined()
            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(viewModel.uiState.value.showResumeSessionDialog)

            // Second call should be no-op (e.g. returning from Settings)
            viewModel.restoreSession()
            assertFalse(viewModel.uiState.value.showResumeSessionDialog)
        }

        @Test
        fun `onImageSelected triggers immediate save`() = runTest {
            val repo = FakeSessionRepository()
            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.restoreSession()
            val uri = mockk<Uri>()
            every { uri.toString() } returns "file:///img"

            viewModel.onImageSelected(uri)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(repo.saveCount >= 1)
            assertEquals("file:///img", repo.lastSavedData().imageUri)
        }

        @Test
        fun `debounce save coalesces rapid changes`() = runTest {
            val repo = FakeSessionRepository()
            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.restoreSession()
            val uri = mockk<Uri>()
            every { uri.toString() } returns "file:///img"
            viewModel.onImageSelected(uri)
            testDispatcher.scheduler.advanceUntilIdle()
            val countAfterSelect = repo.saveCount

            // Rapid opacity changes should coalesce via debounce
            viewModel.onOpacityChanged(0.1f)
            viewModel.onOpacityChanged(0.2f)
            viewModel.onOpacityChanged(0.3f)
            testDispatcher.scheduler.advanceUntilIdle()

            // Should have only 1 debounced save (not 3)
            assertEquals(countAfterSelect + 1, repo.saveCount)
            assertEquals(0.3f, repo.lastSavedData().overlayOpacity)
        }

        @Test
        fun `onToggleSession triggers auto-save`() = runTest {
            val repo = FakeSessionRepository()
            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.restoreSession()
            viewModel.onToggleSession()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(repo.saveCount >= 1)
            assertTrue(viewModel.uiState.value.isSessionActive)
        }

        @Test
        fun `pause then resume preserves session state`() = runTest {
            val repo = FakeSessionRepository()
            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.restoreSession()
            viewModel.onToggleSession()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isSessionActive)

            viewModel.onToggleSession()
            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isSessionActive)
            assertTrue(repo.saveCount >= 2)
        }

        @Test
        fun `saveSession persists lock and viewport state`() = runTest {
            val repo = FakeSessionRepository()
            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.restoreSession()
            viewModel.setViewDimensions(1080f, 1920f)

            viewModel.onToggleLock()
            viewModel.onViewportZoom(3f)
            viewModel.onViewportPan(Offset(50f, 30f))
            viewModel.saveSession()

            val saved = repo.lastSavedData()
            assertTrue(saved.isOverlayLocked)
            assertEquals(3f, saved.viewportZoom, 0.01f)
            assertTrue(saved.viewportPanX != 0f || saved.viewportPanY != 0f)
        }

        @Test
        fun `viewport zoom triggers debounced save`() = runTest {
            val repo = FakeSessionRepository()
            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.restoreSession()
            viewModel.setViewDimensions(1080f, 1920f)
            viewModel.onToggleLock()
            testDispatcher.scheduler.advanceUntilIdle()
            val countBefore = repo.saveCount

            viewModel.onViewportZoom(2f)
            viewModel.onViewportPan(Offset(30f, 20f))
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(repo.saveCount > countBefore,
                "Viewport gestures should trigger debounced save")
        }

        @Test
        fun `restore session restores lock and viewport state`() = runTest {
            val fakeUri = mockk<Uri>()
            mockkStatic(Uri::class)
            every { Uri.parse(any()) } returns fakeUri
            every { fakeUri.toString() } returns "file:///data/ref"

            val repo = FakeSessionRepository()
            repo.save(
                SessionData(
                    imageUri = "file:///data/ref",
                    isOverlayLocked = true,
                    viewportZoom = 2.5f,
                    viewportPanX = 40f,
                    viewportPanY = 20f
                )
            )

            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.restoreSession()
            viewModel.onResumeSessionAccepted()

            assertTrue(viewModel.uiState.value.isOverlayLocked)
            assertEquals(2.5f, viewModel.uiState.value.viewportZoom, 0.01f)
            assertEquals(40f, viewModel.uiState.value.viewportPanX, 0.01f)
            assertEquals(20f, viewModel.uiState.value.viewportPanY, 0.01f)

            unmockkStatic(Uri::class)
        }
    }
}

private class FakeFlashlightController(
    override val hasFlashlight: Boolean = true
) : FlashlightController {

    private val _isTorchOn = MutableStateFlow(false)
    override val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    override fun toggleTorch() {
        if (hasFlashlight) {
            _isTorchOn.value = !_isTorchOn.value
        }
    }
}

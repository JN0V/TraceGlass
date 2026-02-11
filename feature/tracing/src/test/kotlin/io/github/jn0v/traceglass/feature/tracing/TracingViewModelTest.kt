package io.github.jn0v.traceglass.feature.tracing

import android.net.Uri
import androidx.compose.ui.geometry.Offset
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
    inner class SessionPersistence {
        @Test
        fun `saveSession persists current state`() = runTest {
            val repo = FakeSessionRepository()
            val viewModel = createViewModel(sessionRepository = repo)
            val uri = mockk<Uri>()
            viewModel.onImageSelected(uri)
            viewModel.onOpacityChanged(0.8f)

            viewModel.saveSession()

            assertEquals(1, repo.saveCount)
        }

        @Test
        fun `restoreSession loads saved state`() = runTest {
            val fakeUri = mockk<Uri>()
            mockkStatic(Uri::class)
            every { Uri.parse("content://test/image") } returns fakeUri

            val repo = FakeSessionRepository()
            repo.save(SessionData(
                imageUri = "content://test/image",
                overlayOffsetX = 50f,
                overlayOffsetY = 100f,
                overlayScale = 2f,
                overlayOpacity = 0.7f,
                colorTint = "RED",
                isInvertedMode = true,
                isSessionActive = true
            ))

            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.restoreSession()

            assertEquals(0.7f, viewModel.uiState.value.overlayOpacity)
            assertEquals(2f, viewModel.uiState.value.overlayScale)
            assertTrue(viewModel.uiState.value.isInvertedMode)
            assertEquals(fakeUri, viewModel.uiState.value.overlayImageUri)

            unmockkStatic(Uri::class)
        }

        @Test
        fun `restoreSession with no saved data keeps defaults`() = runTest {
            val repo = FakeSessionRepository()
            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.restoreSession()

            assertNull(viewModel.uiState.value.overlayImageUri)
            assertEquals(0.5f, viewModel.uiState.value.overlayOpacity)
            assertEquals(1f, viewModel.uiState.value.overlayScale)
        }

        @Test
        fun `onToggleSession triggers auto-save`() = runTest {
            val repo = FakeSessionRepository()
            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.onToggleSession()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, repo.saveCount)
            assertTrue(viewModel.uiState.value.isSessionActive)
        }

        @Test
        fun `pause then resume preserves session state`() = runTest {
            val repo = FakeSessionRepository()
            val viewModel = createViewModel(sessionRepository = repo)
            viewModel.onToggleSession()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isSessionActive)

            viewModel.onToggleSession()
            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isSessionActive)
            assertEquals(2, repo.saveCount)
        }
    }
}

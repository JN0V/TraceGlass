package io.github.jn0v.traceglass

import io.github.jn0v.traceglass.core.cv.DetectionMode
import io.github.jn0v.traceglass.core.cv.DetectionModeSelector
import io.github.jn0v.traceglass.feature.onboarding.OnboardingRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetectionModeSyncTest {

    @Test
    fun `a stored mode reaches the selector and later changes follow`() = runTest(UnconfinedTestDispatcher()) {
        val stored = MutableStateFlow(DetectionMode.CUSTOM_SHAPES)
        val repository = mockk<OnboardingRepository>()
        every { repository.detectionMode } returns stored
        val selector = DetectionModeSelector()

        val job = launch { syncDetectionMode(repository, selector) }
        assertEquals(DetectionMode.CUSTOM_SHAPES, selector.mode)

        stored.value = DetectionMode.ARUCO
        assertEquals(DetectionMode.ARUCO, selector.mode)
        job.cancel()
    }
}

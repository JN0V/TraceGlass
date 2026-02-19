package io.github.jn0v.traceglass.feature.tracing.settings

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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeSettingsRepository
    private lateinit var vm: SettingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeSettingsRepository()
        vm = SettingsViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    inner class InitialState {
        @Test
        fun `audio feedback is off by default`() {
            assertFalse(vm.uiState.value.audioFeedbackEnabled)
        }

        @Test
        fun `break reminder is off by default`() {
            assertFalse(vm.uiState.value.breakReminderEnabled)
        }

        @Test
        fun `break interval is 30 minutes by default`() {
            assertEquals(30, vm.uiState.value.breakReminderIntervalMinutes)
        }
    }

    @Nested
    inner class AudioFeedback {
        @Test
        fun `toggle audio feedback enables it`() = runTest(testDispatcher) {
            vm.onAudioFeedbackToggled(true)
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(vm.uiState.value.audioFeedbackEnabled)
        }

        @Test
        fun `toggle audio feedback persists to repository`() = runTest(testDispatcher) {
            vm.onAudioFeedbackToggled(true)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(1, repo.audioFeedbackSetCount)
        }

        @Test
        fun `toggle audio feedback off after on`() = runTest(testDispatcher) {
            vm.onAudioFeedbackToggled(true)
            testDispatcher.scheduler.advanceUntilIdle()
            vm.onAudioFeedbackToggled(false)
            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(vm.uiState.value.audioFeedbackEnabled)
        }
    }

    @Nested
    inner class BreakReminder {
        @Test
        fun `toggle break reminder enables it`() = runTest(testDispatcher) {
            vm.onBreakReminderToggled(true)
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(vm.uiState.value.breakReminderEnabled)
        }

        @Test
        fun `toggle break reminder persists to repository`() = runTest(testDispatcher) {
            vm.onBreakReminderToggled(true)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(1, repo.breakReminderSetCount)
        }

        @Test
        fun `change interval updates state`() = runTest(testDispatcher) {
            vm.onBreakIntervalChanged(45)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(45, vm.uiState.value.breakReminderIntervalMinutes)
        }

        @Test
        fun `change interval persists to repository`() = runTest(testDispatcher) {
            vm.onBreakIntervalChanged(15)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(1, repo.breakIntervalSetCount)
        }
    }

    @Nested
    inner class NonDefaultInitialState {
        @Test
        fun `loads pre-existing audio feedback enabled from repository`() = runTest(testDispatcher) {
            repo.setAudioFeedbackEnabled(true)
            val freshVm = SettingsViewModel(repo)
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(freshVm.uiState.value.audioFeedbackEnabled)
        }

        @Test
        fun `loads pre-existing break interval from repository`() = runTest(testDispatcher) {
            repo.setBreakReminderIntervalMinutes(15)
            val freshVm = SettingsViewModel(repo)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(15, freshVm.uiState.value.breakReminderIntervalMinutes)
        }
    }

    @Nested
    inner class IntervalValidation {
        @Test
        fun `clamps interval below minimum to 5`() = runTest(testDispatcher) {
            vm.onBreakIntervalChanged(0)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(5, vm.uiState.value.breakReminderIntervalMinutes)
        }

        @Test
        fun `clamps interval above maximum to 60`() = runTest(testDispatcher) {
            vm.onBreakIntervalChanged(100)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(60, vm.uiState.value.breakReminderIntervalMinutes)
        }
    }
}

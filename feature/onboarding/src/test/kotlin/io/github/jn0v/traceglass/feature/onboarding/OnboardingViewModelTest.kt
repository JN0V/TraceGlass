package io.github.jn0v.traceglass.feature.onboarding

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
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeOnboardingRepository

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeOnboardingRepository()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): OnboardingViewModel {
        val vm = OnboardingViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    @Nested
    inner class InitialState {
        @Test
        fun `starts on page 0`() {
            val vm = createViewModel()
            assertEquals(0, vm.uiState.value.currentPage)
        }

        @Test
        fun `default tier is FULL_DIY`() {
            val vm = createViewModel()
            assertEquals(SetupTier.FULL_DIY, vm.uiState.value.selectedTier)
        }

        @Test
        fun `not completed initially`() {
            val vm = createViewModel()
            assertFalse(vm.uiState.value.isCompleted)
        }

        @Test
        fun `restores persisted tier on init`() = runTest {
            repo.setSelectedTier(SetupTier.FULL_KIT)
            val vm = createViewModel()
            assertEquals(SetupTier.FULL_KIT, vm.uiState.value.selectedTier)
        }

        @Test
        fun `uses default tier when no persisted tier`() {
            val vm = createViewModel()
            assertEquals(SetupTier.FULL_DIY, vm.uiState.value.selectedTier)
        }
    }

    @Nested
    inner class Navigation {
        @Test
        fun `onNextPage advances to next page`() {
            val vm = createViewModel()
            vm.onNextPage()
            assertEquals(1, vm.uiState.value.currentPage)
        }

        @Test
        fun `onNextPage does not exceed last page`() {
            val vm = createViewModel()
            repeat(OnboardingViewModel.PAGE_COUNT + 1) { vm.onNextPage() }
            assertEquals(OnboardingViewModel.PAGE_COUNT - 1, vm.uiState.value.currentPage)
        }

        @Test
        fun `onPreviousPage goes back`() {
            val vm = createViewModel()
            vm.onNextPage()
            vm.onNextPage()
            assertEquals(2, vm.uiState.value.currentPage)
            vm.onPreviousPage()
            assertEquals(1, vm.uiState.value.currentPage)
        }

        @Test
        fun `onPreviousPage does not go below 0`() {
            val vm = createViewModel()
            vm.onPreviousPage()
            assertEquals(0, vm.uiState.value.currentPage)
        }

        @Test
        fun `onPageChanged sets current page`() {
            val vm = createViewModel()
            vm.onPageChanged(2)
            assertEquals(2, vm.uiState.value.currentPage)
        }

        @Test
        fun `onPageChanged clamps negative values to 0`() {
            val vm = createViewModel()
            vm.onPageChanged(-1)
            assertEquals(0, vm.uiState.value.currentPage)
        }

        @Test
        fun `onPageChanged clamps values above max to last page`() {
            val vm = createViewModel()
            vm.onPageChanged(100)
            assertEquals(OnboardingViewModel.PAGE_COUNT - 1, vm.uiState.value.currentPage)
        }
    }

    @Nested
    inner class TierSelection {
        @Test
        fun `onTierSelected changes tier`() {
            val vm = createViewModel()
            vm.onTierSelected(SetupTier.SEMI_EQUIPPED)
            assertEquals(SetupTier.SEMI_EQUIPPED, vm.uiState.value.selectedTier)
        }

        @Test
        fun `onTierSelected to FULL_KIT`() {
            val vm = createViewModel()
            vm.onTierSelected(SetupTier.FULL_KIT)
            assertEquals(SetupTier.FULL_KIT, vm.uiState.value.selectedTier)
        }

        @Test
        fun `onTierSelected persists to repository`() = runTest {
            val vm = createViewModel()
            vm.onTierSelected(SetupTier.SEMI_EQUIPPED)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(1, repo.setTierCount)
        }
    }

    @Nested
    inner class Completion {
        @Test
        fun `onComplete marks as completed`() = runTest {
            val vm = createViewModel()
            vm.onComplete()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(vm.uiState.value.isCompleted)
            assertFalse(vm.uiState.value.wasSkipped)
        }

        @Test
        fun `onComplete persists to repository`() = runTest {
            val vm = createViewModel()
            vm.onComplete()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(1, repo.setCompletedCount)
        }

        @Test
        fun `onSkip marks as completed with wasSkipped true`() = runTest {
            val vm = createViewModel()
            vm.onSkip()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(vm.uiState.value.isCompleted)
            assertTrue(vm.uiState.value.wasSkipped)
            assertEquals(1, repo.setCompletedCount)
        }
    }

    @Nested
    inner class Reopen {
        @Test
        fun `onReopen resets page to 0`() {
            val vm = createViewModel()
            vm.onPageChanged(2)
            assertEquals(2, vm.uiState.value.currentPage)
            vm.onReopen()
            assertEquals(0, vm.uiState.value.currentPage)
        }

        @Test
        fun `onReopen does not call resetOnboarding on repository`() {
            val vm = createViewModel()
            vm.onReopen()
            assertEquals(0, repo.resetCount)
        }

        @Test
        fun `onComplete after reopen does not persist to repository`() = runTest {
            val vm = createViewModel()
            vm.onReopen()
            vm.onComplete()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(0, repo.setCompletedCount)
        }

        @Test
        fun `onComplete after reopen still marks isCompleted true`() = runTest {
            val vm = createViewModel()
            vm.onReopen()
            vm.onComplete()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(vm.uiState.value.isCompleted)
        }

        @Test
        fun `onSkip after reopen does not persist to repository`() = runTest {
            val vm = createViewModel()
            vm.onReopen()
            vm.onSkip()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(0, repo.setCompletedCount)
        }

        @Test
        fun `onReopen preserves selected tier`() {
            val vm = createViewModel()
            vm.onTierSelected(SetupTier.FULL_KIT)
            vm.onReopen()
            assertEquals(SetupTier.FULL_KIT, vm.uiState.value.selectedTier)
        }
    }
}

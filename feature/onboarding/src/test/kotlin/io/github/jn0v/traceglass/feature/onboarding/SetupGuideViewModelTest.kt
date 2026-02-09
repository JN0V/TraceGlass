package io.github.jn0v.traceglass.feature.onboarding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupGuideViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var vm: SetupGuideViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        vm = SetupGuideViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    inner class InitialState {
        @Test
        fun `default section is MARKER_GUIDE`() {
            assertEquals(SetupGuideSection.MARKER_GUIDE, vm.uiState.value.selectedSection)
        }
    }

    @Nested
    inner class SectionNavigation {
        @Test
        fun `onSectionSelected changes to MACGYVER_GUIDE`() {
            vm.onSectionSelected(SetupGuideSection.MACGYVER_GUIDE)
            assertEquals(SetupGuideSection.MACGYVER_GUIDE, vm.uiState.value.selectedSection)
        }

        @Test
        fun `onSectionSelected changes back to MARKER_GUIDE`() {
            vm.onSectionSelected(SetupGuideSection.MACGYVER_GUIDE)
            vm.onSectionSelected(SetupGuideSection.MARKER_GUIDE)
            assertEquals(SetupGuideSection.MARKER_GUIDE, vm.uiState.value.selectedSection)
        }
    }
}

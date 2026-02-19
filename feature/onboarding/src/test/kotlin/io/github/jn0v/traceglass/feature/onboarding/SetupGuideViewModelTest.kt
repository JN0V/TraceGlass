package io.github.jn0v.traceglass.feature.onboarding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

        @Test
        fun `initial state matches default constructor`() {
            assertEquals(SetupGuideUiState(), vm.uiState.value)
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

        @Test
        fun `selecting same section is idempotent`() {
            val before = vm.uiState.value
            vm.onSectionSelected(SetupGuideSection.MARKER_GUIDE)
            assertEquals(before, vm.uiState.value)
        }

        @Test
        fun `all enum values are selectable`() {
            SetupGuideSection.entries.forEach { section ->
                vm.onSectionSelected(section)
                assertEquals(section, vm.uiState.value.selectedSection)
            }
        }
    }

    @Nested
    inner class UrlConstants {
        @Test
        fun `marker sheet URL is not blank`() {
            assertTrue(SetupGuideViewModel.MARKER_SHEET_URL.isNotBlank())
        }

        @Test
        fun `stand model URL is not blank`() {
            assertTrue(SetupGuideViewModel.STAND_MODEL_URL.isNotBlank())
        }

        @Test
        fun `marker sheet URL ends with pdf extension`() {
            assertTrue(SetupGuideViewModel.MARKER_SHEET_URL.endsWith(".pdf"))
        }

        @Test
        fun `stand model URL ends with stl extension`() {
            assertTrue(SetupGuideViewModel.STAND_MODEL_URL.endsWith(".stl"))
        }

        @Test
        fun `URLs use https`() {
            assertTrue(SetupGuideViewModel.MARKER_SHEET_URL.startsWith("https://"))
            assertTrue(SetupGuideViewModel.STAND_MODEL_URL.startsWith("https://"))
        }
    }
}

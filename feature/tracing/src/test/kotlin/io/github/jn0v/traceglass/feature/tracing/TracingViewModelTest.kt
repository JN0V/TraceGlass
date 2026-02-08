package io.github.jn0v.traceglass.feature.tracing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TracingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has permission not requested`() {
        val viewModel = TracingViewModel()
        assertEquals(PermissionState.NOT_REQUESTED, viewModel.uiState.value.permissionState)
    }

    @Test
    fun `when permission granted then state is GRANTED`() {
        val viewModel = TracingViewModel()
        viewModel.onPermissionResult(granted = true)
        assertEquals(PermissionState.GRANTED, viewModel.uiState.value.permissionState)
    }

    @Test
    fun `when permission denied then state is DENIED`() {
        val viewModel = TracingViewModel()
        viewModel.onPermissionResult(granted = false)
        assertEquals(PermissionState.DENIED, viewModel.uiState.value.permissionState)
    }

    @Test
    fun `when permission denied then granted on retry then state is GRANTED`() {
        val viewModel = TracingViewModel()
        viewModel.onPermissionResult(granted = false)
        assertEquals(PermissionState.DENIED, viewModel.uiState.value.permissionState)
        viewModel.onPermissionResult(granted = true)
        assertEquals(PermissionState.GRANTED, viewModel.uiState.value.permissionState)
    }
}

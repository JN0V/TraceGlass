package io.github.jn0v.traceglass.feature.timelapse

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimelapseSession(
    private val intervalMs: Long = 5000L,
    private val onCapture: suspend (index: Int) -> Unit = {}
) {
    private val _state = MutableStateFlow(TimelapseState.IDLE)
    val state: StateFlow<TimelapseState> = _state.asStateFlow()

    private val _snapshotCount = MutableStateFlow(0)
    val snapshotCount: StateFlow<Int> = _snapshotCount.asStateFlow()

    private var captureJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (_state.value == TimelapseState.RECORDING) return
        _state.value = TimelapseState.RECORDING
        startCaptureLoop(scope)
    }

    fun pause() {
        if (_state.value != TimelapseState.RECORDING) return
        _state.value = TimelapseState.PAUSED
        captureJob?.cancel()
        captureJob = null
    }

    fun resume(scope: CoroutineScope) {
        if (_state.value != TimelapseState.PAUSED) return
        _state.value = TimelapseState.RECORDING
        startCaptureLoop(scope)
    }

    fun stop() {
        captureJob?.cancel()
        captureJob = null
        _state.value = TimelapseState.IDLE
        _snapshotCount.value = 0
    }

    private fun startCaptureLoop(scope: CoroutineScope) {
        captureJob?.cancel()
        captureJob = scope.launch {
            while (true) {
                val index = _snapshotCount.value
                onCapture(index)
                _snapshotCount.value = index + 1
                delay(intervalMs)
            }
        }
    }
}

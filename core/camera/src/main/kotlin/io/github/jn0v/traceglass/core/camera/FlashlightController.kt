package io.github.jn0v.traceglass.core.camera

import kotlinx.coroutines.flow.StateFlow

interface FlashlightController {
    val isTorchOn: StateFlow<Boolean>
    val hasFlashlight: Boolean
    fun toggleTorch()
}

package io.github.jn0v.traceglass.core.camera

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeFlashlightController(
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

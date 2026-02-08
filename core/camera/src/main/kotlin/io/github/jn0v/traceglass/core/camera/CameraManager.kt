package io.github.jn0v.traceglass.core.camera

import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner

interface CameraManager {
    fun bindPreview(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider)
    fun unbind()
}

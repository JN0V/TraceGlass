package io.github.jn0v.traceglass.core.camera.di

import io.github.jn0v.traceglass.core.camera.CameraManager
import io.github.jn0v.traceglass.core.camera.FlashlightController
import io.github.jn0v.traceglass.core.camera.impl.CameraXManager
import org.koin.dsl.module

val cameraModule = module {
    single { CameraXManager(get()) }
    single<CameraManager> { get<CameraXManager>() }
    single<FlashlightController> { get<CameraXManager>() }
}

package io.github.jn0v.traceglass.core.camera.di

import io.github.jn0v.traceglass.core.camera.CameraManager
import io.github.jn0v.traceglass.core.camera.impl.CameraXManager
import org.koin.dsl.module

val cameraModule = module {
    single<CameraManager> { CameraXManager(get()) }
}

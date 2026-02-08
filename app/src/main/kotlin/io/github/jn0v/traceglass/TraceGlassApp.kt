package io.github.jn0v.traceglass

import android.app.Application
import io.github.jn0v.traceglass.core.camera.di.cameraModule
import io.github.jn0v.traceglass.core.cv.di.cvModule
import io.github.jn0v.traceglass.feature.tracing.di.tracingModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TraceGlassApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TraceGlassApp)
            modules(cameraModule, cvModule, tracingModule)
        }
    }
}

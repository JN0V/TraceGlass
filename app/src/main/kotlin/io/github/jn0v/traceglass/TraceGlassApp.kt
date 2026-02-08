package io.github.jn0v.traceglass

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TraceGlassApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TraceGlassApp)
            modules(emptyList())
        }
    }
}

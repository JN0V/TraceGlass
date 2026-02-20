# kotlinx.coroutines — targeted rules for R8 compatibility
# ServiceLoader classes that R8 cannot trace statically
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Volatile fields updated via AtomicFU — must not be mangled
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Koin DI — keep implementation classes resolved by name at runtime
-keep class io.github.jn0v.traceglass.core.camera.CameraXManager { *; }
-keep class io.github.jn0v.traceglass.core.session.DataStoreSessionRepository { *; }
-keep class io.github.jn0v.traceglass.core.session.DataStoreSettingsRepository { *; }
-keep class io.github.jn0v.traceglass.feature.timelapse.FileSnapshotStorage { *; }
-keep class io.github.jn0v.traceglass.feature.timelapse.MediaCodecCompiler { *; }
-keep class io.github.jn0v.traceglass.feature.timelapse.MediaStoreVideoExporter { *; }
-keep class io.github.jn0v.traceglass.feature.timelapse.IntentVideoSharer { *; }
-keep class io.github.jn0v.traceglass.feature.onboarding.DataStoreOnboardingRepository { *; }

# Koin module definitions
-keep class io.github.jn0v.traceglass.**.di.*Module* { *; }

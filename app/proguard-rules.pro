# kotlinx.coroutines — targeted rules for R8 compatibility
# ServiceLoader classes that R8 cannot trace statically
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Volatile fields updated via AtomicFU — must not be mangled
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

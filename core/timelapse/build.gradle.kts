plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.jn0v.traceglass.core.timelapse"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}

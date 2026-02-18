plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.jn0v.traceglass.core.overlay"

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:cv"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.koin.android)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}


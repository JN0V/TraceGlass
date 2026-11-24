plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

// The release version comes from the pushed tag (TRACEGLASS_VERSION_NAME, e.g. "1.2.3"
// from tag v1.2.3): versionCode = major * 10000 + minor * 100 + patch, so it only goes up.
val taggedVersionName: String? = System.getenv("TRACEGLASS_VERSION_NAME")?.takeIf { it.isNotBlank() }
val releaseVersionName: String = taggedVersionName ?: "0.1.0-dev"
val releaseVersionCode: Int = run {
    val parts = releaseVersionName.substringBefore('-').split('.').map { it.toIntOrNull() }
    if (parts.size == 3 && parts.all { it != null && it in 0..99 }) {
        parts[0]!! * 10000 + parts[1]!! * 100 + parts[2]!!
    } else if (taggedVersionName != null) {
        throw GradleException(
            "TRACEGLASS_VERSION_NAME '$taggedVersionName' is not major.minor.patch with each part 0..99; " +
                "refusing to build a release whose versionCode would not go up."
        )
    } else {
        1
    }
}

val keystoreFile: String? = System.getenv("TRACEGLASS_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
val keystorePassword: String? = System.getenv("TRACEGLASS_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
val keyAlias: String? = System.getenv("TRACEGLASS_KEY_ALIAS")?.takeIf { it.isNotBlank() }
val keyPassword: String? = System.getenv("TRACEGLASS_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
val hasReleaseKey = keystoreFile != null && keystorePassword != null && keyAlias != null && keyPassword != null

android {
    namespace = "io.github.jn0v.traceglass"

    defaultConfig {
        applicationId = "io.github.jn0v.traceglass"
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.generatedDensities?.clear()
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystoreFile!!)
                storePassword = keystorePassword
                keyAlias = this@Build_gradle.keyAlias
                keyPassword = this@Build_gradle.keyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Never falls back to the debug key: without the project key the APK stays unsigned.
            if (hasReleaseKey) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

}

// No secrets, no APK: `verifyReleaseSigning` fails, naming each variable that is missing, and
// every release artifact task of a tag build (TRACEGLASS_VERSION_NAME set) depends on it, so an
// unsigned APK is never produced. Debug builds, unit tests and the plain CI build stay unaffected.
val verifyReleaseSigning = tasks.register("verifyReleaseSigning") {
    group = "verification"
    description = "Fails unless the four TRACEGLASS_* release signing variables are set."
    val vars = listOf(
        "TRACEGLASS_KEYSTORE_FILE" to keystoreFile,
        "TRACEGLASS_KEYSTORE_PASSWORD" to keystorePassword,
        "TRACEGLASS_KEY_ALIAS" to keyAlias,
        "TRACEGLASS_KEY_PASSWORD" to keyPassword
    )
    doLast {
        val missing = vars.filter { it.second == null }.map { it.first }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Release signing secret missing: ${missing.joinToString()}. Refusing to build an unsigned release APK."
            )
        }
    }
}

if (taggedVersionName != null) {
    tasks.configureEach {
        if (Regex("^(assemble|bundle|package)Release$").matches(name)) {
            dependsOn(verifyReleaseSigning)
        }
    }
}

tasks.whenTaskAdded {
    if (name.contains("ArtProfile")) {
        enabled = false
    }
}

dependencies {
    implementation(project(":core:camera"))
    implementation(project(":core:cv"))
    implementation(project(":core:session"))
    implementation(project(":feature:tracing"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:timelapse"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}


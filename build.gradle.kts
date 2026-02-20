plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}

subprojects {
    pluginManager.withPlugin("com.android.library") {
        configure<com.android.build.gradle.LibraryExtension> {
            compileSdk = 36
            buildToolsVersion = "34.0.0"
            defaultConfig.minSdk = 33
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            lint {
                abortOnError = true
                warningsAsErrors = false
                checkDependencies = false
            }
        }
    }
    pluginManager.withPlugin("com.android.application") {
        configure<com.android.build.api.dsl.ApplicationExtension> {
            compileSdk = 36
            buildToolsVersion = "34.0.0"
            defaultConfig.minSdk = 33
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            lint {
                abortOnError = true
                warningsAsErrors = false
                checkDependencies = true
            }
        }
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "17"
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

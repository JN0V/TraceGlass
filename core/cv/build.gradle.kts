plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.jn0v.traceglass.core.cv"
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        consumerProguardFiles("proguard-rules.pro")

        ndk {
            // x86 excluded: OpenCV 4.10.0 libippicv.a linker error (ld.lld .note.gnu.property)
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                    "-DOpenCV_DIR=${project.rootDir}/sdk/OpenCV-android-sdk/sdk/native/jni"
                )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.5"
        }
    }
}

dependencies {
    implementation(project(":core:camera"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.koin.android)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}


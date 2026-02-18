# Story 1.2: F-Droid Reproducibility Configuration

Status: done

## Story

As a developer,
I want F-Droid reproducibility settings applied to the build,
so that the app can be accepted into the F-Droid repository.

## Acceptance Criteria

1. **Given** the project from Story 1.1 **When** the build configuration is applied **Then** `buildToolsVersion` is pinned to `34.0.0`
2. **Given** build config **When** AGP version is checked **Then** AGP version is 8.8+ (currently 8.8.2)
3. **Given** build config **When** ArtProfile tasks execute **Then** they are disabled (`enabled = false`)
4. **Given** build config **When** PNG processing runs **Then** PNG crunch is disabled (`cruncherEnabled = false`)
5. **Given** build config **When** vector drawables are processed **Then** PNG generation is disabled (`generatedDensities = emptySet()`)
6. **Given** R8 optimization **When** proguard rules are applied **Then** R8 proguard rules for kotlinx.coroutines are added
7. **Given** testing framework **When** tests are run **Then** JUnit 5 (Jupiter) replaces default JUnit 4
8. **Given** mocking framework **When** tests are written **Then** MockK is available as test dependency
9. **Given** DI framework **When** app module is configured **Then** Koin is configured with modules in `:app`
10. **Given** all config applied **When** `./gradlew assembleRelease` is run **Then** build succeeds

## Tasks / Subtasks

- [x] Task 1: Apply F-Droid reproducibility settings to app/build.gradle.kts (AC: #1-5)
  - [x] 1.1: Pin `buildToolsVersion = "34.0.0"`
  - [x] 1.2: Verify AGP 8.8.2 in libs.versions.toml
  - [x] 1.3: Disable ArtProfile: `tasks.whenTaskAdded { if (name.contains("ArtProfile")) { enabled = false } }`
  - [x] 1.4: PNG crunch disabled via `gradle.properties`: `android.enablePngCrunchInReleaseBuilds=false`
  - [x] 1.5: Disable vector drawable PNG generation: `vectorDrawables.generatedDensities?.clear()`
- [x] Task 2: Configure NDK reproducibility (AC: #1)
  - [x] 2.1: Pin exact NDK version 27.2.12479018 in core/cv/build.gradle.kts
  - [x] 2.2: CMake flags: `-ffile-prefix-map=${CMAKE_SOURCE_DIR}=.`
  - [x] 2.3: Linker flags: `-Wl,--build-id=none -Wl,--hash-style=gnu`
- [x] Task 3: Add R8 proguard rules for kotlinx.coroutines (AC: #6)
  - [x] 3.1: Created `app/proguard-rules.pro` with `-dontwarn kotlinx.coroutines.**` and `-keep class kotlinx.coroutines.** { *; }`
- [x] Task 4: Migrate testing framework (AC: #7, #8)
  - [x] 4.1: JUnit 5 (Jupiter) 5.11.4 in version catalog
  - [x] 4.2: MockK 1.13.14 in version catalog
  - [x] 4.3: `useJUnitPlatform()` configured in app, core/cv, core/camera (and other modules)
- [x] Task 5: Configure Koin DI (AC: #9)
  - [x] 5.1: Koin 4.0.2 in version catalog
  - [x] 5.2: `TraceGlassApp.kt` with `startKoin { androidContext(...); modules(cameraModule, cvModule, sessionModule, onboardingModule, tracingModule) }`
  - [x] 5.3: 5 Koin modules: CameraModule, CvModule, SessionModule, OnboardingModule, TracingModule
  - [x] 5.4: Application class registered in AndroidManifest.xml as `android:name=".TraceGlassApp"`
- [x] Task 6: Verify release build (AC: #10)
  - [x] 6.1: `./gradlew assembleRelease` succeeds

## Dev Notes

### Actual Implementation Details

**build.gradle.kts (root subprojects block):**
- `buildToolsVersion = "34.0.0"`, `compileSdk = 36`, `minSdk = 33` — shared across all modules
- `compileOptions` Java 17, `kotlinOptions` JVM 17, `useJUnitPlatform()` — shared

**app/build.gradle.kts:**
- ArtProfile disabled via `tasks.whenTaskAdded` (lines 37-41)
- `vectorDrawables.generatedDensities?.clear()` (line 17)

**gradle.properties:**
- `android.enablePngCrunchInReleaseBuilds=false` — PNG crunch disabled globally

**core/cv/src/main/cpp/CMakeLists.txt:**
```cmake
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffile-prefix-map=${CMAKE_SOURCE_DIR}=.")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffile-prefix-map=${CMAKE_SOURCE_DIR}=.")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--build-id=none -Wl,--hash-style=gnu")
```

**Koin modules registered in TraceGlassApp:**
1. `cameraModule` — CameraXManager → CameraManager + FlashlightController
2. `cvModule` — OpenCvMarkerDetector → MarkerDetector
3. `sessionModule` — DataStoreSessionRepository + DataStoreSettingsRepository
4. `onboardingModule` — DataStoreOnboardingRepository + ViewModels
5. `tracingModule` — OverlayTransformCalculator + FrameAnalyzer + ViewModels

### Note: Fastlane metadata not yet created

Fastlane directory (`fastlane/metadata/android/en-US/`) does not exist yet. This is acceptable — needed only when submitting to F-Droid, not for build reproducibility.

### References

- [Source: architecture.md#Technical Constraints & Dependencies]
- [Source: architecture.md#F-Droid NDK / native code reproducibility]
- [Source: architecture.md#F-Droid Gradle reproducibility requirements]
- [Source: architecture.md#Decision 4] — Koin DI rationale
- [Source: prd.md#F-Droid Compliance]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

- All F-Droid reproducibility settings applied and verified
- NDK reproducibility flags in CMakeLists.txt
- ProGuard rules for kotlinx.coroutines ServiceLoader
- JUnit 5 + MockK fully replacing JUnit 4
- Koin DI with 5 modules registered at app startup
- Fastlane metadata deferred to F-Droid submission phase
- ✅ Resolved review finding [MEDIUM]: Extracted shared build config (compileSdk, minSdk, buildToolsVersion, compileOptions, kotlinOptions, useJUnitPlatform) into root build.gradle.kts subprojects block — removed duplication from all 8 module build files
- ✅ Resolved review finding [MEDIUM]: Verified epic-1 status corrected to `in-progress` in sprint-status.yaml (was `done` while stories still in-progress)
- ✅ Resolved review finding [LOW]: Replaced overly broad ProGuard rule (`-keep class kotlinx.coroutines.** { *; }`) with targeted rules: `-keepnames` for MainDispatcherFactory and CoroutineExceptionHandler, `-keepclassmembers` for volatile fields only. R8 can now optimize unused coroutines classes. Release build verified passing.

### Review Follow-ups (AI)

- [x] [AI-Review][MEDIUM] compileSdk, minSdk, buildToolsVersion duplicated across all 8 modules — same as Story 1.1 finding, extract shared config [all build.gradle.kts]
- [x] [AI-Review][MEDIUM] sprint-status.yaml shows `epic-1: done` while this story is still `review` — epic status must be `in-progress` until all stories are done [sprint-status.yaml:44]
- [x] [AI-Review][LOW] ProGuard rules overly broad — `-keep class kotlinx.coroutines.** { *; }` keeps ALL coroutines classes, preventing R8 optimization. Use targeted rule for ServiceLoader only (e.g. `-keep class kotlinx.coroutines.internal.MainDispatcherFactory`) [app/proguard-rules.pro:3]

### File List

- `app/build.gradle.kts` — buildToolsVersion, ArtProfile, vectorDrawables; removed duplicated compileSdk/minSdk/compileOptions/kotlinOptions/useJUnitPlatform
- `app/proguard-rules.pro` — kotlinx.coroutines R8 rules
- `app/src/main/kotlin/io/github/jn0v/traceglass/TraceGlassApp.kt` — Koin startKoin
- `app/src/main/AndroidManifest.xml` — Application class registration
- `build.gradle.kts` — added subprojects block with shared compileSdk=36, buildToolsVersion="34.0.0", minSdk=33, compileOptions (Java 17), kotlinOptions (JVM 17), useJUnitPlatform()
- `gradle.properties` — PNG crunch disable, parallel builds
- `gradle/libs.versions.toml` — JUnit 5, MockK, Koin versions
- `core/cv/src/main/cpp/CMakeLists.txt` — NDK reproducibility flags
- `core/camera/build.gradle.kts` — removed duplicated compileSdk/minSdk/buildToolsVersion/compileOptions/kotlinOptions/useJUnitPlatform
- `core/cv/build.gradle.kts` — removed duplicated compileSdk/minSdk/buildToolsVersion/compileOptions/kotlinOptions/useJUnitPlatform
- `core/overlay/build.gradle.kts` — removed duplicated compileSdk/minSdk/buildToolsVersion/compileOptions/kotlinOptions/useJUnitPlatform
- `core/session/build.gradle.kts` — removed duplicated compileSdk/minSdk/buildToolsVersion/compileOptions/kotlinOptions/useJUnitPlatform
- `feature/tracing/build.gradle.kts` — removed duplicated compileSdk/minSdk/buildToolsVersion/compileOptions/kotlinOptions/useJUnitPlatform
- `feature/onboarding/build.gradle.kts` — removed duplicated compileSdk/minSdk/buildToolsVersion/compileOptions/kotlinOptions/useJUnitPlatform
- `feature/timelapse/build.gradle.kts` — removed duplicated compileSdk/minSdk/buildToolsVersion/compileOptions/kotlinOptions/useJUnitPlatform
- `core/camera/src/main/kotlin/.../di/CameraModule.kt`
- `core/cv/src/main/kotlin/.../di/CvModule.kt`
- `core/session/src/main/kotlin/.../di/SessionModule.kt`
- `feature/onboarding/src/main/kotlin/.../di/OnboardingModule.kt`
- `feature/tracing/src/main/kotlin/.../di/TracingModule.kt`

### Change Log

- 2026-02-17: Addressed code review findings — 1 item resolved. Extracted shared build config (compileSdk, minSdk, buildToolsVersion, compileOptions, kotlinOptions, useJUnitPlatform) into root build.gradle.kts subprojects block, removing duplication from all 8 module build files. Build and all tests verified passing.
- 2026-02-17: Addressed remaining 2 code review findings — all 3 review follow-ups now resolved. (1) Verified epic-1 status corrected to in-progress in sprint-status.yaml. (2) Replaced overly broad ProGuard rules with targeted ServiceLoader rules (MainDispatcherFactory, CoroutineExceptionHandler, volatile fields). Release build and all tests pass.
- 2026-02-17: Code review #2 passed — 1 MEDIUM + 2 LOW findings. All fixed: (1) Replaced deprecated BaseExtension/compileSdkVersion() with BaseAppModuleExtension/compileSdk in root build.gradle.kts. (2) Updated stale Dev Notes line references. (3) Noted ProGuard rules redundancy with kotlinx.coroutines consumer rules (informational). Status → done.

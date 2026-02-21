# TraceGlass — Development Guide

> Generated: 2026-02-21

## Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| Android Studio | Latest stable | Hedgehog or newer recommended |
| JDK | 17 (Temurin) | Set in project config |
| Android SDK | API 36 | compileSdk / targetSdk |
| NDK | 27.2.12479018 | For OpenCV native build |
| CMake | 3.31.5 | Native build system |
| Git | Any | Version control |

## Initial Setup

### 1. Clone and Open

```bash
git clone https://github.com/JN0V/TraceGlass.git
cd TraceGlass
```

Open in Android Studio. The Gradle sync will download dependencies automatically.

### 2. OpenCV SDK

The OpenCV Android SDK is expected at `sdk/OpenCV-android-sdk/`. If missing:

```bash
# Download OpenCV 4.10.0 Android SDK
wget https://github.com/opencv/opencv/releases/download/4.10.0/opencv-4.10.0-android-sdk.zip
unzip opencv-4.10.0-android-sdk.zip -d sdk/
```

The CI workflow caches this automatically with SHA256 verification.

### 3. NDK Installation

Android Studio should auto-install the NDK. If not:
- Open SDK Manager → SDK Tools → NDK (Side by side)
- Install version `27.2.12479018`

## Build Commands

```bash
# Debug build (all ABIs)
./gradlew assembleDebug

# Release build (minified, shrunk)
./gradlew assembleRelease

# Build + install on connected device
./gradlew installDebug

# Specific ABI only (faster build)
./gradlew assembleDebug -Pabi=arm64-v8a
```

## Test Commands

```bash
# All unit tests
./gradlew test

# Specific module tests
./gradlew :core:overlay:test          # Homography, tracking (most important)
./gradlew :core:camera:test           # Lifecycle tests
./gradlew :core:cv:test               # Data class tests
./gradlew :core:session:test          # Persistence tests
./gradlew :feature:tracing:test       # ViewModel tests (largest suite)
./gradlew :feature:onboarding:test    # Onboarding flow tests
./gradlew :feature:timelapse:test     # Timelapse pipeline tests

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

## Project Structure

```
TraceGlass/
├── app/                    # Application shell (navigation, DI)
├── core/
│   ├── camera/             # CameraX abstraction
│   ├── cv/                 # OpenCV JNI (ArUco detection)
│   ├── overlay/            # Homography math engine
│   ├── session/            # DataStore persistence
│   └── timelapse/          # Timelapse interfaces
├── feature/
│   ├── tracing/            # Main tracing screen
│   ├── onboarding/         # First-time UX
│   └── timelapse/          # Timelapse implementations
├── sdk/                    # OpenCV Android SDK
└── _bmad-output/           # Specs & stories
```

See [source-tree-analysis.md](./source-tree-analysis.md) for detailed annotated tree.

## Key Development Patterns

### Adding a New Feature

1. Create or modify a feature module under `feature/`
2. Define interfaces in `core/` if reusable
3. Add Koin bindings in the module's `di/` package
4. Register the Koin module in `TraceGlassApp.kt`
5. Add navigation route in `MainActivity.kt` if it's a new screen

### Adding a New Setting

1. Add field to `SettingsData` in `core:session`
2. Add setter to `SettingsRepository` interface
3. Implement in `DataStoreSettingsRepository` with DataStore key
4. Add toggle in `SettingsViewModel` / `SettingsScreen`
5. Consume in `TracingViewModel` via `settingsRepository.settingsData` flow

### Modifying Marker Detection

1. Native code: `core/cv/src/main/cpp/marker_detector_jni.cpp`
2. Data classes: `DetectedMarker.kt`, `MarkerResult.kt`
3. **Critical**: If adding fields to data classes, update JNI constructor call (no default params in JNI)
4. **Critical**: Always pass `rowStride` to `cv::Mat` constructor (handles YUV padding)

### Testing Conventions

- **Framework**: JUnit 5 (Jupiter) — use `@Nested` for test grouping
- **Coroutines**: Always use `runTest` + `advanceUntilIdle()` for coroutine testing
- **Fakes over mocks**: Prefer fake implementations (`FakeCameraManager`, `FakeMarkerDetector`, etc.)
- **State mutation**: Create fresh calculator/ViewModel per test iteration to avoid cascading state
- **android.graphics.Matrix**: Not available in JUnit — use `MatrixUtils` (pure Kotlin) instead

### Known Gotchas

| Issue | Impact | Mitigation |
|-------|--------|------------|
| `viewModelScope + while(true)` | `runTest` hangs forever | Cancel ViewModel or use test dispatcher |
| JNI + Kotlin default params | Crash at runtime | Pass ALL params explicitly in `NewObject` |
| YUV rowStride padding | Corrupted image data | Always use `rowStride` as Mat step parameter |
| CameraX multi-camera | Wrong focal length on Pixel 9 Pro | Auto-estimation from markers (Story 8-4 deferred) |
| `android.graphics.Matrix` in tests | `Method not mocked` exception | Use `MatrixUtils` for ViewModel code |

## CI/CD Pipeline

**GitHub Actions** (`.github/workflows/ci.yml`):

```
Trigger: push to main, pull requests
├── Validate Gradle wrapper (security)
├── Setup JDK 17 (Temurin)
├── Cache Gradle dependencies
├── Cache OpenCV SDK (SHA256 verified)
├── Build debug APK
├── Build release APK
├── Run unit tests
└── Run lint checks
```

## Release Build

```bash
# Build release APK (minified + shrunk)
./gradlew assembleRelease

# APK location
app/build/outputs/apk/release/app-release.apk
```

**ProGuard rules**:
- Koin DI classes kept (runtime name resolution)
- JNI-referenced classes kept (OpenCV data classes)
- Coroutines volatile fields kept

## Device Testing

### ArUco Markers

Print the marker sheet from `docs/aruco-markers-a4.pdf` (4 markers, DICT_4X4_50, IDs 0-3).

Place markers at the four corners of your drawing paper:
- ID 0 = Top-Left
- ID 1 = Top-Right
- ID 2 = Bottom-Right
- ID 3 = Bottom-Left

### Recommended Test Setup

1. Mount phone above paper (tripod, stand, or improvised setup)
2. Print and position 4 ArUco markers at paper corners
3. Launch app → complete onboarding → import reference image
4. Verify: markers detected (green indicator), overlay tracks paper position
5. Test perspective: tilt phone 15-20° and verify overlay follows

## String Resources

All user-facing strings are in `res/values/strings.xml` (i18n ready). Do not hardcode UI strings in Kotlin/Compose code.

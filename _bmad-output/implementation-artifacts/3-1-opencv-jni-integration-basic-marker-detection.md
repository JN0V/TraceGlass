# Story 3.1: OpenCV JNI Integration & Basic Marker Detection

Status: done

## Story

As a developer,
I want OpenCV integrated via JNI with a basic marker detection pipeline,
So that the app can detect fiducial markers in camera frames.

## Acceptance Criteria

1. **OpenCV SDK integration:** Given the project with CameraX from Epic 1, when the OpenCV Android SDK is integrated, then the `:core:cv` module contains a `MarkerDetector` interface and `OpenCvMarkerDetector` implementation.

2. **JNI native code:** JNI native code is in `:core:cv/src/main/cpp/` with CMakeLists.txt including reproducibility flags (`-ffile-prefix-map`, `--build-id=none`, `--hash-style=gnu`).

3. **Test double:** A `FakeMarkerDetector` exists for unit tests in `:core:cv/src/test/`.

4. **Tests pass:** `./gradlew :core:cv:test` passes with 7 new tests.

5. **CI passes:** CI pipeline still passes with NDK integration (CMake + OpenCV SDK download with cache).

6. **Detection contract:** Given a camera frame is available, when the frame is passed to `MarkerDetector.detect()`, then the method returns a `MarkerResult` with detected marker positions, corners, and confidence scores, and detection completes within 50ms (NFR2).

7. **Frame integration:** `FrameAnalyzer` implements `ImageAnalysis.Analyzer` to bridge CameraX frames to `MarkerDetector.detect()`, passing `rowStride` and `rotation` correctly.

## Tasks / Subtasks

- [x] Task 1: Create MarkerDetector interface and data classes (AC: #1, #6)
  - [x] 1.1 `MarkerDetector` interface with `detect(frameBuffer, width, height, rowStride, rotation): MarkerResult`
  - [x] 1.2 `MarkerResult` data class with `markers`, `detectionTimeMs`, `frameWidth`, `frameHeight`, `isTracking`, `markerCount`
  - [x] 1.3 `DetectedMarker` data class with `id`, `centerX`, `centerY`, `corners: List<Pair<Float, Float>>`, `confidence`
- [x] Task 2: Create OpenCvMarkerDetector JNI implementation (AC: #1, #2, #6)
  - [x] 2.1 `OpenCvMarkerDetector` class loading `traceglass_cv` native library
  - [x] 2.2 External `nativeDetect()` function delegating to C++
- [x] Task 3: Write C++ JNI code (AC: #2, #6)
  - [x] 3.1 `marker_detector_jni.cpp` with ArUco 4x4_50 dictionary detection
  - [x] 3.2 YUV Y-plane extraction with proper rowStride handling (`cv::Mat(h, w, CV_8UC1, buf, rowStride)`)
  - [x] 3.3 Rotation support (0, 90, 180, 270) via `cv::rotate`
  - [x] 3.4 JNI object construction: `DetectedMarker` with corners as `List<Pair<Float, Float>>`
  - [x] 3.5 Effective dimensions after rotation for `frameWidth`/`frameHeight`
  - [x] 3.6 Timing via `std::chrono` for `detectionTimeMs`
- [x] Task 4: CMakeLists.txt with F-Droid reproducibility (AC: #2)
  - [x] 4.1 `-ffile-prefix-map=${CMAKE_SOURCE_DIR}=.` for C and C++ flags
  - [x] 4.2 `-Wl,--build-id=none -Wl,--hash-style=gnu` for linker flags
  - [x] 4.3 C++17 standard, OpenCV SDK linked from `sdk/OpenCV-android-sdk/`
- [x] Task 5: build.gradle.kts for :core:cv (AC: #1, #5)
  - [x] 5.1 NDK version 27.2.12479018 pinned
  - [x] 5.2 ABI filters: arm64-v8a, armeabi-v7a, x86_64
  - [x] 5.3 `ANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON` for 16KB page size
  - [x] 5.4 CMake version 3.31.5
- [x] Task 6: FakeMarkerDetector for unit tests (AC: #3)
  - [x] 6.1 Configurable `resultToReturn` and `detectCallCount` tracking
- [x] Task 7: Unit tests (AC: #4)
  - [x] 7.1 MarkerResult: empty not tracking, with markers tracking, detection time recorded
  - [x] 7.2 FakeMarkerDetector: returns configured result, counts calls, default no markers
  - [x] 7.3 DetectedMarker: stores position, corners, confidence
- [x] Task 8: Koin DI wiring (AC: #1)
  - [x] 8.1 `CvModule.kt` with `single<MarkerDetector> { OpenCvMarkerDetector() }`
  - [x] 8.2 `cvModule` registered in `TraceGlassApp`
- [x] Task 9: CI pipeline update (AC: #5)
  - [x] 9.1 NDK/CMake install step in GitHub Actions
  - [x] 9.2 OpenCV SDK download with cache
- [x] Task 10: FrameAnalyzer CameraX bridge (AC: #7)
  - [x] 10.1 Implements `ImageAnalysis.Analyzer`
  - [x] 10.2 Extracts Y plane buffer with `planes[0].rowStride`
  - [x] 10.3 Passes `imageInfo.rotationDegrees` to detector
  - [x] 10.4 Emits `MarkerResult` via `StateFlow`
  - [x] 10.5 Diagnostic logging every 60 frames
  - [x] 10.6 Exception catch to prevent analysis pipeline crash

## Dev Notes

### Architecture

- **Module:** `:core:cv` — self-contained OpenCV integration with clean interface
- **JNI bridge:** Single native function `nativeDetect()` — all OpenCV calls stay in C++
- **ArUco dictionary:** `DICT_4X4_50` — 50 unique marker IDs, robust at small sizes
- **Frame pipeline:** CameraX ImageAnalysis → FrameAnalyzer → MarkerDetector → StateFlow → ViewModel

### Critical Implementation Details

- **YUV row stride:** `ImageProxy.planes[0].rowStride` may differ from width (e.g., 832 vs 800 at 800x600). MUST pass rowStride to native code and use `cv::Mat(h, w, type, buf, rowStride)`. Bug was dormant at 640x480 (no padding), exposed at 800x600.
- **JNI + Kotlin data class default params:** Kotlin data classes with default params generate synthetic constructors with bitmask. JNI MUST pass ALL params explicitly — no default param shortcuts.
- **Rotation handling:** CameraX `imageInfo.rotationDegrees` accounts for sensor orientation. Native code applies `cv::rotate` before detection so marker positions are in display coordinates.
- **Confidence:** Hardcoded to 1.0f in JNI (ArUco detection is binary — detected or not). Future: could use corner refinement quality.

### Project Structure Notes

- `core/cv/src/main/cpp/marker_detector_jni.cpp` — single C++ file, no headers needed
- `core/cv/src/main/cpp/CMakeLists.txt` — F-Droid reproducibility flags
- OpenCV SDK at `sdk/OpenCV-android-sdk/` — .gitignored, downloaded by CI
- `core/cv/.gitignore` should include `.cxx/` build directory

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 3.1]
- [Source: core/cv/src/main/kotlin — MarkerDetector, OpenCvMarkerDetector, MarkerResult]
- [Source: core/cv/src/main/cpp — marker_detector_jni.cpp, CMakeLists.txt]
- [Source: core/cv/src/test/kotlin — FakeMarkerDetector, MarkerDetectorTest]
- [Source: feature/tracing/src/main/kotlin — FrameAnalyzer]
- [Source: .github/workflows/ci.yml — NDK/CMake/OpenCV CI steps]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

### Completion Notes List

- MarkerDetector interface takes rowStride parameter (critical for YUV padding)
- OpenCvMarkerDetector loads `traceglass_cv` native library in companion init block
- JNI constructs DetectedMarker with ALL params (no Kotlin default param support in JNI)
- ArUco 4x4_50 dictionary chosen for robustness at small marker sizes
- FrameAnalyzer logs every 60 frames to avoid performance overhead
- CI caches OpenCV SDK download for faster builds
- 42 total tests passing after this story (7 new)
- ✅ Code review #1 (2026-02-18): Fixed 1H, 3M, 4L — ProGuard keep rules, JNI null checks, Mat copy perf, FrameAnalyzer tests, impl/ move, DetectedMarker split, .gitignore

### Change Log

- 2026-02-08: Implementation complete — commit c5acccb
- 2026-02-09: JNI constructor fix for new MarkerResult signature — commit 273ac29
- 2026-02-10: YUV row stride fix + wide-angle camera — commit 6f08369
- 2026-02-18: Code review #1 fixes — ProGuard JNI keep rules (H1), JNI null checks (M1), removed unnecessary Mat copy (M3), added 6 FrameAnalyzer tests (M2), moved OpenCvMarkerDetector to impl/ (L1), split DetectedMarker to own file (L2), LOGI→LOGE (L3), added .gitignore (L4)

### File List

**New files:**
- core/cv/build.gradle.kts
- core/cv/proguard-rules.pro (H1: JNI class keep rules for R8)
- core/cv/.gitignore (L4: exclude .cxx/ build dir)
- core/cv/src/main/cpp/CMakeLists.txt
- core/cv/src/main/cpp/marker_detector_jni.cpp
- core/cv/src/main/kotlin/io/github/jn0v/traceglass/core/cv/MarkerDetector.kt
- core/cv/src/main/kotlin/io/github/jn0v/traceglass/core/cv/MarkerResult.kt
- core/cv/src/main/kotlin/io/github/jn0v/traceglass/core/cv/DetectedMarker.kt (L2: split from MarkerResult.kt)
- core/cv/src/main/kotlin/io/github/jn0v/traceglass/core/cv/impl/OpenCvMarkerDetector.kt (L1: moved to impl/)
- core/cv/src/main/kotlin/io/github/jn0v/traceglass/core/cv/di/CvModule.kt
- core/cv/src/test/kotlin/io/github/jn0v/traceglass/core/cv/FakeMarkerDetector.kt
- core/cv/src/test/kotlin/io/github/jn0v/traceglass/core/cv/MarkerDetectorTest.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/FrameAnalyzer.kt
- feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/FrameAnalyzerTest.kt (M2: 6 new tests)

**Deleted files:**
- core/cv/src/main/kotlin/io/github/jn0v/traceglass/core/cv/OpenCvMarkerDetector.kt (L1: moved to impl/)

**Modified files:**
- .github/workflows/ci.yml (NDK/CMake install + OpenCV SDK cache)
- app/build.gradle.kts (cvModule dependency)
- app/src/main/kotlin/io/github/jn0v/traceglass/TraceGlassApp.kt (cvModule registration)
- core/cv/build.gradle.kts (H1: consumerProguardFiles added)
- core/cv/src/main/cpp/marker_detector_jni.cpp (M1: JNI null checks, M3: removed Mat copy, L3: LOGI→LOGE)
- core/cv/src/main/kotlin/io/github/jn0v/traceglass/core/cv/di/CvModule.kt (L1: updated import for impl/)

## Senior Developer Review (AI)

**Review Date:** 2026-02-18
**Reviewer Model:** Claude Opus 4.6
**Review Outcome:** Changes Requested → All Fixed

### Findings Summary

**Issues Found:** 1 High, 3 Medium, 4 Low — **All resolved**

### Action Items

- [x] **[HIGH] H1: Missing ProGuard keep rules for JNI-referenced classes** — `MarkerResult`, `DetectedMarker`, and `OpenCvMarkerDetector` are referenced by JNI via fully-qualified class names. R8 with `isMinifyEnabled = true` would obfuscate them, causing release build crashes. **Fix:** Created `core/cv/proguard-rules.pro` with `-keep` rules, added `consumerProguardFiles` to build.gradle.kts.
- [x] **[MED] M1: No null checks after JNI FindClass/GetMethodID** — All `FindClass()`/`GetMethodID()` calls could return null (e.g., ProGuard renaming). Code would segfault instead of giving descriptive error. **Fix:** Added `JNI_CHECK_NULL` macro that logs error and throws `RuntimeException`.
- [x] **[MED] M2: Zero test coverage for FrameAnalyzer** — FrameAnalyzer (Task 10) shipped without any tests, violating NFR18-25 TDD requirements. **Fix:** Added 6 tests: parameter passing, StateFlow update, initial state, error handling (image close + no state corruption), multi-frame processing.
- [x] **[MED] M3: Unnecessary Mat copy when rowStride != width** — `grayPadded.copyTo(gray)` was called when rowStride differed from width, but the Mat with rowStride as step parameter is valid for `detectMarkers()` and `cv::rotate()`. Wasted memory + latency. **Fix:** Removed conditional copy, use single `cv::Mat gray(h, w, type, buf, rowStride)`.
- [x] **[LOW] L1: OpenCvMarkerDetector not in `impl/` subdirectory** — Architecture requires `impl/` for concrete implementations. **Fix:** Moved to `core/cv/.../impl/OpenCvMarkerDetector.kt`, updated JNI function name and CvModule import.
- [x] **[LOW] L2: Two data classes in one file** — `MarkerResult` and `DetectedMarker` in same file violates "one public class per file" rule. **Fix:** Split `DetectedMarker` into its own file.
- [x] **[LOW] L3: C++ JNI uses LOGI for error case** — Architecture says never use `Log.i` in production. Null buffer error used `LOGI`. **Fix:** Replaced `LOGI` with `LOGE` and `LOGD` macros.
- [x] **[LOW] L4: Missing `core/cv/.gitignore`** — Dev Notes mention it should exist for `.cxx/` build directory. **Fix:** Created file.
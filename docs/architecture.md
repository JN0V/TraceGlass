# TraceGlass — Architecture Reference

> Generated: 2026-02-21 | Source: Deep scan of all modules

## 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        app (shell)                              │
│  MainActivity → NavHost (7 routes) ← TraceGlassApp (6 Koin)    │
└────────┬──────────────────┬──────────────────┬──────────────────┘
         │                  │                  │
┌────────▼────────┐ ┌──────▼───────┐ ┌────────▼────────┐
│ feature:tracing │ │ feature:     │ │ feature:        │
│  TracingVM      │ │ onboarding   │ │ timelapse       │
│  TracingScreen  │ │ 3 ViewModels │ │ 4 impls         │
│  FrameAnalyzer  │ │ 5 Screens    │ │ (codec,export)  │
└──┬──┬──┬──┬──┬──┘ └──┬──┬───────┘ └──┬──┬───────────┘
   │  │  │  │  │        │  │            │  │
┌──▼──▼──┘  │  │    ┌───▼──▼───┐    ┌───▼──▼───┐
│core:camera│  │    │core:     │    │core:     │
│CameraXMgr │  │    │session   │    │timelapse │
│Flashlight │  │    │DataStore │    │interfaces│
└───────────┘  │    └──────────┘    └──────────┘
           ┌───▼───────┐
           │core:overlay│
           │HomographyH │◄── core:cv
           │Transform   │    MarkerDetector
           │Tracking    │    OpenCV JNI
           └────────────┘
```

**Architecture Pattern**: MVVM + Clean Architecture (interface-based DI)

**Key Principles**:
- Core modules expose **interfaces**, feature modules consume them
- Single `CameraXManager` provides both `CameraManager` and `FlashlightController`
- `core:timelapse` contains only interfaces; `feature:timelapse` provides implementations
- All DI via Koin module definitions; no `@Inject` annotations

## 2. Dependency Injection Graph

```kotlin
// app/TraceGlassApp.kt — Module registration order
startKoin {
    modules(cameraModule, cvModule, sessionModule,
            onboardingModule, tracingModule, timelapseModule)
}
```

| Module | Bindings | Scope |
|--------|----------|-------|
| `cameraModule` | `CameraXManager → CameraManager + FlashlightController` | singleton |
| `cvModule` | `OpenCvMarkerDetector → MarkerDetector` | singleton |
| `sessionModule` | `DataStoreSessionRepository → SessionRepository` | singleton |
| | `DataStoreSettingsRepository → SettingsRepository` | singleton |
| `onboardingModule` | `DataStoreOnboardingRepository → OnboardingRepository` | singleton |
| | `WalkthroughAnalyzer` | singleton |
| | `OnboardingViewModel, WalkthroughViewModel, SetupGuideViewModel` | viewModel |
| `tracingModule` | `OverlayTransformCalculator` | singleton |
| | `FrameAnalyzer` | **factory** (new per request) |
| | `TimelapseOperations` | singleton |
| | `TracingViewModel` (7 deps), `SettingsViewModel` | viewModel |
| `timelapseModule` | `FileSnapshotStorage → SnapshotStorage` | singleton |
| | `MediaCodecCompiler → TimelapseCompiler` | singleton |
| | `MediaStoreVideoExporter → VideoExporter` | singleton |
| | `IntentVideoSharer → VideoSharer` | singleton |

**Notable**: `FrameAnalyzer` is a **factory** (new instance per request) because it holds mutable state (`latestResult`, `snapshotCallback`).

## 3. Data Flow — Camera Frame to Screen Render

```
Camera Sensor
    │
    ▼
CameraX ImageAnalysis (1280×720, STRATEGY_KEEP_ONLY_LATEST)
    │  runs on: analysisExecutor (single-threaded)
    ▼
FrameAnalyzer.analyze(ImageProxy)
    │  extracts Y-plane buffer + rowStride
    ▼
MarkerDetector.detect(buffer, w, h, rowStride, rotation)
    │  JNI → OpenCV ArUco DICT_4X4_50
    │  returns: MarkerResult(markers[], detectionTimeMs, frameW, frameH)
    ▼
FrameAnalyzer.latestResult (StateFlow<MarkerResult>)
    │  collected on: viewModelScope
    ▼
TracingViewModel.onMarkerResultReceived(result)
    │  1. TrackingStateManager.onMarkerResult() → TrackingStatus
    │  2. OverlayTransformCalculator.computeSmoothed() → OverlayTransform
    │  3. updateOverlayFromCombined() → renderMatrix: FloatArray(9)
    ▼
TracingScreen (Compose)
    │  drawWithContent { canvas.nativeCanvas.concat(Matrix(renderMatrix)) }
    ▼
Screen pixels
```

## 4. Overlay Transform Pipeline

### 4.1 OverlayTransformCalculator

**Purpose**: Convert detected marker positions into an overlay transformation matrix.

**State lifecycle**:
1. **Pre-reference** (0-3 markers first seen): Affine fallback (translate/rotate/scale from marker centroid)
2. **Reference established** (first 4-marker detection): Stores reference corners, computes paper aspect ratio
3. **Tracking** (subsequent frames): EMA smoothing + progressive degradation

**Progressive degradation**:

| Visible Markers | Strategy | Error Budget |
|----------------|----------|--------------|
| 4 | Full homography (DLT 4-point) | <0.1px |
| 3 | Constrained homography (Newton-Raphson) if f known, else affine delta | <5px at 20° tilt |
| 2 | Similarity transform (translation + rotation + scale) | <15px |
| 1 | Translation-only delta | <70px for 5° rotation |
| 0 | Hold last known position | 0px (frozen) |

**EMA smoothing**: `smooth[id] = lerp(smooth[id], detected[id], 0.12)` per-corner, applied before homography computation.

### 4.2 HomographySolver

**Pure Kotlin, no OpenCV dependency.**

| Method | Algorithm | Input | Output |
|--------|-----------|-------|--------|
| `solveHomography` | DLT (8×8 Gaussian elimination) | 4 src→dst point pairs | FloatArray(9) or null |
| `solveAffine` | 6×6 Gaussian elimination | 3 src→dst point pairs | FloatArray(9) or null |
| `estimateFocalLength` | Orthogonality + equal-norm constraints | paper+frame coords, cx, cy | Float? |
| `solveConstrainedHomography` | Newton-Raphson (2 quadratic constraints) | 3 points + f, cx, cy | FloatArray(9) or null |
| `correctAspectRatio` | Column norm ratio from H | paper+frame coords + f | Float? |

**Focal length auto-estimation**:
- Requires fronto-parallel reference (`isReferenceRectangular`: edge ratios within 8%)
- Uses H column orthogonality: `h₁ᵀ·ω·h₂ = 0` where `ω = K⁻ᵀK⁻¹`
- Equal-norm fallback: `|v₁|² = |v₂|²`
- Degenerate for near-square paper or horizontal-only tilt (H[6]≈0)

### 4.3 Coordinate Spaces

```
Frame Space (camera pixels, e.g. 1280×720)
    │  F2S matrix: pvScale + letterbox offset
    ▼
Screen Space (view pixels, e.g. 1080×1920)
    │  Homography: viewCorners → screenCorners
    ▼
View Space (centered overlay rect, paper AR)
    │  Manual transforms: rotate/scale before H
    ▼
Rendered Image
```

**Two rendering modes**:
- **Paper-corner mode** (`paperCornersFrame != null` + perspective enabled): `renderMatrix = mOffset × H × mView`
- **Affine mode**: `renderMatrix = translate × translate(center) × rotate × scale × translate(-center)`

## 5. Persistence Architecture

### 5.1 DataStore Instances

| DataStore | File | Repository | Fields |
|-----------|------|------------|--------|
| `session_prefs` | Session state | `SessionRepository` | 16 (URI, transform, timelapse, lock, viewport) |
| `settings_prefs` | User settings | `SettingsRepository` | 4 (audio, break, interval, perspective) |
| `onboarding_prefs` | Onboarding | `OnboardingRepository` | 3 (completed, tooltip, tier) |

### 5.2 Session Save Strategy

- **Trigger**: Any state change (opacity, position, timelapse, etc.)
- **Debounce**: 500ms (prevents excessive writes during gesture sequences)
- **Context**: `NonCancellable` (survives ViewModel clearing)
- **Restore**: Dialog on cold start if `hasSavedOverlay == true`
- **Image**: Copied to internal storage (`filesDir/reference_image.*`) for URI stability

### 5.3 Schema Versioning

`DataStoreSessionRepository` includes `SCHEMA_VERSION = 1`. On incompatible version, session data is silently discarded and defaults returned.

## 6. Camera Architecture

### 6.1 CameraXManager

**Dual-interface singleton**: Implements `CameraManager` (lifecycle) + `FlashlightController` (torch).

**Camera selection** (`buildWideAngleSelector`):
1. Enumerate back-facing logical cameras
2. Extract focal lengths via Camera2 interop
3. Select camera with **minimum focal length** (widest FOV)
4. Apply `setWidestZoom(minZoomRatio)` for ultra-wide (e.g. 0.5× on Pixel 9 Pro)

**Lifecycle**:
- `bindPreview()`: Binds preview + optional ImageAnalysis to lifecycle
- `unbind()`: Releases camera, keeps executor alive (allows rebind)
- `close()`: Shuts down executor, idempotent, prevents future binds

**Threading**:
- `analysisExecutor`: Single-threaded `ExecutorService` for frame processing
- State flows emitted on main thread
- Executor survives `unbind()`, destroyed only in `close()`

### 6.2 FrameAnalyzer

**`ImageAnalysis.Analyzer` implementation** in `feature:tracing`.

- Extracts Y-plane `ByteBuffer` + `rowStride` from `ImageProxy`
- Calls `MarkerDetector.detect()` with rotation from `imageProxy.imageInfo.rotationDegrees`
- Emits `MarkerResult` via `latestResult: StateFlow`
- **Snapshot capture**: One-shot `AtomicReference<(ByteArray) -> Unit>` callback, encodes to JPEG (quality 85) on IO dispatcher

## 7. Native/JNI Architecture

### 7.1 OpenCV Integration

```
libtraceglass_cv.so
    ├── JNI_OnLoad: Cache class/method refs (ArrayList, Pair, Float, DetectedMarker, MarkerResult)
    ├── nativeDetect: Buffer validation → Mat(h,w,CV_8UC1,buf,rowStride) → rotate → detectMarkers → build Java objects
    └── JNI_OnUnload: Release global refs
```

**ArUco config**: `DICT_4X4_50` (50 markers, 4×4 bit grid), default `DetectorParameters`

**Security hardening** (CMake):
- `-fstack-protector-strong`, `-D_FORTIFY_SOURCE=2`
- `-Wl,-z,relro -Wl,-z,now` (GOT read-only)
- `-ffile-prefix-map` + `--build-id=none` (F-Droid reproducible builds)

**ProGuard rules**: Keep `MarkerResult`, `DetectedMarker`, `OpenCvMarkerDetector` native methods (JNI uses `FindClass` with FQN).

## 8. Navigation Architecture

**Single-Activity** with Jetpack Compose `NavHost`:

```
                    ┌──────────────────────────┐
                    │     Start Destination     │
                    │  (onboarding or tracing)  │
                    └──────────┬───────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
   onboarding            walkthrough              tracing
   (3-page pager)     (camera + markers)      (main screen)
        │                      │                 │    │    │
        ▼                      ▼              settings │ about
   walkthrough              tracing          setup-guide │
                                           onboarding-reopen
```

**Start destination logic**:
- First launch → `onboarding`
- Subsequent → `tracing`
- Determined by `OnboardingRepository.isOnboardingCompleted` (DataStore)
- Loading spinner shown while DataStore initializes (no `runBlocking`)

## 9. Timelapse Pipeline

```
TimelapseSession (state machine: IDLE→RECORDING→PAUSED)
    │  periodic onCapture(index) every 5s
    ▼
FrameAnalyzer.snapshotCallback (one-shot, AtomicReference)
    │  captures camera frame + overlay as JPEG
    ▼
FileSnapshotStorage.saveSnapshot(jpegBytes, index)
    │  writes to filesDir/timelapse/snapshot_NNNN.jpg
    ▼
MediaCodecCompiler.compile(files, outputFile, fps=10)
    │  H.264 encoding via MediaCodec + MediaMuxer
    │  16-aligned resolution, 4Mbps, skips corrupt frames
    ▼
MediaStoreVideoExporter.exportToGallery(videoFile, displayName)
    │  Movies/TraceGlass/ via MediaStore, IS_PENDING atomic write
    ▼
IntentVideoSharer.shareVideo(uri)
    │  ACTION_SEND, video/mp4, chooser dialog
```

## 10. Testing Architecture

### 10.1 Framework

| Tool | Purpose |
|------|---------|
| JUnit 5 (Jupiter) | Test runner, `@Nested`, `@BeforeEach`/`@AfterEach` |
| MockK | Mocking (used sparingly — prefer fakes) |
| kotlinx.coroutines.test | `runTest`, `TestDispatcher`, `advanceUntilIdle` |
| Compose UI Test | `createComposeRule`, UI assertion |

### 10.2 Test Doubles

| Fake | Module | Purpose |
|------|--------|---------|
| `FakeCameraManager` | core:camera | Instant bind, no real camera |
| `FakeFlashlightController` | core:camera | Configurable hasFlashlight |
| `FakeMarkerDetector` | core:cv | Returns configured results, counts calls |
| `FakeSessionRepository` | core:session | In-memory DataStore, tracks save/clear counts |

### 10.3 Test Distribution

| Module | Test Files | Notable |
|--------|-----------|---------|
| core:camera | 1 (4 tests) | Lifecycle/idempotency |
| core:cv | 1 (6 tests) | Data class validation |
| core:overlay | 4 (~80 tests) | Algorithmic correctness, corner estimation |
| core:session | 3 (23 tests) | Schema sync, persistence |
| feature:tracing | 6 (~100+ tests) | ViewModel state machine, gestures, timelapse |
| feature:onboarding | 4 (~20 tests) | Page navigation, walkthrough flow |
| feature:timelapse | 5 (~30 tests) | Storage, compilation, DI |

### 10.4 Known Testing Patterns

- **viewModelScope + runTest hang**: Infinite loops in viewModelScope prevent runTest from completing. Must cancel ViewModel or use test-injectable dispatchers.
- **State mutation trap**: When testing hidden corners in a loop, create fresh calculator per iteration to avoid cascading errors from mutated smoothedCorners.
- **android.graphics.Matrix not available**: Use `MatrixUtils` (pure Kotlin) instead of `android.graphics.Matrix` in ViewModel unit tests.

## 11. Build Architecture

### 11.1 Build Variants

| Variant | Minify | Shrink Resources | ProGuard |
|---------|--------|-------------------|----------|
| debug | No | No | No |
| release | Yes | Yes | optimize.txt + custom rules |

### 11.2 ABI Support

| ABI | Device Type |
|-----|-------------|
| arm64-v8a | Modern phones (Pixel, Samsung, OnePlus) |
| armeabi-v7a | Older 32-bit ARM devices |
| x86_64 | Emulators (Intel/AMD) |

### 11.3 CI/CD

GitHub Actions (`ci.yml`):
- Trigger: push to main, PRs
- Runner: Ubuntu latest, JDK 17 (Temurin)
- Steps: Gradle wrapper validation → cache → debug build → release build → unit tests → lint
- Caches: Gradle, OpenCV SDK (SHA256 verified)

## 12. Security Measures

| Measure | Location |
|---------|----------|
| Cleartext HTTP blocked | `network_security_config.xml` |
| Stack protector | CMake: `-fstack-protector-strong` |
| FORTIFY_SOURCE | CMake: `-D_FORTIFY_SOURCE=2` |
| RELRO + NOW | CMake: `-Wl,-z,relro -Wl,-z,now` |
| ProGuard/R8 | Release builds: minification + shrinking |
| JNI class keep | `proguard-rules.pro` in core:cv |
| No backup | `android:allowBackup="false"` |
| Buffer validation | JNI: capacity check before Mat construction |

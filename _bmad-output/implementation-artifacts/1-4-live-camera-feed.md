# Story 1.4: Live Camera Feed

Status: done

## Story

As a user,
I want to see a live camera feed filling my screen when I open the app,
so that I can see my drawing surface through the phone.

## Acceptance Criteria

1. **Given** the app is launched and camera permission is not yet granted **When** the app requests camera permission **Then** the system permission dialog is shown with a rationale
2. **Given** camera permission is requested **When** the user grants it **Then** the rear camera feed fills the screen in real-time
3. **Given** camera permission is requested **When** the user denies it **Then** an explanatory screen is shown with a "Grant Permission" button that re-triggers the request
4. **Given** camera permission is already granted **When** the app launches **Then** the rear camera feed is displayed immediately without any dialog
5. **Given** camera is active **When** the feed runs **Then** it runs at the device's native preview frame rate
6. **Given** camera is active **When** the device orientation changes **Then** both portrait and landscape orientations are supported

## Tasks / Subtasks

- [x] Task 1: Create CameraManager interface and CameraX implementation (AC: #2, #4, #5)
  - [x] 1.1: `CameraManager` interface with `bindPreview()`, `unbind()`, `reapplyZoom()`
  - [x] 1.2: `CameraXManager` implementation using CameraX Preview + ImageAnalysis use cases
  - [x] 1.3: Rear camera (back-facing) as default
  - [x] 1.4: Lifecycle-aware binding with `ProcessCameraProvider`
  - [x] 1.5: Wide-angle camera selection via Camera2Interop (smallest focal length)
  - [x] 1.6: `setZoomRatio(minZoomRatio)` for widest FOV on multi-cam devices
- [x] Task 2: Implement camera permission flow (AC: #1, #2, #3)
  - [x] 2.1: `CAMERA` permission in AndroidManifest.xml + `uses-feature` with `required="false"`
  - [x] 2.2: `ActivityResultContracts.RequestPermission()` in TracingScreen
  - [x] 2.3: `PermissionState` enum: `NOT_REQUESTED`, `GRANTED`, `DENIED`
  - [x] 2.4: Permission denied → `PermissionDeniedContent()` with rationale text + "Grant Permission" button
  - [x] 2.5: ViewModel handles `onPermissionResult(granted: Boolean)`
- [x] Task 3: Create Tracing Screen with camera preview (AC: #2, #4, #6)
  - [x] 3.1: `TracingScreen` composable with `koinViewModel()`
  - [x] 3.2: `PreviewView` via `AndroidView` in Compose, fullscreen
  - [x] 3.3: Camera preview fills entire screen
  - [x] 3.4: Portrait and landscape supported
- [x] Task 4: Set up Koin DI for camera module (AC: #2)
  - [x] 4.1: `CameraModule.kt` — `single { CameraXManager(get()) }`, `single<CameraManager> { get<CameraXManager>() }`
  - [x] 4.2: Wired in TraceGlassApp.kt `startKoin`
- [x] Task 5: Create FakeFlashlightController for testing
  - [x] 5.1: `FakeFlashlightController` in core/camera/src/test/
- [x] Task 6: Navigation setup (AC: #4)
  - [x] 6.1: Compose Navigation in MainActivity — routes to Onboarding or Tracing based on DataStore flag
  - [x] 6.2: Tracing screen as default destination when onboarding completed
- [x] Task 7: Frame analysis pipeline (future stories prep)
  - [x] 7.1: `FrameAnalyzer` implements `ImageAnalysis.Analyzer` — passes frames to MarkerDetector
  - [x] 7.2: Resolution target 1280x720, `STRATEGY_KEEP_ONLY_LATEST` backpressure
  - [x] 7.3: Single-threaded executor for analysis
- [x] Task 8: Lifecycle management
  - [x] 8.1: `ON_STOP` → save session, `ON_RESUME` → reapplyZoom
  - [x] 8.2: `onDispose` → `cameraManager.unbind()`

## Dev Notes

### Actual Implementation Architecture

**CameraManager interface:**
```kotlin
interface CameraManager {
    fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        imageAnalyzer: ImageAnalysis.Analyzer? = null
    )
    fun unbind()
    fun reapplyZoom()
}
```

**Wide-angle camera selection:** CameraXManager uses Camera2Interop to inspect `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` and selects the camera with the smallest focal length. Falls back to `CameraSelector.DEFAULT_BACK_CAMERA`. On Pixel 9 Pro, `setZoomRatio(minZoomRatio)` activates ultra-wide.

**Permission state machine:** TracingScreen uses `PermissionState` enum driven by ViewModel. Permission requested on `NOT_REQUESTED` via `LaunchedEffect`. Denied state shows `PermissionDeniedContent` with retry button.

**Session resume:** On cold start, if a saved session exists, an `AlertDialog` offers to resume with same settings.

### References

- [Source: architecture.md#Decision 8] — Camera Pipeline Architecture
- [Source: architecture.md#Decision 1] — MVVM pattern
- [Source: architecture.md#Compose Pattern] — Screen/Content separation
- [Source: prd.md#Device Permissions] — CAMERA only
- [Source: prd.md#NFR1-3] — Performance requirements

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

- CameraX integration with Preview + ImageAnalysis use cases
- Wide-angle auto-selection via Camera2Interop focal length inspection
- Permission flow with 3 states: NOT_REQUESTED, GRANTED, DENIED
- Frame analysis pipeline ready for marker detection (1280x720, backpressure)
- Lifecycle management: session save on stop, zoom reapply on resume, unbind on dispose
- Resume dialog on cold start for session persistence
- ✅ Resolved review finding [HIGH]: Replaced `runBlocking` in MainActivity with `collectAsStateWithLifecycle(initialValue = null)` — async DataStore read, no main thread blocking
- ✅ Resolved review finding [MEDIUM]: Split TracingScreen.kt (148 lines stateful) + TracingContent.kt (stateless composables) per architecture pattern
- ✅ Resolved review finding [MEDIUM]: Extracted `copyImageToInternal` to `ImageFileHelper` object with proper `Log.w` error logging
- ✅ Resolved review finding [HIGH]: Increased VisualModeControls top padding from 72dp to 80dp — ensures 16dp clearance below Settings FAB (48dp + 16dp top padding = 64dp), eliminating visual overlap
- ✅ Resolved review finding [MEDIUM]: Renamed `CameraPreviewContent()` → `TracingContent()` in TracingContent.kt and TracingScreen.kt per architecture Compose Pattern (Screen/Content separation)
- ✅ Resolved review finding [MEDIUM]: Changed `Log.i` → `Log.d` in `logSelectedCamera()` and `setWidestZoom()` per architecture Logging Pattern (no Log.i/Log.v in production)
- ✅ Resolved review finding [LOW]: Added `@Preview(showBackground = true)` annotation to `PermissionDeniedContent` with default parameter for previewability

### Review Follow-ups (AI)

- [x] [AI-Review][HIGH] `runBlocking` in MainActivity.onCreate() blocks main thread reading DataStore — risk of ANR, violates NFR3 (<3s cold start). Replace with SplashScreen or coroutine-based loading state [app/.../MainActivity.kt:33]
- [x] [AI-Review][MEDIUM] TracingScreen.kt not split into Screen/Content per architecture pattern — architecture doc prescribes separate TracingContent.kt (stateless, previewable). Current file has both in ~480 lines [feature/tracing/.../TracingScreen.kt]
- [x] [AI-Review][MEDIUM] `copyImageToInternal` utility function embedded in composable file — business logic (file I/O) in UI layer, `catch (_: Exception)` silently swallows all errors. Move to repository/utility class [feature/tracing/.../TracingScreen.kt:80-89]
- [x] [AI-Review][HIGH] Settings FAB and VisualModeControls both positioned at `Alignment.TopStart` with identical padding — visual overlap when overlay image is loaded. Move Settings FAB to a non-conflicting position or integrate into ExpandableMenu [feature/tracing/.../TracingContent.kt:204,240]
- [x] [AI-Review][MEDIUM] Architecture naming deviation — composable named `CameraPreviewContent()` should be `TracingContent()` per architecture Compose Pattern (Screen/Content separation) [feature/tracing/.../TracingContent.kt:58]
- [x] [AI-Review][MEDIUM] `Log.i` used in production code — architecture Logging Pattern mandates "NEVER Log.i or Log.v in production code". Change to `Log.d` in `logSelectedCamera()` and `setWidestZoom()` [core/camera/.../CameraXManager.kt:170,185]
- [x] [AI-Review][LOW] No `@Preview` annotations on Content composables — architecture says Content composables should be previewable. Add `@Preview` to `PermissionDeniedContent` at minimum [feature/tracing/.../TracingContent.kt]

### File List

- `core/camera/src/main/kotlin/io/github/jn0v/traceglass/core/camera/CameraManager.kt`
- `core/camera/src/main/kotlin/io/github/jn0v/traceglass/core/camera/impl/CameraXManager.kt`
- `core/camera/src/main/kotlin/io/github/jn0v/traceglass/core/camera/di/CameraModule.kt`
- `core/camera/src/main/kotlin/io/github/jn0v/traceglass/core/camera/FlashlightController.kt`
- `core/camera/src/test/kotlin/io/github/jn0v/traceglass/core/camera/FakeFlashlightController.kt`
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingScreen.kt`
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt`
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingUiState.kt`
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/FrameAnalyzer.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/io/github/jn0v/traceglass/MainActivity.kt`
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingContent.kt`
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/ImageFileHelper.kt`

### Change Log

- 2026-02-17: Addressed code review findings — 4 items resolved (1 HIGH, 2 MEDIUM, 1 LOW): fixed TopStart overlap spacing, renamed CameraPreviewContent→TracingContent, Log.i→Log.d, added @Preview to PermissionDeniedContent
- 2026-02-17: Code review #2 passed — 4 additional fixes (1M, 3L): FrameAnalyzer catch block, AudioFeedbackPlayer cached, ImageFileHelper file extension, TracingUiState imports cleanup. 96 tests pass.

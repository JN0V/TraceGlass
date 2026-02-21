# TraceGlass — Component Inventory

> Generated: 2026-02-21

## Interfaces

| Interface | Module | Methods | Purpose |
|-----------|--------|---------|---------|
| `CameraManager` | core:camera | `bindPreview`, `unbind`, `reapplyZoom`, `close` | Camera lifecycle abstraction |
| `FlashlightController` | core:camera | `toggleTorch` | Torch control |
| `MarkerDetector` | core:cv | `detect(buffer, w, h, rowStride, rotation)` | ArUco marker detection |
| `SessionRepository` | core:session | `save`, `clear` + `sessionData` flow | Session persistence |
| `SettingsRepository` | core:session | 4 individual setters + `settingsData` flow | User settings |
| `OnboardingRepository` | feature:onboarding | `setCompleted`, `setTooltipShown`, `setTier`, `reset` | Onboarding state |
| `SnapshotStorage` | core:timelapse | `saveSnapshot`, `getSnapshotFiles`, `clear` | JPEG snapshot files |
| `TimelapseCompiler` | core:timelapse | `compile(files, output, fps)` | Video encoding |
| `VideoExporter` | core:timelapse | `exportToGallery(file, name)` | MediaStore export |
| `VideoSharer` | core:timelapse | `shareVideo(uri)` | Android share intent |

## Data Classes

| Class | Module | Fields | Purpose |
|-------|--------|--------|---------|
| `DetectedMarker` | core:cv | `id`, `centerX/Y`, `corners[4]`, `confidence` | Single detected ArUco marker |
| `MarkerResult` | core:cv | `markers[]`, `detectionTimeMs`, `frameW/H` | Detection frame result |
| `OverlayTransform` | core:overlay | `offsetX/Y`, `scale`, `rotation`, `paperCornersFrame`, `paperAR` | Transform output |
| `SessionData` | core:session | 16 fields (URI, transform, timelapse, lock, viewport) | Persisted session state |
| `SettingsData` | core:session | `audioFeedback`, `breakReminder`, `interval`, `perspective` | Persisted settings |
| `TracingUiState` | feature:tracing | 50 fields | Full UI state snapshot |
| `SettingsUiState` | feature:tracing | 4 fields | Settings screen state |
| `TimelapseState` | core:timelapse | Enum: `IDLE`, `RECORDING`, `PAUSED` | Recording state machine |
| `CompilationResult` | core:timelapse | Sealed: `Success(file, skipped)`, `Error(msg)` | Compile outcome |
| `ExportResult` | core:timelapse | Sealed: `Success(uri)`, `Error(msg)` | Export outcome |

## Implementations

| Class | Module | Implements | Key Details |
|-------|--------|------------|-------------|
| `CameraXManager` | core:camera | `CameraManager` + `FlashlightController` | Wide-angle selector, zoom, single executor |
| `OpenCvMarkerDetector` | core:cv | `MarkerDetector` | JNI → `libtraceglass_cv.so` → OpenCV ArUco |
| `OverlayTransformCalculator` | core:overlay | — | EMA smoothing, progressive degradation, focal auto-estimation |
| `HomographySolver` | core:overlay | — | Pure Kotlin DLT, constrained H, Newton-Raphson |
| `MatrixUtils` | core:overlay | — | 3×3 matrix ops (identity, translate, scale, rotate, multiply) |
| `TrackingStateManager` | core:overlay | — | INACTIVE→TRACKING→LOST (500ms grace) |
| `DataStoreSessionRepository` | core:session | `SessionRepository` | Schema v1, selective key clear |
| `DataStoreSettingsRepository` | core:session | `SettingsRepository` | IOException fallback |
| `DataStoreOnboardingRepository` | feature:onboarding | `OnboardingRepository` | Tier selection persistence |
| `TimelapseSession` | core:timelapse | — | Coroutine-based capture loop, state machine |
| `FileSnapshotStorage` | feature:timelapse | `SnapshotStorage` | `filesDir/timelapse/snapshot_%04d.jpg` |
| `MediaCodecCompiler` | feature:timelapse | `TimelapseCompiler` | H.264, 4Mbps, 16-aligned, Surface encoding |
| `MediaStoreVideoExporter` | feature:timelapse | `VideoExporter` | `Movies/TraceGlass/`, IS_PENDING atomic |
| `IntentVideoSharer` | feature:timelapse | `VideoSharer` | ACTION_SEND chooser |

## ViewModels

| ViewModel | Module | Dependencies | State |
|-----------|--------|-------------|-------|
| `TracingViewModel` | feature:tracing | 7 (flashlight, transform, session, settings, camera, frameAnalyzer, timelapseOps) | `TracingUiState` + `renderMatrix` |
| `SettingsViewModel` | feature:tracing | 1 (settingsRepository) | `SettingsUiState` |
| `OnboardingViewModel` | feature:onboarding | 1 (onboardingRepository) | page, tier, completed |
| `WalkthroughViewModel` | feature:onboarding | 2 (onboardingRepo, sessionRepo) | step, timer, URI |
| `SetupGuideViewModel` | feature:onboarding | 0 | selectedSection |

## Compose Screens

| Screen | Module | Route | Key Components |
|--------|--------|-------|----------------|
| `TracingScreen` | feature:tracing | `tracing` | Camera preview, overlay image, gesture layer, tracking indicator |
| `SettingsScreen` | feature:tracing | `settings` | Toggle switches, break interval slider |
| `OnboardingScreen` | feature:onboarding | `onboarding` | HorizontalPager (3 pages), skip/next buttons |
| `WalkthroughScreen` | feature:onboarding | `walkthrough` | Camera preview, step overlays, photo picker |
| `SetupGuideScreen` | feature:onboarding | `setup-guide` | Section selector, step cards, download links |
| `AboutScreen` | feature:tracing | `about` | App version, licenses |

## Compose UI Components

| Component | Module | Purpose |
|-----------|--------|---------|
| `OpacityFab` | feature:tracing | Vertical slider, auto-collapse 3s, percentage badge |
| `VisualModeControls` | feature:tracing | FilterChip row (5 tints) + invert toggle |
| `TrackingIndicator` | feature:tracing | Green/orange status dot with label |
| `LockButton` | feature:tracing | Lock/unlock overlay, teal when locked |
| `ExpandableMenu` | feature:tracing | Bottom-left FAB with staggered expand animation |
| `OnboardingPages` | feature:onboarding | Welcome, TierSelection, MarkerPreparation pages |
| `OnboardingOverlay` | feature:onboarding | DetectingOverlay, MarkersFoundOverlay, TooltipOverlay |

## Utility Classes

| Class | Module | Purpose |
|-------|--------|---------|
| `FrameAnalyzer` | feature:tracing | `ImageAnalysis.Analyzer` + snapshot capture (JPEG 85) |
| `WalkthroughAnalyzer` | feature:onboarding | Simplified marker detector for onboarding |
| `AudioFeedbackPlayer` | feature:tracing | DTMF tones for tracking gained/lost, break reminder |
| `ImageFileHelper` | feature:tracing | Copy picker URI to internal storage (atomic rename) |
| `TimelapseOperations` | feature:tracing | Groups 5 timelapse dependencies for ViewModel injection |

## Test Doubles

| Fake | Module | For |
|------|--------|-----|
| `FakeCameraManager` | core:camera/test | `CameraManager` (instant bind) |
| `FakeFlashlightController` | core:camera/test | `FlashlightController` (configurable hasFlash) |
| `FakeMarkerDetector` | core:cv/test | `MarkerDetector` (returns configured result) |
| `FakeSessionRepository` | core:session/test | `SessionRepository` (in-memory, tracks counts) |

## Enums

| Enum | Module | Values |
|------|--------|--------|
| `ColorTint` | feature:tracing | `NONE`, `RED`, `GREEN`, `BLUE`, `GRAYSCALE` |
| `TrackingState` | feature:tracing | `INACTIVE`, `TRACKING`, `LOST` |
| `PermissionState` | feature:tracing | `NOT_REQUESTED`, `GRANTED`, `DENIED` |
| `TrackingStatus` | core:overlay | `INACTIVE`, `TRACKING`, `LOST` |
| `TimelapseState` | core:timelapse | `IDLE`, `RECORDING`, `PAUSED` |
| `SetupGuideSection` | feature:onboarding | `MARKER_GUIDE`, `MACGYVER_GUIDE` |
| `SetupTier` | feature:onboarding | `FULL_DIY`, `SEMI_EQUIPPED`, `FULL_KIT` |

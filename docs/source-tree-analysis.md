# TraceGlass — Source Tree Analysis

> Generated: 2026-02-21

## Project Root

```
TraceGlass/
├── app/                              # Application shell
│   ├── src/main/
│   │   ├── kotlin/.../
│   │   │   ├── MainActivity.kt       # Single-activity, Compose NavHost (7 routes)
│   │   │   └── TraceGlassApp.kt      # Application class, Koin DI bootstrap (6 modules)
│   │   ├── AndroidManifest.xml        # CAMERA permission, network security config
│   │   └── res/xml/
│   │       └── network_security_config.xml  # Blocks cleartext HTTP
│   └── proguard-rules.pro            # Koin + coroutines keep rules
│
├── core/
│   ├── camera/                       # CameraX abstraction layer
│   │   ├── src/main/kotlin/.../
│   │   │   ├── CameraManager.kt          # Interface: bind/unbind/reapplyZoom
│   │   │   ├── FlashlightController.kt   # Interface: toggleTorch/isTorchOn
│   │   │   ├── impl/
│   │   │   │   └── CameraXManager.kt     # Implements both interfaces
│   │   │   │                              # Wide-angle selector, zoom management
│   │   │   └── di/
│   │   │       └── CameraModule.kt       # Koin: single CameraXManager → 2 interfaces
│   │   └── src/test/kotlin/.../
│   │       ├── FakeCameraManager.kt       # Test double
│   │       ├── FakeFlashlightController.kt
│   │       └── CameraXManagerCloseTest.kt # Lifecycle tests (4 tests)
│   │
│   ├── cv/                           # Computer vision (OpenCV JNI)
│   │   ├── src/main/
│   │   │   ├── kotlin/.../
│   │   │   │   ├── MarkerDetector.kt      # Interface: detect(buffer, w, h, rowStride, rotation)
│   │   │   │   ├── DetectedMarker.kt      # Data: id, center, corners[4], confidence
│   │   │   │   ├── MarkerResult.kt        # Data: markers[], detectionTimeMs, frameW/H
│   │   │   │   ├── impl/
│   │   │   │   │   └── OpenCvMarkerDetector.kt  # JNI bridge → libtraceglass_cv.so
│   │   │   │   └── di/
│   │   │   │       └── CvModule.kt        # Koin: single MarkerDetector
│   │   │   └── cpp/
│   │   │       ├── CMakeLists.txt         # OpenCV linking, security flags, F-Droid repro
│   │   │       └── marker_detector_jni.cpp # ArUco DICT_4X4_50, cached JNI refs
│   │   │                                  # ABIs: arm64-v8a, armeabi-v7a, x86_64
│   │   ├── src/test/kotlin/.../
│   │   │   ├── MarkerDetectorTest.kt      # Data class tests
│   │   │   └── FakeMarkerDetector.kt      # Test double with call counter
│   │   └── proguard-rules.pro             # Keep JNI-referenced classes
│   │
│   ├── overlay/                      # Overlay math engine
│   │   ├── src/main/kotlin/.../
│   │   │   ├── OverlayTransformCalculator.kt  # Core: marker tracking + homography pipeline
│   │   │   │                                   # Progressive degradation 4→3→2→1→0
│   │   │   │                                   # EMA smoothing, focal length auto-estimation
│   │   │   ├── HomographySolver.kt        # Pure Kotlin DLT, constrained H, f estimation
│   │   │   ├── MatrixUtils.kt             # 3x3 matrix ops (avoids android.graphics.Matrix)
│   │   │   ├── OverlayTransform.kt        # Data: offset/scale/rotation OR paperCornersFrame
│   │   │   └── TrackingStateManager.kt    # State machine: INACTIVE→TRACKING→LOST (500ms grace)
│   │   └── src/test/kotlin/.../
│   │       ├── OverlayTransformCalculatorTest.kt  # 974 lines, corner estimation tests
│   │       ├── HomographySolverTest.kt            # 546 lines, DLT/constrained H tests
│   │       ├── MatrixUtilsTest.kt                 # 308 lines, matrix operation tests
│   │       └── TrackingStateManagerTest.kt        # 154 lines, state transition tests
│   │
│   ├── session/                      # Data persistence
│   │   ├── src/main/kotlin/.../
│   │   │   ├── SessionRepository.kt       # Interface: save/clear SessionData
│   │   │   ├── SettingsRepository.kt      # Interface: per-setting setters
│   │   │   ├── SessionData.kt             # 16 fields (URI, transform, timelapse, lock, viewport)
│   │   │   ├── SettingsData.kt            # 4 fields (audio, break, interval, perspective)
│   │   │   ├── DataStoreSessionRepository.kt   # DataStore impl with schema versioning
│   │   │   ├── DataStoreSettingsRepository.kt  # DataStore impl
│   │   │   └── di/
│   │   │       └── SessionModule.kt       # Koin: 2 DataStores → 2 repositories
│   │   └── src/test/kotlin/.../
│   │       ├── SessionRepositoryTest.kt           # 10 tests
│   │       ├── DataStoreSessionRepositoryTest.kt  # 13 tests (schema sync validation)
│   │       └── FakeSessionRepository.kt           # Test double
│   │
│   └── timelapse/                    # Timelapse interfaces (pure Kotlin)
│       └── src/main/kotlin/.../
│           ├── SnapshotStorage.kt         # Interface: save/get/clear JPEG snapshots
│           ├── TimelapseCompiler.kt       # Interface: compile(files) → CompilationResult
│           ├── VideoExporter.kt           # Interface: exportToGallery() → ExportResult
│           ├── VideoSharer.kt             # Interface: shareVideo(uri)
│           ├── TimelapseSession.kt        # State machine: IDLE→RECORDING→PAUSED
│           └── TimelapseState.kt          # Enum: IDLE/RECORDING/PAUSED
│
├── feature/
│   ├── tracing/                      # Main tracing feature
│   │   ├── src/main/kotlin/.../
│   │   │   ├── TracingViewModel.kt        # 844 lines — orchestrates everything
│   │   │   │                               # Camera, tracking, overlay, gestures, timelapse, session
│   │   │   ├── TracingUiState.kt          # 50 fields
│   │   │   ├── TracingScreen.kt           # Compose: camera preview + overlay + controls
│   │   │   ├── FrameAnalyzer.kt           # ImageAnalysis.Analyzer + snapshot capture
│   │   │   ├── AudioFeedbackPlayer.kt     # DTMF tones for tracking gained/lost
│   │   │   ├── ImageFileHelper.kt         # Copy picker URI to internal storage
│   │   │   ├── TimelapseOperations.kt     # Groups 5 timelapse dependencies
│   │   │   ├── components/
│   │   │   │   ├── OpacityFab.kt          # Vertical slider with auto-collapse
│   │   │   │   ├── VisualModeControls.kt  # Color tint chips + invert toggle
│   │   │   │   ├── TrackingIndicator.kt   # Green/orange status badge
│   │   │   │   ├── LockButton.kt          # Lock/unlock overlay transform
│   │   │   │   └── ExpandableMenu.kt      # Bottom-left expanding FAB menu
│   │   │   ├── settings/
│   │   │   │   ├── SettingsViewModel.kt   # Audio, break, perspective toggles
│   │   │   │   ├── SettingsUiState.kt
│   │   │   │   └── SettingsScreen.kt
│   │   │   └── di/
│   │   │       └── TracingModule.kt       # Koin: ViewModel (7 deps), FrameAnalyzer (factory)
│   │   └── src/test/kotlin/.../
│   │       └── TracingViewModelTest.kt    # 2727 lines, 15 nested test groups
│   │
│   ├── onboarding/                   # First-time user experience
│   │   ├── src/main/kotlin/.../
│   │   │   ├── OnboardingViewModel.kt     # 3-page carousel logic
│   │   │   ├── OnboardingScreen.kt        # HorizontalPager + page indicator
│   │   │   ├── OnboardingPages.kt         # Welcome, TierSelection, MarkerPreparation
│   │   │   ├── WalkthroughViewModel.kt    # 5-step camera walkthrough
│   │   │   ├── WalkthroughScreen.kt       # Camera preview + step overlays
│   │   │   ├── WalkthroughAnalyzer.kt     # Simplified marker detector for onboarding
│   │   │   ├── OnboardingOverlay.kt       # Detecting/Found/Tooltip overlays
│   │   │   ├── SetupGuideViewModel.kt     # Marker/stand guide selection
│   │   │   ├── SetupGuideScreen.kt        # Step cards + download links
│   │   │   ├── OnboardingRepository.kt    # DataStore: completed, tooltip, tier
│   │   │   └── di/
│   │   │       └── OnboardingModule.kt    # Koin: repository, analyzer, 3 ViewModels
│   │   └── src/test/kotlin/.../
│   │       ├── OnboardingViewModelTest.kt
│   │       ├── WalkthroughViewModelTest.kt
│   │       ├── WalkthroughAnalyzerTest.kt
│   │       └── SetupGuideViewModelTest.kt
│   │
│   └── timelapse/                    # Timelapse implementations
│       ├── src/main/kotlin/.../
│       │   ├── FileSnapshotStorage.kt     # JPEG files in filesDir/timelapse/
│       │   ├── MediaCodecCompiler.kt      # H.264 encoding via MediaCodec + MediaMuxer
│       │   ├── MediaStoreVideoExporter.kt # Export to Movies/TraceGlass/ via MediaStore
│       │   ├── IntentVideoSharer.kt       # Android share sheet (ACTION_SEND)
│       │   └── di/
│       │       └── TimelapseModule.kt     # Koin: 4 singleton implementations
│       └── src/test/kotlin/.../
│           ├── FileSnapshotStorageTest.kt
│           ├── MediaCodecCompilerTest.kt
│           ├── TimelapseSessionTest.kt
│           ├── VideoExporterTest.kt
│           └── TimelapseModuleTest.kt
│
├── sdk/
│   └── OpenCV-android-sdk/           # Pre-built OpenCV 4.10.0 (native libs + Java wrappers)
│
├── docs/
│   ├── aruco-markers-a4.pdf          # Printable ArUco markers for testing
│   └── generate-markers.py           # Python script to generate marker PDFs
│
├── _bmad-output/                     # BMAD planning & implementation artifacts
│   ├── planning-artifacts/           # PRD, architecture, epics, UX, test plan
│   └── implementation-artifacts/     # 37 story files + sprint-status.yaml
│
├── .github/workflows/
│   └── ci.yml                        # Build + test + lint on push/PR
│
├── build.gradle.kts                  # Root: subprojects config, JUnit 5 platform
├── settings.gradle.kts               # Module includes, dependency resolution
├── gradle/libs.versions.toml         # Version catalog
└── gradle.properties                 # Memory, Compose, AndroidX flags
```

## Critical Directories

| Directory | Purpose | Key Files |
|-----------|---------|-----------|
| `core/overlay/` | The algorithmic heart — homography, tracking, smoothing | `OverlayTransformCalculator.kt`, `HomographySolver.kt` |
| `core/cv/src/main/cpp/` | Native OpenCV integration | `marker_detector_jni.cpp`, `CMakeLists.txt` |
| `feature/tracing/` | Main user experience | `TracingViewModel.kt` (844 lines), `TracingScreen.kt` |
| `core/session/` | All persisted state | `SessionData.kt` (16 fields), `SettingsData.kt` |
| `app/` | Navigation + DI wiring | `MainActivity.kt`, `TraceGlassApp.kt` |

## Entry Points

| Entry Point | Type | Location |
|-------------|------|----------|
| Android launch | Activity | `app/.../MainActivity.kt` |
| DI bootstrap | Application | `app/.../TraceGlassApp.kt` |
| Frame processing | JNI | `core/cv/src/main/cpp/marker_detector_jni.cpp` → `JNI_OnLoad` |
| Native library | Shared lib | `libtraceglass_cv.so` (built per ABI) |

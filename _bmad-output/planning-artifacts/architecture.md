---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7]
workflowStatus: complete
inputDocuments: ['_bmad-output/planning-artifacts/prd.md']
workflowType: 'architecture'
project_name: 'TraceGlass'
user_name: 'JN0V'
date: '2026-02-08'
---

# Architecture Decision Document

_This document builds collaboratively through step-by-step discovery. Sections are appended as we work through each architectural decision together._

## Project Context Analysis

### Requirements Overview

**Functional Requirements:**
36 FRs across 9 capability areas: Image Overlay & Display (7), Marker Tracking & Adaptive Behavior (6), Overlay Positioning (2), Time-lapse (4), User Onboarding (4), Setup Assistance (4), Session Persistence (5), User Controls & Settings (4).

Core architectural challenge: real-time camera pipeline with computer vision (OpenCV) overlay rendering at ≥ 20fps with < 50ms marker detection latency.

**Non-Functional Requirements:**
25 NFRs across 5 categories: Performance (5), Reliability (4), Privacy (4), Accessibility (4), Development Process / TDD (8).

Architecture-driving NFRs:
- NFR1: ≥ 20 fps rendering on Snapdragon 835
- NFR2: < 50ms marker detection per frame
- NFR6: No crash during 60-minute sessions
- NFR8-9: Session data survives backgrounding and battery death
- NFR10: Zero network calls — no INTERNET permission
- NFR18-25: Strict TDD — architecture must be testable with injectable dependencies

**Scale & Complexity:**

- Primary domain: Native mobile + real-time computer vision
- Complexity level: Medium-high (CV pipeline + state management + Android lifecycle)
- Major simplifications: Zero backend, zero network, zero auth, single permission
- Estimated architectural components: ~8-10 modules

### Technical Constraints & Dependencies

- **OpenCV Android SDK (C++ via JNI):** Standard JNI bridge to C++. Kotlin + C++ chosen over Kotlin + Rust — Rust adds an extra FFI layer without performance benefit since OpenCV is C++ underneath. Rust considered for Phase 3+ if custom CV code is needed.
- **CameraX API:** Imposes its own lifecycle (ImageAnalysis + Preview use cases). Must coordinate with Activity lifecycle and session state.
- **API 33 Photo Picker:** No storage permission required for image import.
- **MediaStore API:** No storage permission required for time-lapse MP4 export to shared Movies/.
- **MediaCodec:** Native Android encoder for MP4 compilation from snapshots.
- **Jetpack Compose:** UI framework, but overlay rendering likely requires custom SurfaceView/TextureView (Compose cannot do direct GPU composition).
- **F-Droid build compliance (verified 2026-02-08 against current docs):**
  - All dependencies FOSS (OpenCV = Apache 2.0, Jetpack = Apache 2.0)
  - Trusted Maven repos only: Maven Central + Google Maven
  - No Google Play Services, no proprietary analytics/tracking
  - Source in public git, tagged releases required
  - Fastlane metadata structure: `fastlane/metadata/android/en-US/` for descriptions, screenshots, changelogs
  - Metadata file: `metadata/io.github.jn0v.traceglass.yml` in fdroiddata fork
  - `AutoUpdateMode: Version` + `UpdateCheckMode: Tags` for auto-update
  - `AllowedAPKSigningKeys` for reproducible builds with upstream signature
- **F-Droid NDK / native code reproducibility (CRITICAL):**
  - Pin exact NDK version in `build.gradle.kts` AND F-Droid metadata (`ndk: <version>`)
  - CMake flags for reproducibility: `-ffile-prefix-map=${rootDir}=.` (strip build paths)
  - Linker flags: `--build-id=none` (non-deterministic otherwise), `--hash-style=gnu`
  - NDK r26+: clang version string differs macOS vs Linux → `objcopy --remove-section .comment` on `.so` if needed
  - Native lib stripping can cause issues → pin exact NDK version, consider `doNotStrip '**/*.so'` if problems arise
  - 16 KB page size support required for Android 15+ devices using NDK code
- **F-Droid Gradle reproducibility requirements:**
  - Pin build-tools to 34.x.x (apksigner ≥ 35.0.0-rc1 produces unverifiable APKs)
  - Use AGP 8.8+ (fixes DEX ordering and ZIP non-determinism)
  - Disable baseline profile: `tasks.whenTaskAdded { if (name.contains("ArtProfile")) { enabled = false } }`
  - Disable PNG crunch: `aaptOptions { cruncherEnabled = false }`
  - Disable vector drawable PNG generation: `vectorDrawables.generatedDensities = []`
  - R8 proguard rules for kotlinx.coroutines to prevent non-deterministic ServiceLoader optimization
  - Build from clean tagged commit; VCS info included by default since AGP 8.3 (keep it, build from tag)
- **OpenCV pre-built .so strategy:** Use pre-built OpenCV Android SDK with `scanignore` in F-Droid metadata. Upgrade to building from source if F-Droid reviewers require it.
- **Application ID:** `io.github.jn0v.traceglass` (GitHub-based, always accepted by F-Droid).

### Cross-Cutting Concerns Identified

- **Android lifecycle vs session lifecycle:** Activity destruction must not kill session state. ViewModel + persistent storage.
- **Memory management:** Camera frames + OpenCV Mats + overlay bitmaps = high RAM. No per-frame allocation. Buffer recycling mandatory.
- **Thread architecture:** CV pipeline on background thread, UI on main thread, time-lapse capture on worker thread. Clear threading boundaries.
- **Testability (TDD):** Interfaces for Camera, MarkerDetector, OverlayRenderer, SessionManager. Mock implementations for unit tests without hardware.
- **Modularity:** Marker detection strategy must be swappable (custom shapes → ArUco fallback). Strategy pattern.

## Starter Template Evaluation

### Primary Technology Domain

Native Android (Kotlin) + real-time computer vision (OpenCV C++ via JNI). No web, no backend, no cross-platform.

### Starter Options Considered

| Option | Verdict | Rationale |
|--------|---------|-----------|
| Android Studio Empty Compose Activity | **Selected** | Minimal, clean, Compose pre-configured. All structure added manually. |
| Now in Android (Google reference) | Rejected | Multi-module best practices but includes backend sync, Firebase, DataStore patterns we don't need. Massive cleanup. |
| Custom from scratch (manual Gradle) | Rejected | More initial setup for no benefit over Empty Compose template. |

### Selected Starter: Empty Compose Activity

**Rationale:** Simplest starting point that gives us a compiling Compose project. We control every dependency and structural decision. No cleanup required.

**Initialization:**
Android Studio (or command-line) → New Project → Empty Activity (Compose)
- Package: `io.github.jn0v.traceglass`
- Min SDK: API 33
- Build configuration language: Kotlin DSL (`build.gradle.kts`)
- Version catalog: `libs.versions.toml`

**IDE Independence:** Project uses Gradle wrapper (`./gradlew`). Development in Windsurf/VSCode is primary. Android Studio optional for profiling and native C++ debugging.

**Architectural Decisions Provided by Starter:**

- **Language & Runtime:** Kotlin, Compose Compiler plugin
- **UI Framework:** Jetpack Compose + Material 3
- **Build:** Gradle Kotlin DSL with version catalog
- **Testing (default):** JUnit 4 + Compose UI test → will migrate to JUnit 5 + MockK
- **Structure:** Single `app` module → will evolve to multi-module

**Dependencies to Add Post-Init:**

| Dependency | Purpose |
|-----------|---------|
| CameraX (camera-camera2, camera-lifecycle, camera-view) | Camera pipeline |
| OpenCV Android SDK | Computer vision / marker detection |
| Kotlin Coroutines | Async / threading |
| MockK | Kotlin-idiomatic mocking for TDD |
| JUnit 5 (Jupiter) | Modern test framework |
| AndroidX Lifecycle (ViewModel, SavedStateHandle) | Session state management |
| MediaCodec (Android framework) | MP4 encoding (no extra dependency) |

**F-Droid Reproducibility Configuration (applied at init):**

```kotlin
// build.gradle.kts (app)
android {
    buildToolsVersion = "34.0.0"
    defaultConfig {
        vectorDrawables.generatedDensities = emptySet()
    }
    aaptOptions {
        cruncherEnabled = false
    }
    buildTypes {
        release {
            // Build from clean tagged commit — VCS info is fine
        }
    }
    packaging {
        // Consider doNotStrip if reproducibility issues arise
        // jniLibs { keepDebugSymbols += "**/*.so" }
    }
}

// Disable non-deterministic baseline profile generation
tasks.whenTaskAdded {
    if (name.contains("ArtProfile")) {
        enabled = false
    }
}
```

**Note:** Project initialization using this starter is the first implementation story.

## Guiding Principles

All architectural decisions MUST respect these three principles. They are non-negotiable and apply to every code change, module boundary, and design choice.

### SOLID

- **S — Single Responsibility:** Each class, module, and file has exactly one reason to change. One responsibility per file (200-800 lines max).
- **O — Open/Closed:** Open for extension, closed for modification. Strategy pattern for marker detection. Interfaces for swappable implementations.
- **L — Liskov Substitution:** Any interface implementation can replace another without breaking behavior. Mock implementations must be valid substitutes.
- **I — Interface Segregation:** Small, focused interfaces. No god-interfaces. A `MarkerDetector` does not also manage sessions.
- **D — Dependency Inversion:** High-level modules depend on abstractions, not concretions. Koin provides implementations at runtime. Core modules never depend on feature modules.

### YAGNI (You Aren't Gonna Need It)

- Do NOT build abstractions "for later." Split modules, add layers, or create interfaces only when there is a concrete, current need.
- Start simple. Refactor when complexity demands it, not before.
- No speculative generalization. Phase 2/3 features are NOT designed into the MVP architecture.

### KISS (Keep It Simple, Stupid)

- Prefer the simplest solution that works correctly.
- Small, focused modules > large multi-responsibility modules. Smaller units are easier for AI agents to reason about and test.
- If a decision adds complexity without solving a current problem, reject it.
- Readable code > clever code. Explicit > implicit.

## Core Architectural Decisions

### Decision 1: Application Architecture Pattern

**Decision:** MVVM (ViewModel + StateFlow)

**Rationale:**
- Standard Android pattern, native to Compose (KISS)
- ViewModel manages UI state, StateFlow exposes it reactively
- No formal Clean Architecture layers — interfaces only where testability demands it (YAGNI)
- Clear separation: ViewModel (logic) → State (data) → Composable (UI) (SOLID-S)

**Affects:** All feature modules

### Decision 2: Module Structure

**Decision:** 8 modules (optimized for AI-assisted development and unit testability)

```
:app                  → DI (Koin), Navigation, MainActivity
:core:camera          → CameraX wrapper, camera lifecycle, frame provider interface
:core:cv              → OpenCV JNI bridge, marker detection, MarkerDetector interface
:core:overlay         → Overlay rendering, transformation math, compositing
:core:session         → Session state, DataStore persistence
:feature:tracing      → Main tracing screen, controls UI
:feature:onboarding   → Onboarding flow (3 screens) + setup guides
:feature:timelapse    → Time-lapse capture, snapshot management, MP4 export
```

**Rationale:**
- Small, focused modules are easier for AI agents to reason about, generate, and test in isolation (KISS for vibe coding).
- Each module has a single, clear responsibility (SOLID-S): camera ≠ CV ≠ overlay ≠ time-lapse.
- Smaller modules = faster incremental builds, more granular test suites, better TDD feedback loops.
- Clear interface boundaries between modules prevent AI agents from creating unintended coupling.

**Dependency graph:**
```
:app → :feature:tracing, :feature:onboarding, :feature:timelapse
:feature:tracing → :core:camera, :core:cv, :core:overlay, :core:session
:feature:timelapse → :core:camera, :core:session
:feature:onboarding → :core:session
:core:cv → :core:camera (consumes frame data)
:core:overlay → :core:cv (consumes marker results)
:core:camera → (CameraX — no internal deps)
:core:session → (DataStore — no internal deps)
```

**Rule:** Core modules NEVER depend on feature modules. Feature modules NEVER depend on each other (SOLID-D).

### Decision 3: Data Persistence

**Decision:** DataStore Preferences

**Rationale:**
- Key-value storage is sufficient for settings + session metadata (KISS)
- Coroutines-native, type-safe (better than SharedPreferences)
- No Room — we have no relational data (YAGNI)
- Time-lapse snapshots: JPEG files in `context.filesDir/timelapse/` (simple file I/O)

**Stored data:**
- Onboarding completed flag
- Last overlay opacity, color tint, inverted mode
- Session state: image URI, overlay transform (position, scale), time-lapse snapshot count
- Settings preferences

### Decision 4: Dependency Injection

**Decision:** Koin

**Rationale:**
- Lightweight, Kotlin-native DSL (KISS)
- No annotation processing → faster builds
- Runtime DI — errors caught by tests, not compiler (acceptable with strict TDD discipline)
- Simpler than Hilt/Dagger for a solo dev project

### Decision 5: Threading Model

**Decision:** Kotlin Coroutines with explicit dispatchers

| Dispatcher | Usage |
|-----------|-------|
| `Dispatchers.Main` | Compose UI, state updates |
| `Dispatchers.Default` | OpenCV frame processing (CPU-bound) |
| `Dispatchers.IO` | DataStore, file I/O, snapshot save, MP4 export |

**Rule:** All dispatchers are injected (not hardcoded) for testability (SOLID-D). Tests use `UnconfinedTestDispatcher`.

### Decision 6: Navigation

**Decision:** Compose Navigation (Jetpack)

**Rationale:**
- Standard, handles back stack, type-safe routes since Navigation 2.8+ (KISS)
- Only 3-4 destinations: Onboarding → Tracing → Settings (YAGNI — no custom router needed)

### Decision 7: OpenCV Integration Pattern

**Decision:** Thin JNI wrapper with Kotlin interface

**Pattern:**
- Kotlin `interface MarkerDetector` in `:core:camera` (SOLID-D)
- `OpenCvMarkerDetector` implementation calls JNI functions
- JNI layer is minimal — receives frame buffer, returns marker positions (KISS)
- `FakeMarkerDetector` for unit tests (SOLID-L)
- Strategy pattern allows swapping detection algorithm without changing consumers (SOLID-O)

### Decision 8: Camera Pipeline Architecture

**Decision:** CameraX ImageAnalysis + PreviewView

**Pipeline:**
```
CameraX Preview → PreviewView (display)
CameraX ImageAnalysis → OpenCV Mat → MarkerDetector → MarkerResult
MarkerResult + OverlayImage → OverlayTransform → Compose Canvas (render)
```

- ImageAnalysis runs on its own executor (background thread)
- Results posted to StateFlow, consumed by Compose on Main thread
- No frame copying — ImageProxy buffer passed directly to JNI (KISS, performance)

### Decision Impact Analysis

**Implementation Sequence:**
1. Project init + Gradle config + F-Droid repro settings
2. `:core:camera` — CameraX wrapper + frame provider interface
3. `:core:cv` — OpenCV JNI bridge + MarkerDetector interface
4. `:core:overlay` — Overlay rendering + transformation math
5. `:core:session` — DataStore + session state model
6. `:feature:tracing` — Main screen, controls, wire core modules
7. `:feature:timelapse` — Snapshot capture + MP4 export
8. `:feature:onboarding` — Onboarding flow
9. `:app` — Koin DI, Navigation, wire everything

**Cross-Module Dependencies:**
- `:core:camera` and `:core:session` are fully independent (can be developed in parallel)
- `:core:cv` depends on `:core:camera` (frame data)
- `:core:overlay` depends on `:core:cv` (marker results)
- `:feature:tracing` is the central consumer — assembles core modules
- `:feature:timelapse` uses `:core:camera` and `:core:session` only
- All feature modules are independent of each other

## Implementation Patterns & Consistency Rules

### Principle Compliance Tags

Every architectural decision and pattern in this document references which guiding principle justifies it:
- **(SOLID-S)** = Single Responsibility
- **(SOLID-O)** = Open/Closed
- **(SOLID-L)** = Liskov Substitution
- **(SOLID-I)** = Interface Segregation
- **(SOLID-D)** = Dependency Inversion
- **(YAGNI)** = You Aren't Gonna Need It
- **(KISS)** = Keep It Simple, Stupid

### Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Classes/Interfaces | PascalCase | `MarkerDetector`, `TracingViewModel` |
| Functions | camelCase | `detectMarkers()`, `saveSession()` |
| Variables/properties | camelCase | `overlayOpacity`, `markerPositions` |
| Constants | SCREAMING_SNAKE | `MAX_OVERLAY_OPACITY`, `DEFAULT_SNAPSHOT_INTERVAL` |
| Packages | lowercase dot-separated | `io.github.jn0v.traceglass.core.camera` |
| Composables | PascalCase | `TracingScreen()`, `OverlayControls()` |
| State classes | suffix `UiState` | `TracingUiState`, `OnboardingUiState` |
| Events/Actions | suffix `Action`, sealed interface | `TracingAction.AdjustOpacity` |
| Interfaces | no `I` prefix | `MarkerDetector` (not `IMarkerDetector`) (KISS) |
| Implementations | descriptive prefix | `OpenCvMarkerDetector`, `DataStoreSessionRepository` |
| Test classes | suffix `Test` | `MarkerDetectorTest`, `TracingViewModelTest` |
| Test fakes | prefix `Fake` | `FakeMarkerDetector`, `FakeCameraManager` |
| JNI C++ files | snake_case | `marker_detection.cpp` |

### File & Package Structure Pattern

```
:core:camera/
  src/main/kotlin/io/github/jn0v/traceglass/core/camera/
    CameraManager.kt               ← Interface (SOLID-D)
    FrameProvider.kt                ← Interface (SOLID-I)
    impl/
      CameraXManager.kt            ← Implementation
    di/
      CameraModule.kt              ← Koin module
  src/test/kotlin/io/github/jn0v/traceglass/core/camera/
    FakeCameraManager.kt           ← Test fake (SOLID-L)
    CameraXManagerTest.kt          ← Unit test
```

**Rules:**
- Tests mirror source package structure (KISS)
- Fakes/Stubs in `src/test/`, never in `src/main/` (SOLID-S)
- One public class/interface per file (SOLID-S)
- `impl/` subdirectory for concrete implementations (SOLID-D)
- `di/` subdirectory for Koin module definitions
- Max 200-800 lines per file

### State Management Pattern

```kotlin
// ViewModel exposes immutable StateFlow (SOLID-S, KISS)
class TracingViewModel(
    private val dispatchers: AppDispatchers,
    private val markerDetector: MarkerDetector,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TracingUiState())
    val uiState: StateFlow<TracingUiState> = _uiState.asStateFlow()

    fun onAction(action: TracingAction) {
        when (action) {
            is TracingAction.AdjustOpacity -> { ... }
            is TracingAction.ToggleControls -> { ... }
        }
    }
}

// State = immutable data class (KISS)
data class TracingUiState(
    val overlayOpacity: Float = 0.5f,
    val controlsVisible: Boolean = true,
    val trackingState: TrackingState = TrackingState.NoMarkers,
)

// Actions = sealed interface, exhaustive when (SOLID-O)
sealed interface TracingAction {
    data class AdjustOpacity(val value: Float) : TracingAction
    data object ToggleControls : TracingAction
}
```

**Rules:**
- Immutable state (`data class`), never mutated directly (KISS)
- Actions via `sealed interface` with exhaustive `when` (SOLID-O)
- One `StateFlow` per ViewModel (KISS)
- ViewModel receives actions, updates state — no business logic in Composables

### Error Handling Pattern

```kotlin
// Typed result for failable operations (SOLID-S)
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable) : Result<Nothing>
}
```

**Rules:**
- `Result<T>` for failable repository operations (SOLID-S)
- No empty `try-catch` — always log + propagate (KISS)
- Errors surfaced in `UiState` (e.g., `errorMessage: String?`)
- No exceptions in Composables — ViewModel handles all errors

### Coroutine Pattern

```kotlin
// Dispatchers injected via Koin (SOLID-D)
data class AppDispatchers(
    val main: CoroutineDispatcher = Dispatchers.Main,
    val default: CoroutineDispatcher = Dispatchers.Default,
    val io: CoroutineDispatcher = Dispatchers.IO,
)
```

**Rules:**
- Never hardcode `Dispatchers.X` — always via `AppDispatchers` (SOLID-D)
- `viewModelScope` for ViewModel coroutines
- `withContext(dispatchers.io)` for punctual I/O
- Tests use `AppDispatchers(UnconfinedTestDispatcher(), ...)`

### Compose Pattern

```kotlin
// Screen = stateful, consumes ViewModel (SOLID-S)
@Composable
fun TracingScreen(viewModel: TracingViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TracingContent(state = uiState, onAction = viewModel::onAction)
}

// Content = stateless, previewable, testable (SOLID-S, KISS)
@Composable
fun TracingContent(
    state: TracingUiState,
    onAction: (TracingAction) -> Unit,
) { ... }
```

**Rules:**
- Separate Screen (stateful + ViewModel) from Content (stateless + preview) (SOLID-S)
- `collectAsStateWithLifecycle()` not `collectAsState()` (lifecycle-aware)
- Callbacks via `onAction` lambda — no ViewModel references in Content (SOLID-D)
- `@Preview` only on Content composables

### Logging Pattern

```kotlin
private const val TAG = "OpenCvMarkerDetector"
// Log.d → Debug info (dev only)
// Log.w → Unexpected but handled situations
// Log.e → Real errors
// NEVER Log.i or Log.v in production code
```

### Enforcement

**All AI agents MUST:**
1. Follow naming conventions exactly — no variations
2. Place files in the correct package/directory structure
3. Use the State/Action/ViewModel pattern for all feature screens
4. Inject dispatchers, never hardcode
5. Write tests that mirror source structure
6. Tag every architectural decision with the principle it respects

## Project Structure & Boundaries

### Complete Project Directory Structure

```
TraceGlass/
├── README.md
├── LICENSE                          (GPL-3.0 or Apache-2.0 — TBD)
├── CONTRIBUTING.md
├── .gitignore
├── .github/
│   └── workflows/
│       └── ci.yml                   (Build + test on push/PR)
├── fastlane/
│   └── metadata/
│       └── android/
│           └── en-US/
│               ├── short_description.txt
│               ├── full_description.txt
│               ├── changelogs/
│               │   └── 1.txt
│               └── images/
│                   ├── icon.png
│                   └── phoneScreenshots/
├── gradle/
│   ├── libs.versions.toml           (Version catalog — single source of truth)
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts              (Module includes)
├── build.gradle.kts                 (Root — plugins, common config)
│
├── app/
│   ├── build.gradle.kts             (Depends on all feature modules)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/io/github/jn0v/traceglass/
│       │       ├── TraceGlassApp.kt          (Application class, Koin init)
│       │       ├── MainActivity.kt           (Single activity)
│       │       ├── navigation/
│       │       │   └── AppNavGraph.kt        (Compose Navigation graph)
│       │       └── di/
│       │           └── AppModule.kt          (Root Koin module, assembles all)
│       └── test/kotlin/io/github/jn0v/traceglass/
│           └── NavigationTest.kt
│
├── core/
│   ├── camera/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/kotlin/io/github/jn0v/traceglass/core/camera/
│   │       │   ├── CameraManager.kt          (Interface: start/stop/lifecycle)
│   │       │   ├── FrameProvider.kt           (Interface: frame delivery)
│   │       │   ├── FlashlightController.kt    (Interface: torch on/off)
│   │       │   ├── impl/
│   │       │   │   └── CameraXManager.kt      (CameraX implementation)
│   │       │   └── di/
│   │       │       └── CameraModule.kt
│   │       └── test/kotlin/io/github/jn0v/traceglass/core/camera/
│   │           ├── FakeCameraManager.kt
│   │           └── CameraXManagerTest.kt
│   │
│   ├── cv/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/
│   │       │   ├── kotlin/io/github/jn0v/traceglass/core/cv/
│   │       │   │   ├── MarkerDetector.kt      (Interface: detect markers)
│   │       │   │   ├── MarkerResult.kt        (Data class: positions, confidence)
│   │       │   │   ├── impl/
│   │       │   │   │   └── OpenCvMarkerDetector.kt  (JNI calls)
│   │       │   │   └── di/
│   │       │   │       └── CvModule.kt
│   │       │   └── cpp/
│   │       │       ├── CMakeLists.txt         (OpenCV link, repro flags)
│   │       │       ├── marker_detection.cpp   (JNI native functions)
│   │       │       └── marker_detection.h
│   │       └── test/kotlin/io/github/jn0v/traceglass/core/cv/
│   │           ├── FakeMarkerDetector.kt
│   │           └── OpenCvMarkerDetectorTest.kt
│   │
│   ├── overlay/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/kotlin/io/github/jn0v/traceglass/core/overlay/
│   │       │   ├── OverlayRenderer.kt         (Interface: render overlay)
│   │       │   ├── OverlayTransform.kt        (Data class: position, scale, rotation)
│   │       │   ├── OverlayConfig.kt           (Data class: opacity, color, inverted)
│   │       │   ├── impl/
│   │       │   │   └── CanvasOverlayRenderer.kt  (Compose Canvas implementation)
│   │       │   └── di/
│   │       │       └── OverlayModule.kt
│   │       └── test/kotlin/io/github/jn0v/traceglass/core/overlay/
│   │           ├── OverlayTransformTest.kt    (Math: scale, position calculations)
│   │           └── FakeOverlayRenderer.kt
│   │
│   └── session/
│       ├── build.gradle.kts
│       └── src/
│           ├── main/kotlin/io/github/jn0v/traceglass/core/session/
│           │   ├── SessionRepository.kt       (Interface: save/load session)
│           │   ├── SessionState.kt            (Data class: full session state)
│           │   ├── SettingsRepository.kt      (Interface: app preferences)
│           │   ├── impl/
│           │   │   ├── DataStoreSessionRepository.kt
│           │   │   └── DataStoreSettingsRepository.kt
│           │   └── di/
│           │       └── SessionModule.kt
│           └── test/kotlin/io/github/jn0v/traceglass/core/session/
│               ├── FakeSessionRepository.kt
│               ├── DataStoreSessionRepositoryTest.kt
│               └── DataStoreSettingsRepositoryTest.kt
│
├── feature/
│   ├── tracing/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/kotlin/io/github/jn0v/traceglass/feature/tracing/
│   │       │   ├── TracingScreen.kt           (Stateful: ViewModel consumer)
│   │       │   ├── TracingContent.kt          (Stateless: previewable UI)
│   │       │   ├── TracingViewModel.kt        (State + actions)
│   │       │   ├── TracingUiState.kt          (State data class)
│   │       │   ├── TracingAction.kt           (Sealed interface)
│   │       │   └── components/
│   │       │       ├── OverlayControls.kt     (Opacity, color, invert controls)
│   │       │       └── TrackingStatusBar.kt   (Marker status indicator)
│   │       └── test/kotlin/io/github/jn0v/traceglass/feature/tracing/
│   │           └── TracingViewModelTest.kt
│   │
│   ├── onboarding/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/kotlin/io/github/jn0v/traceglass/feature/onboarding/
│   │       │   ├── OnboardingScreen.kt
│   │       │   ├── OnboardingContent.kt
│   │       │   ├── OnboardingViewModel.kt
│   │       │   ├── OnboardingUiState.kt
│   │       │   ├── OnboardingAction.kt
│   │       │   └── pages/
│   │       │       ├── WelcomePage.kt
│   │       │       ├── SetupTierPage.kt
│   │       │       └── MarkerGuidePage.kt
│   │       └── test/kotlin/io/github/jn0v/traceglass/feature/onboarding/
│   │           └── OnboardingViewModelTest.kt
│   │
│   └── timelapse/
│       ├── build.gradle.kts
│       └── src/
│           ├── main/kotlin/io/github/jn0v/traceglass/feature/timelapse/
│           │   ├── TimelapseManager.kt        (Interface: capture, compile, export)
│           │   ├── TimelapseState.kt          (Data class: snapshot count, status)
│           │   ├── impl/
│           │   │   ├── SnapshotCapturer.kt    (Periodic JPEG capture)
│           │   │   └── Mp4Compiler.kt         (MediaCodec MP4 encoding)
│           │   └── di/
│           │       └── TimelapseModule.kt
│           └── test/kotlin/io/github/jn0v/traceglass/feature/timelapse/
│               ├── FakeTimelapseManager.kt
│               ├── SnapshotCapturerTest.kt
│               └── Mp4CompilerTest.kt
```

### Architectural Boundaries

**Module Boundaries (enforced by Gradle dependencies):**

| Module | Exposes | Consumes | Never Depends On |
|--------|---------|----------|------------------|
| `:core:camera` | `CameraManager`, `FrameProvider`, `FlashlightController` | CameraX | Any feature module |
| `:core:cv` | `MarkerDetector`, `MarkerResult` | `:core:camera` (frames) | Any feature module |
| `:core:overlay` | `OverlayRenderer`, `OverlayTransform` | `:core:cv` (markers) | Any feature module |
| `:core:session` | `SessionRepository`, `SettingsRepository` | DataStore | Any feature module |
| `:feature:tracing` | `TracingScreen` composable | All 4 core modules | Other feature modules |
| `:feature:onboarding` | `OnboardingScreen` composable | `:core:session` | Other feature modules |
| `:feature:timelapse` | `TimelapseManager` interface | `:core:camera`, `:core:session` | Other feature modules |
| `:app` | Nothing (entry point) | All feature modules | Nothing |

**Data Flow:**
```
Camera Frame → FrameProvider → MarkerDetector → MarkerResult
                                                     ↓
User Image → OverlayConfig → OverlayRenderer ← OverlayTransform
                                    ↓
                              Compose Canvas (display)
                                    ↓
                          SnapshotCapturer → JPEG files → Mp4Compiler → MediaStore
```

### Requirements to Module Mapping

| FR Category | Module | FRs |
|-------------|--------|-----|
| Image Overlay & Display | `:core:overlay`, `:feature:tracing` | FR1-FR7 |
| Marker Tracking | `:core:cv`, `:core:camera` | FR8-FR13 |
| Overlay Positioning | `:core:overlay`, `:feature:tracing` | FR14-FR15 |
| Time-lapse | `:feature:timelapse` | FR16-FR19 |
| Onboarding | `:feature:onboarding` | FR20-FR23 |
| Setup Assistance | `:feature:onboarding` | FR24-FR27 |
| Session Persistence | `:core:session`, `:feature:tracing` | FR28-FR32 |
| Controls & Settings | `:feature:tracing`, `:app` | FR33-FR36 |

### Development Workflow

**Build:** `./gradlew assembleDebug` (all modules)
**Test all:** `./gradlew test` (unit tests across all modules)
**Test one module:** `./gradlew :core:cv:test` (isolated module test)
**Deploy:** `adb install -r app/build/outputs/apk/debug/app-debug.apk`
**Release:** `./gradlew assembleRelease` (from clean tagged commit)
**F-Droid local verification:** Docker-based fdroidserver build (see F-Droid constraints section)

## Architecture Validation Results

### Coherence Validation ✅

**Decision Compatibility:**
- Kotlin + Compose + CameraX + OpenCV JNI: All compatible. CameraX provides ImageAnalysis frames, OpenCV processes them via JNI, Compose renders overlay. No conflicts.
- Koin + Coroutines + StateFlow: All Kotlin-native, no annotation processing, coroutines-compatible. No conflicts.
- DataStore + Koin: DataStore is coroutines-native, injected via Koin. Compatible.
- AGP 8.8+ + build-tools 34 + NDK pinning: All compatible with F-Droid reproducible build requirements.

**Pattern Consistency:**
- MVVM + StateFlow + sealed interface Actions: Consistent across all 3 feature modules.
- Naming conventions apply uniformly to all 8 modules.
- Interface + impl/ + di/ structure is consistent in all core modules.

**Structure Alignment:**
- 8-module Gradle structure enforces boundaries at build level — violations cause compile errors (SOLID-D enforced by tooling).
- Every module has matching test structure.
- F-Droid metadata (fastlane/) is at project root as required.

### Requirements Coverage Validation ✅

**Functional Requirements (36 FRs):**

| FR Range | Capability | Module(s) | Covered |
|----------|-----------|-----------|---------|
| FR1-FR7 | Image Overlay & Display | `:core:overlay`, `:feature:tracing` | ✅ |
| FR8-FR13 | Marker Tracking | `:core:cv`, `:core:camera` | ✅ |
| FR14-FR15 | Overlay Positioning | `:core:overlay`, `:feature:tracing` | ✅ |
| FR16-FR19 | Time-lapse | `:feature:timelapse` | ✅ |
| FR20-FR23 | Onboarding | `:feature:onboarding` | ✅ |
| FR24-FR27 | Setup Assistance | `:feature:onboarding` | ✅ |
| FR28-FR32 | Session Persistence | `:core:session`, `:feature:tracing` | ✅ |
| FR33-FR36 | Controls & Settings | `:feature:tracing`, `:app` | ✅ |

**Non-Functional Requirements (25 NFRs):**

| NFR Category | Architectural Support | Covered |
|-------------|----------------------|---------|
| Performance (NFR1-5) | Dedicated CV thread (Dispatchers.Default), no per-frame allocation, JNI buffer pass-through | ✅ |
| Reliability (NFR6-9) | Session persistence via DataStore, ViewModel survives config changes, SavedStateHandle | ✅ |
| Privacy (NFR10-13) | No INTERNET permission, no analytics, memory-only image processing, MediaStore for output | ✅ |
| Accessibility (NFR14-17) | Compose built-in accessibility, Material 3 contrast | ✅ |
| TDD (NFR18-25) | Interfaces for all core modules, Fakes for testing, injected dispatchers, MockK | ✅ |

**Gaps found:** None. All 36 FRs and 25 NFRs have architectural support.

### Implementation Readiness Validation ✅

**Decision Completeness:** 8/8 decisions documented with rationale and principle tags.
**Structure Completeness:** Full project tree with ~70 files mapped.
**Pattern Completeness:** 7 pattern categories with code examples and enforcement rules.

### Gap Analysis Results

**Critical Gaps:** None.

**Minor Gaps (non-blocking):**
1. **Exact library versions** not pinned yet (CameraX, OpenCV SDK, Koin, Compose BOM) — will be resolved at project init when checking latest stable versions.
2. **CI pipeline** (.github/workflows/ci.yml) not fully specified — standard Gradle build + test, details at implementation time.
3. **ProGuard/R8 rules** for release builds — needed for F-Droid but defined during release configuration.
4. **License choice** (GPL-3.0 vs Apache-2.0) — deferred to project setup.

### Architecture Completeness Checklist

**✅ Requirements Analysis**
- [x] Project context thoroughly analyzed
- [x] Scale and complexity assessed
- [x] Technical constraints identified (including F-Droid NDK reproducibility)
- [x] Cross-cutting concerns mapped

**✅ Architectural Decisions**
- [x] 8 critical decisions documented with rationale
- [x] Technology stack fully specified
- [x] Guiding principles (SOLID, YAGNI, KISS) defined and enforced
- [x] Performance considerations addressed (threading model, buffer management)

**✅ Implementation Patterns**
- [x] Naming conventions established (13 categories)
- [x] Structure patterns defined (file/package layout)
- [x] State management pattern with code examples
- [x] Error handling, coroutine, compose, logging patterns documented

**✅ Project Structure**
- [x] Complete 8-module directory structure defined
- [x] Module boundaries established (enforced by Gradle)
- [x] Data flow mapped
- [x] All 36 FRs mapped to specific modules

### Architecture Readiness Assessment

**Overall Status:** READY FOR IMPLEMENTATION

**Confidence Level:** High

**Key Strengths:**
- F-Droid compliance verified against current (2026) documentation
- 8-module structure optimized for AI-assisted development and TDD
- Every decision tagged with SOLID/YAGNI/KISS justification
- Clear dependency graph prevents coupling violations at compile time

**Areas for Future Enhancement:**
- Library version pinning (first implementation story)
- R8/ProGuard rules (release configuration)
- CI/CD pipeline details (GitHub Actions)

### Implementation Handoff

**AI Agent Guidelines:**
- Follow all architectural decisions exactly as documented
- Use implementation patterns consistently across all modules
- Respect module boundaries — Gradle enforces them
- Refer to this document for all architectural questions
- Tag principle compliance in code comments where non-obvious

**First Implementation Priority:**
1. Create project from Empty Compose Activity template
2. Configure 8-module Gradle structure
3. Apply F-Droid reproducibility settings
4. Add version catalog with all dependencies
5. Verify `./gradlew assembleDebug` and `./gradlew test` pass

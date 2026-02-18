# Story 1.5: Flashlight Toggle

Status: done

## Story

As a user,
I want to toggle the flashlight from the tracing screen,
so that I can illuminate my drawing surface in low light.

## Acceptance Criteria

1. **Given** the camera feed is active **When** the user taps the flashlight button **Then** the device torch turns on
2. **Given** the torch is on **When** the button icon visually indicates state **Then** the button icon shows the torch is active (`FlashlightOn`)
3. **Given** the torch is on **When** the user taps the flashlight button again **Then** the torch turns off
4. **Given** the torch turns off **When** the button icon updates **Then** the button icon returns to inactive state (`FlashlightOff`)
5. **Given** the device has no flashlight hardware **When** the tracing screen loads **Then** the flashlight button is hidden (not disabled)

## Tasks / Subtasks

- [x] Task 1: Create FlashlightController interface (AC: #1, #3, #5)
  - [x] 1.1: `FlashlightController` interface with `toggleTorch()`, `isTorchOn: StateFlow<Boolean>`, `hasFlashlight: Boolean`
- [x] Task 2: Implement in CameraXManager (AC: #1, #3)
  - [x] 2.1: `CameraXManager` implements both `CameraManager` and `FlashlightController`
  - [x] 2.2: `hasFlashlight` via `PackageManager.FEATURE_CAMERA_FLASH`
  - [x] 2.3: `toggleTorch()` via `camera.cameraControl.enableTorch(newState)` — guards with `hasFlashUnit()` check
  - [x] 2.4: Torch state resets to `false` on `unbind()`
- [x] Task 3: Wire to TracingViewModel (AC: #1, #3)
  - [x] 3.1: `FlashlightController` injected via constructor
  - [x] 3.2: `isTorchOn` collected in `init {}` block → `_uiState.update { it.copy(isTorchOn = ...) }`
  - [x] 3.3: `hasFlashlight` exposed in `TracingUiState`
  - [x] 3.4: `onToggleTorch()` delegates to `flashlightController.toggleTorch()`
- [x] Task 4: UI button in TracingScreen (AC: #1, #2, #3, #4, #5)
  - [x] 4.1: Bottom-left FAB, 48dp size, `navigationBarsPadding()`
  - [x] 4.2: `Icons.Filled.FlashlightOn` / `Icons.Filled.FlashlightOff` based on state
  - [x] 4.3: Content description: "Turn off flashlight" / "Turn on flashlight"
  - [x] 4.4: Entire FAB hidden when `hasFlashlight == false` (not disabled)
- [x] Task 5: Koin DI binding
  - [x] 5.1: `single<FlashlightController> { get<CameraXManager>() }` — dual interface binding from same singleton
- [x] Task 6: Testing
  - [x] 6.1: `FakeFlashlightController` in core/camera/src/test/ AND feature/tracing/src/test/
  - [x] 6.2: 5 unit tests in `TracingViewModelTest.Flashlight` nested class:
    - Initial torch state is off
    - Toggle torch turns it on
    - Toggle torch twice turns it off
    - hasFlashlight reflects controller capability
    - Device without flashlight hides the button

## Dev Notes

### Actual Implementation

**Interface:**
```kotlin
interface FlashlightController {
    val isTorchOn: StateFlow<Boolean>
    val hasFlashlight: Boolean
    fun toggleTorch()
}
```

**Implementation in CameraXManager (dual interface pattern):**
```kotlin
class CameraXManager(private val context: Context) : CameraManager, FlashlightController {
    private val _isTorchOn = MutableStateFlow(false)
    override val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()
    override val hasFlashlight: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    override fun toggleTorch() {
        val cam = camera ?: return
        if (!cam.cameraInfo.hasFlashUnit()) return
        val newState = !_isTorchOn.value
        cam.cameraControl.enableTorch(newState)
        _isTorchOn.value = newState
    }
}
```

**DI (dual binding):**
```kotlin
val cameraModule = module {
    single { CameraXManager(get()) }
    single<CameraManager> { get<CameraXManager>() }
    single<FlashlightController> { get<CameraXManager>() }
}
```

### References

- [Source: architecture.md#Architectural Boundaries] — `:core:camera` exposes FlashlightController
- [Source: architecture.md#State Management Pattern] — ViewModel + StateFlow
- [Source: prd.md#FR7] — User can toggle the device flashlight
- [Source: ux-design-specification.md] — FAB layout

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

- FlashlightController as separate interface from CameraManager (SOLID-I)
- CameraXManager implements both interfaces — single Koin singleton bound to both
- Dual safety check: PackageManager feature check + CameraInfo hasFlashUnit
- Torch state auto-resets on camera unbind
- 5 unit tests covering all acceptance criteria
- Two FakeFlashlightController copies (core + feature test sources)
- **Review follow-ups resolved (2026-02-17):**
  - Deduplicated FakeFlashlightController: deleted standalone file from feature/tracing/src/test/, inlined as private class in TracingViewModelTest. AGP+Kotlin testFixtures not viable (no Kotlin compilation task).
  - toggleTorch() now waits for enableTorch() ListenableFuture before updating state; logs on hardware failure.
  - Null camera guard logs warning instead of silent return.

### Review Follow-ups (AI)

- [x] [AI-Review][MEDIUM] Duplicate FakeFlashlightController in two test source sets — removed standalone duplicate from feature/tracing/src/test/, inlined as private class in TracingViewModelTest. Canonical copy remains in core/camera/src/test/. AGP testFixtures does not support Kotlin sources (no compileDebugTestFixturesKotlin task).
- [x] [AI-Review][MEDIUM] Torch state set optimistically before hardware confirms — toggleTorch() now listens to enableTorch() ListenableFuture, updates _isTorchOn only on success, logs warning on failure.
- [x] [AI-Review][LOW] No user feedback when torch can't toggle — camera null guard now logs `Log.w(TAG, "toggleTorch called but camera not yet bound")` instead of silent return.
- [ ] [AI-Review][LOW] FakeFlashlightController does not model async torch behavior — real impl waits for ListenableFuture before updating state, fake toggles synchronously. Async failure path untested [feature/tracing/.../TracingViewModelTest.kt:952-957]

### File List

- `core/camera/src/main/kotlin/io/github/jn0v/traceglass/core/camera/FlashlightController.kt`
- `core/camera/src/main/kotlin/io/github/jn0v/traceglass/core/camera/impl/CameraXManager.kt`
- `core/camera/src/main/kotlin/io/github/jn0v/traceglass/core/camera/di/CameraModule.kt`
- `core/camera/src/test/kotlin/io/github/jn0v/traceglass/core/camera/FakeFlashlightController.kt`
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingScreen.kt`
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt`
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingUiState.kt`
- ~~`feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/FakeFlashlightController.kt`~~ (deleted — inlined in TracingViewModelTest)
- `feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModelTest.kt`

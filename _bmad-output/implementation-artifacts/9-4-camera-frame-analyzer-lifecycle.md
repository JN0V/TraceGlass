# Story 9.4: Camera & FrameAnalyzer Lifecycle Management

Status: completed

## Story

As a developer,
I want CameraXManager and FrameAnalyzer to properly release resources on lifecycle events,
So that executor threads and coroutine scopes do not leak across activity recreations.

## Acceptance Criteria

1. **Given** `CameraXManager` is destroyed (activity recreation or ViewModel cleared) **When** cleanup runs **Then** the `analysisExecutor` is shut down and no orphan thread remains

2. **Given** `FrameAnalyzer` is injected as a Koin singleton **When** the ViewModel that uses it is cleared **Then** the `snapshotScope` is cancelled and no coroutine leaks

3. **Given** the camera is unbound and rebound (e.g., returning from settings) **When** the new bind completes **Then** the executor is reused (not a new thread created) — no thread accumulation

4. **Given** lifecycle cleanup is implemented **When** all existing tests run **Then** all tests still pass with no regressions

## Tasks / Subtasks

- [x] Task 1: Implement `Closeable` on CameraXManager (AC: #1, #3)
  - [x] 1.1 Add `Closeable` interface to `CameraManager` interface (inherited by `CameraXManager`)
  - [x] 1.2 In `close()`: call `unbind()` then `analysisExecutor.shutdown()`
  - [x] 1.3 Keep executor alive across bind/unbind cycles (only shutdown on close)
  - [x] 1.4 Update `unbind()` to NOT shutdown the executor (existing behavior preserved)
  - [x] 1.5 Kept `single` in CameraModule (per recommended approach — one thread for app lifetime is acceptable)
- [x] Task 2: Fix FrameAnalyzer scope lifecycle (AC: #2)
  - [x] 2.3 Option C selected: Changed Koin registration from `single` to `factory`
  - [x] 2.4 Verified `close()` is called in TracingViewModel.onCleared() (line 136)
  - [x] Extra: Changed TracingScreen to get FrameAnalyzer from ViewModel (not separate koinInject) to avoid factory creating two instances
- [x] Task 3: Verify Koin scoping is correct (AC: #1, #2)
  - [x] 3.1 CameraXManager stays `single` — one thread, acceptable for app lifetime
  - [x] 3.2 FrameAnalyzer changed to `factory` — each ViewModel gets fresh instance with its own scope
  - [x] 3.3 No circular dependency found between camera and tracing modules
- [x] Task 4: Add tests for lifecycle cleanup (AC: #4)
  - [x] 4.1 Test: CameraXManager.close() shuts down executor (CameraXManagerCloseTest)
  - [x] 4.2 Test: FrameAnalyzer.close() cancels snapshotScope (FrameAnalyzerTest.Lifecycle)
  - [x] 4.3 Full test suite passes (333 tasks, 0 failures)

## Dev Notes

### Key Files to Modify

- `core/camera/src/main/kotlin/io/github/jn0v/traceglass/core/camera/impl/CameraXManager.kt` — add Closeable, lifecycle management
- `core/camera/src/main/kotlin/io/github/jn0v/traceglass/core/camera/CameraManager.kt` — interface may need Closeable
- `core/camera/src/main/kotlin/io/github/jn0v/traceglass/core/camera/di/CameraModule.kt` — Koin scoping
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/FrameAnalyzer.kt` — scope lifecycle
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt` — onCleared() cleanup

### Architecture Constraints

- CameraXManager currently implements both `CameraManager` and `FlashlightController` interfaces
- Koin `single` means one instance for app lifetime — `close()` would only run on app termination
- If changed to `scoped` or `factory`, need to ensure camera provider reuse (CameraX ProcessCameraProvider is itself a singleton)
- The `analysisExecutor` comment says "Don't shutdown — reused across bind/unbind cycles" — this is correct for bind/unbind, but NOT for full lifecycle teardown

### Current Koin Registration

```kotlin
// CameraModule.kt
single { CameraXManager(get()) }
single<CameraManager> { get<CameraXManager>() }
single<FlashlightController> { get<CameraXManager>() }
```

This means CameraXManager lives forever. Options:
1. Keep `single` but add cleanup hook (Android has no standard singleton teardown)
2. Change to `scoped` tied to Activity lifecycle
3. Accept the leak for singletons (it's one thread) but fix FrameAnalyzer which is the more impactful leak

### Recommended Approach

- **CameraXManager**: Keep as `single`, add `Closeable.close()` for testability, but accept that in production the single thread lives for app lifetime (acceptable — it's one thread). The real fix is ensuring `unbind()` is always called.
- **FrameAnalyzer**: This is the actual problem. Verify that `TracingViewModel.onCleared()` calls `frameAnalyzer.close()`. If FrameAnalyzer is `single` in Koin, the ViewModel's `close()` call kills the scope for ALL future ViewModels. Change to `factory` scoping.

### Previous Story Intelligence

From Story 1.4: CameraXManager was created with the single-thread executor pattern. From Story 4.1: FrameAnalyzer was added with snapshot capture. The `close()` method exists but the Koin scoping was not reviewed.

### References

- [Source: Adversarial review findings #10, #11]
- [Source: core/camera/.../CameraXManager.kt — lines 33, 122-133]
- [Source: feature/tracing/.../FrameAnalyzer.kt — lines 30, 92-93]
- [Source: core/camera/.../di/CameraModule.kt]
- [Source: feature/tracing/.../TracingViewModel.kt — line 130]

## Dev Agent Record

### Agent Model Used
Claude Opus 4.6

### Debug Log References
N/A — no debugging issues encountered.

### Completion Notes List
- CameraManager interface now extends Closeable; CameraXManager.close() shuts down executor after unbind
- FrameAnalyzer Koin registration changed from `single` to `factory` — critical fix for scope leak
- TracingScreen no longer injects FrameAnalyzer separately; uses viewModel.frameAnalyzer instead
- TracingViewModel.frameAnalyzer visibility changed from private to internal for Screen access
- TracingContent.frameAnalyzer parameter type changed to nullable (FrameAnalyzer?)
- FakeCameraManager and FakeCameraManagerForTest updated with close() implementations
- 6 new tests: 4 in CameraXManagerCloseTest, 2 in FrameAnalyzerTest.Lifecycle
- Adversarial review: added isClosed guard (use-after-close), null-frameAnalyzer log warning, improved test assertions

## Review Notes
- Adversarial review completed
- Findings: 10 total, 3 fixed, 7 skipped (noise/out-of-scope)
- Resolution approach: auto-fix real findings

### File List
- `core/camera/src/main/kotlin/.../CameraManager.kt` — added Closeable
- `core/camera/src/main/kotlin/.../impl/CameraXManager.kt` — added close()
- `core/camera/src/test/kotlin/.../FakeCameraManager.kt` — added close()
- `core/camera/src/test/kotlin/.../CameraXManagerCloseTest.kt` — NEW: 3 lifecycle tests
- `feature/tracing/src/main/kotlin/.../di/TracingModule.kt` — FrameAnalyzer: single → factory
- `feature/tracing/src/main/kotlin/.../TracingViewModel.kt` — frameAnalyzer: private → internal
- `feature/tracing/src/main/kotlin/.../TracingScreen.kt` — removed koinInject() for FrameAnalyzer
- `feature/tracing/src/main/kotlin/.../TracingContent.kt` — frameAnalyzer param nullable
- `feature/tracing/src/test/kotlin/.../FrameAnalyzerTest.kt` — added Lifecycle nested class (2 tests)
- `feature/tracing/src/test/kotlin/.../TracingViewModelTest.kt` — FakeCameraManagerForTest: added close()

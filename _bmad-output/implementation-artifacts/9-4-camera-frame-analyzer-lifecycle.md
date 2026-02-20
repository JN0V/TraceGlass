# Story 9.4: Camera & FrameAnalyzer Lifecycle Management

Status: ready-for-dev

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

- [ ] Task 1: Implement `Closeable` on CameraXManager (AC: #1, #3)
  - [ ] 1.1 Add `Closeable` interface to `CameraXManager`
  - [ ] 1.2 In `close()`: call `analysisExecutor.shutdown()` after `unbindAll()`
  - [ ] 1.3 Keep executor alive across bind/unbind cycles (only shutdown on close)
  - [ ] 1.4 Update `unbind()` to NOT shutdown the executor (current behavior, keep it)
  - [ ] 1.5 Update Koin CameraModule: change from `single` to `scoped` or ensure `close()` is called
- [ ] Task 2: Fix FrameAnalyzer scope lifecycle (AC: #2)
  - [ ] 2.1 Option A: Move snapshotScope creation to a start/stop pattern tied to ViewModel lifecycle
  - [ ] 2.2 Option B: Have TracingViewModel.onCleared() explicitly call frameAnalyzer.close()
  - [ ] 2.3 Option C: Change Koin registration from `single` to `factory` so each ViewModel gets a fresh instance
  - [ ] 2.4 Verify `close()` is called in TracingViewModel.onCleared() (line 130 already does this — verify it works)
- [ ] Task 3: Verify Koin scoping is correct (AC: #1, #2)
  - [ ] 3.1 Review CameraModule DI registration: is CameraXManager a singleton? Should it be?
  - [ ] 3.2 Review TracingModule: is FrameAnalyzer a singleton? Should it be factory-scoped?
  - [ ] 3.3 Ensure no circular dependency between camera and tracing modules
- [ ] Task 4: Add tests for lifecycle cleanup (AC: #4)
  - [ ] 4.1 Test: CameraXManager.close() shuts down executor
  - [ ] 4.2 Test: FrameAnalyzer.close() cancels snapshotScope
  - [ ] 4.3 Run full test suite

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

### Debug Log References

### Completion Notes List

### File List

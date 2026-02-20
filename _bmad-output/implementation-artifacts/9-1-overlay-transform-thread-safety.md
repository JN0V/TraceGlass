# Story 9.1: OverlayTransformCalculator Thread Safety & State Integrity

Status: ready-for-dev

## Story

As a developer,
I want the OverlayTransformCalculator to be safe against concurrent access and partial state corruption,
So that tracking never crashes or produces corrupted overlay positions.

## Acceptance Criteria

1. **Given** `setFocalLength()` is called from viewModelScope while `compute()` runs on the analysis executor **When** both execute concurrently **Then** no data corruption, no crash, and the focal length is applied on the next `compute()` call

2. **Given** an early return occurs in `estimateFromDelta()` due to missing corner lookup **When** the next frame is processed **Then** `smoothedCorners` is not in a partially-updated state — either fully updated or unchanged

3. **Given** the static ArUco detector in C++ (`s_detector`) **When** `nativeDetect()` is called **Then** access is serialized (single-threaded CameraX analysis executor enforced, or mutex in native code)

4. **Given** thread safety is enforced **When** all existing `OverlayTransformCalculatorTest` tests run **Then** all tests still pass with no regressions

## Tasks / Subtasks

- [ ] Task 1: Choose and implement thread confinement strategy for OverlayTransformCalculator (AC: #1)
  - [ ] 1.1 Option A: Confine all calls (compute, setFocalLength, resetReference) to a single dispatcher (recommended: analysis executor)
  - [ ] 1.2 Option B: Add @Synchronized or ReentrantLock around mutable state
  - [ ] 1.3 Option C: Make calibratedFocalLength/needsRebuildPaperCoords atomic, keep rest single-threaded
  - [ ] 1.4 Add thread assertion (`check(Thread.currentThread() == expectedThread)`) as debug-mode guard
- [ ] Task 2: Fix partial-write bug in estimateFromDelta (AC: #2)
  - [ ] 2.1 Copy `smooth` map before mutation, swap atomically on success
  - [ ] 2.2 Or: replace `?: return` with `?: continue` / safe default to avoid mid-loop exit
  - [ ] 2.3 Add test: verify smoothedCorners consistency after failed delta estimation
- [ ] Task 3: Document/enforce single-threaded access to s_detector (AC: #3)
  - [ ] 3.1 Verify CameraX `analysisExecutor` is truly single-threaded (Executors.newSingleThreadExecutor)
  - [ ] 3.2 Add comment in JNI code documenting the threading contract
  - [ ] 3.3 Optional: add std::mutex around s_detector.detectMarkers() as defense-in-depth
- [ ] Task 4: Run full test suite, verify no regressions (AC: #4)

## Dev Notes

### Key Files to Modify

- `core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransformCalculator.kt` — main target
- `core/cv/src/main/cpp/marker_detector_jni.cpp` — static detector threading
- `core/overlay/src/test/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransformCalculatorTest.kt` — add thread safety tests

### Architecture Constraints

- CameraX analysis frames arrive on `analysisExecutor` (single-thread pool in CameraXManager:33)
- `setFocalLength()` is called from `TracingViewModel` which runs on main thread via viewModelScope
- `compute()` is called from composable `LaunchedEffect` collecting `StateFlow` — runs on main thread
- The KDoc claims all calls run on main thread, but `setFocalLength` setting `needsRebuildPaperCoords = true` can race with `compute()` reading it

### Recommended Approach

The simplest fix: make `calibratedFocalLength`, `isFocalLengthExternal`, and `needsRebuildPaperCoords` `@Volatile`. These are the only fields written by `setFocalLength()` (cross-thread). All other state is only touched by `compute()` which runs on main thread. This is sufficient because:
- The writes are independent (no compound check-then-act)
- `compute()` reads `needsRebuildPaperCoords` at the start, then clears it — a race just means rebuild happens on next frame

For the `estimateFromDelta` partial-write issue: work on a copy of the smooth map, then assign back only on success.

### Previous Story Intelligence

From Story 8.2-8.4: the `OverlayTransformCalculator` went through significant refactoring with paper-size agnostic coords, auto-f estimation, and constrained homography. The current test suite (OverlayTransformCalculatorTest) has good coverage of the math but zero coverage of threading scenarios.

### References

- [Source: Adversarial review findings #4, #5, #9]
- [Source: core/overlay/src/main/kotlin/.../OverlayTransformCalculator.kt — KDoc lines 23-28]
- [Source: core/cv/src/main/cpp/marker_detector_jni.cpp — lines 23-25]
- [Source: core/camera/src/main/kotlin/.../CameraXManager.kt — line 33]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

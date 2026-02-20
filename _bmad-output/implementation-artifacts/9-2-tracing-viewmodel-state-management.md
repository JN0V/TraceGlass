# Story 9.2: TracingViewModel State Management Cleanup

Status: completed

## Story

As a developer,
I want TracingViewModel to use safe concurrency primitives for session restore and timelapse operations,
So that race conditions between restore flow, compilation, and export are eliminated.

## Acceptance Criteria

1. **Given** `pendingRestoreData` is set from the init coroutine and read from UI event handlers **When** both access it concurrently **Then** the reader sees either null or a fully-constructed `SessionData` — never a partial object

2. **Given** `restoreSession()` throws `CancellationException` **When** the user retries restore **Then** the restore flow executes correctly (not silently no-op'd by a stale flag)

3. **Given** a timelapse export is in progress **When** the user triggers `shareTimelapse()` which calls `performExport()` **Then** the second export is rejected or queued — not started concurrently

4. **Given** all state management changes are applied **When** all existing `TracingViewModelTest` tests run **Then** all tests still pass with no regressions

## Tasks / Subtasks

- [x] Task 1: Replace `@Volatile pendingRestoreData` with `AtomicReference<SessionData?>` (AC: #1)
  - [x] 1.1 Replace declaration with `AtomicReference<SessionData?>(null)`
  - [x] 1.2 Update all read sites — `getAndSet(null)` for consume-once in `onResumeSessionAccepted()`
  - [x] 1.3 Update all write sites to use `.set()`
- [x] Task 2: Replace dual AtomicBoolean restore flags with sealed class state (AC: #2)
  - [x] 2.1 Created `RestoreState` sealed class: `Idle`, `Loading`, `Completed` (Ready/Failed not needed — pendingRestoreData serves Ready role, errors reset to Idle)
  - [x] 2.2 Replaced `restoreAttempted: AtomicBoolean` + `restoreCompleted: AtomicBoolean` with `AtomicReference<RestoreState>`
  - [x] 2.3 Used `compareAndSet(Idle, Loading)` for entry guard
  - [x] 2.4 On CancellationException: transition back to `Idle` so retry works
  - [x] 2.5 Added test: verify restore retries correctly after CancellationException
- [x] Task 3: Add export mutual exclusion guard (AC: #3)
  - [x] 3.1 Added `if (_uiState.value.isExporting) return` guard to `performExport()`
  - [x] 3.3 Added tests: concurrent export rejected + concurrent share rejected
- [x] Task 4: Run full test suite, verify no regressions (AC: #4) — 200 tests, 0 failures

## Dev Notes

### Key Files to Modify

- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt` — main target (lines 78-79, 551-572, 356-421)
- `feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModelTest.kt` — add concurrency tests

### Architecture Constraints

- TracingViewModel has 11 constructor parameters — do NOT add more. Refactor existing state, don't add new dependencies.
- viewModelScope uses `Dispatchers.Main.immediate` — all state mutations should be on main thread
- Tests use `StandardTestDispatcher` with `runTest` — the `viewModelScope + while(true)` hang pattern from MEMORY.md applies if any infinite loop is introduced
- `android.graphics.Matrix` not available in JUnit 5 — any matrix code must use pure Kotlin

### Current State Flow

```
init {
  viewModelScope.launch {
    sessionRepository.sessionData.first() → pendingRestoreData = data  // SET from init coroutine
  }
}

onResumeSessionAccepted() {
  val data = pendingRestoreData ?: return  // READ from UI handler
  pendingRestoreData = null
  // ... apply data ...
}
```

The race: if `first()` is slow (disk I/O), the user could tap "Resume" before data is ready, see null, and silently skip restore.

### Previous Story Intelligence

From Stories 5.1-5.2: Session persistence was added with eager save + debounce pattern. The restore dialog on cold start was added in 5.2. The dual-AtomicBoolean pattern was introduced to prevent re-entry but has the CancellationException gap.

### References

- [Source: Adversarial review findings #6, #13, #29]
- [Source: feature/tracing/.../TracingViewModel.kt — lines 78-79, 551-572, 356-421]
- [Source: MEMORY.md — viewModelScope + runTest infinite hang]

## Dev Agent Record

### Agent Model Used
Claude Opus 4.6

### Debug Log References
N/A — no debugging needed, all changes compiled and tested on first run.

### Completion Notes List
- Simplified RestoreState to 3 states (Idle/Loading/Completed) instead of spec's 5 — Ready(data) role served by pendingRestoreData AtomicReference, Failed not needed since errors transition to Idle for retry.
- Used `_uiState.value.isExporting` guard (Task 3.1 path) instead of separate AtomicBoolean — simpler, consistent with existing `isCompiling` guard pattern.
- Removed unused `AtomicBoolean` import.

### File List
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt` — main implementation changes
- `feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModelTest.kt` — 4 new tests added

## Review Notes
- Adversarial review completed
- Findings: 13 total, 8 fixed, 3 skipped (noise), 2 skipped (intentional/undecided)
- Resolution approach: auto-fix
- Fixed: IO error test (F3), try/finally for unmockkStatic (F4), viewModelScope cleanup (F5), final state assertions (F6/F7), field ordering (F8), sealed interface (F9), Log.w for error path (F10)
- Skipped: F1 (intentional design — save blocked during dialog), F2 (TOCTOU noise — main thread only), F11 (data object singleton invariant obvious), F12 (AC #1 requires AtomicReference), F13 (type widening minimal impact)

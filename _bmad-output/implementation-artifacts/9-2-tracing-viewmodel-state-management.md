# Story 9.2: TracingViewModel State Management Cleanup

Status: ready-for-dev

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

- [ ] Task 1: Replace `@Volatile pendingRestoreData` with `AtomicReference<SessionData?>` (AC: #1)
  - [ ] 1.1 Replace declaration at line 572
  - [ ] 1.2 Update all read sites (`pendingRestoreData` access) to use `.get()`
  - [ ] 1.3 Update all write sites to use `.set()` or `.getAndSet(null)` for consume-once pattern
- [ ] Task 2: Replace dual AtomicBoolean restore flags with sealed class state (AC: #2)
  - [ ] 2.1 Create `RestoreState` sealed class: `Idle`, `Loading`, `Ready(data)`, `Completed`, `Failed`
  - [ ] 2.2 Replace `restoreAttempted: AtomicBoolean` + `restoreCompleted: AtomicBoolean` with `AtomicReference<RestoreState>`
  - [ ] 2.3 Use `compareAndSet(Idle, Loading)` for entry guard
  - [ ] 2.4 On CancellationException: transition back to `Idle` (not `Failed`) so retry works
  - [ ] 2.5 Add test: verify restore retries correctly after CancellationException
- [ ] Task 3: Add export mutual exclusion guard (AC: #3)
  - [ ] 3.1 Add `if (_uiState.value.isExporting) return` guard to `performExport()`
  - [ ] 3.2 Or: add `isExporting` AtomicBoolean similar to `isCompiling` pattern
  - [ ] 3.3 Add test: verify concurrent export is rejected
- [ ] Task 4: Run full test suite, verify no regressions (AC: #4)

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

### Debug Log References

### Completion Notes List

### File List

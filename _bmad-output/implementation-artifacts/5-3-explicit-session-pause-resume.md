# Story 5.3: Explicit Session Pause & Resume

Status: done

## Story

As a user,
I want to explicitly pause and resume my tracing session,
so that I can take breaks and come back later.

## Acceptance Criteria

1. Given a tracing session is active, when the user taps pause, then the session is paused and persisted to DataStore (FR28), the wake lock is released, and the UI indicates the session is paused
2. Given a session is paused, when the user taps resume, then the session resumes with all state intact and the wake lock is re-acquired

## Tasks / Subtasks

- [x] Task 1: Add auto-save on session toggle (AC: #1, #2)
  - [x] `onToggleSession()` calls `viewModelScope.launch { saveSession() }` after toggling `isSessionActive`
- [x] Task 2: Verify wake lock lifecycle (AC: #1, #2)
  - [x] `TracingContent.kt` DisposableEffect adds/removes `FLAG_KEEP_SCREEN_ON` based on `isSessionActive`
  - [x] Pre-existing from Story 2-6 — no new code needed
- [x] Task 3: Write unit tests (AC: #1, #2)

## Dev Notes

### Architecture Patterns

- **Minimal incremental change:** This story leverages all infrastructure from 5-1 (DataStore, save/debounce) and 5-2 (restore). The only new code is 1 line: `viewModelScope.launch { saveSession() }` in `onToggleSession()`.
- **Wake lock management:** `FLAG_KEEP_SCREEN_ON` is toggled in `TracingContent.kt` via `DisposableEffect(isSessionActive)` — this was built in Story 2-6 and works unchanged for pause/resume.
- **Session toggle = save trigger:** Starting or stopping a session immediately persists the full session state, ensuring that if the app is killed after pausing, the paused session data is retained for the resume dialog on next cold start.

### Project Structure Notes

- `feature/tracing/TracingViewModel.kt`: 1 line added to `onToggleSession()`
- `feature/tracing/TracingContent.kt`: wake lock DisposableEffect (pre-existing from 2-6)

### References

- [Source: feature/tracing/src/main/kotlin/.../TracingViewModel.kt#onToggleSession] — save trigger
- [Source: feature/tracing/src/main/kotlin/.../TracingContent.kt] — wake lock lifecycle

## Dev Agent Record

### Agent Model Used

Claude Opus 4 (claude-opus-4-20250514)

### Debug Log References

### Completion Notes List

- 2 ViewModel tests (toggle triggers save, pause/resume preserves state)
- Smallest story in Epic 5 — all infrastructure already existed from 5-1 and 5-2

### File List

- feature/tracing/src/main/kotlin/.../TracingViewModel.kt (modified — 1 line in onToggleSession)
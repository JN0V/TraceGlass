# Story 5.1: Session Auto-Save on Background

Status: done

## Story

As a user,
I want my session to be automatically saved when I leave the app,
so that I don't lose my work if I receive a phone call.

## Acceptance Criteria

1. Given a tracing session is active with an overlay image, position, opacity, and settings, when the app goes to background (home button, phone call, app switch), then the full session state is saved to DataStore: image URI, overlay transform (position, scale, rotation), opacity, color tint, inverted mode, time-lapse progress
2. The save completes before the Activity is destroyed (FR30) via `withContext(NonCancellable)`

## Tasks / Subtasks

- [x] Task 1: Create SessionData model (AC: #1)
  - [x] Data class with 16 fields covering full tracing state
  - [x] Computed `hasSavedOverlay` property
- [x] Task 2: Create SessionRepository interface (AC: #1)
  - [x] `sessionData: Flow<SessionData>`, `save()`, `clear()`
- [x] Task 3: Implement DataStoreSessionRepository (AC: #1, #2)
  - [x] 16 typed preference keys (incl. schema version)
  - [x] Single `dataStore.edit {}` transaction for atomic writes
- [x] Task 4: Add save triggers in TracingViewModel (AC: #1, #2)
  - [x] Eager save on `onImageSelected()`, `onToggleSession()`
  - [x] Debounced save (500ms) on gesture/opacity/tint/mode changes
  - [x] `NonCancellable` wrapper to survive scope cancellation
  - [x] `restoreCompleted` guard to prevent save-before-restore race
- [x] Task 5: Wire DI — SessionModule + TracingModule injection
- [x] Task 6: Write unit tests (7 core + 5 ViewModel)

## Dev Notes

### Architecture Patterns

- **DataStore Preferences** chosen over Room/Proto for simplicity — flat key-value pairs suffice for session state
- **Eager + debounced save strategy:** critical state changes (image selection, session toggle) save immediately; frequent gesture events debounce at 500ms to prevent DataStore thrashing
- **NonCancellable:** `saveSession()` wraps `repo.save()` in `withContext(NonCancellable)` so writes complete even during `ON_STOP` when `viewModelScope` is being cancelled
- **Race condition guard:** `saveSession()` checks `if (!restoreCompleted) return` to prevent overwriting persisted data with defaults before restoration finishes

### Critical Bug Fixed

**Silent null DI (commit `b95003f`):** `SessionRepository?` constructor param with default `null` meant Koin happily constructed the ViewModel without injecting the repository. All persistence was silently a no-op. Fix: explicit `sessionRepository = get()` in TracingModule.

### Project Structure Notes

- `core/session/` module: `SessionData.kt`, `SessionRepository.kt`, `DataStoreSessionRepository.kt`, `di/SessionModule.kt`
- `feature/tracing/TracingViewModel.kt`: `saveSession()`, `debounceSave()`, `restoreCompleted` flag
- DI: `sessionModule` registered in `TraceGlassApp.kt`, injected via `TracingModule.kt`

### References

- [Source: core/session/src/main/kotlin/.../SessionData.kt] — data model with 15 fields
- [Source: core/session/src/main/kotlin/.../DataStoreSessionRepository.kt] — DataStore implementation
- [Source: feature/tracing/src/main/kotlin/.../TracingViewModel.kt] — save triggers, debounce, NonCancellable
- [Source: core/session/src/main/kotlin/.../di/SessionModule.kt] — Koin DI module

## Dev Agent Record

### Agent Model Used

Claude Opus 4 (claude-opus-4-20250514)

### Debug Log References

- Commit `b95003f`: fix inject sessionRepository in DI (was silently null)

### Completion Notes List

- 7 core unit tests (SessionData defaults, FakeRepository save/clear/counter)
- 5 ViewModel tests (save blocking, immediate save, debounce coalescing, session toggle save)
- Key lesson: avoid nullable default parameters for DI-injected dependencies

### File List

- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/SessionData.kt (created)
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/SessionRepository.kt (created)
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/DataStoreSessionRepository.kt (created)
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/di/SessionModule.kt (created)
- core/session/build.gradle.kts (modified — added datastore dependency)
- feature/tracing/src/main/kotlin/.../TracingViewModel.kt (modified — saveSession, debounceSave)
- feature/tracing/src/main/kotlin/.../di/TracingModule.kt (modified — sessionRepository = get())
- app/src/main/kotlin/.../TraceGlassApp.kt (modified — added sessionModule)
- core/session/src/test/kotlin/.../SessionRepositoryTest.kt (created — 7 tests)
- core/session/src/test/kotlin/.../FakeSessionRepository.kt (created)
- feature/tracing/src/test/kotlin/.../FakeSessionRepository.kt (created)
- feature/tracing/src/test/kotlin/.../TracingViewModelTest.kt (modified — 5 session tests)
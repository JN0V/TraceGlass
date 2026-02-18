# Story 5.2: Session Auto-Restore on Foreground

Status: done

## Story

As a user,
I want my session to be seamlessly restored when I return to the app,
so that I can continue tracing exactly where I left off.

## Acceptance Criteria

1. Given a session was saved when the app went to background, when the app returns to foreground (same process), then the overlay image, position, scale, rotation, opacity, color tint, and inverted mode are restored exactly as they were (FR31), and the camera feed resumes automatically
2. Given a session was saved but the app was killed (cold start / process death), when the app is relaunched, then a "Resume session?" dialog is shown; if accepted, all settings are restored and the image is loaded from internal storage copy; if declined, saved session is cleared
3. Given the saved image URI is no longer accessible, when session restore is attempted, then the app shows the camera feed without overlay and a snackbar informs "Reference image no longer available"

## Tasks / Subtasks

- [x] Task 1: Add lifecycle observer in TracingScreen (AC: #1)
  - [x] `DisposableEffect(lifecycleOwner)` with `LifecycleEventObserver`
  - [x] ON_STOP triggers `saveSession()`
  - [x] ON_RESUME triggers `cameraManager.reapplyZoom()`
- [x] Task 2: Implement `restoreSession()` in TracingViewModel (AC: #1, #2)
  - [x] Read saved `SessionData` from repository
  - [x] `restoreAttempted` flag for idempotent restore (one-shot)
  - [x] `pendingRestoreData` to hold data until user decides
- [x] Task 3: Add resume dialog flow (AC: #2)
  - [x] `showResumeSessionDialog: Boolean` in TracingUiState
  - [x] `onResumeSessionAccepted()` — apply all saved state, set `restoreCompleted = true`
  - [x] `onResumeSessionDeclined()` — clear saved data, set `restoreCompleted = true`
  - [x] AlertDialog in TracingScreen
- [x] Task 4: Image persistence across process death (AC: #2, #3)
  - [x] `ImageFileHelper.copyImageToInternal()` — copies content:// to file:// in filesDir
  - [x] Save file:// URI to DataStore (survives process death)
  - [x] Photo picker callback calls `ImageFileHelper` before `onImageSelected()`
- [x] Task 5: Add `overlayRotation` to SessionData + DataStore keys (AC: #1)
- [x] Task 6: Write unit tests (5 ViewModel + 6 ImageFileHelper)

## Dev Notes

### Architecture Patterns

- **Resume dialog on cold start:** `restoreSession()` does NOT auto-apply saved state. It stores data in `pendingRestoreData` and sets `showResumeSessionDialog = true`. User must explicitly accept or decline.
- **Idempotent restore:** `restoreAttempted` flag prevents re-triggering when navigating back from Settings screen (LaunchedEffect runs again on recomposition).
- **Image copy to internal storage:** Android Photo Picker `content://` URIs have temporary read permission lost on process death. `ImageFileHelper` copies bytes to `context.filesDir/reference_image.{ext}` and returns a permanent `file://` URI.
- **MIME-based extension:** `ImageFileHelper` reads content resolver MIME type to pick `.jpg`, `.png`, or `.webp` extension.

### Critical Bug Fixed

**Content URI expiry (commit `0ddea2f`):** After process death, `content://` URIs from Photo Picker become inaccessible (`SecurityException`). Without the internal copy, restored sessions would always fail to load the image. The `ImageFileHelper` pattern ensures the reference image survives any lifecycle event.

### Project Structure Notes

- `feature/tracing/TracingScreen.kt`: lifecycle observer, resume dialog AlertDialog, photo picker → ImageFileHelper
- `feature/tracing/TracingUiState.kt`: added `showResumeSessionDialog`
- `feature/tracing/TracingViewModel.kt`: `restoreSession()`, `pendingRestoreData`, `restoreAttempted`, accept/decline handlers
- `feature/tracing/ImageFileHelper.kt`: static `copyImageToInternal()` utility

### References

- [Source: feature/tracing/src/main/kotlin/.../TracingScreen.kt] — lifecycle observer, resume dialog
- [Source: feature/tracing/src/main/kotlin/.../TracingViewModel.kt] — restoreSession, accept/decline
- [Source: feature/tracing/src/main/kotlin/.../ImageFileHelper.kt] — content:// to file:// copy
- [Source: feature/tracing/src/main/kotlin/.../TracingUiState.kt] — showResumeSessionDialog

## Dev Agent Record

### Agent Model Used

Claude Opus 4 (claude-opus-4-20250514)

### Debug Log References

- Commit `0ddea2f`: fix robust session persistence with resume dialog on cold start

### Completion Notes List

- 5 ViewModel tests (full save/kill/restore cycle, decline clears data, no saved data keeps defaults, idempotent restore, lock/viewport persistence)
- 6 ImageFileHelper tests (copy bytes, null input, IOException, SecurityException, overwrite, MIME extension)
- Key lesson: Photo Picker content:// URIs expire on process death — always copy to internal storage

### File List

- feature/tracing/src/main/kotlin/.../TracingScreen.kt (modified — lifecycle observer, resume dialog)
- feature/tracing/src/main/kotlin/.../TracingUiState.kt (modified — showResumeSessionDialog)
- feature/tracing/src/main/kotlin/.../TracingViewModel.kt (modified — restoreSession, accept/decline)
- feature/tracing/src/main/kotlin/.../ImageFileHelper.kt (created — copyImageToInternal)
- core/session/src/main/kotlin/.../SessionData.kt (modified — added overlayRotation field)
- core/session/src/main/kotlin/.../DataStoreSessionRepository.kt (modified — KEY_ROTATION)
- feature/tracing/src/test/kotlin/.../TracingViewModelTest.kt (modified — 5 restore tests)
- feature/tracing/src/test/kotlin/.../ImageFileHelperTest.kt (created — 6 tests)
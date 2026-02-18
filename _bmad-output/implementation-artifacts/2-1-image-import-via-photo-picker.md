# Story 2.1: Image Import via Photo Picker

Status: done

## Story

As a user,
I want to pick an image from my gallery to use as a tracing reference,
so that I can overlay any image I want onto my drawing surface.

## Acceptance Criteria

1. **Given** the camera feed is active and no overlay image is loaded **When** the user taps the image picker button **Then** the system Photo Picker (API 33+) opens, and no storage permission is requested
2. **Given** the Photo Picker is open **When** the user selects an image **Then** the image is loaded into memory and displayed as an overlay on the camera feed, the image is copied to internal app storage only for session persistence (no external storage permission required), and the overlay appears centered on the screen with default 50% opacity
3. **Given** the Photo Picker is open **When** the user cancels without selecting **Then** the app returns to the camera feed unchanged

## Tasks / Subtasks

- [x] Task 1: Integrate Android Photo Picker (AC: 1)
  - [x] 1.1 Add `ActivityResultContracts.PickVisualMedia()` launcher in TracingScreen
  - [x] 1.2 Filter for images only (`PickVisualMedia.ImageOnly`)
  - [x] 1.3 No storage permission requested (Photo Picker handles access)

- [x] Task 2: Handle image selection in ViewModel (AC: 2, 3)
  - [x] 2.1 `onImageSelected(uri: Uri?)` in TracingViewModel updates `overlayImageUri` in UiState
  - [x] 2.2 Null URI (cancel) leaves state unchanged
  - [x] 2.3 Default opacity set to 0.5f on first load
  - [x] 2.4 Image URI persisted via SessionRepository

- [x] Task 3: Display overlay image on camera feed (AC: 2)
  - [x] 3.1 Conditional rendering in TracingContent when `overlayImageUri != null`
  - [x] 3.2 Image centered on screen
  - [x] 3.3 Semi-transparent (default 50% opacity)

- [x] Task 4: Image persistence for session restore (AC: 2)
  - [x] 4.1 `ImageFileHelper.copyImageToInternal()` copies picked image to internal cache
  - [x] 4.2 Returns `Uri.fromFile()` for stable reference across app restarts
  - [x] 4.3 Handles ContentResolver exceptions gracefully

- [x] Task 5: Unit tests
  - [x] 5.1 Test image selection updates UiState
  - [x] 5.2 Test null selection leaves state unchanged
  - [x] 5.3 Test image replacement updates URI
  - [x] 5.4 Test session persistence includes image URI

### Review Follow-ups (AI)

- [x] [AI-Review][Med] Update AC 2 text: replace "image is not copied to app storage (NFR12)" with "image is copied to internal app storage only for session persistence; no external storage permission required" — current text contradicts Task 4 and actual implementation
- [x] [AI-Review][Med] Add unit tests for `ImageFileHelper.copyImageToInternal()` — currently zero test coverage on this utility (needs Robolectric or mock Context)

## Dev Notes

### Architecture Requirements

- **Module:** `:feature:tracing`
- **Photo Picker:** `ActivityResultContracts.PickVisualMedia()` — no permission needed (API 33+)
- **NFR12:** Images processed in memory only — but for session persistence (Epic 5), `ImageFileHelper` copies to internal cache
- **State:** `TracingUiState.overlayImageUri: Uri?`

### Implementation Details

- Photo picker launched from `TracingScreen.kt` (lines 67-74)
- `onImageSelected()` in TracingViewModel (lines 104-108)
- `ImageFileHelper.copyImageToInternal()` copies to app internal storage for session restore
- Session persistence saves URI string via `SessionRepository`

### References

- [Source: epics.md — Epic 2, Story 2.1]
- [Source: prd.md — FR2, NFR12]
- [Source: ux-design-specification.md — Journey 2: Photo Picker flow]

## Dev Agent Record

### Agent Model Used

(Pre-BMAD implementation); Review follow-ups: Claude Opus 4.6

### Debug Log References

### Completion Notes List

- Implemented before BMAD workflow adoption
- Story file created retroactively for audit purposes
- ✅ Resolved review finding [Med]: NFR12 AC text contradiction — Updated AC 2 to reflect that image is copied to internal storage for session persistence
- ✅ Resolved review finding [Med]: ImageFileHelper zero test coverage — Added 5 unit tests (ImageFileHelperTest.kt) covering: successful copy, null InputStream, IOException, SecurityException, overwrite existing file

## Senior Developer Review (AI)

**Review Date:** 2026-02-17
**Reviewer:** Amelia (Dev Agent — Retroactive Audit)
**Outcome:** Changes Requested

### Summary

Story functionally complete. 2 action items found during retroactive audit.

### Action Items

- [x] [Med] **NFR12 AC text contradiction** — AC 2 says "not copied to app storage" but `ImageFileHelper.copyImageToInternal()` explicitly copies to `filesDir/reference_image`. Dev Notes already acknowledge this trade-off for session persistence (Epic 5). Update AC 2 text to match reality. File: `2-1-image-import-via-photo-picker.md` AC 2
- [x] [Med] **ImageFileHelper has zero test coverage** — `copyImageToInternal()` is an `object` with static method taking real `Context`. Needs Robolectric or instrumented test. Handles exceptions gracefully but no verification exists. File: `feature/tracing/.../ImageFileHelper.kt`

### File List

- feature/tracing/src/main/kotlin/.../TracingScreen.kt (photo picker launcher)
- feature/tracing/src/main/kotlin/.../TracingViewModel.kt (onImageSelected)
- feature/tracing/src/main/kotlin/.../TracingContent.kt (conditional overlay rendering)
- feature/tracing/src/main/kotlin/.../TracingUiState.kt (overlayImageUri field)
- feature/tracing/src/main/kotlin/.../ImageFileHelper.kt (copy to internal storage)
- feature/tracing/src/test/kotlin/.../TracingViewModelTest.kt (ImageImport test group)
- feature/tracing/src/test/kotlin/.../ImageFileHelperTest.kt (NEW — unit tests for copyImageToInternal)

## Change Log

- 2026-02-17: Addressed code review findings — 2 items resolved
  - Updated AC 2 text to remove NFR12 contradiction (image is copied to internal storage for session persistence)
  - Added ImageFileHelperTest with 5 unit tests covering success path, null stream, IOException, SecurityException, and file overwrite

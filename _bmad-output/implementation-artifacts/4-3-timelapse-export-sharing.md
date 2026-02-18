# Story 4.3: Time-lapse Export & Sharing

Status: done

## Story

As a user,
I want to save and share my time-lapse video,
so that I can show my friends how my drawing was made.

## Acceptance Criteria

1. **Given** an MP4 video has been compiled **When** the video is saved **Then** it is saved to the device shared Movies/ directory via MediaStore (NFR13) **And** no storage permission is required (API 33+)

2. **Given** a time-lapse video exists **When** the user taps the share button **Then** the system share intent opens with the video file **And** the user can share via any installed app (WhatsApp, etc.)

## Tasks / Subtasks

- [x] Task 1: Wire VideoExporter and VideoSharer into DI (AC: #1, #2)
  - [x] 1.1 Ensure `VideoExporter` → `MediaStoreVideoExporter(context)` binding exists in `TimelapseModule.kt` (from Story 4-1)
  - [x] 1.2 Ensure `VideoSharer(context)` binding exists in `TimelapseModule.kt`
  - [x] 1.3 Inject both into TracingViewModel via Koin

- [x] Task 2: Implement export flow in ViewModel (AC: #1)
  - [x] 2.1 Add `exportTimelapse()` method — calls `videoExporter.exportToGallery(videoFile, displayName)` on Dispatchers.IO
  - [x] 2.2 Generate display name with timestamp: `"TraceGlass_YYYYMMDD_HHmmss.mp4"`
  - [x] 2.3 Handle `ExportResult.Success(uri)` → store URI in ViewModel state, show success snackbar "Video saved to Movies/TraceGlass/"
  - [x] 2.4 Handle `ExportResult.Error` → show error snackbar with message
  - [x] 2.5 Add `isExporting: Boolean` to `TracingUiState` for loading indicator

- [x] Task 3: Implement share flow in ViewModel (AC: #2)
  - [x] 3.1 Add `shareTimelapse()` method — calls `videoSharer.share(videoUri)` using the URI from export step
  - [x] 3.2 The VideoSharer creates an `Intent.ACTION_SEND` with `video/mp4` MIME type and `FLAG_GRANT_READ_URI_PERMISSION`
  - [x] 3.3 If no export URI available, export first then share

- [x] Task 4: Post-compilation UI (AC: #1, #2)
  - [x] 4.1 After compilation success, show a dialog/bottom sheet with options: "Save to Gallery", "Share", "Discard"
  - [x] 4.2 "Save to Gallery" triggers export, shows progress, then confirms
  - [x] 4.3 "Share" triggers export (if not yet done) then share intent
  - [x] 4.4 "Discard" deletes the temp MP4 from cacheDir
  - [x] 4.5 After export/share, clean up temporary snapshot files from `filesDir/timelapse/`

- [x] Task 5: Unit tests (AC: #1, #2)
  - [x] 5.1 Test export flow calls VideoExporter with correct display name format
  - [x] 5.2 Test share flow opens intent with correct MIME type
  - [x] 5.3 Test error handling for export failures
  - [x] 5.4 Test temp file cleanup after successful export
  - [x] 5.5 Use fake/mock implementations for VideoExporter and VideoSharer

## Dev Notes

### Existing Implementation (Already Done — DO NOT Rewrite)

| Class | File | Tests | Description |
|-------|------|-------|-------------|
| `VideoExporter` | `VideoExporter.kt` | — | Interface: `suspend exportToGallery(file, displayName): ExportResult` |
| `MediaStoreVideoExporter` | `MediaStoreVideoExporter.kt` | 3 tests | Exports to `Movies/TraceGlass/` via MediaStore with `IS_PENDING` atomic write |
| `ExportResult` | `ExportResult.kt` | — | Sealed class: Success(uri) / Error(message) |
| `VideoSharer` | `VideoSharer.kt` | — | Opens system share intent with `ACTION_SEND`, `video/mp4`, read permission grant |

**DO NOT** recreate these files. They are production-ready.

### What's Missing (The Actual Work)

1. **DI bindings** in `TimelapseModule.kt` for `VideoExporter` and `VideoSharer`
2. **ViewModel methods** — `exportTimelapse()` and `shareTimelapse()`
3. **Post-compilation UI** — dialog/bottom sheet with save/share/discard options
4. **Temp file cleanup** — delete snapshot JEGs and temp MP4 after export

### Architecture Constraints

- **No storage permission needed**: API 33+ allows `MediaStore.Video` writes without permission [Source: architecture.md#Technical-Constraints]
- **Export path**: `Movies/TraceGlass/` via MediaStore (NFR13 — user expects shared Movies directory)
- **No INTERNET permission**: Sharing uses system intent, not direct upload [Source: architecture.md — NFR10]
- **MediaStoreVideoExporter** uses `IS_PENDING` flag for atomic writes — file only becomes visible to gallery apps after write completes
- **VideoSharer** needs a `content://` URI (from MediaStore export), not a `file://` URI

### FileProvider Note

The `VideoSharer` uses `FLAG_GRANT_READ_URI_PERMISSION`. If sharing the cached MP4 file before MediaStore export, you'd need a FileProvider configured in `AndroidManifest.xml`. However, the recommended flow is: export to MediaStore first (gets a `content://` URI), then share that URI. This avoids needing FileProvider.

### Dependency on Stories 4-1 and 4-2

- Story 4-1: `TimelapseModule.kt` must exist
- Story 4-2: Compilation must produce an MP4 file stored in ViewModel state

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-4.3]
- [Source: _bmad-output/planning-artifacts/architecture.md#Data-Persistence] — MediaStore for shared storage
- [Source: _bmad-output/planning-artifacts/architecture.md#Technical-Constraints] — No INTERNET, MediaStore API
- [Source: feature/timelapse/src/main/kotlin/.../MediaStoreVideoExporter.kt] — Movies/TraceGlass/ export
- [Source: feature/timelapse/src/main/kotlin/.../VideoSharer.kt] — ACTION_SEND intent

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

No blocking issues encountered during implementation.

### Completion Notes List

- **Task 1**: DI bindings for `VideoExporter` and `VideoSharer` already existed in `TimelapseModule.kt` (from Story 4-1). Added `videoExporter` and `videoSharer` constructor params to `TracingViewModel` and wired them in `TracingModule.kt`.
- **Task 2**: Added `exportTimelapse()` method to `TracingViewModel` — calls `videoExporter.exportToGallery()` on IO dispatcher, generates `TraceGlass_YYYYMMDD_HHmmss.mp4` display name, handles success (stores URI, shows snackbar) and error (shows error snackbar). Added `isExporting`, `exportedVideoUri`, `exportSuccessMessage`, `exportError` fields to `TracingUiState`.
- **Task 3**: Added `shareTimelapse()` method — if URI already exists from prior export, calls `videoSharer.shareVideo()` directly. If no URI, exports first then shares. Both branches dismiss the post-compilation dialog.
- **Task 4**: Added post-compilation `AlertDialog` with "Save to Gallery", "Share", and "Discard" buttons. Dialog shows `CircularProgressIndicator` during export. `discardTimelapse()` deletes temp MP4 and cleans snapshot storage. `cleanupTempFiles()` calls `snapshotStorage.clear()` after successful export.
- **Task 5**: Added 12 unit tests in `ExportAndShare` nested class: display name format validation, export success/error handling, share with/without prior export, discard + file deletion, temp file cleanup, snackbar message clearing, and no-op safety tests (no exporter, no compiled video). Used `FakeVideoExporter` and `FakeVideoSharer` (mockk-based).
- **Full regression**: 347 tests pass, 0 failures across all modules.

### Change Log

- 2026-02-18: Story 4.3 implementation complete — export/share/discard flow wired end-to-end, 12 new tests, 347 total pass
- 2026-02-18: Code review #1 — 10 findings (2H, 5M, 3L). H1+H2+L2 already fixed by 4-2 review. Fixed: M1 (duplicated export → `performExport(shareAfter)`), M2 (async discard file deletion on IO), M5 (accessibility contentDescription on progress indicator), L1 (SimpleDateFormat → DateTimeFormatter). M3+M4 (File List updated). L3 deferred (3-button AlertDialog layout). 172 tests pass.

### File List

- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt (modified — added videoExporter/videoSharer params, exportTimelapse, shareTimelapse, discardTimelapse, cleanupTempFiles methods)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingUiState.kt (modified — added isExporting, exportedVideoUri, exportSuccessMessage, exportError, showPostCompilationDialog fields)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingContent.kt (modified — added post-compilation dialog, export snackbars, new callback params)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingScreen.kt (modified — wired new export/share UI state and callbacks)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/di/TracingModule.kt (modified — added videoExporter=get(), videoSharer=get() to ViewModel DI)
- feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModelTest.kt (modified — added ExportAndShare nested class with 12 tests, FakeVideoExporter, FakeVideoSharer, updated createViewModel helper)
- feature/timelapse/src/main/kotlin/io/github/jn0v/traceglass/feature/timelapse/di/TimelapseModule.kt (modified — VideoSharer binding added for DI)
- app/src/main/kotlin/io/github/jn0v/traceglass/TraceGlassApp.kt (modified — timelapseModule added to Koin modules list)

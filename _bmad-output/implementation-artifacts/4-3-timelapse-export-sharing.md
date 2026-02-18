# Story 4.3: Time-lapse Export & Sharing

Status: ready-for-dev

## Story

As a user,
I want to save and share my time-lapse video,
so that I can show my friends how my drawing was made.

## Acceptance Criteria

1. **Given** an MP4 video has been compiled **When** the video is saved **Then** it is saved to the device shared Movies/ directory via MediaStore (NFR13) **And** no storage permission is required (API 33+)

2. **Given** a time-lapse video exists **When** the user taps the share button **Then** the system share intent opens with the video file **And** the user can share via any installed app (WhatsApp, etc.)

## Tasks / Subtasks

- [ ] Task 1: Wire VideoExporter and VideoSharer into DI (AC: #1, #2)
  - [ ] 1.1 Ensure `VideoExporter` → `MediaStoreVideoExporter(context)` binding exists in `TimelapseModule.kt` (from Story 4-1)
  - [ ] 1.2 Ensure `VideoSharer(context)` binding exists in `TimelapseModule.kt`
  - [ ] 1.3 Inject both into TracingViewModel via Koin

- [ ] Task 2: Implement export flow in ViewModel (AC: #1)
  - [ ] 2.1 Add `exportTimelapse()` method — calls `videoExporter.exportToGallery(videoFile, displayName)` on Dispatchers.IO
  - [ ] 2.2 Generate display name with timestamp: `"TraceGlass_YYYYMMDD_HHmmss.mp4"`
  - [ ] 2.3 Handle `ExportResult.Success(uri)` → store URI in ViewModel state, show success snackbar "Video saved to Movies/TraceGlass/"
  - [ ] 2.4 Handle `ExportResult.Error` → show error snackbar with message
  - [ ] 2.5 Add `isExporting: Boolean` to `TracingUiState` for loading indicator

- [ ] Task 3: Implement share flow in ViewModel (AC: #2)
  - [ ] 3.1 Add `shareTimelapse()` method — calls `videoSharer.share(videoUri)` using the URI from export step
  - [ ] 3.2 The VideoSharer creates an `Intent.ACTION_SEND` with `video/mp4` MIME type and `FLAG_GRANT_READ_URI_PERMISSION`
  - [ ] 3.3 If no export URI available, export first then share

- [ ] Task 4: Post-compilation UI (AC: #1, #2)
  - [ ] 4.1 After compilation success, show a dialog/bottom sheet with options: "Save to Gallery", "Share", "Discard"
  - [ ] 4.2 "Save to Gallery" triggers export, shows progress, then confirms
  - [ ] 4.3 "Share" triggers export (if not yet done) then share intent
  - [ ] 4.4 "Discard" deletes the temp MP4 from cacheDir
  - [ ] 4.5 After export/share, clean up temporary snapshot files from `filesDir/timelapse/`

- [ ] Task 5: Unit tests (AC: #1, #2)
  - [ ] 5.1 Test export flow calls VideoExporter with correct display name format
  - [ ] 5.2 Test share flow opens intent with correct MIME type
  - [ ] 5.3 Test error handling for export failures
  - [ ] 5.4 Test temp file cleanup after successful export
  - [ ] 5.5 Use fake/mock implementations for VideoExporter and VideoSharer

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

### Debug Log References

### Completion Notes List

### File List

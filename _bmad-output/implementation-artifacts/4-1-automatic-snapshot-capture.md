# Story 4.1: Automatic Snapshot Capture

Status: ready-for-dev

## Story

As a user,
I want the app to automatically capture snapshots of my drawing progress,
so that a time-lapse video can be created from my tracing session.

## Acceptance Criteria

1. **Given** a tracing session is active **When** time-lapse is recording **Then** periodic snapshots are captured automatically (every 5 seconds) **And** snapshots are saved as JPEG files in `context.filesDir/timelapse/` **And** capture does not drop the camera feed below 20 fps (NFR1)

2. **Given** time-lapse is recording **When** the user pauses time-lapse (FR29) **Then** snapshot capture pauses **And** a visual indicator shows time-lapse is paused

3. **Given** time-lapse is paused **When** the user resumes time-lapse **Then** snapshot capture resumes from where it left off

## Tasks / Subtasks

- [ ] Task 1: Create Koin DI module for timelapse (AC: all)
  - [ ] 1.1 Create `feature/timelapse/src/main/kotlin/io/github/jn0v/traceglass/feature/timelapse/di/TimelapseModule.kt`
  - [ ] 1.2 Declare bindings: `TimelapseSession`, `FileSnapshotStorage` (with `context.filesDir` base dir), `TimelapseCompiler` → `MediaCodecCompiler`, `VideoExporter` → `MediaStoreVideoExporter`, `VideoSharer`
  - [ ] 1.3 Register `timelapseModule` in `TraceGlassApp.kt` alongside existing modules

- [ ] Task 2: Add JPEG frame capture to FrameAnalyzer (AC: #1)
  - [ ] 2.1 Add a `snapshotCallback: ((ByteArray) -> Unit)?` property to FrameAnalyzer (nullable, default null)
  - [ ] 2.2 In `analyze()`, when callback is set, encode the current `ImageProxy` to JPEG bytes using YUV→Bitmap→compress pipeline
  - [ ] 2.3 Ensure JPEG encoding runs only when timelapse is recording (callback non-null) — gated by ViewModel
  - [ ] 2.4 **CRITICAL**: Respect `rowStride` padding when converting YUV buffer (see MEMORY.md — bug was dormant at 640x480)
  - [ ] 2.5 Run JPEG compression on Dispatchers.IO to avoid blocking the camera analysis thread

- [ ] Task 3: Integrate TimelapseSession into TracingViewModel (AC: #1, #2, #3)
  - [ ] 3.1 Inject `TimelapseSession` and `FileSnapshotStorage` via Koin into TracingViewModel
  - [ ] 3.2 Add timelapse state fields to `TracingUiState`: `isTimelapseRecording: Boolean`, `isTimelapsePaused: Boolean`, `snapshotCount: Int`
  - [ ] 3.3 Implement `startTimelapse()` — sets FrameAnalyzer snapshot callback, calls `timelapseSession.start(viewModelScope)` with capture callback that saves JPEG via FileSnapshotStorage
  - [ ] 3.4 Implement `pauseTimelapse()` / `resumeTimelapse()` — delegates to TimelapseSession, clears/re-sets snapshot callback
  - [ ] 3.5 Implement `stopTimelapse()` — clears callback, stops session
  - [ ] 3.6 Expose snapshot count via StateFlow (observe `timelapseSession.snapshotCount`)

- [ ] Task 4: Add timelapse UI controls (AC: #1, #2, #3)
  - [ ] 4.1 Add record/pause/stop button to ExpandableMenu (or as a new FAB near existing controls)
  - [ ] 4.2 Show snapshot count badge when recording (e.g. "📷 12")
  - [ ] 4.3 Show recording indicator (red dot or similar) when timelapse is active
  - [ ] 4.4 Show paused indicator when timelapse is paused
  - [ ] 4.5 Timelapse controls only visible when a tracing session is active

- [ ] Task 5: Unit tests (AC: all)
  - [ ] 5.1 Test ViewModel timelapse state transitions (start → recording → pause → resume → stop)
  - [ ] 5.2 Test FrameAnalyzer snapshot callback invocation
  - [ ] 5.3 Test that timelapse controls are hidden when no session is active
  - [ ] 5.4 Verify Koin module resolves all timelapse dependencies

## Dev Notes

### Existing Implementation (Already Done — DO NOT Rewrite)

The following classes already exist in `feature/timelapse/` with full unit test coverage:

| Class | File | Tests | Description |
|-------|------|-------|-------------|
| `TimelapseSession` | `TimelapseSession.kt` | 21 tests | State machine (IDLE/RECORDING/PAUSED), periodic capture callback, `restoreFromExisting()` |
| `FileSnapshotStorage` | `FileSnapshotStorage.kt` | 9 tests | Saves JPEG to `{baseDir}/timelapse/snapshot_XXXX.jpg`, enumeration, clear |
| `TimelapseState` | `TimelapseState.kt` | — | Enum: IDLE, RECORDING, PAUSED |
| `SnapshotStorage` | `SnapshotStorage.kt` | — | Interface for FileSnapshotStorage |

**DO NOT** recreate or modify these files unless absolutely necessary for integration. They are production-ready.

### What's Missing (The Actual Work)

1. **Koin DI module** (`TimelapseModule.kt`) — does not exist yet
2. **TraceGlassApp.kt** registration — `timelapseModule` not in `modules(...)` call
3. **FrameAnalyzer JPEG capture** — currently only does marker detection
4. **TracingViewModel integration** — no timelapse fields or methods
5. **UI controls** — no recording/pause/stop buttons

### Architecture Constraints

- **Threading**: Snapshot JPEG encoding on `Dispatchers.IO`, camera analysis on its own executor [Source: architecture.md#Threading-Model]
- **Storage**: JPEG files in `context.filesDir/timelapse/` (NOT external storage) [Source: architecture.md#Data-Persistence]
- **DI pattern**: Follow existing module pattern — see `CameraModule.kt`, `TracingModule.kt` for examples
- **Feature modules never depend on each other**: `feature:timelapse` → `core:camera`, `core:session` only [Source: architecture.md#Module-Dependencies]
- **No per-frame allocation**: Use buffer recycling for JPEG encoding if possible [Source: architecture.md#Additional-Requirements]

### Project Structure Notes

- Koin module file: `feature/timelapse/src/main/kotlin/io/github/jn0v/traceglass/feature/timelapse/di/TimelapseModule.kt`
- DI registration: `app/src/main/kotlin/io/github/jn0v/traceglass/TraceGlassApp.kt` line 17
- Current modules registered: `cameraModule, cvModule, sessionModule, onboardingModule, tracingModule`
- `SessionData` already has `timelapseSnapshotCount: Int = 0` field in `core/session/SessionData.kt`

### YUV→JPEG Conversion Warning

**CRITICAL**: `ImageProxy.planes[0].buffer` may have `rowStride != width` (e.g. 832 vs 800 on some devices). When converting YUV to Bitmap for JPEG compression, use `ImageProxy.toBitmap()` (available on CameraX 1.3+) which handles stride correctly, OR manually handle row stride padding. Do NOT assume contiguous pixel data.

### Previous Story Intelligence

- Epic 4 code was committed 2026-02-08 as isolated implementations
- All 40 unit tests pass across the timelapse module
- The code follows project conventions (sealed classes, suspend functions, Dispatchers.IO)
- `MediaCodecCompiler` uses `lockCanvas()` + `drawBitmap()` on input surface (correct for Surface-based H.264 encoding)

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-4.1]
- [Source: _bmad-output/planning-artifacts/architecture.md#Data-Persistence] — JPEG in `context.filesDir/timelapse/`
- [Source: _bmad-output/planning-artifacts/architecture.md#Threading-Model] — Dispatchers.IO for file I/O
- [Source: _bmad-output/planning-artifacts/architecture.md#Module-Dependencies] — `feature:timelapse` → `core:camera`, `core:session`
- [Source: _bmad-output/planning-artifacts/architecture.md#Camera-Pipeline] — ImageAnalysis + PreviewView

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

# Story 4.2: MP4 Video Compilation

Status: ready-for-dev

## Story

As a user,
I want my snapshots compiled into a time-lapse video,
so that I can watch my drawing appear in fast-forward.

## Acceptance Criteria

1. **Given** a tracing session has captured snapshots **When** the user stops the session or requests compilation **Then** snapshots are compiled into an MP4 video using MediaCodec **And** compilation completes within 2x session duration (NFR5) **And** the video plays correctly on standard Android video players **And** compilation runs on a background thread (Dispatchers.IO)

## Tasks / Subtasks

- [ ] Task 1: Wire MediaCodecCompiler into DI and ViewModel (AC: #1)
  - [ ] 1.1 Ensure `TimelapseCompiler` → `MediaCodecCompiler` binding exists in `TimelapseModule.kt` (created in Story 4-1)
  - [ ] 1.2 Inject `TimelapseCompiler` into TracingViewModel via Koin
  - [ ] 1.3 Add compilation state to `TracingUiState`: `isCompiling: Boolean`, `compilationProgress: Float` (0..1)

- [ ] Task 2: Trigger compilation on session stop (AC: #1)
  - [ ] 2.1 In `stopTimelapse()`, after stopping the session, check if `snapshotCount > 0`
  - [ ] 2.2 Launch compilation coroutine on `Dispatchers.IO` via `viewModelScope.launch`
  - [ ] 2.3 Call `timelapseCompiler.compile(snapshotFiles, outputFile, fps=10, onProgress)` where `outputFile` is in `context.cacheDir/timelapse_output.mp4`
  - [ ] 2.4 Update `compilationProgress` via the `onProgress` callback (flows to UI)
  - [ ] 2.5 Handle `CompilationResult.Success` → store `outputFile` path in ViewModel state for export step
  - [ ] 2.6 Handle `CompilationResult.Error` → show error snackbar, log the error

- [ ] Task 3: Compilation progress UI (AC: #1)
  - [ ] 3.1 Show a progress indicator (LinearProgressIndicator or circular) during compilation
  - [ ] 3.2 Display progress percentage from `compilationProgress` state
  - [ ] 3.3 Disable timelapse start button while compiling
  - [ ] 3.4 Show success confirmation when compilation completes (snackbar or dialog with export/share options)

- [ ] Task 4: Unit tests (AC: #1)
  - [ ] 4.1 Test compilation triggered when stopping timelapse with snapshots > 0
  - [ ] 4.2 Test compilation NOT triggered when stopping with 0 snapshots
  - [ ] 4.3 Test compilation progress updates flow to UI state
  - [ ] 4.4 Test error handling (CompilationResult.Error shows snackbar)
  - [ ] 4.5 Use `FakeTimelapseCompiler` (already exists in test sources) for ViewModel tests

## Dev Notes

### Existing Implementation (Already Done — DO NOT Rewrite)

| Class | File | Tests | Description |
|-------|------|-------|-------------|
| `TimelapseCompiler` | `TimelapseCompiler.kt` | — | Interface: `suspend compile(files, output, fps, onProgress)` |
| `MediaCodecCompiler` | `MediaCodecCompiler.kt` | 7 tests | H.264 Surface encoding, 4Mbps bitrate, 16px alignment, drain loop |
| `CompilationResult` | `CompilationResult.kt` | — | Sealed class: Success(outputFile) / Error(message) |
| `FakeTimelapseCompiler` | `FakeTimelapseCompiler.kt` | — | Test double for ViewModel testing |

**DO NOT** recreate or modify these files. They are production-ready with tests passing.

### What's Missing (The Actual Work)

1. **DI wiring** — `TimelapseCompiler` → `MediaCodecCompiler` in Koin (part of TimelapseModule from Story 4-1)
2. **ViewModel integration** — inject compiler, trigger on stop, expose progress
3. **UI** — progress indicator, success/error feedback

### Architecture Constraints

- **Threading**: Compilation MUST run on `Dispatchers.IO` — it's CPU+IO intensive [Source: architecture.md#Threading-Model]
- **Output location**: Compile to `context.cacheDir` (temporary), export to MediaStore in Story 4-3 [Source: architecture.md#Data-Persistence]
- **MediaCodecCompiler** aligns frame dimensions to 16px boundary — no action needed, already handled
- **NFR5**: Compilation within 2x session duration — the existing `MediaCodecCompiler` uses Surface-based encoding which is hardware-accelerated on most devices

### Dependency on Story 4-1

This story depends on Story 4-1 completing:
- `TimelapseModule.kt` must exist with DI bindings
- `TracingViewModel` must have `TimelapseSession` integration
- `FileSnapshotStorage` must be providing snapshot files

### MediaCodecCompiler Implementation Details

The existing compiler:
- Uses `MediaCodec.createEncoderByType("video/avc")` for H.264
- Creates `InputSurface` for frame submission (Surface-based, not ByteBuffer)
- Loads each JPEG snapshot as Bitmap, draws to Surface canvas
- Drains encoder after each frame + final drain with EOS signal
- Uses `MediaMuxer` to write MP4 container
- Progress reported as `frameIndex / totalFrames`

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-4.2]
- [Source: _bmad-output/planning-artifacts/architecture.md#Threading-Model] — Dispatchers.IO for MP4 export
- [Source: feature/timelapse/src/main/kotlin/.../MediaCodecCompiler.kt] — existing implementation
- [Source: feature/timelapse/src/test/kotlin/.../FakeTimelapseCompiler.kt] — test double

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

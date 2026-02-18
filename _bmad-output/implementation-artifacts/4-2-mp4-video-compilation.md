# Story 4.2: MP4 Video Compilation

Status: done

## Story

As a user,
I want my snapshots compiled into a time-lapse video,
so that I can watch my drawing appear in fast-forward.

## Acceptance Criteria

1. **Given** a tracing session has captured snapshots **When** the user stops the session or requests compilation **Then** snapshots are compiled into an MP4 video using MediaCodec **And** compilation completes within 2x session duration (NFR5) **And** the video plays correctly on standard Android video players **And** compilation runs on a background thread (Dispatchers.IO)

## Tasks / Subtasks

- [x] Task 1: Wire MediaCodecCompiler into DI and ViewModel (AC: #1)
  - [x] 1.1 Ensure `TimelapseCompiler` → `MediaCodecCompiler` binding exists in `TimelapseModule.kt` (created in Story 4-1)
  - [x] 1.2 Inject `TimelapseCompiler` into TracingViewModel via Koin
  - [x] 1.3 Add compilation state to `TracingUiState`: `isCompiling: Boolean`, `compilationProgress: Float` (0..1)

- [x] Task 2: Trigger compilation on session stop (AC: #1)
  - [x] 2.1 In `stopTimelapse()`, after stopping the session, check if `snapshotCount > 0`
  - [x] 2.2 Launch compilation coroutine on `Dispatchers.IO` via `viewModelScope.launch`
  - [x] 2.3 Call `timelapseCompiler.compile(snapshotFiles, outputFile, fps=10, onProgress)` where `outputFile` is in `context.cacheDir/timelapse_output.mp4`
  - [x] 2.4 Update `compilationProgress` via the `onProgress` callback (flows to UI)
  - [x] 2.5 Handle `CompilationResult.Success` → store `outputFile` path in ViewModel state for export step
  - [x] 2.6 Handle `CompilationResult.Error` → show error snackbar, log the error

- [x] Task 3: Compilation progress UI (AC: #1)
  - [x] 3.1 Show a progress indicator (LinearProgressIndicator or circular) during compilation
  - [x] 3.2 Display progress percentage from `compilationProgress` state
  - [x] 3.3 Disable timelapse start button while compiling
  - [x] 3.4 Show success confirmation when compilation completes (snackbar or dialog with export/share options)

- [x] Task 4: Unit tests (AC: #1)
  - [x] 4.1 Test compilation triggered when stopping timelapse with snapshots > 0
  - [x] 4.2 Test compilation NOT triggered when stopping with 0 snapshots
  - [x] 4.3 Test compilation progress updates flow to UI state
  - [x] 4.4 Test error handling (CompilationResult.Error shows snackbar)
  - [x] 4.5 Use `FakeTimelapseCompiler` (already exists in test sources) for ViewModel tests

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

## Scope Creep Notes

> **Reviewer note (code review 2026-02-18):** The implementation includes Story 4-3 (Export & Sharing) code co-mingled with Story 4-2 code in the same files. Specifically:
> - `TracingUiState.kt` contains 4-3 fields: `isExporting`, `exportedVideoUri`, `exportSuccessMessage`, `exportError`, `showPostCompilationDialog`
> - `TracingViewModel.kt` contains 4-3 methods: `exportTimelapse()`, `shareTimelapse()`, `discardTimelapse()`, `onDismissPostCompilationDialog()`, `onExportSuccessShown()`, `onExportErrorShown()`
> - `TracingContent.kt` contains post-compilation dialog (Save/Share/Discard)
> - `TracingViewModelTest.kt` contains `ExportAndShare` nested class (13 tests)
>
> This was likely done because Stories 4-2 and 4-3 were implemented in the same session. The 4-2 code itself is correct and complete. The 4-3 code should be tracked under its own story review.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

- Initial test failure: compilation coroutine launched on `Dispatchers.IO` was not controlled by test dispatcher
- Fix: made IO dispatcher injectable (`ioDispatcher` parameter with default `Dispatchers.IO`), tests pass `testDispatcher`

### Completion Notes List

- **Task 1.1**: `TimelapseCompiler` → `MediaCodecCompiler` binding already existed in `TimelapseModule.kt` — verified, no changes needed
- **Task 1.2**: Added `timelapseCompiler: TimelapseCompiler?` and `cacheDir: File?` constructor params to `TracingViewModel`; updated `TracingModule.kt` to provide `timelapseCompiler = get()` and `cacheDir = androidContext().cacheDir`
- **Task 1.3**: Added `isCompiling`, `compilationProgress`, `compiledVideoPath`, `compilationError` to `TracingUiState`
- **Task 2**: Implemented `compileTimelapse()` private method called from `stopTimelapse()` when `snapshotFiles.isNotEmpty()`. Launches on injectable `ioDispatcher` (defaults to `Dispatchers.IO`). Handles `Success` (stores path) and `Error` (stores error message) results
- **Task 3**: Updated `TimelapseControls` composable to show `LinearProgressIndicator` with percentage text during compilation. Added snackbar feedback for success ("Timelapse video ready!") and error. Controls remain visible during compilation even after session stops
- **Task 4**: Added 7 unit tests in new `Compilation` nested class: initial state, compilation triggered/not triggered, progress flow, error handling, error dismissal, no-compiler guard, concurrent compilation guard. Created local `FakeTimelapseCompiler` in test sources (timelapse module's test fake is not accessible from tracing module tests)
- Added `ioDispatcher` parameter to ViewModel constructor for testability (injectable dispatcher pattern)

### Code Review Fixes (2026-02-18)

- **HIGH #1**: Extracted timelapse interfaces to `:core:timelapse` module — `:feature:tracing` no longer depends on `:feature:timelapse` (architecture violation fixed)
- **HIGH #2**: Added `if (_uiState.value.isCompiling) return` guard to `compileTimelapse()` to prevent concurrent compilations
- **MEDIUM #3**: Documented scope creep (Story 4-3 code mixed in) in this file
- **MEDIUM #4**: Updated File List to reflect all modified/created files
- **MEDIUM #5**: Replaced hardcoded `Dispatchers.Main` with injectable `mainDispatcher` parameter
- **MEDIUM #6**: Replaced deprecated `createTempDir()` with `kotlin.io.path.createTempDirectory().toFile()` in tests

### Change Log

- 2026-02-18: Story 4-2 implementation — wired MediaCodecCompiler into DI/ViewModel, compilation on stop, progress UI, 7 new tests
- 2026-02-18: Code review fixes — architecture violation, concurrent compilation guard, injectable dispatchers, deprecated API replacement

### File List

- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingUiState.kt (modified — added isCompiling, compilationProgress, compiledVideoPath, compilationError)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt (modified — added timelapseCompiler, cacheDir, ioDispatcher, mainDispatcher params; compileTimelapse() with concurrent guard, onCompilationErrorShown(), onCompilationCompleteShown())
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/di/TracingModule.kt (modified — added timelapseCompiler and cacheDir to DI)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingContent.kt (modified — added compilation progress UI in TimelapseControls, snackbar LaunchedEffects)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingScreen.kt (modified — wired compilation state and callbacks)
- feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModelTest.kt (modified — added Compilation nested class with 7 tests, FakeTimelapseCompiler, updated createViewModel helper)
- feature/tracing/build.gradle.kts (modified — changed dependency from :feature:timelapse to :core:timelapse)
- core/timelapse/build.gradle.kts (new — core timelapse module with interfaces)
- core/timelapse/src/main/kotlin/io/github/jn0v/traceglass/core/timelapse/TimelapseCompiler.kt (new — interface + CompilationResult sealed class)
- core/timelapse/src/main/kotlin/io/github/jn0v/traceglass/core/timelapse/SnapshotStorage.kt (new — interface)
- core/timelapse/src/main/kotlin/io/github/jn0v/traceglass/core/timelapse/TimelapseState.kt (new — enum)
- core/timelapse/src/main/kotlin/io/github/jn0v/traceglass/core/timelapse/TimelapseSession.kt (new — class)
- core/timelapse/src/main/kotlin/io/github/jn0v/traceglass/core/timelapse/VideoExporter.kt (new — interface + ExportResult sealed class)
- core/timelapse/src/main/kotlin/io/github/jn0v/traceglass/core/timelapse/VideoSharer.kt (new — interface)
- settings.gradle.kts (modified — added :core:timelapse include)

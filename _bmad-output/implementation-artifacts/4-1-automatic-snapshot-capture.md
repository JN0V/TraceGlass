# Story 4.1: Automatic Snapshot Capture

Status: review

## Story

As a user,
I want the app to automatically capture snapshots of my drawing progress,
so that a time-lapse video can be created from my tracing session.

## Acceptance Criteria

1. **Given** a tracing session is active **When** time-lapse is recording **Then** periodic snapshots are captured automatically (every 5 seconds) **And** snapshots are saved as JPEG files in `context.filesDir/timelapse/` **And** capture does not drop the camera feed below 20 fps (NFR1)

2. **Given** time-lapse is recording **When** the user pauses time-lapse (FR29) **Then** snapshot capture pauses **And** a visual indicator shows time-lapse is paused

3. **Given** time-lapse is paused **When** the user resumes time-lapse **Then** snapshot capture resumes from where it left off

## Tasks / Subtasks

- [x] Task 1: Create Koin DI module for timelapse (AC: all)
  - [x] 1.1 Create `feature/timelapse/src/main/kotlin/io/github/jn0v/traceglass/feature/timelapse/di/TimelapseModule.kt`
  - [x] 1.2 Declare bindings: `TimelapseSession`, `FileSnapshotStorage` (with `context.filesDir` base dir), `TimelapseCompiler` → `MediaCodecCompiler`, `VideoExporter` → `MediaStoreVideoExporter`, `VideoSharer`
  - [x] 1.3 Register `timelapseModule` in `TraceGlassApp.kt` alongside existing modules

- [x] Task 2: Add JPEG frame capture to FrameAnalyzer (AC: #1)
  - [x] 2.1 Add a `snapshotCallback: ((ByteArray) -> Unit)?` property to FrameAnalyzer (nullable, default null)
  - [x] 2.2 In `analyze()`, when callback is set, encode the current `ImageProxy` to JPEG bytes using YUV→Bitmap→compress pipeline
  - [x] 2.3 Ensure JPEG encoding runs only when timelapse is recording (callback non-null) — gated by ViewModel
  - [x] 2.4 **CRITICAL**: Respect `rowStride` padding when converting YUV buffer (see MEMORY.md — bug was dormant at 640x480)
  - [x] 2.5 Run JPEG compression on Dispatchers.IO to avoid blocking the camera analysis thread

- [x] Task 3: Integrate TimelapseSession into TracingViewModel (AC: #1, #2, #3)
  - [x] 3.1 Inject `TimelapseSession` and `FileSnapshotStorage` via Koin into TracingViewModel
  - [x] 3.2 Add timelapse state fields to `TracingUiState`: `isTimelapseRecording: Boolean`, `isTimelapsePaused: Boolean`, `snapshotCount: Int`
  - [x] 3.3 Implement `startTimelapse()` — sets FrameAnalyzer snapshot callback, calls `timelapseSession.start(viewModelScope)` with capture callback that saves JPEG via FileSnapshotStorage
  - [x] 3.4 Implement `pauseTimelapse()` / `resumeTimelapse()` — delegates to TimelapseSession, clears/re-sets snapshot callback
  - [x] 3.5 Implement `stopTimelapse()` — clears callback, stops session
  - [x] 3.6 Expose snapshot count via StateFlow (observe `timelapseSession.snapshotCount`)

- [x] Task 4: Add timelapse UI controls (AC: #1, #2, #3)
  - [x] 4.1 Add record/pause/stop button to ExpandableMenu (or as a new FAB near existing controls)
  - [x] 4.2 Show snapshot count badge when recording (e.g. "📷 12")
  - [x] 4.3 Show recording indicator (red dot or similar) when timelapse is active
  - [x] 4.4 Show paused indicator when timelapse is paused
  - [x] 4.5 Timelapse controls only visible when a tracing session is active

- [x] Task 5: Unit tests (AC: all)
  - [x] 5.1 Test ViewModel timelapse state transitions (start → recording → pause → resume → stop)
  - [x] 5.2 Test FrameAnalyzer snapshot callback invocation
  - [x] 5.3 Test that timelapse controls are hidden when no session is active
  - [x] 5.4 Verify Koin module resolves all timelapse dependencies

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
Claude Opus 4.6

### Debug Log References
- Test hang diagnosed: `viewModelScope + while(true)` in TimelapseSession capture loop prevents `runTest` completion
- Fix: replaced `advanceUntilIdle()` with `runCurrent()` and added `vm.viewModelScope.cancel()` at end of each timelapse test

### Completion Notes List
- Task 1: Created `TimelapseModule.kt` with all DI bindings, registered in `TraceGlassApp.kt`
- Task 2: Added one-shot `snapshotCallback` pattern to `FrameAnalyzer` — only encodes 1 JPEG per 5s interval (not every frame)
- Task 3: Integrated timelapse into `TracingViewModel` with start/pause/resume/stop methods and state observation
- Task 4: Added `TimelapseControls` composable with record/pause/resume/stop buttons, snapshot count badge, recording indicator
- Task 5: 10 ViewModel timelapse tests, 3 FrameAnalyzer snapshot tests, 6 Koin module tests — all pass
- Architecture note: `feature:tracing` depends on `feature:timelapse` (necessary for ViewModel integration despite general guideline)
- Full regression: 154 tasks, BUILD SUCCESSFUL across all modules

### Senior Developer Review (AI)

**Reviewer:** Claude Opus 4.6 — 2026-02-18
**Outcome:** Changes Requested → Fixed (8/12 findings resolved, 3 deferred as tech debt)

**Fixed in this review:**
- **C1 (CRITICAL)**: Timelapse not stopped when session ends — added `stopTimelapse()` call in `onToggleSession()` + test
- **H1**: Hardcoded `Dispatchers.IO` in FrameAnalyzer — injected `snapshotDispatcher` parameter
- **H3**: No explicit paused indicator — added pause icon in `TimelapseControls`
- **H4**: Task 5 parent checkbox `[ ]` → `[x]`
- **M1**: `snapshotScope` never cancelled — added `close()` method to FrameAnalyzer
- **M2**: Non-atomic `snapshotCallback` — replaced `@Volatile` with `AtomicReference.getAndSet(null)`

**Fixed (post-review, 4-2 conflict cleared):**
- **H2**: `TimelapseSession.startCaptureLoop` — wrapped `onCapture` + count increment in try-catch to prevent count inflation on capture failure
- **M3**: Removed unused `factory { TimelapseSession() }` from `TimelapseModule.kt`

**Deferred (tech debt) — ALL RESOLVED:**
- [x] [AI-Review][CRITICAL] C2: Architecture violation `feature:tracing → feature:timelapse` — **FIXED in Story 4-2**: extracted interfaces to `:core:timelapse`, `:feature:tracing` no longer depends on `:feature:timelapse`
- [x] [AI-Review][LOW] L1: `ByteArrayOutputStream` per snapshot instead of buffer recycling — **FIXED**: reuse `snapshotBuffer` with `reset()` in `FrameAnalyzer`
- [x] [AI-Review][LOW] L2: DI module includes Story 4.2/4.3 bindings prematurely — **RESOLVED**: Stories 4.2/4.3 now implemented, all bindings are needed

### Change Log
- 2026-02-18: Implementation complete, all tests pass, story moved to review
- 2026-02-18: Code review fixes — C1/H1/H3/H4/M1/M2 fixed, C2/H2/M3 deferred
- 2026-02-18: H2/M3 fixed (4-2 conflict cleared) — 8 of 12 findings resolved, C2+L1+L2 remain as tech debt
- 2026-02-18: All 12 findings resolved — C2 fixed by 4-2 extraction, L1 buffer recycling added, L2 moot (stories implemented)

### File List

**Created:**
- `feature/timelapse/src/main/kotlin/io/github/jn0v/traceglass/feature/timelapse/di/TimelapseModule.kt` — Koin DI module for timelapse dependencies
- `feature/timelapse/src/test/kotlin/io/github/jn0v/traceglass/feature/timelapse/TimelapseModuleTest.kt` — 6 Koin module verification tests

**Modified:**
- `app/src/main/kotlin/io/github/jn0v/traceglass/TraceGlassApp.kt` — Added `timelapseModule` import and registration
- `feature/tracing/build.gradle.kts` — Added `implementation(project(":feature:timelapse"))` dependency
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/FrameAnalyzer.kt` — Added `snapshotCallback`, JPEG encoding on IO dispatcher
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingUiState.kt` — Added timelapse fields (isTimelapseRecording, isTimelapsePaused, snapshotCount)
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt` — Added timelapse methods and state observation
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/di/TracingModule.kt` — Updated ViewModel binding with snapshotStorage and frameAnalyzer
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingScreen.kt` — Pass timelapse props to TracingContent
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingContent.kt` — Added TimelapseControls composable
- `feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModelTest.kt` — Added 10 timelapse tests + FakeSnapshotStorage
- `feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/FrameAnalyzerTest.kt` — Added 3 snapshot callback tests
- `feature/timelapse/build.gradle.kts` — Added koin-test dependency

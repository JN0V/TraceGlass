# Story 4.4: Time-lapse Snapshot Preservation

Status: ready-for-dev

## Story

As a user,
I want my time-lapse snapshots to survive interruptions,
so that I don't lose my recording if the app is backgrounded.

## Acceptance Criteria

1. **Given** time-lapse is recording with captured snapshots **When** the app goes to background (phone call, app switch) **Then** all captured snapshots are preserved on disk (FR32)

2. **Given** the app returns to foreground after interruption **When** the session is restored **Then** time-lapse can resume capturing from where it left off **And** previously captured snapshots are included in final compilation

3. **Given** the device battery dies during a session **When** the app relaunches **Then** all snapshots captured before the battery died are still available (NFR9)

## Tasks / Subtasks

- [ ] Task 1: Save timelapse state on background (AC: #1, #3)
  - [ ] 1.1 In TracingViewModel's existing `saveSession()` method, add timelapse fields to the `SessionData` being saved
  - [ ] 1.2 Save `timelapseSnapshotCount` (field already exists in `SessionData`), `isTimelapseRecording`, and `isTimelapsePaused`
  - [ ] 1.3 Snapshots are already on disk (`context.filesDir/timelapse/snapshot_XXXX.jpg`) — no additional save needed for the actual image data
  - [ ] 1.4 Ensure save happens with `NonCancellable` context (same pattern as existing session save) to survive process death

- [ ] Task 2: Restore timelapse state on foreground (AC: #2)
  - [ ] 2.1 In TracingViewModel's existing `restoreSession()` method, read timelapse fields from restored `SessionData`
  - [ ] 2.2 If `timelapseSnapshotCount > 0`, call `timelapseSession.restoreFromExisting(count)` to set the snapshot counter
  - [ ] 2.3 If `isTimelapseRecording` was true, auto-resume recording (re-set FrameAnalyzer callback, call `timelapseSession.start()`)
  - [ ] 2.4 If `isTimelapsePaused` was true, restore to paused state
  - [ ] 2.5 Verify existing snapshot files on disk match expected count (handle partial loss gracefully)

- [ ] Task 3: Cold start recovery (AC: #3)
  - [ ] 3.1 On cold start, when resume dialog is shown and accepted, also restore timelapse state
  - [ ] 3.2 Scan `context.filesDir/timelapse/` directory for existing `snapshot_XXXX.jpg` files
  - [ ] 3.3 Set `TimelapseSession` snapshot count from actual file count (more reliable than DataStore value after crash)
  - [ ] 3.4 User can choose to: continue recording (append to existing snapshots), compile existing snapshots, or discard

- [ ] Task 4: Add timelapse fields to SessionData persistence (AC: #1, #2, #3)
  - [ ] 4.1 `SessionData.timelapseSnapshotCount` already exists — ensure it's being saved/restored in `DataStoreSessionRepository`
  - [ ] 4.2 Add `isTimelapseRecording: Boolean = false` to `SessionData` if not present
  - [ ] 4.3 Add `isTimelapsePaused: Boolean = false` to `SessionData` if not present
  - [ ] 4.4 Update `DataStoreSessionRepository.save()` and `load()` to handle new fields
  - [ ] 4.5 Update `FakeSessionRepository` in tests to support new fields

- [ ] Task 5: Cleanup lifecycle (AC: all)
  - [ ] 5.1 When user explicitly discards timelapse or starts a new session, delete all snapshot files from `filesDir/timelapse/`
  - [ ] 5.2 Call `FileSnapshotStorage.clear()` to remove old snapshots
  - [ ] 5.3 Reset `timelapseSnapshotCount` to 0 in SessionData

- [ ] Task 6: Unit tests (AC: #1, #2, #3)
  - [ ] 6.1 Test session save includes timelapse state
  - [ ] 6.2 Test session restore restores timelapse state and calls `restoreFromExisting()`
  - [ ] 6.3 Test cold start recovery scans disk for existing snapshots
  - [ ] 6.4 Test cleanup deletes snapshot files and resets count
  - [ ] 6.5 Test graceful handling when disk snapshot count doesn't match DataStore value

## Dev Notes

### Existing Implementation (Already Done — DO NOT Rewrite)

| Class | File | Tests | Description |
|-------|------|-------|-------------|
| `FileSnapshotStorage` | `FileSnapshotStorage.kt` | 9 tests | Saves to `{baseDir}/timelapse/snapshot_XXXX.jpg`, `getSnapshotCount()`, `clear()` |
| `TimelapseSession.restoreFromExisting()` | `TimelapseSession.kt` | 21 tests | Restores snapshot counter without re-capturing |
| `SessionData.timelapseSnapshotCount` | `SessionData.kt` | — | Field exists: `val timelapseSnapshotCount: Int = 0` |

**DO NOT** recreate these files. The snapshot preservation infrastructure already works — this story is about WIRING it into the session lifecycle.

### What's Missing (The Actual Work)

1. **Save timelapse state** in existing `saveSession()` flow
2. **Restore timelapse state** in existing `restoreSession()` flow
3. **Cold start scan** of filesystem for surviving snapshots
4. **New `SessionData` fields** for recording/paused state (or encode in existing fields)
5. **Cleanup** when user discards or starts fresh

### Architecture Constraints

- **Snapshots survive by design**: `FileSnapshotStorage` writes to `context.filesDir/timelapse/` which persists across app restarts and process death [Source: architecture.md#Data-Persistence]
- **NFR8**: Time-lapse data must survive app backgrounding — already handled by file-based storage
- **NFR9**: No data loss on battery death — JPEG files on disk survive, DataStore uses atomic writes
- **Session save pattern**: Follow existing `saveSession()` pattern with `NonCancellable` + debounce [Source: TracingViewModel.kt — session persistence]
- **DataStore persistence**: Follow existing `DataStoreSessionRepository` pattern for new fields

### Integration with Existing Session Persistence (Epic 5)

The session persistence infrastructure (Epic 5, already done) provides:
- `SessionRepository.save(SessionData)` — atomic DataStore write with `NonCancellable`
- `SessionRepository.sessionData: Flow<SessionData>` — reactive session state
- Debounced saves (500ms) on state changes
- Resume dialog on cold start

This story EXTENDS that infrastructure with timelapse-specific fields. The `saveSession()` and `restoreSession()` methods in TracingViewModel already exist — add timelapse fields to them.

### SessionData Current Fields

```kotlin
data class SessionData(
    val imageUri: String? = null,
    val overlayOffsetX: Float = 0f,
    val overlayOffsetY: Float = 0f,
    val overlayScale: Float = 1f,
    val overlayRotation: Float = 0f,
    val overlayOpacity: Float = 0.5f,
    val colorTint: String = "NONE",
    val isInvertedMode: Boolean = false,
    val isSessionActive: Boolean = false,
    val timelapseSnapshotCount: Int = 0,  // ← ALREADY EXISTS
    val isOverlayLocked: Boolean = false,
    val viewportZoom: Float = 1f,
    val viewportPanX: Float = 0f,
    val viewportPanY: Float = 0f
)
```

You need to add: `isTimelapseRecording: Boolean = false` and `isTimelapsePaused: Boolean = false` (or use a single `timelapseState: String = "IDLE"` field).

### Dependency on Stories 4-1, 4-2, 4-3

- Story 4-1: TimelapseSession and FrameAnalyzer integration must be wired
- Story 4-2: Compilation must work for "compile existing snapshots" cold start option
- Story 4-3: Export must work for post-compilation sharing

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story-4.4]
- [Source: _bmad-output/planning-artifacts/architecture.md#Data-Persistence] — DataStore for metadata, filesDir for snapshots
- [Source: core/session/src/main/kotlin/.../SessionData.kt] — existing timelapseSnapshotCount field
- [Source: feature/tracing/src/main/kotlin/.../TracingViewModel.kt] — saveSession() and restoreSession() methods
- [Source: feature/timelapse/src/main/kotlin/.../FileSnapshotStorage.kt] — disk-based snapshot storage
- [Source: feature/timelapse/src/main/kotlin/.../TimelapseSession.kt] — restoreFromExisting() method

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

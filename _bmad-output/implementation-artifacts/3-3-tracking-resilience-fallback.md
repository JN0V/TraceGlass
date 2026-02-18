# Story 3.3: Tracking Resilience & Fallback

Status: done

## Story

As a user,
I want tracking to recover gracefully when my hand covers a marker,
So that I can draw without worrying about blocking markers.

## Acceptance Criteria

1. **Partial occlusion (FR11):** Given tracking is active with all markers visible, when one or more markers are partially occluded by the user's hand, then tracking continues using remaining visible markers and overlay stability is maintained.

2. **Timeout-based fallback (NFR7):** Given all markers are lost, when 500ms pass without detection, then the overlay switches to fixed-screen mode (last known position) and the tracking status indicator changes to LOST state.

3. **Grace period:** Given markers are briefly lost (< 500ms), the tracking state stays TRACKING (no flicker), and the overlay holds its last known transform.

4. **Recovery (NFR7):** Given tracking was lost, when markers become visible again, then tracking recovers within 500ms and the overlay smoothly transitions back to tracked position.

5. **Last transform hold:** Given tracking is lost (0 markers), the overlay does NOT reset to IDENTITY — it holds the last computed transform (position, rotation, scale) indefinitely until markers reappear or session is cleared.

6. **Manual adjustments preserved:** Given the user manually drags/scales the overlay while marker tracking is active, the manual adjustment is preserved as an additive offset on top of marker-driven transform and is not overwritten by marker updates.

7. **2→1→2 round trip:** Given a 2→1→2 marker transition occurs, when both markers return to their original positions, the overlay returns to approximately the same transform as before occlusion.

## Tasks / Subtasks

- [x] Task 1: Create TrackingStateManager (AC: #2, #3, #4)
  - [x] 1.1 `TrackingStatus` enum: `INACTIVE`, `TRACKING`, `LOST`
  - [x] 1.2 `onMarkerResult(result)` — state machine with 500ms timeout
  - [x] 1.3 `hasEverTracked` flag: stays INACTIVE until first marker detection
  - [x] 1.4 Grace period: brief marker loss stays TRACKING if elapsed < `lostTimeoutMs`
  - [x] 1.5 `timeProvider: () -> Long` injectable for deterministic unit tests
  - [x] 1.6 `reset()` to clear all state
- [x] Task 2: Integrate TrackingStateManager into ViewModel (AC: #2, #3, #4)
  - [x] 2.1 Replace inline tracking logic in `onMarkerResultReceived()` with `trackingStateManager.onMarkerResult()`
  - [x] 2.2 Map `TrackingStatus` → `TrackingState` (feature-layer enum)
  - [x] 2.3 Update `TracingUiState.trackingState` from manager output
- [x] Task 3: Last transform hold in OverlayTransformCalculator (AC: #5)
  - [x] 3.1 Store `lastTransform` field in calculator
  - [x] 3.2 Return `lastTransform` when `!result.isTracking` (instead of `IDENTITY`)
  - [x] 3.3 Update `lastTransform` on every successful computation
- [x] Task 4: Manual + marker separation in ViewModel (AC: #6)
  - [x] 4.1 Separate `manualOffset: Offset` from `markerTransform`
  - [x] 4.2 `onOverlayDrag()` updates `manualOffset`, not `overlayOffset` directly
  - [x] 4.3 Final: `overlayOffset = markerTransform.offset + manualOffset`
  - [x] 4.4 Final: `overlayScale = markerTransform.scale * manualScaleFactor`
  - [x] 4.5 Marker updates do not reset manual adjustments
- [x] Task 5: TrackingStateManager unit tests (AC: #2, #3, #4)
  - [x] 5.1 Starts INACTIVE
  - [x] 5.2 First detection → TRACKING
  - [x] 5.3 Brief loss → stays TRACKING (grace period)
  - [x] 5.4 500ms+ elapsed → LOST
  - [x] 5.5 Recovery: LOST → TRACKING when markers reappear
  - [x] 5.6 Partial occlusion (remaining markers) → stays TRACKING
  - [x] 5.7 Reset → INACTIVE
  - [x] 5.8 Empty result after reset → stays INACTIVE
- [x] Task 6: ViewModel tracking integration tests (AC: #4, #5, #6, #7)
  - [x] 6.1 Manual drag preserved after marker update
  - [x] 6.2 Lost tracking holds last transform values
  - [x] 6.3 Smoothed transform used (no large jumps between frames)
  - [x] 6.4 Marker recovery doesn't snap (smoothing applied)
- [x] Task 7: OverlayTransformCalculator transition tests (AC: #1, #5, #7)
  - [x] 7.1 2→1 transition preserves rotation
  - [x] 7.2 0 markers returns last known transform (not IDENTITY)
  - [x] 7.3 2→1→2 round trip returns approximately same transform
  - [x] 7.4 0→1 recovery returns valid transform

## Dev Notes

### Architecture

- **Module:** `:core:overlay` — `TrackingStateManager` (pure Kotlin, injectable timeProvider)
- **Module:** `:feature:tracing` — ViewModel integration
- **TrackingStateManager** is independent from OverlayTransformCalculator — clean separation of concerns

### TrackingStateManager State Machine

```
INACTIVE ──[markers detected]──→ TRACKING
TRACKING ──[markers lost, <500ms]──→ TRACKING (grace period)
TRACKING ──[markers lost, ≥500ms]──→ LOST
LOST ──[markers detected]──→ TRACKING (recovery)
```

- Grace period prevents flicker on brief occlusions (hand passing over marker)
- `lostTimeoutMs = 500L` default, configurable for tests
- `timeProvider` injection enables deterministic time control in tests

### Fallback Strategy

| Markers | Strategy |
|---------|----------|
| 4 | Full paper corners (homography) — added in Story 8.1 |
| 3 | Perspective-correct 4th corner estimation (if focal length known) or affine delta |
| 2 | Similarity transform (translation + rotation + scale) from marker spacing |
| 1 | Translation only + single-marker rotation/scale from edge geometry |
| 0 | Hold `lastTransform` indefinitely |

### Manual + Marker Separation

Two independent transform streams combined in `updateOverlayFromCombined()`:
- **Marker transform:** From `OverlayTransformCalculator.computeSmoothed()`, updated each frame
- **Manual transform:** From user gestures (drag, pinch, rotate), persisted across marker updates
- Combined: marker position + manual offset, marker scale * manual scale

### Testing Standards

- TrackingStateManager tests: injectable `timeProvider` returning controlled timestamps
- ViewModel tests: `StandardTestDispatcher` + `runTest` + `vm.viewModelScope.cancel()` at end
- Calculator tests: fresh calculator per test iteration when testing hidden corners in loops

### Project Structure Notes

- TrackingStateManager at `core/overlay/` (not `core/cv/`) — it manages overlay state, not detection
- `TrackingStatus` (core layer) vs `TrackingState` (feature layer) — same values, different packages

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 3.3]
- [Source: core/overlay/src/main/kotlin — TrackingStateManager, OverlayTransformCalculator]
- [Source: core/overlay/src/test/kotlin — TrackingStateManagerTest, OverlayTransformCalculatorTest]
- [Source: feature/tracing/src/main/kotlin — TracingViewModel, TracingUiState]
- [Source: feature/tracing/src/test/kotlin — TracingViewModelTest]
- [Source: MEMORY.md — viewModelScope + runTest hang fix, test state mutation trap]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

### Completion Notes List

- TrackingStateManager uses injectable `timeProvider` — deterministic tests without `delay()`
- Grace period (500ms) prevents UI flicker on brief hand-over-marker occlusions
- `lastTransform` hold prevents overlay reset on marker loss (was returning IDENTITY before fix)
- Manual offset is purely additive: `overlayOffset = markerTransform.offset + manualOffset`
- Manual scale is multiplicative: `overlayScale = markerTransform.scale * manualScaleFactor`
- ViewModel uses `computeSmoothed()` with `previousTransform` for all frames
- 64 total tests passing after this story (8 new TrackingStateManager + 2 new ViewModel)

### Change Log

- 2026-02-08: TrackingStateManager + ViewModel integration — commit 6afde74
- 2026-02-09: Single-marker + manual separation fixes — commit ed196a2

### File List

**New files:**
- core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/TrackingStateManager.kt
- core/overlay/src/test/kotlin/io/github/jn0v/traceglass/core/overlay/TrackingStateManagerTest.kt

**Modified files:**
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt (TrackingStateManager integration)
- feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModelTest.kt (tracking resilience tests)

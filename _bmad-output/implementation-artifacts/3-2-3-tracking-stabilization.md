# Story 3.2 + 3.3: Tracking Stabilization & Resilience

Status: review

## Story

As a user,
I want the overlay to stay stable when one marker is occluded, and recover smoothly when markers reappear,
So that I can draw freely without the overlay jumping or resetting.

## Acceptance Criteria

1. **Given** two markers are detected
   **When** one marker is occluded (hand covers it)
   **Then** the overlay continues tracking with the remaining marker (position, rotation, scale)
   **And** there is no visible jump in overlay rotation or scale

2. **Given** one marker is detected
   **When** the overlay is displayed
   **Then** rotation is computed from the single marker's corner geometry (atan2 of top edge)
   **And** scale is computed from the marker's edge length vs reference edge length
   **And** position centers on the single marker

3. **Given** all markers are lost
   **When** the overlay was previously tracking
   **Then** the overlay holds its last known transform (position, rotation, scale)
   **And** the tracking indicator changes to LOST after 500ms grace period

4. **Given** tracking was lost and markers reappear
   **When** the overlay resumes tracking
   **Then** the overlay smoothly transitions to the new tracked position (no snap/jump)
   **And** the tracking indicator returns to TRACKING

5. **Given** tracking is active (any number of markers)
   **When** the overlay position updates each frame
   **Then** temporal smoothing is applied to prevent jitter
   **And** the smoothing factor is configurable (default 0.3)

6. **Given** the user manually drags/scales the overlay
   **When** marker tracking is also active
   **Then** the manual adjustment is preserved as an additive offset on top of marker-driven transform
   **And** the manual adjustment is not overwritten by marker updates

7. **Given** a 2→1→2 marker transition occurs
   **When** both markers return to their original positions
   **Then** the overlay returns to approximately the same transform as before occlusion
   **And** no reference angle/spacing reset occurs

## Tasks / Subtasks

- [x] Task 1: Refactor OverlayTransformCalculator for single-marker support (AC: #1, #2, #3, #5)
  - [x] 1.1 Add single-marker rotation via `atan2(corner[1].y - corner[0].y, corner[1].x - corner[0].x)` using DetectedMarker.corners
  - [x] 1.2 Add single-marker scale from marker edge length (distance corner[0]→corner[1]) vs reference edge length
  - [x] 1.3 Set reference edge length on first single-marker detection (alongside existing referenceSpacing for 2-marker)
  - [x] 1.4 Hold last known transform when `!result.isTracking` instead of returning IDENTITY
  - [x] 1.5 Store `lastTransform` field, return it when no markers detected
  - [x] 1.6 Ensure `computeSmoothed()` works correctly with new single-marker logic
- [x] Task 2: Refactor TracingViewModel.onMarkerResultReceived (AC: #4, #5, #6)
  - [x] 2.1 Use `computeSmoothed()` instead of `compute()` for temporal smoothing
  - [x] 2.2 Store previous transform for smoothing input
  - [x] 2.3 Separate marker-driven transform from manual adjustments: add `manualOffset` field in ViewModel
  - [x] 2.4 `onOverlayDrag` updates manualOffset, not overlayOffset directly
  - [x] 2.5 Final overlayOffset = markerTransform.offset + manualOffset; final rotation = markerTransform.rotation; final scale = markerTransform.scale * manualScale
  - [x] 2.6 When tracking is LOST, hold last computed values (from calculator's lastTransform)
- [x] Task 3: Comprehensive OverlayTransformCalculator tests (AC: #1, #2, #3, #7)
  - [x] 3.1 Test: single marker with corners returns non-zero rotation
  - [x] 3.2 Test: single marker scale computed from edge length
  - [x] 3.3 Test: 2→1 marker transition preserves approximate rotation (no jump to 0)
  - [x] 3.4 Test: 0 markers returns last known transform (not IDENTITY)
  - [x] 3.5 Test: 0→1 marker recovery returns valid transform
  - [x] 3.6 Test: 2→1→2 round-trip returns approximately same transform
  - [x] 3.7 Test: smoothed transform converges without overshoot
  - [x] 3.8 Update existing tests that expect rotation=0 for single marker
- [x] Task 4: TracingViewModel tracking integration tests (AC: #4, #5, #6)
  - [x] 4.1 Test: manual drag preserved after marker update
  - [x] 4.2 Test: smoothed transform used (verify no large jumps between frames)
  - [x] 4.3 Test: lost tracking holds last transform values
  - [x] 4.4 Test: marker recovery doesn't snap (smoothing applied)
- [x] Task 5: Run full test suite and verify
  - [x] 5.1 `./gradlew :core:overlay:test` passes
  - [x] 5.2 `./gradlew :feature:tracing:test` passes
  - [x] 5.3 `./gradlew assembleDebug` succeeds

## Dev Notes

### Architecture

- **Module:** `:core:overlay` for OverlayTransformCalculator changes
- **Module:** `:feature:tracing` for TracingViewModel changes
- **No new modules or dependencies needed**

### Key Bug Analysis (Current Code)

**Bug 1: Rotation jumps to 0 on single marker**
```kotlin
// Current code in OverlayTransformCalculator.compute():
if (markers.size >= 2) {
    rotation = angle - referenceAngle!!
} else {
    scale = 1f       // <-- hardcoded, loses scale
    rotation = 0f    // <-- hardcoded, causes visible jump
}
```

**Bug 2: Returns IDENTITY when no markers**
```kotlin
if (!result.isTracking) return OverlayTransform.IDENTITY  // <-- loses all state
```

**Bug 3: computeSmoothed() never called**
```kotlin
// TracingViewModel.onMarkerResultReceived() uses:
val transform = transformCalculator.compute(result, frameWidth, frameHeight)
// Should use:
val transform = transformCalculator.computeSmoothed(result, frameWidth, frameHeight, previousTransform)
```

**Bug 4: Manual adjustments overwritten**
```kotlin
_uiState.update {
    it.copy(
        overlayOffset = Offset(transform.offsetX, transform.offsetY),  // overwrites manual drag
        overlayScale = transform.scale,                                  // overwrites manual pinch
        overlayRotation = transform.rotation
    )
}
```

### Single-Marker Rotation Algorithm

ArUco markers have 4 corners in a consistent order (top-left, top-right, bottom-right, bottom-left when the marker is upright). The rotation angle relative to the image frame is:

```
angle = atan2(corners[1].second - corners[0].second, corners[1].first - corners[0].first)
```

where corners[0] = top-left and corners[1] = top-right of the marker.

For 2-marker mode, the angle between marker centers is used (existing code, which works well).

For consistency, the **reference angle** should be set independently for each mode:
- `referenceAngleSingle`: set from first single-marker detection
- `referenceAngle`: set from first 2-marker detection (existing)

### Single-Marker Scale Algorithm

The marker's "edge length" in pixels varies with distance:
```
edgeLength = distance(corners[0], corners[1])
referenceEdgeLength = edgeLength on first detection
scale = edgeLength / referenceEdgeLength
```

### Manual + Marker Separation

The ViewModel stores:
- `markerTransform: OverlayTransform` — from calculator, smoothed
- `manualOffset: Offset` — from user drag gestures
- `manualScaleFactor: Float` — from user pinch gestures

The final UI state is:
```
overlayOffset = Offset(markerTransform.offsetX + manualOffset.x, markerTransform.offsetY + manualOffset.y)
overlayScale = markerTransform.scale * manualScaleFactor
overlayRotation = markerTransform.rotation
```

### Testing Standards

- JUnit 5 with @Nested, StandardTestDispatcher
- DetectedMarker with realistic corners (not emptyList())
- Helper function `markerWithCorners(id, cx, cy, size, angleDeg)` to generate test markers
- Test transitions explicitly: 2→1→0 and 0→1→2
- Assert no large jumps (delta < threshold between consecutive frames)

### Human Validation Test Plan Points

After implementation, the following must be validated on-device:

- **TC-TRK-01**: Place two markers, start tracking. Cover one marker with hand. Overlay should NOT jump. Rotation and scale stay approximately stable.
- **TC-TRK-02**: Cover both markers. Overlay should freeze in last position (not reset to center). Indicator shows LOST after ~0.5s.
- **TC-TRK-03**: Uncover markers. Overlay should smoothly return to tracking (no snap). Indicator shows TRACKING.
- **TC-TRK-04**: With tracking active, manually drag the overlay. The drag offset should be preserved even when markers move slightly.
- **TC-TRK-05**: With only one marker visible, rotate the paper slightly. Overlay rotation should follow the paper rotation.
- **TC-TRK-06**: 2→1→2 transition: cover one marker, then uncover. Overlay should return to approximately same position/rotation.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 3.2, Story 3.3]
- [Source: core/overlay/src/main/kotlin — OverlayTransformCalculator, TrackingStateManager, OverlayTransform]
- [Source: feature/tracing/src/main/kotlin — TracingViewModel, TracingUiState]
- [Source: core/cv/src/main/kotlin — MarkerResult, DetectedMarker]
- [Source: MEMORY.md — viewModelScope + runTest pattern]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

### Completion Notes List

- Single-marker rotation uses separate `referenceAngleSingle` (independent from 2-marker `referenceAngle`)
- Single-marker scale uses `referenceEdgeLength` from first single-marker detection
- When marker has no corners (empty list), falls back to `lastTransform.scale` and `lastTransform.rotation`
- ViewModel uses `computeSmoothed()` with `previousTransform` for all frames (including lost frames)
- Manual offset is purely additive: `overlayOffset = markerTransform.offset + manualOffset`
- Manual scale is multiplicative: `overlayScale = markerTransform.scale * manualScaleFactor`
- Tests use `markerWithCorners()` helper that generates realistic ArUco-order corners at any angle/size
- ViewModel tests use `smoothingFactor = 1f` by default for instant results; smoothing-specific tests use 0.3f
- All existing tests updated and passing (no regressions)

### Change Log

- 2026-02-09: Story created — tracking stabilization covering 3.2 + 3.3
- 2026-02-09: Implementation complete — all 5 tasks done, all tests passing

### File List

**Modified files:**
- core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransformCalculator.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt
- core/overlay/src/test/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransformCalculatorTest.kt
- feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModelTest.kt

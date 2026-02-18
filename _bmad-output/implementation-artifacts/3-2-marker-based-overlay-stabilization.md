# Story 3.2: Marker-Based Overlay Stabilization

Status: done

## Story

As a user,
I want the overlay to follow my markers automatically,
So that the reference image stays aligned even if the phone or glass shifts.

## Acceptance Criteria

1. **Marker-driven positioning:** Given markers are detected in the camera feed, when the overlay is displayed, then the overlay position and scale are computed from marker positions, and the overlay updates within 1 frame of marker movement (NFR4).

2. **Auto-scale from marker spacing (FR12):** Given markers are detected, the overlay scale is determined by the spacing between detected markers relative to reference spacing set on first detection.

3. **Rotation tracking:** Given two or more markers are detected, the overlay rotation is computed from the angle between marker centers relative to the reference angle.

4. **No visible jitter:** Given stable tracking with markers detected, the overlay does not exhibit visible jitter (temporal smoothing applied via exponential moving average).

5. **Smooth transition to tracked mode:** Given markers are detected and were not detected in the previous frame (transition from fixed to tracked mode), then the overlay smoothly transitions to tracked position (no snap/jump).

6. **Single-marker support:** Given only one marker is visible, the overlay position centers on the marker, rotation is computed from the marker's corner geometry (atan2 of top edge), and scale is computed from marker edge length vs reference.

7. **CameraX integration:** FrameAnalyzer wires marker detection results into TracingViewModel via StateFlow, with CameraXManager accepting optional ImageAnalysis.Analyzer for backpressure handling.

## Tasks / Subtasks

- [x] Task 1: Create OverlayTransform data class (AC: #1, #2, #3)
  - [x] 1.1 `OverlayTransform` with `offsetX`, `offsetY`, `scale`, `rotation`, `paperCornersFrame`, `paperAspectRatio`
  - [x] 1.2 `IDENTITY` companion object constant
- [x] Task 2: Create OverlayTransformCalculator (AC: #1, #2, #3, #6)
  - [x] 2.1 `compute(result, frameWidth, frameHeight)` — main entry point
  - [x] 2.2 Two-marker stabilization: center on midpoint, scale from spacing, rotation from angle
  - [x] 2.3 Single-marker fallback: center on marker, rotation from `atan2(corner[1] - corner[0])`, scale from edge length
  - [x] 2.4 Reference geometry set on first detection (`referenceSpacing`, `referenceAngle`, `referenceEdgeLength`)
  - [x] 2.5 `markerOffsetFromMidpoint` stored during 2-marker phase for virtual midpoint reconstruction on 1→2 transitions
  - [x] 2.6 `lastTransform` stored for fallback when no markers detected
- [x] Task 3: Implement temporal smoothing (AC: #4, #5)
  - [x] 3.1 `computeSmoothed(result, frameWidth, frameHeight, previous)` — interpolates between previous and target transform
  - [x] 3.2 Configurable `smoothingFactor` (default 0.3, 1.0 = instant)
  - [x] 3.3 Exponential moving average on `smoothedCorners` for frame-level smoothing
- [x] Task 4: Wire CameraX → FrameAnalyzer → ViewModel (AC: #7)
  - [x] 4.1 `CameraManager.bindPreview()` accepts optional `ImageAnalysis.Analyzer`
  - [x] 4.2 `CameraXManager` wires `ImageAnalysis` use case with `STRATEGY_KEEP_ONLY_LATEST` backpressure
  - [x] 4.3 TracingScreen collects `frameAnalyzer.latestResult` and calls `viewModel.onMarkerResultReceived()`
  - [x] 4.4 TracingViewModel stores `previousTransform` for smoothing input
  - [x] 4.5 Koin DI: `FrameAnalyzer` and `OverlayTransformCalculator` as singletons
- [x] Task 5: Unit tests (AC: #1, #2, #3, #4, #6)
  - [x] 5.1 No markers returns IDENTITY (initial) or last transform (after tracking)
  - [x] 5.2 Single marker centers on marker position
  - [x] 5.3 Two markers center on midpoint, scale from spacing
  - [x] 5.4 Reference angle set on first detection, rotation relative to reference
  - [x] 5.5 Smoothing converges to target without overshoot
  - [x] 5.6 Factor 1.0 gives instant tracking
  - [x] 5.7 2→1 transition preserves rotation via virtual midpoint
  - [x] 5.8 ViewModel: marker update changes overlay offset

## Dev Notes

### Architecture

- **Module:** `:core:overlay` — pure Kotlin, no Android dependencies (testable with JUnit 5)
- **Module:** `:feature:tracing` — ViewModel integration, CameraX wiring
- **Dual-path approach evolved later** (Story 8.1): paper corners for 4+ markers, affine fallback for <4

### Key Algorithms

**Two-marker stabilization:**
```
midpoint = (marker0.center + marker1.center) / 2
spacing = distance(marker0.center, marker1.center)
angle = atan2(marker1.centerY - marker0.centerY, marker1.centerX - marker0.centerX)
scale = spacing / referenceSpacing
rotation = angle - referenceAngle
offset = midpoint - frameCenter
```

**Single-marker rotation (added in follow-up commit):**
```
angle = atan2(corners[1].second - corners[0].second, corners[1].first - corners[0].first)
edgeLength = distance(corners[0], corners[1])
scale = edgeLength / referenceEdgeLength
```

**Virtual midpoint reconstruction (2→1 transition):**
- During 2-marker tracking, store `markerOffsetFromMidpoint` for each marker
- When one marker is lost, reconstruct virtual midpoint: `visible.center - storedOffset`
- Prevents position jump when losing one marker

### Testing Standards

- JUnit 5 with `@Nested` test classes
- `markerWithCorners(id, cx, cy, size, angleDeg)` helper for realistic test markers
- `smoothingFactor = 1f` for instant results in most tests; 0.3f for smoothing-specific tests
- Delta assertions for approximate equality (`abs(actual - expected) < threshold`)

### Project Structure Notes

- `OverlayTransformCalculator` is stateful: stores reference geometry across frames
- `FrameAnalyzer` registered as Koin singleton (one per camera session)
- `CameraXManager` uses `STRATEGY_KEEP_ONLY_LATEST` to drop frames if detection is slow

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 3.2]
- [Source: core/overlay/src/main/kotlin — OverlayTransformCalculator, OverlayTransform]
- [Source: core/overlay/src/test/kotlin — OverlayTransformCalculatorTest]
- [Source: feature/tracing/src/main/kotlin — TracingViewModel, TracingScreen, FrameAnalyzer]
- [Source: core/camera/src/main/kotlin — CameraManager, CameraXManager]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

### Completion Notes List

- Initial implementation had single-marker returning hardcoded `rotation=0, scale=1` (bug fixed in Story 3.2+3.3 combined follow-up)
- `computeSmoothed()` was not called initially in ViewModel (fixed in follow-up commit ed196a2)
- Manual adjustments were overwritten by marker updates initially (fixed: separate `manualOffset` + `markerTransform`)
- Reference angle/spacing independently tracked for single-marker vs two-marker modes
- 55 total tests passing after this story (8 new overlay tests + 5 new ViewModel tests)

### Change Log

- 2026-02-08: Initial implementation — commit 70965ad
- 2026-02-09: Single-marker support + frame scaling fix — commit ed196a2
- 2026-02-09: Rotation tracking + dynamic frame dimensions — commit 5596c3b

### File List

**New files:**
- core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransform.kt
- core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransformCalculator.kt
- core/overlay/src/test/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransformCalculatorTest.kt

**Modified files:**
- core/camera/src/main/kotlin/io/github/jn0v/traceglass/core/camera/CameraManager.kt (optional Analyzer param)
- core/camera/src/main/kotlin/io/github/jn0v/traceglass/core/camera/impl/CameraXManager.kt (ImageAnalysis use case)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingScreen.kt (marker result collection)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingUiState.kt (TrackingState enum)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt (onMarkerResultReceived)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/di/TracingModule.kt (FrameAnalyzer + calculator DI)
- feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModelTest.kt (tracking tests)
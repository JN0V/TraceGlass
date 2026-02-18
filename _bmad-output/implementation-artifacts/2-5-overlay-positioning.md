# Story 2.5: Overlay Positioning (Drag, Pinch, Rotate)

Status: done

## Story

As a user,
I want to manually reposition, resize, and rotate the overlay,
so that I can align the reference image precisely with my paper before locking.

## Acceptance Criteria

1. **Given** the overlay is in positioning mode (not locked) **When** the user drags on the overlay area **Then** the overlay repositions following the finger movement, and the movement is smooth (no jitter)
2. **Given** the overlay is in positioning mode **When** the user performs a pinch gesture **Then** the overlay resizes proportionally from the pinch center, and there is no minimum or maximum hard limit that blocks the gesture
3. **Given** the overlay is in positioning mode **When** the user performs a rotation gesture (two-finger twist) **Then** the overlay rotates around its center
4. **Given** the overlay is in positioning mode **When** the user performs drag, pinch, and rotate simultaneously **Then** all three transforms are applied correctly

## Tasks / Subtasks

- [x] Task 1: Gesture detection in TracingContent (AC: 1, 2, 3, 4)
  - [x] 1.1 `pointerInput` with `detectTransformGestures { _, pan, zoom, rotation -> }` on camera feed area
  - [x] 1.2 Pan → `onOverlayDrag(delta: Offset)`
  - [x] 1.3 Zoom → `onOverlayScale(scaleFactor: Float)`
  - [x] 1.4 Rotation → `onOverlayRotate(angleDelta: Float)`
  - [x] 1.5 All three gestures fire simultaneously in single callback

- [x] Task 2: Manual transform state in ViewModel (AC: 1, 2, 3)
  - [x] 2.1 `manualOffset: Offset` — cumulative drag delta
  - [x] 2.2 `manualScaleFactor: Float` — cumulative scale (multiplicative)
  - [x] 2.3 `manualRotation: Float` — cumulative rotation in degrees (additive)
  - [x] 2.4 Each gesture callback updates corresponding state and triggers debounced save

- [x] Task 3: Transform composition with marker tracking (AC: 4)
  - [x] 3.1 Manual transforms composed with marker-driven transform in `updateOverlayFromCombined()`
  - [x] 3.2 Manual transforms are independent of marker tracking (T_image vs T_paper)
  - [x] 3.3 When markers active: manual rotation/scale applied before homography, offset after
  - [x] 3.4 When no markers: simple affine with manual transforms

- [x] Task 4: Session persistence (AC: 1, 2, 3)
  - [x] 4.1 `manualOffset`, `manualScaleFactor`, `manualRotation` persisted via SessionData
  - [x] 4.2 Restored on session resume via `onResumeSessionAccepted()`
  - [x] 4.3 Debounced save (500ms) on each gesture

- [x] Task 5: Unit tests
  - [x] 5.1 Test drag accumulation
  - [x] 5.2 Test scale accumulation (multiplicative)
  - [x] 5.3 Test rotation accumulation (additive)
  - [x] 5.4 Test combined manual + marker transform
  - [x] 5.5 Test session persistence of manual transforms

### Review Follow-ups (AI)

- [x] [AI-Review][Low] Pinch scales from view center, not gesture centroid — `detectTransformGestures` centroid parameter is ignored (`{ _, pan, zoom, rotation -> }`). AC 2 says "from the pinch center." Deliberate simplification but deviates from spec. File: `TracingContent.kt` line 165
- [x] [AI-Review][Low] Redundant `updateOverlayFromCombined()` calls — each gesture dimension calls it separately (up to 3x per frame). Batch into single call after all 3 updates. File: `TracingViewModel.kt` lines 129-145
- [x] [AI-Review][Low] No dedicated AC 4 test — simultaneous drag+scale+rotate in one logical step is not tested as a unit. Each dimension tested individually. File: `TracingViewModelTest.kt`

## Dev Notes

### Architecture Requirements

- **Module:** `:feature:tracing` (gesture handling), `:core:overlay` (transform math)
- **3-Transform Model:** `T_final = T_viewport × T_paper × T_image`
  - This story implements `T_image` (user manual positioning)
  - `T_paper` comes from marker tracking (Epic 3)
  - `T_viewport` reserved for Story 2-7 (lock/viewport zoom)
- **No hard limits on scale** — gesture is never blocked (AC 2)

### Implementation Details

- Gesture detection: `detectTransformGestures` in TracingContent (lines 164-169)
- ViewModel state: `manualOffset`, `manualScaleFactor`, `manualRotation` (lines 57-59)
- Callbacks: `onOverlayDrag()`, `onOverlayScale()`, `onOverlayRotate()` (lines 129-145)
- Transform composition: `updateOverlayFromCombined()` (lines 304-381) — two branches:
  1. Homography path (4+ corners): manual transforms applied in view space
  2. Affine path (<4 corners): simple transform composition

### Key Design Decisions

- **Multiplicative scale:** `manualScaleFactor *= scaleFactor` (not additive)
- **Additive rotation:** `manualRotation += angleDelta`
- **Offset in screen space:** Drag delta added after homography (feels natural to user)
- **Rotation/scale before homography:** Applied to image within paper frame
- **Pure Kotlin matrix math:** `MatrixUtils.kt` — no `android.graphics.Matrix` (JUnit compatibility)

### References

- [Source: epics.md — Epic 2, Story 2.5]
- [Source: architecture.md — Decision 9: 3-Transform Model]
- [Source: prd.md — FR14, FR15, FR41]

## Dev Agent Record

### Agent Model Used

(Pre-BMAD implementation)

### Debug Log References

### Completion Notes List

- Implemented before BMAD workflow adoption
- Story file created retroactively for audit purposes
- Transform composition closely interleaved with Epic 3 marker tracking
- ✅ Resolved review finding [Low]: Pinch centroid — added `onOverlayGesture(centroid, pan, zoom, rotation)` that computes offset correction `(centroid - viewCenter) * (1 - zoom)` so scale originates from the gesture centroid instead of the view center
- ✅ Resolved review finding [Low]: Redundant recompute — `onOverlayGesture()` batches all 3 gesture dimensions + `updateOverlayFromCombined()` + `debounceSave()` into a single call per frame
- ✅ Resolved review finding [Low]: Combined gesture test — 3 new tests: simultaneous drag+scale+rotate, off-center centroid pivot verification, and multi-call accumulation (252 total tests, 0 failures)

## Change Log

- 2026-02-17: Addressed 3 Low-severity code review findings — centroid-based scale pivot, batched gesture processing, combined AC4 tests

## Senior Developer Review (AI)

**Review Date:** 2026-02-17
**Reviewer:** Amelia (Dev Agent — Retroactive Audit)
**Outcome:** Approve (with minor suggestions)

### Summary

All 4 ACs pass. 3-Transform model correctly implemented. Manual transforms survive marker changes. 3 Low-severity items: centroid ignored, redundant matrix recomputation, no combined gesture test. None are blockers.

### Action Items

- [x] [Low] **Pinch centroid ignored** — Fixed: `onOverlayGesture()` now uses centroid for scale pivot offset correction. Formula: `(centroid - viewCenter) * (1 - zoom)`.
- [x] [Low] **Redundant matrix recomputation** — Fixed: `onOverlayGesture()` batches all 3 gesture updates into a single `updateOverlayFromCombined()` call per frame.
- [x] [Low] **No simultaneous gesture test** — Fixed: 3 new tests added: combined gesture, off-center centroid pivot, and multi-call accumulation.

### File List

- feature/tracing/src/main/kotlin/.../TracingContent.kt (detectTransformGestures — centroid forwarded via onOverlayGesture)
- feature/tracing/src/main/kotlin/.../TracingViewModel.kt (added onOverlayGesture with centroid pivot + batch)
- feature/tracing/src/main/kotlin/.../TracingScreen.kt (wired onOverlayGesture replacing 3 individual callbacks)
- feature/tracing/src/test/kotlin/.../TracingViewModelTest.kt (3 new tests: combined gesture, centroid pivot, accumulation)
- core/overlay/src/main/kotlin/.../MatrixUtils.kt (pure Kotlin 3x3 matrix ops)
- core/overlay/src/main/kotlin/.../OverlayTransform.kt (data class with offsetX/Y, scale, rotation)
- core/overlay/src/main/kotlin/.../OverlayTransformCalculator.kt (computeSmoothed)
- core/overlay/src/main/kotlin/.../HomographySolver.kt (homography/affine solvers)

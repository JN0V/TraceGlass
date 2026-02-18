# Story 8.2: Paper-Size Agnostic 4-Marker Reference System

Status: done

## Story

As a user,
I want the tracking system to work with any paper size by detecting 4 ArUco markers at the paper corners,
So that I can use any paper or surface without configuring dimensions.

## Acceptance Criteria

1. **Given** 4 ArUco markers (IDs 0-3) are placed at the paper corners
   **When** all 4 are detected for the first time
   **Then** the system captures their positions as the reference geometry
   **And** infers the paper aspect ratio from marker spacing (no hardcoded A4/Letter)

2. **Given** the reference geometry is established
   **When** the phone moves or tilts
   **Then** the overlay tracks the paper using the 4-corner reference as baseline
   **And** smoothed corner positions update via exponential smoothing (alpha = 0.12)

3. **Given** the reference is captured with the phone nearly parallel to the paper (fronto-parallel)
   **When** the edge ratio check passes (top/bottom and left/right within 8%)
   **Then** `isReferenceRectangular = true` gates auto-focal-length estimation
   **And** auto-f is computed from the orthogonality constraint on subsequent frames

4. **Given** the reference is captured with the phone already tilted
   **When** the edge ratio check fails
   **Then** auto-f estimation is disabled (`isReferenceRectangular = false`)
   **And** focal length must come from `setFocalLength()` to enable perspective-correct estimation

5. **Given** the focal length becomes available (auto-estimated or injected)
   **When** `needsRebuildPaperCoords` is true
   **Then** `rebuildPaperCoords()` corrects the paper aspect ratio using `correctAspectRatio()`
   **And** `calibratedPaperCorners` are updated to a proper rectangle

6. **Given** fewer than 4 markers are detected initially
   **When** the overlay is displayed
   **Then** the system uses the pre-8.1 affine fallback (translation + scale + rotation)
   **And** waits for all 4 markers before establishing the reference

## Tasks / Subtasks

- [x] Task 1: 4-marker outer corner extraction (AC: #1)
  - [x] 1.1 `extractOuterCorners()`: for each marker ID 0-3, extract `corners[id]` (TL from ID 0, TR from ID 1, BR from ID 2, BL from ID 3)
  - [x] 1.2 Store as `referenceCorners: Map<Int, Pair<Float, Float>>` on first 4-marker detection
  - [x] 1.3 Initialize `calibratedPaperCorners = [TL, TR, BR, BL]` from detected positions
  - [x] 1.4 Compute `referencePaperAR` from average edge lengths

- [x] Task 2: Reference rectangularity check (AC: #3, #4)
  - [x] 2.1 Compute edge ratios: `topEdge/bottomEdge` and `leftEdge/rightEdge`
  - [x] 2.2 Set `isReferenceRectangular = true` when both ratios within 8% of 1.0
  - [x] 2.3 Threshold allows ~5 degrees of tilt at reference capture

- [x] Task 3: Auto focal length estimation (AC: #3)
  - [x] 3.1 `HomographySolver.estimateFocalLength(paperCoords, frameCoords, cx, cy): Float?`
  - [x] 3.2 Compute H from paper coords to frame coords
  - [x] 3.3 Orthogonality constraint: `f^2 = -numerator / (H[6]*H[7])` from `v1.v2 = 0`
  - [x] 3.4 Near-fronto-parallel fallback: when `|H[6]*H[7]| < 1e-12`, use equal-norm constraint
  - [x] 3.5 Return null if `f^2 < 100` (unreasonable, f < 10px)
  - [x] 3.6 Called in `computeWithPaperCorners()` when `isReferenceRectangular && 4 visible && f not yet known`

- [x] Task 4: Aspect ratio correction (AC: #5)
  - [x] 4.1 `HomographySolver.correctAspectRatio(assumed, detected, f, cx, cy): Float?`
  - [x] 4.2 Compute `AR_true = AR_assumed * |K^-1 * h1| / |K^-1 * h2|` (rotation column norms)
  - [x] 4.3 Clamp result to [0.1, 10.0]
  - [x] 4.4 `rebuildPaperCoords()`: build edge-rect from detected corners, correct AR, update `calibratedPaperCorners`
  - [x] 4.5 Triggered by `needsRebuildPaperCoords` flag (set by `setFocalLength()` or auto-f)

- [x] Task 5: `setFocalLength()` public API (AC: #4, #5)
  - [x] 5.1 `OverlayTransformCalculator.setFocalLength(f: Float)` — sets `calibratedFocalLength`, sets `needsRebuildPaperCoords = true`
  - [x] 5.2 Enables constrained homography for 3-marker estimation (Story 8.3)

- [x] Task 6: Exponential smoothing for corners (AC: #2)
  - [x] 6.1 `smoothedCorners: MutableMap<Int, Pair<Float, Float>>` updated each frame
  - [x] 6.2 `lerp(previous, detected, alpha=0.12)` for each visible corner
  - [x] 6.3 Non-visible corners updated by delta estimation (Story 8.3 builds on this)

- [x] Task 7: Reset and lifecycle (AC: #6)
  - [x] 7.1 `resetReference()`: clears all state (referenceCorners, calibratedFocalLength, calibratedPaperCorners, etc.)
  - [x] 7.2 Before reference established, `computeAffine()` fallback provides basic tracking

- [x] Task 8: Unit tests (AC: all)
  - [x] 8.1 First 4-marker detection initializes reference and paper corners
  - [x] 8.2 Edge ratio check: rectangular reference (parallel capture)
  - [x] 8.3 Edge ratio check: non-rectangular reference (tilted capture)
  - [x] 8.4 Auto-f estimation called only when rectangular reference
  - [x] 8.5 `setFocalLength()` triggers paper coord rebuild
  - [x] 8.6 AR correction produces reasonable values
  - [x] 8.7 OverlayTransformCalculatorTest: 35 tests pass

## Dev Notes

### Paper-Size Agnostic Design

The system does NOT hardcode A4, Letter, or any paper size. Paper coordinates are inferred from the first 4-marker detection:

```kotlin
// On first 4-marker detection:
referenceCorners = detected.toMap()  // actual pixel positions
calibratedPaperCorners = [TL, TR, BR, BL]  // same as detected
referencePaperAR = averageWidth / averageHeight
```

This means the system works with any rectangular surface — paper, canvas, whiteboard, tablet screen.

### Focal Length Estimation Math

From a single homography H (paper → frame), the camera calibration matrix K = [f 0 cx; 0 f cy; 0 0 1] is partially recoverable:

```
v_i = K^{-1} * h_col_i
Constraint 1: v1.v2 = 0 (rotation columns are orthogonal)
  → f^2 = -[(H[0]-cx*H[6])*(H[1]-cx*H[7]) + (H[3]-cy*H[6])*(H[4]-cy*H[7])] / (H[6]*H[7])

When H[6]*H[7] ≈ 0 (fronto-parallel):
Constraint 2: |v1| = |v2| (rotation columns have equal norm)
  → f^2 = (a1^2+b1^2 - a2^2-b2^2) / (h7^2 - h6^2)
```

**Degenerate case**: H[6]=0 for pure horizontal tilt → both denominators zero → returns null. This is acceptable: constrained homography still works with affine delta fallback.

### Known Limitation: Equal-Norm Fallback

The equal-norm fallback (`|v1|=|v2|`) depends on paper AR. If the reference is captured at significant tilt, the detected AR is wrong, making the fallback unreliable. This is why `isReferenceRectangular` gates auto-f — only trust it when the reference is close to fronto-parallel.

### Project Structure Notes

- All focal length math lives in `HomographySolver` (pure Kotlin, testable)
- All reference management lives in `OverlayTransformCalculator`
- No Android framework dependencies in any of this code

### References

- [Source: core/overlay/OverlayTransformCalculator.kt:84-111 — reference initialization]
- [Source: core/overlay/OverlayTransformCalculator.kt:100-110 — isReferenceRectangular check]
- [Source: core/overlay/OverlayTransformCalculator.kt:114-122 — auto-f estimation call]
- [Source: core/overlay/OverlayTransformCalculator.kt:409-447 — rebuildPaperCoords()]
- [Source: core/overlay/OverlayTransformCalculator.kt:454-457 — setFocalLength() API]
- [Source: core/overlay/HomographySolver.kt:135-186 — estimateFocalLength()]
- [Source: core/overlay/HomographySolver.kt:356-391 — correctAspectRatio()]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

### Completion Notes List

- Paper-size agnostic: no A4 or Letter hardcoding in production code
- Auto-f estimation works for fronto-parallel reference (edge ratio < 8%)
- `setFocalLength()` API ready for CameraX intrinsics injection (Story 8.4)
- AR correction via rotation column norms — invariant to paper scale
- H[6]=0 degeneracy documented and handled (returns null)
- 35+ OverlayTransformCalculator tests + 20+ HomographySolver tests cover all paths

### Change Log

- 2026-02-13: 4-marker reference system implemented
- 2026-02-14: Focal length estimation and AR correction added
- 2026-02-18: Story documented retroactively

### File List

**Modified files:**
- core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransformCalculator.kt
- core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/HomographySolver.kt
- core/overlay/src/test/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransformCalculatorTest.kt
- core/overlay/src/test/kotlin/io/github/jn0v/traceglass/core/overlay/HomographySolverTest.kt

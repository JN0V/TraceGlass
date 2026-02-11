# Story 8.1: Perspective Correction via Marker Homography

Status: draft

## Story

As a user,
I want the overlay image to automatically compensate for the angle between my phone and the drawing surface,
So that the projected image lines up accurately with my paper even when my phone isn't perfectly parallel.

## Context

When the phone is not held exactly parallel to the drawing surface (e.g. tilted phone stand, uneven desk, hand-held use), the camera image and the overlay projection are not coplanar. This produces a perspective mismatch: what appears centered on the phone screen projects off-center on the paper, and straight lines in the overlay map to slightly curved or shifted lines on the paper.

ArUco markers are squares with known geometry. When detected at an angle, their 4 corners form a trapezoid instead of a square. This deformation encodes the exact perspective transform (homography) between the camera plane and the paper plane. Since `DetectedMarker.corners` already provides these 4 corner coordinates per marker, all the data needed is already available — no gyroscope or additional sensors required.

## Acceptance Criteria

1. **Given** two markers are detected and the phone is perfectly parallel to the surface
   **When** the overlay is displayed
   **Then** the overlay renders identically to the current behavior (no regression)

2. **Given** two markers are detected and the phone is tilted relative to the surface
   **When** the overlay is displayed
   **Then** the overlay is warped with a perspective correction so that its projection on the paper matches the reference image geometry

3. **Given** perspective correction is active
   **When** the phone tilt changes smoothly (user adjusts stand)
   **Then** the overlay correction updates smoothly without jitter (temporal smoothing)

4. **Given** only one marker is detected
   **When** the overlay is displayed
   **Then** perspective correction is disabled and the overlay uses the current affine transform (translation + scale + rotation only)

5. **Given** the user opens Settings
   **When** they look at the tracking section
   **Then** a toggle "Perspective correction" is available (enabled by default)

## Approach: Homography from Marker Corners

### Theory

Each ArUco marker is a perfect square. In the camera image, its 4 detected corners define a quadrilateral. The homography **H** that maps the ideal square to the detected quadrilateral encodes the full 3D perspective relationship between the camera and the paper plane.

With 2 markers (8 corner points, 4 per marker), we have more than enough data to compute a robust homography (minimum 4 points needed).

### Algorithm

```
1. Collect corner points from both detected markers:
   detected_pts = marker0.corners + marker1.corners  (8 points in camera space)

2. Define ideal positions for those same corners:
   ideal_pts = layout of two 2cm squares placed at known spacing
   (spacing derived from referenceSpacing on first 2-marker frame)

3. Compute homography H = findHomography(ideal_pts, detected_pts)
   - This is a 3x3 matrix that maps ideal paper coords → camera coords

4. Compute inverse H⁻¹ = camera coords → ideal paper coords
   - Apply H⁻¹ to the overlay: this "un-warps" the perspective

5. In Compose, apply the homography as a Matrix transformation
   on the overlay Image via graphicsLayer { ... }
```

### Smoothing

To avoid jitter, apply exponential smoothing on the 9 homography matrix coefficients between frames (same approach as current `computeSmoothed()` for translation/scale/rotation).

## Tasks / Subtasks

- [ ] Task 1: Compute homography from marker corners (AC: #1, #2)
  - [ ] 1.1 Add `computeHomography(markers, frameWidth, frameHeight): FloatArray?` to `OverlayTransformCalculator`
  - [ ] 1.2 Build ideal point set from reference marker spacing (square geometry, known IDs)
  - [ ] 1.3 Implement 4-point DLT (Direct Linear Transform) algorithm for 3x3 homography
  - [ ] 1.4 Return null when < 2 markers (fallback to affine)
  - [ ] 1.5 Unit tests: identity when no tilt, correct warp when simulated tilt, null with 1 marker

- [ ] Task 2: Add perspective matrix to OverlayTransform (AC: #1, #2, #4)
  - [ ] 2.1 Add `perspectiveMatrix: FloatArray? = null` field to `OverlayTransform` (3x3, row-major)
  - [ ] 2.2 When non-null, UI uses this matrix instead of separate offset/scale/rotation
  - [ ] 2.3 When null (1 marker), UI falls back to current affine behavior
  - [ ] 2.4 Unit tests: IDENTITY has null matrix, compute populates matrix with 2 markers

- [ ] Task 3: Temporal smoothing for homography (AC: #3)
  - [ ] 3.1 Add `smoothHomography(previous, target, factor): FloatArray` in calculator
  - [ ] 3.2 Smooth each of the 9 matrix coefficients independently
  - [ ] 3.3 Integrate into `computeSmoothed()` flow
  - [ ] 3.4 Unit tests: smoothing interpolates between two matrices

- [ ] Task 4: Apply perspective transform in Compose overlay (AC: #1, #2, #4)
  - [ ] 4.1 Thread `perspectiveMatrix` through TracingUiState → TracingScreen
  - [ ] 4.2 In `graphicsLayer`, convert 3x3 homography to `android.graphics.Matrix` for `transformOrigin` + render
  - [ ] 4.3 Use Compose `graphicsLayer { transformOrigin = ...; clip = true }` with custom matrix
  - [ ] 4.4 Fallback: when `perspectiveMatrix` is null, use existing offset/scale/rotation code path
  - [ ] 4.5 Visual regression: side-by-side check that flat surface renders identically to before

- [ ] Task 5: Settings toggle (AC: #5)
  - [ ] 5.1 Add `perspectiveCorrectionEnabled: Boolean = true` to SettingsData / SettingsRepository
  - [ ] 5.2 Add toggle row in SettingsScreen
  - [ ] 5.3 When disabled, `computeHomography()` returns null → affine fallback
  - [ ] 5.4 Unit test: toggle disables perspective matrix computation

- [ ] Task 6: Full test suite (AC: all)
  - [ ] 6.1 All existing tests pass (no regression)
  - [ ] 6.2 On-device test: flat surface → no visible change
  - [ ] 6.3 On-device test: tilted phone → overlay compensates visually

## Dev Notes

### Architecture Requirements

- **Module:** `:core:overlay` for homography computation, `:feature:tracing` for UI integration
- **No new dependencies:** DLT algorithm is pure math (8x9 matrix SVD or simplified 4-point solve)
- **Data already available:** `DetectedMarker.corners: List<Pair<Float, Float>>` — 4 corner coordinates per marker

### Existing Code Context

**DetectedMarker** (core/cv/MarkerResult.kt):
```kotlin
data class DetectedMarker(
    val id: Int,
    val centerX: Float,
    val centerY: Float,
    val corners: List<Pair<Float, Float>>,  // ← 4 corners, the key input
    val confidence: Float
)
```

**OverlayTransformCalculator** (core/overlay/):
- Already computes offset, scale, rotation from marker centers
- `referenceSpacing` and `referenceAngle` set on first 2-marker detection
- `computeSmoothed()` provides temporal smoothing
- Homography computation extends this class naturally

**TracingScreen graphicsLayer** (feature/tracing/):
```kotlin
.graphicsLayer {
    translationX = overlayOffset.x
    translationY = overlayOffset.y
    scaleX = overlayScale
    scaleY = overlayScale
    rotationZ = overlayRotation
}
```
This will be replaced/augmented with a matrix-based transform when perspective correction is active.

### DLT Algorithm (simplified for 4+ point pairs)

The Direct Linear Transform solves for H in `p' = H * p`:
1. For each point pair (x,y) → (x',y'), construct two rows of matrix A
2. Stack into Ax = 0 system (A is 2N × 9 for N points)
3. Solve via SVD: H = last column of V (the one with smallest singular value)
4. Reshape into 3×3, normalize so H[2][2] = 1

With 8 points (2 markers × 4 corners), A is 16×9 — well-conditioned.

Alternative: since we have exactly 4 point pairs per marker, we could use `getPerspectiveTransform()` from OpenCV via JNI, but a pure Kotlin DLT avoids JNI overhead for this lightweight computation.

### Compose Matrix Application

`Modifier.graphicsLayer` supports a `Matrix` via the `transformOrigin` + individual transform properties. For a full perspective matrix, the approach is:
- Convert 3×3 homography to a 4×4 matrix (embed in the XY plane)
- Use `Canvas` with `drawWithContent { drawContext.canvas.nativeCanvas.concat(matrix) }`
- Or use `Modifier.graphicsLayer { this.renderEffect = ... }` with a custom shader

The simplest reliable approach: use `AndroidView` wrapping an `ImageView` with `imageMatrix` set to the homography, or use a custom `Canvas` draw.

### Risk: Numerical Stability

- Homography can be ill-conditioned when markers are nearly coplanar with camera (extreme tilt)
- Mitigation: check determinant of H, fall back to affine if |det(H)| is too small or too large
- Threshold TBD during implementation (start with 0.1 < |det| < 10)

### Testing Standards

- JUnit 5 with @Nested, StandardTestDispatcher
- Pure math tests for DLT: known inputs → expected outputs
- Regression: all existing OverlayTransformCalculator tests must pass unchanged
- On-device visual testing for final verification

### References

- [Source: core/cv/MarkerResult.kt — DetectedMarker with corners field]
- [Source: core/overlay/OverlayTransformCalculator.kt — current transform pipeline]
- [Source: feature/tracing/TracingScreen.kt — graphicsLayer overlay rendering]
- [Wikipedia: Direct Linear Transform](https://en.wikipedia.org/wiki/Direct_linear_transformation)

## Dev Agent Record

### Agent Model Used

(not yet implemented)

### Completion Notes List

(pending implementation)

### Change Log

- 2026-02-09: Story 8.1 drafted

### File List

**New files:**
(pending implementation)

**Modified files:**
(pending implementation)

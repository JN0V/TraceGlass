# Story 8.3: 3-Marker Constrained Homography

Status: done

## Story

As a user,
I want the overlay to maintain perspective-correct positioning even when one marker is occluded by my hand,
So that I can draw without worrying about blocking a corner marker.

## Acceptance Criteria

1. **Given** 3 out of 4 markers are visible and the focal length is known
   **When** the overlay is displayed
   **Then** the hidden 4th corner is estimated using constrained homography (paper geometry + focal length)
   **And** the estimation error is < 5px at up to 20 degrees tilt

2. **Given** 3 markers are visible but no focal length is available
   **When** the overlay is displayed
   **Then** the system falls back to affine delta estimation (prev-frame visible corners to estimate hidden corner movement)
   **And** tracking remains stable for small frame-to-frame changes

3. **Given** 2 markers are visible
   **When** the overlay is displayed
   **Then** the system uses similarity delta (translation + rotation + scale from 2 point pairs)
   **And** hidden corners move consistently with the visible pair

4. **Given** 1 marker is visible
   **When** the overlay is displayed
   **Then** the system uses translation-only delta
   **And** the overlay moves with the visible marker

5. **Given** 0 markers are visible
   **When** the overlay is displayed
   **Then** the system holds the last known corner positions

6. **Given** the constrained homography solver receives inconsistent geometry
   **When** Newton-Raphson fails to converge or diverges (|h7| > 0.1 or |h8| > 0.1)
   **Then** the solver returns null
   **And** the system falls back to affine delta gracefully

## Tasks / Subtasks

- [x] Task 1: Constrained homography solver (AC: #1, #6)
  - [x] 1.1 `HomographySolver.solveConstrainedHomography(paperCoords3, frameCoords3, f, cx, cy): FloatArray?`
  - [x] 1.2 Build 6x8 linear system from 3 correspondences
  - [x] 1.3 Solve `A6 * x = b6` and `A6 * x = C[:,j]` for parameterization: `h = p0 + h7*d7 + h8*d8`
  - [x] 1.4 Apply 2 quadratic constraints via Newton-Raphson:
    - F1: `v1.v2 = 0` (orthogonality of K^{-1} * H columns)
    - F2: `|v1|^2 - |v2|^2 = 0` (equal norm)
  - [x] 1.5 Starting point: h7=0, h8=0 (affine approximation)
  - [x] 1.6 50 iterations max, tolerance 1e-10
  - [x] 1.7 Divergence check: |h7| > 0.1 or |h8| > 0.1 → return null
  - [x] 1.8 Final validation: |F1| < 1e-4 and |F2| < 1e-4 → accept, else null
  - [x] 1.9 Numerical Jacobian via finite differences (eps = 1e-8)

- [x] Task 2: 4th corner projection from constrained H (AC: #1)
  - [x] 2.1 `estimateFromPaperGeometry()` in OverlayTransformCalculator
  - [x] 2.2 For each missing ID: project paper coord through H: `x' = (H*[px,py,1]^T) / w`
  - [x] 2.3 Check `|w| > 1e-6` to avoid division by near-zero
  - [x] 2.4 Return `true` if successful, `false` to fall back to affine

- [x] Task 3: Delta-based fallback hierarchy (AC: #2, #3, #4, #5)
  - [x] 3.1 `estimateFromDelta()` dispatcher:
    - 3+ visible: try `estimateFromPaperGeometry()` first, fall back to affine delta
    - 2 visible: similarity delta via `computeSimilarityParams()`
    - 1 visible: translation delta
    - 0 visible: hold last known
  - [x] 3.2 `prevSmooth` snapshot before updating smooth corners (frame-to-frame delta source)
  - [x] 3.3 Affine delta: `solveAffine(prev[visible], smooth[visible])`, apply to `prev[missing]`
  - [x] 3.4 Similarity delta: scale + rotation + translation from 2 point pairs

- [x] Task 4: Unit tests (AC: all)
  - [x] 4.1 Constrained H: recover 4th corner from 3 visible at 0 degree tilt (< 2px error)
  - [x] 4.2 Constrained H: recover at 20 degree tilt (< 5px error)
  - [x] 4.3 Constrained H: recover at 30 degree tilt
  - [x] 4.4 Constrained H: works with tilted-first-detection (realistic scenario)
  - [x] 4.5 Constrained H: works with gradual tilt and single-step tilt
  - [x] 4.6 Constrained H: works with `setFocalLength()` injected
  - [x] 4.7 Affine delta fallback when f is null
  - [x] 4.8 Similarity delta for 2 visible markers
  - [x] 4.9 Translation delta for 1 visible marker
  - [x] 4.10 Hold for 0 visible markers
  - [x] 4.11 Fresh calculator per hidden-ID iteration (avoid state mutation trap)

## Dev Notes

### Constrained Homography Algorithm

The problem: 3 point correspondences give only 6 equations for 8 unknowns (h1..h8, h9=1). The focal length provides 2 additional constraints from the rotation matrix properties.

**Linear phase**: Express h1..h6 as functions of (h7, h8):
```
A6 * [h1..h6]^T = b6 + C * [h7, h8]^T
→ h = p0 + h7*d7 + h8*d8
```

**Nonlinear phase**: Newton-Raphson on 2 constraints:
```
F1 = v1·v2 = 0        (rotation columns orthogonal)
F2 = |v1|² - |v2|² = 0  (rotation columns equal norm)
where v_i = K^{-1} * h_col_i
```

Starting from (h7=0, h8=0) — the affine solution — Newton-Raphson typically converges in 3-5 iterations for moderate tilt.

### Test State Mutation Trap

When testing hidden corners in a loop (iterating over which corner is hidden), each `calc.compute()` mutates `smoothedCorners`. Wrong estimated positions feed into the next iteration as `prevSmooth`, causing cascading error. **Fix**: create a fresh calculator for each hidden-ID iteration.

### Degradation Hierarchy

The system degrades gracefully as markers are lost:

| Visible | Method | Accuracy |
|---------|--------|----------|
| 4 | Direct smoothing | Exact (smoothed) |
| 3 + f | Constrained H | < 5px at 20 degree |
| 3 no f | Affine delta | Good for small deltas |
| 2 | Similarity delta | Translation + rotation + scale |
| 1 | Translation delta | Position only |
| 0 | Hold | Static |

### Known Limitations

- H[6]=0 degeneracy: pure horizontal tilt makes the orthogonality constraint degenerate. The solver returns null and affine delta takes over.
- At > 30 degrees, constrained H accuracy degrades for some corner configurations
- The solver assumes square pixels (fx = fy = f). CameraX intrinsics could provide separate fx/fy for higher accuracy.

### Project Structure Notes

- `solveConstrainedHomography()` in `HomographySolver` — pure Kotlin, no Android deps
- `estimateFromPaperGeometry()` and `estimateFromDelta()` in `OverlayTransformCalculator`
- All helper methods (`reconstructH`, `evalConstraints`) are private in `HomographySolver`

### References

- [Source: core/overlay/HomographySolver.kt:205-302 — solveConstrainedHomography()]
- [Source: core/overlay/HomographySolver.kt:304-337 — reconstructH(), evalConstraints()]
- [Source: core/overlay/OverlayTransformCalculator.kt:184-242 — estimateFromDelta() dispatcher]
- [Source: core/overlay/OverlayTransformCalculator.kt:250-279 — estimateFromPaperGeometry()]
- [Source: core/overlay/OverlayTransformCalculator.kt:286-317 — computeSimilarityParams(), applySimilarity()]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

### Completion Notes List

- Newton-Raphson solver converges in 3-5 iterations for moderate tilt
- Sub-5px accuracy at 20 degree tilt validated by unit tests
- Divergence protection: |h7|, |h8| > 0.1 → null, graceful affine fallback
- State mutation trap documented and fixed (fresh calculator per test iteration)
- Full degradation hierarchy: 4→3→2→1→0 markers with appropriate estimation method

### Change Log

- 2026-02-13: Constrained homography solver and delta-based estimation implemented
- 2026-02-14: Integration with paper geometry and focal length
- 2026-02-18: Story documented retroactively

### File List

**Modified files:**
- core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/HomographySolver.kt
- core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransformCalculator.kt
- core/overlay/src/test/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransformCalculatorTest.kt
- core/overlay/src/test/kotlin/io/github/jn0v/traceglass/core/overlay/HomographySolverTest.kt

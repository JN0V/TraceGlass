# Story 8.1: Homography Solver & Perspective Rendering Infrastructure

Status: done

## Story

As a developer,
I want a pure Kotlin homography solver and a perspective-aware overlay rendering pipeline,
So that the overlay image can be warped onto the paper quadrilateral detected by markers instead of using a simple affine transform.

## Acceptance Criteria

1. **Given** 4 paper corner markers are detected
   **When** the overlay is displayed
   **Then** a homography maps the overlay view rect onto the detected paper quadrilateral in screen space
   **And** the overlay follows perspective distortion (trapezoid warping)

2. **Given** fewer than 4 paper corners are known
   **When** the overlay is displayed
   **Then** the system falls back to affine rendering (translation + scale + rotation via `graphicsLayer`)

3. **Given** the user adjusts overlay manually (drag, pinch, rotate) while homography is active
   **When** the combined transform is computed
   **Then** manual rotation/scale are applied in view space before H, and offset is applied after H
   **And** the interaction feels natural (drag moves, pinch scales within paper)

4. **Given** the user opens Settings
   **When** they look at the tracking section
   **Then** a toggle "Perspective correction" is available (enabled by default)

5. **Given** the homography solver receives degenerate input (collinear points, near-singular)
   **When** `solveHomography()` is called
   **Then** it returns null (determinant check: |det| < 0.01 or > 100)
   **And** the system falls back to affine without crashing

## Tasks / Subtasks

- [x] Task 1: Pure Kotlin 4-point DLT homography solver (AC: #1, #5)
  - [x] 1.1 Create `HomographySolver.kt` in `:core:overlay`
  - [x] 1.2 `solveHomography(src, dst): FloatArray?` — 8x8 Gaussian elimination with partial pivoting
  - [x] 1.3 Return `FloatArray(9)` row-major (android.graphics.Matrix layout)
  - [x] 1.4 Determinant sanity check: null if |det| < 0.01 or > 100
  - [x] 1.5 `solveAffine(src, dst): FloatArray?` — 3-point affine solver (6x6)
  - [x] 1.6 `gaussianEliminationN()` — general NxN solver reused by all methods

- [x] Task 2: Pure Kotlin matrix utilities (AC: #3)
  - [x] 2.1 Create `MatrixUtils.kt` in `:core:overlay`
  - [x] 2.2 `multiply(a, b): FloatArray` — 3x3 matrix multiplication
  - [x] 2.3 `compose(vararg matrices): FloatArray` — chain multiplication
  - [x] 2.4 `translate(tx, ty)`, `scale(s)`, `rotate(degrees)` — factory methods
  - [x] 2.5 Unit tests: identity, compose(translate, scale), round-trip

- [x] Task 3: Paper-mapping rendering in TracingViewModel (AC: #1, #2, #3)
  - [x] 3.1 In `updateOverlayFromCombined()`: when `paperCornersFrame` has 4 corners, compute homography from view rect to screen-space paper corners
  - [x] 3.2 View rect: centered in view, matching `paperAspectRatio` from reference
  - [x] 3.3 Manual adjustments: rotation/scale via `MatrixUtils.compose` in view space before H, offset after H
  - [x] 3.4 Separate high-frequency `renderMatrix: StateFlow<FloatArray?>` to avoid full tree recomposition
  - [x] 3.5 Fallback: `computeAffineMatrix()` when corners null or H solver fails

- [x] Task 4: Compose rendering with matrix (AC: #1, #2)
  - [x] 4.1 `Modifier.drawWithContent { drawIntoCanvas { nativeCanvas.concat(matrix) } }` when renderMatrix non-null
  - [x] 4.2 `Modifier.graphicsLayer { translationX/Y, scaleX/Y, rotationZ }` when renderMatrix null (affine path)
  - [x] 4.3 Convert `FloatArray(9)` to `android.graphics.Matrix` via `.setValues()`

- [x] Task 5: Settings toggle (AC: #4)
  - [x] 5.1 `perspectiveCorrectionEnabled: Boolean = true` in SettingsData
  - [x] 5.2 `setPerspectiveCorrectionEnabled()` in SettingsRepository
  - [x] 5.3 Switch row in SettingsScreen (ListItem + Switch + HorizontalDivider)

- [x] Task 6: Regression & unit tests (AC: all)
  - [x] 6.1 HomographySolverTest: identity, translation, scale, perspective, degenerate, round-trip (~20 tests)
  - [x] 6.2 MatrixUtilsTest: identity, compose, translate, scale, rotate, inverse
  - [x] 6.3 All existing TracingViewModelTest and OverlayTransformCalculatorTest pass unchanged
  - [x] 6.4 `./gradlew :core:overlay:test :feature:tracing:test` green

## Dev Notes

### Critical Architecture: Homography-Based Paper Mapping

The overlay is no longer positioned by affine transform alone. When 4 paper corners are available (from OverlayTransformCalculator), the ViewModel computes:

```
H = solveHomography(viewCorners, screenCorners)
renderMatrix = compose(mOffset, H, mView)
```

Where:
- `viewCorners`: centered rect in view matching paper AR
- `screenCorners`: paper corners converted from frame space to screen space via F2S
- `mView`: manual rotation + scale in view space (applied before H)
- `mOffset`: manual drag offset in screen space (applied after H)

### Key Design Decision: Pure Kotlin Math

All matrix operations use pure Kotlin (`FloatArray(9)` + `MatrixUtils`):
- `android.graphics.Matrix` is NOT available in JUnit 5 unit tests
- ViewModel code is fully testable without Android framework mocking
- `MatrixUtils.multiply/compose/translate/scale/rotate` replace `android.graphics.Matrix` operations
- Only the Compose rendering layer (TracingContent) uses `android.graphics.Matrix.setValues()` for `nativeCanvas.concat()`

### Evolution from Original 2-Marker Approach

The initial implementation attempted perspective correction from 2 markers (outer corners of left/right markers). This was abandoned because:
- 2 markers at paper top provide ~3cm vertical baseline for ~30cm of paper
- 1-2px detection noise amplifies into large distortion at paper bottom
- The `perspectiveMatrix` field on `OverlayTransform` was replaced by `paperCornersFrame`

The current 4-corner paper-mapping approach works because each corner is independently tracked by its own ArUco marker (IDs 0-3), providing a reliable quadrilateral.

### Project Structure Notes

- `HomographySolver` and `MatrixUtils` live in `:core:overlay` (pure Kotlin, no Android deps)
- Rendering via `nativeCanvas.concat()` lives in `:feature:tracing` (Android Compose)
- Settings toggle follows existing pattern in `SettingsData` / `SettingsRepository`

### References

- [Source: core/overlay/HomographySolver.kt — solveHomography(), solveAffine(), gaussianEliminationN()]
- [Source: core/overlay/MatrixUtils.kt — compose(), multiply(), translate(), scale(), rotate()]
- [Source: feature/tracing/TracingViewModel.kt:390-455 — updateOverlayFromCombined() paper-mapping branch]
- [Source: feature/tracing/TracingContent.kt — drawWithContent + nativeCanvas.concat() rendering]
- [Source: core/overlay/OverlayTransform.kt — paperCornersFrame, paperAspectRatio fields]
- [Source: core/session/SettingsData.kt — perspectiveCorrectionEnabled toggle]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

### Completion Notes List

- Pure Kotlin HomographySolver: 4-point DLT (8x8), 3-point affine (6x6), general NxN Gaussian elimination
- MatrixUtils provides all 3x3 operations without android.graphics.Matrix dependency
- Paper-mapping branch active: when 4 corners available, overlay warps to paper quadrilateral
- Manual gestures compose correctly: rotation/scale before H, offset after H
- Separate `renderMatrix` StateFlow avoids full UI tree recomposition on every frame
- Settings toggle defaults to enabled
- Original 2-marker perspectiveMatrix approach was abandoned in favor of paper-mapping via 4 corners

### Change Log

- 2026-02-09: Story 8.1 first draft (2-marker approach)
- 2026-02-11: Implementation complete, 2-marker approach disabled (insufficient baseline)
- 2026-02-13: Evolved to 4-corner paper-mapping with full homography rendering
- 2026-02-18: Story rewritten to document actual architecture

### File List

**New files:**
- core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/HomographySolver.kt
- core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/MatrixUtils.kt
- core/overlay/src/test/kotlin/io/github/jn0v/traceglass/core/overlay/HomographySolverTest.kt
- core/overlay/src/test/kotlin/io/github/jn0v/traceglass/core/overlay/MatrixUtilsTest.kt

**Modified files:**
- core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransform.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingUiState.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingContent.kt
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/SettingsData.kt
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/SettingsRepository.kt
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/DataStoreSettingsRepository.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/settings/SettingsScreen.kt

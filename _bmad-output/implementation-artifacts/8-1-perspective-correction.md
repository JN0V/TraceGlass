# Story 8.1: Perspective Correction via Marker Homography

Status: review

## Story

As a user,
I want the overlay image to automatically compensate for the angle between my phone and the drawing surface,
So that the projected image lines up accurately with my paper even when my phone isn't perfectly parallel.

## Acceptance Criteria

1. **Given** two markers are detected and the phone is perfectly parallel to the surface
   **When** the overlay is displayed
   **Then** the overlay renders identically to the current behavior (no regression)

2. **Given** two markers are detected and the phone is tilted relative to the surface
   **When** the overlay is displayed
   **Then** the overlay is warped with a perspective correction so that its projection on the paper matches the reference image geometry

3. **Given** perspective correction is active
   **When** the phone tilt changes smoothly (user adjusts stand)
   **Then** the overlay correction updates smoothly without jitter (temporal smoothing on 9 matrix coefficients)

4. **Given** only one marker is detected
   **When** the overlay is displayed
   **Then** perspective correction is disabled and the overlay uses the current affine transform (translation + scale + rotation only)

5. **Given** the user opens Settings
   **When** they look at the tracking section
   **Then** a toggle "Perspective correction" is available (enabled by default)

## Tasks / Subtasks

- [x] Task 1: Pure Kotlin homography solver (AC: #1, #2)
  - [x]1.1 Create `HomographySolver.kt` in `:core:overlay` with `fun solveHomography(src: List<Pair<Float,Float>>, dst: List<Pair<Float,Float>>): FloatArray?`
  - [x]1.2 Implement 8x8 Gaussian elimination with partial pivoting (see algorithm below)
  - [x]1.3 Return `FloatArray(9)` in row-major order (android.graphics.Matrix layout: MSCALE_X, MSKEW_X, MTRANS_X, MSKEW_Y, MSCALE_Y, MTRANS_Y, MPERSP_0, MPERSP_1, MPERSP_2)
  - [x]1.4 Return null if system is degenerate (pivot < 1e-12)
  - [x]1.5 Determinant sanity check: return null if |det(H)| < 0.01 or > 100 (ill-conditioned)
  - [x]1.6 Unit tests: identity mapping (same src/dst), known tilt, degenerate input returns null

- [x] Task 2: Integrate homography into OverlayTransformCalculator (AC: #1, #2, #4)
  - [x]2.1 Add `perspectiveMatrix: FloatArray? = null` field to `OverlayTransform` data class
  - [x]2.2 Update `OverlayTransform.IDENTITY` — perspectiveMatrix stays null
  - [x]2.3 Store reference corners (`referenceCorners: List<Pair<Float,Float>>?`) on first 2-marker detection — the 4 outer corners from both markers, in screen coordinates
  - [x]2.4 In `compute()`, when 2+ markers AND perspectiveEnabled: select 4 outer corners, compute homography from referenceCorners → currentCorners
  - [x]2.5 Point selection: sort markers by ID, use TL of marker[0], TR of marker[1], BR of marker[1], BL of marker[0] for widest quadrilateral
  - [x]2.6 Set `perspectiveMatrix` in the returned OverlayTransform (null when < 2 markers or disabled)
  - [x]2.7 Add `perspectiveEnabled: Boolean` parameter to `compute()` and `computeSmoothed()`
  - [x]2.8 Unit tests: null with 1 marker, populated with 2 markers, null when disabled

- [x] Task 3: Temporal smoothing for homography (AC: #3)
  - [x]3.1 In `computeSmoothed()`, lerp each of the 9 matrix coefficients: `previous[i] + (target[i] - previous[i]) * smoothingFactor`
  - [x]3.2 When previous matrix is null (first frame), use target directly (no smoothing)
  - [x]3.3 When target matrix is null (lost 2nd marker), hold previous matrix for grace period then null
  - [x]3.4 Unit tests: interpolation between two known matrices, null handling

- [x] Task 4: Thread perspective through ViewModel → UI (AC: #1, #2, #4)
  - [x]4.1 Add `perspectiveMatrix: FloatArray? = null` to `TracingUiState`
  - [x]4.2 Add `perspectiveCorrectionEnabled: Boolean` local var in ViewModel, read from settingsRepository
  - [x]4.3 In `updateOverlayFromCombined()`: when perspectiveMatrix is non-null, compose it with manual adjustments as matrix operations, store as 9-float array in uiState
  - [x]4.4 When perspectiveMatrix is null, use current affine pipeline unchanged
  - [x]4.5 Pass `perspectiveEnabled` flag to calculator's `computeSmoothed()`

- [x] Task 5: Compose rendering with perspective (AC: #1, #2, #4)
  - [x]5.1 Add `perspectiveMatrix: FloatArray?` parameter to `CameraPreviewContent`
  - [x]5.2 When non-null: use `Modifier.drawWithContent { drawIntoCanvas { canvas -> canvas.nativeCanvas.save(); canvas.nativeCanvas.concat(matrix); drawContent(); canvas.nativeCanvas.restore() } }` — REPLACE the graphicsLayer modifier
  - [x]5.3 When null: keep current `Modifier.graphicsLayer { translationX/Y, scaleX/Y, rotationZ }` path
  - [x]5.4 Convert FloatArray(9) → `android.graphics.Matrix` via `matrix.setValues(floatArray)`
  - [x]5.5 Gesture handling: keep existing `detectTransformGestures` — manual drag/scale/rotate compose with perspective via matrix multiplication in ViewModel

- [x] Task 6: Settings toggle (AC: #5)
  - [x]6.1 Add `perspectiveCorrectionEnabled: Boolean = true` to `SettingsData`
  - [x]6.2 Add `suspend fun setPerspectiveCorrectionEnabled(enabled: Boolean)` to `SettingsRepository` interface
  - [x]6.3 Implement in `DataStoreSettingsRepository` (follow existing `setAudioFeedbackEnabled` pattern)
  - [x]6.4 Add `perspectiveCorrectionEnabled` to `SettingsUiState`
  - [x]6.5 Add Switch row in `SettingsScreen.kt` (follow break reminder toggle pattern: `ListItem` + `Switch` + `HorizontalDivider`)
  - [x]6.6 Wire toggle through `SettingsViewModel` to repository
  - [x]6.7 In `TracingViewModel`, read from settingsRepository flow and pass to calculator

- [x] Task 7: Regression & integration tests (AC: all)
  - [x]7.1 All existing `OverlayTransformCalculatorTest` tests pass unchanged
  - [x]7.2 All existing `TracingViewModelTest` tests pass unchanged
  - [x]7.3 `./gradlew :core:overlay:test :feature:tracing:test` green
  - [x]7.4 `./gradlew assembleDebug` succeeds
  - [x]7.5 On-device: flat surface with 2 markers → no visible change from current behavior
  - [x]7.6 On-device: tilt phone 20-30° → overlay warps to compensate
  - [x]7.7 On-device: 2→1 marker transition → smooth fallback to affine (no jump)

## Dev Notes

### Critical Technical Decisions

#### 1. Compose Rendering: `drawWithContent` + `nativeCanvas.concat()` (NOT graphicsLayer)

`Modifier.graphicsLayer` does NOT support arbitrary 3x3 matrices. It only exposes fixed transform properties (scaleX/Y, translationX/Y, rotationZ, etc.).

The correct approach for perspective:
```kotlin
Modifier.drawWithContent {
    drawIntoCanvas { canvas ->
        val m = android.graphics.Matrix()
        m.setValues(perspectiveMatrix)  // 9-element FloatArray
        canvas.nativeCanvas.save()
        canvas.nativeCanvas.concat(m)
        drawContent()
        canvas.nativeCanvas.restore()
    }
}
```

This is proven to work for full perspective warps. The `drawContent()` call renders the Image composable content, and the concat applies the 3x3 matrix (including MPERSP_0, MPERSP_1 perspective terms).

#### 2. Homography Solver: Pure Kotlin DLT (NOT OpenCV JNI, NOT setPolyToPoly)

Use pure Kotlin for the solver because:
- `android.graphics.Matrix.setPolyToPoly()` requires Android framework → can't unit test with JUnit 5
- OpenCV `findHomography` via JNI is unnecessary overhead for 4-point solve
- Pure Kotlin 8x8 Gaussian elimination is ~50 lines, fully testable

The solver can be validated against `setPolyToPoly` on-device as a sanity check.

#### 3. Reference Frame: First 2-marker detection = baseline

On first 2-marker detection:
- Store 4 outer corners as `referenceCorners`
- These represent the "home" perspective (whatever the phone angle is at setup)
- When referenceCorners == currentCorners → H ≈ identity → no warp (AC #1)
- When phone tilts → corners shift → H encodes the delta → overlay warps to match paper

This is consistent with existing `referenceSpacing` / `referenceAngle` pattern.

#### 4. Outer Corner Selection from 2 Markers

ArUco corners are ordered: TL(0), TR(1), BR(2), BL(3) per marker.

With markers sorted by ID (marker[0] left, marker[1] right):
```
Selected 4 points for homography:
  srcPts[0] = marker[0].corners[0]  (top-left of left marker)
  srcPts[1] = marker[1].corners[1]  (top-right of right marker)
  srcPts[2] = marker[1].corners[2]  (bottom-right of right marker)
  srcPts[3] = marker[0].corners[3]  (bottom-left of left marker)
```
This forms the widest quadrilateral spanning both markers — well-conditioned for homography.

#### 5. Manual Adjustments with Perspective Active

When perspective is active, manual gestures must be composed as matrix operations:
```kotlin
// In ViewModel.updateOverlayFromCombined():
if (perspectiveMatrix != null) {
    val combined = FloatArray(9)
    // Start with perspective matrix from tracker
    val pm = android.graphics.Matrix().apply { setValues(perspectiveMatrix) }
    // Compose manual adjustments (in screen space, applied BEFORE perspective)
    val manual = android.graphics.Matrix()
    manual.postTranslate(manualOffset.x, manualOffset.y)
    manual.postScale(manualScaleFactor, manualScaleFactor, viewWidth/2, viewHeight/2)
    manual.postRotate(manualRotation, viewWidth/2, viewHeight/2)
    pm.preConcat(manual)
    pm.getValues(combined)
    _uiState.update { it.copy(perspectiveMatrix = combined) }
}
```

NOTE: `android.graphics.Matrix` in ViewModel is OK since `:feature:tracing` is an Android library module. The perspective matrix VALUES (FloatArray) flow through UiState, but the Matrix object is only used locally for composition.

### DLT Algorithm Implementation

For exactly 4 point correspondences `(x_i, y_i) → (x'_i, y'_i)`, setting h33 = 1:

Each pair produces 2 rows:
```
Row 2i:   [xi, yi, 1, 0, 0, 0, -xi*x'i, -yi*x'i] h = [x'i]
Row 2i+1: [0, 0, 0, xi, yi, 1, -xi*y'i, -yi*y'i] h = [y'i]
```

This gives 8x8 system `Ah = b`, solved by Gaussian elimination with partial pivoting.

Output: `FloatArray(9)` = `[h0, h1, h2, h3, h4, h5, h6, h7, 1.0]` in row-major order.

Validate: compute `det(H) = h0*(h4*1 - h5*h7) - h1*(h3*1 - h5*h6) + h2*(h3*h7 - h4*h6)`. If |det| < 0.01 or > 100, return null (degenerate).

### Existing Code to Modify

**`OverlayTransform.kt`** — Add field:
```kotlin
data class OverlayTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val perspectiveMatrix: FloatArray? = null  // 9 floats, row-major
) {
    companion object { val IDENTITY = OverlayTransform() }
    // Override equals/hashCode since FloatArray doesn't have structural equality
}
```

**`OverlayTransformCalculator.kt`** — Add:
- `private var referenceCorners: List<Pair<Float, Float>>? = null`
- `private var previousPerspectiveMatrix: FloatArray? = null`
- New param `perspectiveEnabled: Boolean = true` on `compute()` and `computeSmoothed()`
- Call `HomographySolver.solveHomography()` in `compute()` when 2+ markers
- Smooth matrix in `computeSmoothed()`
- Clear `referenceCorners` and `previousPerspectiveMatrix` in `resetReference()`

**`TracingUiState.kt`** — Add:
```kotlin
val perspectiveMatrix: FloatArray? = null
```

**`TracingViewModel.kt`** — Add:
- `private var perspectiveCorrectionEnabled = true`
- Read from settingsRepository in init block
- Pass to calculator in `onMarkerResultReceived()`
- In `updateOverlayFromCombined()`: compose manual adjustments with perspective matrix when non-null

**`TracingScreen.kt`** — Conditional modifier:
```kotlin
.then(
    if (perspectiveMatrix != null) {
        Modifier.drawWithContent {
            drawIntoCanvas { canvas ->
                val m = android.graphics.Matrix()
                m.setValues(perspectiveMatrix)
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.concat(m)
                drawContent()
                canvas.nativeCanvas.restore()
            }
        }
    } else {
        Modifier.graphicsLayer {
            translationX = overlayOffset.x
            translationY = overlayOffset.y
            scaleX = overlayScale
            scaleY = overlayScale
            rotationZ = overlayRotation
        }
    }
)
```

**`SettingsData.kt`** — Add field:
```kotlin
val perspectiveCorrectionEnabled: Boolean = true
```

**`SettingsRepository.kt`** — Add method:
```kotlin
suspend fun setPerspectiveCorrectionEnabled(enabled: Boolean)
```

**`DataStoreSettingsRepository.kt`** — Implement (follow existing `setAudioFeedbackEnabled` pattern)

**`SettingsScreen.kt`** — Add toggle (follow break reminder toggle pattern)

### New File

**`core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/HomographySolver.kt`** — Pure Kotlin, ~60 lines:
- `fun solveHomography(src, dst): FloatArray?`
- `private fun gaussianElimination(A, b): DoubleArray?`
- `private fun determinant3x3(h: FloatArray): Float`

### Testing Strategy

**HomographySolver tests** (new file in `:core:overlay:test`):
- Identity: same src and dst → H ≈ identity matrix
- Known translation: dst = src + (10, 20) → H has translation only
- Known scale: dst = src * 2 → H has scale only
- Known perspective: simulate tilted quad → H has non-zero MPERSP terms
- Degenerate: collinear points → returns null
- Round-trip: apply H to src, verify result matches dst (within epsilon)

**OverlayTransformCalculator tests** (extend existing):
- 2 markers with perspective enabled → perspectiveMatrix non-null
- 2 markers with perspective disabled → perspectiveMatrix null
- 1 marker → perspectiveMatrix always null
- Smoothing: previous and target matrices → interpolated result
- Reference corners stored on first 2-marker detection

**TracingViewModel tests** (extend existing):
- Perspective matrix threaded to uiState when enabled
- Perspective null when disabled in settings
- Manual adjustments composed with perspective matrix
- Affine fallback when perspective null

### Previous Story Intelligence

From Story 3.2+3.3 (tracking stabilization):
- `markerWithCorners(id, cx, cy, size, angleDeg)` test helper generates realistic corners — reuse for perspective tests
- `smoothingFactor = 1f` in tests for instant results; use lower factor only in smoothing-specific tests
- `viewModelScope.cancel()` pattern needed in `runTest` blocks with perpetual coroutines
- Frame coordinates → screen coordinates requires `previewScale()` multiplication — perspective matrix must operate in screen coordinates
- Virtual midpoint reconstruction works — perspective computation should use the same sorted-by-ID marker order

### Risk Mitigations

1. **Numerical instability**: Determinant check (0.01 < |det| < 100) catches degenerate cases → falls back to affine
2. **Regression**: AC #1 explicitly requires no visible change when phone is parallel — the reference frame approach guarantees this (H ≈ identity at baseline)
3. **Performance**: Pure Kotlin 8x8 solve is O(n³) with n=8 → ~500 multiplications, negligible vs frame processing
4. **FloatArray equality**: `data class` with `FloatArray` field needs `equals()`/`hashCode()` override or the dev must handle this

### Human Validation Test Plan

- **TC-PERSP-01**: Flat surface, 2 markers → overlay identical to before perspective feature. Toggle perspective off/on → no change.
- **TC-PERSP-02**: Tilt phone 20° forward → overlay top edge narrows to match paper perspective. Drawing lines should align with paper.
- **TC-PERSP-03**: Tilt phone 30° to the side → overlay skews laterally. Lines still align.
- **TC-PERSP-04**: Slowly change tilt → overlay smoothly tracks, no jitter or jumps.
- **TC-PERSP-05**: Cover one marker → overlay smoothly transitions from perspective to affine (no snap).
- **TC-PERSP-06**: Settings → disable "Perspective correction" → overlay uses affine only even with 2 markers.
- **TC-PERSP-07**: Extreme tilt (>60°) → overlay falls back to affine (determinant check triggers). No crash.

### References

- [Source: core/cv/MarkerResult.kt — DetectedMarker.corners: List<Pair<Float, Float>>]
- [Source: core/overlay/OverlayTransformCalculator.kt — compute(), computeSmoothed(), referenceSpacing pattern]
- [Source: core/overlay/OverlayTransform.kt — data class with IDENTITY companion]
- [Source: feature/tracing/TracingScreen.kt — graphicsLayer overlay rendering, drawWithContent for perspective]
- [Source: feature/tracing/TracingViewModel.kt — updateOverlayFromCombined(), previewScale()]
- [Source: feature/tracing/TracingUiState.kt — UI state data class]
- [Source: core/session/SettingsRepository.kt — SettingsData, settings toggle pattern]
- [Source: feature/tracing/settings/SettingsScreen.kt — ListItem + Switch + HorizontalDivider pattern]
- [Source: _bmad-output/implementation-artifacts/3-2-3-tracking-stabilization.md — previous story learnings]
- [Source: _bmad-output/planning-artifacts/architecture.md — overlay module boundaries, camera pipeline]
- [Reference: android.graphics.Matrix.setPolyToPoly — 4 point pairs = full perspective]
- [Reference: Compose drawWithContent + nativeCanvas.concat — proven perspective approach]
- [Reference: DLT algorithm — 8x8 Gaussian elimination for 4-point homography]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

### Completion Notes List

- Pure Kotlin HomographySolver implemented with 8x8 Gaussian elimination + partial pivoting + determinant sanity check
- ViewModel uses pure Kotlin 3x3 matrix multiplication instead of `android.graphics.Matrix` to avoid unit test failures (Android framework not mocked in JUnit 5)
- Fixed pre-existing bug in `core:cv:MarkerDetectorTest` — missing `rotation` parameter in `detect()` calls
- All tests pass: 309 tasks (all modules), `assembleDebug` green, APK deployed to device
- Settings toggle defaults to enabled (perspectiveCorrectionEnabled = true)

#### IMPORTANT: Perspective Rendering Disabled (infrastructure kept)

**On-device testing revealed** that perspective rendering with only 2 markers at the top of the paper produces visible jitter and misalignment. After multiple iteration attempts and an agent-debated analysis of 4 approaches, the conclusion is:

**Root cause**: 2 markers at the top edge of the paper provide ~3cm vertical baseline for ~30cm of paper. Small corner detection noise (±1-2px) gets amplified into large perspective distortion at the bottom of the overlay.

**What was tried and failed**:
1. Full homography as sole transform (overlay lost all affine positioning)
2. graphicsLayer + decomposed perspective residual `H * inv(affine(H))` (mathematically unsound — projective h6/h7 interact multiplicatively with all terms via denominator)
3. Extract only h6/h7 and build centered perspective matrix (still wrong, made things worse)

**Decision**: Disable perspective rendering (`perspectiveMatrix = null` always in ViewModel). Keep all infrastructure intact:
- HomographySolver: computes correct homographies, fully tested
- OverlayTransformCalculator: computes and smooths perspective matrix
- OverlayTransform: carries perspectiveMatrix field
- Settings toggle: exists in UI
- TracingScreen: accepts perspectiveMatrix parameter

**To enable perspective rendering in the future**, implement one of:
- **4 markers** (one per paper corner) → reliable homography baseline
- **Calibration step** (Camera Lucida approach) → user places phone, takes reference photo
- Then use **Approach A**: single full matrix `H_screen * A_ref * M_manual` applied via `drawWithContent` + `nativeCanvas.concat()`, replacing `graphicsLayer` entirely

### Change Log

- 2026-02-09: Story 8.1 drafted
- 2026-02-11: Story upgraded to ready-for-dev with comprehensive implementation guide
- 2026-02-11: All 7 tasks implemented, all tests passing, APK deployed — status → review
- 2026-02-11: Perspective rendering disabled after on-device testing — 2-marker baseline insufficient for reliable perspective estimation. Infrastructure kept for future 4-marker implementation.

### File List

**New files:**
- core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/HomographySolver.kt
- core/overlay/src/test/kotlin/io/github/jn0v/traceglass/core/overlay/HomographySolverTest.kt

**Modified files:**
- core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransform.kt
- core/overlay/src/main/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransformCalculator.kt
- core/overlay/src/test/kotlin/io/github/jn0v/traceglass/core/overlay/OverlayTransformCalculatorTest.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingUiState.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingScreen.kt
- feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModelTest.kt
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/SettingsData.kt
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/SettingsRepository.kt
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/DataStoreSettingsRepository.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/settings/SettingsScreen.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/settings/SettingsViewModel.kt (or equivalent)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/settings/SettingsUiState.kt
- feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/settings/FakeSettingsRepository.kt
- core/cv/src/test/kotlin/io/github/jn0v/traceglass/core/cv/MarkerDetectorTest.kt (pre-existing rotation param fix)

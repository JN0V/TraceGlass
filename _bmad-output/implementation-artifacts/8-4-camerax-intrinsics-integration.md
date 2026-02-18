# Story 8.4: CameraX Intrinsics Integration

Status: ready-for-dev

## Story

As a user,
I want the camera's focal length to be automatically extracted from CameraX and fed to the perspective correction system,
So that 3-marker tracking works reliably even when I start with my phone tilted.

## Acceptance Criteria

1. **Given** the camera is bound via CameraX
   **When** the camera is ready
   **Then** the focal length in pixels is computed from `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` and `SENSOR_INFO_PHYSICAL_SIZE`
   **And** passed to `OverlayTransformCalculator.setFocalLength()`

2. **Given** the focal length is injected from CameraX
   **When** the reference was captured with the phone tilted (non-rectangular detection)
   **Then** the constrained homography (Story 8.3) uses the injected focal length
   **And** 3-marker corner estimation works correctly

3. **Given** the focal length injection succeeds
   **When** `setFocalLength()` is called
   **Then** `rebuildPaperCoords()` corrects the paper aspect ratio
   **And** subsequent homography computations use the corrected rectangle

4. **Given** the device does not expose focal length data (rare edge case)
   **When** the camera is bound
   **Then** no focal length is injected
   **And** the system relies on auto-estimation (when reference is rectangular) or affine delta fallback

5. **Given** the user switches cameras or zoom changes
   **When** the effective focal length changes
   **Then** the new focal length is re-injected
   **And** paper coordinates are rebuilt

## Tasks / Subtasks

- [ ] Task 1: Focal length extraction from CameraX (AC: #1, #4)
  - [ ] 1.1 In `CameraXManager`, after `bindToLifecycle()`, extract:
    - `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` (mm)
    - `SENSOR_INFO_PHYSICAL_SIZE` (mm width, mm height)
    - `SENSOR_INFO_PIXEL_ARRAY_SIZE` or active array size (pixels)
  - [ ] 1.2 Compute focal length in pixels: `f_pixels = f_mm * pixelArrayWidth / sensorWidthMm`
  - [ ] 1.3 Expose via new method or callback: `fun getFocalLengthPixels(): Float?`
  - [ ] 1.4 Handle missing data gracefully: return null if any characteristic is unavailable

- [ ] Task 2: CameraManager interface extension (AC: #1)
  - [ ] 2.1 Add `val focalLengthPixels: StateFlow<Float?>` to `CameraManager` interface
  - [ ] 2.2 Implement in `CameraXManager`: emit after camera bind
  - [ ] 2.3 Add to `FakeCameraManager` for tests (configurable value)

- [ ] Task 3: Wire focal length to OverlayTransformCalculator (AC: #1, #2, #3)
  - [ ] 3.1 In `TracingViewModel`, collect `cameraManager.focalLengthPixels` flow
  - [ ] 3.2 On non-null emission: call `transformCalculator.setFocalLength(f)`
  - [ ] 3.3 Ensure this runs after reference is potentially established (order doesn't matter — `setFocalLength` sets `needsRebuildPaperCoords` flag, rebuild happens on next `compute()`)

- [ ] Task 4: Zoom-aware focal length (AC: #5)
  - [ ] 4.1 When `setWidestZoom()` changes the zoom ratio, adjust f_pixels proportionally
  - [ ] 4.2 `f_effective = f_pixels * zoomRatio` (digital zoom crops the sensor)
  - [ ] 4.3 Re-emit on zoom change (or compute once at bind if zoom is set before emission)

- [ ] Task 5: Unit tests (AC: all)
  - [ ] 5.1 TracingViewModelTest: focal length from FakeCameraManager reaches transformCalculator
  - [ ] 5.2 TracingViewModelTest: null focal length → no setFocalLength call
  - [ ] 5.3 Integration: tilted-first-detection + injected f → constrained H works
  - [ ] 5.4 FakeCameraManager: configurable focalLengthPixels for test scenarios
  - [ ] 5.5 `./gradlew :core:camera:test :feature:tracing:test` green

- [ ] Task 6: On-device validation (AC: #1, #2)
  - [ ] 6.1 Log extracted f_pixels on Pixel 9 Pro and OnePlus 5T
  - [ ] 6.2 Compare with known values:
    - OnePlus 5T: f=4.103mm, sensor=5.64×4.23mm, ~1170px at 1280 analysis
    - Pixel 9 Pro main: f=6.81mm, sensor varies by camera
  - [ ] 6.3 Verify 3-marker tracking improves with tilted-first-detection scenario

## Dev Notes

### Focal Length Calculation

The key formula converts from physical focal length (mm) to pixel focal length:

```
f_pixels = f_mm * pixelArrayWidth / sensorPhysicalWidthMm
```

CameraX Camera2 interop provides all needed data:
- `CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS` → `FloatArray` of focal lengths in mm
- `CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE` → `SizeF` in mm
- `CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE` → `Size` in pixels

**Important**: The analysis `ImageProxy` resolution (e.g. 1280x720) may differ from the sensor's full pixel array. The focal length should be computed relative to the analysis resolution:

```kotlin
val analysisWidth = imageProxy.width  // e.g. 1280
val sensorWidth = pixelArraySize.width  // e.g. 4032
val cropFactor = analysisWidth.toFloat() / sensorWidth
f_analysis = f_mm * sensorWidth / sensorPhysicalWidthMm * cropFactor
           = f_mm * analysisWidth / sensorPhysicalWidthMm
```

### Current State: Extraction Works, Wiring Missing

`CameraXManager` already extracts focal lengths at lines 131-135 and 166-177 (for camera selection and logging). The missing piece is:
1. Exposing the focal length as a `StateFlow`
2. Collecting it in `TracingViewModel`
3. Calling `transformCalculator.setFocalLength(f)`

### Principal Point Assumption

The system currently assumes `cx = frameWidth/2, cy = frameHeight/2`. For most modern phone cameras, this is accurate within a few pixels. A future enhancement could extract the true principal point from `LENS_INTRINSIC_CALIBRATION` (API 28+), but it's not required for sub-5px corner estimation at moderate tilt.

### Device-Specific Notes

**OnePlus 5T (Snapdragon 835)**:
- All 3 logical cameras report the same focal length (4.103mm)
- Sensor: 5.64 x 4.23mm
- At 1280px analysis width: `f_pixels ≈ 4.103 * 1280 / 5.64 ≈ 931px`

**Pixel 9 Pro**:
- Multiple cameras with different focal lengths
- `buildWideAngleSelector()` already picks the widest
- `setWidestZoom()` may further affect effective focal length

### Existing Code to Modify

**`CameraManager.kt`** — Add:
```kotlin
val focalLengthPixels: StateFlow<Float?>
```

**`CameraXManager.kt`** — Add:
```kotlin
private val _focalLengthPixels = MutableStateFlow<Float?>(null)
override val focalLengthPixels: StateFlow<Float?> = _focalLengthPixels.asStateFlow()

// In bindPreview(), after camera bind:
_focalLengthPixels.value = computeFocalLengthPixels(camera, analysisWidth)
```

**`TracingViewModel.kt`** — Add:
```kotlin
// In init or after camera bind:
cameraManager.focalLengthPixels
    .filterNotNull()
    .onEach { f -> transformCalculator.setFocalLength(f) }
    .launchIn(viewModelScope)
```

**`FakeCameraManager.kt`** — Add:
```kotlin
override val focalLengthPixels = MutableStateFlow<Float?>(null)
```

### Risk Mitigations

1. **Missing sensor data**: Return null, system falls back to auto-f (fronto-parallel reference) or affine delta
2. **Wrong zoom factor**: Log actual values on both target devices, validate against known camera specs
3. **Analysis resolution vs sensor resolution**: Compute f relative to analysis width, not sensor width
4. **Race condition**: `setFocalLength()` is safe to call at any time — if reference isn't set yet, the flag is stored and applied on first `computeWithPaperCorners()`

### References

- [Source: core/camera/impl/CameraXManager.kt:131-135 — focal length extraction for camera selection]
- [Source: core/camera/impl/CameraXManager.kt:162-177 — logSelectedCamera with focal/sensor data]
- [Source: core/camera/CameraManager.kt — interface to extend]
- [Source: core/overlay/OverlayTransformCalculator.kt:454-457 — setFocalLength() API]
- [Source: feature/tracing/TracingViewModel.kt:31-37 — constructor injection points]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

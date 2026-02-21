# TraceGlass — Spec vs Reality Audit

Generated: 2026-02-21
Audited against: commit `f2b1dc7` (HEAD of main)

---

## Legend

| Symbol | Meaning |
|--------|---------|
| OK | Spec implemented and working as intended |
| DRIFT | Implemented but deviates from spec |
| BROKEN | Implemented but not functioning correctly |
| MISSING | Specced but not implemented |
| EXTRA | Implemented but not in any spec |
| STALE | Sprint status out of sync with reality |

---

## 1. Sprint Status vs Git Reality

The `sprint-status.yaml` is **significantly out of date**.

| Story | sprint-status.yaml | Actual (git) | Issue |
|-------|-------------------|--------------|-------|
| Epic 9 | backlog | **done** (4 commits on main) | STALE |
| 9-1 | backlog | **done** (750c1d1) | STALE |
| 9-2 | backlog | **done** (c0f2d87) | STALE |
| 9-3 | backlog | **done** (69c1442) | STALE |
| 9-4 | backlog | **done** (a8ae267) | STALE |
| 4-1 | review | likely done (code present, tests pass) | STALE |
| 6-3 | review | likely done (code present) | STALE |
| 8-4 | review | **BROKEN** — wiring removed (f2b1dc7) | DRIFT |

---

## 2. Epic-by-Epic Audit

### Epic 1: Project Foundation & Camera Feed — OK

| Story | Status | Notes |
|-------|--------|-------|
| 1-1 Scaffolding | OK | 8-module Gradle, libs.versions.toml |
| 1-2 F-Droid | OK | Build reproducibility flags |
| 1-3 CI Pipeline | OK | GitHub Actions |
| 1-4 Live Camera | OK | CameraX, permissions, multi-camera selection |
| 1-5 Flashlight | OK | Torch toggle with hardware check |

### Epic 2: Core Tracing Experience — OK

| Story | Status | Notes |
|-------|--------|-------|
| 2-1 Image import | OK | Photo Picker API |
| 2-2 Overlay rendering | OK | Canvas + renderMatrix via drawWithContent |
| 2-3 Opacity control | OK | OpacityFAB + vertical slider |
| 2-4 Visual modes | OK | Color tint, inverted mode |
| 2-5 Positioning | DRIFT | `onOverlayGesture()` combined callback replaces separate drag/scale/rotate. Adds centroid-based scaling (not in spec). Functionally equivalent. |
| 2-6 Session control | OK | ExpandableMenu, wake lock, session FAB |
| 2-7 Lock + viewport | OK | Lock mechanism, viewport zoom/pan |

### Epic 3: Marker Tracking — OK

| Story | Status | Notes |
|-------|--------|-------|
| 3-1 OpenCV JNI | OK | JNI bridge with cached class/method IDs |
| 3-2 Stabilization | OK | Homography-based overlay positioning |
| 3-3 Resilience | DRIFT | Batch-apply pattern (all-or-nothing) replaces partial writes. More robust against corruption but may fail to estimate when old code succeeded. Net effect: minor. |
| 3-4 Status indicator | OK | Dual encoding, audio tones |

### Epic 4: Timelapse — OK (minor stale tracking)

| Story | Status | Notes |
|-------|--------|-------|
| 4-1 Snapshot capture | OK | FrameAnalyzer one-shot callback, periodic interval |
| 4-2 MP4 compilation | OK | MediaCodec H.264, progress reporting |
| 4-3 Export/share | OK | MediaStore export, system share intent |
| 4-4 Preservation | OK | Restore flow with continue/compile/discard dialog |

### Epic 5: Session Persistence — OK

| Story | Status | Notes |
|-------|--------|-------|
| 5-1 Auto-save | OK | Eager + debounce 500ms + NonCancellable |
| 5-2 Auto-restore | OK | Resume dialog, internal image copy |
| 5-3 Pause/resume | OK | Session FAB |

### Epic 6: Onboarding — OK (minor stale tracking)

| Story | Status | Notes |
|-------|--------|-------|
| 6-1 Carousel | OK | HorizontalPager, tier selection |
| 6-2 Walkthrough | OK | Live marker detection guidance |
| 6-3 Setup guides | OK | External links, guide screens (marked "review" but code present) |
| 6-4 Flow control | OK | Re-access from settings, completion flag |

### Epic 7: Settings — OK

| Story | Status | Notes |
|-------|--------|-------|
| 7-1 Settings screen | OK | Audio, break reminder, perspective correction, about |
| 7-2 Break reminders | OK | Configurable interval (1-120 min), snackbar + tone |
| 7-3 Icon/about | OK | Adaptive icon, version display |

### Epic 8: Advanced Tracking — DRIFT on 8-4

| Story | Status | Notes |
|-------|--------|-------|
| 8-1 Perspective correction | OK | Homography solver, paper-mapping rendering, settings toggle |
| 8-2 Paper-size agnostic | OK | 4 ArUco markers, auto-f estimation, AR correction |
| 8-3 3-marker constrained H | OK | Newton-Raphson solver, delta fallback hierarchy |
| **8-4 CameraX intrinsics** | **BROKEN** | **Code exists** (FocalLengthCalculator, CameraXManager.emitFocalLength, FakeCameraManager) **but wiring removed** from TracingViewModel in f2b1dc7. The CameraX focal length was incorrect on multi-camera devices (Pixel 9 Pro: zoom ratio / lens mismatch), causing tracking regression. Auto-estimation from marker geometry is used instead. |

### Epic 9: Robustness — STALE tracking, code done

| Story | Status | Notes |
|-------|--------|-------|
| 9-1 Thread safety | OK | @Volatile annotations, batch-apply, null-safe guards |
| 9-2 ViewModel state | OK | AtomicReference RestoreState, safe primitives |
| 9-3 i18n strings | OK | All hardcoded strings extracted to strings.xml |
| 9-4 Lifecycle | OK | FrameAnalyzer.close(), CameraXManager.close(), executor management |

---

## 3. Key Discrepancies Summary

### BROKEN: Story 8-4 (CameraX Intrinsics)

**Spec says:** Extract focal length from CameraX Camera2 intrinsics, inject into transform calculator for improved 3-marker tracking when phone starts tilted.

**Reality:** The extraction code exists and works (`FocalLengthCalculator`, `CameraXManager.emitFocalLength()`), but the **wiring in TracingViewModel was removed** because:
- On Pixel 9 Pro, `setWidestZoom()` sets zoom to minZoomRatio (~0.5x), switching to ultra-wide lens
- `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` still reports the main lens focal length
- Result: wrong f_pixels → wrong `rebuildPaperCoords()` → wrong homographies → broken tracking

**Impact:** 3-marker tracking without prior fronto-parallel reference relies on affine delta fallback (less accurate under tilt, but stable). This is acceptable for the current use case.

**To fix properly (future):** Would need to query focal length of the ACTIVE physical camera, not the logical camera's default.

### STALE: Sprint status vs reality

Epic 9 (all 4 stories) is committed and merged but marked as "backlog". Stories 4-1, 6-3, 8-4 are marked as "review" but code is on main.

### DRIFT: Minor behavioral changes (non-breaking)

1. **Gesture handling** (2-5): Combined `onOverlayGesture()` with centroid-based scaling replaces 3 separate callbacks. Functionally improved.
2. **Hidden corner estimation** (3-3): All-or-nothing batch pattern replaces partial writes. More robust.
3. **HomographySolver guards**: Stricter NaN/near-square rejection. May cause more fallbacks to affine, but prevents bad homographies.
4. **TrackingStateManager**: `nanoTime` instead of `currentTimeMillis`. Correctness fix.

---

## 4. Dead Code from Story 8-4

The following code is implemented but **orphaned** (no caller):

| File | Element | Purpose | Status |
|------|---------|---------|--------|
| CameraManager.kt | `focalLengthPixels: StateFlow<Float?>` | Interface contract | No consumer |
| CameraXManager.kt | `emitFocalLength()`, `reemitFocalLength()` | Compute & emit f_pixels | Called internally but flow never collected |
| CameraXManager.kt | `storedFocalMm`, `storedSensorWidthMm`, `actualAnalysisWidth` | Re-emission state | Unused |
| FocalLengthCalculator.kt | `computePixels()` | Pure function | Only called by CameraXManager (whose output is uncollected) |
| FocalLengthCalculatorTest.kt | 8 tests | Unit tests for dead code | Pass but test orphaned code |
| FakeCameraManager.kt | `focalLengthPixels` | Test double | Used in tests for orphaned feature |
| OverlayTransformCalculator.kt | `setFocalLength()` | External f injection | Still callable but never called |
| OverlayTransformCalculator.kt | `isFocalLengthExternal`, related `@Volatile` fields | External f tracking | Never triggered |
| TracingViewModelTest.kt | `FocalLengthInjection` test group (4 tests) | Wiring tests | Test removed wiring |

**Decision needed:** Keep dead code (in case focal length is fixed later) or remove it for cleanliness?

---

## 5. What's NOT Specced but Exists

| Feature | Location | Notes |
|---------|----------|-------|
| `contentEquals` optimization on renderMatrix | TracingViewModel:817 | Prevents unnecessary recompositions. Not in any story. Good optimization. |
| `perspectiveCorrectionEnabled` setting toggle | SettingsScreen, SettingsRepository | Added as part of 8-1 but not a standalone story. |
| Division-by-zero guards on scale computation | OverlayTransformCalculator:391,396 | Defensive coding from Epic 9 review. |

---

## 6. Recommendation

1. **Update sprint-status.yaml** to reflect reality (Epic 9 done, stale "review" items → done, 8-4 → done-with-caveat)
2. **Decide on dead code**: keep or remove FocalLengthCalculator pipeline
3. **Story 8-4 needs a new AC or a note**: "CameraX intrinsics deferred due to multi-camera lens mismatch; auto-estimation sufficient for current use cases"

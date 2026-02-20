# Adversarial Review — Remaining Backlog

Date: 2026-02-20
Source: Full-app adversarial review (40 findings total)
Status: **Completed — 12 fixed inline, 4 stories (Epic 9), 14 fixed below, 1 deferred (NFR conflict), 3 epic-level remaining**

## Review Notes
- Adversarial review completed (24 findings)
- Findings: 1 real (fixed), 23 noise/false/pre-existing (skipped)
- Resolution approach: auto-fix

---

## HIGH

### ~~#14 — ProGuard rules missing for Koin-injected implementations~~ FIXED

- [x] Added keep rules for all Koin-registered impl classes in `app/proguard-rules.pro`
- [x] Added wildcard rule for `**.di.*Module*` classes

---

## MEDIUM

### #16 — No crash reporting or analytics — DEFERRED

**Reason:** NFR10/NFR11 explicitly forbid network calls and telemetry. Conflicts with F-Droid philosophy. Local file-based logger is a separate feature request.

### ~~#20 — OnboardingScreen pager state desync~~ FIXED

- [x] Made pager single source of truth: BackHandler and Next button animate pager directly
- [x] Removed ViewModel→pager sync `LaunchedEffect(uiState.currentPage)`
- [x] Kept pager→ViewModel sync via `snapshotFlow { settledPage }`

### ~~#21 — MediaCodecCompiler silently skips corrupt frames~~ FIXED

- [x] Added `skippedFrames` counter with per-frame warning log
- [x] Added `skippedFrames: Int` to `CompilationResult.Success`
- [x] ViewModel logs warning when compilation completes with skipped frames

### ~~#25 — No network_security_config.xml~~ FIXED

- [x] Created `app/src/main/res/xml/network_security_config.xml` with `cleartextTrafficPermitted="false"`
- [x] Referenced from `AndroidManifest.xml` via `android:networkSecurityConfig`

---

## LOW

### ~~#26 — NDK version not centralized~~ FIXED

- [x] Moved to `libs.versions.toml` as `ndk = "27.2.12479018"`
- [x] `core/cv/build.gradle.kts` uses `libs.versions.ndk.get()`

### ~~#27 — OpenCV path uses fragile 5-level relative path~~ FIXED

- [x] Gradle injects path via `cmake { arguments("-DOpenCV_DIR=...") }` using `project.rootDir`
- [x] CMakeLists.txt falls back to relative path if not provided

### ~~#30 — OpacityFab LaunchedEffect race~~ FIXED

- [x] Key on `isSliderVisible` only
- [x] Uses `snapshotFlow { opacityState.value }` + `debounce(3000)` for auto-collapse

### ~~#34 — No lint configuration~~ FIXED

- [x] Added `lint { abortOnError = true }` to both library and application blocks in root `build.gradle.kts`
- [x] App module gets `checkDependencies = true`

### ~~#35 — TracingViewModel has 11 constructor parameters~~ FIXED

- [x] Created `TimelapseOperations` holder class grouping 5 timelapse deps
- [x] Constructor reduced from 14 to 10 parameters
- [x] Updated DI module and test helper

### ~~#36 — Compose BOM dated 2024.12.01~~ FIXED

- [x] Updated to `2026.02.00` (Compose 1.10.3, Material3 1.4.0)
- [x] Fixed missing `material-icons-extended` dependency in onboarding module

### ~~#37 — Gradle JVM heap 2GB may be insufficient~~ FIXED

- [x] Increased to `-Xmx4096m` in `gradle.properties`

### ~~#38 — Global JNI references never deleted~~ FIXED

- [x] Added `JNI_OnUnload` that deletes all 5 global refs and nulls pointers

### ~~#39 — prevTrackingState stored as local Compose remember~~ FIXED

- [x] Added `previousTrackingState` to `TracingUiState`
- [x] ViewModel sets `previousTrackingState = it.trackingState` before updating
- [x] TracingContent reads from parameter instead of local `remember`

### ~~#40 — x86 ABI missing from native build~~ FIXED

- [x] Added `"x86"` to abiFilters in `core/cv/build.gradle.kts`

---

## TEST COVERAGE (Epic-level effort — requires separate sprint)

### #31 — Zero tests in app module

No tests for MainActivity, TraceGlassApp, Koin initialization, or navigation graph.

### #32 — No tests for CameraXManager, FlashlightController, OpenCvMarkerDetector, DataStoreSettingsRepository

Implementation classes for camera, CV, and persistence are entirely untested. These require either Robolectric or instrumented tests due to Android framework dependencies.

### #33 — Only 2 instrumented (androidTest) files in the entire project

ExpandableMenuTest and OpacityFabTest only. No TracingScreen, navigation, permission flow, or settings integration tests.

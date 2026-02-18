# Story 6.2: Interactive Camera Walkthrough

Status: in-progress

## Story

As a first-time user,
I want to be guided through my first marker detection on the live camera,
so that I know my setup works before I start tracing.

## Acceptance Criteria

1. **Given** the onboarding carousel is completed (not skipped) **When** camera permission is granted **Then** the interactive walkthrough begins on the live camera feed **And** an OnboardingOverlay composable guides the user step by step
2. **Given** the walkthrough is active **When** the user points the camera at their markers **Then** if markers are detected within 10 seconds, a success message is shown **And** if markers are NOT detected within 10 seconds, gentle guidance is displayed ("Try adjusting the angle or lighting")
3. **Given** the walkthrough marker detection succeeds **When** the user proceeds **Then** the Photo Picker opens for selecting the first reference image **And** once selected, the overlay appears with a tooltip: "Adjust opacity here" pointing to the OpacityFAB **And** the tooltip appears only once (never again)

## Tasks / Subtasks

- [x] Task 1: Create WalkthroughViewModel with timer-based detection flow (AC: #1, #2)
  - [x] 1.1 Create `WalkthroughState.kt` with `WalkthroughStep` enum (DETECTING_MARKERS, MARKERS_FOUND, PICK_IMAGE, SHOW_TOOLTIP, COMPLETED) and `WalkthroughUiState` data class
  - [x] 1.2 Create `WalkthroughViewModel` with `startDetection()` (timer loop incrementing `elapsedSeconds`), `onMarkersDetected()`, `onProceedToPickImage()`, `onImagePicked()`, `onTooltipDismissed()`
  - [x] 1.3 After 10 seconds without markers, set `showGuidance = true` to display hint overlay
  - [x] 1.4 Cancel timer job on `onMarkersDetected()` and in `onCleared()`
  - [x] 1.5 Write unit tests with `runVmTest` wrapper (cancels `viewModelScope` to prevent infinite hang from `while(true)` timer loop)
- [ ] Task 2: Create WalkthroughScreen composable (AC: #1, #2, #3)
  - [ ] 2.1 Create `WalkthroughScreen` (stateful) that overlays guidance on the live camera feed
  - [ ] 2.2 Implement `OnboardingOverlay` composable: semi-transparent scrim with spotlight cutout on target area, text + arrow pointing to zone
  - [ ] 2.3 DETECTING_MARKERS state: show "Point your camera at your markers" with camera feed active
  - [ ] 2.4 MARKERS_FOUND state: show success animation/message, "Continue" button
  - [ ] 2.5 PICK_IMAGE state: launch Photo Picker via existing `onPickImage` callback
  - [ ] 2.6 SHOW_TOOLTIP state: show tooltip arrow pointing at OpacityFAB ("Adjust opacity here"), dismiss on tap
  - [ ] 2.7 COMPLETED state: navigate to tracing screen
  - [ ] 2.8 Show guidance text ("Try adjusting the angle or lighting") when `showGuidance = true`
- [ ] Task 3: Integrate navigation, DI, and marker detection bridge (AC: #1, #2)
  - [ ] 3.1 Register `WalkthroughViewModel` in `OnboardingModule.kt` Koin DI
  - [ ] 3.2 Add `"walkthrough"` route to NavHost in `MainActivity.kt`
  - [ ] 3.3 Wire carousel completion (not skipped) → navigate to walkthrough route
  - [ ] 3.4 Bridge marker detection from `:core:cv` MarkerDetector into walkthrough (call `onMarkersDetected()` when markers found)
  - [ ] 3.5 Run full test suite to verify no regressions

## Dev Notes

### Architecture Patterns

- **Module:** `:feature:onboarding` — WalkthroughViewModel and WalkthroughScreen live here
- **Camera integration challenge:** The walkthrough needs the live camera feed from `:core:camera` and marker detection from `:core:cv`. The WalkthroughScreen must either: (a) embed the camera preview directly (like TracingScreen does), or (b) navigate to a variant of TracingScreen with walkthrough overlay. Option (a) is cleaner for module boundaries.
- **MVVM pattern:** WalkthroughScreen (stateful) → WalkthroughViewModel → WalkthroughUiState
- **Timer pattern:** `viewModelScope.launch { while(true) { delay(1000); elapsedSeconds++ } }` — MUST cancel in `onCleared()` and tests must use `runVmTest` wrapper that calls `viewModelScope.cancel()` before `runTest` exits (see MEMORY.md: viewModelScope + runTest infinite hang)
- **OnboardingOverlay component:** Semi-transparent scrim with spotlight cutout effect. States: Waiting (awaiting action), Success (action detected), Hint (help after timeout). Dismiss on tap or successful action.
- **Tooltip persistence:** "Adjust opacity here" tooltip shown once ever — persist `tooltipShown = true` to DataStore via OnboardingRepository (or add a new key)

### What's Already Built (Task 1 complete)

- `WalkthroughViewModel.kt` — full state machine with 5 steps, timer-based detection, guidance after 10s timeout
- `WalkthroughState.kt` — `WalkthroughStep` enum + `WalkthroughUiState` data class (step, elapsedSeconds, markersDetected, showGuidance)
- `WalkthroughViewModelTest.kt` — tests for timer increment, guidance timeout, marker detection transitions, full step flow; uses `runVmTest` pattern

### What's Missing (Tasks 2 & 3)

- **No WalkthroughScreen composable** — the UI that renders on the camera feed with overlay guidance
- **No NavHost route** — no `"walkthrough"` destination in `MainActivity.kt` navigation graph
- **Not in DI** — `WalkthroughViewModel` not registered in `OnboardingModule.kt`
- **No camera bridge** — no wiring between `:core:cv` marker detection callback and `WalkthroughViewModel.onMarkersDetected()`
- **No OnboardingOverlay component** — the spotlight/scrim composable for step-by-step guidance

### Existing Code to Reuse

- **Camera preview:** `TracingScreen.kt` / `TracingContent.kt` already renders CameraX preview — study the pattern but don't import directly (module boundary)
- **Marker detection callback:** `FrameAnalyzer.kt` in `:feature:tracing` calls `MarkerDetector.detect()` — the walkthrough needs a similar bridge or shared callback
- **Photo Picker:** `ImageFileHelper.kt` in `:feature:tracing` handles photo picker — consider whether to duplicate or extract to shared module
- **OpacityFab location:** Bottom-right of camera screen — tooltip must point there

### Testing Standards

- **JUnit 5** (Jupiter) with `@Nested` inner classes for test organization
- **`runVmTest` pattern** (CRITICAL): WalkthroughViewModel has `while(true)` timer loop — `runTest` hangs forever without `viewModelScope.cancel()` at end of block
- **StandardTestDispatcher** for coroutine tests
- **FakeOnboardingRepository** for isolation
- **No UI tests** for this story — ViewModel tests only (Compose UI testing for walkthrough would need instrumented tests)

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 6.2]
- [Source: _bmad-output/planning-artifacts/architecture.md — MVVM Pattern, Module Structure]
- [Source: _bmad-output/planning-artifacts/ux-design-specification.md — OnboardingOverlay Component, Interactive Walkthrough Flow]
- [Source: feature/onboarding/src/main/ — WalkthroughViewModel.kt, WalkthroughState.kt]
- [Source: feature/onboarding/src/test/ — WalkthroughViewModelTest.kt]

## Dev Agent Record

### Agent Model Used

Claude Opus 4 (claude-opus-4-20250514)

### Debug Log References

### Completion Notes List

- Task 1 complete: WalkthroughViewModel with 5-step state machine and 10s guidance timeout
- WalkthroughViewModelTest with runVmTest pattern (prevents infinite hang)
- Tasks 2-3 pending: WalkthroughScreen composable, NavHost route, DI registration, camera/marker bridge

### Change Log

- 2026-02-09: Task 1 implemented — WalkthroughViewModel + tests (commit b6498c5)

### File List

**Existing files (Task 1):**
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/WalkthroughViewModel.kt
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/WalkthroughState.kt
- feature/onboarding/src/test/kotlin/io/github/jn0v/traceglass/feature/onboarding/WalkthroughViewModelTest.kt

**Files to create (Tasks 2-3):**
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/WalkthroughScreen.kt
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/OnboardingOverlay.kt (spotlight/scrim component)

**Files to modify (Task 3):**
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/di/OnboardingModule.kt (register WalkthroughViewModel)
- app/src/main/kotlin/io/github/jn0v/traceglass/MainActivity.kt (add walkthrough route)

# Story 6.4: Onboarding Flow Control

Status: review

## Story

As a returning user,
I want to re-access the onboarding from settings,
so that I can review setup guides if needed.

## Acceptance Criteria

1. **Given** the user has completed onboarding
   **When** the user opens Settings and taps "Re-open onboarding" (FR22)
   **Then** the onboarding carousel is shown again from slide 1
   **And** completing or skipping it returns to the previous screen

2. **Given** the user has completed onboarding previously
   **When** the app launches
   **Then** onboarding is NOT shown — the camera feed appears directly

## Tasks / Subtasks

- [x] Task 1: Ensure re-opened onboarding returns to previous screen (AC: #1)
  - [x] 1.1 Add `onboardingMode` parameter to OnboardingScreen (FIRST_TIME vs REOPENED)
  - [x] 1.2 When REOPENED, onComplete/onSkip navigates back (popBackStack) instead of forward to tracing
  - [x] 1.3 Update NavHost: pass mode based on navigation source
  - [x] 1.4 Add OnboardingViewModel.onReopen() to reset page to 0 without clearing completion flag
- [x] Task 2: Add re-open onboarding entry point in tracing screen menu (AC: #1)
  - [x] 2.1 Add "Setup guide" button to tracing screen (temporary, until Settings screen in Story 7.1)
  - [x] 2.2 Navigate to onboarding with REOPENED mode via "onboarding-reopen" route
- [x] Task 3: Verify first-time launch behavior (AC: #2)
  - [x] 3.1 Write tests verifying OnboardingViewModel reopen behavior (6 tests in Reopen nested class)
  - [x] 3.2 Run full test suite to verify no regressions — all pass

## Dev Notes

### Architecture Requirements

- **Module:** `:feature:onboarding` for ViewModel changes, `:app` for navigation
- **Existing infrastructure:** `OnboardingRepository.resetOnboarding()` already exists but should NOT be called on reopen — completion flag stays true so next launch skips onboarding
- **Navigation pattern:** Two modes:
  - FIRST_TIME: onboarding → navigate("tracing") with popUpTo (current behavior)
  - REOPENED: tracing → onboarding → popBackStack() back to tracing
- **Settings screen:** Does not exist yet (Story 7.1). For now, add a temporary entry point from tracing screen.

### Existing Code Context

**Already implemented (AC #2):**
- `MainActivity.kt` checks `onboardingRepository.isOnboardingCompleted.first()` on launch
- If completed, starts at "tracing" destination — onboarding is NOT shown

**Files to modify:**
- `OnboardingScreen.kt` — accept mode parameter, change completion behavior
- `OnboardingViewModel.kt` — add onReopen() to reset page without clearing completion
- `MainActivity.kt` — pass mode to OnboardingScreen based on navigation source
- `TracingScreen.kt` — add temporary "Re-open onboarding" entry point

**Files to create:**
- None expected — changes to existing files only

### Testing Standards

- JUnit 5 with @Nested, StandardTestDispatcher
- Test OnboardingViewModel.onReopen() resets state correctly
- Verify onComplete does NOT re-persist completion (already completed)

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 6.4]
- [Source: feature/onboarding/src/main/ — Existing code]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

### Completion Notes List

- All 3 tasks completed with TDD approach (tests written first, then implementation)
- `OnboardingMode` enum added (FIRST_TIME, REOPENED) — placed in OnboardingUiState.kt alongside SetupTier
- `OnboardingViewModel.onReopen()` resets page to 0 and sets `isReopened` flag — onComplete/onSkip skip persistence when reopened
- `OnboardingScreen` accepts `mode` parameter; calls `viewModel.onReopen()` via `LaunchedEffect` when REOPENED
- Separate "onboarding-reopen" route in NavHost — uses `popBackStack()` on completion instead of navigating to "tracing"
- "Setup guide" `FilledTonalButton` added to TracingScreen (visible when no overlay image loaded, i.e. before a session)
- 6 new tests in `Reopen` nested class: page reset, no repository reset, no persistence on complete/skip, isCompleted still triggers, tier preserved
- Full test suite passes with zero regressions

### Change Log

- 2026-02-09: Story 6.4 implemented — onboarding flow control with FIRST_TIME/REOPENED modes

### File List

**Modified files:**
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/OnboardingUiState.kt (added OnboardingMode enum)
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/OnboardingViewModel.kt (added onReopen(), isReopened flag, conditional persistence)
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/OnboardingScreen.kt (added mode parameter, LaunchedEffect for onReopen)
- feature/onboarding/src/test/kotlin/io/github/jn0v/traceglass/feature/onboarding/OnboardingViewModelTest.kt (added Reopen nested class with 6 tests)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingScreen.kt (added onReopenOnboarding callback, "Setup guide" button)
- app/src/main/kotlin/io/github/jn0v/traceglass/MainActivity.kt (added "onboarding-reopen" route, passed callback to TracingScreen)


# Story 7.2: Break Reminders

Status: review

## Story

As a user,
I want to be reminded to take breaks during long drawing sessions,
So that I maintain healthy drawing habits.

## Acceptance Criteria

1. **Given** break reminders are enabled with a 30-minute interval (FR39)
   **When** 30 minutes of continuous tracing elapse
   **Then** a gentle, non-blocking snackbar appears: "Time for a break!"
   **And** a soft audio tone plays if audio feedback is enabled
   **And** the snackbar auto-dismisses after 10 seconds or on tap

2. **Given** the break reminder snackbar is dismissed
   **When** the user continues tracing
   **Then** the timer resets and the next reminder is in another 30 minutes

3. **Given** default app settings
   **When** the app is first installed
   **Then** break reminders are disabled (off by default, FR40)

4. **Given** break reminders are enabled
   **When** the user changes the interval in settings
   **Then** the timer resets with the new interval

## Tasks / Subtasks

- [x] Task 1: Add SettingsRepository to TracingViewModel (AC: all)
  - [x] 1.1 Inject `SettingsRepository` into TracingViewModel constructor (optional, default null)
  - [x] 1.2 Observe `settingsData` Flow to track break reminder enabled + interval + audio feedback
  - [x] 1.3 Update TracingModule Koin DI to pass SettingsRepository via `get()`
  - [x] 1.4 Existing TracingViewModel tests pass without changes (settingsRepository defaults to null)
- [x] Task 2: Implement break reminder timer logic (AC: #1, #2, #4)
  - [x] 2.1 7 tests written: default off, timer fires, disabled ignored, inactive ignored, dismiss resets, interval change resets, session stop cancels
  - [x] 2.2 Add `showBreakReminder: Boolean` and `audioFeedbackEnabled: Boolean` to TracingUiState
  - [x] 2.3 `restartBreakTimer()` launches `delay(interval * 60_000L)` coroutine in viewModelScope
  - [x] 2.4 `onBreakReminderDismissed()` resets flag and calls `restartBreakTimer()`
  - [x] 2.5 Timer cancelled and restarted on interval/enabled changes (via Flow observer) and session toggle
- [x] Task 3: Add break reminder snackbar to TracingScreen (AC: #1, #2)
  - [x] 3.1 Added `SnackbarHost` with `SnackbarHostState` in CameraPreviewContent Box
  - [x] 3.2 `LaunchedEffect(showBreakReminder)` shows snackbar "Time for a break!"
  - [x] 3.3 Snackbar auto-dismisses via M3 default SnackbarDuration (uses showSnackbar suspend)
  - [x] 3.4 On dismiss, calls `onBreakReminderDismissed()` to reset flag and restart timer
- [x] Task 4: Implement audio tone for break reminder (AC: #1)
  - [x] 4.1 `AudioFeedbackPlayer` utility plays system notification sound via `RingtoneManager.TYPE_NOTIFICATION`
  - [x] 4.2 Tone plays in `LaunchedEffect` when `showBreakReminder && audioFeedbackEnabled`
  - [x] 4.3 No vibration (audio only)
- [x] Task 5: Run full test suite (AC: all)
  - [x] 5.1 All unit tests pass across all modules (53 tracing + onboarding + settings)
  - [x] 5.2 Debug build succeeds

## Dev Notes

### Architecture Requirements

- **Module:** `:feature:tracing` for timer logic and UI
- **Dependencies:** `SettingsRepository` from `:core:session`
- **Timer pattern:** Coroutine-based timer in TracingViewModel using `delay()` in `viewModelScope`
- **No vibration:** Phone may be balanced on stand — audio tone only (if enabled)
- **Audio:** System notification sound via `RingtoneManager.getDefaultUri(TYPE_NOTIFICATION)` — no custom audio files

### Timer Design

```
Timer starts when: isSessionActive = true AND breakReminderEnabled = true
Timer stops when: isSessionActive = false OR breakReminderEnabled = false
Timer resets when: snackbar dismissed, interval changed, session toggled

Coroutine approach:
- Launch a Job that delays for intervalMinutes * 60_000L ms
- On completion, set showBreakReminder = true
- Cancel Job on any reset condition, relaunch if still applicable
```

### Key Implementation Detail: runVmTest

TracingViewModel will launch a `while(true)` or long-running coroutine for the break timer. Tests MUST use the `runVmTest` pattern (cancel viewModelScope in finally block) to prevent infinite hangs during `runTest` cleanup. See MEMORY.md for details.

### Existing Code Context

**TracingViewModel** currently:
- Has `isSessionActive` state tracked in `uiState`
- Uses `viewModelScope.launch` for session save
- Observes `flashlightController.isTorchOn` via `launchIn`

**TracingViewModel constructor** currently takes:
- `flashlightController: FlashlightController`
- `transformCalculator: OverlayTransformCalculator`
- `trackingStateManager: TrackingStateManager`
- `sessionRepository: SessionRepository?`

Need to add `settingsRepository: SettingsRepository` parameter.

**TracingUiState** needs `showBreakReminder: Boolean = false` added.

**TracingScreen** currently uses a raw `Box` layout — need to either wrap in Scaffold for SnackbarHost, or add a manual Snackbar composable.

### Testing Standards

- JUnit 5 with @Nested, StandardTestDispatcher
- `runVmTest` pattern for tests involving timer coroutines (CRITICAL: see MEMORY.md)
- FakeSettingsRepository for isolation
- Test: timer fires after exact interval (use advanceTimeBy)
- Test: timer does NOT fire when reminders disabled
- Test: timer does NOT fire when session inactive
- Test: timer resets on dismiss
- Test: timer resets on interval change
- Test: default off (FR40)

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 7.2]
- [Source: feature/tracing/src/main/ — Existing TracingViewModel, TracingScreen]
- [Source: core/session/ — SettingsRepository]
- [Source: MEMORY.md — runVmTest pattern for infinite coroutines]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

### Completion Notes List

- SettingsRepository injected as optional param (default null) — no breaking change for existing tests
- Break timer uses single-shot `delay()` coroutine (not while(true)), so no `runVmTest` needed
- Timer test for interval change uses `runCurrent()` instead of `advanceUntilIdle()` to avoid advancing past new timer
- AudioFeedbackPlayer is a minimal wrapper around RingtoneManager — no custom audio files needed
- Snackbar uses M3 `showSnackbar()` suspend function which handles auto-dismiss natively

### Change Log

- 2026-02-09: Story 7.2 implemented — break reminder timer, snackbar, audio feedback

### File List

**New files:**
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/AudioFeedbackPlayer.kt

**Modified files:**
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt (added SettingsRepository, break timer, onBreakReminderDismissed)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingUiState.kt (added showBreakReminder, audioFeedbackEnabled)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingScreen.kt (added SnackbarHost, LaunchedEffect for break reminder, audio tone)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/di/TracingModule.kt (pass settingsRepository to TracingViewModel)
- feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModelTest.kt (added BreakReminder nested class with 7 tests)


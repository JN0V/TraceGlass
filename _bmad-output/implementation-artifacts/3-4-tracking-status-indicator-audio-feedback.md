# Story 3.4: Tracking Status Indicator & Audio Feedback

Status: done

## Story

As a user,
I want to know if marker tracking is active at a glance,
So that I understand why the overlay might not be following my paper.

## Acceptance Criteria

1. **Tracking indicator — tracked:** Given the tracing screen is displayed and markers are actively tracked, then a green circle icon + "Tracking" label is shown in the top-right corner with semi-transparent background.

2. **Tracking indicator — lost:** Given the tracing screen is displayed and markers are lost, then an amber triangle icon + "Lost" label replaces the checkmark. Dual encoding: color + shape (never color alone) for colorblindness accessibility.

3. **Tracking indicator — inactive:** Given markers have never been detected in the session, the indicator is hidden (not shown).

4. **Audio cue — tracking lost:** Given audio feedback is enabled in settings, when tracking state changes from TRACKING to LOST, then a short DTMF tone plays (TONE_DTMF_0, 200ms).

5. **Audio cue — tracking recovered:** Given audio feedback is enabled, when tracking state changes from LOST to TRACKING, then a different short DTMF tone plays (TONE_DTMF_1, 120ms).

6. **Audio off by default (FR38):** Given default app settings, when the app is first installed, then audio feedback is disabled (off by default).

7. **Settings toggle:** Given the user opens Settings, the "Audio feedback" toggle is available with a Switch component and persists to DataStore.

## Tasks / Subtasks

- [x] Task 1: Create TrackingIndicator composable (AC: #1, #2, #3)
  - [x] 1.1 `TrackingIndicator.kt` in `feature/tracing/components/`
  - [x] 1.2 TRACKING state: green circle (Color 0xFF4CAF50) + "Tracking" text
  - [x] 1.3 LOST state: amber triangle (Color 0xFFFF9800) + "Lost" text
  - [x] 1.4 INACTIVE state: early return (component hidden)
  - [x] 1.5 Semi-transparent black background (alpha 0.5f) with rounded corners
  - [x] 1.6 Custom `TriangleShape` using `GenericShape` for warning triangle
  - [x] 1.7 Positioned top-right with `statusBarsPadding`
  - [x] 1.8 Content descriptions for TalkBack accessibility
- [x] Task 2: Create AudioFeedbackPlayer (AC: #4, #5)
  - [x] 2.1 `AudioFeedbackPlayer.kt` in `feature/tracing/`
  - [x] 2.2 `playTrackingLostTone()` — `ToneGenerator.TONE_DTMF_0`, 200ms duration
  - [x] 2.3 `playTrackingGainedTone()` — `ToneGenerator.TONE_DTMF_1`, 120ms duration
  - [x] 2.4 `playBreakReminderTone()` — system notification ringtone (for Story 7.2)
  - [x] 2.5 `ToneGenerator(AudioManager.STREAM_MUSIC, 80)` — plays on music stream at moderate volume
  - [x] 2.6 Single `ToneGenerator` reused across calls, explicit `release()` on composable dispose
- [x] Task 3: Wire audio feedback in TracingContent (AC: #4, #5, #6)
  - [x] 3.1 `LaunchedEffect(trackingState)` detects TRACKING→LOST and LOST→TRACKING transitions
  - [x] 3.2 Checks `audioFeedbackEnabled` before playing tones
  - [x] 3.3 Stores `prevTrackingState` via `remember { mutableStateOf() }` for transition detection
- [x] Task 4: Settings integration (AC: #6, #7)
  - [x] 4.1 `SettingsData.audioFeedbackEnabled = false` (default off)
  - [x] 4.2 `SettingsRepository.setAudioFeedbackEnabled()` persists to DataStore
  - [x] 4.3 `SettingsScreen` Switch component with "Audio feedback" label
  - [x] 4.4 `TracingViewModel` observes `settingsRepository.settingsData` and updates `uiState.audioFeedbackEnabled`
- [x] Task 5: TracingScreen layout integration (AC: #1, #2, #3)
  - [x] 5.1 Add `TrackingIndicator(trackingState)` to TracingScreen layout
  - [x] 5.2 Positioned in top-right corner via Box alignment
  - [x] 5.3 Visible regardless of control visibility toggle (always shown when TRACKING or LOST)

## Dev Notes

### Architecture

- **Module:** `:feature:tracing` — UI components and audio player
- **Module:** `:core:session` — SettingsData, SettingsRepository, DataStoreSettingsRepository
- Audio feedback is a UI concern (plays in response to state transitions in Compose)

### Dual Encoding Design (Colorblindness Accessibility)

| State | Color | Shape | Text |
|-------|-------|-------|------|
| TRACKING | Green (#4CAF50) | Circle (12dp) | "Tracking" |
| LOST | Amber (#FF9800) | Triangle (12dp) | "Lost" |
| INACTIVE | — | — | Hidden |

Both color AND shape change — users who cannot distinguish green from amber can still distinguish circle from triangle. Text labels provide a third encoding channel.

### Audio Feedback Details

- **DTMF tones** chosen over MediaPlayer for: instant playback, no file loading, small footprint
- **STREAM_MUSIC** at volume 80 — audible but not jarring, respects system volume
- **Reused instance:** Single `ToneGenerator` instance, reused across calls, released explicitly via `release()`
- **Defensive:** try-catch around ToneGenerator construction and `startTone()` for device compatibility

### State Transition → Audio Mapping

```
FrameAnalyzer → TracingViewModel.onMarkerResultReceived()
  → TrackingStateManager.onMarkerResult() → TrackingStatus
  → TracingUiState.trackingState (via StateFlow)
  → TracingContent LaunchedEffect(trackingState)
    → if (prevState==TRACKING && state==LOST) → playTrackingLostTone()
    → if (prevState==LOST && state==TRACKING) → playTrackingGainedTone()
```

### Settings Flow

```
SettingsScreen Switch → SettingsViewModel.onAudioFeedbackToggled()
  → SettingsRepository.setAudioFeedbackEnabled()
  → DataStore write (KEY_AUDIO_FEEDBACK)
  → Flow emission → TracingViewModel observes
  → TracingUiState.audioFeedbackEnabled updated
  → TracingContent checks flag before playing audio
```

### Project Structure Notes

- `TrackingIndicator.kt` in `components/` subdirectory (alongside OpacityFab, ExpandableMenu)
- `AudioFeedbackPlayer.kt` at feature root (not a component — it's a utility)
- Settings screen implemented in Story 7.1, but audio feedback toggle was prepared here
- Break reminder tone added to `AudioFeedbackPlayer` preemptively (used in Story 7.2)

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 3.4]
- [Source: feature/tracing/src/main/kotlin — TrackingIndicator, AudioFeedbackPlayer, TracingContent]
- [Source: feature/tracing/src/main/kotlin/settings — SettingsScreen, SettingsViewModel]
- [Source: core/session/src/main/kotlin — SettingsData, SettingsRepository, DataStoreSettingsRepository]
- [Source: feature/tracing/src/test/kotlin/settings — SettingsViewModelTest, FakeSettingsRepository]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

### Completion Notes List

- TrackingIndicator initially showed only checkmark/triangle without text labels — text added for better accessibility
- Audio feedback hooks were "prepared but disabled" in initial commit 3db6c9b — wired to DTMF tones in commit b95003f
- DTMF tones on STREAM_MUSIC was a fix from Feb 16 (originally used STREAM_NOTIFICATION which was too quiet)
- ToneGenerator release via Handler.postDelayed prevents resource leak
- Break reminder tone reuses AudioFeedbackPlayer (added preemptively, wired in Story 7.2)
- 64 total tests passing after this story (UI component tested via instrumented tests)
- Code review 2 (2026-02-18): 3 HIGH, 1 LOW findings fixed:
  - H1: AudioFeedbackPlayer refactored — reuses single ToneGenerator instance instead of creating per tone
  - H2: DisposableEffect added in TracingContent to call `audioPlayer.release()` on composable dispose
  - H3: `semantics(mergeDescendants = true)` with contentDescription added to TrackingIndicator for TalkBack
  - L1: try-catch around ToneGenerator construction and startTone for device compatibility
  - C3: 5 unit tests added for AudioFeedbackPlayer (defensive behavior on JVM without Android runtime)

### Change Log

- 2026-02-08: TrackingIndicator with dual encoding — commit 3db6c9b
- 2026-02-14: DTMF audio tones wired on STREAM_MUSIC — commit b95003f
- 2026-02-16: DTMF tone fix (delayed release) — commit a878ac5
- 2026-02-18: Code review 2 fixes — AudioFeedbackPlayer reuse/release/try-catch, DisposableEffect cleanup, TrackingIndicator semantics, unit tests

### File List

**New files:**
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/components/TrackingIndicator.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/AudioFeedbackPlayer.kt
- feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/AudioFeedbackPlayerTest.kt

**Modified files:**
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingScreen.kt (indicator placement)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingContent.kt (audio LaunchedEffect)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingViewModel.kt (settings observation)
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/SettingsData.kt (audioFeedbackEnabled)
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/SettingsRepository.kt (setAudioFeedbackEnabled)
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/DataStoreSettingsRepository.kt (DataStore key)

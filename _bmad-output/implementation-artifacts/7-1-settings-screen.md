# Story 7.1: Settings Screen

Status: done

## Story

As a user,
I want to access app settings,
So that I can customize my experience and find app information.

## Acceptance Criteria

1. **Given** the user is on the tracing screen
   **When** the user taps the settings icon
   **Then** the Settings screen opens with the following options:
     - Audio feedback toggle (FR38, off by default)
     - Break reminder toggle + interval selector (FR40, off by default, 30min default interval)
     - Re-open onboarding (FR22)
     - About / Licenses (placeholder for Story 7.3)
   **And** the screen uses M3 ListItem components
   **And** the system Back button returns to the camera screen

2. **Given** the user changes a setting (toggle or interval)
   **When** the value changes
   **Then** the new value is persisted immediately to DataStore
   **And** re-opening settings shows the persisted values

3. **Given** the user taps "Re-open onboarding"
   **When** the onboarding opens
   **Then** it uses REOPENED mode (from Story 6.4)
   **And** completing it returns to the settings screen

## Tasks / Subtasks

- [x] Task 1: Create SettingsRepository in `:core:session` (AC: #2)
  - [x] 1.1 Create `SettingsData` data class (audioFeedbackEnabled, breakReminderEnabled, breakReminderIntervalMinutes)
  - [x] 1.2 Create `SettingsRepository` interface (settingsData Flow, update methods)
  - [x] 1.3 Create `DataStoreSettingsRepository` implementation with preferencesDataStore
  - [x] 1.4 Register in `SessionModule.kt` Koin DI with separate `settings_prefs` DataStore
  - [x] 1.5 FakeSettingsRepository created for ViewModel test isolation
- [x] Task 2: Create SettingsViewModel and UiState (AC: #1, #2)
  - [x] 2.1 Create `SettingsUiState` data class
  - [x] 2.2 Create `SettingsViewModel` with toggle/update methods, loads from repository via Flow on init
  - [x] 2.3 Write unit tests for SettingsViewModel (8 tests: initial state, toggle states, persistence calls)
- [x] Task 3: Create SettingsScreen composable (AC: #1)
  - [x] 3.1 Create `SettingsScreen` (stateful, koinViewModel) with Scaffold + TopAppBar + back navigation
  - [x] 3.2 Create `SettingsContent` (stateless) with M3 ListItem components
  - [x] 3.3 Audio feedback: ListItem with Switch + contentDescription
  - [x] 3.4 Break reminder: ListItem with Switch + AnimatedVisibility interval Slider (5-60min, step 5)
  - [x] 3.5 Re-open onboarding: clickable ListItem
  - [x] 3.6 About: clickable ListItem (placeholder for Story 7.3)
- [x] Task 4: Integrate navigation and replace temporary button (AC: #1, #3)
  - [x] 4.1 Add `"settings"` route to NavHost in `MainActivity.kt`
  - [x] 4.2 Replace "Setup guide" FilledTonalButton with Settings FAB icon on TracingScreen
  - [x] 4.3 Wire "Re-open onboarding" from settings → onboarding-reopen route → popBackStack to settings
  - [x] 4.4 Register SettingsViewModel in TracingModule Koin DI
- [x] Task 5: Run full test suite to verify no regressions (AC: all)
  - [x] 5.1 All unit tests pass across all modules
  - [x] 5.2 Debug build succeeds

## Dev Notes

### Architecture Requirements

- **Repository:** `:core:session` module — `SettingsRepository` interface + `DataStoreSettingsRepository`
- **ViewModel + Screen:** `:feature:tracing` module (settings are tightly coupled with tracing behavior)
- **MVVM pattern (mandatory):** Screen (stateful) → Content (stateless) → ViewModel → UiState
- **DI:** Register SettingsRepository in `SessionModule.kt`, SettingsViewModel in `TracingModule.kt`
- **DataStore:** New preferencesDataStore named `"settings_prefs"` (separate from session/onboarding stores)
- **No vibration:** Phone may be balanced on stand — visual feedback only

### Settings Data to Persist

```kotlin
data class SettingsData(
    val audioFeedbackEnabled: Boolean = false,   // FR38, off by default
    val breakReminderEnabled: Boolean = false,    // FR40, off by default
    val breakReminderIntervalMinutes: Int = 30    // FR39, 30min default
)
```

### UI Components (Material 3)

| Setting | M3 Component | Behavior |
|---------|-------------|----------|
| Audio feedback | ListItem + Switch | Toggle persists immediately |
| Break reminder | ListItem + Switch | Toggle persists, shows interval when enabled |
| Break interval | Slider (5-60min, step 5) | Value persists on change |
| Re-open onboarding | ListItem (clickable) | Navigates to onboarding-reopen |
| About | ListItem (clickable) | Placeholder (Story 7.3) |

### Navigation Flow

```
TracingScreen → [Settings icon] → SettingsScreen
SettingsScreen → [Back] → TracingScreen (popBackStack)
SettingsScreen → [Re-open onboarding] → OnboardingScreen(REOPENED) → [Complete] → SettingsScreen
```

### Existing Code Context

**Replace temporary button (Story 6.4):**
- `TracingScreen.kt` currently has a `FilledTonalButton("Setup guide")` that navigates to `onboarding-reopen`
- Replace with a settings gear icon (`Icons.Filled.Settings`) that navigates to `"settings"`
- The "Re-open onboarding" functionality moves into the Settings screen

**DataStore patterns to follow:**
- `DataStoreSessionRepository.kt` — companion object with PreferencesKey, Flow mapping, edit blocks
- `DataStoreOnboardingRepository.kt` — simpler pattern for boolean flags

**ViewModel pattern to follow:**
- `OnboardingViewModel.kt` — MutableStateFlow + update pattern
- `SetupGuideViewModel.kt` — simple state management

**Files to modify:**
- `core/session/src/main/kotlin/.../di/SessionModule.kt` — register SettingsRepository
- `feature/tracing/src/main/kotlin/.../TracingScreen.kt` — replace "Setup guide" button with Settings icon
- `feature/tracing/src/main/kotlin/.../di/TracingModule.kt` — register SettingsViewModel
- `app/src/main/kotlin/.../MainActivity.kt` — add "settings" route, wire navigation callbacks
- `app/src/main/kotlin/.../TraceGlassApp.kt` — no change needed (sessionModule already registered)

**Files to create:**
- `core/session/src/main/kotlin/.../SettingsData.kt`
- `core/session/src/main/kotlin/.../SettingsRepository.kt`
- `core/session/src/main/kotlin/.../DataStoreSettingsRepository.kt`
- `feature/tracing/src/main/kotlin/.../settings/SettingsUiState.kt`
- `feature/tracing/src/main/kotlin/.../settings/SettingsViewModel.kt`
- `feature/tracing/src/main/kotlin/.../settings/SettingsScreen.kt`
- `feature/tracing/src/test/kotlin/.../settings/SettingsViewModelTest.kt`
- `feature/tracing/src/test/kotlin/.../settings/FakeSettingsRepository.kt`

### Testing Standards

- JUnit 5 with @Nested, StandardTestDispatcher
- FakeSettingsRepository for ViewModel isolation
- Test: initial state loads from repository defaults
- Test: toggle audio feedback updates state and persists
- Test: toggle break reminder updates state and persists
- Test: change interval updates state and persists
- Test: interval only visible when break reminder enabled

### Accessibility

- All switches must have contentDescription
- Touch targets >= 48dp
- Text uses sp (respects system font scaling)
- Sufficient color contrast (WCAG AA)

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 7.1]
- [Source: _bmad-output/planning-artifacts/architecture.md — Module Structure, DataStore Pattern]
- [Source: _bmad-output/planning-artifacts/ux-design-specification.md — Settings Screen, M3 Components]
- [Source: core/session/src/main/ — DataStore patterns]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

### Completion Notes List

- All 5 tasks completed with TDD approach (tests RED → implementation GREEN → verify)
- Repository layer in `:core:session` with separate `settings_prefs` DataStore (not mixed with session data)
- SettingsViewModel uses `Flow.launchIn(viewModelScope)` to observe repository changes reactively
- SettingsScreen follows existing MVVM pattern: stateful Screen (koinViewModel) → stateless Content
- Break reminder interval uses AnimatedVisibility + Slider (5-60min, step 5) — only visible when break reminders enabled
- Settings FAB (gear icon) replaces temporary "Setup guide" button from Story 6.4 — always visible on tracing screen
- Navigation: tracing → settings → (re-open onboarding) → popBackStack chain works correctly
- About ListItem is a placeholder — will be implemented in Story 7.3
- 8 SettingsViewModel tests in 4 nested classes: InitialState, AudioFeedback, BreakReminder
- Full test suite passes with zero regressions

### Change Log

- 2026-02-09: Story 7.1 implemented — settings screen with persistence, navigation integration
- 2026-02-19: Adversarial review fixes — a11y (toggleable rows, contentDescription), i18n (stringResource), slider debounce, interval validation, DataStore error handling, non-default state tests

### File List

**New files:**
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/SettingsData.kt
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/SettingsRepository.kt
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/DataStoreSettingsRepository.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/settings/SettingsUiState.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/settings/SettingsViewModel.kt
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/settings/SettingsScreen.kt
- feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/settings/FakeSettingsRepository.kt
- feature/tracing/src/test/kotlin/io/github/jn0v/traceglass/feature/tracing/settings/SettingsViewModelTest.kt

**Modified files:**
- core/session/src/main/kotlin/io/github/jn0v/traceglass/core/session/di/SessionModule.kt (added SettingsRepository + settings_prefs DataStore)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/di/TracingModule.kt (registered SettingsViewModel)
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingScreen.kt (replaced Setup guide button with Settings FAB, renamed callback)
- app/src/main/kotlin/io/github/jn0v/traceglass/MainActivity.kt (added settings route, wired navigation)


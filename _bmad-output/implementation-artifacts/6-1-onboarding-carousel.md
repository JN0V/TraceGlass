# Story 6.1: Onboarding Carousel

Status: done

## Story

As a first-time user,
I want a clear visual introduction to TraceGlass,
so that I understand what the app does and how to set up.

## Acceptance Criteria

1. **Given** the app is launched for the first time **When** the onboarding screen appears **Then** a 3-slide carousel is displayed using HorizontalPager: Slide 1 (Welcome + concept), Slide 2 (Choose setup tier: Full DIY / Semi-equipped / Full kit — FR23), Slide 3 (Prepare your markers based on chosen tier) **And** each slide has a "Next" button and a page indicator **And** swipe navigation works between slides
2. **Given** the user is on any onboarding slide **When** the user taps "Skip" (FR21) **Then** the onboarding is dismissed and the camera screen is shown **And** the onboarding is marked as completed in DataStore

## Tasks / Subtasks

- [x] Task 1: Create OnboardingRepository persistence layer (AC: #2)
  - [x] 1.1 Create `OnboardingRepository` interface with `isOnboardingCompleted: Flow<Boolean>`, `setOnboardingCompleted()`, `resetOnboarding()`
  - [x] 1.2 Create `DataStoreOnboardingRepository` implementation using Jetpack DataStore Preferences
  - [x] 1.3 Create `FakeOnboardingRepository` for unit tests
- [x] Task 2: Create OnboardingViewModel with state management (AC: #1, #2)
  - [x] 2.1 Create `OnboardingUiState` data class (`currentPage`, `selectedTier`, `isCompleted`) and `SetupTier` enum (FULL_DIY, SEMI_EQUIPPED, FULL_KIT)
  - [x] 2.2 Create `OnboardingViewModel` with page navigation (`onPageChanged`, `onNextPage`), tier selection (`onTierSelected`), and completion (`onComplete`, `onSkip`)
  - [x] 2.3 Write unit tests: initial state, page navigation bounds, tier selection, completion persists to repository
- [x] Task 3: Create OnboardingScreen carousel composable (AC: #1)
  - [x] 3.1 Create `OnboardingScreen` (stateful) with `HorizontalPager` (3 pages), page indicator dots, "Skip" button (top-right), "Next"/"Get Started" button (bottom)
  - [x] 3.2 Sync pager state with ViewModel via `LaunchedEffect`
  - [x] 3.3 Create `OnboardingPages.kt` with `WelcomePage`, `TierSelectionPage` (FilterChip for 3 tiers), `MarkerPreparationPage` (tier-specific instructions)
- [x] Task 4: Integrate navigation and DI (AC: #1, #2)
  - [x] 4.1 Register `OnboardingViewModel` and `OnboardingRepository` in `OnboardingModule.kt` Koin DI
  - [x] 4.2 Add `"onboarding"` route to NavHost in `MainActivity.kt`
  - [x] 4.3 Check `isOnboardingCompleted` on launch — if true, skip to "tracing" route
  - [x] 4.4 Run full test suite to verify no regressions

## Dev Notes

### Architecture Patterns

- **Module:** `:feature:onboarding` — depends only on `:core:session`
- **MVVM pattern:** OnboardingScreen (stateful, koinViewModel) → OnboardingPages (stateless content) → OnboardingViewModel → OnboardingUiState
- **State management:** Single `MutableStateFlow<OnboardingUiState>` exposed as `StateFlow`, collected via `collectAsStateWithLifecycle()`
- **HorizontalPager:** Page state synced bidirectionally with ViewModel via `LaunchedEffect` using `settledPage` (not `currentPage`) to avoid animation feedback loops
- **DataStore persistence:** `preferencesDataStore(name = "onboarding_prefs")` with keys: `KEY_COMPLETED` (Boolean), `KEY_TOOLTIP_SHOWN` (Boolean), `KEY_SELECTED_TIER` (String)
- **i18n:** All user-facing strings in `res/values/strings.xml`
- **Accessibility:** `contentDescription` on page indicators, `selectableGroup` on tier chips
- **Back navigation:** `BackHandler` navigates to previous slide; system back on page 0 exits onboarding
- **Tier selection:** `FilterChip` (Material 3) for mutually exclusive choice between 3 setup tiers
- **Skip behavior:** Both "Skip" and "Get Started" (on last page) call `onComplete()`/`onSkip()` which persist completion flag and trigger navigation callback

### Project Structure Notes

- All onboarding code in `feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/`
- DI module at `di/OnboardingModule.kt`
- No conflicts with other modules — `:feature:onboarding` is fully independent

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 6.1]
- [Source: _bmad-output/planning-artifacts/architecture.md — MVVM Pattern, Module Structure]
- [Source: _bmad-output/planning-artifacts/ux-design-specification.md — Hybrid Onboarding, Carousel Design]

## Dev Agent Record

### Agent Model Used

Claude Opus 4 (claude-opus-4-20250514)

### Debug Log References

### Completion Notes List

- 3-slide HorizontalPager carousel with swipe + button navigation
- SetupTier enum (FULL_DIY, SEMI_EQUIPPED, FULL_KIT) with FilterChip selection on slide 2
- Tier-specific marker preparation instructions on slide 3
- OnboardingRepository with DataStore persistence (isOnboardingCompleted, isTooltipShown, selectedTier flows)
- FakeOnboardingRepository for unit tests (with call counters for all methods)
- OnboardingViewModel tests: initial state, navigation bounds, tier selection + persistence, completion persistence, reopen behavior
- MainActivity checks completion flag on launch — skips onboarding if already completed; shows loading spinner during DataStore init
- BackHandler for system back navigation within carousel
- All strings extracted to strings.xml for i18n
- Accessibility: semantics on page indicators and tier chips
- @Preview annotations on WelcomePage, TierSelectionPage, MarkerPreparationPage

### Change Log

- 2026-02-09: Story 6.1 implemented — onboarding carousel with 3-slide HorizontalPager
- 2026-02-19: Adversarial review fixes — 14 findings addressed:
  - Extracted all hardcoded strings to `strings.xml` for i18n readiness
  - Added accessibility: `contentDescription` on page indicator dots, `semantics { selectableGroup() }` on tier chips, increased dot size to 12dp
  - Removed unused `AnimatedVisibility` import
  - Extracted `PAGE_COUNT` constant, replaced all magic number `3` / `2` references
  - Added bounds validation to `onPageChanged()` via `coerceIn(0, PAGE_COUNT - 1)`
  - Fixed pager bidirectional sync: use `settledPage` instead of `currentPage` to prevent animation feedback loop
  - Added `BackHandler` for system back navigation (goes to previous slide when not on page 0)
  - Added `onPreviousPage()` to ViewModel
  - Added tier persistence: `selectedTier` stored in DataStore, restored on init
  - Fixed `resetOnboarding()` to also clear `tooltip_shown` flag and `selected_tier`
  - Marked `isReopened` as `@Volatile` for thread-safety
  - Added `CircularProgressIndicator` during DataStore loading in `MainActivity`
  - Added `@Preview` annotations to `WelcomePage`, `TierSelectionPage`, `MarkerPreparationPage`
  - Added tests: bounds clamping, `onPreviousPage`, tier persistence on init, tier persistence on selection

### File List

**New files:**
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/OnboardingScreen.kt
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/OnboardingViewModel.kt
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/OnboardingUiState.kt
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/OnboardingPages.kt
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/OnboardingRepository.kt
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/DataStoreOnboardingRepository.kt
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/di/OnboardingModule.kt
- feature/onboarding/src/main/res/values/strings.xml
- feature/onboarding/src/test/kotlin/io/github/jn0v/traceglass/feature/onboarding/OnboardingViewModelTest.kt
- feature/onboarding/src/test/kotlin/io/github/jn0v/traceglass/feature/onboarding/FakeOnboardingRepository.kt

**Modified files:**
- app/src/main/kotlin/io/github/jn0v/traceglass/MainActivity.kt (added onboarding route + completion check + loading indicator)
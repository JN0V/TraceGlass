# Story 6.3: Setup Guides & External Links

Status: review

## Story

As a user,
I want to access guides for drawing markers and setting up my tracing station,
so that I can prepare everything I need to start tracing.

## Acceptance Criteria

1. **Given** the user is in the onboarding flow or navigates to guides from settings
   **When** the user accesses the DIY marker drawing guide (FR24)
   **Then** a screen shows visual instructions for drawing each marker shape (heart, star, cross, circle, diamond)
   **And** the content uses vector drawables (not raster images)

2. **Given** the user accesses the MacGyver setup guide (FR25)
   **When** the guide is displayed
   **Then** it shows step-by-step instructions for improvised phone stands (glass of water, books, etc.)

3. **Given** the user accesses external download links
   **When** they tap "Download printable marker sheet" (FR26) or "Download 3D stand model" (FR27)
   **Then** the system browser opens with the respective URL
   **And** no network permission is added to the app (links open in external browser)

## Tasks / Subtasks

- [x] Task 1: Create vector drawable marker shape illustrations (AC: #1)
  - [x] 1.1 Create `res/drawable/` directory in `:feature:onboarding`
  - [x] 1.2 Create `ic_marker_heart.xml` vector drawable
  - [x] 1.3 Create `ic_marker_star.xml` vector drawable
  - [x] 1.4 Create `ic_marker_cross.xml` vector drawable
  - [x] 1.5 Create `ic_marker_circle.xml` vector drawable
  - [x] 1.6 Create `ic_marker_diamond.xml` vector drawable
- [x] Task 2: Create SetupGuideScreen composable and ViewModel (AC: #1, #2, #3)
  - [x] 2.1 Create `SetupGuideUiState` data class and `SetupGuideSection` enum (MARKER_GUIDE, MACGYVER_GUIDE)
  - [x] 2.2 Create `SetupGuideViewModel` with section navigation logic
  - [x] 2.3 Write unit tests for `SetupGuideViewModel` (section selection, external link actions)
  - [x] 2.4 Create `SetupGuideScreen` composable (stateful, koinViewModel)
  - [x] 2.5 Create `MarkerGuideContent` composable showing 5 marker shapes with step-by-step drawing instructions
  - [x] 2.6 Create `MacGyverGuideContent` composable showing improvised stand instructions (glass, books, box)
  - [x] 2.7 Create `ExternalLinksSection` composable with "Download marker sheet" and "Download 3D stand" buttons using `LocalUriHandler`
- [x] Task 3: Integrate navigation and access points (AC: #1, #2, #3)
  - [x] 3.1 Add `"setup-guide"` route to NavHost in `MainActivity.kt`
  - [x] 3.2 Add navigation to guide from `MarkerPreparationPage` (onboarding slide 3) via "View detailed guide" button
  - [x] 3.3 Register `SetupGuideViewModel` in `OnboardingModule.kt` Koin DI
  - [x] 3.4 Run full test suite to verify no regressions

## Dev Notes

### Architecture Requirements

- **Module:** `:feature:onboarding` (all guide code goes here)
- **Module boundaries:** Only depends on `:core:session` — NO other module dependencies
- **MVVM pattern (mandatory):** Screen (stateful) → Content (stateless) → ViewModel → UiState
- **DI:** Register ViewModel and any new dependencies in `OnboardingModule.kt` via Koin
- **No INTERNET permission:** External links use `LocalUriHandler.openUri()` which delegates to system browser via Intent
- **No vibration:** Phone is balanced on stand — all feedback must be visual or audio only
- **File size limit:** 200-800 lines per file

### UI & Design Patterns

- **Material 3** with dynamic color (non-camera screens follow system theme)
- **Typography:** Title 24sp Bold, Subtitle 18sp Medium, Body 16sp Regular, Caption 12sp Regular
- **Spacing:** 24dp screen margins, 16dp section spacing, 8dp paragraph spacing
- **Touch targets:** Minimum 48dp × 48dp for all interactive elements
- **Accessibility:** All vector illustrations MUST have `contentDescription` for TalkBack; external link buttons should indicate "opens in browser"
- **Orientation:** Portrait primary, must not crash in landscape

### Marker Shapes (Decorative Fiducials)

The 5 marker shapes are designed to be **fun to draw** (vs ugly QR/ArUco markers):
1. **Heart** — easy, fun, recognizable
2. **Star** (5-pointed) — distinct outline
3. **Cross** (+) — simple geometry
4. **Circle** — basic but clean
5. **Diamond** (◇) — unique, simple

Vector drawables should be simple outlines (~24dp viewport) showing the shape the user needs to draw. Include sizing guidance in text: markers should be ~3cm, spaced 10-15cm apart.

### MacGyver Setup Guide Content

Three improvised stand options:
1. **Glass of water** — phone leans against glass over paper (best stability)
2. **Stack of books** — phone propped between books, angled toward surface
3. **Box/container** — phone balanced on box edge looking down

Each option: short title + 2-3 sentence instruction + visual tip (text-based is fine for MVP, no illustrations needed for stands).

### External Links

Placeholder URLs (to be updated when project repo is set up):
- Marker sheet PDF: `https://github.com/jn0v/traceglass/releases/latest/download/markers.pdf`
- 3D stand STL: `https://github.com/jn0v/traceglass/releases/latest/download/phone-stand.stl`

### Existing Code Context

**Files to modify:**
- `MainActivity.kt` — add `"setup-guide"` route to NavHost
- `OnboardingPages.kt` — add "View guide" button on MarkerPreparationPage (slide 3)
- `OnboardingModule.kt` — register SetupGuideViewModel

**Files to create:**
- `SetupGuideScreen.kt` — stateful Screen + stateless Content composables
- `SetupGuideViewModel.kt` — section navigation state
- `SetupGuideUiState.kt` — state data class + SetupGuideSection enum
- `SetupGuideViewModelTest.kt` — unit tests
- `res/drawable/ic_marker_*.xml` — 5 vector drawables

**Existing patterns to follow:**
- `OnboardingScreen.kt` → Screen composable pattern (koinViewModel, collectAsStateWithLifecycle)
- `OnboardingPages.kt` → Content composable pattern (stateless, receives state + callbacks)
- `OnboardingViewModel.kt` → ViewModel pattern (MutableStateFlow, update, viewModelScope)
- `OnboardingViewModelTest.kt` → Test pattern (StandardTestDispatcher, @Nested, FakeRepository)

### Testing Standards

- **JUnit 5** (Jupiter) with `@Nested` inner classes for test organization
- **StandardTestDispatcher** for coroutine tests
- **Fake repositories** for isolation (no MockK for simple cases)
- **Test coverage:** ViewModel state transitions, section navigation, initial state defaults
- **No UI tests** (Compose instrumented tests) for this story — ViewModel tests only
- **runVmTest pattern** if ViewModel has infinite coroutines (not expected for this story)

### Previous Story Learnings (6.1, 6.2)

- `HorizontalPager` requires `@OptIn(ExperimentalFoundationApi::class)`
- `LaunchedEffect` for syncing pager state with ViewModel
- `FilterChip` for selection UI (Material 3)
- DataStore persistence with suspend functions
- `viewModelScope + while(true)` causes `runTest` infinite hang — use `runVmTest` wrapper (documented in MEMORY.md)
- Always cancel viewModelScope in tests before runTest cleanup

### Git Intelligence

Recent commit pattern: `feat: <description> (Story X.Y)`
- `f0a5bdb feat: walkthrough ViewModel with marker detection timer (Story 6.2)`
- `31899cd feat: onboarding carousel with 3-slide HorizontalPager (Story 6.1)`
- `fb7be68 feat: explicit session pause/resume with auto-persistence (Story 5.3)`

### Project Structure Notes

- Alignment: All new files go in `feature/onboarding` package, consistent with existing structure
- No conflicts detected with existing modules
- Vector drawables will be the first `res/` content in this module — need to create `src/main/res/drawable/` directory

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic 6 Story 6.3]
- [Source: _bmad-output/planning-artifacts/architecture.md — Module Structure, MVVM Pattern, Testing Standards]
- [Source: _bmad-output/planning-artifacts/ux-design-specification.md — Onboarding Design, Accessibility, Typography]
- [Source: feature/onboarding/src/main/ — Existing code patterns]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

### Completion Notes List

- All 3 tasks completed with TDD approach (tests written before ViewModel implementation)
- 5 vector drawable marker shapes created (heart, star, cross, circle, diamond) as outline-only SVG paths
- SetupGuideViewModel with section navigation (MARKER_GUIDE / MACGYVER_GUIDE) — 3 unit tests pass
- SetupGuideScreen with Scaffold, TopAppBar, FilterChip section selector, scrollable content
- MarkerGuideContent shows 5 shapes in Cards with drawing instructions and vector illustrations
- MacGyverGuideContent shows 3 improvised stand options (glass, books, box) in Cards
- ExternalLinksSection uses LocalUriHandler.openUri() — no INTERNET permission needed
- Navigation: "setup-guide" route added to NavHost, "View detailed setup guide" button on MarkerPreparationPage (slide 3)
- SetupGuideViewModel registered in OnboardingModule Koin DI
- Full test suite passes (all modules, zero regressions)
- Build issue: Icons.Default.Link/OpenInNew not available without material-icons-extended — resolved by removing icon, text "(opens browser)" provides sufficient clarity

### Change Log

- 2026-02-09: Story 6.3 implemented — setup guides, external links, navigation integration

### File List

**New files:**
- feature/onboarding/src/main/res/drawable/ic_marker_heart.xml
- feature/onboarding/src/main/res/drawable/ic_marker_star.xml
- feature/onboarding/src/main/res/drawable/ic_marker_cross.xml
- feature/onboarding/src/main/res/drawable/ic_marker_circle.xml
- feature/onboarding/src/main/res/drawable/ic_marker_diamond.xml
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/SetupGuideUiState.kt
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/SetupGuideViewModel.kt
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/SetupGuideScreen.kt
- feature/onboarding/src/test/kotlin/io/github/jn0v/traceglass/feature/onboarding/SetupGuideViewModelTest.kt

**Modified files:**
- app/src/main/kotlin/io/github/jn0v/traceglass/MainActivity.kt (added setup-guide route + onNavigateToGuide callback)
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/OnboardingScreen.kt (added onNavigateToGuide parameter)
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/OnboardingPages.kt (added onViewGuide button to MarkerPreparationPage)
- feature/onboarding/src/main/kotlin/io/github/jn0v/traceglass/feature/onboarding/di/OnboardingModule.kt (registered SetupGuideViewModel)


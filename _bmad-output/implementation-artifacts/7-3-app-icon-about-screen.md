# Story 7.3: App Icon & About Screen

Status: review

## Story

As a user,
I want a recognizable app icon and access to app information,
So that I can identify TraceGlass on my home screen and know its version and licenses.

## Acceptance Criteria

1. **Given** the app is installed
   **When** the user views their app drawer or home screen
   **Then** the TraceGlass adaptive icon is displayed (glass lens + pencil trace, teal palette)
   **And** the icon is readable at 48dp, 72dp, 96dp, and 108dp sizes
   **And** the icon has separate foreground and background layers (Android adaptive icon format)

2. **Given** the user opens Settings → About
   **When** the About screen is displayed
   **Then** the app name, version, and build number are shown
   **And** open-source license information is accessible
   **And** a link to the project repository is provided

## Tasks / Subtasks

- [x] Task 1: Create adaptive app icon (AC: #1)
  - [x] 1.1 Create `ic_launcher_foreground.xml` — glass lens circle + handle + wavy pencil trace + glare, teal (#00796B/#004D40)
  - [x] 1.2 Create `ic_launcher_background.xml` — solid light teal (#B2DFDB)
  - [x] 1.3 Create `ic_launcher.xml` and `ic_launcher_round.xml` adaptive-icon in `mipmap-anydpi-v26/`
  - [x] 1.4 Add `android:icon` and `android:roundIcon` to AndroidManifest.xml
- [x] Task 2: Create AboutScreen composable (AC: #2)
  - [x] 2.1 Create `AboutScreen` with Scaffold + TopAppBar + back navigation (no ViewModel — static content)
  - [x] 2.2 Display "TraceGlass", version from BuildConfig.VERSION_NAME, build from BuildConfig.VERSION_CODE
  - [x] 2.3 Display GPL-3.0 license + third-party licenses (OpenCV, Coil, Koin, Jetpack — all Apache 2.0)
  - [x] 2.4 "View on GitHub" OutlinedButton using LocalUriHandler
- [x] Task 3: Wire navigation (AC: #2)
  - [x] 3.1 Add `"about"` route to NavHost passing BuildConfig version info
  - [x] 3.2 Wire SettingsScreen `onAbout` callback to `navController.navigate("about")`
- [x] Task 4: Build and verify (AC: all)
  - [x] 4.1 Build succeeds (including BuildConfig generation)
  - [x] 4.2 Full test suite passes, zero regressions

## Dev Notes

### Architecture Requirements

- **Icon:** Adaptive icon format (foreground + background layers) in `app/src/main/res/`
- **About screen:** In `:feature:tracing` module (accessed from settings, same module)
- **No ViewModel needed** for About screen — static content only
- **Version info:** Read from `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE`

### Icon Design

- **Concept:** Glass lens (circle/magnifying glass) + pencil trace line
- **Colors:** Primary teal (#009688) foreground on lighter teal (#B2DFDB) background
- **Style:** Flat material, no photorealism
- **Format:** Vector drawables, 108dp viewport (adaptive icon standard)
- **Safe zone:** Key content within 66dp inner circle (for circular mask)

### About Screen Content

- App name: "TraceGlass"
- Version: from BuildConfig.VERSION_NAME ("0.1.0")
- Build: from BuildConfig.VERSION_CODE (1)
- License: GPL-3.0
- Key dependencies: OpenCV (Apache 2.0), Coil (Apache 2.0), Koin (Apache 2.0), Jetpack (Apache 2.0)
- Repository: https://github.com/jn0v/traceglass

### Existing Code Context

**AndroidManifest.xml** currently missing `android:icon` attribute.
**SettingsScreen.kt** already has `onAbout: () -> Unit = {}` placeholder.
**MainActivity.kt** needs new `"about"` route.

### Files to create
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `feature/tracing/src/main/kotlin/.../settings/AboutScreen.kt`

### Files to modify
- `app/src/main/AndroidManifest.xml` (add icon attributes)
- `app/src/main/kotlin/.../MainActivity.kt` (add about route)
- `app/build.gradle.kts` (enable BuildConfig generation if needed)

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 7.3]
- [Source: _bmad-output/planning-artifacts/ux-design-specification.md — Icon Design]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6

### Debug Log References

### Completion Notes List

- Adaptive icon uses vector drawables only (no raster PNGs) — works on all densities via anydpi-v26
- Icon concept: glass lens (circle + handle) with wavy pencil trace line through it, lens glare accent
- Background: solid light teal #B2DFDB, foreground elements: dark teal #00796B / #004D40
- AboutScreen is purely static — no ViewModel, no repository, receives version info as parameters
- Enabled `buildConfig = true` in app/build.gradle.kts (required since AGP 8+ for BuildConfig generation)
- GitHub link opens in external browser via LocalUriHandler — no INTERNET permission needed

### Change Log

- 2026-02-09: Story 7.3 implemented — adaptive app icon, about screen with version/licenses

### File List

**New files:**
- app/src/main/res/drawable/ic_launcher_foreground.xml
- app/src/main/res/drawable/ic_launcher_background.xml
- app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
- app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
- feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/settings/AboutScreen.kt

**Modified files:**
- app/src/main/AndroidManifest.xml (added android:icon and android:roundIcon)
- app/build.gradle.kts (enabled buildConfig = true)
- app/src/main/kotlin/io/github/jn0v/traceglass/MainActivity.kt (added "about" route, wired onAbout callback)


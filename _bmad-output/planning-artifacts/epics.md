---
stepsCompleted: [1, 2, 3, 4]
workflowStatus: complete
inputDocuments: ['_bmad-output/planning-artifacts/prd.md', '_bmad-output/planning-artifacts/architecture.md', '_bmad-output/planning-artifacts/ux-design-specification.md']
---

# TraceGlass - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for TraceGlass, decomposing the requirements from the PRD, UX Design, and Architecture into implementable stories.

## Requirements Inventory

### Functional Requirements

- FR1: User can see a live camera feed filling the screen
- FR2: User can import an image from device gallery as overlay source
- FR3: User can see the imported image overlaid on the live camera feed
- FR4: User can adjust overlay opacity continuously (fully transparent to fully opaque)
- FR5: User can change overlay color tint
- FR6: User can toggle inverted transparency mode (swap foreground/background layers)
- FR7: User can toggle the device flashlight to illuminate the drawing surface
- FR8: System can detect fiducial markers in the camera feed in real time
- FR9: System can stabilize overlay position and scale based on detected marker positions
- FR10: System can render overlay during active marker tracking without perceptible lag
- FR11: System can handle partial marker occlusion without losing tracking
- FR12: System can automatically determine overlay scale based on detected marker spacing
- FR13: System can fall back to fixed-screen overlay when no markers are detected (user positions manually)
- FR14: User can manually reposition the overlay via drag gesture
- FR15: User can manually resize the overlay via pinch gesture
- FR16: System can automatically capture periodic snapshots during a tracing session
- FR17: System can compile snapshots into an MP4 video file
- FR18: System can save the time-lapse video to the device shared storage
- FR19: User can share the time-lapse video via system share intent
- FR20: First-time user can see a guided onboarding flow (3 screens)
- FR21: User can skip the onboarding at any point
- FR22: User can re-access the onboarding from app settings
- FR23: Onboarding presents setup tier options (Full DIY, Semi-equipped, Full kit)
- FR24: User can access a DIY marker drawing guide within the app
- FR25: User can access a MacGyver setup guide (improvised phone stands)
- FR26: User can access a link to download printable marker sheets
- FR27: User can access a link to download 3D printable phone stand models
- FR28: User can pause a tracing session and resume it later
- FR29: User can pause and resume time-lapse recording independently
- FR30: System can automatically save session state when app goes to background
- FR31: System can restore full session state when app returns to foreground
- FR32: System can preserve time-lapse snapshots captured before an interruption
- FR33: User can control overlay opacity
- FR34: User can toggle control visibility
- FR35: User can access app settings (re-open onboarding, about, licenses)
- FR36: User can start and stop a tracing session
- FR37: System can play a short audio cue when marker tracking state changes
- FR38: User can enable/disable audio feedback for tracking state changes (off by default)
- FR39: System can remind the user to take a break after a configurable duration of continuous tracing
- FR40: User can enable/disable and configure break reminder interval (off by default)

### NonFunctional Requirements

- NFR1: Camera preview + overlay rendering >= 20 fps on Snapdragon 835 or equivalent
- NFR2: Marker detection latency < 50ms per frame
- NFR3: App cold start to camera-ready < 3 seconds
- NFR4: Overlay opacity adjustment responds within 1 frame (no perceptible lag)
- NFR5: Time-lapse MP4 compilation completes within 2x the session duration
- NFR6: App must not crash during continuous tracing session up to 60 minutes
- NFR7: Temporary loss of marker detection must recover within 500ms without user intervention
- NFR8: Time-lapse data must survive app backgrounding and return to foreground
- NFR9: No data loss if battery dies during session
- NFR10: Zero network calls — no INTERNET permission in manifest
- NFR11: No user data collection, no analytics, no telemetry
- NFR12: Imported images processed in memory only — no copies stored by the app
- NFR13: Time-lapse videos saved only where user expects (shared Movies/ directory)
- NFR14: All interactive elements have content descriptions for screen readers
- NFR15: Text elements support system font scaling
- NFR16: Touch targets >= 48dp per Material Design guidelines
- NFR17: Sufficient color contrast (WCAG AA) for all UI elements
- NFR18: TDD — tests written BEFORE implementation code for all business logic
- NFR19: Unit test coverage >= 80% for non-UI code
- NFR20: Instrumented tests for critical user flows
- NFR21: All code must pass CI checks before merge
- NFR22: Test may only be FAILED when written before implementation (red phase TDD)
- NFR23: Tests must never be skipped, disabled, or ignored to bypass failures
- NFR24: Every test must validate meaningful behavior — no trivial assertions
- NFR25: Test code held to same quality standard as production code

### Additional Requirements

**From Architecture:**
- Starter template: Empty Compose Activity (Android Studio) — first story must create project with 8-module Gradle structure
- F-Droid reproducibility configuration (build-tools 34.x.x, AGP 8.8+, disable ArtProfile, PNG crunch, vector drawable PNG gen)
- OpenCV pre-built .so with scanignore in F-Droid metadata
- NDK pinning with CMake reproducibility flags
- 16KB page size support for Android 15+ NDK code
- Koin DI setup across all 8 modules
- Version catalog (libs.versions.toml) as single source of truth
- CI pipeline (.github/workflows/ci.yml)
- Fastlane metadata structure for F-Droid
- JUnit 5 + MockK migration from default JUnit 4
- Injected dispatchers (AppDispatchers) for all coroutine usage
- Buffer recycling for camera frames and OpenCV Mats (no per-frame allocation)

**From UX Design:**
- Custom components: TrackingStatusIndicator, OpacityFAB, ExpandableMenu, OnboardingOverlay
- Dual encoding for all status indicators (color + shape, never color alone) — colorblindness
- Hybrid onboarding: carousel (3 slides) + interactive camera walkthrough
- FAB-based control layout (opacity right, menu left, tracking indicator top-right)
- Auto-collapse for FABs (3s opacity, 5s menu)
- Audio feedback integration (optional, off by default)
- Break reminder integration (optional, off by default)
- Both portrait + landscape orientation support with no state loss
- Adaptive icon design (glass lens + pencil trace concept)
- App icon: test at 48dp, 72dp, 96dp, 108dp
- Reduced motion support (respect ANIMATOR_DURATION_SCALE)
- Dark mode support via Dynamic Color M3

### FR Coverage Map

| FR | Epic | Description |
|----|------|-------------|
| FR1 | 1 | Live camera feed |
| FR2 | 2 | Import image from gallery |
| FR3 | 2 | Overlay image on camera feed |
| FR4 | 2 | Adjust overlay opacity |
| FR5 | 2 | Change overlay color tint |
| FR6 | 2 | Toggle inverted transparency mode |
| FR7 | 1 | Toggle flashlight |
| FR8 | 3 | Detect fiducial markers in real time |
| FR9 | 3 | Stabilize overlay via marker positions |
| FR10 | 3 | Render overlay without lag during tracking |
| FR11 | 3 | Handle partial marker occlusion |
| FR12 | 3 | Auto-scale overlay from marker spacing |
| FR13 | 2 | Fallback to fixed-screen overlay (no markers) |
| FR14 | 2 | Reposition overlay via drag |
| FR15 | 2 | Resize overlay via pinch |
| FR16 | 4 | Auto-capture periodic snapshots |
| FR17 | 4 | Compile snapshots into MP4 |
| FR18 | 4 | Save time-lapse to shared storage |
| FR19 | 4 | Share time-lapse via share intent |
| FR20 | 6 | Guided onboarding flow (3 screens) |
| FR21 | 6 | Skip onboarding |
| FR22 | 6 | Re-access onboarding from settings |
| FR23 | 6 | Onboarding setup tier options |
| FR24 | 6 | DIY marker drawing guide |
| FR25 | 6 | MacGyver setup guide |
| FR26 | 6 | Link to printable marker sheets |
| FR27 | 6 | Link to 3D printable stand models |
| FR28 | 5 | Pause/resume tracing session |
| FR29 | 4 | Pause/resume time-lapse independently |
| FR30 | 5 | Auto-save session on background |
| FR31 | 5 | Restore session on foreground |
| FR32 | 4 | Preserve snapshots after interruption |
| FR33 | 2 | Control overlay opacity |
| FR34 | 2 | Toggle control visibility |
| FR35 | 7 | Access app settings |
| FR36 | 2 | Start/stop tracing session |
| FR37 | 3 | Audio cue on tracking state change |
| FR38 | 3 | Toggle audio feedback (off by default) |
| FR39 | 7 | Break reminder after configurable duration |
| FR40 | 7 | Toggle/configure break reminder (off by default) |

## Epic List

### Epic 1: Project Foundation & Camera Feed
The user can install the app and see a live camera feed with flashlight control. The project is fully scaffolded with 8-module Gradle structure, F-Droid reproducibility configuration, and a GitHub Actions CI pipeline that mirrors the F-Droid build chain — if CI passes, F-Droid builds should pass too.
**FRs covered:** FR1, FR7
**Additional:** 8-module Gradle structure, version catalog, F-Droid repro config, GitHub Actions CI (build + test, NDK pinning, repro flags), JUnit 5 + MockK setup, CameraX integration, camera permission handling.

### Epic 2: Core Tracing Experience
The user can load an image from their gallery, overlay it on the camera feed, adjust opacity, color, and transparency mode, reposition and resize the overlay, and start tracing — all in fixed-screen mode without markers.
**FRs covered:** FR2, FR3, FR4, FR5, FR6, FR13, FR14, FR15, FR33, FR34, FR36
**Additional:** Photo Picker integration, OpacityFAB component, ExpandableMenu component, overlay rendering via Compose Canvas.

### Epic 3: Marker Tracking
The overlay automatically stabilizes via fiducial marker detection. Markers drawn on paper are detected in real-time by OpenCV, and the overlay position/scale adjusts to follow them — enabling true AR tracing.
**FRs covered:** FR8, FR9, FR10, FR11, FR12, FR37, FR38
**Additional:** OpenCV JNI bridge, C++ marker detection, TrackingStatusIndicator component (dual encoding), audio feedback (optional, off by default).

### Epic 4: Time-lapse Capture & Sharing
The user records a satisfying time-lapse of their drawing process and shares it. Snapshots are captured automatically, compiled into MP4, and exported via system share intent.
**FRs covered:** FR16, FR17, FR18, FR19, FR29, FR32
**Additional:** MediaCodec MP4 encoding, MediaStore export, snapshot preservation across interruptions, pause/resume time-lapse.

### Epic 5: Session Persistence
The user's work survives all interruptions seamlessly. Phone calls, app switches, and battery death do not lose the tracing session.
**FRs covered:** FR28, FR30, FR31
**Additional:** DataStore persistence, auto-save on background, auto-restore on foreground, session pause/resume.

### Epic 6: Onboarding & Setup Guides
New users are guided from zero to their first successful trace through a hybrid onboarding experience — carousel + interactive camera walkthrough.
**FRs covered:** FR20, FR21, FR22, FR23, FR24, FR25, FR26, FR27
**Additional:** HorizontalPager carousel, OnboardingOverlay component, setup tier selection, DIY marker guide, MacGyver setup guide, external download links.

### Epic 7: Settings & Comfort Features
The user can customize their experience with break reminders, access app info, and re-open onboarding. The app icon completes the visual identity.
**FRs covered:** FR35, FR39, FR40
**Additional:** Settings screen, break reminder (configurable, off by default), about/licenses, adaptive icon (glass lens + pencil trace).

## Epic 1: Project Foundation & Camera Feed

The user can install the app and see a live camera feed with flashlight control. The project is fully scaffolded with 8-module Gradle structure, F-Droid reproducibility configuration, and a GitHub Actions CI pipeline that mirrors the F-Droid build chain.

### Story 1.1: Project Scaffolding & Multi-Module Structure

As a developer,
I want a properly scaffolded Android project with 8-module Gradle structure,
So that all future development has a clean, consistent foundation.

**Acceptance Criteria:**

**Given** no existing project
**When** the project is created from Empty Compose Activity template
**Then** the following 8 modules exist and compile: `:app`, `:core:camera`, `:core:cv`, `:core:overlay`, `:core:session`, `:feature:tracing`, `:feature:onboarding`, `:feature:timelapse`
**And** `settings.gradle.kts` includes all 8 modules
**And** `libs.versions.toml` is the single source of truth for all dependency versions
**And** Min SDK is API 33, target SDK is latest stable
**And** package name is `io.github.jn0v.traceglass`
**And** `./gradlew assembleDebug` succeeds
**And** `./gradlew test` succeeds (even if no tests yet)
**And** each module has a `build.gradle.kts` with correct dependencies per architecture doc

### Story 1.2: F-Droid Reproducibility Configuration

As a developer,
I want F-Droid reproducibility settings applied to the build,
So that the app can be accepted into the F-Droid repository.

**Acceptance Criteria:**

**Given** the project from Story 1.1
**When** the build configuration is applied
**Then** `buildToolsVersion` is pinned to `34.0.0`
**And** AGP version is 8.8+
**And** ArtProfile tasks are disabled
**And** PNG crunch is disabled (`cruncherEnabled = false`)
**And** vector drawable PNG generation is disabled (`generatedDensities = emptySet()`)
**And** R8 proguard rules for kotlinx.coroutines are added
**And** JUnit 5 (Jupiter) replaces default JUnit 4
**And** MockK is added as test dependency
**And** Koin is configured with empty modules in `:app`
**And** `./gradlew assembleRelease` succeeds

### Story 1.3: GitHub Actions CI Pipeline

As a developer,
I want a CI pipeline on GitHub Actions that mirrors the F-Droid build chain,
So that if CI passes, I am confident the F-Droid build will also pass.

**Acceptance Criteria:**

**Given** the project with F-Droid config from Story 1.2
**When** a push or pull request is made to the main branch
**Then** GitHub Actions runs a workflow that: builds the project (`assembleDebug` + `assembleRelease`), runs all unit tests (`test`), and runs lint
**And** the CI uses the same JDK version as F-Droid (JDK 17)
**And** the CI pins the same Android SDK, build-tools, and NDK versions as the project
**And** the workflow file is at `.github/workflows/ci.yml`
**And** CI results are visible on pull requests
**And** a failing build blocks merge (branch protection configured in README instructions)

### Story 1.4: Live Camera Feed

As a user,
I want to see a live camera feed filling my screen when I open the app,
So that I can see my drawing surface through the phone.

**Acceptance Criteria:**

**Given** the app is launched and camera permission is not yet granted
**When** the app requests camera permission
**Then** the system permission dialog is shown with a rationale
**And** if granted, the rear camera feed fills the screen in real-time
**And** if denied, an explanatory screen is shown with a "Grant Permission" button that re-triggers the request

**Given** camera permission is already granted
**When** the app launches
**Then** the rear camera feed is displayed immediately without any dialog
**And** the feed runs at the device's native preview frame rate
**And** both portrait and landscape orientations are supported

### Story 1.5: Flashlight Toggle

As a user,
I want to toggle the flashlight from the tracing screen,
So that I can illuminate my drawing surface in low light.

**Acceptance Criteria:**

**Given** the camera feed is active
**When** the user taps the flashlight button in the expandable menu
**Then** the device torch turns on
**And** the button icon visually indicates the torch is active

**Given** the torch is on
**When** the user taps the flashlight button again
**Then** the torch turns off
**And** the button icon returns to inactive state

**Given** the device has no flashlight hardware
**When** the tracing screen loads
**Then** the flashlight button is hidden (not disabled)

## Epic 2: Core Tracing Experience

The user can load an image from their gallery, overlay it on the camera feed, adjust opacity, color, and transparency mode, reposition and resize the overlay, and start tracing — all in fixed-screen mode without markers.

### Story 2.1: Image Import via Photo Picker

As a user,
I want to pick an image from my gallery to use as a tracing reference,
So that I can overlay any image I want onto my drawing surface.

**Acceptance Criteria:**

**Given** the camera feed is active and no overlay image is loaded
**When** the user taps the image picker button
**Then** the system Photo Picker (API 33+) opens
**And** no storage permission is requested

**Given** the Photo Picker is open
**When** the user selects an image
**Then** the image is loaded into memory and displayed as an overlay on the camera feed
**And** the image is not copied to app storage (NFR12)
**And** the overlay appears centered on the screen with default 50% opacity

**Given** the Photo Picker is open
**When** the user cancels without selecting
**Then** the app returns to the camera feed unchanged

### Story 2.2: Overlay Rendering on Camera Feed

As a user,
I want to see my chosen image overlaid on the live camera feed,
So that I can trace the image onto my paper.

**Acceptance Criteria:**

**Given** an image has been imported
**When** the overlay is displayed
**Then** the image appears semi-transparent over the camera feed via Compose Canvas
**And** the overlay respects the current opacity setting
**And** rendering does not drop the camera feed below 20 fps (NFR1)
**And** the overlay is rendered correctly in both portrait and landscape orientations

### Story 2.3: Opacity Control (OpacityFAB)

As a user,
I want to quickly adjust the overlay opacity,
So that I can see my paper clearly while still seeing the reference image.

**Acceptance Criteria:**

**Given** an overlay image is displayed
**When** the user taps the OpacityFAB (right side, mid-bottom)
**Then** a vertical slider expands above the FAB
**And** the slider shows the current opacity value

**Given** the opacity slider is visible
**When** the user drags the slider
**Then** the overlay opacity changes in real-time (within 1 frame, NFR4)
**And** the range is 0% (fully transparent) to 100% (fully opaque), step 5%
**And** the current value is announced for TalkBack ("Overlay opacity: 60%")

**Given** the opacity slider is visible
**When** 3 seconds pass without interaction
**Then** the slider auto-collapses back to the FAB

**Given** no overlay image is loaded
**When** the screen is displayed
**Then** the OpacityFAB is hidden

### Story 2.4: Overlay Visual Modes

As a user,
I want to change the overlay color tint and toggle inverted transparency,
So that I can optimize visibility on different paper/drawing combinations.

**Acceptance Criteria:**

**Given** an overlay is displayed
**When** the user selects a color tint option
**Then** the overlay is rendered with the selected color filter applied
**And** the change is visible immediately

**Given** an overlay is displayed
**When** the user toggles inverted transparency mode
**Then** the foreground/background alpha layers swap
**And** the change is visible immediately

### Story 2.5: Overlay Positioning (Drag & Pinch)

As a user,
I want to manually reposition and resize the overlay,
So that I can align the reference image precisely with my paper.

**Acceptance Criteria:**

**Given** an overlay is displayed in fixed-screen mode (no markers)
**When** the user drags on the overlay area
**Then** the overlay repositions following the finger movement
**And** the movement is smooth (no jitter)

**Given** an overlay is displayed
**When** the user performs a pinch gesture
**Then** the overlay resizes proportionally from the pinch center
**And** there is no minimum or maximum hard limit that blocks the gesture

**Given** an overlay is displayed
**When** the user performs both drag and pinch simultaneously
**Then** both reposition and resize are applied correctly

### Story 2.6: Tracing Session & Control Visibility

As a user,
I want to start and stop a tracing session and toggle control visibility,
So that I have minimal UI interference while drawing.

**Acceptance Criteria:**

**Given** an overlay is displayed
**When** the user starts a tracing session
**Then** the session is active and the screen remains on (wake lock)
**And** the FAB controls remain accessible

**Given** a tracing session is active
**When** the user stops the session
**Then** the session ends and the wake lock is released

**Given** controls are visible
**When** the user toggles control visibility
**Then** all overlay controls (FABs, menu) are hidden except the tracking indicator
**And** tapping the screen toggles them back

**Given** the ExpandableMenu FAB is visible (bottom-left)
**When** the user taps it
**Then** it expands to show secondary controls (image picker, flashlight, settings) with staggered animation
**And** it auto-collapses after 5 seconds of inactivity

## Epic 3: Marker Tracking

The overlay automatically stabilizes via fiducial marker detection. Markers drawn on paper are detected in real-time by OpenCV, and the overlay position/scale adjusts to follow them.

### Story 3.1: OpenCV JNI Integration & Basic Marker Detection

As a developer,
I want OpenCV integrated via JNI with a basic marker detection pipeline,
So that the app can detect fiducial markers in camera frames.

**Acceptance Criteria:**

**Given** the project with CameraX from Epic 1
**When** the OpenCV Android SDK is integrated
**Then** the `:core:cv` module contains a `MarkerDetector` interface and `OpenCvMarkerDetector` implementation
**And** JNI native code is in `:core:cv/src/main/cpp/`
**And** CMakeLists.txt includes reproducibility flags (`-ffile-prefix-map`, `--build-id=none`, `--hash-style=gnu`)
**And** a `FakeMarkerDetector` exists for unit tests
**And** `./gradlew :core:cv:test` passes
**And** CI pipeline still passes with NDK integration

**Given** a camera frame is available
**When** the frame is passed to `MarkerDetector.detect()`
**Then** the method returns a `MarkerResult` with detected marker positions and confidence scores
**And** detection completes within 50ms (NFR2)

### Story 3.2: Marker-Based Overlay Stabilization

As a user,
I want the overlay to follow my markers automatically,
So that the reference image stays aligned even if the phone or glass shifts.

**Acceptance Criteria:**

**Given** markers are detected in the camera feed
**When** the overlay is displayed
**Then** the overlay position and scale are computed from marker positions
**And** the overlay updates within 1 frame of marker movement (NFR4)
**And** the overlay scale is determined by marker spacing (FR12)
**And** no visible jitter during stable tracking

**Given** markers are detected
**When** markers were not detected in the previous frame (transition from fixed to tracked mode)
**Then** the overlay smoothly transitions to tracked position (no snap/jump)

### Story 3.3: Tracking Resilience & Fallback

As a user,
I want tracking to recover gracefully when my hand covers a marker,
So that I can draw without worrying about blocking markers.

**Acceptance Criteria:**

**Given** tracking is active with all markers visible
**When** one or more markers are partially occluded by the user's hand
**Then** tracking continues using remaining visible markers (FR11)
**And** overlay stability is maintained

**Given** all markers are lost
**When** 500ms pass without detection (NFR7)
**Then** the overlay switches to fixed-screen mode (last known position)
**And** the tracking status indicator changes to "Lost" state

**Given** tracking was lost
**When** markers become visible again
**Then** tracking recovers within 500ms (NFR7)
**And** the overlay smoothly transitions back to tracked position

### Story 3.4: Tracking Status Indicator & Audio Feedback

As a user,
I want to know if marker tracking is active at a glance,
So that I understand why the overlay might not be following my paper.

**Acceptance Criteria:**

**Given** the tracing screen is displayed
**When** markers are actively tracked
**Then** a green checkmark icon is shown in the top-right corner

**Given** the tracing screen is displayed
**When** markers are lost
**Then** an amber triangle icon replaces the checkmark
**And** the indicator uses dual encoding: color + shape (never color alone)

**Given** audio feedback is enabled in settings
**When** tracking state changes from tracked to lost
**Then** a short, soft audio cue plays

**Given** audio feedback is enabled
**When** tracking state changes from lost to tracked
**Then** a different short audio cue plays

**Given** default app settings
**When** the app is first installed
**Then** audio feedback is disabled (off by default, FR38)

## Epic 4: Time-lapse Capture & Sharing

The user records a satisfying time-lapse of their drawing process and shares it.

### Story 4.1: Automatic Snapshot Capture

As a user,
I want the app to automatically capture snapshots of my drawing progress,
So that a time-lapse video can be created from my tracing session.

**Acceptance Criteria:**

**Given** a tracing session is active
**When** time-lapse is recording
**Then** periodic snapshots are captured automatically (e.g., every 5 seconds)
**And** snapshots are saved as JPEG files in `context.filesDir/timelapse/`
**And** capture does not drop the camera feed below 20 fps (NFR1)

**Given** time-lapse is recording
**When** the user pauses time-lapse (FR29)
**Then** snapshot capture pauses
**And** a visual indicator shows time-lapse is paused

**Given** time-lapse is paused
**When** the user resumes time-lapse
**Then** snapshot capture resumes from where it left off

### Story 4.2: MP4 Video Compilation

As a user,
I want my snapshots compiled into a time-lapse video,
So that I can watch my drawing appear in fast-forward.

**Acceptance Criteria:**

**Given** a tracing session has captured snapshots
**When** the user stops the session or requests compilation
**Then** snapshots are compiled into an MP4 video using MediaCodec
**And** compilation completes within 2x session duration (NFR5)
**And** the video plays correctly on standard Android video players
**And** compilation runs on a background thread (Dispatchers.IO)

### Story 4.3: Time-lapse Export & Sharing

As a user,
I want to save and share my time-lapse video,
So that I can show my friends how my drawing was made.

**Acceptance Criteria:**

**Given** an MP4 video has been compiled
**When** the video is saved
**Then** it is saved to the device shared Movies/ directory via MediaStore (NFR13)
**And** no storage permission is required (API 33+)

**Given** a time-lapse video exists
**When** the user taps the share button
**Then** the system share intent opens with the video file
**And** the user can share via any installed app (WhatsApp, etc.)

### Story 4.4: Time-lapse Snapshot Preservation

As a user,
I want my time-lapse snapshots to survive interruptions,
So that I don't lose my recording if the app is backgrounded.

**Acceptance Criteria:**

**Given** time-lapse is recording with captured snapshots
**When** the app goes to background (phone call, app switch)
**Then** all captured snapshots are preserved on disk (FR32)

**Given** the app returns to foreground after interruption
**When** the session is restored
**Then** time-lapse can resume capturing from where it left off
**And** previously captured snapshots are included in final compilation

**Given** the device battery dies during a session
**When** the app relaunches
**Then** all snapshots captured before the battery died are still available (NFR9)

## Epic 5: Session Persistence

The user's work survives all interruptions seamlessly.

### Story 5.1: Session Auto-Save on Background

As a user,
I want my session to be automatically saved when I leave the app,
So that I don't lose my work if I receive a phone call.

**Acceptance Criteria:**

**Given** a tracing session is active with an overlay image, position, opacity, and settings
**When** the app goes to background (home button, phone call, app switch)
**Then** the full session state is saved to DataStore: image URI, overlay transform (position, scale), opacity, color tint, inverted mode, time-lapse progress
**And** the save completes before the Activity is destroyed (FR30)

### Story 5.2: Session Auto-Restore on Foreground

As a user,
I want my session to be seamlessly restored when I return to the app,
So that I can continue tracing exactly where I left off.

**Acceptance Criteria:**

**Given** a session was saved when the app went to background
**When** the app returns to foreground
**Then** the overlay image, position, scale, opacity, color tint, and inverted mode are restored exactly as they were (FR31)
**And** the restoration is instant (no visible loading for session state)
**And** the camera feed resumes automatically

**Given** the saved image URI is no longer accessible (user deleted the image)
**When** session restore is attempted
**Then** the app shows the camera feed without overlay
**And** a snackbar informs the user "Reference image no longer available"

### Story 5.3: Explicit Session Pause & Resume

As a user,
I want to explicitly pause and resume my tracing session,
So that I can take breaks and come back later.

**Acceptance Criteria:**

**Given** a tracing session is active
**When** the user taps pause
**Then** the session is paused and persisted to DataStore (FR28)
**And** the wake lock is released
**And** the UI indicates the session is paused

**Given** a session is paused
**When** the user taps resume
**Then** the session resumes with all state intact
**And** the wake lock is re-acquired

## Epic 6: Onboarding & Setup Guides

New users are guided from zero to their first successful trace through a hybrid onboarding experience.

### Story 6.1: Onboarding Carousel

As a first-time user,
I want a clear visual introduction to TraceGlass,
So that I understand what the app does and how to set up.

**Acceptance Criteria:**

**Given** the app is launched for the first time
**When** the onboarding screen appears
**Then** a 3-slide carousel is displayed using HorizontalPager:
  - Slide 1: Welcome + concept explanation
  - Slide 2: Choose setup tier (Full DIY, Semi-equipped, Full kit) (FR23)
  - Slide 3: Prepare your markers (based on chosen tier)
**And** each slide has a "Next" button and a page indicator
**And** swipe navigation works between slides

**Given** the user is on any onboarding slide
**When** the user taps "Skip" (FR21)
**Then** the onboarding is dismissed and the camera screen is shown
**And** the onboarding is marked as completed in DataStore

### Story 6.2: Interactive Camera Walkthrough

As a first-time user,
I want to be guided through my first marker detection on the live camera,
So that I know my setup works before I start tracing.

**Acceptance Criteria:**

**Given** the onboarding carousel is completed (not skipped)
**When** camera permission is granted
**Then** the interactive walkthrough begins on the live camera feed
**And** an OnboardingOverlay composable guides the user step by step

**Given** the walkthrough is active
**When** the user points the camera at their markers
**Then** if markers are detected within 10 seconds, a success message is shown
**And** if markers are NOT detected within 10 seconds, gentle guidance is displayed ("Try adjusting the angle or lighting")

**Given** the walkthrough marker detection succeeds
**When** the user proceeds
**Then** the Photo Picker opens for selecting the first reference image
**And** once selected, the overlay appears with a tooltip: "Adjust opacity here" pointing to the OpacityFAB
**And** the tooltip appears only once (never again)

### Story 6.3: Setup Guides & External Links

As a user,
I want to access guides for drawing markers and setting up my tracing station,
So that I can prepare everything I need to start tracing.

**Acceptance Criteria:**

**Given** the user is in the onboarding flow or navigates to guides from settings
**When** the user accesses the DIY marker drawing guide (FR24)
**Then** a screen shows visual instructions for drawing each marker shape (heart, star, cross, circle, diamond)
**And** the content uses vector drawables (not raster images)

**Given** the user accesses the MacGyver setup guide (FR25)
**When** the guide is displayed
**Then** it shows step-by-step instructions for improvised phone stands (glass of water, books, etc.)

**Given** the user accesses external download links
**When** they tap "Download printable marker sheet" (FR26) or "Download 3D stand model" (FR27)
**Then** the system browser opens with the respective URL
**And** no network permission is added to the app (links open in external browser)

### Story 6.4: Onboarding Flow Control

As a returning user,
I want to re-access the onboarding from settings,
So that I can review setup guides if needed.

**Acceptance Criteria:**

**Given** the user has completed onboarding
**When** the user opens Settings and taps "Re-open onboarding" (FR22)
**Then** the onboarding carousel is shown again from slide 1
**And** completing or skipping it returns to the previous screen

**Given** the user has completed onboarding previously
**When** the app launches
**Then** onboarding is NOT shown — the camera feed appears directly

## Epic 7: Settings & Comfort Features

The user can customize their experience with break reminders, access app info, and re-open onboarding.

### Story 7.1: Settings Screen

As a user,
I want to access app settings,
So that I can customize my experience and find app information.

**Acceptance Criteria:**

**Given** the user taps "Settings" from the ExpandableMenu
**When** the Settings screen opens
**Then** the following options are displayed:
  - Audio feedback toggle (FR38, off by default)
  - Break reminder toggle + interval selector (FR40, off by default)
  - Re-open onboarding (FR22)
  - About / Licenses
**And** the screen uses M3 ListItem components
**And** changes are persisted immediately to DataStore
**And** the system Back button returns to the camera screen

### Story 7.2: Break Reminders

As a user,
I want to be reminded to take breaks during long drawing sessions,
So that I maintain healthy drawing habits.

**Acceptance Criteria:**

**Given** break reminders are enabled with a 30-minute interval (FR39)
**When** 30 minutes of continuous tracing elapse
**Then** a gentle, non-blocking snackbar appears: "Time for a break!"
**And** a soft audio tone plays if audio feedback is enabled
**And** the snackbar auto-dismisses after 10 seconds or on tap

**Given** the break reminder snackbar is dismissed
**When** the user continues tracing
**Then** the timer resets and the next reminder is in another 30 minutes

**Given** default app settings
**When** the app is first installed
**Then** break reminders are disabled (off by default, FR40)

**Given** break reminders are enabled
**When** the user changes the interval in settings
**Then** the timer resets with the new interval

### Story 7.3: App Icon & About Screen

As a user,
I want a recognizable app icon and access to app information,
So that I can identify TraceGlass on my home screen and know its version and licenses.

**Acceptance Criteria:**

**Given** the app is installed
**When** the user views their app drawer or home screen
**Then** the TraceGlass adaptive icon is displayed (glass lens + pencil trace, teal palette)
**And** the icon is readable at 48dp, 72dp, 96dp, and 108dp sizes
**And** the icon has separate foreground and background layers (Android adaptive icon format)

**Given** the user opens Settings → About
**When** the About screen is displayed
**Then** the app name, version, and build number are shown
**And** open-source license information is accessible
**And** a link to the project repository is provided

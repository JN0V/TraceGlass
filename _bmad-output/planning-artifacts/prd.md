---
stepsCompleted: ['step-01-init', 'step-02-discovery', 'step-03-success', 'step-04-journeys', 'step-05-domain', 'step-06-innovation', 'step-07-project-type', 'step-08-scoping', 'step-09-functional', 'step-10-nonfunctional', 'step-11-polish', 'step-12-complete']
workflowStatus: complete
classification:
  projectType: mobile_app
  domain: creative_tools_art
  complexity: medium
  projectContext: greenfield
inputDocuments: ['_bmad-output/brainstorming/brainstorming-session-2026-02-07.md']
workflowType: 'prd'
documentCounts:
  briefs: 0
  research: 0
  brainstorming: 1
  projectDocs: 0
---

# Product Requirements Document - TraceGlass

**Author:** JN0V
**Date:** 2026-02-07

## Executive Summary

**Product:** TraceGlass — a free, open-source Android app that overlays a semi-transparent reference image on the live camera feed, enabling users to trace drawings on paper in augmented reality.

**Target Users:** Teen and amateur artists who want to improve their drawing skills by tracing reference images. Primary persona: 13-year-old artist with a mid-range Android phone and a sketchbook.

**Distribution:** F-Droid exclusively. No Google Play Store. No ads, no trackers, no accounts, no proprietary dependencies.

**Core Differentiators:**
- Decorative fiducial markers (technical tracking requirement transformed into delightful UX)
- Physical kit (3D-printable stand + printable marker sheet) as open-source hardware companion
- Three-tier accessibility (Full DIY → Semi-equipped → Full kit) — zero barrier to entry
- First open-source AR tracing app on F-Droid — empty niche
- Single permission (CAMERA only) — maximum privacy trust

**Technology:** Kotlin native, Jetpack Compose, CameraX, OpenCV (C++ via JNI). Min SDK API 33 (Android 13).

## Success Criteria

### User Success

- **Time to first trace (returning user):** < 2 minutes from app launch to first pencil stroke
- **Time to first trace (new user, with setup):** < 10 minutes including onboarding + marker placement
- **Onboarding:** Skippable, re-accessible from settings. Must not block experienced users
- **Core experience:** Overlay repositions within 1 frame of marker movement, no visible jitter. User focuses on drawing, not on the app
- **Time-lapse output:** App produces a shareable video file. Sharing via system share intent (Android), no built-in social features
- **"Aha" moment:** First time the user sees the overlay on their paper through the camera and realizes they can trace

### Business Success

- **Primary metric:** User satisfaction (first user = developer's daughter)
- **Project health:** Active open-source project with community contributions (PRs, translations, 3D models for different phones)
- **3-month goal:** Working v1 on F-Droid, at least 1 external contributor
- **12-month goal:** Stable user base, active issue tracker, community-contributed phone stand models
- **Financial:** No revenue target. Patreon/donations are a bonus, not a success criterion

### Technical Success

- **Frame rate:** ≥ 20 fps for camera + overlay on Snapdragon 835 (OnePlus 5T baseline)
- **Marker tracking:** Stable fiducial detection without visible jitter — decorative markers must work reliably
- **Offline:** 100% functional without any network connection
- **Privacy:** Zero data collection, zero trackers, zero proprietary dependencies
- **F-Droid compliance:** Passes F-Droid inclusion criteria (reproducible builds, FOSS dependencies only)

### Measurable Outcomes

- User completes a full traced drawing in a single session
- Time-lapse video is generated and saved to device
- App runs without crash on devices with API 33+ (Android 13+)
- Onboarding completion rate (measured via user feedback, no in-app analytics)

## User Journeys

### Journey 1: Léa, 13 ans — First Session (Happy Path)

**Situation:** Léa loves drawing but struggles to reproduce proportions from a portrait she found on Pinterest. She has a OnePlus 5T and a sketchbook.

**Opening Scene:** She installs TraceGlass from F-Droid. The app opens with a 3-screen visual onboarding: "Choose your setup", "Prepare your markers", "Pick an image". She has no printer, so she picks Full DIY mode.

**Rising Action:** She follows the guide to draw a heart, a star, a cross, and a circle at the corners of her sketchbook page with a felt-tip pen. She balances her phone on a glass of water above the page (MacGyver setup). She picks the portrait from her gallery.

**Climax:** The image appears as an overlay on her sketchbook. She adjusts opacity by sliding on the right edge. She drags and pinches to align the portrait precisely with her page. She taps "Lock" — the image is now fixed on the paper. The markers are detected, the image stays stable even when the glass shifts slightly. **This is the "aha" moment.** She starts tracing. She pinches to zoom into a detail of the eye she's working on — the camera feed and overlay zoom together.

**Resolution:** 45 minutes later, she has a portrait she's proud of. The time-lapse finishes; she watches it — it's satisfying to see the drawing appear in fast-forward. She shares it with her best friend via WhatsApp.

**Capabilities revealed:** Onboarding, DIY marker guide, MacGyver setup guide, gallery import, overlay, overlay lock, viewport zoom, fiducial tracking, opacity control, time-lapse, share intent.

### Journey 2: Léa — Recurring Use (Tuesday Evening)

**Situation:** Léa has used TraceGlass several times already. Her markers are still drawn on her sketchbook, her glass is in her room.

**Opening Scene:** She opens the app. No onboarding (skipped). Direct camera screen.

**Rising Action:** She picks a stylized cat image from her gallery. Adjusts the overlay size and position with drag and pinch gestures. Taps "Lock" — done. Places phone, starts tracing.

**Climax:** Within 2 minutes, she's drawing. Zero friction. The app disappears — she only thinks about her drawing. She zooms in to trace the cat's whiskers precisely, then zooms back out.

**Resolution:** Drawing done in 30 minutes. Time-lapse saved. She keeps it for herself this time.

**Capabilities revealed:** Skip onboarding, direct camera access, overlay positioning + lock, viewport zoom, time-lapse auto-capture.

### Journey 3: Léa + Friend — Body Art (Phase 2)

> **Note:** This journey targets Phase 2 capabilities (markerless handheld mode). Included here for vision completeness.

**Situation:** A Wednesday afternoon, Léa and her friend want to draw fake tattoos on their arms.

**Opening Scene:** Léa opens TraceGlass, picks a butterfly pattern from her gallery. She points the camera at her friend's arm.

**Rising Action:** No markers needed — the arm is steady, the phone is handheld. The app displays a safety message: "Use cosmetic makeup or body-safe markers. Do not use ballpoint pens or permanent markers on skin." They use eyeliner from Léa's mother.

**Climax:** The butterfly appears as an overlay on the arm. Léa traces carefully. The time-lapse is running. The result is stunning.

**Resolution:** They make three designs each. The time-lapse is shared on their class group chat. Five friends want to install the app.

**Capabilities revealed:** Markerless handheld mode, body art safety messaging, overlay on non-flat surface, viral sharing potential.

### Journey 4: Marc, 28 — Open-Source Contributor

**Situation:** Marc, a developer, is looking for a cool open-source Android project to contribute to. He discovers TraceGlass on F-Droid, tries it, likes it.

**Opening Scene:** He visits the repo, reads the README, sees issues tagged "good first issue".

**Rising Action:** He owns a 3D printer and a Samsung Galaxy S21. He modifies the parametric 3D model to fit his phone. He tests it, it works.

**Climax:** He opens a PR with his adapted model + a photo of the result. The PR is merged.

**Resolution:** Other Samsung users download his model. Marc keeps contributing — he proposes a "sepia" filter for the overlay.

**Capabilities revealed:** Well-documented repo, tagged issues, parametric 3D model, clear contribution process.

### Journey Requirements Summary

| Journey | Key Capabilities |
|---------|-----------------|
| **First Session** | Onboarding, DIY marker guide, MacGyver setup, gallery import, overlay, fiducial tracking, opacity control, time-lapse, share intent |
| **Recurring Use** | Skip onboarding, direct camera, pinch-to-resize, time-lapse |
| **Body Art** | Markerless handheld mode, safety messaging, overlay on non-flat surface |
| **Contributor** | Documented repo, tagged issues, parametric 3D model, contribution guide |

## Innovation & Novel Patterns

### Detected Innovation Areas

1. **Decorative fiducial markers** — Transforming a technical constraint (tracking markers) into a delightful UX element. Simple shapes (heart, star, cross, circle, diamond) that are fun to draw by hand AND technically functional for computer vision tracking. No existing app does this.

2. **Physical kit as software differentiator** — No software competitor provides tangible, printable hardware. A 3D-printable phone stand + decorative marker sheet creates a unique "unboxing" experience even though it's free and open-source.

3. **Three-tier accessibility model** — Full DIY (zero equipment, hand-drawn markers, everyday objects as stands) → Semi-equipped (printed markers, improvised stand) → Full kit (3D-printed stand + printed markers). No competitor addresses users who have nothing.

4. **First open-source AR tracing app on F-Droid** — Empty niche. Positioning innovation rather than technical innovation, but significant for the target audience (privacy-conscious users, open-source community).

### Validation Approach

- **Priority 1:** Prototype decorative marker detection with OpenCV on a Snapdragon 835 device. This is the highest-risk innovation and must be validated before committing to this approach.
- **Priority 2:** Test overlay stability with fiducial tracking in real drawing conditions (varying light, slight phone movement, partial marker occlusion by hand).

### Innovation Risk Mitigation

- **If decorative markers fail detection:** Fallback to standard ArUco markers embedded in a decorative frame/border design around the drawing area
- **If performance is too low on older devices:** Fixed-screen overlay without tracking (user positions manually). See also: Risk Mitigation Strategy in Project Scoping

## Mobile App Specific Requirements

### Platform & Technology

- **Platform:** Android only (native)
- **Language:** Kotlin
- **Min SDK:** API 33 (Android 13)
- **Target SDK:** Latest stable
- **UI Framework:** Jetpack Compose + Material 3 (Material You dynamic colors)
- **Camera:** CameraX API
- **Computer Vision:** OpenCV Android SDK (C++ via JNI)
- **Cross-platform strategy:** Not planned for MVP. Kotlin Multiplatform (KMP) as future path for iOS if needed

### Technology Choice Rationale

Kotlin native selected over TypeScript + Capacitor and Tauri 2.0 due to:
- Real-time camera pipeline requires zero-overhead access to CameraX and GPU rendering
- OpenCV runs natively via JNI — no WebAssembly penalty
- Overlay rendering via OpenGL ES / direct GPU composition — no WebView layer
- F-Droid build toolchain is mature and well-documented for standard Android projects
- Capacitor excluded: JS bridge bottleneck kills frame rate for real-time CV (estimated 5-15 fps vs 20-30 fps native)
- Tauri excluded: Rust backend is fast for CV, but camera integration via JNI from Rust is complex, WebView overlay architecture is fragile, and Tauri Mobile + F-Droid has no community precedent

### Device Permissions

| Permission | Purpose | Required |
|---|---|---|
| `CAMERA` | Live camera feed for tracing overlay | Yes — only dangerous permission |

**No storage permissions needed** thanks to API 33+:
- Image import: System Photo Picker (no permission required)
- Time-lapse export: MediaStore API (no permission required for writing to shared storage)

### Offline Mode

- 100% offline operation — no network calls, no backend, no analytics
- All features work without internet connection
- No account creation, no login, no cloud sync
- Network permission not declared in manifest

### F-Droid Compliance

- All dependencies must be FOSS-licensed
- Reproducible builds required
- No proprietary Google Play Services dependency
- No Firebase, no Google Analytics, no Crashlytics
- OpenCV is BSD-licensed (compatible)
- Build via standard Gradle toolchain

## Project Scoping & Phased Development

### MVP Strategy & Philosophy

**MVP Approach:** Experience MVP — the core tracing experience must be fluid and delightful from day one. No "technical demo" release. The "aha" moment (overlay appearing on paper) must work reliably.

**Resource Reality:** Solo developer with full-time job. AI-assisted development (pair programming with Claude). No external deadline. First user = developer's daughter.

### MVP Feature Set (Phase 1)

**Core User Journeys Supported:**
- Journey 1: First session (happy path with onboarding + DIY setup)
- Journey 2: Recurring use (quick start, direct camera)

**Must-Have Capabilities:**
- Camera overlay with adjustable opacity
- Image import from gallery (Photo Picker API 33+)
- Simple fiducial marker tracking via OpenCV (simplest reliable shapes — to be validated by prototype)
- Automatic overlay scaling based on marker spacing
- Fallback to fixed-screen overlay when no markers detected (manual positioning via drag/pinch)
- Minimalist touch controls (edge slides for opacity, double-tap for controls)
- Overlay color choice (trivial: color filter on overlay bitmap)
- Inverted transparency mode (trivial: alpha blend toggle)
- Flashlight toggle for low-light drawing
- Time-lapse capture with pause/resume + MP4 export via MediaStore
- Session persistence (auto-save on background, restore on foreground)
- System share intent for time-lapse videos
- Skippable first-session onboarding (3 screens), re-accessible from settings
- DIY marker guide + MacGyver setup guide
- Printable marker sheet (external download)
- 3D printable phone stand model (external download)
- 100% offline, single permission (CAMERA only)

### Phase 2 — Growth

- Body art mode (markerless handheld) + safety messaging
- Drawing-as-reference realignment for body art (feature matching via OpenCV ORB/AKAZE on partially traced drawing to realign overlay after pause)
- Edge detection / outline filter
- Additional decorative marker shapes
- Community-contributed 3D stand models for various phones

### Phase 3 — Vision

- Hybrid tracking (fiducial markers + drawing feature matching) for enhanced paper mode resilience
- Progressive difficulty levels
- Composition grids (rule of thirds, golden ratio)
- Before/after comparison slider
- Achievement/badge system
- Ghost mode (progress comparison)
- Corrector mode (freehand then compare)
- Contactless gesture control (hand recognition)
- Mural/fresco mode (vertical surfaces)
- Accessibility adaptations (motor difficulties, seniors)
- Parametric 3D model web configurator

### Risk Mitigation Strategy

| Risk | Type | Mitigation |
|---|---|---|
| Fiducial marker detection reliability | Technical | Prototype first — if custom shapes fail, fallback to standard ArUco markers in decorative frame |
| Performance ≥ 20 fps on 2020 mid-range | Technical | Early benchmarking, frame rate throttling, optional "simple mode" without tracking |
| Solo dev + full-time job = slow velocity | Resource | Minimal MVP scope, AI-assisted development, no external deadline pressure |
| F-Droid adoption uncertainty | Market | Primary user (daughter) validates the product. Community adoption is a bonus, not a requirement |
| OpenCV library size impact on APK | Technical | Use only required OpenCV modules, strip unused native libraries |

## Functional Requirements

### Image Overlay & Display

- **FR1:** User can see a live camera feed filling the screen
- **FR2:** User can import an image from device gallery as overlay source
- **FR3:** User can see the imported image overlaid on the live camera feed
- **FR4:** User can adjust overlay opacity continuously (fully transparent to fully opaque)
- **FR5:** User can change overlay color tint
- **FR6:** User can toggle inverted transparency mode (swap foreground/background layers)
- **FR7:** User can toggle the device flashlight to illuminate the drawing surface

### Marker Tracking & Adaptive Behavior

- **FR8:** System can detect fiducial markers in the camera feed in real time
- **FR9:** System can stabilize overlay position and scale based on detected marker positions
- **FR10:** System can render overlay during active marker tracking without perceptible lag
- **FR11:** System can handle partial marker occlusion without losing tracking
- **FR12:** System can automatically determine overlay scale based on detected marker spacing
- **FR13:** System can fall back to fixed-screen overlay when no markers are detected (user positions manually)

### Overlay Positioning & Lock

- **FR14:** User can manually reposition the overlay via drag gesture (before lock)
- **FR15:** User can manually resize the overlay via pinch gesture (before lock)
- **FR41:** User can rotate the overlay via rotation gesture (before lock)
- **FR42:** User can lock the overlay position/scale/rotation relative to the paper ("Lock" action)
- **FR43:** User can unlock the overlay with a confirmation dialog to return to positioning mode
- **FR44:** After lock, user can zoom (pinch) and pan (drag) the viewport — camera feed and overlay zoom/pan together (digital crop)
- **FR45:** After lock, opacity and visual mode controls remain accessible

### Time-lapse

- **FR16:** System can automatically capture periodic snapshots during a tracing session
- **FR17:** System can compile snapshots into an MP4 video file
- **FR18:** System can save the time-lapse video to the device shared storage
- **FR19:** User can share the time-lapse video via system share intent

### User Onboarding

- **FR20:** First-time user can see a guided onboarding flow (3 screens)
- **FR21:** User can skip the onboarding at any point
- **FR22:** User can re-access the onboarding from app settings
- **FR23:** Onboarding presents setup tier options (Full DIY, Semi-equipped, Full kit)

### Setup Assistance

- **FR24:** User can access a DIY marker drawing guide within the app
- **FR25:** User can access a MacGyver setup guide (improvised phone stands)
- **FR26:** User can access a link to download printable marker sheets
- **FR27:** User can access a link to download 3D printable phone stand models

### Session Persistence

- **FR28:** User can pause a tracing session and resume it later
- **FR29:** User can pause and resume time-lapse recording independently
- **FR30:** System can automatically save session state when app goes to background (phone call, app switch)
- **FR31:** System can restore full session state when app returns to foreground (overlay position, opacity, image, time-lapse progress)
- **FR31a:** On cold start (process death), the system presents a dialog offering to resume the previous session; if accepted, settings are restored and the user re-selects the reference image via Photo Picker (temporary URI permissions are lost on process death)
- **FR32:** System can preserve time-lapse snapshots captured before an interruption

### User Controls & Settings

- **FR33:** User can control overlay opacity
- **FR34:** User can toggle control visibility
- **FR35:** User can access app settings (re-open onboarding, about, licenses)
- **FR36:** User can start and stop a tracing session
- **FR37:** System can play a short audio cue when marker tracking state changes (lost → untracked, recovered → tracked)
- **FR38:** User can enable/disable audio feedback for tracking state changes (off by default)
- **FR39:** System can remind the user to take a break after a configurable duration of continuous tracing (default interval: 30 minutes)
- **FR40:** User can enable/disable and configure break reminder interval (off by default)

## Non-Functional Requirements

### Performance

- **NFR1:** Camera preview + overlay rendering ≥ 20 fps on devices with Snapdragon 835 or equivalent
- **NFR2:** Marker detection latency < 50ms per frame
- **NFR3:** App cold start to camera-ready < 3 seconds
- **NFR4:** Overlay opacity adjustment responds within 1 frame (no perceptible lag)
- **NFR5:** Time-lapse MP4 compilation completes within 2x the session duration

### Reliability

- **NFR6:** App must not crash during a continuous tracing session of up to 60 minutes
- **NFR7:** Temporary loss of marker detection (hand occlusion) must recover within 500ms without user intervention
- **NFR8:** Time-lapse data must survive app backgrounding and return to foreground
- **NFR9:** No data loss if battery dies during session — last captured snapshots are preserved

### Privacy

- **NFR10:** Zero network calls — app must function with no INTERNET permission in manifest
- **NFR11:** No user data collection, no analytics, no telemetry
- **NFR12:** Imported images are processed in memory only — no copies stored by the app
- **NFR13:** Time-lapse videos saved only where user expects (shared Movies/ directory)

### Accessibility

- **NFR14:** All interactive elements have content descriptions for screen readers
- **NFR15:** Text elements support system font scaling
- **NFR16:** Touch targets ≥ 48dp per Material Design guidelines
- **NFR17:** Sufficient color contrast (WCAG AA) for all UI elements

### Development Process — Strict TDD Discipline

- **NFR18:** Test-Driven Development (TDD) — tests are written BEFORE implementation code for all business logic
- **NFR19:** Unit test coverage ≥ 80% for non-UI code (CV pipeline, time-lapse logic, marker detection, session persistence)
- **NFR20:** Instrumented tests for critical user flows (onboarding, import, overlay display, session pause/resume)
- **NFR21:** All code must pass CI checks before merge (lint, tests, build)
- **NFR22:** A test may only be in FAILED state when it was written before its corresponding implementation (red phase of TDD). In all other situations, all tests must pass.
- **NFR23:** Tests must never be marked as skipped, disabled, or ignored to bypass failures
- **NFR24:** Every test must validate meaningful behavior — no trivial assertions (e.g., testing if true == true). Each test must cover a real use case or edge case.
- **NFR25:** Test code is held to the same quality standard as production code — readable, maintainable, no duplication

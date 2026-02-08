# TraceGlass

[![CI](https://github.com/JN0V/TraceGlass/actions/workflows/ci.yml/badge.svg)](https://github.com/JN0V/TraceGlass/actions/workflows/ci.yml)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL%203.0-blue.svg)](LICENSE)
[![Android API](https://img.shields.io/badge/API-33%2B-green.svg)](https://developer.android.com/about/versions/13)

A free, open-source Android app that overlays a semi-transparent reference image on the live camera feed, enabling users to trace drawings on paper in augmented reality.

**100% offline. No ads. No trackers. No accounts. F-Droid exclusive.**

## Features

- **Live camera overlay** — Import any image and display it semi-transparently over the camera feed
- **Opacity control** — Adjustable slider with real-time preview
- **Visual modes** — Color tint filters (Red, Green, Blue, Grayscale) and inverted transparency
- **Drag & pinch** — Reposition and resize the overlay with gestures
- **Flashlight toggle** — Built-in torch control for low-light tracing
- **Session mode** — Keep screen on while tracing, hide UI controls for minimal interference

## Build

```bash
# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run lint
./gradlew lint

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Requirements:**
- JDK 17+
- Android SDK (API 36, Build Tools 34.0.0)

## Architecture

MVVM with Jetpack Compose, CameraX, and Koin DI. The project is organized into 8 focused modules:

| Module | Responsibility |
|--------|---------------|
| `:app` | Application entry point, DI wiring |
| `:core:camera` | CameraX preview and flashlight control |
| `:core:cv` | OpenCV JNI wrapper (Epic 3) |
| `:core:overlay` | Overlay positioning and transforms |
| `:core:session` | Session persistence with DataStore |
| `:feature:tracing` | Main tracing screen, ViewModel, UI |
| `:feature:onboarding` | First-launch onboarding flow |
| `:feature:timelapse` | Time-lapse capture and export |

For detailed planning documents, see:
- [Product Requirements](_bmad-output/planning-artifacts/prd.md)
- [Architecture Decisions](_bmad-output/planning-artifacts/architecture.md)
- [UX Design Specification](_bmad-output/planning-artifacts/ux-design-specification.md)
- [Epics & Stories](_bmad-output/planning-artifacts/epics.md)

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Camera:** CameraX
- **DI:** Koin
- **Image loading:** Coil
- **State management:** StateFlow + MVVM
- **Min SDK:** 33 (Android 13)
- **Target SDK:** 36

## Roadmap

- [x] **Epic 1** — Project foundation, CI, camera feed, flashlight
- [x] **Epic 2** — Image overlay, opacity, visual modes, gestures, sessions
- [ ] **Epic 3** — Fiducial marker tracking (OpenCV)
- [ ] **Epic 4** — Time-lapse capture & sharing
- [ ] **Epic 5** — Session persistence
- [ ] **Epic 6** — Onboarding & setup guides
- [ ] **Epic 7** — Settings & comfort features

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

Copyright (C) 2026 JN0V

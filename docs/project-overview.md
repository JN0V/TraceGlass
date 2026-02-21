# TraceGlass — Project Overview

> Generated: 2026-02-21 | Scan Level: Deep | Workflow: document-project v1.2.0

## What is TraceGlass?

TraceGlass is an Android app that turns a phone into a camera lucida — a drawing aid that overlays a reference image on the camera feed so the user can trace it onto physical paper. It uses ArUco markers placed at the paper corners for real-time tracking and perspective correction.

## Project Classification

| Property | Value |
|----------|-------|
| **Repository Type** | Monorepo (Android multi-module) |
| **Project Type** | Mobile (native Android) |
| **Primary Language** | Kotlin 2.1.0 |
| **Secondary Language** | C++17 (JNI/OpenCV) |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Build System** | Gradle 8.12, AGP 8.8.2 |
| **Min SDK** | 33 (Android 13) |
| **Target SDK** | 36 (Android 16) |
| **License** | GPL v3 |

## Module Architecture

```
app                         ← Application shell (navigation, DI bootstrap)
├── core:camera             ← CameraX abstraction (preview, zoom, flashlight)
├── core:cv                 ← OpenCV JNI bridge (ArUco marker detection)
├── core:overlay            ← Overlay math (homography, tracking, smoothing)
├── core:session            ← DataStore persistence (session + settings)
├── core:timelapse          ← Timelapse interfaces (storage, compiler, exporter)
├── feature:tracing         ← Main tracing screen (ViewModel, Compose UI)
├── feature:onboarding      ← First-time setup (carousel, walkthrough, guides)
└── feature:timelapse       ← Timelapse implementations (MediaCodec, MediaStore)
```

**9 modules total**: 1 app shell + 5 core libraries + 3 feature modules

## Key Features

1. **Live Camera Overlay** — Reference image rendered over camera preview with adjustable opacity
2. **ArUco Marker Tracking** — 4 markers at paper corners for automatic overlay positioning
3. **Perspective Correction** — Homography-based transform corrects for phone tilt angles
4. **Progressive Degradation** — Graceful fallback from 4→3→2→1→0 visible markers
5. **Visual Modes** — Color tints (red/green/blue/grayscale), inverted mode
6. **Overlay Lock + Viewport Zoom** — Lock overlay position, then zoom/pan the viewport
7. **Timelapse Capture** — Periodic snapshot → H.264 MP4 compilation → Gallery export/share
8. **Session Persistence** — Auto-save/restore via DataStore (survives app restart)
9. **Break Reminders** — Configurable timer with notification tone
10. **Onboarding** — 3-page carousel + interactive camera walkthrough + setup guides

## Technology Stack Summary

| Category | Technology | Version |
|----------|-----------|---------|
| Language | Kotlin | 2.1.0 |
| Native | C++17 via NDK | 27.2.12479018 |
| UI | Jetpack Compose + Material 3 | BOM 2026.02.00 |
| Navigation | Navigation Compose | 2.8.5 |
| DI | Koin | 4.0.2 |
| Camera | CameraX | 1.4.1 |
| CV | OpenCV Android SDK | 4.10.0 |
| Persistence | DataStore Preferences | 1.1.1 |
| Image Loading | Coil | 3.0.4 |
| Testing | JUnit 5, MockK, Coroutines Test | 5.11.4 / 1.13.14 |
| CI | GitHub Actions | v4 |

## Target Devices

| Device | OS | SoC | Notes |
|--------|----|-----|-------|
| Pixel 9 Pro | Android 16 | Tensor G4 | Multi-camera, ultra-wide via minZoomRatio |
| OnePlus 5T | Android 15 (LineageOS) | Snapdragon 835 | Single focal length (4.103mm) |

## Documentation Map

- [Architecture](./architecture.md) — Module design, data flows, algorithms
- [Source Tree](./source-tree-analysis.md) — Annotated directory structure
- [Component Inventory](./component-inventory.md) — All classes, interfaces, composables
- [Development Guide](./development-guide.md) — Build, test, deploy instructions

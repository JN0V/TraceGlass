# TraceGlass — Documentation Index

> Generated: 2026-02-21 | Scan: Deep | Workflow: document-project v1.2.0

## Project Overview

- **Type**: Monorepo (Android multi-module, 9 modules)
- **Primary Language**: Kotlin 2.1.0
- **Architecture**: MVVM + Clean Architecture (interface-based DI via Koin)
- **UI**: Jetpack Compose + Material 3
- **Native**: C++17 JNI (OpenCV ArUco marker detection)

## Quick Reference

- **App ID**: `io.github.jn0v.traceglass`
- **Min SDK**: 33 (Android 13)
- **Target SDK**: 36 (Android 16)
- **Build**: Gradle 8.12, AGP 8.8.2
- **Entry Point**: `app/.../MainActivity.kt` → Compose NavHost
- **DI Bootstrap**: `app/.../TraceGlassApp.kt` → 6 Koin modules
- **Test Runner**: JUnit 5 (Jupiter) on all modules

## Generated Documentation

- [Project Overview](./project-overview.md) — What TraceGlass is, features, tech stack summary
- [Architecture](./architecture.md) — Module design, data flows, algorithms, coordinate spaces
- [Source Tree Analysis](./source-tree-analysis.md) — Annotated directory structure with purpose
- [Component Inventory](./component-inventory.md) — All interfaces, classes, ViewModels, composables
- [Development Guide](./development-guide.md) — Build, test, deploy, known gotchas

## Existing Documentation

- [README.md](../README.md) — Project overview, badges, roadmap
- [ArUco Markers PDF](./aruco-markers-a4.pdf) — Printable test markers (DICT_4X4_50, IDs 0-3)
- [Marker Generator](./generate-markers.py) — Python script for custom marker PDFs

## Planning Artifacts

- [Product Requirements Document](../_bmad-output/planning-artifacts/prd.md)
- [Architecture Decisions](../_bmad-output/planning-artifacts/architecture.md)
- [Epics & Stories](../_bmad-output/planning-artifacts/epics.md)
- [UX Design Specification](../_bmad-output/planning-artifacts/ux-design-specification.md)
- [Manual Test Plan](../_bmad-output/planning-artifacts/manual-test-plan.md)

## Implementation Tracking

- [Sprint Status](../_bmad-output/implementation-artifacts/sprint-status.yaml) — All 37 stories across 9 epics
- [Spec vs Reality Audit](../_bmad-output/implementation-artifacts/spec-vs-reality-audit.md) — Comprehensive comparison

## Getting Started

1. Clone the repo, ensure JDK 17 + NDK 27.2 are installed
2. Download OpenCV 4.10.0 Android SDK to `sdk/OpenCV-android-sdk/`
3. `./gradlew assembleDebug` to build
4. `./gradlew test` to run all unit tests
5. Print `docs/aruco-markers-a4.pdf` for marker testing
6. See [Development Guide](./development-guide.md) for detailed instructions

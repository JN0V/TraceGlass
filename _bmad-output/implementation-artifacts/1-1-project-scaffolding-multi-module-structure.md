# Story 1.1: Project Scaffolding & Multi-Module Structure

Status: done

## Story

As a developer,
I want a properly scaffolded Android project with 8-module Gradle structure,
so that all future development has a clean, consistent foundation.

## Acceptance Criteria

1. **Given** no existing project **When** the project is created from Empty Compose Activity template **Then** the following 8 modules exist and compile: `:app`, `:core:camera`, `:core:cv`, `:core:overlay`, `:core:session`, `:feature:tracing`, `:feature:onboarding`, `:feature:timelapse`
2. **Given** modules are created **When** `settings.gradle.kts` is configured **Then** all 8 modules are included
3. **Given** dependencies are needed **When** `libs.versions.toml` is configured **Then** it is the single source of truth for all dependency versions
4. **Given** SDK targets are defined **When** `build.gradle.kts` files are configured **Then** Min SDK is API 33, compileSdk is latest stable (36)
5. **Given** the project is initialized **When** the package name is set **Then** it is `io.github.jn0v.traceglass`
6. **Given** all modules exist **When** `./gradlew assembleDebug` is run **Then** the build succeeds
7. **Given** all modules exist **When** `./gradlew test` is run **Then** tests pass
8. **Given** architecture dependencies are defined **When** each module's `build.gradle.kts` is configured **Then** dependencies match the architecture doc's dependency graph

## Tasks / Subtasks

- [x] Task 1: Create project from Empty Compose Activity template (AC: #1, #5)
  - [x] 1.1: Initialize project with package `io.github.jn0v.traceglass`
  - [x] 1.2: Set compileSdk 36, minSdk 33, targetSdk 36
  - [x] 1.3: Configure Kotlin DSL (`build.gradle.kts`) for all build files
- [x] Task 2: Configure version catalog (AC: #3)
  - [x] 2.1: Create `gradle/libs.versions.toml` as single source of truth
  - [x] 2.2: Add all dependencies with pinned versions: AGP 8.8.2, Kotlin 2.1.0, Compose BOM 2024.12.01, CameraX 1.4.1, Coroutines 1.9.0, Koin 4.0.2, DataStore 1.1.1, JUnit 5.11.4, MockK 1.13.14, Coil 3.0.4
  - [x] 2.3: Pin NDK 27.2.12479018, CMake 3.31.5 (in `core/cv/build.gradle.kts`, not in version catalog — NDK/CMake versions cannot be managed via `libs.versions.toml`)
- [x] Task 3: Create 8-module Gradle structure (AC: #1, #2, #8)
  - [x] 3.1: Create `:core:camera` module with CameraX dependencies
  - [x] 3.2: Create `:core:cv` module with OpenCV + NDK/CMake configuration
  - [x] 3.3: Create `:core:overlay` module with Compose dependencies
  - [x] 3.4: Create `:core:session` module with DataStore dependencies
  - [x] 3.5: Create `:feature:tracing` module depending on all 4 core modules
  - [x] 3.6: Create `:feature:onboarding` module depending on `:core:session`
  - [x] 3.7: Create `:feature:timelapse` module depending on `:core:camera`, `:core:session`
  - [x] 3.8: Update `:app` module to depend on all 3 feature modules + `:core:camera`, `:core:cv`, `:core:session`
  - [x] 3.9: Update `settings.gradle.kts` to include all 8 modules
- [x] Task 4: Configure module dependency graph (AC: #8)
  - [x] 4.1: Core modules NEVER depend on feature modules (SOLID-D) — verified
  - [x] 4.2: Feature modules NEVER depend on each other — verified
  - [x] 4.3: `:core:cv` depends on `:core:camera` (frame data) — verified
  - [x] 4.4: `:core:overlay` depends on `:core:cv` (marker results) — verified
- [x] Task 5: Verify build and tests (AC: #6, #7)
  - [x] 5.1: `./gradlew assembleDebug` succeeds
  - [x] 5.2: `./gradlew test` passes

## Dev Notes

### Architecture Patterns & Constraints

- **Architecture Decision 2 (Module Structure):** 8 modules optimized for AI-assisted development and unit testability. Each module has a single, clear responsibility (SOLID-S). [Source: architecture.md#Decision 2]
- **Starter Template:** Empty Compose Activity — simplest starting point, all structure added manually. [Source: architecture.md#Starter Template Evaluation]
- **Dependency Inversion (SOLID-D):** High-level modules depend on abstractions. Core modules NEVER depend on feature modules. Feature modules NEVER depend on each other.

### Verified Module Dependency Graph

```
:app → :core:camera, :core:cv, :core:session, :feature:tracing, :feature:onboarding, :feature:timelapse
:feature:tracing → :core:camera, :core:cv, :core:overlay, :core:session
:feature:timelapse → :core:camera, :core:session
:feature:onboarding → :core:session
:core:cv → :core:camera
:core:overlay → :core:cv
:core:camera → (no internal deps)
:core:session → (no internal deps)
```

No violations of SOLID-D boundaries detected.

### Key Library Versions (from libs.versions.toml)

| Library | Version | Purpose |
|---------|---------|---------|
| AGP | 8.8.2 | Android Gradle Plugin |
| Kotlin | 2.1.0 | Language |
| Compose BOM | 2024.12.01 | UI framework |
| CameraX | 1.4.1 | Camera pipeline |
| Coroutines | 1.9.0 | Async/threading |
| Koin | 4.0.2 | Dependency injection |
| DataStore | 1.1.1 | Persistence |
| JUnit 5 | 5.11.4 | Testing |
| MockK | 1.13.14 | Mocking |
| NDK | 27.2.12479018 | Native development |
| CMake | 3.31.5 | Native build |
| Coil | 3.0.4 | Image loading |

### Project Structure Notes

- Package: `io.github.jn0v.traceglass`
- Each module follows pattern: `src/main/kotlin/io/github/jn0v/traceglass/{layer}/{module}/`
- Core modules have `impl/` subdirectory for concrete implementations
- Core modules have `di/` subdirectory for Koin module definitions
- All modules have mirrored test structures under `src/test/kotlin/`

### References

- [Source: architecture.md#Starter Template Evaluation]
- [Source: architecture.md#Decision 2] — Module structure decision
- [Source: architecture.md#Complete Project Directory Structure]
- [Source: architecture.md#Naming Conventions]
- [Source: prd.md#Mobile App Specific Requirements]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

- All 8 modules created and compiling
- Version catalog with 25+ library entries as single source of truth
- Module dependency graph enforced by Gradle — boundary violations cause compile errors
- `gradle.properties` configured with parallel builds, caching, and F-Droid PNG crunch settings
- ✅ Resolved review finding [MEDIUM]: Extracted compileSdk, minSdk, buildToolsVersion, compileOptions, kotlinOptions, and useJUnitPlatform to root `subprojects {}` block — removed 8-fold duplication across all module build files
- ✅ Resolved review finding [LOW]: Clarified Task 2.3 description — NDK/CMake versions are pinned in `core/cv/build.gradle.kts`, not in `libs.versions.toml` (not supported by version catalogs)

### Review Follow-ups (AI)

- [x] [AI-Review][MEDIUM] compileSdk, minSdk, buildToolsVersion duplicated across all 8 build.gradle.kts files — extract to convention plugin or root subprojects block [all build.gradle.kts]
- [x] [AI-Review][LOW] Task 2.3 scoping misleading — NDK/CMake versions listed under "Configure version catalog" but can't go in libs.versions.toml; clarify task description [story docs only]
- [x] [AI-Review][LOW] Deprecated API `compileSdkVersion(36)` used for application plugin in root subprojects block — use `ApplicationExtension` with `compileSdk = 36` for consistency with the library block [build.gradle.kts:22]
- [x] [AI-Review][LOW] Root build.gradle.kts uses AGP internal API `BaseAppModuleExtension` — replaced with public `ApplicationExtension` [build.gradle.kts:21]

### File List

- `settings.gradle.kts`
- `build.gradle.kts` (root) — added `subprojects {}` block for centralized Android config
- `gradle/libs.versions.toml`
- `gradle.properties`
- `app/build.gradle.kts` — removed duplicated compileSdk/minSdk/compileOptions/kotlinOptions/useJUnitPlatform
- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/io/github/jn0v/traceglass/TraceGlassApp.kt`
- `app/src/main/kotlin/io/github/jn0v/traceglass/MainActivity.kt`
- `core/camera/build.gradle.kts` — removed duplicated settings (centralized to root)
- `core/cv/build.gradle.kts` — removed duplicated settings (kept NDK/CMake-specific config)
- `core/overlay/build.gradle.kts` — removed duplicated settings (kept compose buildFeature)
- `core/session/build.gradle.kts` — removed duplicated settings (centralized to root)
- `feature/tracing/build.gradle.kts` — removed duplicated settings (kept compose buildFeature)
- `feature/onboarding/build.gradle.kts` — removed duplicated settings (kept compose buildFeature)
- `feature/timelapse/build.gradle.kts` — removed duplicated settings (centralized to root)

### Change Log

- 2026-02-17: Addressed code review findings — 2 items resolved (centralized build config to root subprojects block, clarified Task 2.3 NDK/CMake scope)
- 2026-02-17: Code review #3 — 2 LOW findings fixed: (1) Marked stale review follow-up checkbox as resolved. (2) Replaced AGP internal API `BaseAppModuleExtension` with public `ApplicationExtension` in root build.gradle.kts.

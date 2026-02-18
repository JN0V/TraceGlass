# Story 1.3: GitHub Actions CI Pipeline

Status: done

## Story

As a developer,
I want a CI pipeline on GitHub Actions that mirrors the F-Droid build chain,
so that if CI passes, I am confident the F-Droid build will also pass.

## Acceptance Criteria

1. **Given** the project with F-Droid config from Story 1.2 **When** a push or pull request is made to the main branch **Then** GitHub Actions runs a workflow that builds the project (`assembleDebug` + `assembleRelease`), runs all unit tests (`test`), and runs lint
2. **Given** the CI environment **When** JDK is configured **Then** the CI uses JDK 17 (same as F-Droid)
3. **Given** the CI environment **When** Android SDK is configured **Then** the CI pins the same Android SDK (36), build-tools (34.0.0), and NDK (27.2.12479018) versions as the project
4. **Given** the workflow is created **When** its location is checked **Then** it is at `.github/workflows/ci.yml`
5. **Given** a PR is opened **When** CI runs **Then** CI results are visible on pull requests
6. **Given** CI fails **When** the developer checks the PR **Then** a failing build blocks merge (branch protection configured in README instructions)

## Tasks / Subtasks

- [x] Task 1: Create CI workflow file (AC: #1, #4)
  - [x] 1.1: Created `.github/workflows/ci.yml`
  - [x] 1.2: Trigger on push and pull_request to `main` branch
  - [x] 1.3: `assembleDebug` step with `--no-daemon`
  - [x] 1.4: `assembleRelease` step with `--no-daemon`
  - [x] 1.5: `test` step (unit tests) with `--no-daemon`
  - [x] 1.6: `lint` step with `--no-daemon`
- [x] Task 2: Configure CI environment (AC: #2, #3)
  - [x] 2.1: `ubuntu-latest` runner
  - [x] 2.2: JDK 17 (Temurin distribution) via `actions/setup-java@v4`
  - [x] 2.3: Android SDK 36 via `android-actions/setup-android@v3`
  - [x] 2.4: Build-tools 34.0.0
  - [x] 2.5: NDK 27.2.12479018
  - [x] 2.6: CMake 3.31.5
- [x] Task 3: Configure OpenCV SDK in CI (AC: #1)
  - [x] 3.1: Download OpenCV 4.10.0 Android SDK via curl from GitHub releases
  - [x] 3.2: Cache OpenCV SDK with key `opencv-4.10.0-android-sdk` via `actions/cache@v4`
  - [x] 3.3: Installed to `sdk/OpenCV-android-sdk/` (matched by CMakeLists.txt)
- [x] Task 4: Configure caching for performance
  - [x] 4.1: Gradle packages cache (`~/.gradle/caches` + `~/.gradle/wrapper`)
  - [x] 4.2: Cache key based on hash of `*.gradle.kts`, `libs.versions.toml`, `gradle-wrapper.properties`
  - [x] 4.3: OpenCV SDK cache with static key
- [x] Task 5: Additional CI features
  - [x] 5.1: Concurrency control: `cancel-in-progress: true` (cancels stale runs on new push)
  - [x] 5.2: Lint results uploaded as artifacts (7-day retention) via `actions/upload-artifact@v4`

## Dev Notes

### Version Matrix (CI = Project)

| Component | CI Value | Project Value | Match |
|-----------|----------|---------------|-------|
| JDK | 17 (temurin) | 17 | yes |
| Android SDK | 36 | 36 | yes |
| Build Tools | 34.0.0 | 34.0.0 | yes |
| NDK | 27.2.12479018 | 27.2.12479018 | yes |
| CMake | 3.31.5 | 3.31.5 | yes |
| Gradle | 8.12 (wrapper) | 8.12 | yes |
| AGP | (implied) | 8.8.2 | yes |

### References

- [Source: architecture.md#Technical Constraints & Dependencies]
- [Source: architecture.md#Development Workflow]
- [Source: prd.md#NFR21] — All code must pass CI checks before merge

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

- CI pipeline fully operational, mirrors F-Droid build chain
- OpenCV SDK downloaded and cached for native builds
- Lint results archived as GitHub artifacts
- Concurrency control prevents wasted CI minutes
- ✅ Resolved review finding [MEDIUM]: Added branch protection setup instructions to README (AC #6)
- ✅ Resolved review finding [LOW]: Extracted OpenCV SDK version to `OPENCV_VERSION` env variable in ci.yml
- ✅ Resolved review finding [MEDIUM]: Updated README roadmap — Epics 3, 5, 7 marked done, Epic 8 added
- ✅ Resolved review finding [LOW]: Added Gradle Wrapper Validation step to CI
- ✅ Resolved review finding [LOW]: Added `timeout-minutes: 30` to CI build job

### Review Follow-ups (AI)

- [x] [AI-Review][MEDIUM] Branch protection setup instructions missing from README — AC #6 requires README to document how to enable "Require status checks to pass before merging" [README.md]
- [x] [AI-Review][LOW] OpenCV SDK URL hardcoded (v4.10.0) in CI — consider extracting to a CI variable [.github/workflows/ci.yml:52]
- [x] [AI-Review][MEDIUM] README roadmap stale — Epics 3, 5, 7 shown as `[ ]` (unchecked) but are done per sprint-status.yaml. Update to reflect actual completion [README.md:76-80]
- [x] [AI-Review][LOW] No Gradle Wrapper Validation in CI — added `gradle/actions/wrapper-validation@v4` step [.github/workflows/ci.yml]
- [x] [AI-Review][LOW] No job timeout in CI — added `timeout-minutes: 30` [.github/workflows/ci.yml:18]
- [ ] [AI-Review][LOW] No OpenCV SDK checksum verification — downloaded zip not verified against SHA256 hash [.github/workflows/ci.yml:53-57]

### File List

- `.github/workflows/ci.yml`
- `README.md`

### Change Log

- 2026-02-17: Code review #3 — 1 MEDIUM + 3 LOW findings. Fixed: (1) README roadmap synced with sprint-status (Epics 3,5,7 → done, Epic 8 added). (2) Added Gradle Wrapper Validation to CI. (3) Added 30-min timeout to CI job. 1 LOW deferred: OpenCV checksum verification. Status → done.

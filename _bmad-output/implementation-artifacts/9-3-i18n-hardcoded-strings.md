# Story 9.3: Internationalization of Hardcoded UI Strings

Status: ready-for-dev

## Story

As a user,
I want all user-facing text to be in string resources,
So that the app can be translated to other languages in the future.

## Acceptance Criteria

1. **Given** all user-facing strings in the tracing feature module **When** the strings are audited **Then** every hardcoded string is moved to `res/values/strings.xml` and accessed via `stringResource()`

2. **Given** the timelapse dialog strings ("Timelapse ready!", "Save to Gallery", "Discard", "Share", "Saving...") **When** they are displayed **Then** they use string resources, not hardcoded text

3. **Given** the unlock dialog strings ("Unlock overlay?", "Your current position will be adjustable again.", "Unlock", "Cancel") **When** they are displayed **Then** they use string resources

4. **Given** the tracking indicator text ("Tracking", "Lost") **When** it is displayed **Then** it uses string resources

5. **Given** all strings are extracted **When** `./gradlew test` runs **Then** all tests pass (update test assertions if they match on hardcoded text)

## Tasks / Subtasks

- [ ] Task 1: Audit all hardcoded strings in feature:tracing (AC: #1)
  - [ ] 1.1 TracingContent.kt — timelapse dialog (lines 241-279)
  - [ ] 1.2 TracingContent.kt — unlock dialog (lines 290-299)
  - [ ] 1.3 TracingContent.kt — "Grant Permission" button (line 650)
  - [ ] 1.4 VisualModeControls.kt — "Inverted" chip label (line 38)
  - [ ] 1.5 TrackingIndicator.kt — "Tracking" / "Lost" text (lines 56-58)
- [ ] Task 2: Create string resources in feature:tracing strings.xml (AC: #1-4)
  - [ ] 2.1 Add all strings to `feature/tracing/src/main/res/values/strings.xml`
  - [ ] 2.2 Use descriptive keys (e.g., `timelapse_dialog_title`, `unlock_dialog_message`)
  - [ ] 2.3 Replace all `Text("...")` with `Text(stringResource(R.string.xxx))`
- [ ] Task 3: Audit other modules for hardcoded strings
  - [ ] 3.1 IntentVideoSharer.kt — "Share time-lapse" chooser title
  - [ ] 3.2 Any other modules with user-facing text
- [ ] Task 4: Update tests if they assert on hardcoded text (AC: #5)
  - [ ] 4.1 Check androidTest files (ExpandableMenuTest, OpacityFabTest) for text matchers
  - [ ] 4.2 Update any `hasText("...")` assertions to use string resource references
- [ ] Task 5: Run full test suite, verify no regressions (AC: #5)

## Dev Notes

### Key Files to Modify

- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/TracingContent.kt` — bulk of hardcoded strings
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/components/VisualModeControls.kt`
- `feature/tracing/src/main/kotlin/io/github/jn0v/traceglass/feature/tracing/components/TrackingIndicator.kt`
- `feature/tracing/src/main/res/values/strings.xml` — add new string resources
- `feature/timelapse/src/main/kotlin/.../IntentVideoSharer.kt` — chooser title

### Architecture Constraints

- String resources must be in the module where they're used (`feature:tracing` has its own `res/values/strings.xml`)
- Use `stringResource(R.string.xxx)` in composables, NOT `context.getString()` (Compose idiomatic)
- Content descriptions for accessibility are already handled in some places — verify they also use string resources
- DO NOT add strings for developer-facing log messages (those stay hardcoded English)

### String Inventory (from adversarial review)

| Location | String | Proposed Key |
|----------|--------|-------------|
| TracingContent:241 | "Timelapse ready!" | `timelapse_dialog_title` |
| TracingContent:253 | "Saving..." | `timelapse_saving` |
| TracingContent:256 | "What would you like to do with your time-lapse video?" | `timelapse_dialog_message` |
| TracingContent:264 | "Save to Gallery" | `timelapse_save_to_gallery` |
| TracingContent:273 | "Discard" | `timelapse_discard` |
| TracingContent:279 | "Share" | `timelapse_share` |
| TracingContent:290 | "Unlock overlay?" | `unlock_dialog_title` |
| TracingContent:291 | "Your current position will be adjustable again." | `unlock_dialog_message` |
| TracingContent:294 | "Unlock" | `unlock_confirm` |
| TracingContent:299 | "Cancel" | `dialog_cancel` |
| TracingContent:650 | "Grant Permission" | `grant_permission` |
| VisualModeControls:38 | "Inverted" | `visual_mode_inverted` |
| TrackingIndicator:56 | "Tracking" | `tracking_status_active` |
| TrackingIndicator:58 | "Lost" | `tracking_status_lost` |
| IntentVideoSharer:19 | "Share time-lapse" | `share_chooser_title` |

### Previous Story Intelligence

From Story 6.2 and 7.1-7.2 adversarial reviews: i18n was already addressed in onboarding and settings modules. The pattern used was `stringResource(R.string.xxx)` in composables. Follow the same pattern here.

### References

- [Source: Adversarial review finding #19]
- [Source: feature/tracing/.../TracingContent.kt — lines 241-299, 650]
- [Source: feature/tracing/.../components/VisualModeControls.kt — line 38]
- [Source: feature/tracing/.../components/TrackingIndicator.kt — lines 56-58]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

# TraceGlass Manual Test Plan

**App:** TraceGlass (Android)
**Version:** Epics 1-7 (all implemented features)
**Date:** 2026-02-09

---

## Prerequisites

- Android device running API 33+ with a rear camera
- Device has a flashlight (for torch tests; note torch-less behavior separately)
- At least one image file accessible via the photo picker
- Two printed ArUco 4x4 markers (ID #0 and #1), approximately 2 cm each
- Flat drawing surface with markers taped at opposite corners
- Fresh install of the app (clear app data before starting Section 1)

---

## 1. First Launch & Onboarding

### TC-ONB-01: First launch shows onboarding carousel
**Description:** Verify that on a fresh install, the app navigates to the onboarding screen instead of the tracing screen.
**Steps:**
1. Clear app data or perform a fresh install.
2. Launch the app.
**Expected Result:** The onboarding screen appears showing the Welcome page with the text "Welcome to TraceGlass" and a description about turning your phone into a tracing lightbox.

### TC-ONB-02: Swipe through carousel pages
**Description:** Verify horizontal swiping navigates between the 3 onboarding slides.
**Steps:**
1. On the onboarding screen, swipe left.
2. Observe the second page.
3. Swipe left again.
4. Observe the third page.
5. Swipe right twice to return to the first page.
**Expected Result:** Three distinct pages are shown in order: Welcome, Tier Selection ("Choose Your Setup"), and Marker Preparation ("Prepare Your Markers"). Page indicator dots update to reflect the current page.

### TC-ONB-03: Tier selection -- Full DIY
**Description:** Verify selecting the Full DIY tier on page 2.
**Steps:**
1. Navigate to the second onboarding page (Tier Selection).
2. Tap the "Full DIY -- Print your own markers" chip.
3. Navigate to the third page.
**Expected Result:** The Full DIY chip becomes selected. On the Marker Preparation page, the text instructs the user to print two ArUco markers (ID #0 and #1) at about 2 cm and tape them at opposite corners.

### TC-ONB-04: Tier selection -- Semi-equipped
**Description:** Verify selecting the Semi-equipped tier changes page 3 content.
**Steps:**
1. Navigate to page 2 and tap "Semi-equipped -- Printed markers".
2. Navigate to page 3.
**Expected Result:** Page 3 text mentions printing the ArUco marker sheet from the setup guide, cutting out markers #0 and #1, and taping at opposite corners.

### TC-ONB-05: Tier selection -- Full kit
**Description:** Verify selecting the Full kit tier changes page 3 content.
**Steps:**
1. Navigate to page 2 and tap "Full kit -- Printed markers + stand".
2. Navigate to page 3.
**Expected Result:** Page 3 text mentions printing the marker sheet, setting up the phone stand, taping markers, and mounting the phone above.

### TC-ONB-06: Next button and Get Started
**Description:** Verify the Next button advances through pages, then becomes "Get Started" on the last page.
**Steps:**
1. On page 0 (Welcome), tap "Next".
2. On page 1 (Tier Selection), tap "Next".
3. On page 2 (Marker Preparation), observe the button text.
4. Tap "Get Started".
**Expected Result:** Each tap of "Next" advances to the next page. On page 2, the button reads "Get Started". Tapping it completes onboarding and navigates to the tracing screen.

### TC-ONB-07: Skip button completes onboarding
**Description:** Verify the Skip button exits onboarding immediately.
**Steps:**
1. On any onboarding page, tap "Skip" (top-right).
**Expected Result:** Onboarding completes and the app navigates to the tracing screen. Will not show on subsequent launches.

### TC-ONB-08: Onboarding completion is persisted
**Description:** Verify subsequent launches skip onboarding.
**Steps:**
1. Complete onboarding (via "Get Started" or "Skip").
2. Force-stop the app.
3. Relaunch the app.
**Expected Result:** The app launches directly to the tracing screen, not the onboarding screen.

### TC-ONB-09: Setup guide -- Marker Guide content
**Description:** Verify the Marker Guide section displays correct ArUco instructions.
**Steps:**
1. Navigate to onboarding page 3 and tap "View detailed setup guide".
2. Ensure the "Marker Guide" chip is selected.
**Expected Result:** Three step cards: (1) Print or display markers -- ArUco 4x4 at ~2 cm, (2) Place at opposite corners, (3) Keep markers visible.

### TC-ONB-10: Setup guide -- Stand Setup content
**Description:** Verify the Stand Setup section displays improvised phone stand options.
**Steps:**
1. On the Setup Guide screen, tap the "Stand Setup" chip.
**Expected Result:** Shows "Improvised Phone Stand" with three cards: Glass of Water, Stack of Books, Box or Container.

### TC-ONB-11: Setup guide -- Back navigation
**Description:** Verify the back arrow returns to the onboarding carousel.
**Steps:**
1. From the Setup Guide screen, tap the back arrow.
**Expected Result:** Returns to the onboarding carousel at the page from which the guide was opened.

---

## 2. Camera & Permissions

### TC-CAM-01: Camera permission requested on first launch
**Description:** Verify the camera permission is requested when the tracing screen is shown.
**Steps:**
1. Complete onboarding.
2. Observe the system permission dialog.
**Expected Result:** A system dialog appears requesting camera permission.

### TC-CAM-02: Camera preview shown after granting permission
**Description:** Verify granting camera permission shows the live camera preview.
**Steps:**
1. On the permission dialog, tap "Allow" / "While using the app".
**Expected Result:** Camera preview fills the screen. Controls appear: Settings (top-left), tracking indicator (top-right), flashlight (bottom-left), image picker FAB (bottom-right).

### TC-CAM-03: Permission denied -- denial screen shown
**Description:** Verify denying camera permission shows the denial screen.
**Steps:**
1. On the permission dialog, tap "Don't allow".
**Expected Result:** Screen shows "Camera permission required" with explanation and a "Grant Permission" button.

### TC-CAM-04: Retry permission from denial screen
**Description:** Verify the "Grant Permission" button re-requests permission.
**Steps:**
1. On the denial screen, tap "Grant Permission".
**Expected Result:** System permission dialog reappears.

### TC-CAM-05: Flashlight toggle on/off
**Description:** Verify the flashlight toggle works.
**Steps:**
1. Tap the flashlight FAB (bottom-left, outlined star).
2. Tap again.
**Expected Result:** First tap: flashlight turns on, icon becomes filled star. Second tap: flashlight off, icon reverts to outlined star.

### TC-CAM-06: Flashlight hidden on devices without flash
**Description:** Verify the flashlight button is hidden on devices without a flashlight.
**Steps:**
1. Run on a device/emulator without a flashlight.
**Expected Result:** No flashlight FAB is rendered.

---

## 3. Image Import & Overlay Display

### TC-IMG-01: Open photo picker
**Description:** Verify tapping the image picker FAB opens the system photo picker.
**Steps:**
1. On the tracing screen, tap the "+" FAB (bottom-right).
**Expected Result:** System photo picker opens, filtered to images only.

### TC-IMG-02: Select an image
**Description:** Verify picking an image displays it as an overlay.
**Steps:**
1. Tap "+" FAB, select any image.
**Expected Result:** Image appears overlaid on camera preview at 50% default opacity with ContentScale.Fit. Additional controls appear: opacity FAB, visual mode controls, Start/Stop button.

### TC-IMG-03: Cancel photo picker
**Description:** Verify cancelling the picker does not change state.
**Steps:**
1. Tap "+" FAB, dismiss picker without selecting.
**Expected Result:** Picker closes. No overlay change.

### TC-IMG-04: Replace overlay image
**Description:** Verify picking a new image replaces the current overlay.
**Steps:**
1. Select an image. Then tap "+" again and select a different image.
**Expected Result:** New image replaces old one. Opacity retains its current value.

---

## 4. Overlay Positioning (Manual)

### TC-POS-01: Drag overlay to reposition
**Description:** Verify dragging pans the overlay image.
**Steps:**
1. With overlay displayed, drag one finger in any direction.
**Expected Result:** Overlay moves in the drag direction. Camera preview stays stationary.

### TC-POS-02: Pinch to zoom overlay
**Description:** Verify pinch gesture scales the overlay.
**Steps:**
1. With overlay displayed, pinch out (enlarge) then pinch in (shrink).
**Expected Result:** Overlay scales proportionally. Scale multiplies cumulatively.

### TC-POS-03: Combined drag and pinch
**Description:** Verify simultaneous drag and pinch.
**Steps:**
1. Use two fingers to simultaneously drag and pinch.
**Expected Result:** Overlay moves and scales simultaneously.

---

## 5. Visual Modes (Opacity, Color Tint, Inverted)

### TC-VIS-01: Default opacity is 50%
**Description:** Verify the default overlay opacity.
**Steps:**
1. Load an overlay image. Check opacity FAB.
**Expected Result:** FAB displays "50%".

### TC-VIS-02: Toggle opacity slider visibility
**Description:** Verify tapping the opacity FAB shows/hides the slider.
**Steps:**
1. Tap the opacity FAB.
**Expected Result:** Vertical slider animates in. Auto-hides after ~3 seconds of inactivity.

### TC-VIS-03: Adjust opacity 0% to 100%
**Description:** Verify the slider changes overlay opacity across full range.
**Steps:**
1. Drag slider to 0%. Observe overlay.
2. Drag slider to 100%. Observe overlay.
**Expected Result:** At 0%: invisible. At 100%: fully opaque. FAB label updates in real time.

### TC-VIS-04: Color tint modes
**Description:** Verify each color tint mode.
**Steps:**
1. With overlay loaded, tap each filter chip: None, Red, Green, Blue, Gray.
**Expected Result:** Each tint applies immediately. Only one chip selected at a time. "None" restores original colors. "Gray" makes grayscale.

### TC-VIS-05: Inverted mode toggle
**Description:** Verify inverted mode inverts the effective opacity.
**Steps:**
1. Set opacity to 30%.
2. Tap "Inverted" chip.
**Expected Result:** Visual opacity flips to 70% (1 - 0.3). FAB still reads "30%".

### TC-VIS-06: Controls hidden without overlay
**Description:** Verify visual mode controls and opacity FAB are hidden when no image is loaded.
**Steps:**
1. On tracing screen with no overlay, check top-left and center-right areas.
**Expected Result:** No color tint chips, inverted chip, or opacity FAB visible.

---

## 6. ArUco Marker Tracking

### TC-TRK-01: Tracking indicator -- INACTIVE (default)
**Description:** Verify no tracking indicator when markers never detected.
**Steps:**
1. Camera preview visible, no ArUco markers in view.
**Expected Result:** No tracking indicator badge visible (INACTIVE state).

### TC-TRK-02: Tracking indicator -- TRACKING
**Description:** Verify green "Tracking" when markers are detected.
**Steps:**
1. Place two ArUco 4x4 markers (ID #0 and #1) in camera view.
**Expected Result:** Green circle dot + "Tracking" text badge appears top-right.

### TC-TRK-03: Tracking indicator -- LOST
**Description:** Verify orange "Lost" when markers disappear.
**Steps:**
1. With tracking active, remove markers from view.
2. Wait ~500 ms.
**Expected Result:** Badge transitions to orange triangle + "Lost".

### TC-TRK-04: Grace period before LOST
**Description:** Verify brief occlusion doesn't trigger LOST.
**Steps:**
1. With tracking active, briefly cover markers for <500 ms, then reveal.
**Expected Result:** Indicator stays "Tracking" without flickering.

### TC-TRK-05: Overlay offset follows marker center
**Description:** Verify overlay position tracks marker midpoint.
**Steps:**
1. Load overlay, place both markers in view.
2. Slowly move markers (or phone) to the left.
**Expected Result:** Overlay shifts to follow marker positions.

### TC-TRK-06: Scale from marker spacing
**Description:** Verify overlay scales when markers move closer/farther.
**Steps:**
1. With overlay and both markers tracked, move phone closer (markers appear farther apart in frame).
2. Move phone farther away (markers appear closer).
**Expected Result:** Overlay scales up when markers are farther apart, scales down when closer. Scale is relative to first detection.

### TC-TRK-07: Rotation from marker angle
**Description:** Verify overlay rotates when the angle between markers changes.
**Steps:**
1. With both markers tracked, rotate the drawing surface ~45 degrees.
**Expected Result:** Overlay rotates to match. Clockwise physical rotation = clockwise overlay rotation.

### TC-TRK-08: Single marker -- no scale or rotation
**Description:** Verify behavior with only one marker.
**Steps:**
1. Place only marker #0 in view (hide #1).
**Expected Result:** Tracking shows green. Overlay follows single marker position. Scale stays 1.0, rotation stays 0.

---

## 7. Session Controls & Persistence

### TC-SES-01: Start/Stop button with overlay
**Description:** Verify session button appears only with overlay loaded.
**Steps:**
1. No overlay: check bottom-center. Load overlay: check again.
**Expected Result:** No button without overlay. "Start" FAB appears after loading overlay.

### TC-SES-02: Start session
**Description:** Verify tapping "Start" activates the session.
**Steps:**
1. Tap "Start" FAB.
**Expected Result:** Button changes to "Stop" in red. Session active.

### TC-SES-03: Keep screen on during active session
**Description:** Verify screen stays on during active session.
**Steps:**
1. Start session. Set device timeout to minimum. Wait.
**Expected Result:** Screen stays on while session is active.

### TC-SES-04: Stop session
**Description:** Verify tapping "Stop" deactivates the session.
**Steps:**
1. With active session, tap "Stop".
**Expected Result:** Button returns to "Start" in primary color. Session inactive.

### TC-SES-05: Hide/show controls
**Description:** Verify tap to hide/show all controls.
**Steps:**
1. Tap main screen area to hide controls.
2. Tap again to show.
**Expected Result:** All controls disappear/reappear on tap.

### TC-SES-06: Session persistence across restart
**Description:** Verify session state survives app restart.
**Steps:**
1. Load image, set opacity 70%, Blue tint, Inverted mode, drag to custom position.
2. Start then Stop session.
3. Force-stop and relaunch.
**Expected Result:** All settings restored: same image, 70% opacity, Blue tint, Inverted mode, custom position.

### TC-SES-07: Auto-save on background
**Description:** Verify session saves when app goes to background.
**Steps:**
1. Set up session. Press Home.
2. Force-stop. Relaunch.
**Expected Result:** Session state restored.

### TC-SES-08: No restore without previous session
**Description:** Verify no stale data on fresh launch.
**Steps:**
1. Clear all app data. Complete onboarding.
**Expected Result:** Default state: no overlay, 50% opacity, no tint, no inverted mode.

---

## 8. Settings

### TC-SET-01: Navigate to Settings and back
**Description:** Verify Settings navigation.
**Steps:**
1. Tap gear icon FAB (top-left).
2. On Settings, tap back arrow.
**Expected Result:** Settings opens with top bar. Back returns to tracing screen.

### TC-SET-02: Audio feedback toggle
**Description:** Verify audio toggle defaults to off and persists.
**Steps:**
1. Open Settings. Check Audio feedback switch (should be OFF).
2. Toggle ON. Leave and return to Settings.
**Expected Result:** Switch persists in ON position.

### TC-SET-03: Break reminder toggle reveals slider
**Description:** Verify enabling break reminders shows interval slider.
**Steps:**
1. Toggle Break reminders ON.
**Expected Result:** Slider animates in showing "Reminder interval: 30 min" (default). Range: 5-60 min.

### TC-SET-04: Break reminder interval slider
**Description:** Verify slider adjusts interval.
**Steps:**
1. With break reminders ON, drag slider to 5 min, then to 60 min.
**Expected Result:** Label updates in real time. Value persists.

### TC-SET-05: Disable break reminders hides slider
**Description:** Verify disabling hides the slider.
**Steps:**
1. Toggle Break reminders OFF.
**Expected Result:** Slider animates out.

### TC-SET-06: Re-open onboarding
**Description:** Verify re-opening onboarding from Settings.
**Steps:**
1. Tap "Re-open onboarding".
2. Complete it.
3. Force-stop and relaunch.
**Expected Result:** Onboarding opens in REOPENED mode. After completion, returns to previous screen. App still starts at tracing on next launch (completion not re-persisted).

### TC-SET-07: About link
**Description:** Verify About navigation.
**Steps:**
1. Tap "About".
**Expected Result:** About screen opens.

### TC-SET-08: Settings persistence across restart
**Description:** Verify all settings persist.
**Steps:**
1. Enable audio, enable break reminders, set 15 min.
2. Force-stop and relaunch. Open Settings.
**Expected Result:** Audio ON, break reminders ON, interval 15 min.

---

## 9. Break Reminders

### TC-BRK-01: Break reminder fires after interval
**Description:** Verify reminder appears after configured interval during active session.
**Steps:**
1. Enable break reminders, set to 5 min (minimum).
2. Start session. Wait 5 minutes.
**Expected Result:** Snackbar appears: "Time for a break!"

### TC-BRK-02: Audio tone with reminder (audio enabled)
**Description:** Verify notification tone plays when audio feedback is enabled.
**Steps:**
1. Enable both audio feedback and break reminders at 5 min.
2. Start session, wait for reminder.
**Expected Result:** Snackbar + notification ringtone plays.

### TC-BRK-03: No audio when audio disabled
**Description:** Verify no sound when audio feedback off.
**Steps:**
1. Disable audio feedback, enable break reminders.
2. Start session, wait for reminder.
**Expected Result:** Snackbar appears but no sound.

### TC-BRK-04: Timer resets on dismiss
**Description:** Verify timer restarts after dismissing reminder.
**Steps:**
1. Enable reminders at 5 min. Start session. Wait for reminder.
2. After Snackbar dismisses, wait another 5 min.
**Expected Result:** Second reminder appears.

### TC-BRK-05: Timer stops when session stops
**Description:** Verify no reminder after stopping session.
**Steps:**
1. Enable reminders at 5 min. Start session.
2. After 2 min, stop session. Wait 5 more min.
**Expected Result:** No reminder appears.

### TC-BRK-06: No reminder when disabled or session inactive
**Description:** Verify no reminder when conditions aren't met.
**Steps:**
1. Disable reminders, start session. Wait.
2. Enable reminders, don't start session. Wait.
**Expected Result:** No reminder in either case.

---

## 10. About Screen

### TC-ABT-01: App info displayed
**Description:** Verify About screen shows correct info.
**Steps:**
1. Settings > About.
**Expected Result:** Shows "TraceGlass", "Version 0.1.0 (build 1)", "GNU General Public License v3.0", third-party licenses (OpenCV, Coil, Koin, Jetpack -- all Apache 2.0).

### TC-ABT-02: GitHub link opens browser
**Description:** Verify GitHub button works.
**Steps:**
1. Tap "View on GitHub (opens browser)".
**Expected Result:** Browser opens to https://github.com/jn0v/traceglass.

### TC-ABT-03: Back navigation
**Description:** Verify back arrow returns to Settings.
**Steps:**
1. Tap back arrow on About screen.
**Expected Result:** Returns to Settings.

---

## 11. App Icon

### TC-ICN-01: Adaptive icon renders correctly
**Description:** Verify the app icon in launcher.
**Steps:**
1. Find TraceGlass icon on home screen or app drawer.
**Expected Result:** Teal glass lens + pencil trace foreground on light teal background. Adaptive icon shapes (circle, squircle, etc.) display correctly.

---

## 12. Edge Cases & Error Handling

### TC-EDGE-01: App killed while session active
**Description:** Verify restore after process death.
**Steps:**
1. Start session with custom settings. Press Home (triggers save). Force-stop. Relaunch.
**Expected Result:** Session restored with all saved state.

### TC-EDGE-02: Rapidly toggling torch
**Description:** Verify rapid flashlight toggling doesn't crash.
**Steps:**
1. Tap flashlight toggle 10+ times rapidly.
**Expected Result:** Toggles cleanly without crash. Icon reflects current state.

### TC-EDGE-03: Very large image as overlay
**Description:** Verify large images don't crash the app.
**Steps:**
1. Select a very large image (e.g., 8000x6000) from gallery.
**Expected Result:** Image loads as overlay. May have brief delay but no crash.

### TC-EDGE-04: Markers partially in frame
**Description:** Verify behavior with markers at frame edge.
**Steps:**
1. Position markers barely within camera frame. Slowly move one out.
**Expected Result:** Tracking may become intermittent. When marker exits frame, tracker reports fewer markers.

### TC-EDGE-05: No markers -- overlay remains manual
**Description:** Verify manual-only mode without markers.
**Steps:**
1. Load overlay without any markers in view. Drag and pinch manually.
**Expected Result:** Tracking indicator stays INACTIVE. Overlay fully controllable via gestures.

### TC-EDGE-06: Background then foreground -- camera rebinds
**Description:** Verify camera reconnects after backgrounding.
**Steps:**
1. Verify camera preview active. Press Home. Return to app.
**Expected Result:** Camera preview resumes. No black screen.

### TC-EDGE-07: Session restore with deleted image
**Description:** Verify app handles restored session with deleted source image.
**Steps:**
1. Load image, save session. Delete image from gallery. Force-stop, relaunch.
**Expected Result:** Session data restored but overlay fails to load silently. No crash.

### TC-EDGE-08: Settings rapid toggling
**Description:** Verify rapid settings changes don't cause race conditions.
**Steps:**
1. Rapidly toggle Audio and Break reminders on/off. Rapidly drag interval slider.
**Expected Result:** Consistent final state matching last action. No crash.

---

## Test Execution Notes

- **Fresh install required** for Sections 1 and 2. Clear app data before starting.
- **Marker printing:** ArUco 4x4 markers ID #0 and #1 at ~2 cm. Download from Setup Guide external link.
- **Timing tests:** Break reminder tests (Section 9) require real-time waits. Use 5 min minimum.
- **Multi-device:** Test on at least 2 devices with different screen sizes if possible.

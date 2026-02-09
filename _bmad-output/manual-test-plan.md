# TraceGlass Manual Test Plan

**App:** TraceGlass (Android)
**Version:** Epics 1-7 (all implemented features)
**Date:** 2026-02-09

---

## Prerequisites

- Android device running API 26+ with a rear camera
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

### TC-ONB-02: Welcome page content
**Description:** Verify the first slide displays correct welcome content.
**Steps:**
1. Launch the app on first install.
2. Observe the first page of the carousel.
**Expected Result:** Page displays "Welcome to TraceGlass" as headline, with body text: "Turn your phone into a tracing lightbox. Point your camera at your drawing surface, pick a reference image, and trace with precision."

### TC-ONB-03: Swipe through carousel pages
**Description:** Verify horizontal swiping navigates between the 3 onboarding slides.
**Steps:**
1. On the onboarding screen, swipe left.
2. Observe the second page.
3. Swipe left again.
4. Observe the third page.
5. Swipe right twice to return to the first page.
**Expected Result:** Three distinct pages are shown in order: Welcome, Tier Selection ("Choose Your Setup"), and Marker Preparation ("Prepare Your Markers"). Swiping back returns to previous pages. Page indicator dots update to reflect the current page.

### TC-ONB-04: Page indicator reflects current page
**Description:** Verify the dot indicator at the bottom accurately shows which of the 3 pages is active.
**Steps:**
1. On the onboarding screen, observe the page indicator (3 dots).
2. Navigate to each page using swipe or the Next button.
**Expected Result:** Exactly 3 dots are displayed. The dot corresponding to the current page is highlighted in the primary color; inactive dots are dimmed.

### TC-ONB-05: Tier selection — Full DIY
**Description:** Verify selecting the Full DIY tier on page 2.
**Steps:**
1. Navigate to the second onboarding page (Tier Selection).
2. Tap the "Full DIY -- Print your own markers" chip.
3. Navigate to the third page.
**Expected Result:** The Full DIY chip becomes selected (visually highlighted). On the Marker Preparation page, the text instructs the user to print two ArUco markers (ID #0 and #1) at about 2 cm and tape them at opposite corners.

### TC-ONB-06: Tier selection — Semi-equipped
**Description:** Verify selecting the Semi-equipped tier changes page 3 content.
**Steps:**
1. Navigate to page 2 and tap "Semi-equipped -- Printed markers".
2. Navigate to page 3.
**Expected Result:** The Semi-equipped chip is selected. Page 3 text mentions printing the ArUco marker sheet from the setup guide, cutting out markers #0 and #1, and taping at opposite corners.

### TC-ONB-07: Tier selection — Full kit
**Description:** Verify selecting the Full kit tier changes page 3 content.
**Steps:**
1. Navigate to page 2 and tap "Full kit -- Printed markers + stand".
2. Navigate to page 3.
**Expected Result:** The Full kit chip is selected. Page 3 text mentions printing the marker sheet, setting up the phone stand, taping markers, and mounting the phone above.

### TC-ONB-08: Next button advances pages
**Description:** Verify the Next button advances through pages, then becomes "Get Started" on the last page.
**Steps:**
1. On page 0 (Welcome), tap "Next".
2. On page 1 (Tier Selection), tap "Next".
3. On page 2 (Marker Preparation), observe the button text.
4. Tap "Get Started".
**Expected Result:** Each tap of "Next" advances to the next page. On page 2, the button reads "Get Started". Tapping it completes onboarding and navigates to the tracing screen.

### TC-ONB-09: Skip button completes onboarding
**Description:** Verify the Skip button at the top-right exits onboarding immediately.
**Steps:**
1. On any onboarding page, tap "Skip" (top-right).
**Expected Result:** Onboarding completes. The app navigates to the tracing screen. Onboarding is marked as completed and will not show on subsequent launches.

### TC-ONB-10: Onboarding completion is persisted
**Description:** Verify that after completing onboarding, subsequent launches skip it.
**Steps:**
1. Complete onboarding (via "Get Started" or "Skip").
2. Force-stop the app.
3. Relaunch the app.
**Expected Result:** The app launches directly to the tracing screen (camera permission flow), not the onboarding screen.

### TC-ONB-11: View detailed setup guide link
**Description:** Verify the "View detailed setup guide" link on page 3 navigates to the Setup Guide screen.
**Steps:**
1. Navigate to onboarding page 3 (Marker Preparation).
2. Tap "View detailed setup guide".
**Expected Result:** The Setup Guide screen opens with a top app bar reading "Setup Guide", section selector chips ("Marker Guide" and "Stand Setup"), and relevant content.

### TC-ONB-12: Setup guide — Marker Guide content
**Description:** Verify the Marker Guide section displays 3 step cards with correct instructions.
**Steps:**
1. Open the Setup Guide screen.
2. Ensure the "Marker Guide" chip is selected (default).
3. Read the step cards.
**Expected Result:** Three step cards are shown:
- Step 1: "Print or display markers" with instructions about ArUco 4x4 markers 0 and 1 at about 2 cm.
- Step 2: "Place at opposite corners" with instructions to tape at two opposite corners.
- Step 3: "Keep markers visible" with instructions to keep both markers visible to the camera.

### TC-ONB-13: Setup guide — MacGyver Stand content
**Description:** Verify the Stand Setup section displays improvised phone stand options.
**Steps:**
1. On the Setup Guide screen, tap the "Stand Setup" chip.
**Expected Result:** The section switches to show "Improvised Phone Stand" with three stand option cards: "Glass of Water", "Stack of Books", and "Box or Container", each with practical instructions.

### TC-ONB-14: Setup guide — External download links
**Description:** Verify the download links at the bottom of the Setup Guide open the browser.
**Steps:**
1. On the Setup Guide screen, scroll to the bottom "Downloads" section.
2. Tap "Download printable marker sheet (opens browser)".
3. Return to app, tap "Download 3D phone stand model (opens browser)".
**Expected Result:** Each button opens the device browser navigating to the respective GitHub release URLs (markers.pdf and phone-stand.stl).

### TC-ONB-15: Setup guide — Back navigation
**Description:** Verify the back arrow on the Setup Guide returns to the onboarding carousel.
**Steps:**
1. From the Setup Guide screen, tap the back arrow in the top app bar.
**Expected Result:** The app returns to the onboarding carousel at the page from which the guide was opened.

---

## 2. Camera & Permissions

### TC-CAM-01: Camera permission requested on first grant flow
**Description:** Verify the camera permission is automatically requested when the tracing screen is first shown.
**Steps:**
1. Complete onboarding (or launch the app after onboarding is done).
2. Observe the system permission dialog.
**Expected Result:** A system dialog appears requesting camera permission. The tracing screen content is not yet visible behind it.

### TC-CAM-02: Camera preview shown after granting permission
**Description:** Verify that granting camera permission shows the live camera preview.
**Steps:**
1. On the permission dialog, tap "Allow" (or "While using the app").
**Expected Result:** The camera preview fills the screen showing the live rear camera feed. UI controls appear: Settings button (top-left), tracking indicator area (top-right), flashlight toggle (bottom-left), and image picker FAB (bottom-right).

### TC-CAM-03: Permission denied — denial screen shown
**Description:** Verify that denying camera permission shows the denial screen.
**Steps:**
1. On the permission dialog, tap "Don't allow".
**Expected Result:** A centered screen appears with the headline "Camera permission required", an explanation "TraceGlass needs camera access to display your drawing surface. No images are stored or sent anywhere.", and a "Grant Permission" button.

### TC-CAM-04: Retry permission from denial screen
**Description:** Verify the "Grant Permission" button re-requests the camera permission.
**Steps:**
1. On the permission denied screen, tap "Grant Permission".
**Expected Result:** The system permission dialog appears again. If the user grants permission, the camera preview appears. If denied again, the denial screen remains.

### TC-CAM-05: Flashlight toggle — turn on
**Description:** Verify the flashlight toggle turns on the torch.
**Steps:**
1. With camera permission granted and the camera preview visible, locate the flashlight FAB (bottom-left, outlined star icon).
2. Tap the flashlight button.
**Expected Result:** The device flashlight turns on. The icon changes to a filled star.

### TC-CAM-06: Flashlight toggle — turn off
**Description:** Verify toggling the flashlight again turns it off.
**Steps:**
1. With the flashlight on, tap the flashlight button again.
**Expected Result:** The device flashlight turns off. The icon changes back to an outlined star.

### TC-CAM-07: Flashlight hidden on devices without flash
**Description:** Verify the flashlight button is not displayed on devices that lack a flashlight.
**Steps:**
1. Run the app on a device or emulator without a flashlight unit.
**Expected Result:** No flashlight FAB is rendered in the bottom-left corner. The layout does not show an empty space or placeholder.

---

## 3. Image Import & Overlay Display

### TC-IMG-01: Open photo picker
**Description:** Verify tapping the image picker FAB opens the system photo picker.
**Steps:**
1. On the tracing screen (camera preview visible), tap the "+" FAB (bottom-right).
**Expected Result:** The Android system photo picker opens, showing the device gallery filtered to images only.

### TC-IMG-02: Select an image
**Description:** Verify picking an image displays it as an overlay on the camera preview.
**Steps:**
1. Tap the "+" FAB to open the photo picker.
2. Select any image from the gallery.
**Expected Result:** The selected image appears overlaid on the camera preview at 50% default opacity. The image fills the screen area with Fit content scale. Additional controls appear: opacity FAB (center-right), visual mode controls (top-left area), and Start/Stop session button (bottom-center).

### TC-IMG-03: Cancel photo picker
**Description:** Verify cancelling the photo picker does not change the current overlay state.
**Steps:**
1. Tap the "+" FAB to open the photo picker.
2. Press the back button or tap outside to dismiss the picker without selecting.
**Expected Result:** The photo picker closes. If no image was previously selected, no overlay is shown. If an image was already loaded, it remains unchanged.

### TC-IMG-04: Replace overlay image
**Description:** Verify picking a new image replaces the current overlay.
**Steps:**
1. Select an image via the photo picker so it appears as an overlay.
2. Tap the "+" FAB again and select a different image.
**Expected Result:** The new image replaces the old one as the overlay. The opacity retains its current value. The overlay offset resets only if a new session is started.

### TC-IMG-05: Overlay displays with Fit content scale
**Description:** Verify the overlay image uses ContentScale.Fit and does not crop.
**Steps:**
1. Select a landscape-oriented image while holding the phone in portrait mode.
2. Observe the overlay.
**Expected Result:** The image is scaled to fit within the screen bounds without cropping. Letterboxing (empty space above/below or to the sides) may be visible depending on aspect ratio.

---

## 4. Overlay Positioning (Manual)

### TC-POS-01: Drag overlay to reposition
**Description:** Verify that dragging on the overlay image pans it.
**Steps:**
1. With an overlay image displayed, place one finger on the overlay.
2. Drag the finger in any direction (up, down, left, right).
**Expected Result:** The overlay moves in the same direction as the drag gesture. The camera preview remains stationary beneath.

### TC-POS-02: Pinch to zoom overlay
**Description:** Verify that a pinch gesture scales the overlay image.
**Steps:**
1. With an overlay image displayed, place two fingers on the screen.
2. Spread fingers apart (pinch out).
3. Pinch fingers together (pinch in).
**Expected Result:** Spreading fingers enlarges the overlay image. Pinching reduces it. The scale factor multiplies cumulatively. The camera preview is unaffected.

### TC-POS-03: Combined drag and pinch
**Description:** Verify simultaneous drag and pinch work together.
**Steps:**
1. With an overlay displayed, use two fingers to simultaneously drag and pinch.
**Expected Result:** The overlay moves and scales simultaneously within a single gesture. Both transformations are applied smoothly.

### TC-POS-04: Overlay position persists during a session
**Description:** Verify the overlay stays at its repositioned location while interacting with other controls.
**Steps:**
1. Drag the overlay to an off-center position.
2. Change the opacity slider.
3. Toggle a color tint.
**Expected Result:** The overlay remains at the repositioned location after interacting with other controls.

---

## 5. Visual Modes (Opacity, Color Tint, Inverted)

### TC-VIS-01: Opacity FAB shows current percentage
**Description:** Verify the opacity FAB displays the current opacity as a percentage.
**Steps:**
1. With an overlay image loaded, look at the opacity FAB on the center-right edge.
**Expected Result:** The FAB displays "50%" (default opacity).

### TC-VIS-02: Toggle opacity slider visibility
**Description:** Verify tapping the opacity FAB shows/hides the slider.
**Steps:**
1. Tap the opacity FAB.
2. Observe the slider appearing.
3. Wait more than 3 seconds without interaction.
**Expected Result:** A vertical slider animates in above the FAB, showing the current percentage at the top. After 3 seconds of inactivity, the slider automatically hides itself with a fade/slide-out animation.

### TC-VIS-03: Adjust opacity via slider
**Description:** Verify the slider changes overlay opacity from 0% to 100%.
**Steps:**
1. Tap the opacity FAB to reveal the slider.
2. Drag the slider to the minimum (0%).
3. Observe the overlay.
4. Drag the slider to the maximum (100%).
5. Observe the overlay.
6. Set to approximately 75%.
**Expected Result:** At 0%, the overlay is fully transparent (invisible). At 100%, the overlay is fully opaque, completely covering the camera preview beneath. At 75%, the overlay is mostly opaque with slight camera feed showing through. The FAB label and slider label update in real time to reflect the percentage.

### TC-VIS-04: Color tint — None (default)
**Description:** Verify the default color tint is None.
**Steps:**
1. Load an overlay image.
2. Look at the visual mode controls (top-left area).
**Expected Result:** The "None" filter chip is selected (highlighted). The overlay displays its original colors without tinting.

### TC-VIS-05: Color tint — Red
**Description:** Verify applying the Red tint.
**Steps:**
1. With an overlay loaded, tap the "Red" filter chip in the visual mode controls.
**Expected Result:** The "Red" chip becomes selected. The overlay image takes on a red color tint (Modulate blend at 50% alpha red). Other tint chips are deselected.

### TC-VIS-06: Color tint — Green
**Description:** Verify applying the Green tint.
**Steps:**
1. Tap the "Green" filter chip.
**Expected Result:** The overlay takes on a green color tint. The "Green" chip is selected; previous selection is deselected.

### TC-VIS-07: Color tint — Blue
**Description:** Verify applying the Blue tint.
**Steps:**
1. Tap the "Blue" filter chip.
**Expected Result:** The overlay takes on a blue color tint. The "Blue" chip is selected.

### TC-VIS-08: Color tint — Grayscale
**Description:** Verify applying the Grayscale tint.
**Steps:**
1. Tap the "Gray" filter chip.
**Expected Result:** The overlay image becomes desaturated (black and white / grayscale). The "Gray" chip is selected.

### TC-VIS-09: Switch between tints
**Description:** Verify switching from one tint to another.
**Steps:**
1. Select "Red" tint, then tap "Blue", then "None".
**Expected Result:** Each tap immediately applies the new tint. Only one tint chip is selected at a time. Selecting "None" restores original image colors.

### TC-VIS-10: Inverted mode toggle
**Description:** Verify the Inverted mode chip inverts the effective opacity.
**Steps:**
1. Set opacity to 30% (overlay is faintly visible).
2. Tap the "Inverted" filter chip.
**Expected Result:** The "Inverted" chip becomes selected. The effective opacity flips to 70% (1 - 0.3 = 0.7), making the overlay more opaque. The opacity FAB still reads "30%" (the stored value), but the visual opacity is inverted.

### TC-VIS-11: Inverted mode off
**Description:** Verify toggling inverted mode off restores original opacity behavior.
**Steps:**
1. With inverted mode active and opacity at 30%, tap the "Inverted" chip again.
**Expected Result:** The "Inverted" chip is deselected. The overlay returns to 30% visual opacity (faintly visible).

### TC-VIS-12: Inverted mode at boundary values
**Description:** Verify inverted mode works at 0% and 100% opacity.
**Steps:**
1. Set opacity to 0%, enable Inverted mode.
2. Set opacity to 100%, keep Inverted mode on.
**Expected Result:** At 0% + Inverted: overlay is fully opaque (effective 100%). At 100% + Inverted: overlay is fully transparent (effective 0%).

### TC-VIS-13: Visual mode controls only shown when overlay is loaded
**Description:** Verify color tint and inverted mode controls are hidden when no image is loaded.
**Steps:**
1. On the tracing screen with no overlay image loaded, inspect the top-left area.
**Expected Result:** No color tint chips or "Inverted" chip are visible. Only the Settings button is in the top-left area.

### TC-VIS-14: Opacity FAB only shown when overlay is loaded
**Description:** Verify the opacity FAB is hidden when no image is loaded.
**Steps:**
1. On the tracing screen with no overlay image, inspect the center-right area.
**Expected Result:** No opacity FAB is visible.

---

## 6. ArUco Marker Tracking

### TC-TRK-01: Tracking indicator — INACTIVE (default)
**Description:** Verify no tracking indicator is shown when markers have never been detected.
**Steps:**
1. On the tracing screen with the camera preview visible, without any ArUco markers in view.
2. Inspect the top-right area.
**Expected Result:** No tracking indicator badge is visible (the INACTIVE state renders nothing).

### TC-TRK-02: Tracking indicator — TRACKING
**Description:** Verify the tracking indicator shows green "Tracking" when markers are detected.
**Steps:**
1. Place two ArUco 4x4 markers (ID #0 and #1) in view of the camera.
2. Wait for detection (should be near-instantaneous per frame).
**Expected Result:** A badge appears in the top-right corner with a green circle dot and the text "Tracking" on a semi-transparent black background.

### TC-TRK-03: Tracking indicator — LOST
**Description:** Verify the indicator transitions to orange "Lost" when markers disappear.
**Steps:**
1. With tracking active (green "Tracking" badge visible), cover or remove both markers from the camera view.
2. Wait approximately 500 ms.
**Expected Result:** The badge transitions to show an orange triangle and the text "Lost". During the first ~500 ms after markers disappear, the badge may still show "Tracking" (grace period).

### TC-TRK-04: Grace period before LOST
**Description:** Verify the grace period prevents flickering.
**Steps:**
1. With tracking active, briefly obstruct the markers for less than 500 ms, then reveal them again.
**Expected Result:** The tracking indicator remains "Tracking" (green) throughout the brief obstruction, without flickering to "Lost".

### TC-TRK-05: Overlay offset follows marker center
**Description:** Verify the overlay position adjusts based on detected marker positions.
**Steps:**
1. Load an overlay image.
2. Place both ArUco markers in the camera view.
3. Slowly move the markers (or the phone) to the left.
**Expected Result:** The overlay image shifts to follow the marker positions. The overlay center aligns approximately with the centroid of the detected markers, offset from the frame center.

### TC-TRK-06: Scale from marker spacing
**Description:** Verify the overlay scale changes when markers move closer or farther apart.
**Steps:**
1. With overlay loaded and both markers tracked, note the initial overlay scale.
2. Move the markers farther apart (or move the phone closer so markers appear farther apart in frame).
3. Move the markers closer together (or move the phone farther away).
**Expected Result:** When markers appear farther apart relative to the initial detection, the overlay scales up. When markers appear closer together, the overlay scales down. The scale is relative to the first detection spacing.

### TC-TRK-07: Rotation from marker angle
**Description:** Verify the overlay rotates when the angle between the two markers changes.
**Steps:**
1. With both markers tracked, rotate the drawing surface (or tilt the phone) so the line between marker #0 and #1 changes angle.
**Expected Result:** The overlay rotates to match the angular change between the two markers, relative to the angle at first detection. A clockwise physical rotation results in a corresponding clockwise overlay rotation.

### TC-TRK-08: Single marker detection — no scale or rotation
**Description:** Verify behavior when only one marker is detected.
**Steps:**
1. Place only marker #0 in the camera view (keep #1 hidden).
**Expected Result:** The tracking indicator shows "Tracking" (green). The overlay offsets to follow the single marker position. The overlay scale remains at 1.0 and rotation at 0 degrees (scale and rotation require 2 markers).

### TC-TRK-09: Tracking uses actual camera frame dimensions
**Description:** Verify the offset computation uses actual camera resolution, not hardcoded values.
**Steps:**
1. This is best verified by observing that overlay positioning is accurate on different device screen sizes and camera resolutions.
**Expected Result:** The overlay offset aligns correctly relative to the camera preview on different devices. There should be no systematic drift or misalignment that would indicate hardcoded 1080x1920 values.

---

## 7. Session Controls & Persistence

### TC-SES-01: Start button appears only with overlay loaded
**Description:** Verify the Start/Stop button is only shown when an overlay image is loaded.
**Steps:**
1. On the tracing screen with no overlay image, check the bottom-center area.
2. Load an overlay image.
**Expected Result:** No session button is visible without an overlay. After loading an overlay, a "Start" button (FAB) appears at bottom-center with the primary color.

### TC-SES-02: Start session
**Description:** Verify tapping "Start" activates the session.
**Steps:**
1. With an overlay loaded, tap the "Start" FAB at bottom-center.
**Expected Result:** The button text changes to "Stop" and the button color changes to the error color (red). The session is now active.

### TC-SES-03: Keep screen on during active session
**Description:** Verify the screen does not turn off during an active session.
**Steps:**
1. Start a session (tap "Start").
2. Set the device screen timeout to its shortest value (e.g., 15 seconds).
3. Leave the device untouched for longer than the screen timeout.
**Expected Result:** The screen remains on while the session is active. The FLAG_KEEP_SCREEN_ON flag is set.

### TC-SES-04: Stop session
**Description:** Verify tapping "Stop" deactivates the session.
**Steps:**
1. With an active session, tap the "Stop" FAB.
**Expected Result:** The button text changes back to "Start" and the color returns to primary. The session is no longer active.

### TC-SES-05: Screen timeout resumes after stopping
**Description:** Verify the screen can time out again after stopping the session.
**Steps:**
1. Stop an active session.
2. Wait for the device screen timeout period.
**Expected Result:** The screen dims and turns off normally (FLAG_KEEP_SCREEN_ON is cleared).

### TC-SES-06: Hide controls by tapping
**Description:** Verify tapping the screen hides all controls when controls are visible.
**Steps:**
1. With controls visible and no overlay gesture in progress, tap once on the main screen area.
**Expected Result:** All UI controls disappear: Settings button, flashlight button, image picker FAB, opacity FAB, visual mode controls, session button, and tracking indicator. Only the camera preview and overlay image remain visible.

### TC-SES-07: Show controls by tapping
**Description:** Verify tapping the screen when controls are hidden brings them back.
**Steps:**
1. With controls hidden (invisible), tap once on the screen.
**Expected Result:** All UI controls reappear in their original positions.

### TC-SES-08: Auto-save session on Stop
**Description:** Verify session data is saved when the session is stopped.
**Steps:**
1. Load an image, adjust opacity to 70%, select Blue tint, enable Inverted mode.
2. Drag the overlay to a custom position.
3. Tap "Start" then "Stop".
4. Force-stop the app, then relaunch it.
**Expected Result:** After relaunching, the overlay restores with the previously selected image, 70% opacity, Blue tint, Inverted mode, and the custom position offset.

### TC-SES-09: Auto-save session on background
**Description:** Verify session data is saved when the app goes to the background.
**Steps:**
1. Start a session with a loaded image and custom settings.
2. Press the Home button to send the app to the background.
3. Force-stop the app from recent apps or settings.
4. Relaunch the app.
**Expected Result:** The session state is restored: the same overlay image, offset, scale, opacity, color tint, inverted mode, and session active state.

### TC-SES-10: Auto-restore session on foreground
**Description:** Verify the session is restored when the app returns to the foreground.
**Steps:**
1. Set up a session with a specific image, offset, and visual modes.
2. Press Home to background the app.
3. Switch back to the app.
**Expected Result:** The overlay and all session settings are intact. The app does not reset to defaults.

### TC-SES-11: No restore without previous session
**Description:** Verify no stale data is restored on a fresh launch with no previous session.
**Steps:**
1. Clear all app data.
2. Complete onboarding.
3. Observe the tracing screen.
**Expected Result:** No overlay image is loaded. Default state: 50% opacity, no color tint, inverted mode off, session inactive.

### TC-SES-12: Session persists all visual mode settings
**Description:** Verify all visual settings survive a full restart cycle.
**Steps:**
1. Load an image, set opacity to 25%, select Green tint, enable Inverted mode.
2. Tap Start, then Stop.
3. Force-stop and relaunch the app.
**Expected Result:** Image is restored. Opacity is 25%. Color tint is Green. Inverted mode is enabled.

---

## 8. Settings

### TC-SET-01: Navigate to Settings
**Description:** Verify tapping the Settings button opens the Settings screen.
**Steps:**
1. On the tracing screen, tap the gear icon FAB (top-left area).
**Expected Result:** The Settings screen opens with a top app bar reading "Settings" and a back arrow.

### TC-SET-02: Back navigation from Settings
**Description:** Verify the back arrow returns to the tracing screen.
**Steps:**
1. On the Settings screen, tap the back arrow in the top app bar.
**Expected Result:** The app navigates back to the tracing screen with all previous state intact.

### TC-SET-03: Audio feedback toggle — default off
**Description:** Verify the Audio feedback toggle defaults to off.
**Steps:**
1. Open the Settings screen on a fresh install.
**Expected Result:** The "Audio feedback" list item shows with supporting text "Play sound on tracking state changes" and the switch is in the OFF position.

### TC-SET-04: Audio feedback toggle — enable
**Description:** Verify toggling audio feedback on.
**Steps:**
1. On the Settings screen, tap the Audio feedback switch to ON.
2. Go back to the tracing screen.
**Expected Result:** The switch moves to ON. The setting is persisted (verifiable by leaving and returning to Settings).

### TC-SET-05: Break reminder toggle — default off
**Description:** Verify the Break reminders toggle defaults to off.
**Steps:**
1. Open the Settings screen on a fresh install.
**Expected Result:** The "Break reminders" list item shows with supporting text "Remind to take breaks during tracing" and the switch is in the OFF position. No interval slider is visible.

### TC-SET-06: Break reminder toggle — enable reveals slider
**Description:** Verify enabling break reminders shows the interval slider.
**Steps:**
1. On the Settings screen, tap the Break reminders switch to ON.
**Expected Result:** The switch moves to ON. An interval slider animates into view below, showing "Reminder interval: 30 min" (default value) with a slider from 5 to 60 minutes.

### TC-SET-07: Break reminder interval slider adjustment
**Description:** Verify the break reminder interval slider adjusts the value.
**Steps:**
1. With break reminders enabled, drag the interval slider to the left (toward 5 min).
2. Drag the slider to the right (toward 60 min).
**Expected Result:** The label updates in real time (e.g., "Reminder interval: 5 min", "Reminder interval: 60 min"). The value snaps to discrete steps between 5 and 60.

### TC-SET-08: Break reminder toggle — disable hides slider
**Description:** Verify disabling break reminders hides the interval slider.
**Steps:**
1. With break reminders enabled and slider visible, tap the Break reminders switch to OFF.
**Expected Result:** The interval slider animates out and is no longer visible.

### TC-SET-09: Re-open onboarding from Settings
**Description:** Verify the "Re-open onboarding" item navigates to the onboarding carousel.
**Steps:**
1. On the Settings screen, tap "Re-open onboarding" (supporting text: "Review setup guides and tips").
**Expected Result:** The onboarding carousel opens in REOPENED mode, starting at the first page.

### TC-SET-10: Re-opened onboarding does not re-persist completion
**Description:** Verify completing the re-opened onboarding does not reset the completion flag.
**Steps:**
1. Complete onboarding on first launch.
2. Go to Settings and tap "Re-open onboarding".
3. Tap "Get Started" or "Skip" to complete the re-opened onboarding.
4. Force-stop and relaunch the app.
**Expected Result:** The app still starts directly at the tracing screen (onboarding remains marked as completed from the first time). The re-opened flow simply navigates back rather than re-persisting the flag.

### TC-SET-11: About link navigates to About screen
**Description:** Verify the "About" item navigates to the About screen.
**Steps:**
1. On the Settings screen, tap "About" (supporting text: "App info and licenses").
**Expected Result:** The About screen opens.

### TC-SET-12: Settings persistence across app restart
**Description:** Verify settings values are persisted across app restarts.
**Steps:**
1. Enable audio feedback, enable break reminders, set interval to 15 minutes.
2. Force-stop and relaunch the app.
3. Navigate back to Settings.
**Expected Result:** Audio feedback is ON. Break reminders are ON. Interval slider shows 15 min.

---

## 9. Break Reminders

### TC-BRK-01: Break reminder fires after interval
**Description:** Verify the break reminder notification appears after the configured interval during an active session.
**Steps:**
1. In Settings, enable break reminders and set the interval to 5 minutes (minimum).
2. Return to the tracing screen, load an image, and start a session.
3. Wait 5 minutes without stopping the session.
**Expected Result:** A Snackbar appears at the bottom of the screen with the message "Time for a break!".

### TC-BRK-02: Audio tone plays with break reminder (audio enabled)
**Description:** Verify the notification tone plays when audio feedback is enabled.
**Steps:**
1. In Settings, enable both audio feedback and break reminders.
2. Set interval to 5 minutes.
3. Start a session and wait for the reminder.
**Expected Result:** When the break reminder Snackbar appears, the device plays the default notification ringtone.

### TC-BRK-03: No audio tone when audio feedback disabled
**Description:** Verify no sound plays when audio feedback is disabled.
**Steps:**
1. In Settings, disable audio feedback but enable break reminders.
2. Set interval to 5 minutes.
3. Start a session and wait for the reminder.
**Expected Result:** The break reminder Snackbar appears but no notification sound plays.

### TC-BRK-04: Break reminder resets on dismiss
**Description:** Verify the timer restarts after the reminder is dismissed.
**Steps:**
1. Enable break reminders at 5 minutes.
2. Start a session, wait for the first reminder.
3. Dismiss the Snackbar (it auto-dismisses).
4. Wait another 5 minutes.
**Expected Result:** A second break reminder Snackbar appears after another 5 minutes.

### TC-BRK-05: Break timer stops when session stops
**Description:** Verify no break reminder fires after stopping the session.
**Steps:**
1. Enable break reminders at 5 minutes.
2. Start a session.
3. After 2 minutes, stop the session.
4. Wait an additional 5 minutes.
**Expected Result:** No break reminder Snackbar appears. The timer was cancelled when the session stopped.

### TC-BRK-06: Break timer resets on interval change
**Description:** Verify changing the interval mid-session restarts the timer.
**Steps:**
1. Enable break reminders at 10 minutes.
2. Start a session.
3. After 3 minutes, go to Settings and change the interval to 5 minutes.
4. Return to the tracing screen.
5. Wait 5 minutes from the interval change.
**Expected Result:** The break reminder fires approximately 5 minutes after the interval was changed (not 7 minutes from session start). The timer restarted when the setting changed.

### TC-BRK-07: No break reminder when reminders disabled
**Description:** Verify no reminders fire when break reminders are disabled.
**Steps:**
1. In Settings, disable break reminders.
2. Start a session with an overlay loaded.
3. Wait well beyond any reasonable reminder interval.
**Expected Result:** No break reminder Snackbar ever appears.

### TC-BRK-08: Break reminder does not fire when session is inactive
**Description:** Verify the timer only runs during an active session.
**Steps:**
1. Enable break reminders at 5 minutes.
2. Load an image but do NOT start a session.
3. Wait 10 minutes.
**Expected Result:** No break reminder appears. The timer is only active when both break reminders are enabled AND the session is active.

---

## 10. About Screen

### TC-ABT-01: App name and version displayed
**Description:** Verify the About screen shows app name, version, and build number.
**Steps:**
1. Navigate to Settings then tap "About".
**Expected Result:** The screen shows "TraceGlass" as headline, and "Version X.Y.Z (build N)" as supporting text, where X.Y.Z and N come from BuildConfig.

### TC-ABT-02: License information
**Description:** Verify the license section is displayed correctly.
**Steps:**
1. On the About screen, check the License list item.
**Expected Result:** Shows "License" as headline and "GNU General Public License v3.0" as supporting text.

### TC-ABT-03: Third-party licenses
**Description:** Verify third-party library licenses are listed.
**Steps:**
1. On the About screen, check the third-party licenses section.
**Expected Result:** Shows an overline "Open-source libraries", headline "Third-party licenses", and lists: "OpenCV (Apache 2.0)", "Coil (Apache 2.0)", "Koin (Apache 2.0)", "Jetpack libraries (Apache 2.0)" as supporting text.

### TC-ABT-04: GitHub link opens browser
**Description:** Verify the GitHub button opens the project URL in the browser.
**Steps:**
1. On the About screen, tap the "View on GitHub (opens browser)" button.
**Expected Result:** The device browser opens navigating to https://github.com/jn0v/traceglass.

### TC-ABT-05: Back navigation from About
**Description:** Verify the back arrow returns to Settings.
**Steps:**
1. On the About screen, tap the back arrow.
**Expected Result:** The app returns to the Settings screen.

---

## 11. App Icon

### TC-ICN-01: Adaptive icon renders correctly
**Description:** Verify the app icon displays correctly on the device launcher.
**Steps:**
1. Install the app.
2. Find the TraceGlass icon on the device home screen or app drawer.
**Expected Result:** The app icon is an adaptive icon. The foreground layer shows a teal glass lens (circle outline) with a handle, a darker pencil trace wavy line through the lens, and a small lens glare arc. The background layer is a light teal (#B2DFDB) solid color.

### TC-ICN-02: Icon in various launcher shapes
**Description:** Verify the adaptive icon looks correct in different launcher mask shapes.
**Steps:**
1. If possible, test on launchers that use circle, squircle, rounded square, and teardrop icon masks (or use a launcher that allows changing icon shape).
**Expected Result:** The icon foreground elements (lens + trace) remain visible and centered within all common icon shapes. No critical elements are clipped.

### TC-ICN-03: Round icon variant
**Description:** Verify the round icon variant renders correctly on devices that use it.
**Steps:**
1. Check the app icon on devices or launchers that use the round icon variant.
**Expected Result:** The ic_launcher_round variant renders the same foreground and background layers as the standard adaptive icon within a circular mask.

---

## 12. Edge Cases & Error Handling

### TC-EDGE-01: Rotate device during tracing
**Description:** Verify the app handles device rotation gracefully.
**Steps:**
1. Load an overlay image, start a session.
2. Rotate the device from portrait to landscape.
3. Rotate back to portrait.
**Expected Result:** The camera preview and overlay adjust to the new orientation. The session remains active. No crash occurs.

### TC-EDGE-02: App killed by system while session active
**Description:** Verify the app can restore after being killed by the system (process death).
**Steps:**
1. Start a session with an overlay, custom offset, and visual modes applied.
2. Background the app (press Home, which triggers ON_STOP save).
3. Force-stop the app from Settings > Apps.
4. Relaunch the app.
**Expected Result:** The session is restored with all previously saved state. The overlay image, position, opacity, tint, and inverted mode are restored.

### TC-EDGE-03: Rapidly toggling torch
**Description:** Verify rapid flashlight toggling does not crash the app.
**Steps:**
1. Tap the flashlight toggle rapidly 10+ times in quick succession.
**Expected Result:** The flashlight toggles state each time without crashing. The icon accurately reflects the current torch state.

### TC-EDGE-04: Rapidly toggling session start/stop
**Description:** Verify rapid session toggling does not corrupt state.
**Steps:**
1. With an overlay loaded, rapidly tap Start/Stop multiple times.
**Expected Result:** The session state toggles cleanly. The button text and color update correctly. No crash or inconsistent UI state.

### TC-EDGE-05: Very large image as overlay
**Description:** Verify the app handles large images (e.g., 8000x6000 high-resolution photos) without crashing.
**Steps:**
1. Select a very large image file from the gallery as the overlay.
**Expected Result:** The image loads and displays as an overlay. There may be a brief loading delay but no OutOfMemoryError or crash.

### TC-EDGE-06: Overlay gesture when controls are hidden
**Description:** Verify tapping to show controls does not also trigger overlay drag.
**Steps:**
1. Load an overlay and hide controls by tapping.
2. Tap once to show controls.
**Expected Result:** Controls reappear. The overlay does not jump or shift position from the tap (the tap is consumed by the show-controls handler, not the drag gesture).

### TC-EDGE-07: Markers partially in frame
**Description:** Verify behavior when markers are at the very edge of the camera view.
**Steps:**
1. Position both markers so they are barely within the camera frame.
2. Slowly move one marker out of frame.
**Expected Result:** Tracking may become intermittent as markers move to the edge. When a marker exits the frame, the tracker reports fewer markers and the overlay adjusts accordingly (scale/rotation revert to defaults with only one marker).

### TC-EDGE-08: No markers ever detected — overlay remains manual
**Description:** Verify that without marker detection, the overlay is purely manual.
**Steps:**
1. Load an overlay without any ArUco markers in view.
2. Drag and pinch to position the overlay manually.
**Expected Result:** The tracking indicator stays INACTIVE (not shown). The overlay is fully controllable via manual drag and pinch. No automatic position adjustments occur.

### TC-EDGE-09: Background then foreground — camera rebinds
**Description:** Verify the camera preview reconnects after backgrounding and foregrounding.
**Steps:**
1. On the tracing screen, verify the camera preview is active.
2. Press Home.
3. Return to the app.
**Expected Result:** The camera preview resumes showing the live feed. No black screen or frozen frame.

### TC-EDGE-10: Permission permanently denied (system redirect)
**Description:** Verify behavior when the user has permanently denied camera permission.
**Steps:**
1. Deny camera permission.
2. On the denial screen, tap "Grant Permission".
3. If the system shows "Don't ask again" or silently denies, observe behavior.
**Expected Result:** The permission denied screen remains displayed. The user can go to system Settings to manually grant the permission. The app does not crash.

### TC-EDGE-11: Photo picker — select non-image file
**Description:** Verify the photo picker is restricted to images only.
**Steps:**
1. Tap the "+" FAB to open the photo picker.
2. Attempt to select a video or non-image file.
**Expected Result:** The photo picker should only show images (PickVisualMedia.ImageOnly is used). Videos and other media types should not be selectable.

### TC-EDGE-12: Opacity slider auto-hide timer
**Description:** Verify the opacity slider auto-hides after 3 seconds.
**Steps:**
1. Tap the opacity FAB to show the slider.
2. Do not touch the slider for 3 seconds.
3. Immediately tap the FAB again to re-show.
**Expected Result:** The slider auto-hides after 3 seconds with an animation. Tapping the FAB again immediately brings it back.

### TC-EDGE-13: Multiple settings changes — rapid toggling
**Description:** Verify rapidly toggling settings does not cause race conditions.
**Steps:**
1. On the Settings screen, rapidly toggle Audio feedback on/off 5 times.
2. Rapidly toggle Break reminders on/off 5 times.
3. Rapidly drag the break interval slider back and forth.
**Expected Result:** All settings end up in a consistent state matching the last user action. No crash or inconsistent display.

### TC-EDGE-14: Onboarding re-open then navigate to guide and back
**Description:** Verify the full navigation path through re-opened onboarding and setup guide.
**Steps:**
1. From Settings, tap "Re-open onboarding".
2. Navigate to page 3 (Marker Preparation).
3. Tap "View detailed setup guide".
4. On the Setup Guide, tap back.
5. On the onboarding carousel, tap "Get Started".
**Expected Result:** Each navigation step works correctly. After completing the re-opened onboarding, the app returns to the Settings screen (or the previous screen in the back stack). No orphaned screens or navigation loops.

### TC-EDGE-15: Session restore with deleted image
**Description:** Verify the app handles a restored session where the original image URI is no longer accessible.
**Steps:**
1. Load an image and start/stop a session to save it.
2. Delete that image from the device gallery.
3. Force-stop and relaunch the app.
**Expected Result:** The session data is restored (opacity, tint, etc.) but the overlay image either fails to load silently (showing no overlay image) or displays a placeholder. The app does not crash.

### TC-EDGE-16: Concurrent gestures with controls visible
**Description:** Verify overlay drag/pinch works correctly when controls are also visible and tappable.
**Steps:**
1. With an overlay loaded and controls visible, drag the overlay near the opacity FAB or session button.
**Expected Result:** Drag gestures on the overlay image area move the overlay. Tapping on control buttons activates those controls. There is no conflict between overlay gestures and button taps.

---

## Test Execution Notes

- **Device recommendations:** Test on at least 2 devices with different screen sizes and Android versions (minimum API 26 and a recent API level).
- **Marker printing:** ArUco 4x4 markers ID #0 and #1 must be printed clearly at approximately 2 cm for tracking tests. The marker sheet can be downloaded from the Setup Guide's external link.
- **Timing tests:** Break reminder tests (Section 9) require waiting for real-time intervals. Use the minimum 5-minute interval to keep test duration reasonable.
- **Fresh install:** Sections 1 and 2 require a fresh install or cleared app data. Document the initial app data state before each test run.

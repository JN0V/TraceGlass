---
stepsCompleted: [1, 2, 3, 4]
session_active: false
workflow_completed: true
inputDocuments: []
session_topic: 'AR Tracing App - Mobile app for drawing assistance using camera overlay'
session_goals: 'Competitive analysis, product vision & differentiation (F-Droid/privacy/UX), feature exploration, UX/technical choices, app naming, produce brief for BMAD project launch'
selected_approach: 'AI-Recommended Techniques'
techniques_used: ['SCAMPER', 'Cross-Pollination']
ideas_generated: [30]
context_file: ''
candidate_app_name: 'TraceGlass'
---

# Brainstorming Session Results

**Facilitator:** JN0V
**Date:** 2026-02-07

## Session Overview

**Topic:** AR Tracing App — Mobile application that overlays a semi-transparent reference image from the gallery onto the live camera feed, enabling users to trace/copy drawings in real life, like digital tracing paper in augmented reality.

**Goals:**
- Competitive analysis — understand existing apps, their strengths/weaknesses
- Product vision & differentiation — F-Droid, no trackers, no ads, free, top-tier UX
- Feature exploration — beyond basic AR overlay
- UX/Design — make the experience truly excellent
- Technical considerations — stack, F-Droid constraints
- App naming — unique, memorable, reflective of function, available on F-Droid

### Session Setup

- **Approach:** AI-Recommended Techniques
- **User context:** First BMAD session, needs guided facilitation
- **Target platform:** F-Droid (Android, open-source, privacy-focused)

## Technique Selection

**Approach:** AI-Recommended Techniques
**Analysis Context:** AR Tracing App with focus on competitive analysis, differentiation, features, UX, naming

**Recommended Techniques:**

- **SCAMPER:** Systematic creativity through seven lenses applied to competitor analysis
- **Cross-Pollination:** Transfer solutions from other domains (photography, gaming, music) + app naming
- **Six Thinking Hats:** Evaluate and converge on final vision from 6 perspectives

## Competitive Analysis

| App | Platform | Price | Strengths | Weaknesses |
|-----|----------|-------|-----------|------------|
| **SketchAR** | Android/iOS | $46/yr or $8/mo | Advanced AR, tutorials, community | Expensive subscription, AR glitches, complex UI, trackers |
| **Da Vinci Eye** | Android/iOS | Paid (top paid 2021) | 2M+ users, multiple modes, filters | Paid, proprietary |
| **Tracing Projector** | iOS only | Free + IAP ($5) | 3 modes (tripod, light table, handheld), 650+ drawings, video | iOS only, not on Android |
| **Draw: Trace & Sketch** | Android | Free + ads | Simple to use | Ads, trackers |
| **DrawAnywhere** (F-Droid) | Android | Free, open-source | Floating canvas over other apps | No camera AR, no image overlay |

**Key finding:** No open-source app on F-Droid does camera-based AR tracing. The niche is empty.

**Common user pain points (from reviews):**
- Holding phone in one hand while drawing with the other = uncomfortable
- AR tracking glitches
- Expensive subscriptions
- Intrusive ads and trackers
- No well-designed "tripod" mode

## Technique Execution Results

### Phase 1: SCAMPER — Systematic Differentiation

#### S — Substitute

**[Substitute #1]**: Open-source economic model
_Concept_: 100% free on F-Droid, funded by Patreon/donations. Future possibility of Google Play Store publication, but not the initial goal.
_Novelty_: No AR tracing competitor is on F-Droid or open-source.

**[Substitute #2]**: Fiducial marker tracking (ArUco/QR type)
_Concept_: Instead of complex AR tracking (ARCore) that glitches, use printable markers that the user places at the corners of their sheet. The system knows exactly where the sheet is and how it's oriented, even if the stand moves.
_Novelty_: Competitors use either ARCore (unstable) or nothing (rigid tripod mode). Fiducial markers offer a robust and unique compromise — and can be provided with the 3D printed stand.

**[Substitute #3]**: Zero outgoing data
_Concept_: Everything stays on the phone. No account, no cloud, no user tracking, no telemetry.
_Novelty_: Radical differentiation vs SketchAR/Da Vinci Eye that require accounts and collect data.

#### C — Combine

**[Combine #1]**: Complete printable AR kit
_Concept_: App + 3D stand model + printable fiducial marker sheet = complete free kit. All downloadable from the app or the repo.
_Novelty_: No competitor provides printable hardware.

**[Combine #2]**: Drawing assistance filters on reference image
_Concept_: Edge detection, grayscale, progressive detail simplification. User starts with main outlines, then adds details.
_Novelty_: Most competitors display the raw image — here we guide the learning process.

**[Combine #3]**: Dual camera AR + light table mode
_Concept_: Two modes in the same app depending on available equipment.
_Novelty_: Tracing Projector does it but is iOS-only. No open-source Android app offers this.

**[Combine #4]**: DIY mode without printer for markers
_Concept_: An in-app guide showing how to draw markers by hand with a ruler — a simple, reproducible pattern. No printer needed.
_Novelty_: Makes the app accessible even without any special equipment.

**[Combine #5]**: "MacGyver Setup" — everyday object stand guide
_Concept_: An in-app assistant suggesting common objects as phone stands: water glass, stack of books, box... with step-by-step visual instructions for each option.
_Novelty_: Competitors just say "use a tripod" without more. Here we truly guide the user through setup with what they have at hand.

**Three entry levels:**
1. **Full DIY** — hand-drawn markers + glass/stack of books
2. **Semi-equipped** — printed markers + everyday object stand
3. **Full kit** — 3D printed stand + printed markers

#### A — Adapt

**[Adapt #1]**: Progressive difficulty levels
_Concept_: Like in a video game — "contours only", "contours + shadows", "full image". Guides drawing learning by stages.
_Novelty_: Competitors display the image as-is. Here we create a real pedagogical journey.

**[Adapt #2]**: Automatic drawing time-lapse
_Concept_: Periodic photo capture of the sheet via camera and generates a time-lapse of the drawing process. Easily shareable.
_Novelty_: Natively integrated, no need for a second app.

**[Adapt #3]**: Multi-accessibility (children, seniors, reduced mobility)
_Concept_: Adapted modes: simplified interface for young users, thicker/higher-contrast guides for motor difficulties, adaptable text sizes.
_Novelty_: No competitor thinks about accessibility.

#### M — Modify

**[Modify #1]**: Minimalist touch controls
_Concept_: Vertical slide on right edge = opacity. Vertical slide on left edge = zoom. No menu, no visible button — screen stays 100% dedicated to camera+overlay view. Double-tap to show/hide overlay quickly.
_Novelty_: Competitors have cluttered UIs with toolbars eating the screen.

**[Modify #1b]**: Contactless gesture control (future v2)
_Concept_: Hand gesture recognition via camera to control the app without touching the screen. Closed fist = pause, open hand = resume, rotation = opacity.
_Novelty_: No competitor does this. "Wow" feature for a v2.

**[Modify #2]**: Zoom/magnifier on detail
_Concept_: Pinch-to-zoom on overlay to work on a detail. Rest of the image fades out.
_Novelty_: Assisted focus that competitors don't have.

**[Modify #3]**: Adaptable overlay color
_Concept_: Choice of overlay tint (blue, red, sepia) to contrast with the drawing medium used.
_Novelty_: Competitors only offer transparency.

**[Modify #4]**: Support size presets
_Concept_: Quick buttons A4, A5, sketchbook to instantly resize the reference image.
_Novelty_: Time savings vs manual pinch-to-zoom.

#### P — Put to other uses

**[Other Use #1]**: Calligraphy / Lettering (v1)
_Concept_: Overlay of letter models for practice. Same mechanics, just a different use case.
_Novelty_: Natural use case, zero additional dev — it's just content/marketing.

**[Other Use #2]**: Body art / fake tattoo (v1)
_Concept_: Project a pattern onto skin to draw fake tattoos, scars, SFX makeup.
_Novelty_: Same mechanics as drawing on paper, but "fun" use case highly shareable on social media. Could be a strong marketing angle.
_Safety note_: MUST include safety guidance — recommend skin-safe tools only (cosmetic makeup, face paint, body-safe markers). Explicitly discourage regular ink pens/permanent markers on skin. This aligns with the project's ethical stance.

**[Other Use #3]**: Mural / fresco (v2+)
_Concept_: Project a model onto a wall for large-scale painting. Requires adapted tracking for vertical surfaces and greater distance.
_Novelty_: Feasible but technical context change — kept for a future version.

#### E — Eliminate

**[Eliminate #1]**: No user accounts — zero signup, zero login.
**[Eliminate #2]**: No built-in drawing library — user uses THEIR images from their gallery. More personal and lighter.
**[Eliminate #3]**: No complex UI — the app does ONE thing and does it well. No menus, AI tutorials, community, shop clutter.
**[Eliminate #4]**: No internet connection required — the app works 100% offline.

#### R — Reverse

**[Reverse #1]**: Corrector mode (v2+)
_Concept_: User draws freehand, then overlays the original to compare differences. Learning tool through error analysis.
_Novelty_: Interesting but workflow needs refinement — kept for later.

**[Reverse #2]**: Inverted transparency mode
_Concept_: A simple toggle: either the image is semi-transparent (classic tracing mode), or the camera is semi-transparent and the reference image is sharp. A "reference study" mode to analyze details before drawing.
_Novelty_: Simple to implement, useful, no competitor offers this.

### Phase 2: Cross-Pollination — Cross-domain Inspiration

#### From Photography

**[CrossPoll #1]**: Composition grids
_Concept_: Superimposable helper grids (rule of thirds, golden ratio) on top of the reference image. Helps position the drawing on the sheet.
_Novelty_: Borrowed from photography apps, not present in any tracing app.

**[CrossPoll #2]**: Before/after slider
_Concept_: A slider to compare "reference image" on the left and "camera/my drawing" on the right. Real-time comparison.
_Novelty_: Common in photo editors, never applied to AR tracing.

#### From Video Games

**[CrossPoll #3]**: Achievement/badge system
_Concept_: "First drawing completed", "10 drawings", "First body art"... Light gamification to motivate, especially for teens.
_Novelty_: No tracing app gamifies the experience.

**[CrossPoll #4]**: Ghost mode (racing games)
_Concept_: Like Mario Kart ghosts — overlay an old drawing on a new one to see progression over time.
_Novelty_: Unique progress visualization for artists.

#### From Music

**[CrossPoll #5]**: "Slow down the track" — progressive complexity
_Concept_: Like slowing down a song to learn it. Simplify the image by complexity levels: basic shapes first, then details, then shadows. Progressive "sheet music" of drawing difficulty.
_Novelty_: Pedagogical approach borrowed from music education.

### App Naming Research

| Name | Availability | Risk |
|------|-------------|------|
| **Lucida** | Problematic | "Lucida" is a registered trademark of Bigelow & Holmes (fonts). "Camera Lucida" already used by multiple apps. Conflict risk. |
| **TraceGlass** | Available | No existing app found. Unique compound word. Descriptive (tracing glass). |
| **GhostDraw** | Available | No existing app. But "ghost draw" returns many ghost drawing results — potential SEO confusion. |
| **Calqo** | Unknown | Short, fun, "calque" + modern sound. |
| **Vetra** | Unknown | From Latin "vetrum" (glass). Original, short. |
| **LightTrace** | Unknown | Light + tracing — evokes light table. |
| **Overlay** | Too generic | Simple but too common a word. |

**Current top candidate: TraceGlass** — available, descriptive, intuitive, no trademark conflict.

### Phase 3: Six Thinking Hats — Evaluation and Convergence

#### White Hat — Facts

- **Market:** AR tracing apps exist (SketchAR, Da Vinci Eye, Tracing Projector) but none on F-Droid
- **Target user:** Teen artist (primary persona), draws on A4 and sketchbooks
- **Device baseline:** OnePlus 5T (Snapdragon 835, 2017) — good performance floor
- **Tech:** Android native, fiducial markers (ArUco via OpenCV — FOSS), standard camera
- **F-Droid constraints:** 100% open-source, no proprietary dependencies (no Google Play Services / ARCore as mandatory dependency)
- **Distribution of physical kit:** Open question — 3D model + marker sheet must not bloat the app. Options: downloadable from repo/website, or linked from in-app guide

#### Red Hat — Emotions / Instinct

- **Excitement:** The printable kit (3D stand + markers) is the most emotionally compelling differentiator — tangible, "unboxing" feel even though it's free
- **Pride:** Delivering pro-quality on F-Droid, no ads, no trackers — feels meaningful beyond code
- **Fun factor:** Body art use case has strongest viral/sharing potential among teens
- **Ethical care:** Safety guidance for body art aligns with the project's values — responsible open-source

#### Yellow Hat — Benefits / Optimism

- **Empty niche on F-Droid** — first mover = reference app
- **Low infrastructure cost** — no backend, no cloud, no accounts. Self-contained app
- **Natural virality** — body art and time-lapse are ultra-shareable content. Free organic marketing
- **Open-source community** — contributors can help create 3D models for different phones, improve filters, translate the app
- **Physical kit as "wow factor"** — gives the app a rare tangible dimension. Memorable

#### Black Hat — Risks / Pessimism

- **Performance on older phones** — OnePlus 5T is from 2017. Camera feed + overlay + marker detection in real-time is resource-intensive. Need a "light" mode without markers (simple fixed tripod)
- **Marker tracking quality** — if it glitches as much as ARCore, we lose our advantage. Must prototype fiducial tracking very early
- **3D model multi-phone diversity** — Android phone size diversity is enormous. A parametric model (OpenSCAD) would be better than a fixed model, but adds complexity for the user
- **Body art liability** — even with warnings, if a teen gets hurt, it could reflect on the project. Safety messaging must be very visible, not a hidden disclaimer

#### Green Hat — Additional Creativity

**[Green #1]**: Parametric 3D model configurator
_Concept_: A simple web configurator where the user enters their phone dimensions and downloads a custom STL. Not in the app, on the project website/repo.
_Novelty_: Solves the multi-phone problem elegantly.

**[Green #2]**: Decorative markers instead of ugly QR codes
_Concept_: Design markers that are themselves visually pleasant — simple recognizable shapes: heart, star, cross, circle, diamond. Each shape is unique for the tracking system to distinguish them, but they look nice on the drawing surface. Can be drawn by hand easily (Full DIY mode).
_Novelty_: Makes the setup aesthetically pleasing instead of technical-looking. Lowers the barrier — a heart or star is fun to draw, a QR code is not.

**[Green #3]**: First session onboarding
_Concept_: A 3-4 screen visual guide: choose your setup → prepare your markers → pick an image → start tracing. Not a 10-page tutorial, just quick visual steps.
_Novelty_: Guided first experience that competitors lack.

#### Blue Hat — Process / Synthesis

- ✅ Product vision: clear and differentiated
- ✅ Candidate name: TraceGlass (verified available)
- ✅ v1 scope: defined with realistic features
- ✅ v2 backlog: strong ideas for the future
- ✅ Risks: identified with mitigations
- ✅ Project values: privacy, accessibility, safety, quality

### Phasing Strategy (User Decision)

All ideas validated but not all for v1. Architecture must not block future features.

**v1 Core:**
- Camera overlay with adjustable opacity
- Fiducial marker tracking
- Image import from gallery
- Minimalist touch controls (edge slides, double-tap)
- Light table mode
- DIY marker guide (hand-drawn)
- MacGyver setup guide (everyday objects)
- 3D printable stand model (downloadable)
- Printable marker sheet
- Edge detection / outline filter
- Support size presets (A4, A5, sketchbook)
- Overlay color choice
- Inverted transparency mode
- Time-lapse capture (periodic photo capture + video generation)
- 100% offline, no account, no tracking

**Target user persona:**
- Teen artist (primary), broader audience (secondary)
- Draws on A4 paper and sketchbooks
- Device: OnePlus 5T (Snapdragon 835, Android — good baseline for performance constraints)

**v2 Future:**
- Progressive difficulty levels
- Composition grids
- Before/after slider
- Achievement system
- Ghost mode (progress comparison)
- Corrector mode
- Contactless gesture control
- Mural/fresco mode
- Accessibility adaptations

## Idea Organization and Prioritization

### Thematic Clustering

**Theme 1: Core AR Tracing Experience**
_Focus: The fundamental drawing assistance functionality_
- Camera overlay with adjustable opacity
- Fiducial marker tracking with decorative markers (heart, star, cross, circle, diamond)
- Image import from gallery
- Inverted transparency mode (toggle reference vs camera transparency)
- Light table mode (alternative to camera mode)
- Support size presets (A4, A5, sketchbook)

**Theme 2: Physical Kit & Accessibility**
_Focus: Making the app accessible regardless of equipment_
- 3D printable phone stand (parametric OpenSCAD model via web configurator)
- Printable decorative marker sheet
- DIY hand-drawn marker guide (no printer needed)
- MacGyver Setup guide (everyday object stands: glass, books, box)
- Three entry levels: Full DIY → Semi-equipped → Full kit

**Theme 3: Image Processing & Drawing Aids**
_Focus: Helping the user draw better_
- Edge detection / outline filter
- Overlay color choice (blue, red, sepia — contrast with drawing medium)
- Minimalist touch controls (edge slides for opacity/zoom, double-tap toggle)
- Zoom/magnifier on detail with fade-out of surrounding area

**Theme 4: Capture & Sharing**
_Focus: Recording and sharing the creative process_
- Time-lapse capture (periodic photo + video generation)
- Body art / fake tattoo use case (with skin-safe tool safety guidance)
- Calligraphy / lettering use case

**Theme 5: Privacy & Ethics**
_Focus: The project's core values_
- 100% offline, no account, no tracking, no telemetry
- Open-source on F-Droid, no proprietary dependencies
- Body art safety messaging (recommend cosmetic makeup, face paint, body-safe markers only)
- Patreon/donation funding model

**Theme 6: Onboarding & UX**
_Focus: First-time and ongoing user experience_
- First session onboarding (3-4 visual screens)
- Setup wizard (choose your setup level)
- Clutter-free UI — one purpose, zero bloat

### Cross-cutting Breakthrough Concepts

- **Decorative fiducial markers** — transforms a technical requirement into a delightful UX element. Simple shapes (heart, star, cross, circle, diamond) that are fun to draw by hand AND technically functional for tracking
- **Physical kit as differentiator** — no software competitor provides tangible, printable hardware. Creates an emotional "unboxing" experience even though it's free and open-source
- **Three-tier accessibility** — Full DIY / Semi-equipped / Full kit ensures zero barrier to entry regardless of the user's equipment

### Prioritization Results

**Top Priority (v1 Must-Have):**
1. Camera overlay + opacity control — the core value proposition
2. Fiducial marker tracking with decorative markers — the key technical differentiator
3. Image import from gallery — essential input
4. Minimalist touch controls — the UX differentiator
5. Edge detection / outline filter — immediate drawing aid value
6. Time-lapse capture — high user value, low dev cost
7. First session onboarding — critical for adoption
8. 100% offline, no tracking — non-negotiable project value

**High Priority (v1 Should-Have):**
9. Light table mode — alternative use mode, broadens user base
10. Overlay color choice — small effort, meaningful UX improvement
11. Inverted transparency mode — simple toggle, unique feature
12. Support size presets — convenience feature
13. Zoom/magnifier on detail — drawing aid
14. MacGyver Setup guide — accessibility
15. DIY marker guide — accessibility
16. 3D printable stand model — key differentiator (hosted externally)
17. Printable marker sheet — companion to tracking

**v2 Backlog (Architecture Must Not Block):**
18. Progressive difficulty levels
19. Composition grids (rule of thirds, golden ratio)
20. Before/after comparison slider
21. Achievement/badge system (gamification)
22. Ghost mode (overlay old drawing for progress tracking)
23. Corrector mode (freehand then compare)
24. Contactless gesture control (hand recognition)
25. Mural/fresco mode (vertical surface, large scale)
26. Accessibility adaptations (motor difficulties, seniors)
27. Parametric 3D model web configurator

### Action Planning

**Immediate Next Steps:**
1. **Validate the name "TraceGlass"** — check domain availability, F-Droid package name availability
2. **Prototype fiducial marker tracking** — build a minimal Android app that detects decorative markers via OpenCV. This is the highest technical risk item and must be validated first
3. **Design the decorative markers** — create 4-5 simple shapes that are visually distinct and trackable
4. **Set up the project** — create the repo, choose the tech stack (Kotlin + Jetpack Compose + OpenCV), initialize the BMAD workflow for full project planning (PRD, architecture, stories)

**Technical Risks to Validate Early:**
- OpenCV fiducial detection performance on Snapdragon 835
- Camera feed + overlay rendering at acceptable frame rate
- Decorative marker shapes vs standard ArUco markers — custom marker design feasibility

**Open Questions for Next Phase:**
- Final app name confirmation (TraceGlass or alternative)
- Tech stack decision (Kotlin native vs Flutter vs other)
- License choice (GPL, MIT, Apache?)
- How to host/distribute the 3D model and marker sheet (repo, website, in-app link?)

## Session Summary and Insights

**Session Statistics:**
- **Total ideas generated:** 30+
- **Techniques used:** SCAMPER, Cross-Pollination, Six Thinking Hats
- **Themes identified:** 6
- **v1 features defined:** 17
- **v2 backlog items:** 10
- **Risks identified:** 4
- **Duration:** ~50 minutes

**Key Achievements:**
- Complete competitive analysis revealing an empty F-Droid niche
- Strong product vision with clear differentiation on multiple axes (open-source, privacy, physical kit, decorative markers, minimalist UX)
- Candidate app name verified (TraceGlass)
- Phased roadmap (v1/v2) with architecture-aware backlog
- Ethical considerations addressed (body art safety)
- Unique "three-tier accessibility" approach (Full DIY → Semi-equipped → Full kit)

**Creative Breakthroughs:**
- Decorative fiducial markers (technical requirement → delightful UX)
- Printable physical kit as software differentiator
- Body art as viral marketing angle (with safety guidance)
- MacGyver Setup guide (empathetic onboarding)

**Session Reflections:**
This brainstorming session produced a comprehensive and differentiated product vision for TraceGlass. The strongest insights came from combining technical constraints with user empathy — the decorative markers and three-tier accessibility being prime examples. The project has a clear identity: a privacy-respecting, open-source AR tracing app that treats its users with respect and creativity.

**Recommended Next BMAD Workflow:** Launch the full BMAD project lifecycle — start with the PM agent to create a PRD based on this brainstorming output, then proceed to architecture, stories, and development.

## PRD Divergences (Updated 2026-02-08)

The following decisions were made during PRD creation that diverge from or refine the original brainstorming output:

### Technology Stack (Confirmed)
- **Kotlin native** confirmed over Flutter, TypeScript+Capacitor, and Tauri 2.0
- Rationale: real-time camera pipeline requires zero-overhead CameraX access and native OpenCV via JNI

### API Level (Changed)
- **Original:** API 26 (Android 8.0) implied by OnePlus 5T baseline
- **PRD decision:** API 33 (Android 13) — enables Photo Picker (no storage permission needed) and simplifies permission model to CAMERA only. Aligns with F-Droid privacy philosophy.

### Light Table Mode (Removed)
- **Original:** Listed as v1 feature
- **PRD decision:** Replaced by automatic adaptive behavior — if markers detected → tracking mode, if no markers → fixed overlay (user positions manually). No separate "mode" needed.

### Edge Detection / Outline Filter (Moved to V2)
- **Original:** v1 Must-Have (#5)
- **PRD decision:** Moved to Phase 2 to reduce MVP scope for solo developer

### Body Art Mode (Moved to V2)
- **Original:** Discussed as v1 use case
- **PRD decision:** Moved to Phase 2 — requires markerless handheld mode which is technically different from fiducial tracking pipeline

### Paper Size Presets (Removed)
- **Original:** v1 feature
- **PRD decision:** Removed entirely — marker spacing provides automatic scale detection, manual positioning via pinch/drag covers non-marker use. No need for user to know their paper size.

### Decorative Markers (Simplified for MVP)
- **Original:** 5 shapes (heart, star, cross, circle, diamond)
- **PRD decision:** Start with simplest reliable shapes for MVP. Additional decorative shapes in Phase 2. Reliability over variety.

### Phasing (Restructured)
- **Original:** v1 / v2
- **PRD decision:** Phase 1 (MVP) / Phase 2 (Growth) / Phase 3 (Vision). Original v2 items moved to Phase 3.

### Session Persistence (Added in PRD)
- **Not in brainstorming:** Pause/resume capability for tracing sessions and time-lapse. Critical for real-world usage (30-45 min sessions, phone calls, breaks).

### Flashlight Toggle (Added in PRD)
- **Not in brainstorming:** Simple LED flash toggle for drawing in low-light environments. Trivial to implement.

### Drawing-as-Reference Realignment (Added in PRD)
- **Not in brainstorming:** For body art mode (Phase 2), use OpenCV feature matching (ORB/AKAZE) on partially traced drawing to realign overlay after a pause. Enables user to leave and return without needing exact repositioning. Generalized hybrid tracking (markers + drawing features) considered for Phase 3 paper mode.

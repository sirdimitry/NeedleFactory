# Stage 2 — First Automation Line Report

**ACCEPTED / COMPLETED — 2026-10-05.** The owner accepted the current Stage 2
after a physical playtest on Samsung SM-S938B and authorized its closing commit
and push. Stages 0, 1 and 2 are complete. Stage 3 remains unimplemented and requires
separate explicit owner authorization.

Responsive scale correction completed: Samsung comparison identified a
double landscape reduction (480 versus 400 viewport units per short edge, plus
220 versus 248 table units). A bounded presentation refactor replaces this with
one orientation-independent canvas scale and fixed-size modules; anchors and
transfer geometry absorb orientation changes. No simulation/economy refactor.

## Status and authorization
Final owner-feedback / Toolkit pass, 2026-10-05. main was safely fast-forwarded
to origin/main **cc5b231**. Uncommitted Stage 2 work was preserved with
stash/apply; the integration had no conflicts and the insurance stash remains.
Before changes, reread AGENTS.md, STAGE_2.md, SKILLS.md, STAGE_1.md,
STAGE_1_REPORT.md, ARCHITECTURE.md, ART_BIBLE.md and ROADMAP.md.
For this continuation, reread the updated AGENTS.md, STAGE_2.md, SKILLS.md,
NEEDLEFACTORY_GAMEDEV_TOOLKIT.md, UNIVERSAL_MODDER.md and this local report.

**Closing Toolkit profiles: QA_VERIFY + ANDROID_PLATFORM + GAMEPLAY_CORE.**
Implementation passes also used PHYSICS_TOUCH, VISUAL_ART, UI_UX and PERFORMANCE.
Project-local routing and the verification-oracle
method are applied; no external plugin, service or runtime dependency was installed.
Only Stage 2 is implemented. Stages 0/1/2 are **accepted/completed**. Stage 3,
economy, unlock implementation, saves and offline production remain locked.
Owner confirmation for the final Stage 2 commit/push has been received.

Latest owner pass, 2026-10-05: accepted needle size retained; both polished tint and
reflection require >60% rendered straw-uncovered exposure. Late payoff now grows
through 75%, 90% and full exposure; see the latest late-reveal section below.
The separate GAMEPLAY_ECONOMY_PROPOSAL.md is owner-requested preparatory design,
not Stage 3 implementation. Current responsive physical smoke passed on Samsung:
same-state portrait/landscape captures, zero module-scale difference, wide slow/fast
manual input, paused rotation and Home/resume retention. The owner subsequently
accepted the current iteration after physical testing, closing the Stage 2 review
gate. No new quantitative sustained thermal/FPS measurement was supplied with
acceptance. An earlier cold/idle attempt was interrupted
by external touches and subsequent loss of game focus. It is not a passed smoke.
Earlier automatically fed builds do not verify this final artifact.

## Final owner playtest acceptance — Samsung SM-S938B

The following records the owner's final acceptance of the current Stage 2,
separately from the instrumented agent checks and historical iterations below.
No runtime/gameplay code changes were made during closure. Staged-file whitespace
validation also removed one trailing space from each copy of the font license;
the license text, font assets and accepted visuals remain unchanged.

| Acceptance criterion | Final result / supporting evidence |
| --- | --- |
| Portrait/landscape gameplay scale and search-field screen size | Owner accepted. Instrumented Samsung comparison: field **669.60 × 535.68 px** in both orientations, **0% scale difference**; module unit **2.7 px** in both. |
| Needle/straw/machine perceived scale | Owner accepted. Same module transform; needle axis **87.048 px**, width **2.45528 px**, representative straw width **1.674 px** in both orientations. No landscape-only needle compensation. |
| Manual feed | Owner accepted. Material is pushed into the mouth by touch; no starting auto-feeder or idle suction. Native idle and slow/fast tests provide independent regression coverage. |
| Fullscreen / white edges | Owner accepted in portrait/landscape. Earlier instrumented Samsung cold-launch, rotation and Home/resume screenshots showed zero near-white outer-edge pixels. Full-window GL coverage remains unchanged. |
| Rotation / background / resume | Owner accepted. Active batch, machine states, counters and manual eligibility retained. Earlier same-PID paused rotation/Home checks retained an exact domain summary. |
| Needle visibility / metallic payoff | Owner accepted. No highlight at **<=60%** uncovered length; gradual internal polished-metal reflection above 60%, with stronger late payoff after 75% and 90%. Dimensions unchanged. |
| Several complete batches / conservation | Owner accepted. Exact physical batch count was not supplied in the closing message. Separately, the final automated GL smoke checks ten complete batches with exactly-once routing/counters. |
| Performance / visual judgement | Current iteration accepted on the target phone. No new numeric FPS, frame-time, jank or temperature values were supplied; historical mixed samples below are not a final benchmark. |

No blocker was reported in the final owner acceptance. Sustained thermal testing
and profiling other devices remain limitations, not claims of measured success.

## Closing verification and preserved project documents

- Fresh `:core:test --rerun-tasks`: **42 tests, zero failures/errors**.
- Final `:lwjgl3:assemble`, `:android:assembleDebug`, `:android:assembleRelease`
  (unsigned release) and `:android:lintDebug`: **BUILD SUCCESSFUL**;
  lint **0 errors / 5 existing version warnings**.
- Fresh `:lwjgl3:factorySmoke`: **PASS — 10 batches / 9 packages / 1 intentional
  miss / 9 haptics / 31,126 frames**, with item conservation and lifecycle/layout
  assertions enabled. Fresh `:lwjgl3:prototypeSmoke`: **PASS — 10 Stage 1 rounds /
  34,241 frames**, including blind-tap rejection, physical clearing, next,
  pause/resume and rotation.
- Installed owner-review debug APK SHA-256:
  **053CDC1B1AF58A999CCC7336617EE1C9469ACB823AC5EF2352083012CC06FCE8**,
  No new physical sanity run was necessary: closure changed documentation and
  license whitespace only, after owner physical acceptance. The final APK is
  repackaged with that license whitespace normalization; its hash is recorded below.
- Final repackaged debug APK SHA-256:
  **D0CD9567C06CBC3A5C6791FB5B42C7AD4AADF45B5137EAF590F3A63CC53A572A**.
  Desktop/debug/unsigned-release assembly and lint passed again after license
  normalization (0 errors / 5 existing warnings). Compilation remained up-to-date;
  no gameplay code, textures, layout or tuning changed.
- `git fetch origin main`: pre-closure HEAD and origin/main both **cc5b231**;
  all local Stage 2 work retained. No history rewrite or rollback.
- Whitespace checks passed. Candidate paths were audited: build/APK output,
  verification logs/screenshots, local SDK settings and signing material remain
  ignored. Secret-pattern and local SDK-path scans found no matches.
- Runtime font assets are intentional, reproducible project inputs with the
  Barlow OFL license, source font, provenance note and offline generator.
- Preserved `GAMEPLAY_ECONOMY_PROPOSAL.md` as **proposal only**. No multi-needle,
  wallet, rare-item, upgrade or economy runtime was added.
- Updated this report, AGENTS.md, README.md, ROADMAP.md and the Stage 2/Toolkit
  authorization notes. ARCHITECTURE.md and ART_BIBLE.md retain the accepted
  lifecycle, module-scale, material and presentation contracts. Existing Stage 1
  acceptance reports remain intact.

The later iteration sections preserve the development record. Their historical
pending/stop-point notes describe checks at that time; this final acceptance and
closure authorization supersede those review gates.

Reproduction (documented JDK and Android SDK configured):

```powershell
.\gradlew.bat :core:test --rerun-tasks --console=plain
.\gradlew.bat :core:test :lwjgl3:assemble :android:assembleDebug :android:lintDebug :android:assembleRelease :lwjgl3:factorySmoke :lwjgl3:prototypeSmoke --console=plain
```

## Manual feeding and progression
The latest owner correction supersedes the original hands-off intake scope.
Starting loop: **manual physical feed → automatic downstream processing**.
Assisted feed, partial automation and a full automatic feeder are future
progression rewards; no unlock/shop is implemented.

- Removed rightward intake force and sinusoidal material drive. Without touch,
  all 321 items stay on the table; the running belt cannot suck them in.
- A bounded Box2D finger physically pushes bodies. Downstream-directed contact
  arms only the contacted item. A blind stationary tap does not feed material.
- An armed item must physically reach the right edge for a metered BELT handoff.
  Already delivered items proceed hands-off; no timer-only intake travel/refill.
- Feed gate holds/releases hand-delivered material; its closed stop is visible.
  Internal legacy identifier VIBRATION is retained. Decorative table-motor
  movement supplies no automatic feed force.
- Per-item manual eligibility, poses/velocities and feed-meter remainder live in
  the portable session. Pause, rotation and native rehydration retain them.
  The current gesture is safely cancelled on lifecycle changes.
- HUD identifies MANUAL FEED and asks the player to push right into the mouth.

Batch: 320 straw patches and one needle. Belt transport, magnetic capture at
55% progress, chute/buffer, jaw cycle and ejection remain domain-owned.
Recovered/Missed/Packaged increment once per needle. Next requires all 321
items to reach OUTPUT/RESIDUE/MISSED. Counters are diagnostics, not money.
A package requests one system-respecting haptic.

Global Pause freezes the whole simulation. Gate off holds hand-fed material;
belt off holds feed/transport; magnet off causes a visible miss for uncaptured
metal; packer off holds buffer/jaws. Already latched capture remains committed.

## Fullscreen / white-strip fix
The previous light Activity theme and root padding around the GL SurfaceView
exposed white window/inset regions that changed sides on rotation.

- Dark application theme, window/content and Android 12+ launch background
  match the renderer exactly: #101E24.
- Root content padding removed. GL Surface fills the whole window, including
  cutout/system-bar regions; decor fitting is disabled and cutout render allowed.
- Transparent bars, light icons and disabled contrast scrims on supported APIs.
  System navigation remains available; forced immersive navigation hiding is
  not used.
- Portable SafeArea passes pixel obstructions onto the GL thread. Full-window
  viewport/background stays full size; only content coordinates shift into the
  safe rectangle. Clear covers the entire framebuffer; input shares that camera.
- Insets/window appearance refresh after configuration changes and resume.

Android's [edge-to-edge setup](https://developer.android.com/develop/ui/views/layout/edge-to-edge-manually)
and [cutout guidance](https://developer.android.com/develop/ui/views/layout/display-cutout)
inform this separation of background coverage and safe important content.
GL and current Samsung screenshots include safe insets; the responsive pass
confirms dark edges after portrait launch, rotation and paused Home/resume.

## Material, difficulty and complete visual pass
- Physical needle stays **1.3 m long × 0.018 m wide**, metallic grey, tapered sharp
  tip and small dark eye. The latest readability correction adds 1.25 physical
  pixels only to rendered width and a short internal attitude-driven reflection.
  No glow or length pulse.
- Increased 288 × 9 = 2,592 visual strands to **320 × 12 = 3,840**, approximately
  48% more strands for 32 extra physical bodies.
- Straw lengths 2.7–4.0 m, typical width 0.021–0.031 m; deliberate cover 0.036 m.
  Smooth large-radius quadratic bends, tapered ends and continuous tangents.
  Crossing angles, local separation, lengths, bends and warm tones vary.
- Broad irregular coverage, dense pockets and peripheral pieces. Bounds now use
  each rotated strand envelope, avoiding oversized invisible patch margins that
  previously pushed material away from tray edges.
- Needle starts upstream under deliberate cover. Five seeded native tests retain
  at least half its centreline coverage after one broad swipe. Repeated physical
  pushes drain all tested seeds. Subjective difficulty remains an owner decision.
- Belt perspective compresses loose loads into its bed while retaining crossing
  angles; no forced parallel brush alignment. Discharge tips material into its bin.
- Receiving-pan walls now end at actual drum shaft ends in both orientations,
  correcting detached diagonal guards. Pan thickness, bolts and feet join intake.
- Belt has rubber bed, return depth, side rails, drums/bearings, drive housing,
  trestles and moving ribs. Flanged supported channels join all downstream nodes.
- Magnet yoke/coil/gantry spans the capture path. Carriage and needle now use the
  same interpolated smooth trajectory instead of drifting apart during capture.
- Buffer has folded tapered hopper walls, stop pocket and pneumatic ram. Packer
  has open infeed/outfeed, rollers, reel, threaded wrapper web, closing jaws,
  drive vents and inspection-frame fasteners. Loading aligns before jaw closure.
- One extruded plinth, shared restrained teal/steel/rubber/copper palette,
  contact shadows, front faces and supports unify scale/depth.

Technical-art, industrial/motion-design and UI/UX disciplines were applied with
reference-only MIT [game-ui-design guidance](https://github.com/jeremylongworth-source/AgentSkills/tree/main/skills/game-ui-design).
No skill code/runtime framework is added. OFL Barlow-derived bitmap typography
loads asynchronously: 1024 × 512 RGBA atlas, 2 MiB; source, license, SHA-256 and
offline generation notes are tracked with the asset.

## Portrait / landscape and HUD
Portrait stacks intake, vertical belt and returning packing branch. Landscape
uses a compact two-level return-flow workshop with a separate side console.
Modules centre on extra-wide screens; the landscape belt is capped at 214 UI units.
Search tray is 248 x 198.4 units in both layouts, with unit-1 main machines.
Samsung canvas density is 2.7 pixels per unit in both orientations. Landscape
identity/counters use the free bay beneath the tray; side controls stay compact.

Landscape toggles remain 78 × 60 UI units and actions 164 × 52. Portrait uses
compact bottom controls. Grouped counters, Packaged emphasis, explicit ON/OFF,
Run/Pause icons and disabled Next states communicate hierarchy beyond color.
Telemetry is a collapsible i drawer in reserved console space. Labels sit on
machine nameplates. Safe-area handling applies independently to both layouts.

## Architecture and tunables
FactorySession/FactoryBatch own state independently from rendering. Box2D is
restricted to intake; downstream transport is deterministic fixed-step Kotlin.
FactoryLayout shares machine attachment points with rendered trajectories.
Stage 1 remains an unchanged desktop reference. No ECS/DI/new runtime dependency,
render-thread file/network work, economy or save system.

| Tunable | Final value |
| --- | --- |
| Intake | 10 × 8 m; 320 physical patches / 3,840 strands / one needle |
| Native budget | 322 initial bodies, falling to one inactive finger |
| Fixed step / catch-up | 60 Hz / 5 steps / 0.1 s delta cap |
| Straw contact | Three tangent-aligned fixtures per patch; finger only |
| Needle | Physical 1.3 × 0.018 m; rendered width +1.25 physical pixels |
| Reflection gate | Off at <=60%; smooth gain 0.32 at 75%, 0.70 at 90%, 1.0 at 100% |
| Visibility | 31 axis samples, rendered curved straw, conservative 0.25-pixel padding |
| Finger | Radius 0.62 m (was 0.42); maximum 10 m/s |
| Material limits | 7 m/s; angular cap 2.5 rad/s (was 4); damping 10 (was 6) |
| Idle intake force | Zero; no feeder or directional assistance |
| Gate metering | 0.115 s minimum handoff interval; retained remainder |
| Belt / capture / chute | 5 s / 1.1 s / 1.2 s |
| Jaw / ejection | 1.8 s / 1 s |
| Scripted manual duration | About 49–61 active seconds; not a human benchmark |

Game owns the portable session across screen/GL recreation. Android
non-configuration retention preserves it while the process lives. Lifecycle
changes cancel touch, discard clock remainder and synchronize snapshots without
resetting batch, machines, counters or manual state. No background catch-up.
True process death/cold start begins a new session.

## Automated verification
- **42 JVM tests passed**, zero failures: retained Stage 0/1 tests, generation,
  manual eligibility/gate pause retention, coverage, native slow/fast control,
  rehydration, bounded bodies, manual drains and 30/60/120 cadence consistency;
  four additional lighting tests cover filtering, screen-axis mapping, fallback,
  bounded reflection and physical-pixel width; four visibility tests check
  the strict threshold, curved/projected coverage, immediate re-cover shutdown
  and continuous monotonic late payoff through 75%, 90% and full exposure.
  Four canvas tests check rotation scale, realistic safe-area variants, compact
  device aspect ratios and bar-axis-independent calibration without landscape-only zoom.
- Five native idle seeds: 60 seconds without input retain 321 intake items,
  zero armed/recovered/packaged items and poses within native float tolerance.
- Desktop assembly and Android debug/unsigned release assembly passed.
- **Lint: 0 errors, 5 existing version warnings.** Matching pinned libGDX/Box2D
  retained; no unrelated migration.
- Real OpenGL/Box2D: **10 batches, 9 packages, 1 intentional miss,
  9 haptic callbacks, 31,126 frames** through actual screen gestures and physical
  gate. Checks idle no-feed, held buffer, pause, rotation, lifecycle callbacks
  and retained-session screen reconstruction.
- Runtime harness uses a hidden GL window to exclude real desktop mouse events,
  and releases its synthetic finger before Next. Gameplay assertions stay active.
- Real GL screenshots inspected: 1080 × 2340 / 2340 × 1080, with safe insets;
  initial pile, flow, capture, buffer, package/miss, pause and telemetry.
  Captures assert unchanged summaries. Synthetic-time FPS is not a device benchmark.
- Final whitespace/conflict-marker check: git diff --check passed.

## Historical field notes: symptom -> cause -> fix -> verification

These entries preserve the intermediate test gates. Final physical acceptance
and current verification results are recorded above; old pending notes below do
not reopen the accepted milestone.

| Symptom | Cause | Fix | Verification / acceptance oracle |
| --- | --- | --- | --- |
| White strips move between top/side after rotation. | Light Activity/window background and inset padding expose space outside the GL Surface. | Matching dark theme/window/launch backgrounds; unpadded full-window Surface; edge-to-edge/cutout support; safe content camera independent of full-window clear. | Phone-size GL captures cover both orientations. Final Samsung cold launch, rotation and resume must have no white edge/gutter; inspect actual screenshots, not only pixel statistics. Physical result pending. |
| Running line sucks in a fresh pile without interaction. | Directional intake force substituted for manual progression. | Removed intake drive; downstream-directed finger contact arms individual items, which must physically reach the metered mouth. | Five native seeds remain idle for 60 s with 321 INTAKE / zero armed / zero packages. Final Samsung idle and slow/fast manual gestures pending. |
| Pile is sparse and one broad gesture makes search trivial. | Too few rendered strands and broad regular gaps. | 320 patches with 12 varied strands each; irregular coverage and dense pockets; upstream concealed needle. | Five seeded native tests retain at least half the needle centreline coverage after one swipe and remain drainable. Search satisfaction/difficulty requires owner playtest. |
| Straw resembles sticks or parallel brushes, with empty tray margins. | Thick short geometry, aligned transfer drawing and overestimated rotated bounds. | Thin long smooth strands with varied angles/bends; crossing transfer angles; per-strand envelope bounds. | Seeded geometry/bounds tests and inspected GL start/flow captures. Samsung visual judgement pending; individual fibres still share patch physics. |
| Needle became almost invisible after strict thinning. | Sub-pixel muted flat shading loses exposed metal against dense straw at phone scale. | Add 1.25 physical pixels to visual width; muted body with a small filtered attitude-driven internal specular reflection. Length/physics/occlusion unchanged. | Four lighting tests, phone-resolution GL tilt pairs and manual-drain/coverage regression tests; physical sensor feel and search balance require Samsung owner review. |
| Reflection is hard to read and must not assist early search. | Previous reflection lacked an exposure gate and had a very narrow dim band. | Keep accepted size/body color; compute rendered straw-uncovered fraction, omit reflection at <=60%, fade in at 60–75%; modestly widen/brighten only the internal specular band. | 37 JVM tests; actual GL below-threshold tilt pairs have zero changed pixels, above-threshold pairs respond; Samsung sensor/subjective check pending. |
| Intake guard endpoints float; captured metal drifts away from carriage. | Guard geometry did not use drum endpoints; carriage and item used different interpolation. | Shared drum attachment points and smooth capture trajectory; supported pan, flanged channels and integrated machine housings. | Shared-layout tests and GL flow/capture/buffer/package frames. Final Samsung handoff screenshots pending. |
| Landscape stretches controls and the factory into a long strip. | Portrait-derived wide layout lacks independent composition. | Two-level return-flow workshop, capped belt, centred modules and compact side console. | Aspect matrix tests and 2340 x 1080 GL capture; Samsung landscape visual/touch verification pending. |
| Lifecycle risks resetting manual eligibility or accumulating background simulation. | Native bodies/gesture clocks must not own persistent gameplay truth. | Session retains items, manual flags, gate remainder, machines and counters; cancel gesture and discard lifecycle clock remainder. | Native rehydration, pause/cadence tests and retained-screen GL smoke. Final Samsung same-PID paused exact-state and running no-catch-up checks pending. |
| Desktop smoke was interrupted by real mouse input or retained synthetic finger. | Visible test window accepts external events; Next boundary lacked finger release. | Hidden GL smoke window and explicit release before Next. | Full actual-input OpenGL smoke passed 10 batches / 31,620 frames; gameplay assertions remain enabled. |

Physical evidence is collected separately from synthetic-time desktop evidence.
The device oracle records the foreground/lock state, process ID, domain summary,
orientation and screenshots. It stops if the game loses foreground focus, so a
concurrent owner interaction cannot silently count as a successful test.

## Previous owner correction: needle readability and metallic reflection

This section preserves evidence for the earlier 1.25-pixel correction. The latest
threshold pass below supersedes its reflection color/width and ungated behavior.

**Symptom -> cause -> fix -> verification:** nearly invisible exposed needle ->
flat subdued shading and very narrow pixel footprint -> slightly wider metal body
and a tilt-responsive internal reflection -> 34 JVM tests/builds/lint pass;
real GL comparison passes; physical owner judgement remains pending.

### Width and visual logic
- Length stays 1.3 m; Box2D fixtures, search geometry, seed generation, material
  coverage, feed eligibility and counters are unchanged.
- Rendered width is the previous width plus **1.25 actual framebuffer pixels**,
  converted through the current full-window viewport scale. At Samsung-resolution
  layouts this is approximately 2.3–2.5 px in portrait and 2.1–2.2 px in landscape,
  slightly wider than typical individual straw (approximately 1.3–2.1 px).
- Base grey is RGB (0.47, 0.51, 0.50). A dark lower facet and two tapered triangles
  form a short internal metallic reflection; maximum RGB (0.67, 0.69, 0.67).
  Its centre slides within -0.22 to +0.14 of shaft length, and its span remains
  inside the shaft, away from the sharp tip and eye. No outside outline, halo,
  bloom, neon, timer pulse or extension of the needle.

### Attitude input and noise filtering
- Android owns the optional SensorEventListener. Priority: **TYPE_ROTATION_VECTOR
  -> TYPE_GAME_ROTATION_VECTOR -> TYPE_GRAVITY -> TYPE_ACCELEROMETER**. Registration
  failure tries the next available sensor; no sensor gives a neutral static reflection.
- Rotation-vector input is converted with getRotationMatrixFromVector; the world-up
  projection onto device X/Y supplies tilt. Gravity/accelerometer fallback uses
  components divided by GRAVITY_EARTH. Display.rotation remaps axes for all four
  screen rotations, following Android's [sensor coordinate guidance](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview).
- Requested interval: 30,000 microseconds (about 33 Hz), no high-rate permission.
  Listener registers on resume and unregisters on pause/destroy. It never moves
  material or modifies the session.
- One packed volatile Long transfers the latest finite, bounded X/Y sample to GL.
  No Runnable/sample objects or queues per event; one reusable 3x3 matrix.
  GL applies a **180 ms exponential low-pass**, delta capped at 100 ms. Filtering
  is frame-rate independent at 30/60/120 Hz; invalid input is ignored. There is no
  Euler-angle wraparound and no time-only animation. Static device/static fallback
  produces a static highlight.
- The procedural model approximates a broad cylindrical specular lobe from tilt
  across the shaft; projected tilt along it shifts a finite light reflection.
  It is a restrained shading approximation, not a full environment-light shader.

### Difficulty / performance oracle
- Draw order remains metal **under all intake straw**. The reflection is part of
  the metal silhouette and cannot shine through cover; moving the phone does not
  expose or feed anything. Dense 3,840-strand coverage remains in place.
- Width helps already exposed metal read against straw; low-contrast bounded
  reflection rewards careful viewing instead of providing an always-visible marker.
- Existing five-seed native tests still retain at least half the needle centreline
  coverage after one broad swipe and drain through deliberate repeated manual pushes.
  This establishes regression bounds, not subjective proof that search is balanced.
- Added work is one scalar filter and a few triangles for the single needle;
  no shader, texture, render target or per-strand lighting was added to the game.
  Phone FPS/jank and real sensor jitter still require an uninterrupted owner test.

### Real OpenGL evidence for this correction
- Full runtime rerun: **10 batches / 9 packages / 1 intentional miss / 9 haptics /
  31,621 frames**, with the actual manual finger and Box2D gate.
- Real 1080 x 2340 / 2340 x 1080 captures compare two injected, filtered attitudes
  at identical session state. The capture helper asserts that lighting/capture
  does not change the session summary. This is desktop GL, not physical sensor evidence.
- Fresh concealed seed 42: **zero changed pixels** in both orientations when tilt
  changes. Fully covered metal does not leak a locating signal.
- After **11 real manual gestures**, seed 42 still had 0.41935 centreline coverage
  while the needle remained INTAKE. The partial-needle tilt pairs differ in only
  **13 portrait / 8 landscape pixels**, inside the exposed needle. Captured/open
  metal pairs differ in **10 portrait / 33 landscape pixels**. No straw/HUD/machine
  pixels change from the lighting input.
- Inspected the partial and captured phone-resolution frames: metal remains
  slender, fragmented by straw cover, with a subdued short reflection and no halo.
  The partial pose is reached through player-style feeding, not hand-edited items.
- Reproducible artifacts (ignored local QA output):
  verification/stage2-needle-{hidden,partial,metal}-{portrait,landscape}-{left,right}.png
  and verification/stage2-needle-smoke.log. Two-attitude pixel counts are a
  visibility/occlusion oracle, not a subjective search-difficulty benchmark.

Owner oracle: expose part of a needle using several deliberate moves, slowly tilt
the phone in portrait and landscape, and judge whether a small stable metallic
reflection assists recognition without revealing the still-covered metal. Repeat
while paused and after background/resume; tilt alone must leave batch/routes/
machines/counters unchanged. A fresh pile must still demand search/manual feeding.

## Previous owner pass: highlight only above 60% exposure + design proposal

Historical verification for the previous APK; the late-reveal pass below supersedes
its reflection style and gain curve, retaining the same visibility estimator.

### Symptom -> cause -> fix -> verification
Weak reflection / risk of assisting before recognition -> narrow dim band with no
explicit exposure gate -> keep approved needle size and add a strict rendered-cover
threshold, then a stronger soft internal reflection -> 37 JVM tests, builds/lint,
10-batch GL smoke and paired orientation evidence; physical Samsung check pending.

- **No size change:** physical 1.3 x 0.018 m and the existing +1.25 rendered-pixel
  width remain unchanged; subdued RGB (0.47, 0.51, 0.50), tip and eye retained.
- `NeedleVisibility` samples **31 equally spaced points along the rendered needle
  axis**. Each point is tested against individually curved/tapered straw ribbons,
  not Box2D rectangles or a whole opaque tuft. Overlap counts once. This estimates
  exposed length; it is not a full silhouette/pixel-coverage percentage.
- Occluders use `locateItem` interpolation, exact same shared `projectedStrawScale`
  as drawing, strand offsets/angles/bends, ribbon widths and tip taper. Includes
  infeed/belt/discharge straw, not only INTAKE. Rendering projection may therefore
  legitimately give different exposure across authored portrait/landscape layouts.
- A conservative 0.25 physical-pixel cover padding accounts for raster/tessellation
  edges. Broad-phase cached circles, squared distance tests, cached strand basis
  and reusable arrays reject distant cover; sample loops are indexed/allocation-free.
  Geometry is reevaluated every frame, avoiding stale eligibility during fast drags.
- **If visibility <=0.60: zero reflection triangles are emitted.** First eligible
  axis-sample count is 19/31 (61.29%). Smoothstep gain across (0.60, 0.75) prevents
  a large binary pop; no below-threshold hysteresis, no lingering fade when re-covered.
  Raised CAPTURE metal is above straw and treated as exposed. Other visible routes
  use current rendered cover. This changes presentation only, never discovery,
  feed permissions, transport, counters or the native body.
- Sensor priority, 33 Hz request, display-axis mapping, 180 ms tilt filtering and
  pause/unregister behavior are unchanged. No sensors: calm static metal reflection
  **still respects the same visibility gate**.
- Above threshold, the internal tapered band spans 40% of length (was 34%), uses
  44–56% of shaft width (was 24–38%) at its widest point, and a restrained maximum
  RGB (0.80, 0.82, 0.80). Its alpha is visibility gain. Tilt controls intensity and
  position, no elapsed-time pulse. Band remains inside shaft with no outline/glow,
  and intake/BELT straw is drawn over it. Covered pixels cannot receive the reflection.

### Verification oracle and remaining phone gate
- JVM: strict 60% boundary / >60% soft gain, no-cover/full-cover, curved straw,
  scaled/rotated rendered cover, moving-cover shutdown. Existing manual idle,
  one-swipe cover, slow/fast, conservation and cadence tests still pass.
- Actual GL seed 42: hidden visibility 0%, gain 0 in both orientations. After 11
  player-style gestures, partial poses expose **45.16% portrait / 58.06% landscape**;
  gain is zero and opposite-tilt screenshots differ in **zero pixels**.
- Later still-on-intake poses expose **77.42% portrait / 64.52% landscape**, gain
  1 / 0.21736. Paired tilts differ in **28 / 11 pixels** only on exposed metal.
  Lifted metal exposes 100%, pairs differ in **21 / 38 pixels**. Session summaries
  are unchanged by tilt captures. Images inspected at 1080 x 2340 / 2340 x 1080.
- Full real-input GL smoke: 10 batches, 9 packages, 1 intentional miss, 9 haptics,
  31,622 frames; manual idle, held buffer, pause/rotation/recreation exercised.
- Fullscreen audit retains unpadded full-window Surface, dark window/launch theme,
  transparent bars, cutout coverage and safe-area-only content offset. Phone-size
  GL captures show continuous dark edges. **Samsung was connected but locked**;
  current cold launch / rotation / Home-resume edge screenshots and real sensor
  jitter/FPS are not marked passed. No fullscreen regression code change is justified
  by the available evidence; repeat the physical oracle after unlocking.
- Manual feed stays unchanged; no idle intake force or automatic feeder was added.
- Proposal delivered separately: `GAMEPLAY_ECONOMY_PROPOSAL.md` compares multi-needle
  models, partial settlement, credit formula, collectible currency/rare rolls and
  manual-to-automatic ladder. ECONOMY_PROGRESSION used for this document only by
  explicit owner request. No multi-needle/economy/progression runtime implemented.

## Samsung artifact and physical evidence
Debug artifact: android/build/outputs/apk/debug/android-debug.apk.
Installed owner-review responsive-scale SHA-256: **053CDC1B1AF58A999CCC7336617EE1C9469ACB823AC5EF2352083012CC06FCE8**.
Wireless-ADB replacement installation of this responsive-scale APK succeeded
on Samsung SM-S938B, Android 17 / API 37, physical display 1080 × 2340. A
guarded responsive smoke was completed on the preceding responsive build; see
the latest responsive section for its exact artifact/results. The final compact-
inset hardening rebuild above installed successfully without relaunching the phone,
which was in another app. Samsung's module geometry/baseline is unchanged.
The owner's final physical acceptance is recorded above.
Owner physically reviewed the preceding >60%-threshold version and reported that
the overall state was good, but substantially exposed metal lacked visual payoff.
This is qualitative owner feedback, not acceptance or FPS evidence for the new APK.

2026-10-05 wireless attempt **before this needle correction**, using artifact
E783EC36D5A4645E7B3DD605D4D7CEBFB7FDC05EA2ACCDB17C32BEEA581E4AB0:
the previous manual-feed artifact cold-launched with seed 42.
At 5 active seconds the process reported 321 INTAKE / zero armed / zero touches /
zero packages. Before the 12-second idle oracle, five external touches were
registered (the QA script had sent none), so the assertion correctly stopped.
Subsequent read-only evidence capture also stopped when the game lost foreground.
No final portrait/landscape device screenshot pair was obtained in this attempt.

Observed current-process log samples during this interrupted run: 102, 71, 76
and 66 FPS; average frame intervals 18, 14, 12 and 16 ms; cumulative peak up to
129.69 ms and dropped fixed-step time 0.04635 s. These are short, mixed startup /
externally interacted samples, **not** a stable performance or jank pass. Battery
temperature readings before/after were 30.1 / 34.5 C; concurrent phone use prevents
attributing the rise to this game. Android thermal status was 0 at both reads.
The process-filtered diagnostic log is retained locally under ignored
verification/samsung-stage2-final-device.log.

**Historical pending gate (before final owner acceptance):** free phone window for cold launch in both orientations,
outer-edge screenshots, rotation, paused/running Home-resume, slow/fast manual
feed, several complete batches and an uninterrupted frame/thermal sample.
No credentials were supplied/guessed. Earlier 119–121 FPS/lifecycle samples were
from superseded automatically fed builds, not results for this artifact.

## Known limits after acceptance
- The owner accepted the current late-reveal metal and search balance on Samsung.
  The cylindrical reflection lobe is an inexpensive approximation, not a full
  physically based metal/environment renderer. Visibility uses 31 rendered-axis
  samples with conservative straw coverage, rather than exact pixel silhouette area.
- Straw patches share motion; individual fibres do not bend independently or
  collide pairwise. Procedural art remains replaceable.
- Native warm-start/sub-tick solver details rebuild on full recreation;
  authoritative manual state persists, exact future native paths are not promised.
- One-needle buffer; no quality/economy/unlocks/save across process death.
- No sound pipeline, sustained thermal study or weaker-device profiling.
- Difficulty, manual-work satisfaction, straw appearance and factory coherence
  have received owner acceptance for this iteration. Other phone configurations
  remain unprofiled; no blocker-level limitation was reported at closure.

## Retained regression checklist — Samsung SM-S938B
The owner has completed review and accepted Stage 2. This checklist is retained
for future regressions; it does not request another acceptance or authorize Stage 3.
For this needle correction, first expose less than about 60% and tilt: no glint.
Then compare roughly 60–75%, 75–90%, and almost/full opening, slowly tilting
forward/back and side-to-side in both orientations. Judge whether the progressively
stronger internal metallic glint helps
recognition without revealing hidden shaft, and whether it
stays stable in a resting hand. Check unchanged length, slender width, no timed
blink, and sensor response after Pause/Run, rotation and Home/resume. Complete a
few fresh piles and compare against the accepted difficult-but-fair search feel.

1. Cold launch portrait/landscape. Inspect all edges, cutout and navigation;
   repeat after rotation and Home/resume. No white strips or light flashes.
2. Leave a fresh running batch untouched: no self-feed. Push right into the
   mouth with slow deliberate and fast broad gestures.
3. Complete ten batches. Judge purposeful search/feeding versus tedious work.
4. Judge dense irregular coverage, separate thin bent straws and concealed,
   understated sharp metal needle with unchanged length.
5. Trace the supported machine chain without labels. Review magnet capture,
   hopper stop, jaws/wrapper and package ejection.
6. Toggle each switch: held intake, belt hold, intentional miss and queued needle.
   Re-enable and finish exactly once.
7. Pause/Run, rotate both ways and Home/resume during manual feed and processing.
   Preserve batch, machines, counters and manual state; no background catch-up.
8. Check compact landscape controls, HUD/ON-OFF and i drawer; observe FPS/jank/
   heating for several minutes. Target stable 60 FPS or better.

Stage 2 owner confirmation has been received; closing commit/push is authorized.
Stage 3 remains locked until separate explicit authorization.

## Iteration history: polished-steel late reveal (2026-10-05)

**Toolkit: GAMEPLAY_CORE + VISUAL_ART + UI_UX + QA_VERIFY + PERFORMANCE.**
Reread AGENTS.md, STAGE_2.md, this report, ART_BIBLE.md, SKILLS.md,
GAMEPLAY_ECONOMY_PROPOSAL.md and the local Toolkit routing. Git fetch completed;
HEAD and origin/main both remain cc5b231 (0 ahead / 0 behind). All existing local
Stage 2 work and the insurance stash were retained. No commit/push or Stage 3 work.

### Symptom -> cause -> fix -> verification
Owner Samsung feedback: exposed needle is generally good but does not reward the
eye enough -> flat muted body and a single small reflection whose exposure gain
already saturates at 75% -> retain accepted dimensions and early-search material,
introduce progressively polished steel plus two nested vertex-faded specular lobes
only above 60% -> 38 JVM tests, debug/release/desktop builds, lint and 10-batch real
GL smoke pass; exact early-frame comparisons and late-tilt pairs inspected. New
APK installed on Samsung; final artistic/sensor judgement awaits owner review.

### Dimensions, material and exposure curve
- **No further thickening:** physical dimensions stay 1.3 x 0.018 m, with the
  previously accepted +1.25 physical pixels of rendered width. Length, tip, eye,
  hit/collision geometry and straw density are unchanged.
- Early-search base RGB stays **(0.47, 0.51, 0.50)** and the dark lower facet stays
  (0.32, 0.37, 0.36). Above the gate, the body gradually interpolates toward cool
  polished steel **(0.56, 0.62, 0.65)**. This is exposure-earned shading, not a
  global brightness change that helps the first glance.
- The same 31 rendered-axis samples, individually curved/tapered straw occluders,
  interpolation/projection and conservative 0.25-pixel padding estimate uncovered
  length. It remains a length heuristic, not a full pixel/silhouette measurement.

| Exposed length | Gain / visual response |
| --- | --- |
| <=60% | Exactly zero: original subdued body, no polish or reflection geometry |
| >60–75% | Smoothstep 0 to 0.32; weak readable metal confirmation |
| >75–90% | Smoothstep 0.32 to 0.70; increasingly polished material / specular |
| >90–100% | Smoothstep 0.70 to 1; strongest elegant payoff at full opening |

The curve is continuous and monotonic with zero slope at each join. No exposure
history/fade persists below the gate: re-covering removes all assistance immediately.

### Reflection and performance
- A soft steel shoulder uses RGB **(0.74, 0.80, 0.84)**, spans 40% of needle length,
  and reaches 68% of shaft width at its centre. Alpha is gain x (0.28 + 0.30 x tilt
  strength). A nested silver core uses RGB **(0.91, 0.94, 0.96)**, spans 22% of
  length and 28–34% of shaft width; alpha is gain x (0.40 + 0.55 x strength).
  These are source colors, not a white opaque stripe: both are blended and fade
  to zero alpha at their ends through vertex interpolation.
- Both lobes share the attitude-driven centre, remain inside the shaft and are
  drawn underneath covering straw. Dark facet/eye preserve slender metal structure.
  **No glow/bloom, outline, external halo, pulse or time-driven sparkle added.**
  A broad internal shoulder supplies softness without an external locating signal.
- Sensor priority remains rotation vector -> game rotation vector -> gravity ->
  accelerometer, requested at about 33 Hz. Display-axis mapping and 180 ms EMA
  remain unchanged, with bounded/finite input and a calm static fallback. Sensors
  never change authoritative gameplay or material motion.
- Only two extra triangles per visible needle versus the previous reflection;
  preallocated Color objects, no new allocations in this path, shaders, textures,
  offscreen targets or dependencies. Sustained Samsung frame/heat measurements
  were not repeated in this small rendering pass.

### Verification / balance risks
- 38 JVM tests, zero failures; monotonic curve/joins, strict <=60% shutdown and
  moving-cover regression. Existing manual feed, native slow/fast, seeded search
  cover, conservation and 30/60/120 cadence checks pass.
- Desktop assemble, Android debug and unsigned release assemble pass; lint has
  0 errors / 5 existing version warnings. Real GL smoke: 10 batches, 9 packages,
  1 intentional miss, 9 haptics, 31,622 frames, including idle, pause, rotation,
  buffer and retained-session reconstruction. Synthetic FPS is not phone profiling.
- Full-size GL early images (hidden and 45.16% portrait / 58.06% landscape open)
  are **pixel-identical to the preceding APK render**, including opposite-tilt
  pairs: zero changes. This directly checks that the early search was not brightened.
- Eligible intake poses: 77.42% portrait / 64.52% landscape, gain 0.34647 / 0.06955;
  opposite tilts change 32 / 15 pixels. Full exposure changes 31 / 39 pixels.
  Session summaries are unchanged. Before/after cropped comparisons and full
  1080 x 2340 / 2340 x 1080 captures inspected: cool exposed steel, soft local
  specular, unchanged slender silhouette, no through-straw reflection or halo.
- Outer 8-pixel bands of all paired GL captures contain zero near-white pixels.
  Android window/insets/Surface code was untouched. This verifies GL coverage,
  not new physical cold-launch/rotation/Home screenshots for this artifact.
- Main balance risk: brighter exposed steel may make the final recognition too
  easy; it is deliberately concentrated after substantial player-driven opening.
  No hidden marker, density reduction, size change or sensor-only discovery. A
  nearly open item earns a small visual reward rather than extra early-search aid.
  Axis sampling and phone subpixel rasterization can affect perceived strength;
  owner should compare each exposure stage and judge hand-rest stability on Samsung.

Files changed in this pass: FactoryScreen.kt, NeedleVisibility.kt,
NeedleVisibilityTest.kt, AGENTS.md, STAGE_2.md, ART_BIBLE.md and this report.
GAMEPLAY_ECONOMY_PROPOSAL.md was read but not changed or implemented.
Ignored QA artifacts: verification/stage2-needle-*.png, payoff-before/,
payoff-comparison-*.png, inspect_needle_payoff.py and stage2-needle-smoke.log.
Historical stop point: this build was submitted for owner review without
commit/push. Final acceptance and closure authorization are recorded above;
no Stage 3 authorization is inferred.

## Iteration history: stable module scale across rotation

**Toolkit: VISUAL_ART + UI_UX + GAMEPLAY_CORE + PHYSICS_TOUCH + QA_VERIFY +
ANDROID_PLATFORM.** Presentation/layout correction only. No physics tuning,
needle dimensions, sensors, highlight thresholds, transport timers, density or
economy changes. GAMEPLAY_ECONOMY_PROPOSAL.md remains a proposal.

### Symptom -> cause -> fix -> verification
Landscape makes the search field and needle much smaller -> a double reduction:
480 viewport units per short edge instead of portrait's 400, and a 220-unit tray
instead of 248 -> introduce an orientation-independent device canvas baseline,
keep fixed modules and reroute/shorten connections -> 42 JVM tests, real GL
same-state captures, and Samsung same-PID screenshots/layout diagnostics confirm
0% field/material/machine-scale difference. Debug/release/desktop builds and lint
pass. This candidate was prepared for owner review and has subsequently received
the final physical acceptance recorded above.

### Stable module transform
- `FactoryCanvas` calibrates units/pixel from the window BEFORE choosing a layout:
  `max(400/rawShortAxis, 814/(rawLongAxis - max(horizontalInsets, verticalInsets)))`.
  The second term reserves a fixed-module bay on compact screens in both
  orientations. It does not fit/shrink the landscape factory independently.
  Reserving the larger obstructed axis also handles status/navigation bars that
  remain top/bottom in landscape. Samsung uses 400/1080 = 0.37037036 units/pixel.
- Both trays are **248 x 198.4 units**; material transform remains trayWidth/10 m.
  Main machine geometry retains unit=1. Needle physical 1.3 x 0.018 m and existing
  +1.25 rendered pixels are unchanged. No landscape-only needle compensation.
- Simulation dimensions, body poses/velocities, IDs, routes, machine states,
  manual flags and counters are not layout inputs to physics. Resize cancels the
  current gesture and synchronizes interpolation without replacing the batch.
  Pointer unprojection uses the same camera/transform as the rendered tray.
- GL Surface/background still covers the entire window, with a camera offset
  reserving safe content. No padded Surface, fitted bars or navigation suppression.

| Screen-space module metric | Portrait | Landscape | Difference |
| --- | ---: | ---: | ---: |
| Search field width | 669.60 px | 669.60 px | 0% |
| Search field height | 535.68 px | 535.68 px | 0% |
| Needle full axis length | 87.048 px | 87.048 px | 0% |
| Needle rendered shaft width | 2.45528 px | 2.45528 px | 0% |
| Representative 0.025 m straw width | 1.674 px | 1.674 px | 0% |
| Machine unit | 2.7 px | 2.7 px | 0% |

These values are recorded by the actual Samsung debug renderer; needle/straw
dimensions are transform-derived, not a silhouette measurement of hidden metal.
For example the 106-unit packer remains 286.2 px wide, the 74-unit magnet 199.8 px,
and the 64-unit buffer 172.8 px in both layouts. Old landscape tray width was
495 px (26.1% below portrait), while machine scale was 2.25 versus 2.7 px/unit.

### New landscape routing / UI
- The main conveyor remains supported rubber belt + rails/drums/drive. Its straight
  run is shortened to **214 units / 577.8 px** at the phone-size reference, versus
  roughly 395 units / 890 px before. The 48-unit receiving apron still joins the
  full physical feed edge to the actual drive drum ends.
- Search tray uses the upper-left bay. Main belt runs right through the magnet.
  Straw/missed branches turn toward bins on the upper-right of the floor plan.
  Metal runs down the extraction saddle, diagonally to the lower-right buffer,
  then left through the packer and a shorter 87-unit output handoff.
- Shared port anchors continue to define both rail/channel drawing and normalized
  item trajectories, including capture carriage, discharge and parcel ejection.
  No simulation-route shortcut or teleport was added; collars/rails/supports remain.
- Identity/counters occupy the bay under the search tray instead of reserving a
  factory-wide top header. A full-height compact side console holds status and
  78 x 60 switches / 164 x 52 actions. No controls cover material or stretch to
  match screen width. Portrait composition and its Samsung scale are preserved.

### Verification and actual phone evidence
- 42 JVM tests / zero failures; four canvas tests verify material/machine scale,
  realistic safe-area placements, compact aspect ratios and invariant bar-axis
  calibration. Existing manual feed, slow/fast native touch, coverage, cadence,
  lifecycle and conservation checks pass.
- Desktop assemble, Android debug and unsigned release assemble pass. Lint:
  0 errors / 5 existing version warnings. `git diff --check` passes.
- Real GL/Box2D smoke: 10 batches, 9 packages, 1 deliberate miss, 9 haptics,
  **31,126 frames**. Same-state portrait/landscape full-size captures assert equal
  pixels/metre and unchanged session summary; captured fully exposed metal stays
  the same length/width at different presentation angles. Early partial frames
  are below 60% and show no tilt effect; full metal responds to tilt. The latest
  run did not encounter an eligible on-intake pose before feeding the needle, so
  older `eligible` image files are NOT claimed as new evidence for this pass.
- Physical Samsung smoke (APK 6B8BF769E9FD23833C7B518DE89D8FFA1411C1F565EE7838FEDAED0C410AC686):
  portrait cold launch, seed 42, 6.816701 active seconds, 321 INTAKE / zero touches /
  zero armed / zero production. Pause then rotate to landscape: same PID 15645
  and byte-identical domain summary. Actual insets in both orientations were
  left=0/top=96/right=0/bottom=45. The scale metrics above match exactly.
- Actual 1080 x 2340 / 2340 x 1080 screenshots and 670 x 536 tray crops inspected
  side-by-side: same straw distribution, field/material size, housing scale and
  continuous dark edges. No overlapping controls or disconnected transfer ends
  observed. Small crop pixel differences reflect subpixel anchor/raster alignment;
  geometric scale and material state match, no pixel-identical claim is made.
- Physical landscape slow 1400 ms and fast 250 ms swipes: two accepted touches,
  17.6374 m drag, 191 armed, 312 INTAKE / 9 BELT at pause. Rotate back and Home/resume:
  same PID and exact paused summary at 8.933369 seconds; machines/counters retained.
  All five screenshots have zero near-white pixels in their outer 8-pixel bands.
- Original OS rotation settings restored. A subsequent held-buffer screenshot
  attempt stopped BEFORE any input/capture because the game lost foreground to
  another app. Open-needle physical comparison therefore remains an owner check;
  full open-metal scale is verified in actual GL, not in that aborted phone attempt.
- After this physical run, the compact-device bar-axis calibration was hardened
  and covered by an additional JVM test. Samsung's selected baseline and all module
  geometry are unchanged; the final rebuilt artifact is installed separately.
  No new physical full-batch/thermal or open-needle pass is attributed to that rebuild.

### Owner review / limits
On the installed APK, compare a paused partial reveal after rotation: needle
length/width, individual straw thickness and the machine housings should retain
the same perceived size. Check slow/fast gestures and every handoff, especially
upper-right residue routes and the lower-right buffer/packer return. Repeat
rotation and Home/resume; check all edges, cutout/navigation and compact controls.
Continue several full batches to judge search fairness and late metallic payoff.

Fixed-module geometry is presentation truth only. In-flight straw near the feed
mouth follows different orientation-specific handoff trajectories and may have
different local overlaps; this does not change the seed, intake material geometry,
needle detection, physical dimensions or authoritative state. Genuine search feel,
open-needle phone readability and Stage 2 acceptance were subsequently approved
by the owner. No new sustained FPS/heat measurements were supplied with that
acceptance. No true process-death persistence was added.

Changed: FactoryCanvas.kt, FactoryLayout.kt, FactoryScreen.kt, FactoryHud.kt,
FactoryCanvasTest.kt, FactorySmoke.kt and the scale contract/report in AGENTS.md,
STAGE_2.md, ART_BIBLE.md, ARCHITECTURE.md. No new runtime dependency.
Local ignored evidence: samsung-stage2-scale.json, samsung-stage2-scale-*.png,
samsung-stage2-scale-fields-side-by-side.png and stage2-needle-smoke.log.
Final status: Stage 2 accepted/completed; closing commit/push authorized by owner.
Stage 3 remains locked.

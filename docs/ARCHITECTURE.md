# Technical Architecture

## Goal
Create a foundation that can grow from a tactile physics prototype into a factory simulation without coupling rendering, economy, persistence and simulation.

## Candidate stack
Kotlin + libGDX, KTX selectively, Box2D selectively, Gradle, Android Studio.

Stage 0 must verify current stable versions, Android toolchain compatibility and licenses before pinning dependencies.

## Implemented Stage 0 foundation

Physical modules: `core` (Kotlin/JVM library), `android` (Android application),
`lwjgl3` (desktop application). Both launchers depend on `core`; `core` has no
platform-backend imports. Android owns SDK/lifecycle/insets integration and
supplies immutable `BuildInfo`. `game/NeedleFactoryGame` owns a single
`presentation/DiagnosticScreen`; its `debug/BootStatus` is pure Kotlin event
state, tested without libGDX initialization. There is no gameplay state yet.

Remaining logical packages are reserved by `package.md` files, not speculative
classes or empty Gradle modules. Shared prototype assets live in `assets/`.
No fixed-step simulation, physics, economy or save implementation is introduced.
See `STAGE_0_REPORT.md` for toolchain selection and verification evidence.

## Implemented Stage 1 prototype

The existing three modules are retained. `game/NeedleFactoryGame` now opens
`presentation/SearchScreen`; the Stage 0 diagnostic remains available through
the desktop launcher and its smoke harness. Disposal hides the current screen
before freeing its native resources.

- `content/SearchConfig`: one centralized set of prototype feel tunables.
- `simulation/SearchRound`: domain-owned poses, seeded generator, visual
  exposure/hit rules and `SearchSession` transitions. No rendering imports.
- `simulation/FixedStep`: bounded 60 Hz clock with interpolation alpha.
- `physics/SearchPhysics`: native Box2D ownership, collision filters, bounded
  physical pusher and pose snapshots. Geometry stays in the tray's visual
  envelope; it does not assign rewards or define discovery.
- `presentation/SearchScreen`: single-touch intent, fitted camera, interpolated
  placeholder rendering, success accent, HUD and debug metrics.
- `game/PlatformFeedback`: one portable find-confirmation callback; Android
  uses View haptic feedback on the main thread; desktop is a no-op.

Session count, exposure and round lifecycle remain outside Box2D. No economy,
factory or persistence code was added. Assets are still asynchronously loaded.
See `STAGE_1_REPORT.md` for tunables, runtime evidence and pending acceptance.

## Planned logical boundaries
### Stage 2 implementation decision
The new automation loop needs persistent machine/item truth and two camera
compositions. Rather than refactor away the accepted search prototype, retain
`SearchScreen` as a desktop reference (`-Pscreen=search`) and make `FactoryScreen`
the default. The three modules, toolchain, fixed-step clock and platform callback
remain in place. No ECS, DI, economy or save system is introduced.

- `FactoryConfig`: centralized geometry, feed and processing tunables.
- `FactorySession` / `FactoryBatch`: pure Kotlin seeded items, machine toggles,
  route transitions and exactly-once recovered/missed/packaged counters.
- `FactoryIntakePhysics`: Box2D only on the intake; one body per twelve-strand
  tuft, three smooth-tangent collision sections, bounded physical finger and
  actual feed-edge handoff. Tufts overlap each other; simplified bulk contact
  is intentional. Downstream items use deterministic normalized route progress.
- `FactoryLayout`: authored landscape flow and portrait stacked composition.
- `FactoryScreen`: interpolated geometry, machinery, touch intent and haptic
  notification. Rendering never feeds an item or produces a package.
- `NeedleLighting` / Android `NeedleTiltSensor`: optional filtered device attitude
  affects only internal metal shading. `NeedleVisibility` estimates exposed axis
  length against the same curved/projected straw poses as rendering; <=60% disables
  reflection completely. This presentation heuristic is not discovery/reward truth.

The dedicated visual pass splits original procedural art into `FactoryDiorama`,
shared `FactoryDrawing`/palette and `FactoryHud`. This makes projection, supports,
shadows, material palette and instrument typography consistent without adding a
runtime framework. `FactoryLayout` attachment points define both visible
connections and interpolated material endpoints. The first 12% of BELT animation
traverses the receiving pan, the middle portion runs on the framed belt and the
last 10% follows an attached discharge chute. The capture zone still uses the
same authoritative 55% route crossing. These mappings do not change production
logic, timings, item identity, physics, needle length or session lifecycle.

The visual smoke renders screenshots to real GL framebuffers at 1080×2160 and
2160×1080; desktop OS window-size limits cannot silently turn portrait into a
square screenshot. Those buffers are verification-only, never production work.

`NeedleFactoryGame` owns the session independently of screen/native resources.
Android retains that portable object across configuration recreation through
Activity non-configuration retention. Normal pause/resume and resize merely
cancel the current finger gesture and discard the clock remainder. Native
rehydration restores item poses/velocities; it creates bodies only for items
still on the intake. Counters, routes, progress and enabled machines remain
unchanged. There is no process-death persistence and no background/offline work.

The final owner correction removes directional intake forces. A downstream
finger contact arms the contacted item; only that item physically reaching the
gate can hand off. Manual eligibility, item velocity and the metering remainder
live in the portable session, so native rehydration does not turn untouched
material into automatic production. Without input all intake poses settle/hold.
No feeder unlock, economy or automation selection is implemented at Stage 2.

Android's GL surface fills the window without root padding. A dark application
theme and transparent system bars cover launch/transition frames. `SafeArea`
passes pixel obstructions onto the GL thread; `FactoryScreen` keeps a full-window
viewport, offsets only content coordinates and clears the entire surface. Insets
are refreshed on configuration/resume; touch unprojection shares that camera.

Responsive module scale is now explicit in `FactoryCanvas`, independent of
orientation selection. Its device baseline is max(400 / raw short pixel axis,
814 / (raw long pixel axis minus the larger obstructed-axis total)): compact displays reserve room for the fixed module
bay in both orientations, without a landscape-only fit/zoom. Samsung uses
400 / 1080 units per pixel in both orientations. `FactoryLayout` positions the
same 248 x 198.4 tray and unit-1 machine housings with authored anchors; belt length
and transfer directions change instead of material/machine size. Insets still
offset safe content on a full-window Surface. Fixed-size modules, shared handoff
ports and inverse touch transforms never change simulation dimensions or state.

The packer commits its result after its visible jaw cycle and package ejection.
Turning the separator off affects needles that have not yet crossed its capture
zone; an already latched capture remains committed. Belt-off stops feeding and
transport; an already loaded packer may cycle, then holds ejection until restart.

- **platform/android** — launcher, Android lifecycle, platform services, haptics.
- **core/game** — game orchestration and state.
- **simulation** — fixed-step simulation and world rules.
- **physics** — collision/physical interaction adapter.
- **factory** — machines, transport, processing and bottlenecks.
- **economy** — currencies, prices, rewards, upgrades, progression.
- **content** — data definitions for materials, needles, machines and levels.
- **presentation** — rendering, animation, particles, camera and audio triggers.
- **ui** — HUD, shop, upgrades, settings and onboarding.
- **save** — versioned persistence and migrations.
- **debug** — diagnostics, overlays, cheats/dev controls.
- **tests** — deterministic unit/integration tests.

These are logical boundaries first. Do not create a Gradle module for every bullet until module separation has a demonstrated build or ownership benefit.

## Simulation
Use a fixed timestep for systems whose behavior must be stable across frame rates. Rendering may interpolate between simulation states.

Physics tiers:
1. Full physics: needle and important interactable/production objects.
2. Simplified simulation: bulk material where full rigid-body simulation is wasteful.
3. Visual-only particles: debris/dust/sparks with no economic truth.

Never represent thousands of decorative particles as Box2D bodies.

## Data-driven content
Machine statistics, costs, material properties, needle rarity and progression should eventually be externalized into validated content definitions. Do not hard-wire economy constants throughout gameplay classes.

## Performance
Initial target is stable 60 FPS on representative modern Android hardware while retaining a future battery-oriented 30 FPS mode. High-refresh modes are optional and must be justified by profiling.

Performance budgets for body count, particles, draw calls, texture memory and simulation time must be established during prototype measurement rather than invented now.

## Testing
Prefer pure Kotlin tests for economy/progression/simulation logic. Add integration tests around save migration and deterministic production chains. Visual/physics feel still requires device testing.

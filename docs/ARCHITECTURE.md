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

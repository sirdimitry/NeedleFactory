# Technical Architecture

## Goal
Create a foundation that can grow from a tactile physics prototype into a factory simulation without coupling rendering, economy, persistence and simulation.

## Candidate stack
Kotlin + libGDX, KTX selectively, Box2D selectively, Gradle, Android Studio.

Stage 0 must verify current stable versions, Android toolchain compatibility and licenses before pinning dependencies.

## Logical boundaries
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

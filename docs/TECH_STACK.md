# NeedleFactory — Stage 0 Technical Stack

Validated: 2026-10-03.

Implemented Stage 0 pins: Gradle 8.13, AGP 8.13.2, Kotlin 2.3.21, libGDX 1.14.2,
JUnit 4.13.2; Gradle runs on JDK 21, shared Java/Kotlin bytecode targets 11.
Android compile/target SDK 36, min SDK 23, Build Tools 35.0.0 (AGP default).
AndroidX is a required transitive dependency of the libGDX Android backend,
not an extra UI framework. Stage 0 omitted KTX and Box2D; Stage 1 adds Box2D below. Exact rationale, sources,
licenses and test results are in `STAGE_0_REPORT.md`; pins live in
`gradle/libs.versions.toml` and the wrapper properties.

## Core choice

Stage 1 adds `gdx-box2d` **1.14.2** and matching
`gdx-box2d-platform` natives: desktop and Android `armeabi-v7a`, `arm64-v8a`,
`x86_64`. JVM native integration tests load matching libGDX core natives too.
KTX remains absent: direct Box2D ownership is small and explicit. Existing
Gradle/AGP/Kotlin/backend versions remain pinned. Stage 1 choices and results
are recorded in `STAGE_1_REPORT.md`.

**Kotlin + libGDX + selective KTX + selective Box2D**, generated/maintained as a Gradle project that opens cleanly in Android Studio.

### Why this stack
- libGDX is free/open source (Apache-2.0), mature, Android-native, and gives us a real game loop, rendering, input, audio and physics integration without adopting a heavyweight commercial engine.
- gdx-liftoff is the official modern libGDX project generator and supports Android, desktop, Kotlin and extensions.
- KTX is a modular Kotlin-friendly layer over libGDX. Use only modules that materially improve code clarity.
- Box2D is appropriate for important tactile objects and machine interactions, but bulk filler/debris must use cheaper simulation or visual particles.

## Version policy

Do **not** blindly pin versions from this document. At implementation time, Codex must check the current stable versions exposed by gdx-liftoff/Maven/official docs and record the exact selected versions in the build files and Stage 0 report.

At validation time the official libGDX setup documentation lists libGDX **1.14.2** as the latest stable release. Prefer stable releases over snapshots.

gdx-liftoff currently defaults Kotlin projects to Kotlin 2.2.x, while its own documentation notes that some libraries may still have compatibility considerations versus 1.9.x. Therefore Codex must validate the generated dependency graph/build before accepting the Kotlin version. Do not downgrade merely from caution; do not keep a newer version merely from novelty.

## Initial modules/platforms

Start with:
- `core` — portable game code.
- `android` — Android launcher/platform integration.
- `lwjgl3` — desktop development launcher for fast iteration and debugging.

Do not add web/iOS/server modules in Stage 0.

## Initial dependencies

Required:
- libGDX core
- libGDX Android backend
- LWJGL3 desktop backend
- Kotlin stdlib/tooling as generated
- JUnit/test tooling appropriate to the generated project

Candidate, add only when used:
- KTX app
- KTX collections/math helpers
- KTX Box2D
- libGDX Box2D

Deferred until a demonstrated need:
- Ashley ECS
- dependency injection frameworks
- networking/backend SDKs
- analytics SDKs
- ad/IAP SDKs
- database layers
- Compose
- large UI frameworks

## UI

Gameplay/HUD should initially use libGDX rendering/Scene2D-style facilities or a deliberately thin game UI layer. Jetpack Compose is **not** the game renderer. Native Android UI may later be appropriate for platform-only surfaces, but mixing UI stacks must have a concrete benefit.

## Free visual tooling candidates

The official libGDX tools list includes free tooling useful to this project:
- Talos — node-based VFX.
- HyperLap2D — visual 2D world/scene editor.
- Skin Composer — Scene2D UI skin authoring.
- Particle Editor — 2D particle effects.
- Flame — 3D particle effects.
- Hiero — bitmap fonts.
- Texture Packer — sprite atlas workflow.
- Tiled — 2D level/layout authoring.
- fbx-conv — model conversion where needed.

Do not install all of them. Stage 1/visual prototyping should select tools based on the chosen asset pipeline.

## Physics strategy

Use three tiers:
1. **Authoritative full physics** — needle and important interactables.
2. **Simplified bulk simulation** — filler/material streams and large quantities.
3. **Visual-only particles** — dust, tiny scraps, sparks and decorative motion.

Game economy may never depend on a decorative particle.

## Performance baseline
Stage 2 keeps the existing pinned dependencies and uses Box2D only on the
intake (322 initial bodies for 3,840 visual strands). Downstream transport is
pure fixed-step Kotlin. Procedural quadratic ribbons and batched geometry
remain replaceable prototype art. Barlow-derived bitmap UI typography includes
its source, SHA-256 and SIL OFL license; no new runtime libraries are introduced.
See `STAGE_2_REPORT.md` for verification limits.


Stage 0 establishes instrumentation hooks; Stage 1 measures actual budgets.
- Balanced target: 60 FPS.
- Battery-oriented 30 FPS mode is a future option.
- High-refresh support is future/profiling-driven.
- Fixed timestep for authoritative simulation where needed.
- Avoid allocations in per-frame hot paths.
- Pool high-churn entities.
- Never perform blocking I/O on the render loop.

## Android baseline

Codex must inspect the locally installed Android SDK/AGP compatibility before setting compile/target SDK values. Do not lower target SDK merely to support obsolete devices. Minimum SDK should be chosen from actual libGDX/toolchain constraints and our intended device coverage, documented in the Stage 0 report.

Primary physical test device: modern flagship Android phone. The game must nevertheless be designed for a substantially weaker mid-range Android performance class.

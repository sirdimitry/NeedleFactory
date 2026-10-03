# First Codex Task — NeedleFactory Stage 0

You are working in the `sirdimitry/NeedleFactory` repository.

## Before touching code
Read, in this order:
1. `AGENTS.md`
2. `README.md`
3. `docs/GDD.md`
4. `docs/ARCHITECTURE.md`
5. `docs/TECH_STACK.md`
6. `docs/SKILLS.md`
7. `docs/ART_BIBLE.md`
8. `docs/ECONOMY.md`
9. `docs/ROADMAP.md`
10. `docs/STAGE_0.md`

Treat `AGENTS.md` and `docs/STAGE_0.md` as hard scope boundaries.

## Task
Implement **Stage 0 only**.

Inspect the local machine first: Android Studio/SDK/JDK/Gradle environment and available device/emulator. Use the official current libGDX/gdx-liftoff approach as a reference for a Kotlin project, but integrate it cleanly into this existing repository rather than overwriting project documentation.

Use stable, mutually compatible dependency versions. Verify them rather than guessing. Keep the runtime dependency set minimal.

Create the Android + core + LWJGL3 project skeleton, a minimal diagnostic boot screen, appropriate tests and build instructions exactly as specified in `docs/STAGE_0.md`.

## Engineering behavior
- Do not build Stage 1.
- Do not add Ashley/ECS, DI, Compose, backend, analytics, ads, IAP or other frameworks without a demonstrated Stage 0 need.
- Keep gameplay-independent core boundaries clean.
- Do not commit `local.properties`, SDK paths, signing material, tokens or secrets.
- Do not mass-generate speculative classes.
- Prefer understandable Kotlin over clever abstractions.
- If the generated toolchain conflicts with a documented assumption, investigate and document the reason instead of forcing the assumption.

## Verification
Run the relevant Gradle tasks yourself. Fix build/test failures before reporting completion. If Android hardware is connected, launch the debug build and report the result. If device access is unavailable, say so explicitly rather than claiming a device test.

Create/update `docs/STAGE_0_REPORT.md` with:
- exact selected versions;
- architecture actually created;
- commands run;
- test/build results;
- device/emulator result;
- known issues;
- deliberate deviations from the plan and why;
- proposed Stage 1 entry point.

At completion, summarize the result and **STOP for human review**. Do not proceed to Stage 1.

# Stage 0 — Technical Foundation

## Mission
Create the smallest production-shaped project that proves our toolchain, module boundaries and Android/desktop launch path **without implementing the game prematurely**.

## Required output

### 1. Toolchain verification
Record in the Stage 0 report:
- Android Studio version
- JDK used by Gradle
- Android SDK/compile/target/min SDK
- Gradle version
- Android Gradle Plugin version
- Kotlin version
- libGDX version
- KTX version(s), if used
- Box2D status
- local test devices/emulators available

### 2. Project skeleton
Create only the justified physical modules:
- `core`
- `android`
- `lwjgl3`

Inside `core`, establish packages for future logical boundaries without filling them with speculative abstractions:
- game
- simulation
- physics
- factory
- economy
- content
- presentation
- ui
- save
- debug

### 3. Boot screen
A minimal NeedleFactory boot screen is allowed. It should prove:
- renderer works;
- input works;
- resize/orientation handling works;
- basic font/text rendering works;
- application can pause/resume;
- desktop and Android launchers both reach the same core.

This is a technical diagnostic screen, not Stage 1 gameplay.

### 4. Tests
Add at least:
- one pure Kotlin/JVM unit test proving the core test path;
- one deterministic simulation-clock/fixed-step test if a simulation clock is introduced.

Do not create fake test suites merely to increase test count.

### 5. Developer diagnostics
In debug builds expose a small non-production diagnostic overlay or logging path for:
- FPS
- frame time
- viewport size
- app version/build type
- simulation tick count if applicable

### 6. Build documentation
Update README with exact local commands for:
- desktop run
- unit tests
- Android debug build

### 7. CI
If straightforward with the generated project, add a minimal GitHub Actions workflow for JVM tests/build validation. Do not spend Stage 0 on elaborate release automation.

## Explicit non-goals
Do not implement:
- hidden-needle gameplay;
- factory machines;
- economy;
- progression;
- save-game content;
- ads/IAP;
- analytics SDKs;
- backend/accounts;
- production art;
- procedural generation.

## Exit criteria
Stage 0 is complete when:
- clean Gradle sync succeeds;
- desktop launcher runs;
- Android debug build succeeds;
- Android app launches on a device/emulator;
- tests pass;
- diagnostic screen works;
- repository contains no secrets/local SDK paths;
- implementation matches documentation;
- Codex writes `docs/STAGE_0_REPORT.md`;
- human owner reviews the result.

Then STOP. Stage 1 remains locked until explicit approval.

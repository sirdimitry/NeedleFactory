# NeedleFactory

A mobile factory/physics game for Android built around the fantasy of finding a needle in increasingly complex material streams, then automating the process into a satisfying production line.

## Status
**Stage 0 — accepted after physical testing on Samsung SM-S938B.**

**Stage 1 — tactile search prototype accepted/completed after the owner's
10-round playtest on Samsung SM-S938B.**

**Stage 2 — first automation line accepted/completed after the owner's physical
playtest on Samsung SM-S938B (2026-10-05).**
Latest material clarification is recorded in `docs/STAGE_2.md` and the
Art Bible: accepted needle dimensions, exposure-gated polished metal and smooth dense straw.
The separate visual polish pass follows the updated `docs/SKILLS.md` craft profile:
connected machinery on a workshop plinth, framed/supported belt and continuous
transfer chutes, shallow 2.5D depth, a redesigned instrument HUD and licensed
bitmap typography. The owner accepted the current visual/gameplay iteration.
The final-feedback pass increases the intake to 3,840 varied crossing
straws, strengthens the mechanical nodes and authors a compact two-level
landscape workshop with a fixed-size side control cluster. Intake is manual:
push material into the feed mouth; automatic feeding remains a future unlock.
Android uses a full-window dark GL surface with safe-area-aware content.
Search field and main-machine screen-space scale match in both orientations.
Stage 3 requires separate explicit owner authorization and has not started.
The accepted Stage 2 preserves the owner's Stage 1 feedback:
real landscape reflow, thin/sharp understated needle, dense natural straw and
lifecycle-safe session state.

No gameplay implementation should expand beyond the currently approved milestone.

## Product pillars
- Satisfying tactile search and physical interaction.
- Factory automation with visible, understandable cause and effect.
- Meaningful upgrades rather than endless +10% buttons.
- Short sessions that can naturally grow into longer optimization sessions.
- Strong visual identity: stylized miniature industrial diorama.
- Mobile-first performance, controls, readability, battery use and lifecycle handling.
- Data-driven content so economy, machines and progression can be tuned without architectural rewrites.

## Planned stack
- Kotlin
- libGDX
- KTX where it reduces Kotlin boilerplate
- Box2D for selected gameplay physics
- Gradle / Android Studio
- Git + GitHub

Dependencies are added only when a milestone has a demonstrated use case. Stage 1 is the first authorized Box2D use case.

See `AGENTS.md` and `docs/` before making architectural changes.

## Build and run

Install JDK 21 and Android SDK Platform 36 with Build Tools 35.0.0. The Gradle
wrapper pins Gradle 8.13 and verifies downloads with the official SHA-256.
Set `JAVA_HOME` to your JDK and `ANDROID_HOME` to your SDK. In Android Studio,
open this repository root, choose JDK 21 as the Gradle JDK, select the installed
SDK in SDK Manager, and sync. Android Studio may create an ignored
`local.properties`; never commit it. No global Gradle installation is needed.
First build needs network access to Google Maven/Maven Central.

From the repository root in Windows PowerShell:

```powershell
$env:JAVA_HOME = '<path-to-JDK-21>'
$env:ANDROID_HOME = '<path-to-Android-SDK>'
.\gradlew.bat :lwjgl3:run
.\gradlew.bat :core:test
.\gradlew.bat :android:assembleDebug
```

On Linux/macOS use `bash ./gradlew` with the same task names and exported
`JAVA_HOME`/`ANDROID_HOME`. macOS desktop launch adds `-XstartOnFirstThread`.
These hosts were configured but not physically tested during Stage 0.

Additional verification commands:

```powershell
.\gradlew.bat :core:clean :lwjgl3:clean :android:clean :core:test :lwjgl3:assemble :android:assembleDebug :android:lintDebug
.\gradlew.bat :lwjgl3:diagnosticSmoke
.\gradlew.bat :lwjgl3:prototypeSmoke
.\gradlew.bat :lwjgl3:factorySmoke
.\gradlew.bat :lwjgl3:run -Pseed=42
```

Android Studio can run the `android` configuration directly on a connected
device. Stages 0, 1 and 2 were physically validated and accepted on Samsung SM-S938B.

The default screen is the Stage 2 automation line: dense vibration table,
conveyor, magnet, buffer and packer. Each batch contains one needle and 320
physical straw patches (3,840 slender curved visual strands). Push material
right into the feed mouth with repeated finger sweeps. No material feeds without
touch; already delivered items continue through the machines. **Pause line** stops
the whole simulation; **Feed gate** holds/releases hand-delivered material, and
the other switches control belt, magnet and packer. **Next batch** unlocks
only after every item reaches output, residue or missed-metal storage. An
enabled packer produces one package/haptic; a disabled magnet causes a visible
miss. Counters are session diagnostics, not currencies.

Portrait and landscape reflow the same active state. Background/resume and
configuration recreation preserve progress/counters while the process lives.
Cold launch/process death starts fresh; there are no saves or offline progress.
Tap the upper-right **i** to open/close FPS/frame interval/body count telemetry
inside the control console. Logcat tag `Factory` records seed, duration, routes,
buffer, throughput, manual-feed eligibility, machine states and touch/drag data, plus a
terminal batch summary. Use `-Pseed=<signed-long>` to reproduce a batch.
The Stage 0 diagnostic and Stage 1 reference remain available on desktop with
`-Pscreen=diagnostic` and `-Pscreen=search`. The factory OpenGL smoke runs ten
accelerated batches and does not replace owner device review. See
`docs/STAGE_2_REPORT.md` for verification and the Samsung checklist.

With the phone connected and USB debugging authorized:

```powershell
& "$env:ANDROID_HOME/platform-tools/adb.exe" devices -l
& "$env:ANDROID_HOME/platform-tools/adb.exe" -s '<phone-serial>' install -r android/build/outputs/apk/debug/android-debug.apk
& "$env:ANDROID_HOME/platform-tools/adb.exe" -s '<phone-serial>' shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n com.needlefactory.android/.AndroidLauncher
& "$env:ANDROID_HOME/platform-tools/adb.exe" -s '<phone-serial>' logcat -s Factory:I NeedleFactory:I AndroidRuntime:E
```

For a debug-only reproducible phone start, force-stop the app and add
`--el seed 42` to `am start`. Release ignores the seed extra and hides metrics.
Haptic confirmation follows Android's system haptic preferences.

## Milestone documents

- `docs/STAGE_0_REPORT.md` — accepted technical foundation.
- `docs/STAGE_1.md` — authorized tactile-search prototype scope.
- `CODEX_STAGE_1_PROMPT.md` — execution brief for Codex CLI.
- `docs/STAGE_1_REPORT.md` — accepted Stage 1 implementation and evidence.
- `docs/STAGE_2.md` — accepted first automation-line scope and regression requirements.
- `CODEX_STAGE_2_PROMPT.md` — execution brief for Codex CLI.
- `docs/PROGRESS.md` — permanent milestone history, immutable tags, curated screenshots and release links.
- `docs/STAGE_2_REPORT.md` — accepted Stage 2 implementation, tests and device evidence.
- `docs/GAMEPLAY_ECONOMY_PROPOSAL.md` — saved design proposal, not approved for implementation.

Stages 0–2 are accepted/completed. Stage 3 is next and remains locked.

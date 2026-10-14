# NeedleFactory

## Current playable snapshot — Стог и сталь

[Download Stage3 playtest1 debug APK](https://github.com/sirdimitry/NeedleFactory/releases/tag/v0.3.0-stage3-playtest.1).
Owner authorized commit/push and prerelease publication on2026-10-14.
Stage3 remains CURRENT / AUTHORIZED, not accepted/completed. Includes the live
workshop, saved progression, tools/robot/fans/dividers, sound mixer and NF icon.
Debug playtest credit cheats are available in the i menu. Accepted Stage2 remains
the stable baseline at v0.2-stage2. Reports below are historical pass summaries.

Current Stage3 owner-review candidate (2026-10-10): live material/tool upgrades,
always-available scrolling accordion shop and Да/Нет purchase confirmations;
paid robot hand200, fans500 with individual speed/cone/range, physical1.3m
rotatable dividers40 and50% resale. Larger manual yard retains material scale.
Conveyors/processing deferred; owner progression preserved. 149 JVM tests,
debug/release/lint, native GL and physical Samsung smoke pass. Stage3 remains
CURRENT / AUTHORIZED, not accepted; no commit/push. See docs/STAGE_3_REPORT.md.


Current Stage3 candidate (2026-10-08): owner-supplied prototype ideas adapted to
our libGDX diorama. Select Hand/Rake/Steel for1/2/3 nearby manual grips; normalized
products-v3 economy, preserved purchases and schema3 saves. Late revealed steel
has a small tilt-dependent star. See [adaptation note](docs/ARCHIVE_REFERENCE_ADAPTATION.md).
New machine construction is deferred. Candidate is not accepted/completed;
no commit/push/tag/release authorization.

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
Stage 3 now has separate explicit owner authorization for its first playable
economy slice; it is **CURRENT / AUTHORIZED, not accepted/completed**. See
`docs/STAGE_3.md` and `docs/STAGE_3_REPORT.md`: 3 needles, early finish/results,
workshop credits, rake/guides and versioned progression save. Automatic feed,
offline income, rare economy and Stage 4 remain outside this pass.
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

The default Stage 3 candidate starts with a search table, manual straw receiver
and raw-metal tray. Three seeded needles are buried beneath 24 small clumps from
ten mirrored families of 3–5 curved stalks. Touch a visible stalk and drag one
clump right across the receiver edge; release stops feeding. Rake ownership is
retained but does not widen this experimental grab. Expose over 60% of a needle
and grab an uncovered point, then physically carry it through the lower metal
slot. Tapping does not secure it. Hidden steel can accompany contacted straw
through the common manual receiver and be missed. No free conveyor or separator:
machines and upgrades are paid progression equipment. Old push presets remain for regression.
Loose straw earns 6 credits per full lot; recovered metal earns 3 per needle.
Finish previews actual secured value, then settles once; untouched material is
not paid. Normal completion opens results automatically.

Workshop purchases: rake 28, optional guides 16, Baler 42 (guides/Baler require
rake). Baler appears on the empty mount and binds hand-delivered straw, raising
the full-lot straw value to 16. Short conveyor 48 requires Baler; magnet 72
requires conveyor. The paid lane accepts nearer local hand delivery, carries
material to processing and, with the separator on, sells sorted steel for 4
each. Unowned/off magnet can still miss steel. Open the second Workshop page
for these purchases. No starter conveyor/magnet/buffer/packer, auto-feed,
rare economy or offline money. The accepted complete Stage 2 line remains in
reference tests and `:lwjgl3:factorySmoke`; its immutable version is `v0.2-stage2`.

Portrait and landscape reflow the same active state. Background/resume and
configuration recreation preserve progress/counters while the process lives.
Schema-2 private saves retain credits/tools/Baler/transport and the last paid receipt across
restart. Schema-1 wallets/tools/receipts migrate without repricing earned money.
Process death may reset the unpaid physical lot; there is no per-straw save or
offline income. Rotation/Home retain the active state while the process lives.
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

Stages 0–2 are accepted/completed. Stage 3's first slice is authorized, not accepted.
The first Manual-to-Factory Progression v2 receiver/Baler sub-pass is authorized,
not accepted. Later machine purchases require a separate owner review.

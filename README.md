# NeedleFactory

A mobile factory/physics game for Android built around the fantasy of finding a needle in increasingly complex material streams, then automating the process into a satisfying production line.

## Status
**Stage 0 — accepted after physical testing on Samsung SM-S938B.**

**Stage 1 — tactile search prototype accepted/completed after the owner's
10-round playtest on Samsung SM-S938B.**

**Stage 2 — first automation line authorized/current.**
Stage 3 remains locked until the automation prototype is physically playtested
and explicitly accepted. Stage 2 must preserve the owner's Stage 1 feedback:
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
.\gradlew.bat :lwjgl3:run -Pseed=42
```

Android Studio can run the `android` configuration directly on a connected
device. Stages 0 and 1 were physically validated on Samsung SM-S938B.

The default screen is now the Stage 1 tray: drag to push straw, uncover and
tap the needle, then use **Next pile**. `Found` counts this session's finds;
there is no currency, save or automation. Pause/resume and rotation preserve
the current in-memory round; process death starts a new session. Portrait is
primary; landscape fits the same portrait composition with side bars.

Debug builds show seed, FPS/frame interval, bodies, active time, touches and
drag distance below the tray. Logcat tag `NeedleFactory` prints a summary on
each find. Use `-Pseed=<signed-long>` on desktop to reproduce a starting pile.
To retain the Stage 0 diagnostic on desktop, use `-Pscreen=diagnostic`.
The OpenGL prototype smoke uses accelerated time and synthetic gestures; it
does not replace the owner's 10-round phone playtest.

With the phone connected and USB debugging authorized:

```powershell
& "$env:ANDROID_HOME/platform-tools/adb.exe" devices -l
& "$env:ANDROID_HOME/platform-tools/adb.exe" -s '<phone-serial>' install -r android/build/outputs/apk/debug/android-debug.apk
& "$env:ANDROID_HOME/platform-tools/adb.exe" -s '<phone-serial>' shell am start -W -n com.needlefactory.android/.AndroidLauncher
& "$env:ANDROID_HOME/platform-tools/adb.exe" -s '<phone-serial>' logcat -s NeedleFactory:I AndroidRuntime:E
```

For a debug-only reproducible phone start, force-stop the app and add
`--el seed 42` to `am start`. Release ignores the seed extra and hides metrics.
Haptic confirmation follows Android's system haptic preferences.

## Milestone documents

- `docs/STAGE_0_REPORT.md` — accepted technical foundation.
- `docs/STAGE_1.md` — authorized tactile-search prototype scope.
- `CODEX_STAGE_1_PROMPT.md` — execution brief for Codex CLI.
- `docs/STAGE_1_REPORT.md` — accepted Stage 1 implementation and evidence.
- `docs/STAGE_2.md` — authorized first automation-line scope.
- `CODEX_STAGE_2_PROMPT.md` — execution brief for Codex CLI.

Stage 2 is current. Stage 3 is locked.

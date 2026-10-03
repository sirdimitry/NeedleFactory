# NeedleFactory

A mobile factory/physics game for Android built around the fantasy of finding a needle in increasingly complex material streams, then automating the process into a satisfying production line.

## Status
**Stage 0 — Foundation accepted after physical testing on Samsung SM-S938B.**
Stage 1 requires separate explicit authorization and remains locked.

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

Dependencies are not added merely because they are listed here. Stage 0 must validate versions, licenses, maintenance status and actual need.

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
These hosts are configured but were not tested during Stage 0.

Additional verification commands:

```powershell
.\gradlew.bat :core:clean :lwjgl3:clean :android:clean :core:test :lwjgl3:assemble :android:assembleDebug :android:lintDebug
.\gradlew.bat :lwjgl3:diagnosticSmoke
.\gradlew.bat :lwjgl3:run -Pdiagnostics=false
```

`diagnosticSmoke` requires a real OpenGL desktop session. It renders 240 frames,
changes the actual window from landscape to portrait, invokes synthetic input
and lifecycle callbacks, and writes two framebuffer screenshots in ignored
`verification/`. It is separate from headless unit tests and CI. Actual Android
input/background events were checked through ADB; desktop mouse/focus events
remain in the owner checklist.

To install and launch with an emulator or device connected (replace the SDK
placeholder above; add `-s <serial>` if multiple devices are connected):

```powershell
& "$env:ANDROID_HOME/platform-tools/adb.exe" devices -l
& "$env:ANDROID_HOME/platform-tools/adb.exe" install -r android/build/outputs/apk/debug/android-debug.apk
& "$env:ANDROID_HOME/platform-tools/adb.exe" shell am start -W -n com.needlefactory.android/.AndroidLauncher
& "$env:ANDROID_HOME/platform-tools/adb.exe" logcat -s NeedleFactory:I AndroidRuntime:E
```

Android Studio can also run the `android` configuration. Debug APK:
`android/build/outputs/apk/debug/android-debug.apk`. JVM test results:
`core/build/reports/tests/test/index.html`. Lint:
`android/build/reports/lint-results-debug.html`.

## Diagnostic screen and review

Both launchers reach the same portable Kotlin core. Tap/click anywhere to see
a marker and press count. Rotate/resize, then leave and return to observe
lifecycle counters. Debug diagnostics show FPS, averaged frame interval,
viewport pixels, app version and build type. Android release builds disable
these metrics/logs; desktop defaults to development diagnostics. No simulation
clock, gameplay, economy or persistence exists yet. Process death starts a new
diagnostic session with zero counters.

CI validates JVM tests and both launcher builds; it does not run a GUI/emulator.
See [Stage 0 report](docs/STAGE_0_REPORT.md) for versions, evidence, known
limitations and the owner test checklist. **Stage 1 remains locked.**

# NeedleFactory

A mobile factory/physics game for Android built around the fantasy of finding a needle in increasingly complex material streams, then automating the process into a satisfying production line.

## Status
**Stage 0 — accepted after physical testing on Samsung SM-S938B.**

**Stage 1 — tactile search vertical prototype authorized and in development.**
Stage 2 remains locked until Stage 1 is human-playtested and explicitly accepted.

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
```

Android Studio can run the `android` configuration directly on a connected
device. Stage 0 was physically validated on Samsung SM-S938B.

## Milestone documents

- `docs/STAGE_0_REPORT.md` — accepted technical foundation.
- `docs/STAGE_1.md` — authorized tactile-search prototype scope.
- `CODEX_STAGE_1_PROMPT.md` — execution brief for Codex CLI.

Stage 1 must stop for human playtesting before any Stage 2 work.

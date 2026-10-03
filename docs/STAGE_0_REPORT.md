# Stage 0 report — NeedleFactory

Date: 2026-10-03. Authorization: **Stage 0 only**.

**Stage 0 accepted by the owner on 2026-10-03**, following successful physical
testing on Samsung SM-S938B and authorization to commit and push the Stage 0
foundation. Stage 1 remains locked and requires separate explicit approval.
No gameplay, economy, machines, progression, save data or production art was
implemented.

## Selected toolchain

| Component | Actual selection / observation |
| --- | --- |
| Host | Windows 11, x64 |
| Android Studio | Installed build `AI-261.26222.65.2614.16379836`; settings directory identifies 2026.1.4 |
| Gradle JDK | JetBrains Runtime OpenJDK `21.0.11+1-b1163.116`, 64-bit |
| Java / Kotlin bytecode | Both target JVM 11, independently of the Gradle JDK |
| Gradle wrapper | 8.13, binary distribution, official SHA-256 pinned |
| Android Gradle Plugin | 8.13.2 |
| Kotlin plugin / stdlib | 2.3.21 |
| libGDX core + backends + natives | 1.14.2 |
| LWJGL | 3.3.3, transitively selected by libGDX |
| gdx-jnigen-loader | 2.5.2, transitive |
| Android compile / target SDK | 36 / 36 |
| Minimum SDK | 23 (Android 6.0) |
| Android Build Tools | 35.0.0, AGP default, locally installed |
| Other installed SDK components | Platforms 34, 35, 36; Build Tools 36.1.0 also present |
| AndroidX core | 1.17.0, required transitively by the Android backend |
| Tests | JUnit 4.13.2, test scope only |
| KTX | Not included; no Stage 0 use case |
| Box2D | Not included; no physics bodies or simulation yet |
| App version | 0.0.1, Android versionCode 1 |

Versions were verified against official references and then resolved/built,
not copied blindly from planning documents. [libGDX setup](https://libgdx.com/wiki/start/project-generation)
lists 1.14.2; the [official liftoff project](https://github.com/libgdx/gdx-liftoff)
and [Kotlin example](https://github.com/libgdx/gdx-liftoff-demo-kotlin)
were used as module, launcher and native-packaging references.

[AGP 8.13 compatibility](https://developer.android.com/build/releases/gradle-plugin?hl=es-419)
specifies Gradle 8.13, JDK 17 minimum, SDK 36 support and Kotlin 2.3 support in
8.13.2. [Kotlin's compatibility table](https://kotlinlang.org/docs/gradle-configure-project.html)
includes this Gradle/AGP pair for Kotlin 2.3.21. Kotlin 2.3.21 is a stable bugfix
release, as recorded in the [release history](https://kotlinlang.org/docs/releases.html).

This is a compatible stable baseline, rather than a requirement to adopt every
latest major version. The installed SDK and successful builds validate the
selection. Minimum SDK 23 follows the actual AndroidX dependency floor and
keeps reasonable device coverage; minimum SDK hardware was not runtime tested.
Physical Samsung testing is recorded below. Compile/target SDK was not lowered
to support older devices. Gradle's
`--version` reports embedded Kotlin 2.0.21 for its DSL; this is distinct from the
project's Kotlin 2.3.21 compiler.

## Dependency and license plan

Only Kotlin stdlib, libGDX core and the two platform backends are directly
needed at runtime. Android native libraries are extracted to generated build
output for `armeabi-v7a`, `arm64-v8a`, and `x86_64`. No extra frameworks or
external skill repositories were installed/vendored.

libGDX and its native loader are Apache-2.0, verified in their resolved Maven
POMs and the [libGDX license](https://github.com/libgdx/libgdx/blob/master/LICENSE).
Kotlin stdlib is Apache-2.0 (resolved POM); AndroidX is
[Apache-2.0](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt).
Gradle/wrapper are [Apache-2.0](https://github.com/gradle/gradle/blob/master/LICENSE).
LWJGL uses [BSD-3-Clause](https://www.lwjgl.org/license), also verified in its POM;
its bundled native components retain their own notices. JUnit is test-only
[EPL-1.0](https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt).
The standard desktop backend also brings JLayer 1.0.1-gdx (LGPL-2.1) and
JOrbis 0.0.17 (LGPL), verified in resolved POMs. Audio is disabled in Stage 0,
but these standard transitive artifacts remain packaged. Preserve third-party
licenses/notices and satisfy applicable redistribution terms when preparing
distribution artifacts; this report does not replace their license texts.

Resolved runtime graphs were saved locally as
`verification/android-dependencies.log` and `verification/desktop-dependencies.log`.
The Android graph includes AndroidX lifecycle/coroutines transitively; these
are backend requirements, not an application architecture framework choice.

## Architecture actually created

- `core`: portable Kotlin/JVM library, no Android or LWJGL backend imports.
  `game` owns `NeedleFactoryGame` and immutable platform-supplied `BuildInfo`.
  `presentation` owns the replaceable diagnostic screen and rendering resources.
  `debug/BootStatus` is pure Kotlin diagnostic input/lifecycle state.
- `android`: launcher, BuildConfig metadata and Android system-bar/cutout insets.
  Orientation changes resize the surface; libGDX forwards pause/resume.
- `lwjgl3`: desktop launcher, 60 FPS cap/vsync and an opt-in real OpenGL smoke
  harness under test sources.
- Future `simulation`, `physics`, `factory`, `economy`, `content`, `ui`, `save`
  packages contain only reservation notes. No speculative runtime classes.
- `assets/`: shared prototype asset location; no production assets. The bundled
  font is loaded through libGDX's asynchronous AssetManager loaders. File reads
  happen on the asset worker; OpenGL work happens on the render thread.

No simulation clock was introduced because there is no simulation. The
conditional fixed-step test/tick diagnostic therefore does not apply.
Pause/resume counters are diagnostic state, not a persistence system; cold
launch/process death resets them. Android release metadata disables developer
metrics/logs. The bitmap font and simple geometry are prototype-only.

## Commands and results

Commands below ran from the repository root with local JDK/SDK supplied through
process environment variables. Local paths are deliberately absent from
versioned configuration. README contains reproducible setup/run commands.

```powershell
.\gradlew.bat --version
.\gradlew.bat :core:test :lwjgl3:assemble :android:assembleDebug :android:lintDebug --console=plain
.\gradlew.bat :lwjgl3:run --console=plain
.\gradlew.bat :lwjgl3:diagnosticSmoke :android:assembleDebug :android:lintDebug --console=plain
.\gradlew.bat :core:clean :lwjgl3:clean :android:clean :core:test :lwjgl3:assemble :android:assembleDebug :android:lintDebug :android:assembleRelease :lwjgl3:diagnosticSmoke --console=plain
.\gradlew.bat prepareKotlinBuildScriptModel projects --console=plain
.\gradlew.bat :android:dependencies --configuration debugRuntimeClasspath --console=plain
.\gradlew.bat :lwjgl3:dependencies --configuration runtimeClasspath --console=plain
.\gradlew.bat :core:test :lwjgl3:assemble :android:assembleDebug :android:lintDebug :android:assembleRelease :lwjgl3:diagnosticSmoke --console=plain
.\gradlew.bat :android:assembleDebug :android:lintDebug :android:assembleRelease --console=plain
git diff --check
```

The combined build reruns relevant checks after changing font loading to the
async asset worker: **111 tasks successful, 31 executed and 80 up-to-date**.
The preceding clean build completed **114 tasks successfully**. The final
Android-only check after the insets correction completed **102 tasks
successfully, 20 executed and 82 up-to-date**.
The pre-commit verification after recording owner acceptance also passed:
**111 tasks successful, 15 executed and 96 up-to-date**, including the desktop
OpenGL smoke. JVM tests reused their passing results (2 tests, 0 failures);
Android lint retained 0 errors and the same 4 version-update warnings.

| Check | Result |
| --- | --- |
| Pure JVM core tests | 2 passed, 0 failures; lifecycle/input behavior and duplicate transition handling |
| Desktop assembly | Passed; launcher and distributions built |
| Desktop normal launcher | Same diagnostic core rendered; stable logs around 60–61 FPS after startup; stopped deliberately with Ctrl+C |
| Desktop OpenGL smoke | Passed; 240 frames, actual window resize 960×640 → 640×960, synthetic input and pause/resume callbacks |
| Android debug APK | Built and installed successfully |
| Android unsigned release APK | Built; generated BuildConfig has DEBUG=false; release APK was not installed/signed for distribution |
| Android lint | 0 errors; 4 version-update warnings only |
| IDE Gradle script model / project configuration | Passed; exactly core/android/lwjgl3; GUI Android Studio Sync not directly exercised |
| GitHub Actions | Minimal tests/desktop/Android build workflow added; not executed remotely in this session |

Lint suggests Gradle 8.14.5, AGP 9.4.1 and Kotlin 2.4.20 (the Kotlin warning
appears for both plugins). These warnings remain visible. No forced major
toolchain migration was made after validating the chosen compatible baseline.
AGP also reports that prebuilt `libgdx.so` could not be stripped and packages it
unchanged; no NDK is needed for this skeleton.
The compiler also notes the platform deprecation of `setDecorFitsSystemWindows`;
its explicit false setting supports Android versions before enforced
edge-to-edge behavior. There is no compilation error.

## Android runtime evidence

Initial ADB inventory had no physical device or running emulator. Available
AVDs: `Pixel_9_Pro` and `Medium_Phone_API_36.1`; only Pixel 9 Pro was run.
Android Emulator version: 36.3.10. Tested guest: Android 16 / API 36, x86_64,
4 KiB pages, 2 GiB configured RAM, SwiftShader software graphics. Pixel AVD
physical display 1280×2856, existing override 1080×2340.

The existing Pixel AVD was started without a visible emulator window, audio
or snapshot saving. Actual ADB operations included:

```text
adb devices -l
adb -s emulator-5554 shell getprop sys.boot_completed
adb -s emulator-5554 shell getprop ro.build.version.sdk
adb -s emulator-5554 shell getconf PAGE_SIZE
adb -s emulator-5554 install -r android/build/outputs/apk/debug/android-debug.apk
adb -s emulator-5554 shell am start -W -n com.needlefactory.android/.AndroidLauncher
adb -s emulator-5554 shell input tap 540 1200
adb -s emulator-5554 shell input keyevent KEYCODE_HOME
adb -s emulator-5554 shell am start -W -n com.needlefactory.android/.AndroidLauncher
adb -s emulator-5554 shell cmd window user-rotation lock 1
adb -s emulator-5554 shell cmd window user-rotation lock 0
adb -s emulator-5554 shell am force-stop com.needlefactory.android
adb -s emulator-5554 shell am start -W -n com.needlefactory.android/.AndroidLauncher
adb -s emulator-5554 shell screencap -p /sdcard/needlefactory-portrait.png
adb -s emulator-5554 pull /sdcard/needlefactory-portrait.png verification/android-portrait.png
adb -s emulator-5554 logcat -d -s NeedleFactory:I AndroidRuntime:E
zipalign -c -P 16 4 android/build/outputs/apk/debug/android-debug.apk
```

Cold launch returned `Status: ok`; touch produced `Presses: 1`; Home/return
produced `Pauses: 1 Resumes: 1`; orientation produced a resized viewport.
Force-stop/cold launch reset the counters and relaunched successfully.
Screenshots of both orientations and desktop framebuffers were visually
inspected. Safe-area padding and readable text were demonstrated; a tight
landscape footer gap was corrected. No AndroidRuntime crash was observed.
The final rebuilt APK was reinstalled and checked again.
The final orientation round trip yields 1080×2140 portrait and 2340×880
landscape viewports. An earlier retained side inset was fixed by explicit
window inset ownership and requesting fresh insets on configuration changes.

Local images/logs are under ignored `verification/`; build/test/lint outputs
are under ignored module `build/` directories. ZIP alignment check passed;
this alone does not constitute a 16 KiB-page hardware runtime test.

### Physical device test — owner reported

On 2026-10-03 the owner reported a successful physical test on **Samsung
SM-S938B**:

| Check | Result |
| --- | --- |
| Application launch | Passed |
| Touch input | Passed; touches work |
| Rotation | Passed; checked by the owner |
| Pause/resume | Passed; checked by the owner |
| Observed performance | Approximately 120 FPS |

This is owner-reported runtime evidence. Android/One UI version, installed
APK hash, active ABI, page size, test duration and thermal conditions were
not supplied. The FPS observation validates smooth operation of this Stage 0
diagnostic on the tested phone; sustained performance and future gameplay
budgets still require profiling.

## Performance and known limits

Balanced target: **60 FPS / 16.67 ms frame interval**. Desktop reaches roughly
60 FPS when the build is idle; concurrent compilation caused brief lower
readings. Software-rendered Android emulator showed roughly 12–24 FPS after
startup and larger spikes on rotation/background transitions. This is an
observed emulator limitation. The owner observed approximately **120 FPS on
Samsung SM-S938B**, exceeding the 60 FPS target for the Stage 0 diagnostic on
that physical device. This was an observation, not a controlled benchmark.
Android View `gfxinfo` does not measure the game's GL loop reliably; use the
game diagnostics and later device profiling rather than treating it as a game
frame benchmark. There is no earlier executable baseline for a regression
comparison.

Per-frame vectors/renderers/batches/colors are reused; metric strings update
twice per second, debug logging every two seconds. No game file/network work
occurs in the render loop; smoke screenshots are written after its loop stops.
Body/particle/texture/simulation budgets await a Stage 1 prototype measurement,
as planned. No speculative body count budget or physics engine was added.

Not tested: minimum SDK device, additional physical devices, 16 KiB runtime,
macOS/Linux GUI, desktop real mouse/focus interaction, release runtime and
Android Studio's GUI Sync. No known blocker-level application crash remains
on the tested Windows/API 36 emulator configurations or was reported in the
owner's successful Samsung physical test.

## Deliberate adaptations and resolved failures

- Integrated the official liftoff layout into the existing repository manually
  with Kotlin DSL/version catalog, preserving the product documents. Did not
  run a generator over the repository or adopt its optional extensions.
- Used the locally available Gradle 8.13 baseline with AGP 8.13.2 and Kotlin
  2.3.21 instead of blindly combining newer generator defaults. Compatibility
  tables and successful graph/build checks support the selection.
- Enabled AndroidX after the first build exposed the backend's transitive
  requirement. That initial failure is fixed; no Compose/DI/ECS was added.
- Native extraction runs as an incremental build task and writes only into
  `android/build`, fixing an initial configuration-time resolution warning.
- Sandbox access to the user Gradle cache initially failed; build/checksum
  retrieval was repeated with approved access. No machine-specific paths,
  signing material, local.properties or generated outputs are proposed for Git.
- Async font loading uses libGDX's existing AssetManager, with no new runtime
  dependency, to obey the render-thread I/O boundary.
- The async-loading smoke test caught an outdated default-font resource name.
  Inspected the actual 1.14.2 JAR and corrected the path to `lsans-15.fnt`.
- Rechecked Android orientation after correcting stale cutout padding. Uses
  native Android window APIs and explicit libGDX `renderUnderCutout`, avoiding
  a separate direct AndroidX dependency just for a convenience helper.

## Owner review checklist

- [ ] Open the root in Android Studio with JDK 21 and SDK 36; verify GUI Sync.
- [ ] Run `:core:test` and `:android:assembleDebug` using README commands.
- [ ] Run `:lwjgl3:run`; click, resize, minimize/restore and close normally.
- [x] Physical phone check: Samsung SM-S938B launches successfully; touch,
  rotation and pause/resume passed, as reported by the owner on 2026-10-03.
- [x] Record physical-device FPS: approximately 120 FPS observed by the owner;
  OS version, warm-up duration and thermal state were not supplied.
- [ ] On future device checks, record APK hash, OS, ABI/page size, duration,
  thermal state, text/markers/counters and safe areas in both orientations.
- [ ] Review module/package boundaries, prototype-only visuals and this report.
- [x] Owner accepted Stage 0 and authorized its commit/push on 2026-10-03.
  Stage 1 requires separate explicit authorization and remains locked.

## Proposed Stage 1 entry point — not implemented

After explicit approval, replace the diagnostic screen with one placeholder
tray, one hidden needle and touch manipulation/discovery/restart feedback.
First measure touch feel, camera readability and mid-range device cost; then
introduce only the physics/fixed-step state actually required, independent of
rendering. Measure time-to-find before expanding art or economy. Stage 1 code
has not begun.

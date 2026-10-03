# Stage 1 report — tactile search prototype

Date: 2026-10-03. Base: `origin/main` at `94996d8`, safely fast-forwarded from
the accepted Stage 0 commit with a clean working tree. All eleven documents
listed in `CODEX_STAGE_1_PROMPT.md` were read before editing.
Before the acceptance commit, `main` was fast-forwarded again to `bbe43f7`,
which added earlier owner-feedback notes in `STAGE_1.md`. Those notes are
retained as history; the subsequent explicit owner acceptance supersedes their
pre-acceptance change requests. No additional gameplay changes were made in
this acceptance task.

**Stage 1 accepted/completed by the owner on 2026-10-03**, following a manual
10-round playtest on Samsung SM-S938B. The owner authorized one Stage 1 commit
and push to `origin main`. Stage 2 remains locked and requires separate explicit
authorization. No economy, machines, tools, persistence, analytics or production
art was added.

## Dependencies

- `com.badlogicgames.gdx:gdx-box2d:1.14.2`, matching existing libGDX exactly.
- Matching `gdx-box2d-platform:1.14.2` natives for desktop and Android
  `armeabi-v7a`, `arm64-v8a`, `x86_64`.
- JVM integration tests also load matching libGDX core desktop natives; no
  OpenGL or Android context is required for these tests.
- KTX is not added: direct body/world/fixture ownership is small and explicit.
  No ECS, DI or additional runtime framework is introduced.

The [official libGDX Box2D documentation](https://libgdx.com/wiki/extensions/physics/box2d)
supports explicit initialization, fixed stepping and disposing native shapes.
libGDX's extension retains its Apache-2.0 license and bundled native notices.
Existing toolchain pins are unchanged.

## Physics and touch tunables

All values are centralized in `content/SearchConfig.kt`; distances below are
Box2D metres, not screen pixels.

| Setting | Current value |
| --- | --- |
| Tray / gravity | 10 × 14 m / zero |
| Clutter | 60 strips; length 0.85–1.9 m, width 0.16–0.32 m; four warm tones |
| Needle | 1.3 × 0.1 m; one per round |
| Physical bodies | 66: 60 clutter + needle + 4 walls + reusable pusher |
| Step / solver | 1/60 s; 6 velocity / 2 position iterations |
| Catch-up | At most 5 steps/frame; accept at most 0.1 s delta; discard overdue whole steps |
| Pusher | Dynamic circular body, radius 0.38 m, density 8, max target velocity 10 m/s |
| Clutter | Density 0.65; linear/angular damping 5 / 7; speed cap 8 m/s |
| Needle | Density 32; linear/angular damping 12 / 12; speed cap 1.5 m/s |
| Angular cap | 5 rad/s |
| Fixture size | 75% of each visible strip/needle dimension |
| Exposure | 15 axis samples; at least 50% must be uncovered |
| Hit tolerance | Capsule radius = 0.24 m touch slop + half needle width |
| Tap | At most 0.28 m travel and 0.45 s hold |
| Success beat | 0.7 s before Next pile becomes available |

Clutter and needle collide with walls and the pusher, not each other. This
permits visual overlap without pairwise bulk contacts. Dynamic bodies use
continuous collision detection. Input moves a finite-mass physical pusher
toward its target with bounded velocity; release/cancel removes its active
fixture from interaction. A second touch is ignored.

The visible oriented rectangle is constrained to the tray after each step.
This accommodates smaller collision fixtures and initial touch overlaps at
walls. Constraint corrections cancel outward velocity and angular motion;
they do not inject energy. Debug logs count these corrections as `bounds`.
Material collected against a wall can look denser and stop abruptly: include
edge handling in the owner feel review.

Both previous/current poses are retained; rendering interpolates position and
shortest-path angle at the accumulator alpha. Native resources are recreated
only on explicit Next pile, and disposed when leaving the screen. No file or
network operations run inside the gameplay loop. Fonts retain the Stage 0
async loader. Reused vectors, batches, renderers and cached HUD strings avoid
per-frame object creation; the success burst is twelve procedural rays.

## Round generation, exposure and discovery

`java.util.Random(seed)` defines initial poses, strip sizes/tones and needle
angle. Needle centre stays at least 2.5 m from side walls and 4 m from end
walls. Nine deliberate cover strips surround it; one fully covers its axis.
Remaining strips are randomized across the tray. Initial coverage is tested
over 500 seeds. Exactly one needle exists, and the seed is shown/logged.

Next pile advances the signed seed by a fixed odd 64-bit increment and resets
the round while retaining the session Found count. Seed overflow is defined
and tested. A seed reproduces generation, not guaranteed bit-identical native
physics trajectories across platforms/ABIs.

Exposure samples the needle axis against oriented **visible** clutter
rectangles, independent of collision fixtures. A find requires sufficient
exposure and a hit on an uncovered part of the needle axis. Both current and
previous snapshots must pass, conservatively accounting for interpolation.
Exposure is checked before activating a touch pusher: cover moved by that
same blind tap cannot turn it into an immediate find. Discovery uses a short
tap on release, rather than triggering while a finger sweeps past the needle.

On find, simulation freezes, the needle rises above the pile with a short
accent burst, Found increments once, and the portable `PlatformFeedback`
callback requests Android View haptic confirmation on the main thread.
Android's haptic preferences are respected. Desktop feedback is a no-op.

## Architecture and lifecycle

- `simulation/SearchRound.kt`: seeded generation, domain poses, visual exposure,
  discovery and `SearchSession` transitions. No renderer/Android imports.
- `simulation/FixedStep.kt`: pure bounded accumulator and interpolation alpha.
- `physics/SearchPhysics.kt`: Box2D ownership, pusher intent and pose publication.
- `presentation/SearchScreen.kt`: input adapter, replaceable geometry, HUD,
  interpolation and success animation.
- `game/PlatformFeedback.kt`: platform callback; Android implementation stays
  in `AndroidLauncher`.

The existing module boundaries remain. No broad refactor was needed; the old
diagnostic is preserved for desktop `-Pscreen=diagnostic` and diagnostic smoke.
Screen hiding now happens before native resource disposal.

Pause/resume preserves in-memory seed, poses, elapsed active time and Found.
It cancels the gesture, clears the accumulator and skips the first resumed
frame delta. Rotation fits the same 400 × 760 logical composition, cancels
the gesture and retains the round. Landscape intentionally has side bars.
Process death resets the session; no save system is implied.

## Verification

```powershell
.\gradlew.bat :core:test :lwjgl3:assemble :android:assembleDebug :android:lintDebug :android:assembleRelease :lwjgl3:diagnosticSmoke --console=plain
.\gradlew.bat :lwjgl3:prototypeSmoke --console=plain
.\gradlew.bat :lwjgl3:run --console=plain
git diff --check
```

| Check | Result |
| --- | --- |
| JVM tests | 15 passed, 0 failures: 2 retained diagnostic, 7 rules/generation, 3 clock, 3 native physics |
| Seed invariants | 500 generated seeds: fully hidden needle, correct count, visible geometry inside tray, repeatable generation |
| Native physics | Slow precise movement, bounded pusher, fast sweeping, settling, no body growth or escaped visible geometry over six seeds including an earlier failing seed |
| Fixed-step clock | Equal 10 s simulation at 30/60/120 Hz; interpolation, reset, pathological/invalid delta and bounded catch-up |
| Desktop / Android debug / unsigned release | Passed; combined build: 111 tasks successful, 68 executed, 43 up-to-date |
| Android lint | 0 errors, 5 version-update warnings |
| Stage 0 diagnostic smoke | Passed; old diagnostic remains runnable, 240 OpenGL frames |
| Stage 1 prototype smoke | Passed; 10 seeded rounds through screen input, 34,241 accelerated half-step frames, 66 bodies throughout |
| Prototype smoke coverage | Covered blind tap rejected, physical sweeps expose needle, find once, one feedback callback/find, repeated Next pile, pause/resume state retention, fitted landscape/portrait and clean shutdown |
| ZIP alignment | `zipalign -c -P 16 4` passed for debug APK; not a 16 KiB-page runtime test |
| Remote CI | Updated workflow label; local results below do not constitute a remote CI run |

Smoke gestures/time are synthetic and accelerated, with real OpenGL rendering
and Box2D. Ten scripted rounds are engineering evidence, **not the required
human playtest**. Scripted find times were roughly 13.7–72.6 simulated active
seconds; repeated broad passes took longer on two seeds. The owner subsequently
completed the required manual playtest and accepted the iteration, as recorded
below. No manual find-time range or detailed feel notes were supplied.

Lint keeps four existing toolchain-update warnings and adds a Box2D version
warning suggesting `3.1.1-0`. Stage 1 explicitly requires the matching libGDX
1.14.2 extension, so no unrelated engine/version migration was made. AGP still
packages prebuilt native libraries unstripped; the existing window-inset API
deprecation warning remains.

Resolved during validation: standalone native tests needed core native loading;
initial wall trapping required finite pusher mass plus visual containment;
the native world needed to remain alive through Game.hide on disposal; needle
inertia/damping was tuned after a difficult scripted seed. These failures are
fixed in the current implementation.

## Android/device evidence and performance

Debug APK SHA-256:
`B6F88EE8364D9AD0B84C82B9FCA0438AE7D1FE5D737B5C43C2F3C767A822FB24`.

The APK installed successfully on connected **Samsung SM-S938B**. ADB reported
Android release `17`, API `37`, One UI property `90000`, 4 KiB pages and
1080 × 2340 physical display. `am start -W` returned `Status: ok`; the app
process remained alive. Initially the screensaver/lock screen prevented
foreground testing; after unlocking, the actual prototype was visually
inspected. Debug logs sampled **121 FPS / approximately 8 ms** with 66 bodies,
including successful finds and new seeded rounds. Recent find summaries
recorded 39.19 s (including idle search time), 3.26 s and 2.81 s; these are
observations, not a controlled 10-round human-playtest result. The active
phone samples reported frame peaks around 11–29 ms with no dropped physics
time; sustained thermal behaviour is not validated.

While the app was confirmed foreground, ADB sent a 1,500 ms slow drag, a
180 ms broad sweep, Home and return. The round seed `2691343689449507819`
survived pause/resume. No app crash appeared in the process-filtered runtime
logs. Portrait controls/metrics stayed inside the safe area. Automated rotation
was exercised on desktop and emulator; the owner subsequently checked physical
rotation during the accepted Samsung playtest. Separate subjective haptic notes
were not supplied.

The same APK installed/launched on the existing Pixel 9 Pro Android 16/API 36
software-rendered emulator. Slow/fast ADB swipes registered two touches and
12.9 m of finger path; Home/return and portrait/landscape round trips retained
seed 42 and 66 bodies. Fitted landscape and portrait screenshots were inspected.
Software rendering showed roughly **13–21 FPS / 48–98 ms** with larger
transition spikes and bounded dropped catch-up time. This remains an emulator
limitation, comparable to Stage 0's software-rendering observations.
The ordinary desktop launcher showed **60–61 FPS / about 16 ms**; a find with
real mouse input was observed at 60 FPS (active time 22.20 s, peak 25.84 ms).
Local screenshots/logs are ignored under
`verification/`; APK/test/lint outputs remain ignored build products.

Target stays stable 60 FPS on Samsung. Body budget is 66, far below a few
hundred. No thermal, sustained allocation, weak-device or 16 KiB hardware
profiling has been completed. Debug active time excludes loading/background
and the first lifecycle/resize frame; drag distance measures clamped finger
path in tray metres. FPS/average frame interval/peak are observations, not GPU
profiling or a guarantee of sustained gameplay performance.

## Owner playtest and acceptance

On 2026-10-03 the owner reported completing the manual Stage 1 playtest on
**Samsung SM-S938B**, accepted the current iteration and authorized its commit
and push. This satisfies the human review/acceptance gate for Stage 1 only.

| Owner playtest check | Result |
| --- | --- |
| Manual rounds | 10 completed |
| Slow precise drag / fast sweeping | Passed in the accepted playtest |
| Blind tap on a fully hidden needle | Rejected; cover must be cleared before discovery |
| Next pile | Passed; fresh rounds can be started repeatedly |
| Background/resume | Passed; current in-memory round is preserved |
| Physical rotation | Passed; return to portrait preserves the round |
| Performance | Current iteration accepted; recorded physical-device debug observations were approximately 121 FPS / 8 ms with 66 bodies |
| Owner decision | Stage 1 accepted/completed; one commit/push authorized; Stage 2 not authorized |

The approximately 121 FPS observation comes from the physical-device debug
samples recorded during implementation, rather than a newly supplied numeric
benchmark for all ten manual rounds. The owner did not provide a separate
find-time range, problematic-seed list, thermal measurements or detailed
subjective/haptic notes. Acceptance is recorded without inventing those values;
the profiling limits above remain applicable. No blocker was reported with
the accepted playtest.

## 10-round checklist retained for regression testing

The debug APK is already installed on Samsung. Unlock and open **NeedleFactory**.
For rebuilding/installing manually, use README commands. Record the seed for
any difficult round and keep logcat `NeedleFactory` summaries if possible.

1. Complete **at least 10 rounds** using Next pile; record approximate min/max
   find time, difficult seeds and whether repeating the search feels worthwhile.
2. In several fresh piles, tap covered material first. No fully covered needle
   should be found without clearing its cover.
3. Compare slow precise drags and fast broad sweeps: evaluate lag, control,
   accidental taps, tunnelling, explosive movement and behaviour at tray edges.
4. Check conceal/reveal readability, metallic needle contrast and tapping a
   visible section. Distinguish "not sufficiently exposed" from an unreliable hit.
5. Check the highlight/burst, one haptic confirmation, one Found increment and
   delayed Next pile. Note whether the feedback feels pleasant.
6. Verify fresh cover/new seed and stable body count after repeated Next pile.
7. Background during a search, wait, return and continue the same pile. Repeat
   once during success feedback. Nothing should auto-find or auto-restart.
8. Rotate to landscape and return to portrait: retain the pile/Found and keep
   UI within safe areas. Landscape fitting is intentional.
9. Play for several minutes; observe FPS, hitching and heating. Target is
   stable 60 FPS; 120 FPS is optional. Try a second finger/cancelled gesture.
10. Record whether touch feels enjoyable enough to continue, plus one priority
    improvement. The current Stage 1 iteration is accepted; future changes and
    any next milestone remain subject to their own authorized scope.

**Stage 1 is accepted/completed. Stage 2 remains locked.**

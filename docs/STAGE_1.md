# Stage 1 — Tactile Search Vertical Prototype

## Authorization
Stage 0 is accepted. **Stage 1 is now authorized. Stage 2 remains locked.**

## Goal
Prove that the literal act of finding a needle is satisfying on a real Android phone before building factory automation, economy or content breadth.

The prototype must answer one question:

> Is moving a pile with a finger, gradually exposing a hidden needle, and finding it enjoyable enough to repeat?

If the answer is not yet yes, iterate on feel. Do not hide weak interaction behind progression systems.

## Playable loop
1. A tray appears with a loose pile of prototype material.
2. Exactly one needle is hidden under/inside the visible pile.
3. The player drags a finger through the material and physically pushes pieces aside.
4. The needle becomes visually exposed.
5. The player taps the sufficiently exposed needle.
6. The game gives a short satisfying success response.
7. A session counter increments by one.
8. The player explicitly starts a fresh randomized pile.

No shop, currency economy, machines, automation, research, prestige, contracts or save-game progression in Stage 1.

## Physics model

### Engine
Add libGDX Box2D at the same libGDX version already pinned by the project. Add KTX Box2D only if it clearly reduces code without obscuring ownership/lifecycle.

### World
Use top-down 2D physics with zero gravity.

Use a fixed simulation step of **1/60 s** with an accumulator and a bounded number of catch-up steps. Rendering must remain independent of simulation rate. If uncapped/high-refresh rendering is retained, interpolate body transforms so 120 Hz displays do not visibly stutter from a 60 Hz simulation.

Clamp pathological frame deltas after resume/debugger pauses.

### Prototype pile
Start around **50–70 lightweight clutter pieces**. Exact count is tunable from one centralized prototype configuration.

Prototype material should visually read as straw/wood-like strips:
- varied length, width, angle and warm tone;
- simple replaceable art, not production art;
- collision fixtures may be smaller than the visible strip to permit natural-looking visual overlap.

To preserve the intended bulk-simulation strategy, clutter pieces do **not** need full clutter-vs-clutter rigid collision if that makes the pile noisy or expensive. A preferred first implementation is:
- clutter collides with tray boundaries and the touch pusher;
- clutter pieces can visually overlap one another;
- linear/angular damping makes disturbed material settle;
- no decorative particle may become authoritative gameplay state.

### Needle
One slender metallic body per round:
- visually distinct only when actually exposed;
- initially placed away from impossible edge positions;
- slightly more inertial/stable than filler so it does not launch unrealistically;
- remains inside the tray;
- can be contacted by the touch interaction without becoming uncontrollable.

## Touch interaction

The finger is not a teleporting force ray.

Represent an active touch as a small physical pusher/rake in world space:
- create/activate on touch down;
- move toward the target with bounded velocity;
- collide continuously enough to avoid obvious tunneling;
- remove/deactivate on touch up/cancel;
- support one primary touch for Stage 1; multi-touch is not required.

Tune pusher radius, max velocity, damping and body mass for tactile feel. Centralize tunables.

The player must be able to make small precise movements as well as broad clearing sweeps.

## Occlusion and discovery

The player must not be able to win by blindly tapping through fully covered material.

Render the needle below clutter until success. Determine exposure from visual coverage, not merely render order.

Recommended cheap deterministic method:
- sample several points along the needle's visible axis;
- test whether each point lies inside any clutter visual footprint;
- compute visible fraction from uncovered samples.

A needle becomes discoverable only after a configurable minimum visible fraction is reached (initial target around 40–60%; tune by feel).

A successful find requires:
1. the needle is sufficiently exposed; and
2. the tap/hit test is on the needle.

On successful find, raise the needle visually above the pile for the success animation.

## Round generation

Each round uses a recorded random seed.

Requirements:
- exactly one needle;
- needle begins meaningfully occluded;
- needle is never spawned in an impossible/unreachable location;
- enough cover pieces are deliberately placed around its initial area to guarantee a real search;
- remaining clutter is randomized across the tray;
- debug logs/overlay expose the current seed so a bad round can be reproduced.

Do not build procedural-generation infrastructure beyond what this one round generator actually needs.

## Success feedback

On find:
- brief highlight/glow/scale or lift animation;
- short particle burst or similarly cheap visual accent;
- Android haptic confirmation through a small platform feedback abstraction;
- no copyrighted/external audio asset is required for Stage 1.

After the short success beat:
- show session value such as **Found: N**;
- show a clear **Next pile** action;
- next pile resets the round cleanly with a new seed.

This is a prototype score only, not a currency/economy system.

## Presentation

Direction: clean stylized industrial work tray, consistent with the Art Bible.

Stage 1 is placeholder art, but it should already be pleasant to look at:
- dark/neutral tray;
- warm organic clutter;
- metallic needle with a readable specular/highlight treatment;
- restrained UI;
- no debug boot-screen look in the main playable screen.

Keep debug metrics available in debug builds without dominating the game screen.

Primary test orientation is portrait. Landscape must not crash or corrupt the round; a fitted/letterboxed playable presentation is acceptable for Stage 1 if full responsive composition would distract from the interaction test.

## Runtime architecture

Keep these responsibilities separate:
- round state / game rules;
- Box2D world and body ownership;
- input intent;
- exposure/discovery calculation;
- rendering/animation;
- Android haptic implementation.

Do not let Box2D bodies become the only source of domain state.

Add only abstractions justified by this prototype. No ECS/DI framework.

## Instrumentation

For each round in debug builds record:
- random seed;
- time from round start to successful find;
- touch count;
- approximate drag distance;
- current physics body count;
- FPS/frame-time metrics already available from Stage 0.

After a find, print one compact summary line to debug log.

## Performance budget

Stage 1 target:
- stable **60 FPS** on the physical Samsung SM-S938B;
- no sustained frame-time spikes caused by allocation-heavy input/physics/render loops;
- keep authoritative body count comfortably below a few hundred in this prototype;
- no file/network I/O in the render loop.

120 FPS is welcome if naturally achieved, but is **not** a Stage 1 requirement. Interaction consistency matters more than a benchmark number.

## Tests

Add deterministic JVM tests where logic is platform-independent, especially:
- seeded round generation invariants;
- exposure/discovery threshold logic;
- state transition SEARCHING → FOUND → NEXT ROUND;
- fixed-step accumulator behavior if introduced as pure logic.

Box2D feel itself requires runtime testing; do not pretend unit tests replace device play.

## Required human playtest

On Samsung SM-S938B:
1. Complete at least 10 rounds.
2. Confirm no impossible rounds.
3. Confirm slow precise movement and fast sweeping both feel controllable.
4. Confirm blind tapping cannot find a fully covered needle.
5. Confirm success feedback is obvious and pleasant.
6. Confirm Next pile reliably creates a fresh round.
7. Background/resume during a round and continue.
8. Rotate once and return to portrait without corrupting state.
9. Observe FPS and obvious thermal/jank behavior for at least several minutes.

Record qualitative feedback and find-time range in the Stage 1 report.

## Explicit non-goals
Do not implement:
- magnets or tools;
- conveyors or machines;
- shop;
- money/prices;
- upgrade tree;
- offline progress;
- persistence across process death;
- analytics SDK;
- ads/IAP;
- backend/account;
- final art/audio pipeline;
- multiple materials/levels.

## Deliverables
- playable Stage 1 search prototype;
- Box2D dependency integrated cleanly;
- deterministic round generator;
- touch pusher;
- exposure-gated needle discovery;
- success feedback + haptic;
- session found counter + Next pile;
- automated tests for pure logic;
- updated README run notes if needed;
- `docs/STAGE_1_REPORT.md` with exact implementation, tunables, commands/tests, performance evidence, limitations and owner playtest checklist.

## Exit gate
Stage 1 is complete only when:
- builds/tests/lint pass;
- prototype runs on the physical Samsung;
- at least 10 rounds are human-playtested;
- interaction is judged worth continuing rather than merely technically functional;
- report is complete;
- owner explicitly accepts Stage 1.

Then STOP. **Stage 2 remains locked.**


## Owner playtest feedback — iteration 1 (2026-10-03)

The first physical prototype was playable, but the following changes are required before Stage 1 can be accepted.

### Responsive orientation
- The current landscape presentation is not acceptable: it behaves like a narrow portrait column inside a wide screen.
- Recompose the UI for landscape instead of merely fitting/letterboxing the portrait layout.
- In portrait, keep the tray large and centered with compact HUD.
- In landscape, use the available width: the play tray should become a wide landscape work area and UI should move/reflow around it rather than leaving large unused side areas.
- Preserve the current round and physics state across rotation.

### Needle readability/difficulty
- The current needle is too thick and too bright/easy to spot.
- Make it materially thinner and longer, with a realistic pointed/sharp tip.
- Add a subtle blunt/eye end if useful for recognizability, but keep the silhouette slender.
- Reduce contrast/saturation/brightness so it reads as metal only after being uncovered, not as a glowing target.
- Keep hit testing usable despite the thin visual; interaction hit area may be slightly more forgiving than the rendered width, but discovery must still require actual exposure.

### Straw material
- The current filler reads as wooden sticks rather than straw.
- Make pieces thinner, longer, more fibrous/straw-like, with more variation in length, slight curvature/segmentation if cheap, and warmer natural straw tones.
- Increase piece count and/or distribution density so the tray is densely covered with very little empty background.
- The baseline Stage 1 difficulty should already require real searching; future stages may increase density further.
- Prefer broad coverage over a central clump. Avoid large empty zones.
- Keep performance healthy; use simplified collision/overlap rather than expensive full pairwise rigid-body interaction if necessary.

### Session counter lifecycle bug
- The session `Found: N` count currently resets after minimizing/backgrounding the app.
- Fix this. Background/resume must preserve both the current round and the session found counter while the process remains alive.
- Stage 1 still does not require persistence across true process death/cold restart.

### Acceptance for this iteration
Before asking for another owner review:
- verify portrait and landscape layouts visually;
- verify the needle is significantly harder to spot before clearing straw;
- verify dense straw coverage with minimal empty tray;
- verify touch clearing remains controllable at the increased density;
- verify `Found: N` survives Home/background → resume;
- rerun build/tests/lint and physical-device smoke checks.

# Stage 2 — First Automation Line

## Authorization
Stages 0 and 1 are accepted. **Stage 2 is now authorized. Stage 3 remains locked.**

## Goal
Prove that watching material physically travel through a small machine chain is understandable, satisfying and meaningfully different from manual searching.

Stage 2 must answer:

> Does the player immediately understand what each machine does, enjoy watching the chain work, and feel that manual intervention can still help without being mandatory?

This milestone validates automation and visible cause/effect. It does **not** implement the real economy, prices, upgrades, prestige, offline progress or factory-builder complexity.

## Owner feedback carried forward — hard non-regression requirements

These requirements came directly from the Stage 1 physical playtest and remain binding in Stage 2. Do not regress them.

### Responsive orientation
- Landscape must be a genuine responsive composition, not a narrow portrait column centered inside a wide screen.
- In landscape, use the screen width for the factory and reflow HUD/controls around it.
- In portrait, use a deliberately composed portrait factory layout rather than shrinking the landscape line until it is unreadable.
- Machine logic and in-flight material state must survive rotation.
- Background/resume must preserve the active batch, machine states and session counters while the process remains alive.

### Needle
- The needle must remain thin, long and recognizably sharp at one end.
- It must not be thick, oversized, brightly glowing or visually trivial to spot.
- Before extraction, use restrained metallic contrast. A brighter success highlight is allowed only after the machine has captured/recovered it.
- Interaction/detection geometry may be slightly more forgiving than the visual width, but the rendered object must remain slender.

### Straw
- Straw must read as straw, not wooden sticks.
- Use thinner, longer pieces with natural variation in length, warm straw tones and subtle segmentation/curvature where cheap.
- Material should be dense with very little empty tray/table area; do not return to a sparse central clump.
- Preserve controllable physics/performance by simplifying bulk interaction rather than reducing visual density too aggressively.

### Counters/lifecycle
- Session counters must not reset merely because the app is backgrounded and resumed.
- True process death/cold restart persistence is still outside Stage 2.

These items are acceptance criteria, not optional polish.

## Playable factory loop

The Stage 2 prototype consists of one continuous batch line:

**dense straw intake → vibration table → conveyor → magnetic separator → sorting chute/buffer → packer → packaged needle**

Residue/straw that is not captured continues to a residue/recycle bin.

Each batch contains dense straw and exactly one needle. When the line finishes the batch, the player can start the next batch; an optional auto-next debug toggle is allowed, but production economy is not.

### Manual relevance
The player may swipe/nudge material on the vibration/intake table using the familiar Stage 1 physical touch interaction. This can improve feeding/spreading but must not be required for the line to eventually complete a normal batch.

Automation should feel useful, while manual touch remains a satisfying assist rather than becoming obsolete.

## Machines

### 1. Intake / vibration table
Purpose: loosen dense straw and meter it toward the conveyor.

Required behavior:
- material begins dense, with the needle initially visually concealed;
- table visibly vibrates/oscillates;
- pieces gradually drift toward a feed edge/gate;
- manual swiping can redistribute the material;
- do not simulate thousands of expensive rigid bodies merely for visual density.

### 2. Conveyor
Purpose: transport individual material pieces through the line.

Required behavior:
- obvious moving belt/rollers;
- items move because the conveyor is running, not by unexplained teleportation;
- stopping the conveyor visibly stops downstream transport;
- use deterministic/simplified lane transport after material leaves the physics-heavy intake if that is cheaper and clearer than full Box2D.

### 3. Magnetic separator
Purpose: capture the metallic needle from the straw stream.

Required behavior:
- clearly recognizable magnet/drum/arm with visible active state;
- when the needle reaches its capture zone, it is attracted/lifted/pulled onto a metal lane/chute;
- straw is not attracted;
- capture should have a readable physical/animated transition rather than instant disappearance;
- the needle becomes more visible only during/after successful capture.

### 4. Sort chute / buffer
Purpose: make routing and machine-to-machine handoff visible.

Required behavior:
- captured needle enters a small buffer/chute;
- residue continues separately;
- queue state must be visible if the packer is busy;
- do not invent a complex quality system yet.

### 5. Packer
Purpose: provide a satisfying end-of-line payoff.

Required behavior:
- needle enters a small package/holder;
- clamps/jaws/rollers visibly cycle;
- a short completion effect/haptic fires;
- `Packaged` increments once;
- no money is awarded in Stage 2.

The packer should feel like a machine, not a button that increments a number.

## Machine controls and visible cause/effect

Stage 2 should include lightweight controls sufficient to understand the chain:
- a global Run/Pause control;
- individual machine on/off controls are allowed and preferred if they remain clear;
- turning a machine off must have an immediately understandable consequence.

If individual toggles are implemented:
- vibration off → intake stops feeding;
- conveyor off → transport stops;
- separator off → needle can pass to a visible missed-metal path/bin rather than silently succeeding;
- packer off → captured needle waits in the buffer.

No repair, wear, energy costs or upgrade prices yet.

## Batch/state model

Keep authoritative state independent from rendering.

Suggested domain concepts:
- `FactorySession`
- `FactoryBatch`
- `MachineState` / `MachineId`
- `TransportItem`
- `TransportRoute`
- `BufferState`

Exact class names are not mandatory. Avoid a framework solely to model five machines.

Each batch:
- has a reproducible seed;
- contains exactly one needle;
- tracks pieces entered/processed/residue;
- tracks needle recovered/missed/packaged;
- completes only when all authoritative items reach a terminal state.

Do not let render animations define whether production happened.

## Simulation architecture

Preserve the Stage 1 fixed-step approach where authoritative motion requires it.

Use a hybrid model:
1. Box2D/physical interaction for the intake/vibration table and direct touch manipulation.
2. Lightweight deterministic transport for conveyor/chutes/buffers where full rigid-body simulation adds cost but little gameplay value.
3. Pure visual particles/debris for decorative motion only.

Machine logic must not depend on frame rate.

Do not introduce ECS or DI.

## Layout / camera

Create two authored responsive compositions that share one machine/state model.

### Landscape
Primary factory showcase:
- left-to-right visual flow across most of the available width;
- intake/table on the left, packer/output on the right;
- HUD and compact controls use remaining top/bottom space;
- no large unused side areas.

### Portrait
Mobile portrait composition:
- reflow the same sequence vertically or as a readable zig-zag/stacked line;
- preserve meaningful machine size and touch targets;
- do not simply scale down the landscape line until labels/machines become tiny.

Rotation must preserve batch progress and machine states.

## Owner visual-quality feedback — preliminary, binding for Stage 2 review

The owner reports that the current in-progress Stage 2 graphics look rough, blocky and assembled from disconnected primitives. This is **not acceptable as the visual quality bar for Stage 2 review**, even if machine logic is correct.

### Machine construction and continuity
- Conveyors, chutes and machine bodies must form one visually coherent production line.
- Do not draw a conveyor as a floating rectangle laid over machines.
- Every transport segment must have believable start/end connections, supports, rollers, guards, funnels or transfer mouths where appropriate.
- Material handoffs must look physically plausible: the player should be able to visually trace where straw/needle leaves one machine and enters the next.
- Avoid visible overlaps, hard cuts, gaps and impossible intersections between machine geometry.
- Machines need thickness, frames, feet/supports, housings and contact shadows so they read as objects occupying the same space.
- Prefer a few well-composed connected machines over many crude disconnected blocks.

### Shape language
- Reduce the "rectangles with labels" look.
- Use rounded/channeled industrial forms, cylinders/rollers, rails, ramps, guards, brackets, springs and mechanical silhouettes.
- Corners may be softened/beveled visually; avoid harsh primitive clipping unless intentionally industrial.
- Use consistent scale, stroke/thickness, perspective cues and shadow direction across all machines.

### Conveyor quality bar
A conveyor should visibly contain:
- belt surface;
- side rails/frame;
- rollers or believable drive/end drums;
- supports/legs or attachment to surrounding machinery;
- a clear intake mouth;
- a clear discharge/handoff;
- moving texture/markers that communicate belt direction.

A plain filled rectangle is not an acceptable final Stage 2 conveyor representation.

### UI / HUD quality bar
- HUD must look deliberately designed rather than debug text placed around the scene.
- Use clear hierarchy, spacing, grouping, alignment and touch targets.
- Controls should be compact, consistent and visually integrated with the industrial scene.
- Debug telemetry must remain available but visually subordinate or collapsible.
- Portrait and landscape layouts each need intentional composition, not automated scaling of one into the other.
- Do not let labels cover machinery or moving material.

### Art/technical-art workflow
Before requesting owner review, perform a dedicated **visual polish pass** after machine logic is stable:
1. inspect the whole line at phone resolution;
2. identify disconnected/floating geometry and fix all handoffs;
3. improve machine silhouettes and depth cues;
4. unify materials/palette/shadows;
5. improve HUD hierarchy and controls;
6. verify portrait and landscape screenshots side-by-side;
7. only then ask for owner visual review.

This feedback does **not** ask for final production art yet. It requires a polished, coherent prototype that demonstrates the intended art direction rather than programmer primitives.
## Visual direction

Continue the Art Bible: stylized 2.5D miniature industrial diorama.

Even with procedural/placeholder art, machines must have distinct silhouettes and moving parts:
- vibration table: springs/feet/table motion;
- conveyor: belt + rollers;
- separator: magnet/drum/coil/arm;
- sorter: gate/chute/buffer;
- packer: jaws/clamps/wrapper/box motion.

Use soft shadows/contact depth and restrained industrial colors. Keep straw natural and needle understated before capture.

Avoid a screen made of flat labeled rectangles.

External art assets are optional. If any are introduced, they must be free/compatible, tracked with source/license, and replaceable.

## Feedback

Use restrained but satisfying feedback:
- moving machinery;
- indicator lights/state changes;
- subtle particles at handoffs where appropriate;
- haptic on packaged completion;
- optional lightweight procedural sound only if it is clearly beneficial and introduces no licensing burden.

Do not bury the machinery under excessive effects.

## Stage 2 counters / HUD

Allowed session counters:
- Batch
- Recovered
- Missed
- Packaged
- optional throughput/debug metrics

These are diagnostics/session production values, **not currencies**.

Counters and active line state survive background/resume and rotation while the process remains alive.

## Instrumentation

In debug builds record:
- batch seed;
- batch duration;
- intake item count;
- current item count by route/state;
- machine running/paused state;
- buffer occupancy;
- needle recovered/missed/packaged;
- approximate throughput items/s;
- manual assist touch count/drag distance;
- physics body count;
- FPS/frame-time/peak frame.

Print one concise batch summary when a batch reaches a terminal state.

## Performance

Target on Samsung SM-S938B:
- stable 60 FPS minimum during normal operation;
- no sustained allocation spikes in physics/transport/render loops;
- responsive touch on intake;
- machine animation remains smooth while material is flowing.

High-refresh 120 FPS is welcome but not required.

Prefer simplified transport and pooled visual effects to reducing material density so far that the line looks empty.

## Tests

Add pure deterministic tests for:
- seeded batch generation invariants;
- machine state transitions;
- route progression and terminal states;
- separator captures metal but not straw;
- separator-off missed-needle behavior if individual toggles are implemented;
- packer increments exactly once per needle;
- pause/resume/rotation model retains state;
- no duplicate/lost authoritative items;
- fixed-step/hybrid transport consistency across 30/60/120 render cadence where applicable.

Add an OpenGL/runtime smoke that exercises at least several complete batches, but do not use scripted smoke as a substitute for owner device review.

## Required owner playtest

On Samsung SM-S938B:
1. Run at least 10 complete batches.
2. Watch at least one batch mostly hands-off.
3. Use manual swipes on intake and judge whether assistance is useful and satisfying.
4. Confirm each machine's purpose is understandable without reading debug internals.
5. Pause/resume the whole line.
6. If per-machine controls exist, toggle each one and verify visible cause/effect.
7. Confirm needle extraction by the magnet is visually readable and satisfying.
8. Confirm the packer visibly completes and increments exactly once.
9. Rotate portrait ↔ landscape during an active batch and verify true responsive reflow with no reset.
10. Background/resume during an active batch and verify counters/state survive.
11. Observe FPS/jank/heating for several minutes.
12. Report the least convincing machine/transition and the most satisfying one.

## Explicit non-goals
Do not implement:
- prices or purchasing;
- upgrade levels;
- energy bills;
- maintenance/wear economy;
- research tree;
- contracts/missions;
- prestige;
- offline progress;
- persistent save across process death;
- ads/IAP;
- backend/accounts;
- multiple factory layouts;
- free-form machine placement;
- multiple material environments.

Those belong to later milestones.

## Deliverables
- responsive portrait + landscape factory screen;
- one complete automation chain;
- hybrid intake physics + deterministic transport;
- recognizable animated machines;
- manual assist on intake;
- machine/global run-state behavior;
- needle capture, routing, buffer and packing;
- session counters/instrumentation;
- deterministic tests;
- runtime smoke;
- updated architecture/readme notes as needed;
- `docs/STAGE_2_REPORT.md` with tunables, algorithms, tests, device evidence, known issues and owner checklist.

## Exit gate
Stage 2 is complete only when:
- builds/tests/lint pass;
- line completes batches deterministically without lost/duplicated items;
- owner performs the physical playtest;
- responsive landscape satisfies the carried-forward owner requirement;
- straw/needle presentation does not regress;
- lifecycle counters/state do not regress;
- the machine chain is judged understandable and satisfying enough to justify Stage 3 economy.

Then STOP. **Stage 3 remains locked.**

## Owner visual feedback — screenshot review, iteration 2

Physical-device screenshot review confirms that Stage 2 is functionally readable but visually below the required quality bar.

### Composition / line continuity
- The production line does not yet read as one connected machine system.
- The right-side conveyor reads as two rails/ladder-like bars rather than a believable belt conveyor.
- Intake, conveyor, magnet, buffer and packer feel placed near each other rather than mechanically connected.
- Diagonal transfer sections look like thick floating bars; replace them with believable chutes/ramps/rails with clear joints and entry/exit mouths.
- Increase visual continuity so the eye can trace material flow without relying on labels.

### Machine hierarchy / proportions
- The vibration table currently dominates most of the screen while downstream machines are too small and visually weak.
- Enlarge and strengthen the silhouettes of magnet, buffer and packer so they feel like real stages of the process.
- Reduce empty/unused black space by composing the whole factory as one intentional scene.
- Give every machine an obvious base, supports, housing and connection points.

### Conveyor
- The vertical conveyor must visibly have a belt surface, end drums/rollers, side rails and supports.
- It must physically connect to the vibration-table discharge and to the next machine.
- Material should sit on/inside the belt path, not appear to float through a rail cage.
- Direction/motion should be communicated by belt markers or roller movement.

### Magnet / buffer / packer
- The magnet currently looks like a small detached icon. Integrate it around/over the transport path so its capture action is spatially obvious.
- The buffer needs a recognizable funnel/chute/bin form, not a small outlined square with a label.
- The packer needs a recognizable mechanical body and a clear infeed/outfeed; current geometry reads as a box with primitives.
- Transfers must visually touch and align; no floating endpoints.

### Straw rendering
- Straw is improved relative to earlier wooden blocks, but it currently forms bundled rope/hair-like clumps because too many nearly parallel strands overlap.
- Keep straws thin and longer, but add more variation in curvature, angle, spacing, length and local separation.
- Avoid large coherent bundles that look like brushes or cables.
- Preserve dense coverage while retaining individual-straw readability.

### UI / HUD
- Current UI still reads as debug/programmer UI: raw labels, large rectangular buttons, and separate machine toggles without a unified visual system.
- Reduce floating text labels over the factory; use integrated nameplates/icons/state indicators where possible.
- Group top status values into a compact hierarchy.
- Bottom controls should share consistent spacing, shape language and state indication, with gameplay area receiving more visual priority.
- Debug FPS/body text should be visually subordinate or collapsible.

### Acceptance expectation
Before the next owner review, perform a dedicated visual pass on the actual phone-resolution layout. Functionality alone is not sufficient. The next screenshot should read as a coherent miniature industrial machine line even with all text hidden.

## Owner consolidated feedback — final polish before Stage 2 acceptance

This section consolidates the owner's latest physical-device review. These are the final required corrections before Stage 2 can be considered for acceptance.

### 1. Search difficulty / amount of straw
- The current batch is still too easy because the intake table has too little straw and too much empty space.
- Increase straw density substantially so the needle is not obvious and cannot be found almost immediately.
- Preserve individual-straw readability; do not solve density by drawing a single opaque bundle.
- Distribution should be irregular: denser pockets, overlaps, crossing angles and locally difficult areas rather than an even sparse spread.
- The search/reveal loop should require attention and several deliberate interactions, not one glance or one broad swipe.
- Keep the baseline challenging enough to be interesting, while avoiding impossible seeds.

### 2. Needle appearance
- Do NOT make the needle longer than the accepted Stage 1/current length.
- Make it thinner.
- Use a restrained realistic metallic color; remove the overly bright/glowing look.
- Keep a clear sharp tip.
- It may become more visible only during/after successful magnetic capture.

### 3. Straw appearance / motion
- Straw should be longer and thinner than the earlier wooden-block look.
- Keep natural variation in length, angle, spacing and curvature.
- Avoid parallel bundles that read as rope, hair, brush or cable.
- Add gentle large-radius bends / smooth curvature where practical. Avoid abrupt kinked angles.
- Preserve dense coverage while retaining separate-strand readability.

### 4. Factory visual quality
- The newer visual direction is significantly improved, but one more polish pass is required.
- The whole line should read as one coherent miniature industrial system even if labels are hidden.
- Conveyors, chutes, machine bodies and transfer points must visibly connect with believable joints, mouths, supports, rollers and frames.
- Avoid floating bars, disconnected endpoints, clipping and impossible overlaps.
- Strengthen buffer and packer silhouettes; make final-stage handoffs feel intentional and satisfying.
- Keep the magnet spatially integrated with the material path instead of reading as a detached icon.

### 5. Landscape composition
- The current landscape layout is too stretched horizontally.
- Bottom controls must NOT expand into long wide buttons merely because more width is available.
- Buttons/toggles in landscape should remain approximately the same visual size as in portrait.
- Recompose the landscape layout instead of stretching it.
- Preferred options: compact side control panel (left or right), or a compact 2-row control cluster placed in free space that does not cover the factory.
- The conveyor/factory itself should not become one excessively long straight line just to fill width.
- Prefer a more compact authored layout (for example L-shaped, stacked/two-level, or another coherent modular composition) while preserving readable material flow.
- Wide orientation should provide overview and usability, not horizontal elongation.

### 6. HUD / controls
- HUD has improved but should receive one final premium-game polish pass.
- Use deliberate hierarchy, spacing and grouping.
- Keep debug metrics subordinate/collapsible.
- Controls must use a consistent visual system and must not steal excessive screen area from gameplay.
- Portrait and landscape are two authored responsive compositions, not one layout stretched to fit.

### 7. Lifecycle / state
- Do not regress rotation/background-resume behavior.
- Active batch, machine state and counters must remain intact while the process is alive.

### 8. Review gate
Before asking for owner acceptance again:
1. apply the gameplay-density/difficulty changes;
2. apply needle/straw visual corrections;
3. perform a full factory visual polish pass;
4. recompose landscape controls/layout without stretching buttons;
5. verify portrait + landscape screenshots side by side on phone resolution;
6. run tests/build/lint and physical-device smoke;
7. stop for owner review.

Stage 3 remains locked.

## Owner UI bug — white system strip / window inset

Owner observed a bright white strip along the top edge in landscape orientation on the physical Android device.

Required fix before Stage 2 acceptance:
- Remove the white top strip in landscape.
- Verify portrait has no equivalent white strip at the top, bottom, or around cutout/system-bar insets.
- The app/background color must extend consistently through all visible window/inset areas so no light system/window background flashes or remains exposed.
- Respect safe areas/cutouts without introducing a white gutter.
- Check Android status/navigation bar appearance and edge-to-edge configuration in both orientations.
- Verify after rotation, background/resume, and cold launch on Samsung SM-S938B.
- Do not hide essential system navigation in a way that harms usability; the requirement is visual continuity, not necessarily forced immersive fullscreen.

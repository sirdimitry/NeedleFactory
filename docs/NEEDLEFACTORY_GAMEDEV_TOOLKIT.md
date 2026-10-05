# NeedleFactory GameDev Toolkit

A curated project-local decision layer for choosing agent skills and specialist roles without flooding Codex with every available game-development skill.

This document is the routing policy. External repositories remain reference sources; NeedleFactory owns the final rules, scope and architecture.

## Operating principle

Use the **smallest useful profile** for the current task. Do not load or imitate every source skill at once.

For substantial work, the agent must report the active profile(s) in one concise line, for example:

`Toolkit: VISUAL_ART + UI_UX + QA_VERIFY`

Routine use of an already approved profile does not require owner approval. Any new external service, paid API, runtime dependency, editor/plugin installation, build-system change, or skill outside this whitelist must be surfaced to the owner before adoption.

## Source repositories

### gamedev-skills/awesome-gamedev-agent-skills
Use cross-engine discipline skills, not unrelated engine-specific packs.

Approved now:
- `create-game-assets` — cohesive art direction and asset-production workflow.
- `physics-tuning` — fixed-step, mass/drag/CCD/stability/layers and feel tuning.
- `game-feel` — responsive feedback/juice without obscuring mechanics.
- `game-ui-ux` — HUD, safe areas, scaling and touch-oriented game UI.
- `performance-optimization` — profile-first frame/CPU/GPU/GC/pooling budgets.
- `input-systems` — only where touch/input architecture needs explicit review.
- `prototype-fast` — only for bounded vertical-slice work; never to bypass milestone gates.

Approved later when the feature exists:
- `save-systems`
- `procedural-gen`
- `audio-design`
- `shader-programming`

Do not auto-load engine skills for Godot/Unity/Unreal/etc.; NeedleFactory is Kotlin/libGDX.

### abagames/agentic-gamedev-skills

Approved now:
- `implementing-gameplay-invariants` — turn design promises into mechanically testable invariants.
- `directing-game-visuals` — hierarchy, palette roles, composition and visual contracts.
- `maximizing-game-feel` — tactile feedback and responsiveness.
- `evaluating-gameplay-balance` — telemetry-guided difficulty/balance tuning.
- `auditing-gameplay-implementation-coverage` — find missing behavior/spec/test cases.
- `localizing-game-state-divergence` — deterministic defect localization.
- `adversarially-validating-game-repairs` — stress-test fixes and regressions.

Adapt browser/Godot-specific examples to libGDX/Android instead of importing their platform assumptions.

### krutikJain/android-agent-skills

Approved now:
- `android-kotlin-core`
- `android-gradle-build-logic`
- `android-mobile-frontend-design`
- `android-emulator-automation`
- `android-performance-observability`
- `android-testing-unit`
- `android-testing-ui`

Conditional:
- `android-state-management` when Android shell/lifecycle state becomes complex.
- `android-security-best-practices` before release or when permissions/network/storage expand.
- `android-ci-cd-release-playstore` only in release-readiness stages.

Do not import Compose-specific architecture into the libGDX game surface unless a real Android-shell use case requires it.

### rehan-remade/universal-modder

Already adopted selectively. Use:
- asset-pipeline discipline;
- repeatable runtime/screenshot verification ideas;
- verification-oracle mindset;
- field notes with exact versions and `symptom -> cause -> fix`.

Do not use its reverse-engineering/mod-loader stack for normal NeedleFactory development.

### Codex Game Studios

Use only the **role/workflow model**, not the whole studio template.

Approved role lenses:
- Game Director / Systems Designer
- Senior Gameplay/Physics Engineer
- Technical Artist
- Game UI/UX Designer
- Mobile Performance Engineer
- QA/Test Lead
- Economy Designer when economy is in scope

The project milestone documents and `AGENTS.md` remain authoritative over any external role template.

## Project profiles

### GAMEPLAY_CORE
Use for rules, manual/automation progression, difficulty and player decisions.

Primary skills:
- implementing-gameplay-invariants
- evaluating-gameplay-balance
- physics-tuning when interaction physics matters

Required outputs:
- explicit player action -> system response loop;
- failure/edge cases;
- measurable difficulty/decision targets;
- no new system outside the current milestone.

### PHYSICS_TOUCH
Use for straw, needle, pushers, conveyors, transfer motion and tactile interaction.

Primary skills:
- physics-tuning
- maximizing-game-feel
- input-systems when input behavior is changing

Rules:
- fixed-step authority;
- tune before adding complexity;
- visual density may exceed authoritative physics density;
- preserve deterministic/reproducible seeds where practical.

### VISUAL_ART
Use for machines, straw, needle, asset families, materials, visual coherence and polish.

Primary skills:
- create-game-assets
- directing-game-visuals
- Universal Modder asset-pipeline/verification ideas
- Technical Artist role lens

Rules:
- establish a visual target before producing variants;
- prefer families of coherent assets over one-off drawings;
- validate at real phone resolution;
- reject programmer primitives, disconnected machine geometry and inconsistent perspective/material language;
- do not require paid generation services without owner approval.

### UI_UX
Use for HUD, controls, responsive portrait/landscape and accessibility.

Primary skills:
- game-ui-ux
- android-mobile-frontend-design
- Game UI/UX Designer role lens

Rules:
- touch targets remain intentionally sized across orientations;
- landscape is recomposed, not stretched;
- safe areas/insets/fullscreen must be verified on device;
- debug telemetry is secondary/collapsible;
- state is not communicated by color alone.

### GAME_FEEL
Use for rewarding search, capture, packing, motion and feedback.

Primary skills:
- maximizing-game-feel
- game-feel
- directing-game-visuals

Rules:
- feedback reinforces state change; it must not hide mechanical truth;
- haptics/particles/animation should have a reason;
- measure whether feedback improves readability and satisfaction.

### QA_VERIFY
Use for every milestone exit, meaningful bugfix and visual acceptance claim.

Primary skills:
- auditing-gameplay-implementation-coverage
- localizing-game-state-divergence
- adversarially-validating-game-repairs
- android-emulator-automation
- android-testing-unit
- android-testing-ui
- Universal Modder verification-oracle / field-note method

Required evidence where applicable:
- build/tests/lint;
- deterministic seed/repro steps;
- physical Samsung SM-S938B check;
- portrait + landscape evidence;
- rotation/background/resume;
- screenshot comparison for visual/layout claims;
- exact `symptom -> cause -> fix -> verification` in the current stage report.

### PERFORMANCE
Use when frame time, jank, memory, thermal behavior or build performance is in scope.

Primary skills:
- performance-optimization
- android-performance-observability
- Mobile Performance Engineer role lens

Rules:
- measure first;
- define representative-device budget;
- record before/after evidence;
- do not trade core visual density/gameplay away before identifying the actual bottleneck.

### ANDROID_PLATFORM
Use for lifecycle, fullscreen/insets, ADB/device automation, Gradle/toolchain, permissions and release plumbing.

Primary skills:
- android-kotlin-core
- android-gradle-build-logic
- android-emulator-automation
- android-testing-ui
- android-performance-observability

Rules:
- keep Android-specific code outside portable core where practical;
- do not introduce Compose/MVI/DI simply because a source skill recommends it;
- verify real-device behavior, not only emulator behavior.

### ECONOMY_PROGRESSION — locked until the milestone allows it
Use for costs, upgrades, unlock pacing, manual -> assisted -> automated progression and long-term balance.

Primary inputs:
- existing NeedleFactory economy docs;
- game-design/game-balance references;
- evaluating-gameplay-balance;
- Economy Designer role lens.

No economy work may leak into an earlier locked milestone.

### SAVE_RELEASE — later
Use when persistence/release work is explicitly authorized.

Primary skills:
- save-systems
- android-security-best-practices
- android-ci-cd-release-playstore

Save data must remain versioned and migration-aware.

## Accepted Stage 2 maintenance routing

Stage 2 is accepted/completed after owner Samsung testing (2026-10-05).
Closing verification uses QA_VERIFY + ANDROID_PLATFORM + GAMEPLAY_CORE.
Stage 3 remains unimplemented and needs separate owner authorization.

For any separately requested Stage 2 polish/regression review, default to:

`GAMEPLAY_CORE + PHYSICS_TOUCH + VISUAL_ART + UI_UX + QA_VERIFY + ANDROID_PLATFORM`

Add `PERFORMANCE` only when profiling or frame/thermal evidence is being evaluated.

Do not activate `ECONOMY_PROGRESSION` yet.
Owner-authorized exception (2026-10-05): `ECONOMY_PROGRESSION` may be used only
to author/review `GAMEPLAY_ECONOMY_PROPOSAL.md`. This is preparatory design, not
Stage 3 authorization; no economy/progression runtime is authorized by the note.

## Owner visibility / change-control rule

The owner asked to know when this toolkit is being used so it can be constrained or changed later.

Therefore:
1. At the start or end of substantial agent work, state the active profiles in one concise line.
2. If a task would benefit from a currently locked/conditional skill, mention it before using it.
3. Ask owner approval before:
   - adding a new external toolkit/source;
   - installing a plugin/tool into the development environment;
   - adding a paid/external API or service;
   - adding a new runtime dependency/framework;
   - changing the build toolchain or major project architecture;
   - enabling a profile currently marked locked.
4. The owner may disable or modify any profile at any time; project-local rules then override upstream guidance.
5. Record material routing changes in this document so future agents inherit them.

## Conflict rule

Order of authority:
1. explicit current owner instruction;
2. `AGENTS.md`;
3. current milestone document (`docs/STAGE_X.md`);
4. this toolkit routing document;
5. `docs/SKILLS.md`;
6. external skill/repository guidance.

If external advice conflicts with a higher layer, reject it and record the reason when consequential.

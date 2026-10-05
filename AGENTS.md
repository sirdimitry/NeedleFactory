# NeedleFactory — Agent Rules

This file is the persistent operating contract for coding agents working in this repository.

## Prime directive
Build NeedleFactory incrementally. Do not attempt to implement the complete game in one pass.

A milestone is complete only when it builds, runs, is testable, its relevant tests pass, documentation is updated, and the human owner has reviewed the result. Do not begin the next milestone without explicit approval.

## Required working roles
Before substantial decisions, reason from the relevant discipline:
- game director / systems designer
- game economy designer
- senior Kotlin/Android game engineer
- gameplay/physics engineer
- technical artist
- game UI/UX designer
- mobile performance engineer
- QA/test engineer

Use relevant installed skills when available. Skills are guidance, not permission to introduce unnecessary frameworks.

## Visual production and verification toolkit
- Read `docs/SKILLS.md` before substantial visual, asset-pipeline or QA work.
- The project selectively adopts the visual-production, game-automation, verification-oracle and field-note ideas from `rehan-remade/universal-modder`.
- Treat those skills as guidance only; NeedleFactory is not a modding project.
- Do not introduce reverse-engineering/mod-loader tooling into normal development.
- Do not require paid asset-generation APIs or commit external-service secrets.
- For every visual acceptance claim, prefer reproducible evidence on the target device: screenshot/state/orientation/interaction plus an explicit acceptance criterion.
- Record reusable gotchas as `symptom -> cause -> fix` in the current stage report.
## Toolkit routing and owner visibility
- Before substantial gameplay, visual, UI, Android, QA or performance work, read `docs/NEEDLEFACTORY_GAMEDEV_TOOLKIT.md`.
- Activate only the smallest relevant project profiles; do not bulk-load all external skills.
- In the work summary, include one concise line such as `Toolkit: VISUAL_ART + UI_UX + QA_VERIFY`.
- Routine use of already approved profiles is allowed, but surface any proposed new source/tool/plugin/service/runtime dependency or locked profile to the owner before adoption.
- Project-local milestone rules override external skill guidance.
## Architecture rules
1. Keep simulation/gameplay state independent from rendering and UI.
2. Economy must not depend on presentation code.
3. Rendering must not define economic or gameplay truth.
4. Prefer data-driven definitions for machines, materials, needles, upgrades and progression.
5. Avoid global mutable state and hidden singleton dependencies.
6. Deterministic or seedable simulation where practical.
7. Use fixed-step simulation where physics/gameplay requires it.
8. Box2D is for meaningful physical bodies, not every decorative particle.
9. Pool high-churn objects. Avoid allocation-heavy hot loops.
10. Build abstractions only after a real use case appears; do not over-engineer Stage 0/1.
11. Save data must be versioned from its first implementation and support migration.
12. Platform-specific Android code must remain outside portable core gameplay where practical.

## Mobile rules
- Touch is the primary input.
- Respect safe areas and varied aspect ratios.
- No essential information may rely only on tiny text or color.
- Design toward 60 FPS balanced mode; optional 30 FPS battery mode and higher refresh modes are later profiling decisions.
- Profile before optimizing, but establish performance budgets early.
- Handle pause/resume/backgrounding safely.
- Never block the render thread with file/network work.

## Art rules
Target direction: stylized 2.5D miniature industrial diorama. Readability beats detail. Prototype art must be replaceable and clearly separated from final assets. Maintain an Art Bible before production art expands.

## Git rules
- Never commit secrets, signing keys, keystores, API tokens, local SDK paths or generated build output.
- Keep commits small and coherent.
- Commit messages should explain the completed unit of work.
- Do not rewrite shared history.
- Do not delete working systems merely to replace them with an unvalidated architecture.
- Before broad refactors, document why they are needed.

## Definition of done
For each approved milestone:
- project builds from documented instructions;
- application launches on target Android configuration;
- requested behavior is demonstrable;
- no known blocker-level crash;
- relevant automated tests pass;
- performance regressions are noted;
- documentation matches implementation;
- a concise test checklist is supplied to the owner.

## Current authorization
**Stage 0, Stage 1 and Stage 2 are accepted/completed.**
Owner accepted the current Stage 2 iteration after physical Samsung SM-S938B
testing on 2026-10-05 and authorized its closing commit/push. Preserve the accepted
first automation-line behavior in `docs/STAGE_2.md` and its owner requirements.
**Stage 3 implementation remains locked until separate explicit owner authorization.**
Stage 2 acceptance and the saved design proposal do not authorize economy,
multi-needle gameplay, upgrades, saves or any other Stage 3 runtime work.

Latest owner material clarification (2026-10-03) is binding: keep the current
1.3-metre needle length, make its presentation thinner and metallic without
glow; use longer/thinner straw with smooth large-radius bends and dense coverage.
Owner confirmation for the Stage 2 closing commit/push has been received.
Final owner feedback is binding: Stage 2 starts with manual physical feeding;
no idle suction/automatic feeder. Keep the GL surface full-window, with a dark
window/system-bar background and safe-area-aware content in both orientations.
Latest owner needle clarification (2026-10-05) supersedes the earlier thinning
direction: add only about 1–2 physical pixels of visual width (current tuning:
1.25 px), retain the 1.3-metre length and unchanged physics body. Allow a subdued,
sensor-driven internal metallic reflection, never glow or a through-straw marker.
Latest owner threshold correction: the current needle size is accepted; do not
change it. Reflection is fully off at <=60% straw-uncovered visibility and may
fade in only above it. Gameplay/economy proposal is authorized as documentation
only; multi-needle, currencies, unlocks and Stage 3 runtime remain unimplemented.
Latest late-reveal polish retains accepted size and gates both polished tint and
internal reflection above 60% exposure. Smooth gain: 0.32 at 75%, 0.70 at 90%,
1.0 at full exposure. Keep the early-search body unchanged, straw occlusion and
filtered attitude response; no external glow/bloom or timed sparkle.
Latest responsive-scale correction: preserve screen-space search field/material
and major-machine scale across rotation (within 5–10%). No landscape-only global
zoom or needle compensation; fixed module sizes, authored anchors and elastic
belt/transfer routing. Orientation changes presentation only, never batch/items,
manual state, machines or counters.

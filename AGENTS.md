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
**Stage 0 and Stage 1 are accepted/completed. Stage 2 is authorized/current.**
Implement only the first automation-line scope in `docs/STAGE_2.md`, including
the carried-forward owner playtest requirements. Stage 3 economy/progression
remains locked until Stage 2 is physically playtested, explicitly accepted and
separately authorized by the owner.

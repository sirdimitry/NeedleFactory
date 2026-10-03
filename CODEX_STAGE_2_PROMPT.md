# Codex Task — NeedleFactory Stage 2

You are working in the `sirdimitry/NeedleFactory` repository.

## First action
Safely synchronize:

```powershell
git status --short --branch
git pull --ff-only origin main
```

If the working tree is not clean or fast-forward is impossible, stop and explain. Never discard owner/local work.

## Read before coding
Read:
1. `AGENTS.md`
2. `README.md`
3. `docs/GDD.md`
4. `docs/ARCHITECTURE.md`
5. `docs/ART_BIBLE.md`
6. `docs/ECONOMY.md`
7. `docs/ROADMAP.md`
8. `docs/STAGE_1.md`
9. `docs/STAGE_1_REPORT.md`
10. `docs/STAGE_2.md`

Treat `AGENTS.md` and `docs/STAGE_2.md` as hard scope boundaries.

## Authorization
The owner explicitly authorizes **Stage 2 only**. Stage 3 is locked.

## Owner feedback is binding
Do not treat the Stage 1 owner feedback as historical trivia. Carry these requirements into the actual Stage 2 implementation:
- genuine responsive landscape reflow, not a portrait column/letterbox;
- a thin, long, sharp, restrained metallic needle;
- dense, thin, long, natural-looking straw with minimal empty areas;
- background/resume and rotation retain active state and counters.

If existing Stage 1 implementation/docs conflict with those requirements, the owner feedback wins for Stage 2.

## Task
Implement the first automation line defined in `docs/STAGE_2.md`:

**dense intake/vibration table → conveyor → magnetic separator → sort/buffer → packer → packaged needle**, with residue routed separately.

Preserve manual touch as an assist on the intake table. The line must be understandable by watching it.

Use the existing Box2D/fixed-step foundation. Prefer hybrid simulation: physics where touch/material interaction benefits, deterministic lightweight transport elsewhere.

Do not add economy, purchases, upgrades, save persistence, ECS, DI, backend, analytics, ads or Stage 3 systems.

## Priority order
1. owner non-regression requirements;
2. readable machine cause/effect;
3. satisfying material flow and magnetic capture;
4. satisfying packer handoff/completion;
5. responsive portrait/landscape composition;
6. deterministic correctness;
7. performance;
8. extra polish.

## Before owner review
Run the relevant:
- JVM tests;
- desktop/runtime smoke;
- Android debug/release assembly as already established;
- lint;
- physical Samsung SM-S938B smoke when connected.

Verify several full batches, pause/resume, rotation, manual intake assist, machine controls, capture/routing, buffer/packer and counters.

Create `docs/STAGE_2_REPORT.md` with:
- architecture actually created;
- machine/state model;
- hybrid physics/transport strategy;
- responsive layout strategy;
- all important tunables;
- owner-feedback non-regression evidence;
- test/build/lint results;
- device evidence/performance;
- known limitations;
- concise owner playtest checklist.

## Stop point
When Stage 2 is ready for human playtesting, STOP.

Do **not** make the final Stage 2 acceptance commit/push unless the owner explicitly approves after physical playtesting.

Do not begin Stage 3.

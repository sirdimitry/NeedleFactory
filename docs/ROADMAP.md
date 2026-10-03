# Development Roadmap

## Stage 0 — Pre-production & foundation [ACCEPTED]
Deliverables:
- persistent agent rules;
- GDD, architecture, art direction and economy principles;
- validated free/open-source dependency plan;
- Android/libGDX project skeleton;
- build/run instructions;
- minimal CI/build verification where practical;
- performance/test strategy.

Exit gate passed after physical Samsung SM-S938B testing.

## Stage 1 — Tactile search vertical prototype [ACCEPTED / COMPLETED]
One tray/scene, clutter, one hidden needle, touch manipulation, exposure-gated discovery, satisfying feedback, prototype score and restart. Placeholder art is acceptable but should already feel coherent.

Exit gate: at least 10 human-playtested rounds on the physical phone, no impossible rounds, controllable touch physics, no blind tap-through discovery, stable performance, and owner judgment that the core search loop is worth continuing.

Exit gate passed: the owner completed and accepted the current iteration after
10 rounds on Samsung SM-S938B, checking slow/fast drag, hidden-needle blind-tap
rejection, Next pile, background/resume and rotation. Recorded physical-device
debug samples showed approximately 121 FPS. See `STAGE_1_REPORT.md` for evidence
and measurement limits. Stage 2 is not authorized by this acceptance.

## Stage 2 — First automation line [CURRENT / AUTHORIZED]
Build one continuous chain: dense intake/vibration table → conveyor → magnetic
separator → sort/buffer → packer, with manual intake assistance and residue
routing. Validate visible cause/effect, responsive portrait/landscape layout and
machine-to-machine handoffs. The Stage 1 owner feedback on landscape reflow,
thin/sharp needle, dense natural straw and lifecycle counter retention is a hard
non-regression requirement.

Exit gate: physical Samsung playtest, multiple successful batches, clear machine
purpose/cause-effect, satisfying magnetic capture/packing, stable performance,
true responsive rotation with retained state, and explicit owner acceptance.
Stage 3 stays locked until that gate passes.

## Stage 3 — Economy & progression
Costs, production metrics, meaningful upgrades, unlock pacing, save data and initial offline model.

## Stage 4 — Factory optimization
Placement/layout where appropriate, buffers, bottlenecks, machine interactions and production diagnostics.

## Stage 5 — Materials & environments
Add distinct material streams whose properties alter the best search/processing strategy.

## Stage 6 — Long-term depth
Research, specialization, contracts, rare finds and a carefully evaluated prestige/meta layer.

## Stage 7 — Production art & polish
Finalized art pipeline, animation, particles, audio, haptics, onboarding, accessibility and UX polish.

## Stage 8 — Optimization & release readiness
Device matrix, profiling, battery/thermal behavior, lifecycle recovery, save migration, crash handling and release build preparation.

## Gate rule
The coding agent must stop at the end of every stage and wait for explicit owner approval before proceeding.

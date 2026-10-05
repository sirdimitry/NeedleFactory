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
and measurement limits. This Stage 1 acceptance alone did not authorize Stage 2.

## Stage 2 — First automation line [ACCEPTED / COMPLETED]
Owner accepted the current iteration after physical Samsung SM-S938B testing on
2026-10-05 and authorized the closing commit/push. See `STAGE_2_REPORT.md` for
implementation, final verification, physical evidence and known limitations.
Build one continuous chain: dense intake/vibration table → conveyor → magnetic
separator → sort/buffer → packer, with manual physical intake feeding and residue
routing. Validate visible cause/effect, responsive portrait/landscape layout and
machine-to-machine handoffs. The Stage 1 owner feedback on landscape reflow,
thin/sharp needle, dense natural straw and lifecycle counter retention is a hard
non-regression requirement.
The starting loop is manual feed; assisted feed, partial automation and a fully
automatic feeder remain future progression rewards. Full-window dark Android
rendering without white strips is required before review.

Exit gate: physical Samsung playtest, multiple successful batches, clear machine
purpose/cause-effect, satisfying magnetic capture/packing, stable performance,
true responsive rotation with retained state, and explicit owner acceptance.
Exit gate passed by explicit owner acceptance. No Stage 3 implementation is
authorized by this acceptance.

## Stage 3 — Economy & progression [NEXT / NOT AUTHORIZED]
Costs, production metrics, meaningful upgrades, unlock pacing, save data and initial offline model.
`GAMEPLAY_ECONOMY_PROPOSAL.md` is preserved for a separate design decision; it is
not implemented. Start coding only after explicit owner authorization.

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

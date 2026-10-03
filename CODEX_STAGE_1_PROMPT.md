# Codex Task — NeedleFactory Stage 1

You are working in the `sirdimitry/NeedleFactory` repository.

## First action
Synchronize safely with the remote repository before editing:

```powershell
git status --short --branch
git pull --ff-only origin main
```

If the working tree is not clean or a fast-forward pull is not possible, stop and explain instead of discarding local work.

## Read before coding
Read:
1. `AGENTS.md`
2. `README.md`
3. `docs/GDD.md`
4. `docs/ARCHITECTURE.md`
5. `docs/TECH_STACK.md`
6. `docs/SKILLS.md`
7. `docs/ART_BIBLE.md`
8. `docs/ECONOMY.md`
9. `docs/ROADMAP.md`
10. `docs/STAGE_0_REPORT.md`
11. `docs/STAGE_1.md`

Treat `AGENTS.md` and `docs/STAGE_1.md` as hard scope boundaries.

## Authorization
The owner explicitly authorizes **Stage 1 only**. Stage 2 is locked.

## Task
Implement the tactile-search vertical prototype exactly enough to test whether the core act of searching for and finding a needle is fun on a phone.

The finished Stage 1 loop is:

**fresh pile → finger physically clears material → needle becomes exposed → tap exposed needle → satisfying feedback → Found counter + Next pile → fresh seeded pile**

Use the current architecture. Add Box2D now because Stage 1 has a real physics use case. Do not introduce ECS, DI, Compose, backend, analytics, shop/economy or factory systems.

## Important implementation constraints
- top-down zero-gravity Box2D;
- fixed 1/60 s physics step with bounded catch-up;
- if rendering above 60 Hz, interpolate physics transforms rather than coupling simulation to render FPS;
- roughly 50–70 prototype clutter pieces, centrally tunable;
- touch is a bounded physical pusher/rake, not arbitrary telekinesis;
- clutter can use simplified collision rules; do not make decorative bulk unnecessarily expensive;
- one needle per round;
- seeded deterministic round generation;
- initial needle must be meaningfully visually occluded;
- discovery requires sufficient exposure plus a hit on the needle;
- do not allow blind tap-through discovery;
- success feedback includes visual accent and Android haptic via a portable platform abstraction;
- session `Found` counter is not a currency;
- `Next pile` creates a clean new round with a new seed;
- debug instrumentation records seed, time-to-find, touches, drag distance, body count and FPS/frame-time;
- primary test orientation portrait; landscape may be fitted but must remain safe;
- pause/resume must preserve the current in-memory round.

## Design priority
Optimize in this order:
1. finger feel;
2. readable conceal/reveal;
3. satisfying find;
4. repeatability;
5. performance;
6. visual polish.

Do not expand scope to compensate for weak feel. Tune the core interaction instead.

## Verification before asking for owner playtest
Run relevant tests/build/lint and the desktop prototype where practical. Install/run on the already connected Samsung SM-S938B if available.

Verify at minimum:
- launch;
- controlled slow drag;
- fast sweep;
- no obvious tunneling/explosive bodies;
- covered needle cannot be blind-tapped;
- exposed needle can be found;
- Next pile works repeatedly;
- pause/resume;
- rotation safety;
- debug metrics/log summary;
- stable performance.

Create `docs/STAGE_1_REPORT.md` and include:
- exact Box2D/KTX dependency choice;
- physics/touch tunables;
- round generation and exposure algorithm;
- architecture changes;
- automated test results;
- device result;
- measured/observed performance;
- known issues;
- a concise 10-round owner playtest checklist.

## Stop point
Do **not** commit/push final Stage 1 acceptance yet unless the owner explicitly asks after playtesting.

When the implementation is ready for human testing, stop and tell the owner exactly how to run it on the phone and what to evaluate.

Do not begin Stage 2.

# Stage 3 — Economy & Progression

Publication2026-10-14 authorized by owner: current playable candidate is published
as v0.3.0-stage3-playtest.1 GitHub prerelease, debug APK. This is a playtest snapshot,
not Stage3 acceptance/completion. Historical no-publication notes below describe
earlier bounded passes and are superseded for this snapshot only.

Owner authorization 2026-10-11: original cartoon work glove pinches/carries/releases;
per-instance icon upgrades with no shop sale buttons; fan double-tap saved
anchoring/feet, rotate only anchored; paid lower-right collector invisible until
owned, higher steel prices/all3 bonus; pit width +1m purchased levels; metal loss
130ms haptic/red perimeter pulse; single shop icon; caged floor fan/real air cone;
fully exposed steel cartoon glint; explicit i +100-credit playtest cheat. See
STAGE_3_REPORT.md for tuning, migration and verification. Stage3 not accepted;
no commit/push/tag/release. Existing conveyors/processing stay reference-only.

## Owner field automation / live workshop pass — 2026-10-10

CURRENT / AUTHORIZED, not accepted. Latest owner authorization replaces the
deferred automation gate: one paid robot hand, independently owned fans and
rotatable physical dividers; conveyors and processing machines deferred.
No external dependency, offline income, Stage4 editor, commit/push/tag/release.
Toolkit: GAMEPLAY_CORE + ECONOMY_PROGRESSION + PHYSICS_TOUCH + GAME_FEEL +
UI_UX + QA_VERIFY + ANDROID_PLATFORM + VISUAL_ART + PERFORMANCE.

Symptom: upgrades waited for another lot / paid material could be repriced ->
cause: material manifest snapshots -> fix: value/luck update immediately for
unpaid objects; paid identities retain their exact historical payout. Quantity
adds new straw identities to ACTIVE batch, preserves existing poses/routes,
renumbers independent steel while preserving its gameplay state, then rebuilds
only the native adapter. Closed/draining batch is never reopened; purchased
quantity is available in the next lot. Verification: live repricing, growth,
conservation, save and no-double-settlement regression tests.

Symptom: shop was selectors/pages and purchases had no confirmation -> cause:
fixed three-card composition -> fix: one independently expandable vertical
document, clipped scrolling, bold section hierarchy, persistent wallet; each
paid transaction requires explicit Да / Нет. No cancels debit and failed
transactions keep an explanatory panel. Owned tool selection is free and does
not need a purchase dialog. Simulation pauses behind workshop/confirmation.

Symptom: automation had only a locked placeholder -> fix: explicit bought
world objects with IDs, positions, degrees, speed/width/range and invested cost.
Content save7, schema3; old content6 loads with empty equipment, unchanged
wallet/receipts/tools. Positions are authoritative world coordinates independent
of orientation. Local Box2D barriers and paid airflow are separate from rendering.
One robot travels to a real clump and carries it to the pit; steel can accompany
contacted straw, is still missable, and never sorted for free. Robot does not
create output by remote reward calls. Fans only apply force inside their cone;
heavy steel responds less. Removing equipment removes force/joints/colliders.
No purchased automation = prior manual-only/no-idle-feed baseline.

Tuning: robot200; speed upgrades300/450/675/1012 then2025 (after level5,
doubling). Fan500; each axis upgrades750/1000/1250/1500 then3000, doubling
after level5. Fan width18/26/34/42/50/58/66/74/82/90 degrees (ten versions).
Range3m, +0.45m per bought range tier; force28 +3 per speed tier. Physical
force and robot transit speed have safety ceilings; high-level/endgame tuning
is not balanced yet. One robot only; multiple fans/dividers, each independently
saved. Divider40 (initial tuning), length1.3m, central pivot, quantized1°.
Paid field object sale returns50% invested credits; original starter pile/pit
are permanent, not purchased objects. Bought needle receiver can also be sold.
Previously recovered steel remains payable even if its receiver is sold.

Interaction: tap selects, short drag moves selected object. Second press held
300ms without dragging enters local pivot rotation; direct angular response,
1° quantization, centre dead zone avoids angle jumps. Release saves. Rotation
ring/degrees appear only on selected object; sale and hide buttons remain local.
No free grid, conveyor routing or machine placement framework.

Field: pit world extends12.4 ×22.2m (y=-12.7..9.5) and keeps24.8 canvas units/m.
Portrait bottom console shortened; yard fills more of available floor width and
height. Landscape uses invertible rigid rotation; no individual needle scaling.
Small perimeter and existing Samsung Edge touch inset stay intact.

Verification: 149 JVM tests; debug/release/lint and native GL pass; Samsung
confirmed purchases, live material changes, fixture rotation, robotic carry,
fan influence/resale, save/restart and lifecycle. See STAGE_3_REPORT.md for
measured evidence, funded private QA scope and remaining owner decisions.
Known limits: no offline/rare/processing line; current physical lot is not
persisted through true process death; robot has simple direct routing and may
need a divider moved if blocked; many-fan endgame requires later profiling;
1000 saveable numeric tiers, 360 live clumps and safe motion caps are prototype
technical limits, not a claim of infinite simulated endgame.


2026-10-10 owner clump progression correction is CURRENT, NOT ACCEPTED.
Products-v6: each accepted/ground straw clump pays1 immediately; complete
straw delivery retains6 extra. First three-tooth loosener50,2 local grips,
1.2x hand spring response; paid grip/speed levels, ordinary rake with five
extra grips/speed steps, material value/count/luck and screw receiver levels.
Accordion workshop can be opened from the persistent Upgrades control while
working, paused, draining or viewing results. Shop freezes simulation.
Tool/receiver purchases apply now, material changes start next lot.
Schema3/content6: paid clump identities/amounts, level map and lot snapshot.
Existing balances, tools, unknown owned keys and historical receipts remain.
Automation menu is a future paid-line placeholder, no feeder or construction
editor implemented. See report for validation and prototype limits.

Historical 2026-10-10 bounded economy experiment: products-v5 pays raw needles6,
base loose volume6 plus6 for delivering the complete straw volume. Receiver18,
hand tools150/450, manual physics and pit remain. Historical receipts/wallet
are not repriced. Schema3/content5; future content6 stays read-only. See
STRAW_ECONOMY_CONVEYOR_PROPOSAL.md. No construction/editor/new machines in this
pass; verify/install and stop for Samsung review, not Stage3 acceptance.

Latest owner drawing (2026-10-08): starter auger pit occupies the far lower-left
corner of the portrait floor, not the edge of the search table. Real manual
carry extends across the yard. Landscape rotates the yard at the same object
scale. Pit is deep and dangerous, with sparse idle sparks, stronger straw
impact and a pronounced steel impact. Steel dropped here is missed and earns
zero; buy the nearby independent receiver to sell manually delivered needles.
Conveyor tiles, splitter, branches/spill handling and machine construction
remain later bounded implementation passes, not complete in this candidate.


Status: **CURRENT / AUTHORIZED — archive-inspired manual tools/economy candidate, NOT ACCEPTED**.
Owner approved the [original first-slice design](STAGE_3_DESIGN_V1.md); accepted regression baseline
is `v0.2-stage2`. Implementation is incremental, with a mandatory owner stop.

Toolkit: GAMEPLAY_CORE + ECONOMY_PROGRESSION + PHYSICS_TOUCH + GAME_FEEL + UI_UX +
QA_VERIFY + ANDROID_PLATFORM. No new dependencies or external services.

## Current bounded floor-pit progression pass — 2026-10-08

Latest owner steering supersedes the long right-edge trough with a distant small
square floor pit, upper-left source bin and approximately 4x visual hay density.
Starter pays only collected straw (6/full volume), steel in pit pays zero. One
paid needle receiver (18) mounts alongside, raw manually delivered steel pays
24 each. Rake150/Steel450 are retained hand tools; shop omits downstream machines
for this starter. Old keys/paid receipts remain in saves, historical line stays
reference-only. No new conveyor editor, splitter, magnet, Build Mode or offline.
Current economy model products-v4, versioned schema3/content4, no physical-straw
persistence. Russian UI, exposure thresholds, stable field scale and edge margins
remain. Stop after tests/device install for owner review, no commit/push.


## Current receiver / Russian UI pass — 2026-10-08

Owner requested a visible rotating spiral auger in the bunker opposite the
left straw pile. The existing right-hand manual intake is now an open screw
trough with shaft, front/back helical flights, bearings, drive and outlet collar.
Only live, local hand delivery can admit material. Idle rotation grants no
collection, money, metal capture or downstream ownership. Stop/pausing freezes
the screw; rotation/Home retains the same session. Russian bitmap typography
covers HUD, machine labels, tools, results, workshop, settings and reset.
Future purchased conveyor/machine construction remains a design direction;
preset paid equipment already owned is retained. No new upgrades/build editor.
Toolkit: VISUAL_ART + UI_UX + GAMEPLAY_CORE + QA_VERIFY + ANDROID_PLATFORM.

## Latest owner-authorized paid transport pass — 2026-10-06

Superseded trial tuning, not deleted behavior: on2026-10-08 owner supplied a
separate source prototype and requested adaptation with our graphics, machine
construction next version. See [bounded adaptation](ARCHIVE_REFERENCE_ADAPTATION.md):
selected hand1/rake2/steel3 local grips, rake150/steel450, loose24/bundled64,
raw24/sorted32; old purchased equipment remains and old paid receipts keep value.
Schema3 preserves progression and adds selected tool. Small late-reveal star
above75%, no early glow/reveal/tap recovery. Other numbers below record the prior
trial. No new machinery/build mode/rare/offline system in this adaptation.

Owner follow-up2026-10-08: `i` -> Settings -> Start over -> explicit Reset progress
confirmation starts from zero wallet/purchases, Hand and the manual receiver.
Cancel preserves progress; confirmed new game updates both primary and fallback
save asynchronously and drops the old lot/receipt. Minimum24dp side content
margins avoid Samsung Edge Panel while GL/background still cover the whole window.
The field/manual material-to-receiver loop remains central; construction is deferred.

Owner requested conveyor then magnet after accepting clumps/physical steel.
Add Short Conveyor **48** (requires Baler) and Magnetic Separator **72** (requires
conveyor). Neither is free. Hand-feed boundary x=7.4 replaces x=10 only when
owned; untouched intake never enters by idle motion. Belt carries accepted straw
and steel through the same physical discharge. Owned/on magnet extracts steel,
otherwise steel can be missed. Sorted metal is 4 credits, raw manual metal 3;
full loose/bundled lot remains 6/16. Final product value is paid once.
Schema-2 purchases/optional receipt.sorted preserve existing saves and receipts.
No buffer/packer, feeder, rare/offline, paid tiers or Build Mode in this pass.
Older descriptions below describe the accepted corrective baseline; only the
explicit conveyor/magnet deferral is superseded by this owner authorization.

## This pass

- Starter search table, manual straw receiver and raw-metal tray. No free
  conveyor, magnet, buffer, packer or Baler. Paid preset conveyor/magnet are now
  authorized after the Baler purchase. Keep the accepted Stage 2 line as a
  separate reference; do not delete its simulation/art/tests.
- Exactly 3 seeded needles. Expose then grab an uncovered shaft point and carry
  it to the lower manual metal slot at gameplay x=2.6 m. Acquisition requires
  current/previous exposure >60%; tapping never secures it. Local straw contact
  can entrain buried metal, with a weaker spring and bounded slip. Accepted metal
  follows the same receiver/gravity transfer as straw and is missed without a
  separator. No free conveyor or magnet. In this pass purchase the nearer hand-loaded
  lane and then its discharge separator. Raw metal is 3, sorted is 4 credits.
- Candidate: 24 small clumps from ten mirrored 3–5-straw templates, acute angles
  and staggered crossings instead of a pinched radial centre; a local
  spring grab selects one visible clump. Real edge crossing accepts only the
  held clump/local metal cargo. Release clears permission; no idle collection. Old products-v2
  120-patch push and Stage 2 reference presets remain for regression.
- Authoritative receiver -> loose, or owned Baler queue -> chamber -> bundled
  routes. Pending product has no sale value. Finish drains accepted work only.
- Credits: 3 per raw needle + 4 per sorted needle +
  floor((6*looseUnits+16*bundledUnits)/totalClumps). Final grade, never 3+4.
  No perfect cash bonus, time multiplier or reward for untouched material.
- Finish after the first saleable credit, including straw-only work; compact
  preview/confirmation, one settlement and discarded intake. Normal completion
  opens results automatically; Perfect remains an indication of full recovery.
- Workshop: Feed rake 28, Feed guides 16, Straw baler 42. Guides and Baler require
  rake; guides are optional. Hand tools never automate feeding. Baler appears
  at a preset mounting slot and binds accepted straw, visibly raising its grade.
- Short conveyor: 48 credits, requires Baler. Magnetic separator: 72, requires
  conveyor. Schema-2 ownership keys and optional sorted receipt field persist.
  Belt accepts a hand-loaded clump at gameplay x=7.4 instead of x=10, then
  carries it down its bolted lane to the processing transfer. No remote suction.
  Magnet captures on actual belt discharge; off/unowned means missed steel.
- Rake ownership is preserved, but the experimental grab stays at one clump;
  paid grip/receiver levels remain proposals, not implemented upgrades.
- Versioned schema-2 private progression save: credits, purchases, progression and last
  receipt/active identity; worker IO, backup/fallback, no per-straw pose save.
- Preserve needle art/visibility/tilt, dense straw, fixed module scale,
  full-window Android surface and live rotation/background state.
- Authored machine ports/footprints/hardpoints prepare future layout data; no
  free placement or build mode. Save schema is extensible/versioned.

## Deferred

Owner authorized incremental product-value steps up through paid transport/separation in
[Manual-to-Factory Progression v2](STAGE_3_DESIGN.md). Existing credits, paid
receipts and rake/guides survive migration; the paid historical receipt keeps its
original amount. An unpaid lot may reset after process death, never after rotation.
No sale occurs before final settlement, so restarting cannot replay paid products.

Purchased buffer/packer; contact vibration assist; automatic feeder;
rare content/currency/collection; contracts; offline income; Stage 4 build mode.
The design note remains the source for later work, not permission to expand now.

## Exit checklist

Existing + new JVM tests, debug/release/desktop builds, lint and real GL smoke;
Samsung portrait/landscape, manual feed, multi-needle, early/normal settlement,
purchases, restart, rotation/Home and frame-time evidence. Record results and
limitations in [Stage 3 report](STAGE_3_REPORT.md), then stop for owner review.
No final commit/push, release/tag or completed status without owner approval.

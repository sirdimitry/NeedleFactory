# Technical Architecture

## Stage3 archive adaptation — 2026-10-08

`EconomyConfig.workshop()` selects normalized products-v3 tuning separately from
historical products-v2/reference prices. `HandTool` selection and capacity are
domain/save data. Native primary/additional MouseJoint anchors retain independent
offsets, live admission and carried-metal source IDs; removing one grip does not
remove unrelated cargo. Additional grip selection allocates only at touch-down,
not each frame. All connections are cleared on release/resize/pause.
Schema/content3 adds selectedTool with explicit0/1/2 migration; preserves old
wallet/ownership/paid receipts and refuses newer saves. UI never defines income.
Archive Compose/runtime/signing files are not incorporated. Only the visual
late-star motif is adapted with rendered straw occlusion and filtered lighting;
no new dependency, worker, free construction or machine pipeline rewrite.

## Stage 3 physical metal correction — 2026-10-06

The grab preset alone enables `physicalNeedles`. Tap-to-recover remains only in
historical products/reference behavior. Exposed-shaft acquisition creates a
weaker native grip; bottom sorting-port entry starts CHUTE -> RECOVERED. A
bounded contact spring can entrain a needle intersecting held straw, and slips
at a .85 m gap. Release destroys all contact joints and feed permission. No
global metal attraction. Needles are processed before clumps at the real mouth
to preserve local cargo entry. Loaded clumps remain manually controlled until
their centre reaches the lip, avoiding a short needle being orphaned by early
clump acceptance.

Accepted material uses RECEIVING -> TRANSFER; straw goes to LOOSE or owned
Baler, unseparated metal to MISSED. TRANSFER is the starter receiver's gravity
handoff, not the powered BELT route or a free conveyor purchase. Simulation ports
remain in metres; authored visual chutes connect them across orientation. No
new save fields, economy prices, automatic separator, conveyor shop or layout
editor. Historical presets keep their previous transport behavior.

## Goal
Create a foundation that can grow from a tactile physics prototype into a factory simulation without coupling rendering, economy, persistence and simulation.

## Stage 3 product-value sub-pass rationale

Owner authorized an additive manual-products preset: the free reference line
cannot express purchased processing value. Keep its simulation/rendering and
tests intact. Add receiver/baler routes and capability ownership to the same
portable session; product quantities derive from unique item routes. New UI
selects the preset, never defines sale grade or quantity. Actual fixed-step
baler cycles create bundles; only terminal collected/processed products are
quoted/settled. Schema v2 retains v1 wallet/tools/receipts without repricing.
No free construction system, new dependency or complete factory rewrite.

## Single-clump input rationale — Stage 3, 2026-10-06

`grabClumps` is an additive preset on the existing session/native adapter, not
a factory rewrite. `StrawClumps` owns ten seedable small geometry families;
`StrawGrab` samples actual curved stalks for acquisition. A native mouse joint
holds at most one ID; only this ID receives feed permission at the real boundary.
Overlapping pile stems are layers, not a 2D rigid knot: the held clump skims
above them with bounded local soft deflection. Strand-shaped fixtures represent
geometry; existing reversible tray bounds retain cargo. Release/resize/pause
destroy the transient joint while authoritative item poses/routes remain.
No new runtime dependency, paid tiers or building system. Product value remains
normalized to the complete lot, independent of decorative or clump count.

## Candidate stack
Kotlin + libGDX, KTX selectively, Box2D selectively, Gradle, Android Studio.

Stage 0 must verify current stable versions, Android toolchain compatibility and licenses before pinning dependencies.

## Implemented Stage 0 foundation

Physical modules: `core` (Kotlin/JVM library), `android` (Android application),
`lwjgl3` (desktop application). Both launchers depend on `core`; `core` has no
platform-backend imports. Android owns SDK/lifecycle/insets integration and
supplies immutable `BuildInfo`. `game/NeedleFactoryGame` owns a single
`presentation/DiagnosticScreen`; its `debug/BootStatus` is pure Kotlin event
state, tested without libGDX initialization. There is no gameplay state yet.

Remaining logical packages are reserved by `package.md` files, not speculative
classes or empty Gradle modules. Shared prototype assets live in `assets/`.
No fixed-step simulation, physics, economy or save implementation is introduced.
See `STAGE_0_REPORT.md` for toolchain selection and verification evidence.

## Implemented Stage 1 prototype

The existing three modules are retained. `game/NeedleFactoryGame` now opens
`presentation/SearchScreen`; the Stage 0 diagnostic remains available through
the desktop launcher and its smoke harness. Disposal hides the current screen
before freeing its native resources.

- `content/SearchConfig`: one centralized set of prototype feel tunables.
- `simulation/SearchRound`: domain-owned poses, seeded generator, visual
  exposure/hit rules and `SearchSession` transitions. No rendering imports.
- `simulation/FixedStep`: bounded 60 Hz clock with interpolation alpha.
- `physics/SearchPhysics`: native Box2D ownership, collision filters, bounded
  physical pusher and pose snapshots. Geometry stays in the tray's visual
  envelope; it does not assign rewards or define discovery.
- `presentation/SearchScreen`: single-touch intent, fitted camera, interpolated
  placeholder rendering, success accent, HUD and debug metrics.
- `game/PlatformFeedback`: one portable find-confirmation callback; Android
  uses View haptic feedback on the main thread; desktop is a no-op.

Session count, exposure and round lifecycle remain outside Box2D. No economy,
factory or persistence code was added. Assets are still asynchronously loaded.
See `STAGE_1_REPORT.md` for tunables, runtime evidence and pending acceptance.

## Planned logical boundaries
### Stage 3 first playable slice

`Workshop` is portable credit/receipt/purchase truth, using frozen batch seed/ID
and terminal item states. `EconomyConfig`/`UpgradeId` own prices/thresholds.
`WorkshopCodec`/`WorkshopStore` implement explicit versioned JSON and backup
recovery; `WorkshopPersistence` serializes immutable file snapshots on a worker.
`WorkshopRuntime` is retained with FactorySession across Activity recreation.
Settlement commits wallet+receipt, purchase commits debit+ownership, and UI waits
for durable write completion before another economic action. Process death can
regenerate an unfinished physical batch; unpaid active work is outside this first
save slice, while settled wallet/purchases/receipt persist. No offline accrual.

The factory launcher selects exactly three needles; one-needle config/harness is
retained for Stage 2 regression. Capture FIFO and single-owner downstream slots
hold upstream material while busy. Rake uses a wider physical touch proxy; guides
are two static mouth cheeks. Neither supplies idle feed force. `FactoryMount`
adds authored footprints/ports/hardpoints in canvas world units; rendering anchors
and orientation never define wallet or simulation routes. No Stage 4 build mode.

### Stage 2 implementation decision
The new automation loop needs persistent machine/item truth and two camera
compositions. Rather than refactor away the accepted search prototype, retain
`SearchScreen` as a desktop reference (`-Pscreen=search`) and make `FactoryScreen`
the default. The three modules, toolchain, fixed-step clock and platform callback
remain in place. No ECS, DI, economy or save system is introduced.

- `FactoryConfig`: centralized geometry, feed and processing tunables.
- `FactorySession` / `FactoryBatch`: pure Kotlin seeded items, machine toggles,
  route transitions and exactly-once recovered/missed/packaged counters.
- `FactoryIntakePhysics`: Box2D only on the intake; one body per twelve-strand
  tuft, three smooth-tangent collision sections, bounded physical finger and
  actual feed-edge handoff. Tufts overlap each other; simplified bulk contact
  is intentional. Downstream items use deterministic normalized route progress.
- `FactoryLayout`: authored landscape flow and portrait stacked composition.
- `FactoryScreen`: interpolated geometry, machinery, touch intent and haptic
  notification. Rendering never feeds an item or produces a package.
- `NeedleLighting` / Android `NeedleTiltSensor`: optional filtered device attitude
  affects only internal metal shading. `NeedleVisibility` estimates exposed axis
  length against the same curved/projected straw poses as rendering; <=60% disables
  reflection completely. This presentation heuristic is not discovery/reward truth.

The dedicated visual pass splits original procedural art into `FactoryDiorama`,
shared `FactoryDrawing`/palette and `FactoryHud`. This makes projection, supports,
shadows, material palette and instrument typography consistent without adding a
runtime framework. `FactoryLayout` attachment points define both visible
connections and interpolated material endpoints. The first 12% of BELT animation
traverses the receiving pan, the middle portion runs on the framed belt and the
last 10% follows an attached discharge chute. The capture zone still uses the
same authoritative 55% route crossing. These mappings do not change production
logic, timings, item identity, physics, needle length or session lifecycle.

The visual smoke renders screenshots to real GL framebuffers at 1080×2160 and
2160×1080; desktop OS window-size limits cannot silently turn portrait into a
square screenshot. Those buffers are verification-only, never production work.

`NeedleFactoryGame` owns the session independently of screen/native resources.
Android retains that portable object across configuration recreation through
Activity non-configuration retention. Normal pause/resume and resize merely
cancel the current finger gesture and discard the clock remainder. Native
rehydration restores item poses/velocities; it creates bodies only for items
still on the intake. Counters, routes, progress and enabled machines remain
unchanged. There is no process-death persistence and no background/offline work.

The final owner correction removes directional intake forces. A downstream
finger contact arms the contacted item; only that item physically reaching the
gate can hand off. Manual eligibility, item velocity and the metering remainder
live in the portable session, so native rehydration does not turn untouched
material into automatic production. Without input all intake poses settle/hold.
No feeder unlock, economy or automation selection is implemented at Stage 2.

Android's GL surface fills the window without root padding. A dark application
theme and transparent system bars cover launch/transition frames. `SafeArea`
passes pixel obstructions onto the GL thread; `FactoryScreen` keeps a full-window
viewport, offsets only content coordinates and clears the entire surface. Insets
are refreshed on configuration/resume; touch unprojection shares that camera.

Responsive module scale is now explicit in `FactoryCanvas`, independent of
orientation selection. Its device baseline is max(400 / raw short pixel axis,
814 / (raw long pixel axis minus the larger obstructed-axis total)): compact displays reserve room for the fixed module
bay in both orientations, without a landscape-only fit/zoom. Samsung uses
400 / 1080 units per pixel in both orientations. `FactoryLayout` positions the
same 248 x 198.4 tray and unit-1 machine housings with authored anchors; belt length
and transfer directions change instead of material/machine size. Insets still
offset safe content on a full-window Surface. Fixed-size modules, shared handoff
ports and inverse touch transforms never change simulation dimensions or state.

The packer commits its result after its visible jaw cycle and package ejection.
Turning the separator off affects needles that have not yet crossed its capture
zone; an already latched capture remains committed. Belt-off stops feeding and
transport; an already loaded packer may cycle, then holds ejection until restart.

- **platform/android** — launcher, Android lifecycle, platform services, haptics.
- **core/game** — game orchestration and state.
- **simulation** — fixed-step simulation and world rules.
- **physics** — collision/physical interaction adapter.
- **factory** — machines, transport, processing and bottlenecks.
- **economy** — currencies, prices, rewards, upgrades, progression.
- **content** — data definitions for materials, needles, machines and levels.
- **presentation** — rendering, animation, particles, camera and audio triggers.
- **ui** — HUD, shop, upgrades, settings and onboarding.
- **save** — versioned persistence and migrations.
- **debug** — diagnostics, overlays, cheats/dev controls.
- **tests** — deterministic unit/integration tests.

These are logical boundaries first. Do not create a Gradle module for every bullet until module separation has a demonstrated build or ownership benefit.

## Simulation

Stage3 explicit new-game action is separate from lifecycle restore. Workshop
replaces wallet/ownership/receipt with a fresh seeded manifest; FactorySession
clears switches/counters/paid capabilities, then the native adapter is rebuilt.
WorkshopStore writes a durable new primary and mirrors it into the fallback
backup on its existing serialized worker. UI requires a second affirmative tap
and disables reset while loading/saving/error/read-only. Ordinary rotation and
resume never invoke this operation. Android side-touch safety unions24dp with
system/cutout insets; it does not resize the full-window SurfaceView.

Use a fixed timestep for systems whose behavior must be stable across frame rates. Rendering may interpolate between simulation states.

Physics tiers:
1. Full physics: needle and important interactable/production objects.
2. Simplified simulation: bulk material where full rigid-body simulation is wasteful.
3. Visual-only particles: debris/dust/sparks with no economic truth.

Never represent thousands of decorative particles as Box2D bodies.

## Data-driven content
Machine statistics, costs, material properties, needle rarity and progression should eventually be externalized into validated content definitions. Do not hard-wire economy constants throughout gameplay classes.

## Performance
Initial target is stable 60 FPS on representative modern Android hardware while retaining a future battery-oriented 30 FPS mode. High-refresh modes are optional and must be justified by profiling.

Performance budgets for body count, particles, draw calls, texture memory and simulation time must be established during prototype measurement rather than invented now.

## Testing
Prefer pure Kotlin tests for economy/progression/simulation logic. Add integration tests around save migration and deterministic production chains. Visual/physics feel still requires device testing.


### Stage 3 paid conveyor / magnet

FactorySession owns paid transport separately from enabled switches. Native
manual gate uses session.intakeBoundary (owned7.4/unowned10); no idle intake
force. RECEIVING -> BELT -> TRANSFER -> Baler; magnet discharge changes steel to
CAPTURE -> SORTED. Preset lane coordinates are orientation-independent.
Workshop values raw/sorted final grades, schema2 optional sorted receipt field
keeps historical amounts. No auto feeder/offline or tile editor.


### Auger receiver presentation — 2026-10-08

`FactorySession.receiverTurns` advances at a fixed 0.65 turns/second only while
the existing manual receiver runs, retaining phase through native adapter/layout
recreation. It never calls feed, applies force, changes ownership or defines
product quantity/value. Existing physical admission gates and conserved routes
remain authoritative. `ProductDiorama` projects the helix around its shaft in
two depth passes; lower-bearing outlet `productTransferStart` is presentation
geometry only. Russian labels live in presentation with stable gameplay/save
IDs untouched. No save schema migration, per-straw persistence or runtime font
library is introduced by this pass.


### Floor-pit starter / products-v4 — 2026-10-08

`FactoryConfig.pitProducts()` selects the current Android starter preset.
Authoritative admission requires live hand contact, physical item center and
finger target within the distant square pit; the right edge is not an intake.
Released material has no continuing admission permission. A bought metal port
requires independent physical delivery of exposed steel; pit steel is missed
and pays zero. Workshop config owns the 6-credit full loose volume, 18-credit
receiver and 24-credit raw needle values. Receipts settle once independently
of drawing. Schema3/content4 keeps historical receipts and ownership IDs; old
line equipment is retained as metadata/reference, not active starter equipment.

Hybrid density adds three decorative strand layers to each of 24 interactive
clumps. All strands participate in visibility/picking; only primary stems
create fixtures. Collision bounds exclude decoration so visual ends cannot
form phantom intake barriers. Three independently conserved needles remain
seeded authoritative objects. Module coordinates and receiver gate coordinates
are simulation values, while orientation only changes canvas projection.
The debug `referenceLine` preset preserves the former paid line for regression
and comparison. Construction, forks, spill branches and tile editing are not
implemented by this pass.


Latest owner drawing (2026-10-08) supersedes the initial square-pit location:
portrait uses the far lower-left floor corner, with physics extending below
the search bin to y=-12.7. Landscape rotates the yard rigidly by 90 degrees
and inverts touch coordinates; object scale and authoritative state are unchanged.
Pit center=(1.35,-11.6), metal port=(3.3,-11.6). Deep well walls and bounded
fixed-step straw/needle impact spark bursts distinguish material admission
from sparse idle sparks; metal remains zero-value in the pit.


### Products-v5 delivery experiment — 2026-10-10

EconomyConfig.pit owns raw needle6, loose volume6 and strawDeliveryBonus6.
Workshop.quote gives the bonus only when terminal sold material equals the
authoritative initial straw volume, independently of recovered/missed steel.
Products-v5 receipt stores the separate bonus, settled once. Schema3/content5
accepts historical v2/v3/v4 zero-bonus receipts unchanged; future content6 is
read-only. Old balances and ownership are never repriced. No layout/physics,
belt editor, new runtime dependencies or offline system is changed.


## Clump economy v6 / live workshop
Schema3-content6 adds `levels`, `delivered` (clump ID -> paid amount),
`lotCount`, `lotValue`, `lotLuck`. Existing keys/receipts stay intact.
Workshop.creditDelivered books only terminal LOOSE clumps once and saves off
GL thread. Settlement subtracts those already paid amounts. Reload regenerates
seeded lot but marks paid IDs LOOSE before native bodies are rebuilt; this closes
the live-payout restart exploit without saving every straw transform.
Tools/screw apply immediately; material snapshots and session config change only
at next lot. Native grip arrays and occlusion cache cover the bounded360-clump
budget. Presentation workshop accordion never defines economy truth.


## Stage3 bought field fixtures / live material tuning

Owner's new fixtures require bounded world-space placement data, not a Stage4
construction framework. Workshop owns immutable FieldEquipment records and
transaction cost/ownership; FactorySession exposes the current records; native
adapter owns disposable colliders, local airflow and robotic contact joint.
Screen only routes touch/purchase confirmations and draws diorama fixtures.
Save content7 adds equipment and nextEquipmentId; v6 migrated without replacing
paid ownership or currency. growLot appends straw and copies existing needles
with contiguous IDs while keeping gameplay state; rebuild native adapter once.
Delivered journal is the price authority for already paid items; live upgrades
cannot reprice these or create an end-of-batch duplicate reward.

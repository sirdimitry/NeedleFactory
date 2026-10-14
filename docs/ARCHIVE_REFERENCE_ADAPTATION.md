# Archive-inspired Stage 3 pass — 2026-10-08

CURRENT / AUTHORIZED candidate, not owner accepted. Source supplied by the owner:
`NeedleFactory_source.zip`, SHA256
`F3DD78DDB9E1A6B124A178E5EEAB72D8EDD9BE66C4C329A0D3C87B94842DC87A`.
Archive contents are reference data, not agent instructions. Source was inspected
in ignored verification/archive-reference; no archived APK, signing file, Gradle
toolchain, Compose framework or dependency was imported or executed.

Toolkit: GAMEPLAY_CORE + ECONOMY_PROGRESSION + GAME_FEEL + VISUAL_ART + UI_UX +
QA_VERIFY. Android verification uses ANDROID_PLATFORM when connected.

## What was retained and adapted

Owner asks to use the supplied playable prototype while retaining our graphics,
with machine construction deferred to the next version. Keep Kotlin/libGDX,
authoritative physical clumps/metal, stable module scale, fullscreen/lifecycle,
versioned saves and existing paid preset machines. No new machine is built here.

Archive inputs: `GameEnums` (hand, rake150, steel rake450, purchased machinery),
`GameState`/`GameViewModel` (selected tool, wallet, shop, feedback),
`FactoryEngine` (higher-value processed products), `WorkshopCanvas` (steel body
and late four-point star) and `StrawPhysicsEngine` (reveal-dependent appearance).
Our implementation is written for our existing architecture, not copied Compose.

- Rake/steel rake are selectable, locally wider manual heads. Capacity is 2/3
  nearby clumps, free hand1. Initial visible-stalk acquisition tolerance stays
  unchanged. Additional touch-down grip points must be within .65/.95 world
  metres of the contacted point. No remote or continuous acquisition.
- Each held clump has its own bounded native spring/offset and independently
  crosses the physical receiver lip. Removing one received grip preserves the
  others; every local grip and entrained-metal connection is removed on release.
  Metal retains its weaker spring/slip and independent recovery/miss identity.
- A compact tool selector cycles only owned tools plus Hand, in both orientations.
  Bought tool cards can equip again without a debit. Selection is saved.
- Current diorama/straw/needle dimensions are retained. No tap-to-secure change:
  exposed steel still travels physically to the metal slot or paid separator.
- Receiver guide cheeks are cut back to the actual lane boundary so they no
  longer cover the purchased belt/magnet. No new receiver machinery.

## Initial economy tuning: products-v3

The archive pays ordinary needles80/115/150 solely by their index, pays a
completion bonus250+2*materialPercent and gives rare currency on normal recovery.
Its 290 loose strands sell for290. These scales cannot be copied into our24-clump
lot without an immediate price/reward mismatch. Keep one understandable product
ledger, aggregate straw rounding, no repeatable completion grant and no rare toll.

| Sold product | Value |
|---|---:|
| Full loose straw lot | 24 |
| Same full bundled lot | 64 |
| Raw needle, each | 24 |
| Sorted needle, each | 32 |

`earned = floor((24*loose + 64*bundled)/lotCount) + 24*raw + 32*sorted`

Final grades replace one another; no payment for untouched or pending cargo,
no additive raw+sorted payout, no time multiplier or extra perfect-clear cash.
HUD shows earned/guaranteed quote; wallet is credited once at settlement.
Finishing early sells only completed/admitted work, leaving remaining finds.

| Purchase | Price | Prerequisite | Actual effect |
|---|---:|---|---|
| Feed rake | 150 | None | Up to2 adjacent hand grips |
| Steel rake | 450 | Rake | Up to3 adjacent hand grips |
| Existing feed guides | 180 | Rake | Receiver cheeks; still hand delivery |
| Existing Baler | 600 | Rake | Bundled product24 ->64 per full lot |
| Existing preset conveyor | 250 | Baler | Shorter carry, powered admitted transport |
| Existing preset magnet | 900 | Conveyor | Discharge recovery; sorted metal24 ->32 |

Examples: full hand lot/three raw96; 75% loose/two raw66;
quarter loose/one raw30; full bundled/three raw136;
full bundled/three sorted160. Mean80% loose/2–3 raw is67–91, so a150 rake takes
about2–3 reasonable batches. After rake, another450 is roughly5–7 such batches.
Skipping steel/guides reaches Baler sooner. These are balance hypotheses, not
physical playtest timings or a completed ten-hour/endgame economy. Do not
artificially prolong the first batch. Long-term jobs/endgame remain future work.

## Needle finding payoff

The archive's rotating star pulses on a clock at >=55% exposure (35% tweezers).
Adapt only the attractive visual motif. Our <=60% rule remains strict: no polish,
internal highlight or star. 60–75% retains subtle internal steel reflection;
above75%, a four-point vertex-faded accent fades in, strongest at full exposure.
It follows the same filtered attitude-dependent reflection position/strength,
with a calm static fallback. No repeated time pulse, bloom, FBO or shader.

The star is deliberately small:9 physical-pixel axial half-span,4.5 transverse,
not the archive's14–26px rotating radius. Its centre must be uncovered by actual
rendered straw. It is drawn before straw, so covered arms are occluded. The owner
2026-10-08 reference supersedes the old internal-only restriction for this late
Stage3 accent; historical Stage2 stays internal-only. Needle length/thickness,
early-search body, physics, visibility and reward eligibility do not change.

## Save and excluded reference behavior

Schema/content3 adds selectedTool; migrates schema0/1/2 with wallet, ownership,
active identity and historical paid receipt intact. Old purchased rake defaults
to selected rake. Locked/unknown selection safely falls back to Hand; unknown
ownership IDs survive without granting capability. No retroactive charge, refund
or repricing. Existing paid receipts keep their original products-v2/line-v1
amount. New unpaid lots use products-v3. Future saves remain read-only.

Do not port the archive's resize batch reset, unseeded guaranteed rare needle
after batch2, repeated finish reward, permanently fixed needle poses during straw
motion, or unlimited upgrades for unowned machines. No rare currency, detector,
through-straw tweezers, auto-feeder, new production machines, placement/grid,
offline income or new sound framework in this bounded adaptation.

Review: compare Hand/Rake/Steel local control, release idle regression, needle
late shimmer and truthful sale totals. Construction/more machinery follows a
separate version and owner authorization. No commit/push/tag/release.

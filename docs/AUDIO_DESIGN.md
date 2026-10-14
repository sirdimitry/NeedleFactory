# Stage 3 soundscape — first owner-review palette

Status: CURRENT / AUTHORIZED, not accepted. Owner requested comprehensive audio
on 2026-10-13. No economy, physics or machine progression changes are included.

## Current straw impact v6 — 2026-10-13

Greenworks garden-shredder video supplies dry-material reference direction.
Original0.68s straw cue: continuous fibrous rubbing and irregular short brittle
cracks instead of three regularly spaced smooth swells. No copied recording or
added motor layer. Idle shredder and steel audio byte-identical to prior pass.
Seven audio tests/builds/lint/GL pass; installed for owner listening review.

## Current steel impact v5 — 2026-10-13

Owner accepted idle shredder audio and confirmed the34–40s aluminium-reference
excerpt as the desired loaded-metal texture. Keep auger.wav unchanged. Original
lost.wav uses irregular broadband friction, low inharmonic contact resonances
and two short fracture transients, without the previous rising chirp. No video
samples copied. Straw remains unchanged pending a dry-material sound reference.
Measured headroom and spectral structure are verified, subjective resemblance
requires owner listening. Existing mixer/event/lifecycle architecture unchanged.

## Volume mixer revision — 2026-10-13

Owner feedback replaces binary switches with **i → Звук**, an independent modal.
Ten icon/slider/percentage rows: Общая громкость, Все эффекты, Эмбиент, Шнек,
Вентиляторы, Солома, Рука и инструменты, Иглы, Интерфейс, Деньги и награды.
Portrait uses one column; landscape two. No hidden tab/selection area. Drag/tap
updates currently playing sounds immediately; save on release/cancel/background,
avoiding disk writes for every drag frame. Zero mutes, retaining other levels.

Mix rule: ambient = authored gain × master × ambient; other elements = authored
gain × master × all-effects × element. Percentages describe the authored mix,
not maximum speaker amplitude. One fan category controls all fans; per-instance
wind/ownership physics are unchanged. Short voices retain IDs to update their
volume while playing. The existing capped loop/voice architecture remains.

Icons provide isolated ~2-second audition, ducking room tone while a different
element is previewed. Preview obeys parent volume levels and cannot change game
state. Back/rotation/background stop preview. This allows identifying the actual
source of a recurring sound without guessing from a description.

Audio preferences schemaVersion2 stores `volume.CHANNEL` integers0–100 in the
same separate audio preferences. Old effects/ambience=false migrate to0 on
their corresponding buses; explicit new levels take precedence. Clamp malformed
values; default other categories100. Economic schema remains unchanged.

The synthesized auger rasp was replaced after further owner feedback by a real
quern grinding recording: Monika Widzicka, Work With Sounds / Museum of Municipal
Engineering, CC BY4.0. Source: https://www.soundsofchanges.eu/sound/quern-stones/
Full credit/license/transformations ship in audio/ATTRIBUTION.md. Excerpt12–18.5s
becomes a six-second loop with downsampling, DC removal, high-frequency damping,
crossfade and seam smoothing. Peak-6.02dBFS/RMS-25.17dBFS, no clipping;
spectrogram inspected. It interprets the auger through real grinding texture,
not a recording of this exact machine. Fan and ambient unchanged; owner listening
still required. Reproduce offline via tools/prepare_auger.py.

## Direction

### Current IШМ reference motor — 2026-10-13

Quern/millstone recording is now replaced with original local motor/reducer
synthesis: low75/150/225Hz harmonics,625Hz gear resonance, continuous filtered
friction and gentle irregular load variation. Four-second loop with smoothed
seam, peak-6.94/RMS-16.55dBFS, unclipped. Owner video is private analysis reference
only. All current WAVs are original synthesis; earlier CC BY recording notes
below describe historical assets. Existing straw rubbing and two-break steel
strain are retained. Saved bus levels, economy/physics unchanged.

### Owner IШМ video measurements — 2026-10-13

Extracted mono22.05kHz analysis WAV from owner90.714s MP4, inspected spectrum:
persistent lower tonal bands plus broadband load/transient passages, not merely
isolated metallic ticks. Recording peak-2.62/RMS-22.61dBFS; no clipping. This
numeric inspection is not subjective listening. Video depicts aluminium frame
processing, not straw or a single steel needle; do not equate its exact transient
cadence with our material cues. Reference direction: stable motor/reducer bed,
continuous dry straw friction, needle strain/scrape followed by two short breaks.
No audio from this video is shipped; source remains private QA reference.

### Twin-shaft shredder correction — 2026-10-13 (current)

Latest owner listening feedback supersedes the mixes described below: straw now
one0.92s original rubbing/noise gesture with three connected swells, retrigger
limited to0.7s; steel1.05s original metal-on-metal strain plus exactly two click
envelopes at0.38/0.72s, minimum interval0.45s. No licensed recording remains in
these material impacts; it remains only in idle auger.wav. Spectrum/headroom
checks do not replace owner listening.

Owner photo supersedes millstone appearance with parallel cutter shafts. Straw
load retains dry fibre compression and a reduced0.55 recorded grinding bed, for
papery rubbing/rustle. Steel loss returns to original short crack/fracture
synthesis with no recorded scraping tail. The idle grinding loop remains as
approved previously. Audio label Шреддер retains saved AUGER identifier.
Previous material-load mixes below are historical, superseded by this tuning.

### Millstone material loads — 2026-10-13

Owner requested matching grinding for both straw and steel, and paired
asymmetric millstone presentation in the pit. Straw compression (0.78s) now
mixes the licensed grinding bed at0.85; steel fracture (0.86s) mixes it at1.8
before normalization, so steel has a heavier scraping texture with cracks.
Both mixed assets inherit the recording attribution (audio/ATTRIBUTION.md).
Peak-4.88/-3.10dBFS, RMS-23.61/-22.03dBFS respectively; no clipping. Mixer,
authoritative impact events, loss priority and economy unchanged. Menu calls
the AUGER bus Жернова but preserves its saved identifier/volume.

Small tactile workshop: dry material, low mechanical room tone, restrained
metallic rewards. No voice-over, cloud API or new runtime dependency. Palette is
original local synthesis except the credited licensed quern loop. Subjective
realism and mix need owner listening on Samsung.

| Event | Sound / rule |
|---|---|
| Actual hand/robot grasp, release | Soft contact transients |
| Material dragged while held | Rustle follows movement; a stationary hold is quiet |
| Robot transport | Low air swish follows real carry speed |
| Working auger | Continuous restrained motor loop |
| Straw admitted to pit | Short dry crunch; simultaneous clumps aggregate |
| Needle into pit | Distinct rough metal impact, priority above payout ticks |
| Metal recovered/packaged | Higher metallic two-note receipt |
| Individual straw payout | Quiet credit tick, capped frequency |
| Shop, sections, tools, confirmation/back | Soft UI click; no click for scrolling |
| Purchase / rejected purchase | Ascending confirmation / descending rejection |
| Equipment placed, fan anchoring | Mechanical set-down |
| Early finish prompt | Gentle attention cue |
| Settlement / perfect | Small result phrase / richer completion phrase |
| Anchored powered fans | Shared bounded airflow layer; more fans do not multiply loudness indefinitely |
| Loose fans | Airflow fades to silence, physical wind remains immediately off |
| Ambient | Warm low workshop room tone, reduced in menus/paused work |

There is deliberately no sound for hidden needle exposure or sensor-driven glint:
audio must not locate buried metal. No generic timer attention reminders.

## Ownership and lifecycle

`FactoryAudio` is a disposable presentation observer owned by the current factory
screen. State transitions remain authoritative in `FactorySession` and Workshop.
Audio never admits material, pays money or changes ownership. Loops are preloaded
through AssetManager, reused by instance ID and smoothly mixed (roughly 80 ms
response). Background/hide stops all sound; resume primes counters and contacts
without replaying discoveries. Rotation does not create a second soundscape.
Loss events have priority; cue cooldowns plus a six-token/eight-per-second budget
prevent simultaneous deliveries from spamming playback. Four loop instances and
16 Android streams are the ceiling. Hot mixing arrays are reused.

Settings in `i → Звук`: independent volume sliders described above. Persist
separately in libGDX preferences `needlefactory-audio-v1`, outside economic save.
Master/effects/element zero silences corresponding voices, ambient zero stops room tone.
No permissions, connectivity, offline rewards or wallet migration are needed.

## Assets and reproduction

Run `python tools/generate_audio.py` for the 17 synthesized WAVs. It preserves
the separately prepared recorded auger. All18 assets are mono22050Hz PCM16;
prepare auger with `python tools/prepare_auger.py <original WAV>` (source and
credit in audio/ATTRIBUTION.md).
Short Sound assets remain below Android's 1MB decoded limit; room tone streams
as Music. Android packages resources as APK assets because its Music backend
requires real AssetManager files, not Java classpath streams. Desktop falls back
to classpath resources. Existing recorded/visual resources are untouched.

NeedleFactory owns its synthesis script/output; recorded auger retains CC BY4.0
attribution obligations documented in the shipped ATTRIBUTION.md. No paid
generators. Skills are guidance:
`kyu-n/gdx-claude-skills` libgdx-audio-lifecycle and `bman654/audio-forge` audio-lens.
Original quern WAV SHA256:
56b256e9aab37002f80dae37845147f4ca2d226c45cba770147598ac32665b2a.

## Review gates

Automated: nonempty/format/headroom, short-asset memory, seam continuity,
delivery spam/priority gates; preserve all gameplay/save tests. Audio-lens must
pass its 12/12 self-test before measurement. Validate no clipping and inspect
spectrograms, then debug/release/lint and a real GL smoke with audio enabled.

Samsung: verify loading/playback paths, quiet hold vs drag, straw/metal distinction,
anchored/loose fan, shop/back/purchase, mute/unmute persistence, both orientations,
Home/resume and unchanged owner progression. Automated playback evidence does
not establish subjective pleasantness, balance, realism or lack of audible loop
seams. Owner must listen at normal volume for those, including long-session fatigue.

## Auger material impacts — 2026-10-13

Owner correction: straw admission should sound like fibres being compressed and
chewed; needle loss in the auger should sound like metal fracturing. Replaced
`straw.wav` with a 0.78s seeded papery compression/fibre-snap sequence and
`lost.wav` with a 0.86s four-crack inharmonic metal-fracture sequence. Original
local synthesis; no external samples or dependencies. Existing authoritative
strawImpacts/metalImpacts events trigger these cues, with existing aggregation,
loss priority and STRAW/METAL mixer routing. The auger idle loop is unchanged.
Audio-lens self-test12/12; both files unclipped, peak -4.88/-3.10dBFS and RMS
-24.33/-23.31dBFS respectively. Subjective texture requires owner listening.

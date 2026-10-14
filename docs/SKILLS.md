# Agent Skill Plan

## Owner-authorized audio skills (2026-10-13)

- [libgdx-audio-lifecycle](https://github.com/kyu-n/gdx-claude-skills/tree/master/skills/libgdx-audio-lifecycle): Sound/Music, Android asset limits, load/dispose and lifecycle.
- [audio-lens](https://github.com/bman654/audio-forge/tree/main/skills/audio-lens) (MIT): local WAV measurements and spectrograms; self-test first. Measurements do not replace listening.

Installed in the owner's Codex skills directory, not vendored into game runtime.
Use project AUDIO_SOUND profile and [AUDIO_DESIGN.md](AUDIO_DESIGN.md).
Original assets are synthesized locally using Python standard library only.
No external paid API, speech model, plugin runtime or audio framework is adopted.

These are **reference skills**, not vendored runtime dependencies. Their licenses and applicability must be respected. Do not copy large third-party skill repositories into NeedleFactory unless explicitly approved.

## NeedleFactory curated routing layer

The authoritative project-local skill router is `docs/NEEDLEFACTORY_GAMEDEV_TOOLKIT.md`.

It curates approved skills and role lenses from multiple external repositories into task profiles such as `GAMEPLAY_CORE`, `PHYSICS_TOUCH`, `VISUAL_ART`, `UI_UX`, `QA_VERIFY`, `PERFORMANCE`, and `ANDROID_PLATFORM`.

Do not bulk-activate every referenced skill. Use the smallest profile set required for the task, and follow the toolkit's owner-visibility/change-control rules.
## Tier A — use from the beginning

### Game design
Source: `edhahn/agent-skills — game-designer`.
Purpose: mechanics, systems, progression, economy and GDD discipline.

### Game design fundamentals
Source: `fcsouza/agent-skills — game-design-fundamentals`.
Purpose: core loop, feedback, player motivation, progression curves and economy reasoning.

### Game UI design
Source: `jeremylongworth-source/AgentSkills — game-ui-design`.
License shown by source: MIT.
Purpose: HUD/menu state design, touch behavior, accessibility, responsive game UI and event-driven state ownership.

### Game balance
Source: `DSmereski/agent-skills — game-balance`.
Verified reference path (2026-10-10):
https://github.com/DSmereski/agent-skills/blob/main/skills/game-balance.md
Purpose: tune measurable economy/progression targets with headless simulation rather than intuition alone.

## Tier B — Android architecture/testing references

Source: `humanshell/android-skills`.

Useful skills:
- `android-module-structure`
- `android-testing`
- `android-error-handling`

Do **not** automatically apply Compose/MVI/navigation/DI skills to the game core. They are designed primarily for Android applications; use their principles only where appropriate to the Android platform layer.

## Tier C — later, when the feature exists

- game UI/UX review
- progression-tree design
- procedural generation
- CI/CD game workflow
- concept-art/visual-design workflows

Do not activate narrative/backend/payment skills merely because they exist. NeedleFactory is currently offline-first and deliberately light on narrative.

## Stage 2 — mandatory visual craft profile

For Stage 2 automation work, the coding agent must actively apply the following disciplines in addition to game engineering:

### Technical artist / 2.5D environment artist
Responsibilities:
- coherent machine silhouettes and proportions;
- believable mechanical joints and material handoffs;
- consistent perspective/depth cues;
- contact shadows, layering and visual separation;
- procedural shapes that look authored rather than accidental;
- performance-aware visual construction suitable for mobile.

### Industrial product / motion designer
Responsibilities:
- machinery that visually communicates function;
- conveyors with belts, rollers, frames, supports and transfer points;
- clear moving parts and motion language;
- readable cause/effect without relying on labels.

### Game UI/UX designer
Use the existing `game-ui-design` reference skill plus general UI/UX principles.
Responsibilities:
- deliberate HUD hierarchy;
- compact touch controls;
- responsive portrait/landscape composition;
- readable spacing/alignment;
- debug information visually subordinate to gameplay;
- accessible state indication beyond color alone.

### Visual review rule
A feature is not ready for owner review merely because it is functional.
Before review, the agent must do a separate visual pass and reject obvious:
- floating rectangles;
- disconnected conveyors/chutes;
- clipping/overlap artifacts;
- inconsistent machine scale;
- placeholder-looking HUD composition;
- excessive unused space;
- programmer-art geometry that obscures the intended industrial-diorama direction.

When relevant external skills are available, prefer skills covering:
- game UI design;
- visual design / frontend-design principles;
- technical art;
- 2D/2.5D game art;
- industrial/mechanical shape language;
- motion design.

Do not add a runtime framework merely to improve appearance. The skill upgrade is primarily a **design/review discipline**, not a dependency.

## Universal Modder — adopted visual-production / verification toolkit

Source: `rehan-remade/universal-modder` (MIT).

NeedleFactory does **not** adopt Universal Modder as a game-modding framework. We selectively reuse its agent workflow and relevant skills as design/verification guidance.

### Approved uses
- `asset-pipeline`: reference workflow for turning authored/generated art into engine-ready sprites/textures/frames.
- `fal-assets`: optional reference for asset-generation workflows only; do not require fal or any paid API for the project. Use only with explicit owner approval and never commit API keys.
- `game-automation`: reference ideas for repeatable launch/screenshot/window verification where they fit our Android/desktop test flow.
- `share-field-notes` / knowledge-base method: preserve exact versions, verification method, and `symptom -> cause -> fix` notes so later agents do not rediscover solved problems.
- verification-oracle mindset: define observable evidence for visual/gameplay claims before declaring work complete.

### NeedleFactory adaptation
For visual/gameplay work, agents should combine Universal Modder ideas with our existing technical-art and game-UI rules:
1. build one small visual slice;
2. run it on the real target device where possible;
3. capture portrait/landscape evidence;
4. compare against explicit owner acceptance criteria;
5. record defects as reproducible observations, not vague impressions;
6. fix and re-verify;
7. add durable field notes to the relevant `STAGE_X_REPORT.md`.

### Do not use by default
- `mod-any-game`, engine reconnaissance, loaders, decompilation, reverse engineering, memory tooling, Cheat Engine, Frida, Ghidra/IDA workflows, or other PC-game-mod routes are outside NeedleFactory's normal development path.
- Do not install or ship Universal Modder runtime code inside the Android game.
- Do not add Blender/ComfyUI/fal dependencies unless a concrete art task needs them and the owner explicitly approves.

### Codex installation reference
If the owner later chooses to enable the plugin in Codex CLI, the upstream documented route is:

```text
codex plugin marketplace add rehan-remade/universal-modder
codex plugin add universal-modder@universal-modder
```

A skills-only alternative documented upstream is:

```text
npx skills add https://github.com/rehan-remade/universal-modder
```

Installation is an environment action, not a repository dependency. Verify the installed plugin/skills and applicable license before use.

## Skill-selection rule

For every substantial task:
1. Read `AGENTS.md`.
2. Identify the smallest set of relevant skills.
3. State which disciplines/skills are being applied.
4. Reject advice that conflicts with this repository's architecture or mobile constraints.
5. Keep third-party skill text out of production source unless its license and purpose are explicitly acceptable.

## Important distinction

A skill is **guidance for the coding agent**, not a Gradle dependency and not game runtime code. The game must remain buildable without access to an agent-skill repository.

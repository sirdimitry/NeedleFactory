# Agent Skill Plan

These are **reference skills**, not vendored runtime dependencies. Their licenses and applicability must be respected. Do not copy large third-party skill repositories into NeedleFactory unless explicitly approved.

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

## Skill-selection rule

For every substantial task:
1. Read `AGENTS.md`.
2. Identify the smallest set of relevant skills.
3. State which disciplines/skills are being applied.
4. Reject advice that conflicts with this repository's architecture or mobile constraints.
5. Keep third-party skill text out of production source unless its license and purpose are explicitly acceptable.

## Important distinction

A skill is **guidance for the coding agent**, not a Gradle dependency and not game runtime code. The game must remain buildable without access to an agent-skill repository.

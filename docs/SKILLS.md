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

## Skill-selection rule

For every substantial task:
1. Read `AGENTS.md`.
2. Identify the smallest set of relevant skills.
3. State which disciplines/skills are being applied.
4. Reject advice that conflicts with this repository's architecture or mobile constraints.
5. Keep third-party skill text out of production source unless its license and purpose are explicitly acceptable.

## Important distinction

A skill is **guidance for the coding agent**, not a Gradle dependency and not game runtime code. The game must remain buildable without access to an agent-skill repository.

# Universal Modder Integration Notes

Source: `https://github.com/rehan-remade/universal-modder`
License: MIT (verified from upstream repository).

## Why it is relevant
NeedleFactory is built from scratch and does not need the repository's reverse-engineering/mod-loader stack. The useful parts are its repeatable agent workflow around asset production, runtime verification, screenshots, and durable field notes.

## Adopted concepts
- **Asset pipeline discipline** — generated/authored visuals must be transformed into predictable engine-ready assets rather than dropped into the game ad hoc.
- **Verification oracles** — before claiming a visual or gameplay fix, define what observable evidence proves it: target-device screenshot, no white gutters, correct portrait/landscape composition, preserved state, connected machine geometry, etc.
- **Game automation ideas** — use repeatable launch/screenshot/smoke flows where appropriate, adapted to Android/ADB/libGDX rather than PC-game automation.
- **Field notes** — record exact versions, route, verification, and `symptom -> cause -> fix` gotchas in stage reports.

## Specifically useful for the current NeedleFactory pain points
- machine/conveyor visual consistency;
- sprite/texture preparation and transparent assets;
- portrait/landscape screenshot comparison;
- fullscreen/inset regression checks;
- repeatable device smoke tests;
- preventing agents from repeating already-solved visual/Android problems.

## Not adopted
- engine reconnaissance for third-party games;
- decompilation/reverse engineering;
- loaders/injection/memory tooling;
- cheat/mod workflows;
- automatic external asset services as a mandatory dependency.

## External asset generation
Upstream supports fal-based generation and a local ComfyUI path. Neither is required by NeedleFactory. Any external generation service, key, cost, or additional local tool must be explicitly approved by the owner first.

## Future agent request
When the owner asks to enable this toolkit in Codex, first verify the installed plugin/skills and current upstream instructions, then apply only the relevant asset/automation/verification skills to the current stage. Do not broaden scope into modding or reverse engineering.

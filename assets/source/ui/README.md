# Stage 2 instrument typography

Barlow Medium by The Barlow Project Authors, downloaded from
https://github.com/google/fonts/blob/main/ofl/barlow/Barlow-Medium.ttf
and licensed under SIL OFL 1.1. Full copyright/license is in
`assets/licenses/Barlow-OFL.txt` and bundled with the bitmap font under
`core/src/main/resources/ui/Barlow-OFL.txt` for desktop distributions as well.

Source SHA-256:
`F8906F762CB73DCA441DA034BC363B2D8E2E68BC10D5C05E58717646C20CC4B4`.

`python tools/generate_ui_font.py` uses Pillow 12.1.0 to bake a 48px ASCII
atlas as `ui/needle-panel.png` and `.fnt`. The derivative bitmap family is
named NeedlePanel Medium and remains OFL-licensed. No font rasterizer or
Pillow is needed to build/run the game; prepared resources are checked in.
All machine/material geometry is original procedural code, not imported art.

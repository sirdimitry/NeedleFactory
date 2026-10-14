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


## Stage 3 Russian interface — 2026-10-08

The current factory UI uses NeedlePanel RU, an offline 48px bitmap bake of
DejaVu Sans with ASCII, Cyrillic including Ё/ё, and UI punctuation. The source
was copied from the already installed matplotlib font bundle; no download,
new build dependency or runtime font rasterizer is required. DejaVu's full
Bitstream Vera/Arev redistribution license is retained in
`assets/licenses/DejaVu-LICENSE.txt` and packaged as `ui/DejaVu-LICENSE.txt`.

Source SHA256:
`3FDF69CABF06049EA70A00B5919340E2CE1E6D02B0CC3C4B44FB6801BD1E0D22`.
Reproduce with `python tools/generate_ru_font.py` (existing Pillow).
Prepared `needle-panel-ru.fnt` and 1024x512 `needle-panel-ru.png` are shipped;
Barlow assets remain for historical diagnostic/search reference screens.

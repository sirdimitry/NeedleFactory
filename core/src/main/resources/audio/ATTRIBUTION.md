# Recorded auger grinding sound

CURRENT UPDATE 2026-10-13: the quern recording has been replaced. All currently
shipped WAVs, including auger.wav, are original local synthesis. The credit below
documents the previous owner-review asset only; it is not used by the current
generator. Owner ИШМ video was analysed as reference, not copied into the game.

`auger.wav` is adapted from **WWS Quern-stones**, recorded by **Monika Widzicka**,
**Work With Sounds / Museum of Municipal Engineering**, 25 June 2015.
Recording location: National Museum of Agriculture and Agricultural-Food
Industry, Szreniawa, Poland.

Source: https://commons.wikimedia.org/wiki/File:WWS_Quern-stones.ogg
Recording page: https://www.soundsofchanges.eu/sound/quern-stones/
Original WAV: https://www.soundsofchanges.eu/soundfiles/55c/55ce05e7d7213.wav
License: **Creative Commons Attribution 4.0 International (CC BY 4.0)**
https://creativecommons.org/licenses/by/4.0/

Changes: excerpt 12–18.5s, downsampled mono 22.05kHz PCM16, DC removed,
high-frequency damping, half-second loop crossfade, seam smoothing, peak 0.5.
Result is a 6-second grinding loop. No endorsement by the recording authors.
Reproduce with `tools/prepare_auger.py` and the linked original WAV.
Current `straw.wav` and `lost.wav` are entirely original rubbing/strain/fracture
synthesis, without the recorded bed. Previous versions mixed the recording into
material impacts; that use has been removed. Other WAVs remain original local
synthesis. The recording is now used only in `auger.wav`.

"""Prepare licensed quern recording as auger loop (offline asset tool, stdlib).
Usage: python tools/prepare_auger.py path/to/55ce05e7d7213.wav
Source URL / credit / transformations: core/src/main/resources/audio/ATTRIBUTION.md
"""
import hashlib, math, struct, sys, wave
from pathlib import Path

source=Path(sys.argv[1])
with wave.open(str(source)) as w:
    assert (w.getnchannels(),w.getsampwidth(),w.getframerate())==(1,2,44100)
    samples=struct.unpack('<'+'h'*w.getnframes(),w.readframes(w.getnframes()))
# Fixed 12..18.5s section, preserve actual mechanical cadence. Pair averaging
# reduces aliasing for 22.05kHz mobile playback; remove DC / damp high hiss.
section=samples[12*44100:round(18.5*44100)]
mean=sum(section)/len(section);data=[];low=0.
for i in range(0,len(section)-1,2):
    value=((section[i]+section[i+1])*.5-mean)/32768
    low+=(value-low)*.65
    data.append(low)
# Overlap half-second tail with head using unity-sum smooth fade (6s loop).
fade=11025;tail=data[-fade:];data=data[:-fade]
for i in range(fade):
    u=i/(fade-1);a=u*u*(3-2*u)
    data[i]=tail[i]*(1-a)+data[i]*a
for i in range(96):
    u=i/95;a=u*u*(3-2*u)
    data[-96+i]=data[-96+i]*(1-a)+data[0]*a
scale=.5/(max(abs(v) for v in data) or 1)
output=Path(__file__).resolve().parents[1]/'core/src/main/resources/audio/auger.wav'
with wave.open(str(output),'wb') as w:
    w.setnchannels(1);w.setsampwidth(2);w.setframerate(22050)
    w.writeframes(struct.pack('<'+'h'*len(data),*(round(v*scale*32767) for v in data)))
print('Source SHA256',hashlib.sha256(source.read_bytes()).hexdigest())
print('Prepared',output,'duration',len(data)/22050)

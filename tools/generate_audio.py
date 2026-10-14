"""Original NeedleFactory synthesis, standard library only.
Run from any directory; shipped mono PCM WAVs are deterministic and replaceable.
"""
import math, random, wave, struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'core/src/main/resources/audio'
RATE = 22050
OUT.mkdir(parents=True, exist_ok=True)

def write(name, data, peak=.65, loop=False):
    # Periodic synthesis + end/start crossfade removes noise discontinuities in loops.
    if loop:
        fade = min(2205, len(data)//8)
        tail = data[-fade:]
        data = data[:-fade]
        for i in range(fade):
            a=i/(fade-1)
            data[i]=tail[i]*(1-a)+data[i]*a
        # Close the final 4 ms with a smooth bridge to the exact first sample.
        # Broadband noise can otherwise have a large discontinuity at repetition.
        bridge=96
        for i in range(bridge):
            u=i/(bridge-1);a=u*u*(3-2*u)
            data[-bridge+i]=data[-bridge+i]*(1-a)+data[0]*a
    else:
        fade=min(220,len(data)//8)
        for i in range(fade):
            data[i]*=i/fade;data[-1-i]*=i/fade
    maximum=max(abs(x) for x in data) or 1
    data=[x*peak/maximum for x in data]
    with wave.open(str(OUT/(name+'.wav')), 'wb') as w:
        w.setnchannels(1);w.setsampwidth(2);w.setframerate(RATE)
        w.writeframes(struct.pack('<'+'h'*len(data),*(round(x*32767) for x in data)))

def noise(name, seconds, cutoff, tone=0, loop=False, peak=.55):
    rng=random.Random(name);low=0;data=[]
    for i in range(round(seconds*RATE)):
        t=i/RATE; low+=(rng.uniform(-1,1)-low)*cutoff
        env=1 if loop else math.exp(-5*t/seconds)*min(1,t/.012)
        trem=.75+.25*math.sin(2*math.pi*(7 if name=='auger' else 3)*t)
        data.append(env*(low*trem+(math.sin(2*math.pi*tone*t)*.13 if tone else 0)))
    write(name,data,peak,loop)

def notes(name, tones, duration=.18, peak=.55):
    data=[0.] * round((len(tones)*duration+.32)*RATE)
    for n,f in enumerate(tones):
        for i in range(round(.32*RATE)):
            t=i/RATE;env=min(1,t/.006)*math.exp(-t*15)
            v=env*(math.sin(2*math.pi*f*t)+.22*math.sin(2*math.pi*f*2.01*t))
            j=round(n*duration*RATE)+i
            if j<len(data):data[j]+=v
    write(name,data,peak)

noise('rustle',1.2,.28,loop=True,peak=.42)
noise('fan',2,.045,110,True,.48)
# Shredder motor/reducer bed: sustained low mechanical harmonics and friction,
# not the old millstone recording. Owner video is reference only, no copied audio.
rng=random.Random('ishm-reducer-bed-v4');low=0.;fast=0.;data=[]
for i in range(round(4.1*RATE)):
    t=i/RATE;n=rng.uniform(-1,1)
    low+=(n-low)*.018;fast+=(n-fast)*.16
    load=.88+.07*math.sin(2*math.pi*.75*t)+.05*math.sin(2*math.pi*1.25*t)
    motor=sum(math.sin(2*math.pi*f*t)*a for f,a in [(75,.18),(150,.08),(225,.035)])
    gearbox=.03*math.sin(2*math.pi*625*t+.35*math.sin(2*math.pi*.75*t))
    data.append((motor+gearbox+(fast-low)*.3+low*.4)*load)
write('auger',data,.45,True)
noise('carry',1.2,.035,loop=True,peak=.35)
# Dry cutting/crumpling texture, informed by the owner's garden-shredder video.
# No recording samples or motor layer; the existing idle bed supplies machinery.
def straw_crush():
    rng=random.Random('garden-dry-fibres-v6');slow=0.;fast=0.;grain=0.;data=[]
    duration=.68
    # Irregular small fibre fractures, with no metallic ring or repeating swells.
    fractures=[];start=.035
    while start<duration-.08:
        fractures.append((start,rng.uniform(.10,.24),rng.uniform(.004,.014)))
        start+=rng.uniform(.018,.063)
    for i in range(round(duration*RATE)):
        t=i/RATE;n=rng.uniform(-1,1)
        slow+=(n-slow)*.055;fast+=(n-fast)*.42;grain+=(n-grain)*.003
        env=min(1,t/.018)*min(1,(duration-t)/.12)
        pressure=.55+min(.45,abs(grain)*9)
        rubbing=((fast-slow)*.40+slow*.55)*pressure
        crackle=0.
        for start,a,decay in fractures:
            u=t-start
            if u>=0:
                crackle+=a*min(1,u/.0008)*math.exp(-u/decay)*(fast-slow)*3
        data.append(env*(rubbing+crackle))
    write('straw',data,.57)

def metal_fracture():
    # Reference 34-40s: uneven broadband loaded scraping rather than a pitched
    # cartoon squeal. Original synthesis; no recording from the video is used.
    rng=random.Random('ishm-loaded-steel-v5');low=0.;fast=0.;grain=0.;data=[]
    duration=1.22
    modes=[(f,random.Random(f).uniform(0,math.tau)) for f in (680,1123,1837,2671,3911)]
    for i in range(round(duration*RATE)):
        t=i/RATE;n=rng.uniform(-1,1)
        low+=(n-low)*.065;fast+=(n-fast)*.48;grain+=(n-grain)*.0015
        attack=min(1,t/.035);release=max(0,1-t/1.04)**.55
        # Irregular stick/slip under strain, no regularly repeated pulses.
        load=attack*release*(.48+.52*min(1,abs(grain)*18))
        contacts=sum(math.sin(2*math.pi*f*t+p)*.018 for f,p in modes)
        v=load*((fast-low)*.95+low*.2+contacts)
        for start,a in [(.43,1.),(.79,.85)]:
            u=t-start
            if u>=0:
                snap_attack=min(1,u/.001)
                snap=(n-low)*math.exp(-u/.007)*2.5
                ring=sum(math.sin(2*math.pi*f*u)*g for f,g in
                         [(1837,.25),(2671,.17),(3911,.10)])*math.exp(-u/.027)
                v+=a*snap_attack*(snap+ring)
        data.append(v)
    write('lost',data,.7)

straw_crush()
noise('grip',.13,.17,peak=.4)
noise('release',.2,.12,peak=.4)
noise('place',.16,.06,150,peak=.55)
metal_fracture()
notes('click',[620],.08,.35)
notes('reject',[280,210],.10,.45)
notes('metal',[1320,1760],.065,.58)
notes('credit',[880,1100],.07,.35)
notes('purchase',[440,660,880],.10,.6)
notes('result',[440,550,660],.16,.55)
notes('perfect',[440,660,880,1100],.14,.65)
notes('attention',[660,550],.12,.4)
# Sparse, warm workshop room tone, no melody that competes with search cues.
rng=random.Random(71);data=[];low=0
for i in range(12*RATE):
    t=i/RATE;low+=(rng.uniform(-1,1)-low)*.007
    data.append(low*.2+sum(math.sin(2*math.pi*f*t)*a for f,a in [(55,.08),(110,.035),(165,.012)])*(.8+.2*math.cos(2*math.pi*t/12)))
write('ambient',data,.32,True)
print('Prepared',len(list(OUT.glob('*.wav'))),'original mono WAV assets')

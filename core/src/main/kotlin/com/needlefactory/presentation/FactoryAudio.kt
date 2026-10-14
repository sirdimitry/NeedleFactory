package com.needlefactory.presentation

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.needlefactory.economy.EquipmentKind
import com.needlefactory.physics.FactoryIntakePhysics
import com.needlefactory.simulation.*
import kotlin.math.*

enum class AudioCue(val file:String, val volume:Float, val interval:Float) {
    CLICK("click",.32f,.08f), REJECT("reject",.4f,.3f), METAL("metal",.48f,.12f),
    STRAW("straw",.4f,.7f), LOST("lost",.6f,.45f), CREDIT("credit",.22f,.35f),
    PURCHASE("purchase",.5f,.3f), RESULT("result",.45f,.7f), PERFECT("perfect",.55f,.7f),
    ATTENTION("attention",.3f,.6f), GRIP("grip",.28f,.18f), RELEASE("release",.28f,.18f), PLACE("place",.35f,.15f)
}

/** Pure rate oracle: simultaneous deliveries aggregate, urgent loss remains audible. */
class AudioGate {
    private val last=FloatArray(AudioCue.entries.size) { -100f }
    private var now=0f;private var tokens=6f
    fun advance(dt:Float) { val d=if(dt.isFinite()) dt.coerceIn(0f,.1f) else 0f;now+=d;tokens=min(6f,tokens+d*8) }
    fun allow(cue:AudioCue):Boolean {
        if(now-last[cue.ordinal]<cue.interval || (tokens<1 && cue!=AudioCue.LOST)) return false
        last[cue.ordinal]=now;tokens=max(0f,tokens-1);return true
    }
}

/** Presentation-only soundscape. Never owns delivery/reward truth or mutates simulation. */
internal class FactoryAudio(private val diagnostics:Boolean) {
    // Android Music/Sound require APK assets, not Java classpath streams.
    private val assets=AssetManager { name -> Gdx.files.internal(name).let { if(it.exists()) it else Gdx.files.classpath(name) } }
    private val prefs=Gdx.app.getPreferences("needlefactory-audio-v1")
    val levels=AudioLevels(AudioChannel.entries.filter {prefs.contains("volume.${it.name}")}.associateWith {prefs.getInteger("volume.${it.name}")},
        prefs.getBoolean("effects",true),prefs.getBoolean("ambience",true))
    val effects get()=levels.gain(AudioChannel.EFFECTS)>0
    val ambience get()=levels.gain(AudioChannel.AMBIENT)>0
    private var failed=false;private var ready=false;private var halted=false
    private val gate=AudioGate()
    private val loops=arrayOf("auger","fan","rustle","carry")
    private val loopChannels=arrayOf(AudioChannel.AUGER,AudioChannel.FAN,AudioChannel.STRAW,AudioChannel.ROBOT)
    private val ids=LongArray(4) { -1L };private val gains=FloatArray(4)
    private val targets=FloatArray(4)
    private var ambient:Music?=null
    private var robotHeld=false;private var handHeld=false
    private var primed=false;private var recovered=0;private var seed=0L
    private var lastX=0f;private var lastY=0f;private var robotX=0f;private var robotY=0f
    private var variation=0
    private val voiceIds=LongArray(12) {-1};private val voiceSounds=arrayOfNulls<Sound>(12)
    private val voiceCues=arrayOfNulls<AudioCue>(12);private var voiceCursor=0
    private var previewSound:Sound?=null;private var previewId=-1L;private var previewAge=0f
    private var previewChannel:AudioChannel?=null;private var previewBase=0f
    private var levelsDirty=false
    init {
        assets.setErrorListener { _,error -> failed=true;Gdx.app.error("FactoryAudio","Audio unavailable; gameplay continues",error) }
        for(c in AudioCue.entries) assets.load("audio/${c.file}.wav",Sound::class.java)
        for(name in loops) assets.load("audio/$name.wav",Sound::class.java)
        assets.load("audio/ambient.wav",Music::class.java)
    }
    fun setLevel(channel:AudioChannel,percent:Int) {
        if(levels[channel]==percent.coerceIn(0,100)) return
        levels.set(channel,percent);levelsDirty=true
        if(ready && !failed) {
            for(i in voiceIds.indices) voiceCues[i]?.let { c ->voiceSounds[i]?.setVolume(voiceIds[i],c.volume*levels.gain(AudioLevels.channel(c))) }
            for(i in ids.indices) if(ids[i]>=0) assets.get("audio/${loops[i]}.wav",Sound::class.java).setVolume(ids[i],gains[i]*levels.gain(loopChannels[i]))
            if(!ambience) ambient?.stop()
            previewChannel?.let {previewSound?.setVolume(previewId,previewBase*levels.gain(it))}
        }
    }
    fun saveLevels() {
        if(!levelsDirty) return
        for(c in AudioChannel.entries) prefs.putInteger("volume.${c.name}",levels[c])
        prefs.putInteger("schemaVersion",2).flush();levelsDirty=false
        if(diagnostics) Gdx.app.log("FactoryAudio","MIX saved "+AudioChannel.entries.joinToString {"${it.name}=${levels[it]}"})
    }
    fun preview(channel:AudioChannel) {
        stopPreview()
        if(!ready || failed || halted) return
        previewChannel=channel;previewAge=2.2f
        if(channel==AudioChannel.AMBIENT) {ambient?.volume=.2f*levels.gain(channel);ambient?.play()}
        else {
            val name=when(channel) {AudioChannel.AUGER->"auger";AudioChannel.FAN->"fan";AudioChannel.STRAW->"rustle";
                AudioChannel.ROBOT->"grip";AudioChannel.METAL->"metal";AudioChannel.REWARDS->"purchase";else->"click"}
            previewBase=if(channel==AudioChannel.AUGER || channel==AudioChannel.FAN) .3f else .45f
            previewSound=assets.get("audio/$name.wav",Sound::class.java)
            previewId=previewSound!!.play(previewBase*levels.gain(channel))
            ambient?.volume=0f
        }
        if(diagnostics) Gdx.app.log("FactoryAudio","PREVIEW $channel")
    }
    fun stopPreview() {previewSound?.stop(previewId);previewSound=null;previewChannel=null;previewAge=0f;previewId=-1}
    fun cue(c:AudioCue) {
        if(!ready || failed || halted || !effects || !gate.allow(c)) return
        val pitch=when(c) {AudioCue.STRAW,AudioCue.GRIP,AudioCue.RELEASE,AudioCue.CLICK->.96f+(variation++%5)*.02f;else->1f}
        val sound=assets.get("audio/${c.file}.wav",Sound::class.java);val slot=voiceCursor++%voiceIds.size
        voiceSounds[slot]?.stop(voiceIds[slot]);voiceSounds[slot]=sound;voiceCues[slot]=c
        voiceIds[slot]=sound.play(c.volume*levels.gain(AudioLevels.channel(c)),pitch,0f)
        if(diagnostics) Gdx.app.log("FactoryAudio","cue=${c.name}")
    }
    fun update(dt:Float,session:FactorySession,physics:FactoryIntakePhysics,menu:Boolean) {
        if(failed || halted) return
        gate.advance(dt)
        if(!ready) {
            if(!assets.update() || failed) return
            ready=true;ambient=assets.get("audio/ambient.wav",Music::class.java).apply { isLooping=true;volume=0f }
            if(diagnostics) Gdx.app.log("FactoryAudio","READY effects=$effects ambience=$ambience")
        }
        previewAge=(previewAge-dt.coerceIn(0f,.1f)).coerceAtLeast(0f)
        if(previewAge==0f && previewChannel!=null) stopPreview()
        val active=session.running && !menu && session.phase!=BatchPhase.RESULTS
        if(!primed || seed!=session.batch.seed) {
            primed=true;seed=session.batch.seed;recovered=session.recovered
            robotHeld=physics.robotCarrying;handHeld=physics.heldCount>0
            lastX=physics.pusherX;lastY=physics.pusherY;robotX=physics.robotX;robotY=physics.robotY
        }
        if(session.recovered>recovered) cue(AudioCue.METAL)
        recovered=session.recovered
        val held=active && physics.heldCount>0;val robot=active && physics.robotCarrying
        if(active) {
            if(held && !handHeld || robot && !robotHeld) cue(AudioCue.GRIP)
            if(!held && handHeld || !robot && robotHeld) cue(AudioCue.RELEASE)
        }
        val speed=if(dt>.001f && held && handHeld) hypot(physics.pusherX-lastX,physics.pusherY-lastY)/dt else 0f
        val robotSpeed=if(dt>.001f && robot) hypot(physics.robotX-robotX,physics.robotY-robotY)/dt else 0f
        handHeld=held;robotHeld=robot;lastX=physics.pusherX;lastY=physics.pusherY;robotX=physics.robotX;robotY=physics.robotY
        var fans=0
        for(e in session.equipment) if(e.kind==EquipmentKind.FAN && e.anchored) fans++
        targets[0]=if(active && session.config.pitReceiver && session.receivingEnabled) .13f else 0f
        targets[1]=if(active && fans>0) min(.19f,.10f+fans*.025f) else 0f
        targets[2]=if(active) min(.24f,speed*.035f+robotSpeed*.015f) else 0f
        targets[3]=if(active) min(.08f,robotSpeed*.012f) else 0f
        for(i in loops.indices) {
            val target=targets[i]
            gains[i]+=(target-gains[i])*(1f-exp(-dt.coerceIn(0f,.1f)*12f))
            val sound=assets.get("audio/${loops[i]}.wav",Sound::class.java)
            if(gains[i]>.002f) {
                if(ids[i]<0) {ids[i]=sound.loop(0f);if(diagnostics) Gdx.app.log("FactoryAudio","loop=${loops[i]} start id=${ids[i]}")}
                if(ids[i]>=0) sound.setVolume(ids[i],gains[i]*levels.gain(loopChannels[i]))
            } else if(ids[i]>=0) {sound.stop(ids[i]);ids[i]=-1;if(diagnostics) Gdx.app.log("FactoryAudio","loop=${loops[i]} stopped")}
        }
        ambient?.let { music ->
            val target=when {previewAge>0f->if(previewChannel==AudioChannel.AMBIENT) .2f*levels.gain(AudioChannel.AMBIENT) else 0f
                ambience->(if(menu || !session.running) .045f else .10f)*levels.gain(AudioChannel.AMBIENT);else->0f}
            music.volume+=(target-music.volume)*(1f-exp(-dt.coerceIn(0f,.1f)*3f))
            if(target>0 && !music.isPlaying) music.play()
            if(target==0f) music.stop()
        }
    }
    private fun stopEffects() {
        if(ready && !failed) {for(c in AudioCue.entries) assets.get("audio/${c.file}.wav",Sound::class.java).stop()
            for(name in loops) assets.get("audio/$name.wav",Sound::class.java).stop()}
        ids.fill(-1);gains.fill(0f)
        voiceIds.fill(-1);voiceSounds.fill(null);voiceCues.fill(null)
    }
    fun pause() {saveLevels();halted=true;stopPreview();stopEffects();ambient?.stop();handHeld=false;robotHeld=false;primed=false
        if(diagnostics) Gdx.app.log("FactoryAudio","SUSPEND all stopped")}
    fun resume() {halted=false;primed=false}
    fun dispose() {pause();assets.dispose()}
}

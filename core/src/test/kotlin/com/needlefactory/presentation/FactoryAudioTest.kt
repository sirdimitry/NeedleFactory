package com.needlefactory.presentation

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FactoryAudioTest {
    @Test fun `mass straw admission does not restart the rubbing stroke every frame`() {
        val gate=AudioGate();assertTrue(gate.allow(AudioCue.STRAW))
        repeat(6) {gate.advance(.1f);assertFalse(gate.allow(AudioCue.STRAW))}
        gate.advance(.1f);assertTrue(gate.allow(AudioCue.STRAW))
        assertTrue(gate.allow(AudioCue.LOST))
    }
    @Test fun `master and effects scale elements independently of ambience`() {
        val levels=AudioLevels()
        levels.set(AudioChannel.MASTER,50);levels.set(AudioChannel.EFFECTS,40);levels.set(AudioChannel.FAN,25)
        assertEquals(.05f,levels.gain(AudioChannel.FAN),.0001f)
        assertEquals(.2f,levels.gain(AudioChannel.AUGER),.0001f)
        assertEquals(.5f,levels.gain(AudioChannel.AMBIENT),.0001f)
        levels.set(AudioChannel.EFFECTS,0)
        assertEquals(0f,levels.gain(AudioChannel.FAN),0f)
        assertEquals(.5f,levels.gain(AudioChannel.AMBIENT),.0001f)
        assertEquals(25,levels[AudioChannel.FAN])
        levels.set(AudioChannel.EFFECTS,100);assertEquals(.125f,levels.gain(AudioChannel.FAN),.0001f)
    }
    @Test fun `old mute migrates while saved levels override it and invalid values clamp`() {
        val old=AudioLevels(legacyEffects=false,legacyAmbient=false)
        assertEquals(0,old[AudioChannel.EFFECTS]);assertEquals(0,old[AudioChannel.AMBIENT])
        val loaded=AudioLevels(mapOf(AudioChannel.EFFECTS to 45,AudioChannel.AMBIENT to 20,AudioChannel.FAN to 999),false,false)
        assertEquals(45,loaded[AudioChannel.EFFECTS]);assertEquals(20,loaded[AudioChannel.AMBIENT]);assertEquals(100,loaded[AudioChannel.FAN])
        loaded.set(AudioChannel.MASTER,-100);assertEquals(0f,loaded.gain(AudioChannel.METAL),0f)
        for(c in AudioCue.entries) assertTrue(AudioLevels.channel(c) !in listOf(AudioChannel.MASTER,AudioChannel.AMBIENT))
    }
    @Test fun `all sliders and Back remain separate accessible targets in both orientations`() {
        for((w,h) in listOf(400f to 820f,864f to 400f,650f to 324f)) {
            val l=AudioSettingsLayout();l.layout(w,h,w>h)
            for(i in l.rows.indices) {
                val row=l.rows[i];val t=l.tracks[i]
                assertTrue(l.box.contains(row));assertFalse(row.overlaps(l.back))
                assertFalse(l.icons[i].overlaps(t));assertTrue(t.width>100)
                assertEquals(0,l.percent(i,t.x-40));assertEquals(100,l.percent(i,t.x+t.width+40))
                assertEquals(50,l.percent(i,t.x+t.width/2))
                for(j in i+1 until l.rows.size) assertFalse(row.overlaps(l.rows[j]))
            }
        }
    }
    @Test fun `mass delivery aggregates but loss remains audible when budget exhausted`() {
        val gate=AudioGate()
        assertTrue(gate.allow(AudioCue.STRAW))
        repeat(100) {assertFalse(gate.allow(AudioCue.STRAW))}
        for(c in listOf(AudioCue.CLICK,AudioCue.CREDIT,AudioCue.GRIP,AudioCue.PLACE,AudioCue.RELEASE)) assertTrue(gate.allow(c))
        assertFalse(gate.allow(AudioCue.PURCHASE))
        assertTrue(gate.allow(AudioCue.LOST))
        assertFalse(gate.allow(AudioCue.LOST))
        repeat(4) {gate.advance(.1f)}
        assertTrue(gate.allow(AudioCue.PURCHASE))
    }
    @Test fun `invalid and stalled frame times do not release a cue burst`() {
        val gate=AudioGate();assertTrue(gate.allow(AudioCue.RESULT))
        gate.advance(Float.NaN);gate.advance(-1f);gate.advance(999f)
        assertFalse(gate.allow(AudioCue.RESULT))
        repeat(8) {gate.advance(.1f)}
        assertTrue(gate.allow(AudioCue.RESULT))
    }
    @Test fun `all shipped sounds are mono valid nonempty PCM with headroom and bounded memory`() {
        var bytesTotal=0
        val names=AudioCue.entries.map {it.file}+listOf("auger","fan","rustle","carry","ambient")
        for(name in names) {
            val bytes=javaClass.getResourceAsStream("/audio/$name.wav")!!.use {it.readBytes()};bytesTotal+=bytes.size
            val b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals("RIFF",String(bytes,0,4));assertEquals("WAVE",String(bytes,8,4))
            assertEquals(1,b.getShort(20).toInt());assertEquals(1,b.getShort(22).toInt())
            assertEquals(22050,b.getInt(24));assertEquals(16,b.getShort(34).toInt())
            assertTrue(name,bytes.size>1000)
            if(name!="ambient") assertTrue("Android SoundPool PCM budget: $name",bytes.size<1_000_000)
            var peak=0
            for(i in 44 until bytes.size step 2) peak=maxOf(peak,kotlin.math.abs(b.getShort(i).toInt()))
            assertTrue("Non silent: $name",peak>500);assertTrue("Headroom: $name",peak<25000)
            if(name in listOf("auger","fan","rustle","carry","ambient"))
                assertTrue("Loop seam: $name",kotlin.math.abs(b.getShort(44)-b.getShort(bytes.size-2))<300)
        }
        assertTrue("Entire audio palette under 2MB PCM",bytesTotal<2_000_000)
    }
}

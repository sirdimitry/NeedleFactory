package com.needlefactory.presentation

import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.Test

class NeedleVisibilityTest {
    private fun straw(length: Float, bend: Float = 0f) = FactoryItem(0,Material.STRAW,length,.1f,bend,0,0f,0f,0f,
        listOf(StrawStrand(0f,0f,0f,length,.1f,bend,0)))

    @Test fun `strict sixty percent cutoff never lights a covered needle`() {
        for(v in listOf(0f,.59f,.60f,Float.NaN)) assertEquals(0f,NeedleVisibility.highlightGain(v),0f)
        assertTrue(NeedleVisibility.highlightGain(.60001f)>0f)
        assertTrue(NeedleVisibility.highlightGain(19f/31)<.03f)
        assertEquals(.32f,NeedleVisibility.highlightGain(.75f),.00001f)
    }
    @Test fun `late payoff grows continuously through seventy five ninety and full exposure`() {
        assertEquals(.70f,NeedleVisibility.highlightGain(.90f),.00001f)
        assertEquals(1f,NeedleVisibility.highlightGain(1f),.00001f)
        var previous=0f
        for (step in 0..1000) {
            val gain=NeedleVisibility.highlightGain(step/1000f)
            assertTrue(gain in previous..1f)
            previous=gain
        }
        for (boundary in listOf(.60f,.75f,.90f)) {
            assertEquals(NeedleVisibility.highlightGain(boundary-.0001f),
                NeedleVisibility.highlightGain(boundary+.0001f),.00001f)
        }
    }
    @Test fun `rendered curve projection and pose determine coverage instead of collision bounds`() {
        val cover=NeedleVisibility(1)
        assertEquals(1f,cover.fraction(0f,0f,1f,0f),0f)
        cover.add(straw(2f),0f,0f,0f,1f,1f)
        assertEquals(0f,cover.fraction(0f,0f,1f,0f),0f)
        cover.begin(); cover.add(straw(2f,.2f),0f,0f,0f,1f,1f)
        assertEquals(1f,cover.fraction(0f,0f,.5f,0f),0f)
        assertEquals(0f,cover.fraction(0f,.2f,.5f,0f),0f)
        cover.begin(); cover.add(straw(2f),4f,7f,kotlin.math.PI.toFloat()/2,.5f,1f)
        assertEquals(0f,cover.fraction(4f,7f,.5f,kotlin.math.PI.toFloat()/2),0f)
        assertEquals(1f,cover.fraction(0f,0f,.5f,0f),0f)
    }
    @Test fun `moving rendered cover recovers eligibility and a return removes it immediately`() {
        // Keep strand endpoints between axis samples, avoiding an ambiguous zero-width tip.
        val cover=NeedleVisibility(1); val f=straw(.44f)
        cover.add(f,-.3f,0f,0f,1f,1f)
        val partial=cover.fraction(0f,0f,1f,0f)
        assertTrue(partial in .5f.. .6f)
        assertEquals(0f,NeedleVisibility.highlightGain(partial),0f)
        cover.begin(); cover.add(f,-.45f,0f,0f,1f,1f)
        val exposed=cover.fraction(0f,0f,1f,0f)
        assertTrue(exposed>.6f)
        assertTrue(NeedleVisibility.highlightGain(exposed)>0f)
        cover.begin(); cover.add(f,-.3f,0f,0f,1f,1f)
        assertEquals(0f,NeedleVisibility.highlightGain(cover.fraction(0f,0f,1f,0f)),0f)
    }
}

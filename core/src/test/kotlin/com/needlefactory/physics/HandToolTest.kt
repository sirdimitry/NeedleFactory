package com.needlefactory.physics

import com.badlogic.gdx.utils.GdxNativesLoader
import com.needlefactory.content.FactoryConfig
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import kotlin.math.*

class HandToolTest {
    companion object { @JvmStatic @BeforeClass fun natives() { GdxNativesLoader.load() } }
    private fun arranged():FactorySession {
        val s=FactorySession(FactoryConfig.grabProducts(),42)
        // Three nearby stalks, all other cargo is reachable but outside this head.
        for(item in s.batch.items) {
            item.x=if(item.id<3) 3f+item.id*.1f else 7f
            item.y=if(item.id<3) 4f else 6f; item.angle=0f; item.snapshot()
        }
        return s
    }
    private fun tick(s:FactorySession,p:FactoryIntakePhysics,n:Int) { repeat(n) { s.step(); p.step() } }
    private fun begin(s:FactorySession,p:FactoryIntakePhysics) {
        val item=s.batch.items[2]; val f=item.strands.first()
        val a=item.angle+f.angle
        p.begin(item.x+f.x-sin(a)*f.bend,item.y+f.y+cos(a)*f.bend,.28f)
    }
    @Test fun `bought head grips only nearby stalks up to one two or three and release cannot feed`() {
        for(cap in 1..3) {
            val s=arranged(); s.setHandTool(cap,if(cap==1) 0f else .95f)
            FactoryIntakePhysics(s).use { p ->
                begin(s,p); assertEquals(cap,p.heldCount)
                p.target(5f,4f); tick(s,p,60)
                assertEquals(cap,s.batch.items.count { it.material==Material.STRAW && it.manuallyPushed })
                p.end(); assertEquals(0,p.heldCount); val intake=s.batch.count(Route.INTAKE)
                tick(s,p,600); assertEquals(intake,s.batch.count(Route.INTAKE))
                assertEquals(0,s.collectedStraw); assertFalse(s.batch.items.any { it.manuallyPushed })
            }
        }
    }
    @Test fun `receiver meters each held object across its own physical lip without releasing remaining grips`() {
        val s=arranged(); s.setHandTool(3,.95f)
        FactoryIntakePhysics(s).use { p ->
            begin(s,p); assertEquals(3,p.heldCount)
            // Real gradual contact trajectory; no body teleport or route injection.
            for(k in 0..200) { p.target(3.2f+6.84f*k/200,4f); tick(s,p,1) }
            tick(s,p,240); p.end(); tick(s,p,240)
            assertEquals(3,s.looseStraw); assertEquals(24,s.batch.count(Route.INTAKE))
            assertEquals(3,s.batch.needles.size); assertEquals(0,s.missed)
            tick(s,p,600); assertEquals(3,s.looseStraw)
        }
    }
    @Test fun `empty touch and recreation cannot reactivate a bought tool`() {
        val s=arranged(); s.setHandTool(3,.95f)
        FactoryIntakePhysics(s).use { p -> p.begin(.1f,.1f); assertEquals(0,p.heldCount); tick(s,p,120) }
        FactoryIntakePhysics(s).use { p -> tick(s,p,600); assertEquals(0,p.heldCount); assertEquals(27,s.batch.count(Route.INTAKE)) }
    }
}

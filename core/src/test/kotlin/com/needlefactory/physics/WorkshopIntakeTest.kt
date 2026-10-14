package com.needlefactory.physics

import com.needlefactory.content.FactoryConfig
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.BeforeClass
import com.badlogic.gdx.utils.GdxNativesLoader

class WorkshopIntakeTest {
    companion object { @JvmStatic @BeforeClass fun natives() { GdxNativesLoader.load() } }
    private fun sweep(s:FactorySession,p:FactoryIntakePhysics,pass:Int) {
        val y=.65f+(pass%7)*1.1f
        p.begin(.65f,y)
        repeat(100) { i -> p.target(.65f+9.35f*i/99f,y); s.step(); p.step() }
        p.end(); repeat(20) { s.step(); p.step() }
    }
    @Test fun `rake and guides remain manual and can drain three needles without loss`() {
        for(guides in listOf(false,true)) for(seed in listOf(1L,42L,999L)) {
            val s=FactorySession(FactoryConfig(needleCount=3),seed); s.setTools(true,guides)
            FactoryIntakePhysics(s).use { p ->
                assertEquals(if(guides) 326 else 324,p.bodyCount)
                repeat(600) { s.step(); p.step() }; assertEquals(323,s.batch.count(Route.INTAKE))
                assertEquals(0,s.packaged); assertFalse(s.batch.items.any { it.manuallyPushed })
                var pass=0
                while(!s.batch.complete && pass<100) sweep(s,p,pass++)
                assertTrue("seed=$seed guides=$guides ${s.summary()}",s.batch.complete)
                assertEquals(3,s.packaged); assertEquals(320,s.batch.count(Route.RESIDUE))
                assertEquals(if(guides) 3 else 1,p.bodyCount)
            }
        }
    }
    @Test fun `rake moves a wider local contact band than finger without numeric reward multiplier`() {
        fun moved(rake:Boolean):Int {
            val s=FactorySession(FactoryConfig(needleCount=3),42); s.setTools(rake,false)
            s.toggle(MachineId.VIBRATION)
            val start=s.batch.items.map { it.x to it.y }
            FactoryIntakePhysics(s).use { p -> sweep(s,p,3) }
            return s.batch.items.count { kotlin.math.hypot(it.x-start[it.id].first,it.y-start[it.id].second)>.1f }
        }
        assertTrue(moved(true)>moved(false))
    }
    @Test fun `released rake never keeps feeding a stationary armed mass`() {
        for(guides in listOf(false,true)) for(seed in listOf(1L,42L,999L)) {
            val s=FactorySession(FactoryConfig(needleCount=3),seed); s.setTools(true,guides)
            FactoryIntakePhysics(s).use { p ->
                p.begin(3f,4f)
                repeat(90) { i -> p.target(3f+6.35f*i/89f,4f); s.step(); p.step() }
                p.end()
                val intake=s.batch.count(Route.INTAKE)
                repeat(600) { s.step(); p.step() }
                assertEquals("release must not authorize idle intake: seed=$seed guides=$guides",intake,s.batch.count(Route.INTAKE))
                assertFalse(s.batch.items.any { it.manuallyPushed })
                assertTrue(s.batch.items.filter { it.route==Route.INTAKE }.all { kotlin.math.hypot(it.vx,it.vy)<.02f })
            }
        }
    }
    @Test fun `opening a blocked mouth after release cannot resume a stale rake push`() {
        val s=FactorySession(FactoryConfig(needleCount=3),42); s.setTools(true,true)
        s.toggle(MachineId.VIBRATION)
        FactoryIntakePhysics(s).use { p ->
            repeat(7) { sweep(s,p,it) }
            assertEquals(323,s.batch.count(Route.INTAKE))
            s.toggle(MachineId.VIBRATION)
            repeat(600) { s.step(); p.step() }
            assertEquals(323,s.batch.count(Route.INTAKE))
            assertEquals(0,s.recovered)
            assertFalse(s.batch.items.any { it.manuallyPushed })
        }
    }
}

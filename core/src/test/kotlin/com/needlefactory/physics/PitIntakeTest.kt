package com.needlefactory.physics

import com.badlogic.gdx.utils.GdxNativesLoader
import com.needlefactory.content.FactoryConfig
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import kotlin.math.*

class PitIntakeTest {
    companion object { @JvmStatic @BeforeClass fun natives() { GdxNativesLoader.load() } }
    @Test fun `dense source has four visual layers but bounded physical clumps and three hidden needles`() {
        val old=FactoryGenerator.generate(42,FactoryConfig.grabProducts())
        val s=FactorySession(FactoryConfig.pitProducts(),42)
        val before=old.items.sumOf { it.strands.size }; val after=s.batch.items.sumOf { it.strands.size }
        assertTrue(after>=before*3 && after<=before*5)
        assertEquals(27,s.batch.items.size); assertEquals(3,s.batch.needles.size)
        assertTrue(s.batch.items.filter { it.material==Material.STRAW }.all { it.collisionStrandCount in 3..5 && it.strands.size>=10 })
        assertTrue(s.batch.needles.all { NeedleRecovery.fraction(s.batch,it)<=.6f })
        FactoryIntakePhysics(s).use { p ->
            assertTrue(p.bodyCount<=29); repeat(900) { s.step(); p.step() }
            assertEquals(27,s.batch.count(Route.INTAKE)); assertEquals(0,s.collectedStraw)
        }
    }
    private fun drag(s:FactorySession,p:FactoryIntakePhysics,item:FactoryItem,x:Float,y:Float) {
        val f=item.strands.first(); val a=item.angle+f.angle
        val sx=item.x+cos(item.angle)*f.x-sin(item.angle)*f.y-sin(a)*f.bend
        val sy=item.y+sin(item.angle)*f.x+cos(item.angle)*f.y+cos(a)*f.bend
        p.begin(sx,sy)
        repeat(140) { i -> val t=(i/100f).coerceAtMost(1f); p.target(sx+(x-sx)*t,sy+(y-sy)*t); s.step(); p.step() }
        p.end()
    }
    @Test fun `right field edge is not an intake and release never starts pit admission`() {
        val s=FactorySession(FactoryConfig.pitProducts(),42)
        FactoryIntakePhysics(s).use { p ->
            drag(s,p,s.batch.items[23],10f,5f)
            repeat(600) { s.step(); p.step() }; assertEquals(27,s.batch.count(Route.INTAKE))
            assertFalse(s.batch.items.any { it.manuallyPushed })
        }
    }
    @Test fun `real local drag into distant floor hole admits straw without free sorting`() {
        val s=FactorySession(FactoryConfig.pitProducts(),42)
        FactoryIntakePhysics(s).use { p ->
            repeat(8) {
                val item=s.batch.items.lastOrNull { it.material==Material.STRAW && it.route==Route.INTAKE } ?: return@repeat
                drag(s,p,item,FactoryConfig.PIT_X,FactoryConfig.PIT_Y)
            }
            repeat(240) { s.step(); p.step() }; assertTrue(s.looseStraw>0)
            val before=s.batch.count(Route.INTAKE); repeat(600) { s.step(); p.step() }
            assertEquals(before,s.batch.count(Route.INTAKE)); assertEquals(0,s.recovered)
        }
    }
    @Test fun `admitted material emits distinct short impact events and pause holds their ages`() {
        val s=FactorySession(FactoryConfig.pitProducts(),42)
        assertTrue(s.strawImpactAge>1 && s.metalImpactAge>1)
        s.manualPush(0); assertTrue(s.feed(0)); assertEquals(0f,s.strawImpactAge,0f)
        assertTrue(s.metalImpactAge>1)
        s.manualPush(24); assertTrue(s.feed(24)); assertEquals(0f,s.metalImpactAge,0f)
        s.step(); val age=s.metalImpactAge
        s.toggleRunning(); repeat(60) { s.step() }; assertEquals(age,s.metalImpactAge,0f)
        s.toggleRunning(); repeat(90) { s.step() }; assertTrue(s.metalImpactAge>1.05f)
    }
    @Test fun `release commits locally held clumps over hole despite sequential gate and leaves no idle force`() {
        val s=FactorySession(FactoryConfig.pitProducts().copy(feedInterval=100f),42)
        s.setHandTool(2,.65f,1.2f)
        for(i in 0..1) { s.batch.items[i].x=FactoryConfig.PIT_X;s.batch.items[i].y=-9f;s.batch.items[i].angle=0f }
        FactoryIntakePhysics(s).use { p ->
            drag(s,p,s.batch.items[1],FactoryConfig.PIT_X,FactoryConfig.PIT_Y)
            repeat(200) { s.step();p.step() }
            assertTrue(s.looseStraw>0)
            val remaining=s.batch.count(Route.INTAKE)
            repeat(600) { s.step();p.step() };assertEquals(remaining,s.batch.count(Route.INTAKE));assertFalse(p.pushing)
        }
    }
    @Test fun `width purchase accepts real held material outside original hole but no idle cargo`() {
        val s=FactorySession(FactoryConfig.pitProducts(),42)
        s.setReceiverLevels(0,0,1)
        FactoryIntakePhysics(s).use {p->
            val item=s.batch.items[23]
            drag(s,p,item,2.8f,FactoryConfig.PIT_Y)
            repeat(200) {s.step();p.step()}
            assertTrue(s.looseStraw>0)
            val left=s.batch.count(Route.INTAKE);repeat(600) {s.step();p.step()}
            assertEquals(left,s.batch.count(Route.INTAKE))
        }
    }

}

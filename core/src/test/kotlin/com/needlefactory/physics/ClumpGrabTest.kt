package com.needlefactory.physics

import com.badlogic.gdx.utils.GdxNativesLoader
import com.needlefactory.content.*
import com.needlefactory.economy.*
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import kotlin.math.*

class ClumpGrabTest {
    companion object { @JvmStatic @BeforeClass fun natives() { GdxNativesLoader.load() } }
    private fun tick(s:FactorySession,p:FactoryIntakePhysics,n:Int) { repeat(n) { s.step(); p.step() } }
    private fun acquire(s:FactorySession,p:FactoryIntakePhysics):Int {
        for(item in s.batch.items.asReversed()) if(item.material==Material.STRAW && item.route==Route.INTAKE) {
            val f=item.strands.first(); val a=item.angle+f.angle
            val x=item.x+cos(item.angle)*f.x-sin(item.angle)*f.y-sin(a)*f.bend
            val y=item.y+sin(item.angle)*f.x+cos(item.angle)*f.y+cos(a)*f.bend
            p.begin(x,y)
            if(p.grabbedId>=0) return p.grabbedId
        }
        return -1
    }
    @Test fun `ten families generate deterministic small clumps with three buried independent needles`() {
        assertEquals(10,StrawClumps.templates.size)
        for(seed in 1L..50L) {
            val a=FactoryGenerator.generate(seed,FactoryConfig.grabProducts())
            val b=FactoryGenerator.generate(seed,FactoryConfig.grabProducts())
            assertEquals(27,a.items.size); assertEquals(3,a.needles.size)
            for(i in a.items.indices) {
                assertEquals(a.items[i].x,b.items[i].x,0f)
                assertEquals(a.items[i].angle,b.items[i].angle,0f)
                assertEquals(a.items[i].strands,b.items[i].strands)
                if(i<24) assertTrue(a.items[i].strands.size in 3..5)
            }
            for(n in a.needles) {
                assertTrue("visible spawn seed=$seed",NeedleRecovery.fraction(a,n)<=.6f)
                assertFalse(NeedleRecovery.canPick(a,n,n.x,n.y))
                assertEquals(1.3f,n.length,0f)
            }
        }
    }
    @Test fun `empty space cannot grab the nearest distant clump and overlap picks top visible stem`() {
        val s=FactorySession(FactoryConfig.grabProducts(),42)
        assertNull(StrawGrab.pick(s.batch,9f,4f,.28f))
        val a=s.batch.items[0]; val b=s.batch.items[1]
        b.x=a.x; b.y=a.y; b.angle=a.angle
        val batch=FactoryBatch(1,listOf(a,b))
        val f=b.strands.first(); val angle=b.angle+f.angle
        val x=b.x-sin(angle)*f.bend; val y=b.y+cos(angle)*f.bend
        assertEquals(b.id,StrawGrab.pick(batch,x,y,.28f)!!.id)
    }
    @Test fun `grab caps permission at one and release rehydration cannot feed idle cargo`() {
        for(rake in listOf(false,true)) {
            val s=FactorySession(FactoryConfig.grabProducts(),42); s.setTools(rake,true)
            FactoryIntakePhysics(s).use { p ->
                tick(s,p,600); assertEquals(27,s.batch.count(Route.INTAKE))
                val id=acquire(s,p); assertTrue(id>=0)
                p.target(7f,4f); tick(s,p,70)
                assertEquals(id,p.grabbedId)
                assertEquals(1,s.batch.items.count { it.manuallyPushed && it.material==Material.STRAW })
                assertEquals(0,s.collectedStraw)
                p.end(); val intake=s.batch.count(Route.INTAKE)
                tick(s,p,600); assertEquals(intake,s.batch.count(Route.INTAKE))
                assertFalse(s.batch.items.any { it.manuallyPushed })
            }
            FactoryIntakePhysics(s).use { p -> tick(s,p,600); assertEquals(27,s.batch.count(Route.INTAKE)) }
        }
    }
    @Test fun `fifty seeds can be regrabbed at all edges and manually drained without loss`() {
        for(seed in 1L..50L) {
            val s=FactorySession(FactoryConfig.grabProducts(),seed); s.setTools(true,seed%2L==0L)
            FactoryIntakePhysics(s).use { p ->
                repeat(4) { edge ->
                    assertTrue(acquire(s,p)>=0)
                    p.target(if(edge==0) 0f else 5f,if(edge==1) 0f else if(edge==2) 8f else 4f)
                    tick(s,p,60); p.end(); tick(s,p,30)
                }
                var attempts=0
                while(s.batch.items.any { it.material==Material.STRAW && it.route==Route.INTAKE } && attempts++<40) {
                    assertTrue(acquire(s,p)>=0)
                    p.target(10.04f,4f); tick(s,p,240); p.end()
                }
                tick(s,p,100)
                assertEquals("seed=$seed ${s.summary()}",24,s.looseStraw)
                val left=s.batch.needles.filter { it.route==Route.INTAKE }
                for(n in left) {
                    assertTrue(NeedleRecovery.fraction(s.batch,n)>.6f)
                    assertFalse(s.recoverNeedle(n.x,n.y))
                    p.begin(n.x,n.y); assertEquals(n.id,p.grabbedId)
                    p.target(s.config.metalDockX,-.85f); tick(s,p,240); p.end()
                }
                tick(s,p,100)
                assertTrue(s.batch.complete); assertEquals(3,s.batch.count(Route.RECOVERED)+s.batch.count(Route.MISSED))
                val w=Workshop(WorkshopProfile(activeSeed=seed),EconomyConfig.products())
                val earned=6L+3L*s.batch.count(Route.RECOVERED)
                assertEquals(earned,w.settle(s.batch).earned)
                assertEquals(earned,w.settle(s.batch).earned); assertEquals(earned,w.profile.workshopCredits)
            }
        }
    }
    @Test fun `clump count preserves full product value and early finish ignores uncollected cargo`() {
        for(count in listOf(24,32)) {
            val s=FactorySession(FactoryConfig.grabProducts().copy(strawCount=count),42); s.setBaler(true)
            FactoryIntakePhysics(s).use { p ->
                assertTrue(acquire(s,p)>=0); p.target(10.04f,4f); tick(s,p,240); p.end()
                tick(s,p,150)
                assertEquals(1,s.bundledStraw)
                val w=Workshop(WorkshopProfile(activeSeed=42),EconomyConfig.products())
                assertEquals(16L/count,w.quote(s.batch).material)
                assertTrue(s.finish()); tick(s,p,150)
                val r=w.settle(s.batch)
                assertEquals(count+3-r.processed-r.secured-r.missed,r.discarded); assertEquals(16L/count,r.earned)
            }
        }
    }
}

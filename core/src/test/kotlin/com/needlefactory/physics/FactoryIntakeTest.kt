package com.needlefactory.physics

import com.badlogic.gdx.utils.GdxNativesLoader
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import kotlin.math.*

class FactoryIntakeTest {
    companion object { @JvmStatic @BeforeClass fun natives() { GdxNativesLoader.load() } }
    private fun coveredFraction(s:FactorySession):Float {
        val n=s.batch.needle
        var covered=0
        for(k in 0..30) {
            val along=(k/30f-.5f)*n.length
            val px=n.x+cos(n.angle)*along; val py=n.y+sin(n.angle)*along
            val hidden=s.batch.items.dropLast(1).any { item ->
                val dx=px-item.x; val dy=py-item.y
                val localX=cos(item.angle)*dx+sin(item.angle)*dy
                val localY= -sin(item.angle)*dx+cos(item.angle)*dy
                item.strands.any { f ->
                    val x=cos(f.angle)*(localX-f.x)+sin(f.angle)*(localY-f.y)
                    val y= -sin(f.angle)*(localX-f.x)+cos(f.angle)*(localY-f.y)
                    val t=x/f.length
                    val slope= -8*f.bend*t/f.length
                    abs(t)<=.5f && abs(y-f.bend*(1-4*t*t))/sqrt(1+slope*slope)<=f.width/2
                }
            }
            if(hidden) covered++
        }
        return covered/31f
    }
    @Test fun `dense crossed straw does not reveal the needle after one broad swipe`() {
        for(seed in listOf(1L,42L,123L,999L,Long.MIN_VALUE)) {
            val s=FactorySession(firstSeed=seed)
            assertEquals(1f,coveredFraction(s),0f)
            s.toggle(MachineId.VIBRATION)
            FactoryIntakePhysics(s).use { p ->
                p.begin(.5f,4f)
                repeat(60) { i -> p.target(.5f+i*9f/59f,4f); s.step(); p.step() }
                p.end(); repeat(120) { s.step(); p.step() }
                val covered=coveredFraction(s)
                assertTrue("single swipe revealed seed=$seed covered=$covered",covered>=.5f)
                assertEquals(Route.INTAKE,s.batch.needle.route)
            }
        }
    }
    private fun sweep(s:FactorySession,p:FactoryIntakePhysics,pass:Int) {
        val y=.65f+(pass%7)*1.1f
        p.begin(.65f,y)
        repeat(100) { i -> p.target(.65f+8.7f*i/99f,y); s.step(); p.step() }
        p.end(); repeat(20) { s.step(); p.step() }
    }
    private fun drain(s:FactorySession,p:FactoryIntakePhysics) {
        var pass=0
        while(!s.batch.complete && pass<100) sweep(s,p,pass++)
        assertTrue("undrained seed=${s.batch.seed} passes=$pass ${s.summary()}",s.batch.complete)
    }
    @Test fun `idle manual intake remains unchanged and cannot package anything`() {
        for(seed in listOf(1L,42L,123L,999L,Long.MIN_VALUE)) {
            val s=FactorySession(firstSeed=seed)
            val poses=s.batch.items.map { Triple(it.x,it.y,it.angle) }
            FactoryIntakePhysics(s).use { p ->
                repeat(3600) { s.step(); p.step() }
                assertEquals(321,s.batch.count(Route.INTAKE)); assertEquals(0,s.recovered); assertEquals(0,s.packaged)
                assertFalse(s.batch.items.any { it.manuallyPushed })
                s.batch.items.forEachIndexed { i,item ->
                    assertEquals("idle x seed=$seed item=$i",poses[i].first,item.x,.00001f)
                    assertEquals("idle y seed=$seed item=$i",poses[i].second,item.y,.00001f)
                    assertEquals("idle angle seed=$seed item=$i",poses[i].third,item.angle,.00001f)
                }
                assertEquals(322,p.bodyCount)
            }
        }
    }
    @Test fun `repeated physical manual pushes drain reproducible bounded batches`() {
        for(seed in listOf(1L,42L,123L,999L,Long.MIN_VALUE)) {
            val s=FactorySession(firstSeed=seed)
            FactoryIntakePhysics(s).use { p ->
                assertEquals(322,p.bodyCount)
                sweep(s,p,3); assertEquals(0,s.recovered); assertEquals(0,s.packaged)
                drain(s,p)
                assertEquals(1,p.bodyCount); assertEquals(1,s.packaged); assertEquals(320,s.batch.count(Route.RESIDUE))
            }
        }
    }
    @Test fun `native adapter rehydration preserves active poses routes and counters`() {
        val s=FactorySession(firstSeed=42)
        FactoryIntakePhysics(s).use { p -> repeat(3) { sweep(s,p,it) } }
        val poses=s.batch.items.map { Triple(it.x,it.y,it.route) }; val summary=s.summary()
        FactoryIntakePhysics(s).use { p ->
            assertEquals(s.batch.count(Route.INTAKE)+1,p.bodyCount)
            assertEquals(summary,s.summary()); assertEquals(poses,s.batch.items.map { Triple(it.x,it.y,it.route) })
            drain(s,p); assertEquals(1,s.packaged)
        }
    }
    @Test fun `hybrid cadence shares identical native and transport results`() {
        val results=listOf(30,60,120).map { hz ->
            val s=FactorySession(firstSeed=999); val clock=FixedStep(1f/60,5,.1f)
            FactoryIntakePhysics(s).use { p ->
                var pass=0; var tick=0
                repeat(hz*200) {
                    clock.advance(1f/hz) {
                        val i=tick%120; val y=.65f+(pass%7)*1.1f
                        if(i==0) p.begin(.65f,y)
                        if(i<100) p.target(.65f+8.7f*i/99f,y)
                        if(i==100) { p.end(); pass++ }
                        s.step(); p.step(); tick++
                    }
                }
                assertTrue(s.batch.complete)
                s.summary()+s.batch.items.joinToString { "${it.x},${it.y},${it.angle}" }
            }
        }
        assertEquals(results[0],results[1]); assertEquals(results[0],results[2])
    }
    @Test fun `manual slow and fast assists remain finite and cannot discard items`() {
        val s=FactorySession(firstSeed=123)
        FactoryIntakePhysics(s).use { p ->
            s.toggle(MachineId.VIBRATION)
            val first=s.batch.items[0]; val before=first.x
            p.begin(first.x-.8f,first.y)
            repeat(180) { i -> p.target(before-.8f+i*.018f,first.y); s.step(); p.step() }
            assertTrue(first.x>before+.1f)
            repeat(300) { i -> p.target(if(i%60<30) 0f else 10f, if(i%90<45) 0f else 8f); s.step(); p.step() }
            p.end(); assertEquals(321,s.batch.count(Route.INTAKE))
            assertTrue(s.batch.items.all { it.x.isFinite() && it.y.isFinite() && it.x in 0f..10f && it.y in 0f..8f })
            s.toggle(MachineId.VIBRATION); drain(s,p)
        }
    }
}

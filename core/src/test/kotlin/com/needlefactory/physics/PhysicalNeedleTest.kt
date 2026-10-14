package com.needlefactory.physics

import com.badlogic.gdx.utils.GdxNativesLoader
import com.needlefactory.content.*
import com.needlefactory.economy.*
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import kotlin.math.*

class PhysicalNeedleTest {
    companion object { @JvmStatic @BeforeClass fun natives() { GdxNativesLoader.load() } }
    private fun tick(s:FactorySession,p:FactoryIntakePhysics,n:Int) { repeat(n) { s.step(); p.step() } }
    private fun isolated():FactorySession {
        val s=FactorySession(FactoryConfig.grabProducts(),42)
        s.batch.items.filter { it.material==Material.STRAW }.forEach { it.x=7f; it.y=6f; it.angle=0f; it.snapshot() }
        s.batch.needles.forEachIndexed { i,n -> n.x=2.6f+i*1.6f; n.y=3f; n.angle=0f; n.snapshot() }
        return s
    }
    @Test fun `tap exposes grip but cannot award a needle and physical sorting awards once`() {
        val s=isolated(); val n=s.batch.needle
        assertFalse(s.recoverNeedle(n.x,n.y))
        FactoryIntakePhysics(s).use { p ->
            p.begin(n.x,n.y); assertEquals(n.id,p.grabbedId); p.end(); tick(s,p,60)
            assertEquals(Route.INTAKE,n.route); assertEquals(0,s.recovered)
            p.begin(n.x,n.y); p.target(s.config.metalDockX,-.85f); tick(s,p,240); p.end()
            assertEquals(Route.RECOVERED,n.route); assertEquals(1,s.recovered)
            tick(s,p,300); assertEquals(1,s.recovered)
            assertTrue(s.finish()); tick(s,p,150)
            val w=Workshop(WorkshopProfile(activeSeed=42),EconomyConfig.products())
            assertEquals(3L,w.settle(s.batch).earned); assertEquals(3L,w.settle(s.batch).earned)
        }
    }
    @Test fun `unnoticed buried metal moves with a local clump through receiving and is missed without magnet`() {
        val s=isolated(); val cover=s.batch.items[23]; cover.x=5f; cover.y=4f; cover.angle=0f; cover.snapshot()
        val n=s.batch.needle; n.x=cover.x; n.y=cover.y+cover.strands.first().bend; n.angle=0f; n.snapshot()
        assertTrue(NeedleRecovery.fraction(s.batch,n)<=.6f)
        FactoryIntakePhysics(s).use { p ->
            p.begin(cover.x,cover.y); assertEquals(23,p.grabbedId)
            var seenShared=false
            repeat(480) { i ->
                p.target(5f+5.04f*(i/180f).coerceAtMost(1f),4f)
                s.step(); p.step(); if(n.route==Route.RECEIVING || n.route==Route.TRANSFER) {
                    seenShared=true
                    assertEquals("metal must enter with its clump",cover.route,n.route)
                    assertEquals(cover.progress,n.progress,.00001f)
                }
            }
            p.end(); tick(s,p,150)
            assertTrue("no shared receiving route: ${s.summary()}",seenShared)
            assertEquals(Route.MISSED,n.route); assertEquals(1,s.missed); assertEquals(0,s.recovered)
            assertFalse(s.enabled(MachineId.SEPARATOR)); assertFalse(s.enabled(MachineId.CONVEYOR))
            assertEquals(Route.LOOSE,cover.route)
        }
    }
    @Test fun `carried metal and all manual permission stop on release and native recreation`() {
        val s=isolated(); val n=s.batch.needle
        FactoryIntakePhysics(s).use { p ->
            p.begin(n.x,n.y); p.target(8f,4f); tick(s,p,20); p.end()
            tick(s,p,600); assertEquals(Route.INTAKE,n.route); assertEquals(0,s.recovered+s.missed)
            assertFalse(s.batch.items.any { it.manuallyPushed })
        }
        FactoryIntakePhysics(s).use { p -> tick(s,p,600); assertEquals(Route.INTAKE,n.route) }
    }
    @Test fun `acute templates keep separated crossing centres rather than a radial knot`() {
        for(family in StrawClumps.templates) {
            assertTrue(family.size in 3..5)
            assertTrue(family.maxOf { it.angle }-family.minOf { it.angle }<.47f)
            assertTrue(family.map { it.x }.distinct().size>1)
            assertTrue(family.maxOf { it.x }-family.minOf { it.x }>.15f)
        }
    }
}

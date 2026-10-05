package com.needlefactory.simulation

import com.needlefactory.content.FactoryConfig
import org.junit.Assert.*
import org.junit.Test

class FactorySessionTest {
    private fun feedAll(s: FactorySession) { s.batch.items.forEach { s.manualPush(it.id); assertTrue(s.feed(it.id)); assertFalse(s.feed(it.id)) } }
    private fun run(s:FactorySession,n:Int=1200) { repeat(n) { s.step() } }
    @Test fun `seeded batches conserve identity and retain slender needle length`() {
        for(seed in 0L..199L) {
            val a=FactoryGenerator.generate(seed,FactoryConfig()); val b=FactoryGenerator.generate(seed,FactoryConfig())
            assertEquals(321,a.items.size); assertEquals(321,a.items.map { it.id }.toSet().size)
            assertEquals(1,a.items.count { it.material==Material.NEEDLE })
            assertEquals(1.3f,a.needle.length,0f); assertTrue(a.needle.width < .04f)
            a.items.zip(b.items).forEach { (x,y) -> assertEquals(x.x,y.x,0f); assertEquals(x.angle,y.angle,0f) }
            assertTrue(a.items.dropLast(1).all { it.length>=3f && it.width<=.036f && it.strands.size==12 })
            val cover=a.items[160]
            // Central strand alone covers the entire metal silhouette, including its thickest back.
            for(i in 0..30) {
                val x=(i/30f-.5f)*a.needle.length
                val deviation=4*cover.bend*x*x/(cover.length*cover.length)
                assertTrue(deviation+a.needle.width/2 < cover.width/2-.001f)
            }
        }
    }
    @Test fun `normal routing captures only metal packages once and drains all straw`() {
        val s=FactorySession(firstSeed=42); assertFalse(s.next()); feedAll(s); run(s)
        assertTrue(s.batch.complete); assertEquals(320,s.batch.count(Route.RESIDUE))
        assertEquals(1,s.recovered); assertEquals(1,s.packaged); assertEquals(0,s.missed)
        run(s); assertEquals(1,s.packaged)
        assertTrue(s.next()); assertEquals(2,s.batchNumber); assertEquals(1,s.packaged); assertFalse(s.batch.complete)
    }
    @Test fun `separator off routes the needle to missed bin without recovery`() {
        val s=FactorySession(); s.toggle(MachineId.SEPARATOR); feedAll(s); run(s)
        assertTrue(s.batch.complete); assertEquals(Route.MISSED,s.batch.needle.route)
        assertEquals(1,s.missed); assertEquals(0,s.packaged); assertEquals(0,s.recovered)
    }
    @Test fun `individual stops have distinct consequences and global pause preserves all state`() {
        val s=FactorySession(); s.toggle(MachineId.VIBRATION); assertFalse(s.feed(0)); s.toggle(MachineId.VIBRATION)
        s.toggle(MachineId.CONVEYOR); assertFalse(s.feed(0)); s.toggle(MachineId.CONVEYOR); feedAll(s)
        s.toggle(MachineId.PACKER); run(s)
        assertEquals(Route.BUFFER,s.batch.needle.route); assertFalse(s.batch.complete); assertEquals(0,s.packaged)
        val summary=s.summary(); val batch=s.batch
        s.toggleRunning(); run(s); s.synchronizeSnapshots()
        assertSame(batch,s.batch); assertEquals(summary,s.summary())
        s.toggleRunning(); s.toggle(MachineId.PACKER); run(s); assertEquals(1,s.packaged)
    }
    @Test fun `belt stops freeze in flight routes and resume without duplicate output`() {
        val s=FactorySession(); feedAll(s); run(s,100); s.toggle(MachineId.CONVEYOR)
        val p=s.batch.needle.progress; val phase=s.conveyorSeconds
        run(s,120); assertEquals(p,s.batch.needle.progress,0f); assertEquals(phase,s.conveyorSeconds,0f)
        s.toggle(MachineId.CONVEYOR); run(s); assertTrue(s.batch.complete); assertEquals(1,s.packaged)
    }
    @Test fun `render cadence never defines transport outcome`() {
        val results= listOf(30,60,120).map { hz ->
            val s=FactorySession(firstSeed=123); feedAll(s); val clock=FixedStep(1f/60,5,.1f)
            repeat(hz*20) { clock.advance(1f/hz) { s.step() } }
            s.summary()
        }
        assertEquals(results[0],results[1]); assertEquals(results[0],results[2])
    }
    @Test fun `unarmed material cannot feed and manual gate state survives pause`() {
        val s=FactorySession()
        run(s,600)
        assertEquals(321,s.batch.count(Route.INTAKE)); assertFalse(s.feed(0))
        s.manualPush(0)
        val before=s.summary(); val gate=s.feedGateAge
        s.toggleRunning(); run(s); assertEquals(before,s.summary()); assertEquals(gate,s.feedGateAge,0f)
        s.toggleRunning(); assertTrue(s.gateFeed(0)); assertFalse(s.gateFeed(1))
        assertEquals(Route.BELT,s.batch.items[0].route)
    }
}

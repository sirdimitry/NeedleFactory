package com.needlefactory.physics

import com.badlogic.gdx.utils.GdxNativesLoader
import com.needlefactory.content.FactoryConfig
import com.needlefactory.economy.*
import com.needlefactory.save.WorkshopCodec
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

class ProductTransportTest {
    companion object { @JvmStatic @BeforeClass fun natives() { GdxNativesLoader.load() } }
    private val owned=setOf(UpgradeId.RAKE.key,UpgradeId.BALER.key)
    private fun session(magnet:Boolean=false)=FactorySession(FactoryConfig.grabProducts(),42).apply {
        setBaler(true); setTransport(true,magnet)
    }
    private fun ticks(s:FactorySession,n:Int=600) { repeat(n) { s.step() } }
    private fun load(s:FactorySession) {
        s.batch.items.forEach { s.manualPush(it.id); assertTrue(s.feed(it.id)) }
    }
    @Test fun `paid belt and magnet have prerequisites insufficient and duplicate guards`() {
        val w=Workshop(WorkshopProfile(workshopCredits=200),EconomyConfig.products())
        assertEquals(PurchaseResult.LOCKED,w.purchase(UpgradeId.CONVEYOR))
        assertEquals(PurchaseResult.LOCKED,w.purchase(UpgradeId.MAGNET))
        assertEquals(PurchaseResult.BOUGHT,w.purchase(UpgradeId.RAKE))
        assertEquals(PurchaseResult.BOUGHT,w.purchase(UpgradeId.BALER))
        assertEquals(PurchaseResult.BOUGHT,w.purchase(UpgradeId.CONVEYOR))
        assertEquals(PurchaseResult.OWNED,w.purchase(UpgradeId.CONVEYOR))
        assertEquals(PurchaseResult.BOUGHT,w.purchase(UpgradeId.MAGNET))
        assertEquals(10L,w.profile.workshopCredits)
        assertEquals(PurchaseResult.OWNED,w.purchase(UpgradeId.MAGNET))
        val poor=Workshop(WorkshopProfile(workshopCredits=47,purchased=owned),EconomyConfig.products())
        assertEquals(PurchaseResult.INSUFFICIENT,poor.purchase(UpgradeId.CONVEYOR))
        assertEquals(47L,poor.profile.workshopCredits)
    }
    @Test fun `unowned transport cannot toggle or capture and magnet needs belt`() {
        val s=FactorySession(FactoryConfig.grabProducts(),42)
        s.setTransport(true,true); assertFalse(s.conveyorOwned); assertFalse(s.magnetOwned)
        s.toggle(MachineId.CONVEYOR); s.toggle(MachineId.SEPARATOR)
        assertFalse(s.enabled(MachineId.CONVEYOR)); assertFalse(s.enabled(MachineId.SEPARATOR))
    }
    @Test fun `shared belt conserves straw and missed needles without magnet`() {
        val s=session(); load(s); ticks(s)
        assertTrue(s.batch.complete); assertEquals(24,s.bundledStraw)
        assertEquals(3,s.missed); assertEquals(0,s.recovered)
        assertEquals(16L,Workshop(WorkshopProfile(activeSeed=42),EconomyConfig.products()).quote(s.batch).earned)
    }
    @Test fun `owned magnet recovers three separate needles at end of same belt and pays sorted once`() {
        val s=session(true); load(s); ticks(s)
        assertEquals(3,s.batch.count(Route.SORTED)); assertEquals(3,s.recovered); assertEquals(0,s.missed)
        val w=Workshop(WorkshopProfile(activeSeed=42),EconomyConfig.products())
        val r=w.settle(s.batch); assertEquals(3,r.sorted); assertEquals(12L,r.metal); assertEquals(28L,r.earned)
        ticks(s); s.synchronizeSnapshots(); assertEquals(r,w.settle(s.batch)); assertEquals(28L,w.profile.workshopCredits)
        val p=w.profile.copy(purchased=owned+UpgradeId.CONVEYOR.key+UpgradeId.MAGNET.key)
        assertEquals(p,WorkshopCodec.decode(WorkshopCodec.encode(p),42).profile)
    }
    @Test fun `mixed raw and sorted value is a final grade not additive payout`() {
        val s=session(true); val n=s.batch.needle
        s.manualPush(n.id); assertTrue(s.deliverMetal(n.id)); ticks(s,40)
        s.batch.items.filter { it.route==Route.INTAKE }.forEach { s.manualPush(it.id); s.feed(it.id) }
        ticks(s)
        val r=Workshop(WorkshopProfile(activeSeed=42),EconomyConfig.products()).settle(s.batch)
        assertEquals(2,r.sorted); assertEquals(3,r.secured); assertEquals(11L,r.metal); assertEquals(27L,r.earned)
    }
    @Test fun `belt pause and rotation snapshot hold cargo and preserve switch states`() {
        val s=session(true); load(s); ticks(s,45); s.toggle(MachineId.CONVEYOR)
        val poses=s.batch.items.map { it.route to it.progress }
        ticks(s,100); s.synchronizeSnapshots(); s.setTransport(true,true)
        assertEquals(poses,s.batch.items.map { it.route to it.progress })
        assertFalse(s.enabled(MachineId.CONVEYOR))
        s.toggle(MachineId.CONVEYOR); ticks(s); assertEquals(3,s.recovered)
    }
    @Test fun `early finish drains admitted cargo with belt off but does not enable disabled magnet`() {
        val s=session(true)
        val clump=s.batch.items[23]; val n=s.batch.needle
        s.manualPush(clump.id); s.feed(clump.id); s.manualPush(n.id); s.feed(n.id)
        s.toggle(MachineId.CONVEYOR); s.toggle(MachineId.SEPARATOR)
        assertTrue(s.finish()); ticks(s)
        assertTrue(s.batch.complete); assertEquals(1,s.bundledStraw); assertEquals(1,s.missed)
        val q=Workshop(WorkshopProfile(activeSeed=42),EconomyConfig.products()).settle(s.batch)
        assertEquals(0L,q.earned); assertEquals(25,q.discarded); assertEquals(0,q.secured)
    }
    private fun physical(belt:Boolean,magnet:Boolean):FactorySession {
        val s=FactorySession(FactoryConfig.grabProducts(),42); s.setBaler(true); s.setTransport(belt,magnet)
        s.batch.items.filter { it.material==Material.STRAW }.forEach { it.x=3f; it.y=6f; it.angle=0f; it.snapshot() }
        val clump=s.batch.items[23]; clump.x=5f; clump.y=4f; clump.snapshot()
        s.batch.needles.forEachIndexed { i,n -> n.x=2f+i; n.y=2f; n.angle=0f; n.snapshot() }
        s.batch.needle.x=5f; s.batch.needle.y=4f+clump.strands.first().bend; s.batch.needle.snapshot()
        return s
    }
    @Test fun `paid nearer lane removes manual distance but does not move untouched intake`() {
        for(belt in listOf(false,true)) {
            val s=physical(belt,belt)
            FactoryIntakePhysics(s).use { p ->
                repeat(300) { s.step(); p.step() }; assertEquals(27,s.batch.count(Route.INTAKE))
                p.begin(5f,4f); assertEquals(23,p.grabbedId)
                repeat(360) { i -> p.target(5f+3.1f*(i/180f).coerceAtMost(1f),4f); s.step(); p.step() }; p.end()
                repeat(600) { s.step(); p.step() }
                if(belt) { assertEquals(s.summary(),Route.BUNDLED,s.batch.items[23].route); assertEquals(s.summary(),Route.SORTED,s.batch.needle.route) }
                else { assertEquals(s.summary(),Route.INTAKE,s.batch.items[23].route); assertEquals(s.summary(),Route.INTAKE,s.batch.needle.route) }
                val remaining=s.batch.count(Route.INTAKE)
                repeat(600) { s.step(); p.step() }; assertEquals(remaining,s.batch.count(Route.INTAKE))
            }
        }
    }
    @Test fun `contact release before lane never starts mass feed despite owned running equipment`() {
        val s=physical(true,true)
        FactoryIntakePhysics(s).use { p ->
            p.begin(5f,4f); p.target(5.5f,4f); repeat(30) { s.step(); p.step() }; p.end()
            repeat(1200) { s.step(); p.step() }
            assertEquals(27,s.batch.count(Route.INTAKE)); assertFalse(s.batch.items.any { it.manuallyPushed })
        }
    }
}

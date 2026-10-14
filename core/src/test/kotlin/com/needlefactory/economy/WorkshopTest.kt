package com.needlefactory.economy

import com.needlefactory.content.FactoryConfig
import com.needlefactory.save.*
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class WorkshopTest {
    private fun session(seed: Long=1)=FactorySession(FactoryConfig(needleCount=3),seed)
    private fun outcome(secured: Int, processed: Int, missed: Int=0, seed: Long=1): FactorySession {
        val s=session(seed)
        s.batch.items.filter { it.material==Material.STRAW }.take(processed).forEach { it.route=Route.RESIDUE }
        s.batch.needles.take(secured).forEach { it.route=Route.OUTPUT }
        s.batch.needles.drop(secured).take(missed).forEach { it.route=Route.MISSED }
        return s
    }
    @Test fun `three reproducible needles are separate covered objects with unchanged size`() {
        for(seed in 0L..199L) {
            val s=session(seed); val b=session(seed).batch
            assertEquals(323,s.batch.items.size); assertEquals(3,s.batch.needles.size)
            assertEquals(323,s.batch.items.map { it.id }.toSet().size)
            for(n in s.batch.needles) { assertEquals(1.3f,n.length,0f); assertEquals(.018f,n.width,0f) }
            val visibility=com.needlefactory.presentation.NeedleVisibility(320)
            visibility.begin()
            s.batch.items.filter { it.material==Material.STRAW }.forEach { visibility.add(it,it.x*24.8f,it.y*24.8f,it.angle,24.8f,24.8f) }
            for(n in s.batch.needles) assertTrue("initial uncovered seed=$seed id=${n.id}",
                visibility.fraction(n.x*24.8f,n.y*24.8f,n.length*24.8f,n.angle,.1f)<=.6f)
            s.batch.items.zip(b.items).forEach { (a,b) -> assertEquals(a.x,b.x,0f); assertEquals(a.y,b.y,0f) }
            for(a in s.batch.needles) for(b in s.batch.needles) if(a!==b)
                assertTrue(kotlin.math.hypot(a.x-b.x,a.y-b.y)>1.95f)
        }
    }
    @Test fun `burst flow conserves all needles with one packer and one buffer`() {
        val s=session(); s.batch.items.forEach { s.manualPush(it.id); assertTrue(s.feed(it.id)) }
        repeat(1800) {
            s.step(); assertTrue(s.batch.count(Route.PACKING)+s.batch.count(Route.EJECT)<=1)
            assertTrue(s.batch.count(Route.BUFFER)<=1); assertTrue(s.batch.count(Route.CAPTURE)<=1)
            assertTrue(s.batch.count(Route.CHUTE)<=1)
        }
        assertTrue(s.batch.complete); assertEquals(3,s.recovered); assertEquals(3,s.packaged)
        assertEquals(0,s.missed); assertEquals(320,s.batch.count(Route.RESIDUE))
        repeat(500) { s.step() }; assertEquals(3,s.packaged)
    }
    @Test fun `queued burst resumes after packer and belt stops without loss`() {
        val s=session(); s.toggle(MachineId.PACKER)
        s.batch.items.forEach { s.manualPush(it.id); s.feed(it.id) }
        repeat(800) { s.step() }; assertEquals(0,s.packaged); assertFalse(s.batch.complete)
        val before=s.summary(); s.toggleRunning(); repeat(80) { s.step() }; s.synchronizeSnapshots()
        assertEquals(before,s.summary()); s.toggleRunning(); s.toggle(MachineId.PACKER)
        repeat(1500) { s.step() }; assertEquals(3,s.packaged); assertTrue(s.batch.complete)
    }
    @Test fun `pipeline keeps arrival order even when high id arrives first`() {
        val s=session()
        for(i in listOf(2,1,0)) { s.manualPush(s.batch.needles[i].id); s.feed(s.batch.needles[i].id); repeat(90) { s.step() } }
        val order=mutableListOf<Int>()
        repeat(1800) {
            s.step()
            s.batch.needles.filter { it.route==Route.OUTPUT && it.id !in order }.forEach { order.add(it.id) }
        }
        assertEquals(listOf(322,321,320),order)
    }
    @Test fun `first needle needs twenty five percent but all keys waive work gate`() {
        val w=Workshop(); assertFalse(w.eligible(outcome(1,79).batch))
        assertTrue(w.eligible(outcome(1,80).batch)); assertFalse(w.eligible(outcome(0,320).batch))
        assertTrue(w.eligible(outcome(3,0).batch))
        val captured=outcome(0,80); captured.batch.needle.route=Route.CAPTURE
        assertFalse(w.eligible(captured.batch))
    }
    @Test fun `early finish discards only intake drains pending and pays sixteen once`() {
        val s=outcome(1,80); val w=Workshop()
        assertTrue(s.finish()); repeat(600) { s.step() }
        assertTrue(s.batch.complete); assertEquals(16L,w.settle(s.batch).earned)
        assertEquals(242,s.batch.count(Route.DISCARDED)); assertEquals(16L,w.profile.workshopCredits)
        assertEquals(w.profile.receipt,w.settle(s.batch)); assertEquals(16L,w.profile.workshopCredits)
    }
    @Test fun `finish keeps pending metal and runs belt packer with separator setting retained`() {
        val s=outcome(1,80); s.manualPush(s.batch.needles[1].id); s.feed(s.batch.needles[1].id)
        s.toggle(MachineId.CONVEYOR); s.toggle(MachineId.PACKER); s.finish()
        repeat(1400) { s.step() }; assertEquals(2,s.batch.count(Route.OUTPUT))
        assertEquals(31L,Workshop().settle(s.batch).earned)
    }
    @Test fun `normal and perfect reward examples`() {
        assertEquals(49L,Workshop().quote(outcome(2,256).batch).earned)
        val s=outcome(3,320); assertTrue(s.batch.complete)
        val receipt=Workshop().settle(s.batch); assertEquals(90L,receipt.earned); assertTrue(receipt.perfect)
    }
    @Test fun `missed needle keeps service pay but removes perfect bonus`() {
        val r=Workshop().settle(outcome(2,320,1).batch)
        assertEquals(60L,r.earned); assertEquals(1,r.missed); assertEquals(0L,r.bonus)
    }
    @Test fun `zero recovery still pays processed work on normal completion`() {
        assertEquals(30L,Workshop().settle(outcome(0,320,3).batch).earned)
    }
    @Test fun `no reward for a live incomplete batch`() {
        try { Workshop().settle(outcome(1,80).batch); fail() } catch(_: IllegalArgumentException) { }
    }
    @Test fun `purchase debit prerequisites insufficient and duplicate are explicit`() {
        val w=Workshop(); assertEquals(PurchaseResult.INSUFFICIENT,w.purchase(UpgradeId.RAKE))
        assertEquals(PurchaseResult.LOCKED,w.purchase(UpgradeId.GUIDES)); w.settle(outcome(3,320).batch)
        assertEquals(PurchaseResult.BOUGHT,w.purchase(UpgradeId.RAKE)); assertEquals(60L,w.profile.workshopCredits)
        assertEquals(PurchaseResult.OWNED,w.purchase(UpgradeId.RAKE)); assertEquals(60L,w.profile.workshopCredits)
        assertEquals(PurchaseResult.INSUFFICIENT,w.purchase(UpgradeId.GUIDES)); assertTrue(w.next())
        w.settle(outcome(3,320,seed=w.profile.activeSeed).batch); assertEquals(PurchaseResult.BOUGHT,w.purchase(UpgradeId.GUIDES))
        assertEquals(85L,w.profile.workshopCredits)
    }
    @Test fun `receipt survives lifecycle restore and next cannot duplicate settlement`() {
        val s=outcome(3,320); val w=Workshop(); w.settle(s.batch)
        s.toggleRunning(); s.synchronizeSnapshots(); w.settle(s.batch); assertEquals(90L,w.profile.workshopCredits)
        val loaded=Workshop(WorkshopCodec.decode(WorkshopCodec.encode(w.profile),1).profile)
        loaded.settle(session().batch); assertEquals(90L,loaded.profile.workshopCredits)
        assertTrue(loaded.next()); assertFalse(loaded.next()); assertEquals(2L,loaded.profile.activeBatchId)
        try { loaded.settle(s.batch); fail("Stale batch paid twice") } catch(_: IllegalArgumentException) { }
    }
    @Test fun `save codec preserves wallet upgrades seed progression and receipt`() {
        val w=Workshop(WorkshopProfile(activeSeed=42)); w.settle(outcome(3,320,seed=42).batch); w.purchase(UpgradeId.RAKE)
        assertEquals(w.profile,WorkshopCodec.decode(WorkshopCodec.encode(w.profile),999).profile)
    }
    @Test fun `default and v0 migration and unknown fields`() {
        assertEquals(3,WorkshopCodec.decode("{}",42).profile.schemaVersion)
        val p=WorkshopCodec.decode("{schemaVersion:0,credits:95,upgrades:[feed-rake],futureLayout:{x:5}}",42).profile
        assertEquals(95L,p.workshopCredits); assertTrue(UpgradeId.RAKE.key in p.purchased)
        assertEquals(42L,p.activeSeed)
    }
    @Test fun `future schema is read only and invalid balance rejected`() {
        assertTrue(WorkshopCodec.decode("{schemaVersion:4}",42).readOnly)
        assertTrue(WorkshopCodec.decode("{schemaVersion:2,contentVersion:9}",42).readOnly)
        val compatible=WorkshopCodec.decode("{schemaVersion:1,WorkshopCredits:50,purchasedUpgrades:[future-tool]}",42)
        assertEquals(50L,compatible.profile.workshopCredits); assertTrue("future-tool" in compatible.profile.purchased)
        try { WorkshopCodec.decode("{schemaVersion:1,WorkshopCredits:-1}",42); fail() } catch(_: IllegalArgumentException) { }
    }
    @Test fun `atomic file roundtrip and corrupt primary falls back to last valid backup`() {
        val dir=Files.createTempDirectory("needle-save").toFile()
        try {
            val file=java.io.File(dir,"profile.json"); val store=WorkshopStore(file)
            assertEquals(0L,store.load(42).profile.workshopCredits)
            store.write(WorkshopProfile(workshopCredits=90,activeSeed=42))
            store.write(WorkshopProfile(workshopCredits=60,purchased=setOf("feed-rake"),activeSeed=42))
            assertEquals(60L,store.load(42).profile.workshopCredits)
            file.writeText("{broken"); val recovered=store.load(42)
            assertEquals(90L,recovered.profile.workshopCredits); assertNotNull(recovered.warning)
            java.io.File(file.path+".bak").writeText("bad")
            val fallback=store.load(42); assertEquals(0L,fallback.profile.workshopCredits); assertNotNull(fallback.warning)
            file.writeText("{schemaVersion:4}")
            try { store.write(WorkshopProfile()); fail("Newer save overwritten") } catch(_: java.io.IOException) { }
            assertEquals("{schemaVersion:4}",file.readText())
        } finally { dir.deleteRecursively() }
    }
}

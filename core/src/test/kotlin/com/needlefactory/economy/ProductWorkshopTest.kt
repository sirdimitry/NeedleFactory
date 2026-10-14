package com.needlefactory.economy

import com.needlefactory.content.FactoryConfig
import com.needlefactory.simulation.*
import com.needlefactory.save.*
import org.junit.Assert.*
import org.junit.Test

class ProductWorkshopTest {
    private fun session()=FactorySession(FactoryConfig.products(),42)
    private fun workshop(credits:Long=0,purchased:Set<String> = emptySet())=
        Workshop(WorkshopProfile(workshopCredits=credits,purchased=purchased,activeSeed=42),EconomyConfig.products())
    private fun collect(s:FactorySession,count:Int) {
        s.batch.items.filter { it.material==Material.STRAW && it.route==Route.INTAKE }.take(count).forEach {
            s.manualPush(it.id); assertTrue(s.feed(it.id)); assertFalse(s.feed(it.id))
        }
        repeat(40) { s.step() }
    }
    @Test fun `loose reward pays only accepted completed material and rounding is aggregate`() {
        val s=session(); val w=workshop()
        assertEquals(0L,w.quote(s.batch).earned)
        collect(s,60)
        assertEquals(60,s.collectedStraw); assertEquals(60,s.looseStraw)
        assertEquals(3L,w.quote(s.batch).material); assertEquals(0,s.bundledStraw)
        collect(s,60); assertEquals(6L,w.quote(s.batch).material)
    }
    @Test fun `baler purchase prerequisite insufficient duplicate and exact debit`() {
        val w=workshop(42)
        assertEquals(PurchaseResult.LOCKED,w.purchase(UpgradeId.BALER))
        assertEquals(PurchaseResult.BOUGHT,w.purchase(UpgradeId.RAKE)); assertEquals(14L,w.profile.workshopCredits)
        assertEquals(PurchaseResult.INSUFFICIENT,w.purchase(UpgradeId.BALER)); assertEquals(14L,w.profile.workshopCredits)
        val funded=workshop(42,setOf(UpgradeId.RAKE.key))
        assertEquals(PurchaseResult.BOUGHT,funded.purchase(UpgradeId.BALER))
        assertEquals(0L,funded.profile.workshopCredits)
        assertEquals(PurchaseResult.OWNED,funded.purchase(UpgradeId.BALER)); assertEquals(0L,funded.profile.workshopCredits)
        assertFalse(UpgradeId.GUIDES.key in funded.profile.purchased)
    }
    @Test fun `owning a baler does not pay for unprocessed material or create a free machine`() {
        val s=session(); val w=workshop()
        assertFalse(s.baler); assertEquals(0L,w.quote(s.batch).material)
        for(id in listOf(MachineId.CONVEYOR,MachineId.SEPARATOR,MachineId.PACKER)) {
            s.toggle(id); assertFalse(s.enabled(id))
        }
        s.setBaler(true); s.toggleBaler()
        collect(s,120); repeat(300) { s.step() }
        assertEquals(120,s.collectedStraw); assertEquals(0,s.bundledStraw)
        assertEquals(120,s.batch.count(Route.BALER_QUEUE)); assertEquals(0L,w.quote(s.batch).earned)
        s.toggleBaler(); repeat(1000) { s.step() }
        assertEquals(120,s.bundledStraw); assertEquals(0,s.looseStraw)
        assertEquals(16L,w.quote(s.batch).material)
        repeat(600) { s.step() }; assertEquals(120,s.bundledStraw)
    }
    @Test fun `completed bundle value replaces loose value without paying both grades`() {
        val s=session(); val w=workshop()
        collect(s,60); s.setBaler(true); collect(s,60); repeat(1000) { s.step() }
        assertEquals(60,s.looseStraw); assertEquals(60,s.bundledStraw)
        assertEquals(11L,w.quote(s.batch).material)
    }
    @Test fun `early finish excludes all uncollected straw and needles and pays once`() {
        val s=session(); val w=workshop()
        collect(s,40); assertTrue(w.eligible(s.batch)); assertEquals(2L,w.quote(s.batch).earned)
        s.finish(); repeat(120) { s.step() }
        val r=w.settle(s.batch)
        assertEquals(40,r.loose); assertEquals(0,r.bundled); assertEquals(83,r.discarded)
        assertEquals(2L,r.earned); assertEquals(0,r.missed)
        assertEquals(r,w.settle(s.batch)); assertEquals(2L,w.profile.workshopCredits)
        val loaded=Workshop(WorkshopCodec.decode(WorkshopCodec.encode(w.profile),1).profile,EconomyConfig.products())
        assertEquals(r,loaded.settle(s.batch)); assertEquals(2L,loaded.profile.workshopCredits)
        assertTrue(loaded.next()); assertFalse(loaded.next())
    }
    @Test fun `early finish drains accepted baler work but cannot add the unfed pile`() {
        val s=session(); val w=workshop(); s.setBaler(true)
        collect(s,30); s.toggleBaler(); assertTrue(s.finish())
        repeat(600) { s.step() }
        assertEquals(30,s.bundledStraw); assertEquals(30,s.collectedStraw)
        val r=w.settle(s.batch); assertEquals(4L,r.material); assertEquals(93,r.discarded)
    }
    @Test fun `manual three needle recovery is gated and normal perfect has no arbitrary bonus`() {
        val s=session(); val w=workshop()
        for(n in s.batch.needles) assertFalse(s.recoverNeedle(n.x,n.y))
        collect(s,120)
        for(n in s.batch.needles) { assertTrue(s.recoverNeedle(n.x,n.y)); assertFalse(s.recoverNeedle(n.x,n.y)) }
        assertTrue(s.batch.complete); assertEquals(3,s.recovered); assertEquals(0,s.packaged)
        val r=w.settle(s.batch); assertTrue(r.perfect); assertEquals(15L,r.earned); assertEquals(0L,r.bonus)
        s.toggleRunning(); s.synchronizeSnapshots(); assertEquals(r,w.settle(s.batch))
    }
    @Test fun `pushing a needle into straw receiver misses it instead of using an invisible magnet`() {
        val s=session(); val w=workshop()
        val n=s.batch.needles.first(); s.manualPush(n.id); assertTrue(s.feed(n.id))
        assertEquals(Route.MISSED,n.route); assertEquals(1,s.missed); assertEquals(0,s.recovered)
        assertEquals(0L,w.quote(s.batch).metal)
    }
    @Test fun `pickup waits until both interpolated poses are exposed`() {
        val s=session(); val n=s.batch.needles.first()
        s.batch.items.filter { it.material==Material.STRAW }.forEach { it.x=-100f }
        assertEquals(1f,NeedleRecovery.fraction(s.batch,n),0f)
        assertTrue(NeedleRecovery.fraction(s.batch,n,true)<=.6f)
        assertFalse(s.recoverNeedle(n.x,n.y))
        s.synchronizeSnapshots()
        assertTrue(s.recoverNeedle(n.x,n.y))
    }
    @Test fun `schema three retains baler wallet receipt and unknown upgrade ids`() {
        val w=workshop(70,setOf("future-tool")); w.purchase(UpgradeId.RAKE); w.purchase(UpgradeId.BALER)
        val restored=WorkshopCodec.decode(WorkshopCodec.encode(w.profile),999).profile
        assertEquals(w.profile,restored); assertEquals(3,restored.schemaVersion)
        assertTrue(UpgradeId.BALER.key in restored.purchased); assertEquals("products-v2",restored.economyModel)
    }
    @Test fun `v1 migration keeps existing wallet tools and paid receipt without repricing`() {
        val old="""{"schemaVersion":1,"contentVersion":1,"WorkshopCredits":60,"purchasedUpgrades":["feed-rake"],"activeSeed":42,"receipt":{"batchId":1,"secured":3,"total":3,"processed":320,"materialTotal":320,"missed":0,"discarded":0,"metal":45,"material":30,"bonus":15}}"""
        val p=WorkshopCodec.decode(old,999).profile; val w=Workshop(p,EconomyConfig.products())
        assertEquals(60L,w.profile.workshopCredits); assertTrue(UpgradeId.RAKE.key in w.profile.purchased)
        assertEquals(90L,w.profile.receipt!!.earned); assertEquals("line-v1",w.profile.receipt!!.economyModel)
        w.settle(session().batch); assertEquals(60L,w.profile.workshopCredits)
        assertTrue(w.next()); assertEquals("products-v2",w.profile.economyModel)
    }
    @Test fun `denser cheap art keeps needle size and each starter seed fully covered`() {
        for(seed in 1L..100L) {
            val s=FactorySession(FactoryConfig.products(),seed)
            assertEquals(123,s.batch.items.size); assertEquals(3840,s.batch.items.sumOf { it.strands.size })
            for(n in s.batch.needles) {
                assertEquals(1.3f,n.length,0f); assertEquals(.018f,n.width,0f)
                assertTrue(NeedleRecovery.fraction(s.batch,n)<=.6f)
            }
        }
    }
}

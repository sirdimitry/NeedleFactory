package com.needlefactory.economy

import com.needlefactory.content.FactoryConfig
import com.needlefactory.save.WorkshopCodec
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.Test

class StrawDeliveryEconomyTest {
    private fun session()=FactorySession(FactoryConfig.pitProducts(),42)
    private fun workshop()=Workshop(WorkshopProfile(activeSeed=42,purchased=setOf(UpgradeId.METAL_RECEIVER.key)),EconomyConfig.pit().copy(clumpEconomy=false))
    private fun straw(s:FactorySession,count:Int) {
        repeat(count) { s.manualPush(it); assertTrue(s.feed(it)) }
        repeat(2400) { s.step() }
    }
    private fun metal(s:FactorySession,count:Int) {
        s.setMetalReceiver(true)
        repeat(count) { val id=s.batch.needles[it].id; s.manualPush(id); assertTrue(s.deliverMetal(id)) }
        repeat(2400) { s.step() }
    }
    @Test fun `early one needle and quarter straw pays thirteen no delivery premium`() {
        val s=session(); val w=workshop(); metal(s,1); straw(s,6)
        assertTrue(w.eligible(s.batch)); assertTrue(s.finish()); repeat(2400) { s.step() }
        val r=w.settle(s.batch)
        assertEquals(12L,r.metal); assertEquals(1L,r.material); assertEquals(0L,r.bonus); assertEquals(13L,r.earned)
    }
    @Test fun `last group earns delivery without requiring perfect metal`() {
        val s=session(); val w=workshop(); straw(s,23)
        assertEquals(5L,w.quote(s.batch).material); assertEquals(0L,w.quote(s.batch).bonus)
        s.manualPush(23); assertTrue(s.feed(23)); repeat(2400) { s.step() }
        assertEquals(6L,w.quote(s.batch).bonus)
        assertTrue(s.finish()); repeat(2400) { s.step() }
        val r=w.settle(s.batch)
        assertEquals(12L,r.earned); assertEquals(0L,r.metal); assertFalse(r.perfect)
    }
    @Test fun `three recovered and full hay thirty settled once across restart`() {
        val s=session(); val w=workshop(); metal(s,3); straw(s,24)
        assertTrue(s.batch.complete); val r=w.settle(s.batch)
        assertEquals(36L,r.metal); assertEquals(6L,r.material); assertEquals(6L,r.bonus)
        assertTrue(r.perfect); assertEquals(48L,r.earned)
        val saved=WorkshopCodec.decode(WorkshopCodec.encode(w.profile),42).profile
        val reload=Workshop(saved,EconomyConfig.pit().copy(clumpEconomy=false))
        assertEquals(r,reload.settle(s.batch)); assertEquals(48L,reload.profile.workshopCredits)
        s.toggleRunning(); s.toggleRunning(); reload.settle(s.batch)
        assertEquals(48L,reload.profile.workshopCredits)
    }
    @Test fun `old paid v4 receipt and ownership kept without repricing or duplicate bonus`() {
        val r=BatchReceipt(1,3,3,24,24,0,0,72,6,0,24,0,"products-v4")
        val p=WorkshopProfile(workshopCredits=78,activeSeed=42,receipt=r,purchased=setOf("needle-receiver"),economyModel="products-v4")
        val w=Workshop(WorkshopCodec.decode(WorkshopCodec.encode(p),42).profile,EconomyConfig.pit().copy(clumpEconomy=false))
        assertEquals("products-v5",w.model); assertEquals(r,w.profile.receipt)
        assertEquals(r,w.settle(session().batch)); assertEquals(78L,w.profile.workshopCredits)
        assertEquals(p.purchased,w.profile.purchased)
    }
}

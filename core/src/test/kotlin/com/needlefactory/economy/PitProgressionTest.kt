package com.needlefactory.economy

import com.needlefactory.content.FactoryConfig
import com.needlefactory.simulation.*
import com.needlefactory.save.*
import org.junit.Assert.*
import org.junit.Test

class PitProgressionTest {
    private fun workshop(credits:Long=0)=Workshop(WorkshopProfile(activeSeed=42,workshopCredits=credits),EconomyConfig.pit().copy(clumpEconomy=false))
    private fun admit(s:FactorySession,id:Int) { s.manualPush(id); assertTrue(s.feed(id)); repeat(180) { s.step() } }
    @Test fun `all straw six plus delivery six and discarded steel zero even at full clear`() {
        val s=FactorySession(FactoryConfig.pitProducts(),42); val w=workshop()
        for(i in s.batch.items.indices) admit(s,i)
        assertTrue(s.batch.complete); val r=w.settle(s.batch)
        assertEquals(24,r.loose); assertEquals(3,r.missed); assertEquals(0L,r.metal)
        assertEquals(6L,r.material); assertEquals(6L,r.bonus)
        assertEquals(12L,r.earned); assertEquals(12L,w.profile.workshopCredits)
        assertEquals(r,w.settle(s.batch)); assertEquals(12L,w.profile.workshopCredits)
    }
    @Test fun `early finish pays only four admitted straw and never remaining pile`() {
        val s=FactorySession(FactoryConfig.pitProducts(),42); val w=workshop()
        assertFalse(w.eligible(s.batch))
        repeat(4) { admit(s,it) }; assertTrue(w.eligible(s.batch)); assertTrue(s.finish())
        repeat(200) { s.step() }; val r=w.settle(s.batch)
        assertEquals(1L,r.earned); assertEquals(4,r.processed); assertEquals(23,r.discarded)
        assertEquals(0L,r.metal)
    }
    @Test fun `needle receiver costs two hundred forty is single purchase and cannot be bought without money`() {
        val poor=workshop(239); assertEquals(PurchaseResult.INSUFFICIENT,poor.purchase(UpgradeId.METAL_RECEIVER))
        assertEquals(239L,poor.profile.workshopCredits)
        val w=workshop(240); assertEquals(PurchaseResult.BOUGHT,w.purchase(UpgradeId.METAL_RECEIVER))
        assertEquals(0L,w.profile.workshopCredits); assertEquals(PurchaseResult.OWNED,w.purchase(UpgradeId.METAL_RECEIVER))
        assertEquals(PurchaseResult.LOCKED,w.purchase(UpgradeId.CONVEYOR))
        assertEquals(PurchaseResult.LOCKED,w.purchase(UpgradeId.MAGNET))
    }
    @Test fun `no free manual sorting and bought port pays raw steel twelve once`() {
        val s=FactorySession(FactoryConfig.pitProducts(),42); val n=s.batch.needles[0]
        s.manualPush(n.id); assertFalse(s.deliverMetal(n.id))
        val w=workshop(240); w.purchase(UpgradeId.METAL_RECEIVER); s.setMetalReceiver(true)
        s.toggleMetalReceiver(); assertFalse(s.deliverMetal(n.id)); s.toggleMetalReceiver()
        s.manualPush(n.id); assertTrue(s.deliverMetal(n.id)); repeat(120) { s.step() }
        assertEquals(12L,w.quote(s.batch).metal); assertTrue(s.finish()); repeat(200) { s.step() }
        assertEquals(12L,w.settle(s.batch).earned); assertEquals(12L,w.profile.workshopCredits)
        assertFalse(s.deliverMetal(n.id)); w.settle(s.batch); assertEquals(12L,w.profile.workshopCredits)
    }
    @Test fun `new receiver ownership wallet and v4 receipts survive versioned round trip`() {
        val w=workshop(300); w.purchase(UpgradeId.METAL_RECEIVER)
        val p=w.profile.copy(purchased=w.profile.purchased+"future-module",economyModel="products-v4")
        assertEquals(p,WorkshopCodec.decode(WorkshopCodec.encode(p),42).profile)
        val old=WorkshopProfile(workshopCredits=146,purchased=setOf("feed-rake","straw-baler","short-conveyor","magnetic-separator"),activeSeed=42,economyModel="products-v3")
        val migrated=Workshop(old,EconomyConfig.pit().copy(clumpEconomy=false)).profile
        assertEquals(old.workshopCredits,migrated.workshopCredits); assertEquals(old.purchased,migrated.purchased)
        assertFalse(UpgradeId.METAL_RECEIVER.key in migrated.purchased)
    }
}

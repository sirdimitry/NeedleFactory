package com.needlefactory.economy

import com.needlefactory.content.FactoryConfig
import com.needlefactory.save.WorkshopCodec
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.Test

class ClumpProgressionTest {
    private fun session()=FactorySession(FactoryConfig.pitProducts(),42)
    private fun workshop(credits:Long=0)=Workshop(WorkshopProfile(activeSeed=42,workshopCredits=credits),EconomyConfig.pit())
    private fun accept(s:FactorySession,n:Int) {
        repeat(n) { s.manualPush(it);assertTrue(s.feed(it)) };repeat(2400) { s.step() }
    }
    @Test fun `one clump pays one immediately and cannot pay twice or after restart`() {
        val s=session();val w=workshop();accept(s,1)
        assertEquals(1L,w.creditDelivered(s.batch));assertEquals(1L,w.profile.workshopCredits)
        assertEquals(0L,w.creditDelivered(s.batch))
        val loaded=Workshop(WorkshopCodec.decode(WorkshopCodec.encode(w.profile),42).profile,EconomyConfig.pit())
        assertEquals(0L,loaded.creditDelivered(s.batch));assertEquals(1L,loaded.profile.workshopCredits)
        s.finish();repeat(2400) { s.step() }
        assertEquals(1L,loaded.settle(s.batch).earned);assertEquals(1L,loaded.profile.workshopCredits)
    }
    @Test fun `two full straw cycles pay sixty and unlock fifty-credit loosener`() {
        val s=session();val w=workshop()
        repeat(2) {
            accept(s,24);w.creditDelivered(s.batch);s.finish();repeat(2400) { s.step() }
            assertEquals(30L,w.settle(s.batch).earned)
            if(it==0) {w.next();s.restore(w.profile.activeSeed,2)}
        }
        assertEquals(60L,w.profile.workshopCredits)
        assertEquals(50L,w.price(UpgradeId.RAKE));assertEquals(PurchaseResult.BOUGHT,w.purchase(UpgradeId.RAKE))
        assertEquals(10L,w.profile.workshopCredits);assertEquals(2,w.gripCapacity);assertEquals(1.2f,w.gripSpeed,0f)
    }
    @Test fun `level cost geometric grip and speed persist`() {
        val w=workshop(10000);w.purchase(UpgradeId.RAKE)
        assertEquals(35L,w.upgradePrice("loosen-grip"));w.upgrade("loosen-grip")
        assertEquals(61L,w.upgradePrice("loosen-grip"));assertEquals(3,w.gripCapacity)
        w.upgrade("loosen-speed");assertEquals(1.32f,w.gripSpeed,.001f)
        assertEquals(w.profile,WorkshopCodec.decode(WorkshopCodec.encode(w.profile),42).profile)
    }
    @Test fun `rake stays five grip and five speed tiers ahead of loosener`() {
        val w=workshop(10000);w.purchase(UpgradeId.RAKE);repeat(3) { w.upgrade("loosen-grip");w.upgrade("loosen-speed") }
        val count=w.gripCapacity;val speed=w.gripSpeed
        w.purchase(UpgradeId.STEEL_RAKE);assertEquals(count+5,w.gripCapacity);assertEquals(speed+.6f,w.gripSpeed,.001f)
    }
    @Test fun `insufficient funds and missing tool do not mutate levels`() {
        val w=workshop();val before=w.profile
        assertEquals(PurchaseResult.LOCKED,w.upgrade("loosen-grip"))
        assertEquals(PurchaseResult.INSUFFICIENT,w.upgrade("material-value"));assertEquals(before,w.profile)
    }
    @Test fun `material purchases affect current lot and next lot`() {
        val w=workshop(10000);w.upgrade("material-value");repeat(3) { w.upgrade("material-count") }
        assertEquals(2L,w.clumpValue(0));assertEquals(30,w.profile.lotCount)
        val s=session();s.finish();repeat(2400) { s.step() };w.settle(s.batch);w.next()
        assertEquals(2L,w.profile.lotValue);assertEquals(30,w.profile.lotCount);assertTrue(w.profile.delivered.isEmpty())
        s.configureLot(w.profile.lotCount);s.restore(w.profile.activeSeed,2);assertEquals(33,s.batch.items.size)
    }
    @Test fun `lucky clump value is deterministic and never rolls again during quote`() {
        val p=WorkshopProfile(activeSeed=42,lotLuck=12,lotValue=3)
        val a=Workshop(p,EconomyConfig.pit());val b=Workshop(p,EconomyConfig.pit())
        val values=(0..23).map { a.clumpValue(it) }
        assertEquals(values,(0..23).map { b.clumpValue(it) });assertTrue(values.all { it==3L || it==6L })
        assertTrue(values.contains(3L));assertTrue(values.contains(6L))
    }
    @Test fun `settled old receipt is not repriced by new clump model`() {
        val r=BatchReceipt(1,0,3,24,24,3,0,0,6,6,24,0,"products-v5")
        val w=Workshop(WorkshopProfile(activeSeed=42,workshopCredits=12,receipt=r),EconomyConfig.pit())
        assertEquals(r,w.settle(session().batch));assertEquals(12L,w.profile.workshopCredits)
    }
    @Test fun `paid receiver quantity changes simultaneous grinding not idle collection`() {
        val slow=session();val upgraded=session();upgraded.setReceiverLevels(0,2)
        for(s in listOf(slow,upgraded)) {
            repeat(3) { s.manualPush(it);assertTrue(s.feed(it)) };repeat(40) { s.step() }
        }
        assertEquals(2,slow.batch.count(Route.RECEIVING));assertEquals(0,upgraded.batch.count(Route.RECEIVING))
        assertEquals(24,slow.batch.count(Route.INTAKE));assertEquals(24,upgraded.batch.count(Route.INTAKE))
    }
}

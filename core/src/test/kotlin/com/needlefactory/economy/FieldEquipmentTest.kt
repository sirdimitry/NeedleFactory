package com.needlefactory.economy

import com.needlefactory.content.FactoryConfig
import com.needlefactory.simulation.*
import com.needlefactory.save.WorkshopCodec
import org.junit.Assert.*
import org.junit.Test

class FieldEquipmentTest {
    private fun w(credits:Long=100000)=Workshop(WorkshopProfile(activeSeed=42,workshopCredits=credits),EconomyConfig.pit())
    @Test fun `robot single ownership and escalating meaningful speed purchases`() {
        val w=w();assertEquals(PurchaseResult.BOUGHT,w.buyEquipment(EquipmentKind.ROBOT))
        assertEquals(99800L,w.profile.workshopCredits)
        assertEquals(PurchaseResult.LOCKED,w.buyEquipment(EquipmentKind.ROBOT))
        assertEquals(300L,w.profile.equipment.single().price("speed"))
        w.upgradeEquipment(1,"speed");assertEquals(2,w.profile.equipment.single().speed)
        assertEquals(450L,w.profile.equipment.single().price("speed"))
        assertEquals(PurchaseResult.LOCKED,w.upgradeEquipment(1,"width"))
    }
    @Test fun `fan cone levels narrow to ninety and separate upgrades save independently`() {
        val w=w();repeat(2) {assertEquals(PurchaseResult.BOUGHT,w.buyEquipment(EquipmentKind.FAN))}
        val e=w.profile.equipment.first();assertEquals(18f,e.coneDegrees,0f);assertEquals(750L,e.price("width"))
        repeat(9) {assertEquals(PurchaseResult.BOUGHT,w.upgradeEquipment(e.id,"width"))}
        assertEquals(90f,w.profile.equipment.first().coneDegrees,0f)
        assertEquals(PurchaseResult.LOCKED,w.upgradeEquipment(e.id,"width"))
        assertEquals(1,w.profile.equipment.last().width)
        w.upgradeEquipment(e.id,"range");w.upgradeEquipment(e.id,"speed")
        assertEquals(w.profile,WorkshopCodec.decode(WorkshopCodec.encode(w.profile),42).profile)
    }
    @Test fun `wind only affects forward range cone and angular quantization wraps`() {
        val e=FieldEquipment(1,EquipmentKind.FAN,3f,0f,degrees=0)
        assertTrue(e.containsWind(5f,0f));assertFalse(e.containsWind(1f,0f));assertFalse(e.containsWind(5f,1f));assertFalse(e.containsWind(8f,0f))
        assertEquals(359,FieldEquipment.angle(1f,-.0175f));assertEquals(90,FieldEquipment.angle(0f,1f))
    }
    @Test fun `no funds no mutation and sale cannot become money loop`() {
        val w=w(39);val p=w.profile
        assertEquals(PurchaseResult.INSUFFICIENT,w.buyEquipment(EquipmentKind.DIVIDER));assertEquals(p,w.profile)
        val a=w(100);a.buyEquipment(EquipmentKind.DIVIDER);assertTrue(a.sellEquipment(1));assertFalse(a.sellEquipment(1));assertEquals(80L,a.profile.workshopCredits)
    }
    @Test fun `positions angles investment migrate and old saves retain purchases`() {
        val w=w();w.buyEquipment(EquipmentKind.DIVIDER);w.moveEquipment(1,11f,-6f,721)
        val e=w.profile.equipment.single();assertEquals(1,e.degrees);assertEquals(11f,e.x,0f)
        assertEquals(w.profile,WorkshopCodec.decode(WorkshopCodec.encode(w.profile),42).profile)
        val old=WorkshopCodec.decode("{schemaVersion:3,contentVersion:6,WorkshopCredits:36,purchasedUpgrades:[needle-receiver]}",42).profile
        assertEquals(36L,old.workshopCredits);assertTrue("needle-receiver" in old.purchased);assertTrue(old.equipment.isEmpty())
    }
    @Test fun `immediate new material price never reprices paid clumps or final settlement`() {
        val w=w();val s=FactorySession(FactoryConfig.pitProducts(),42)
        s.manualPush(0);s.feed(0);repeat(200) {s.step()};w.creditDelivered(s.batch)
        assertEquals(1L,w.profile.delivered[0]);w.upgrade("material-value")
        assertEquals(2L,w.clumpValue(1));assertEquals(1L,w.quote(s.batch).material)
        s.manualPush(1);s.feed(1);repeat(200) {s.step()};assertEquals(2L,w.creditDelivered(s.batch))
        val before=w.profile.workshopCredits;s.finish();repeat(2400) {s.step()}
        assertEquals(3L,w.settle(s.batch).material);assertEquals(before,w.profile.workshopCredits)
    }
    @Test fun `immediate quantity adds new identities without losing live recovered or queued objects`() {
        val s=FactorySession(FactoryConfig.pitProducts(),42);val w=w()
        s.batch.items[0].x=10f;s.manualPush(1);s.feed(1)
        val oldNeedles=s.batch.needles.map {Triple(it.x,it.y,it.route)}
        w.upgrade("material-count");assertTrue(s.growLot(w.profile.lotCount))
        assertEquals(28,s.batch.items.size);assertEquals(10f,s.batch.items[0].x,0f)
        assertEquals(Route.RECEIVING,s.batch.items[1].route)
        assertEquals(oldNeedles,s.batch.needles.map {Triple(it.x,it.y,it.route)})
        assertEquals((0..27).toList(),s.batch.items.map {it.id});assertFalse(s.growLot(25))
    }
    @Test fun `bought receiver can be sold once without reclaiming paid metal`() {
        val w=w(300);w.purchase(UpgradeId.METAL_RECEIVER);assertTrue(w.sellReceiver());assertEquals(180L,w.profile.workshopCredits)
        assertFalse(w.sellReceiver());assertFalse("needle-receiver" in w.profile.purchased)
    }
}

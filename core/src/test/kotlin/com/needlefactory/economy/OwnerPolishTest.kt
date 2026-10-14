package com.needlefactory.economy

import com.needlefactory.content.FactoryConfig
import com.needlefactory.simulation.*
import com.needlefactory.save.WorkshopCodec
import com.needlefactory.presentation.NeedleGlint
import org.junit.Assert.*
import org.junit.Test

class OwnerPolishTest {
    private fun w()=Workshop(WorkshopProfile(activeSeed=42,workshopCredits=10000),EconomyConfig.pit())
    @Test fun `fan anchor locks position and unlock is required to carry`() {
        val w=w();w.buyEquipment(EquipmentKind.FAN)
        w.moveEquipment(1,8f,-5f,0);assertEquals(270,w.profile.equipment.single().degrees)
        w.toggleFanAnchor(1);w.moveEquipment(1,3f,2f,1)
        val e=w.profile.equipment.single();assertEquals(8f,e.x,0f);assertEquals(-5f,e.y,0f);assertEquals(1,e.degrees)
        assertEquals(w.profile,WorkshopCodec.decode(WorkshopCodec.encode(w.profile),42).profile)
        w.toggleFanAnchor(1);w.moveEquipment(1,3f,2f,180);assertEquals(3f,w.profile.equipment.single().x,0f);assertEquals(1,w.profile.equipment.single().degrees)
    }
    @Test fun `legacy fans deploy feet without losing angle or prior receiver investment`() {
        val p=WorkshopCodec.decode("""{schemaVersion:3,contentVersion:7,WorkshopCredits:10,purchasedUpgrades:[needle-receiver],nextEquipmentId:2,equipment:[{id:1,kind:FAN,x:4,y:0,degrees:32,speed:1,width:1,range:1,invested:500}]}""",42).profile
        assertTrue(p.equipment.single().anchored);assertEquals(32,p.equipment.single().degrees)
        val w=Workshop(p,EconomyConfig.pit());assertTrue(w.sellReceiver());assertEquals(19L,w.profile.workshopCredits)
    }
    @Test fun `all three steel bonus is guaranteed without charging uncollected straw twice`() {
        val w=w();val s=FactorySession(FactoryConfig.pitProducts(),42);s.setMetalReceiver(true)
        for(n in s.batch.needles) {s.manualPush(n.id);assertTrue(s.deliverMetal(n.id))}
        repeat(200) {s.step()};assertEquals(36L,w.quote(s.batch).metal);assertEquals(36L,w.quote(s.batch).bonus)
        s.manualPush(0);s.feed(0);repeat(200) {s.step()};assertEquals(1L,w.creditDelivered(s.batch))
        val balance=w.profile.workshopCredits;s.finish();repeat(200) {s.step()}
        val r=w.settle(s.batch);assertEquals(73L,r.earned);assertEquals(balance+72,w.profile.workshopCredits)
        w.settle(s.batch);assertEquals(balance+72,w.profile.workshopCredits)
        assertEquals(w.profile,WorkshopCodec.decode(WorkshopCodec.encode(w.profile),42).profile)
    }
    @Test fun `missing steel yields zero pit metal bonus and a distinct impact per needle`() {
        val w=w();val s=FactorySession(FactoryConfig.pitProducts(),42)
        for(n in s.batch.needles) {s.manualPush(n.id);s.feed(n.id)}
        assertEquals(3L,s.metalImpacts);assertEquals(0L,s.strawImpacts)
        repeat(200) {s.step()};assertEquals(0L,w.quote(s.batch).metal);assertEquals(0L,w.quote(s.batch).bonus)
        s.manualPush(0);s.feed(0);assertEquals(1L,s.strawImpacts)
    }
    @Test fun `steel scales above even lucky straw and collector is a meaningful purchase`() {
        val w=w();repeat(3) {w.upgrade("material-value")}
        assertTrue(w.needlePrice>w.profile.lotValue*2);assertEquals(24L,w.needlePrice);assertEquals(72L,w.clearMetalBonus)
        assertEquals(240L,w.price(UpgradeId.METAL_RECEIVER))
        w.purchase(UpgradeId.METAL_RECEIVER);assertEquals(240L,w.profile.receiverInvestment)
    }
    @Test fun `width is purchased immediately one metre at a time with bounded honest footprint`() {
        val w=w();val s=FactorySession(FactoryConfig.pitProducts(),42)
        assertEquals(75L,w.upgradePrice("receiver-width"));w.upgrade("receiver-width")
        s.setReceiverLevels(0,0,w.level("receiver-width"));assertEquals(2.64f,s.pitWidth,.001f)
        repeat(7) {assertEquals(PurchaseResult.BOUGHT,w.upgrade("receiver-width"))}
        val before=w.profile;assertEquals(PurchaseResult.LOCKED,w.upgrade("receiver-width"));assertEquals(before,w.profile)
        assertEquals(w.profile,WorkshopCodec.decode(WorkshopCodec.encode(w.profile),42).profile)
    }
    @Test fun `explicit cheats add selected amounts save and reject invalid amounts or overflow`() {
        val w=w();assertTrue(w.addPlaytestCredits());assertEquals(10100L,w.profile.workshopCredits)
        assertTrue(w.addPlaytestCredits(1000));assertEquals(11100L,w.profile.workshopCredits)
        assertTrue(w.addPlaytestCredits(10000));assertEquals(21100L,w.profile.workshopCredits)
        val before=w.profile
        for(amount in listOf(-100L,0L,50L,Long.MAX_VALUE)) assertFalse(w.addPlaytestCredits(amount))
        assertEquals(before,w.profile)
        assertEquals(w.profile,WorkshopCodec.decode(WorkshopCodec.encode(w.profile),42).profile)
        val cap=Workshop(WorkshopProfile(workshopCredits=1_000_000_000_000),EconomyConfig.pit())
        for(amount in listOf(100L,1000L,10000L)) assertFalse(cap.addPlaytestCredits(amount))
    }
    @Test fun `full exposure gets strong stable cartoon glint without early hint`() {
        assertEquals(0f,NeedleGlint.gain(.60f,1f),0f);assertEquals(0f,NeedleGlint.gain(.75f,1f),0f)
        assertTrue(NeedleGlint.gain(.9f,.5f)>0f);assertEquals(1f,NeedleGlint.gain(1f,0f),0f)
    }
    @Test fun `divider length purchases alter real extent and preserve separate instances`() {
        val w=w();w.buyEquipment(EquipmentKind.DIVIDER);w.buyEquipment(EquipmentKind.DIVIDER)
        assertEquals(PurchaseResult.BOUGHT,w.upgradeEquipment(1,"range"))
        assertEquals(1.6f,w.profile.equipment.first().dividerLength,.001f)
        assertEquals(1.3f,w.profile.equipment.last().dividerLength,.001f)
        assertEquals(PurchaseResult.LOCKED,w.upgradeEquipment(1,"speed"))
        assertEquals(w.profile,WorkshopCodec.decode(WorkshopCodec.encode(w.profile),42).profile)
    }

    @Test fun `nearby divider padding cannot steal fan double tap at its visible centre`() {
        val fan=FieldEquipment(1,EquipmentKind.FAN,5.5f,-3.6f)
        val beam=FieldEquipment(2,EquipmentKind.DIVIDER,5.5f,-4.4f)
        assertEquals(fan,FieldEquipmentSelection.find(listOf(fan,beam),5.5f,-3.6f))
        assertEquals(beam,FieldEquipmentSelection.find(listOf(fan,beam),5.5f,-4.4f))
        assertNull(FieldEquipmentSelection.find(listOf(fan,beam),10f,7f))
    }

}

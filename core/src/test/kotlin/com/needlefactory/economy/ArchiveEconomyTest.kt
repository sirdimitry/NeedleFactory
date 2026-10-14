package com.needlefactory.economy

import com.needlefactory.content.FactoryConfig
import com.needlefactory.simulation.*
import com.needlefactory.save.WorkshopCodec
import com.needlefactory.presentation.NeedleGlint
import org.junit.Assert.*
import org.junit.Test

class ArchiveEconomyTest {
    private fun session()=FactorySession(FactoryConfig.grabProducts(),42)
    private fun workshop(credits:Long=0,owned:Set<String> = emptySet())=
        Workshop(WorkshopProfile(workshopCredits=credits,purchased=owned,activeSeed=42),EconomyConfig.workshop())
    @Test fun `manual full product is 96 early sold quarter with one raw is 30 no finish multiplier`() {
        val s=session(); val w=workshop()
        s.batch.items.take(6).forEach { it.route=Route.LOOSE }
        s.batch.needles.first().route=Route.RECOVERED
        assertEquals(30L,w.quote(s.batch).earned); assertTrue(w.eligible(s.batch))
        assertTrue(s.finish()); val r=w.settle(s.batch)
        assertEquals(30L,r.earned); assertEquals(0L,r.bonus)
        repeat(20) { assertEquals(r,w.settle(s.batch)) }; assertEquals(30L,w.profile.workshopCredits)
        val full=session(); full.batch.items.take(24).forEach { it.route=Route.LOOSE }
        full.batch.needles.forEach { it.route=Route.RECOVERED }
        assertEquals(96L,workshop().settle(full.batch).earned)
    }
    @Test fun `perfect bundled sorted is 160 and missed raw is unpaid`() {
        val s=session(); s.batch.items.take(24).forEach { it.route=Route.BUNDLED }
        s.batch.needles.forEach { it.route=Route.SORTED }
        assertEquals(160L,workshop().settle(s.batch).earned)
        s.batch.needles.first().route=Route.MISSED
        assertEquals(128L,workshop().quote(s.batch).earned)
    }
    @Test fun `tool prices meaningful prerequisites and no double debit`() {
        val w=workshop(599)
        assertEquals(PurchaseResult.LOCKED,w.purchase(UpgradeId.STEEL_RAKE))
        assertEquals(PurchaseResult.BOUGHT,w.purchase(UpgradeId.RAKE)); assertEquals(449L,w.profile.workshopCredits)
        assertEquals(HandTool.RAKE,w.tool); assertEquals(2,w.tool.capacity)
        assertEquals(PurchaseResult.INSUFFICIENT,w.purchase(UpgradeId.STEEL_RAKE))
        assertEquals(PurchaseResult.OWNED,w.purchase(UpgradeId.RAKE)); assertEquals(449L,w.profile.workshopCredits)
        val enough=workshop(600); enough.purchase(UpgradeId.RAKE)
        assertEquals(PurchaseResult.BOUGHT,enough.purchase(UpgradeId.STEEL_RAKE))
        assertEquals(0L,enough.profile.workshopCredits); assertEquals(3,enough.tool.capacity)
        assertFalse(enough.select(HandTool.fromKey("unknown")) && enough.tool!=HandTool.HAND)
    }
    @Test fun `legacy wallet ownership and paid receipt survive price revision without repricing`() {
        val old="""{"schemaVersion":2,"contentVersion":2,"WorkshopCredits":263,"purchasedUpgrades":["feed-rake","feed-guides","straw-baler"],"activeSeed":42,"receipt":{"batchId":1,"secured":0,"total":3,"processed":24,"materialTotal":24,"missed":3,"discarded":0,"metal":0,"material":16,"bonus":0,"loose":0,"bundled":24,"economyModel":"products-v2"}}"""
        val load=WorkshopCodec.decode(old,42); val w=Workshop(load.profile,EconomyConfig.workshop())
        assertEquals(263L,w.profile.workshopCredits); assertTrue(UpgradeId.BALER.key in w.profile.purchased)
        assertEquals(HandTool.RAKE,w.tool); assertNotNull(load.warning)
        assertEquals(16L,w.settle(session().batch).earned); assertEquals(263L,w.profile.workshopCredits)
        assertEquals(w.profile,WorkshopCodec.decode(WorkshopCodec.encode(w.profile),42).profile)
    }
    @Test fun `selected tool is persisted and unavailable tool falls back to hand`() {
        val w=workshop(600); assertFalse(w.select(HandTool.STEEL_RAKE)); w.purchase(UpgradeId.RAKE); w.purchase(UpgradeId.STEEL_RAKE)
        w.select(HandTool.HAND); w.cycleTool(); assertEquals(HandTool.RAKE,w.tool)
        w.cycleTool(); assertEquals(HandTool.STEEL_RAKE,w.tool)
        assertEquals(w.profile,WorkshopCodec.decode(WorkshopCodec.encode(w.profile),42).profile)
        val bad=WorkshopCodec.decode("""{"schemaVersion":3,"selectedTool":"steel-rake"}""",42)
        assertEquals("hand",bad.profile.selectedTool)
    }
    @Test fun `late star cannot flash at threshold or use malformed sensor gain`() {
        for(v in listOf(0f,.59f,.60f,.70f,.75f,Float.NaN)) assertEquals(0f,NeedleGlint.gain(v,1f),0f)
        assertTrue(NeedleGlint.gain(.9f,.4f)>NeedleGlint.gain(.8f,.4f))
        assertTrue(NeedleGlint.gain(1f,.4f)>NeedleGlint.gain(.9f,.4f))
        assertEquals(0f,NeedleGlint.gain(1f,Float.NaN),0f)
    }
}

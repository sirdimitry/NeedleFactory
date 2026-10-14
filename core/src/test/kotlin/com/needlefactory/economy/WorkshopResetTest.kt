package com.needlefactory.economy

import com.needlefactory.content.FactoryConfig
import com.needlefactory.save.*
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WorkshopResetTest {
    @Test fun `new game removes wallet purchases receipt and cannot settle abandoned lot`() {
        val old=FactorySession(FactoryConfig.grabProducts(),42)
        old.batch.items.take(24).forEach { it.route=Route.BUNDLED }
        old.batch.needles.forEach { it.route=Route.SORTED }
        val w=Workshop(WorkshopProfile(activeSeed=42),EconomyConfig.workshop())
        w.settle(old.batch); w.purchase(UpgradeId.RAKE)
        w.startOver(99)
        assertEquals(WorkshopProfile(activeSeed=99,economyModel="products-v3"),w.profile)
        assertEquals(HandTool.HAND,w.tool); assertFalse(w.next())
        try { w.settle(old.batch); fail("Abandoned batch paid after reset") } catch(_:IllegalArgumentException) { }
    }
    @Test fun `session reset removes every paid capability and resumes manual starter`() {
        val s=FactorySession(FactoryConfig.grabProducts(),42)
        s.setTools(true,true); s.setBaler(true); s.setTransport(true,true); s.setHandTool(3,.95f)
        s.batch.items.forEach { s.manualPush(it.id); s.feed(it.id) }; repeat(1400) { s.step() }
        assertEquals(3,s.recovered); s.toggleRunning(); s.toggle(MachineId.VIBRATION)
        s.startOver(99)
        assertTrue(s.running); assertTrue(s.receivingEnabled); assertEquals(BatchPhase.ACTIVE,s.phase)
        assertEquals(27,s.batch.count(Route.INTAKE)); assertEquals(0,s.recovered); assertEquals(0,s.missed)
        assertEquals(1,s.batchNumber); assertEquals(1,s.handCapacity); assertFalse(s.rake || s.guides || s.baler || s.conveyorOwned || s.magnetOwned)
        repeat(600) { s.step() }; assertEquals(27,s.batch.count(Route.INTAKE))
    }
    @Test fun `reset primary and fallback both contain only fresh game`() {
        val dir=Files.createTempDirectory("workshop-reset").toFile()
        try {
            val f=File(dir,"save.json"); val store=WorkshopStore(f)
            store.write(WorkshopProfile(workshopCredits=500,purchased=setOf("feed-rake")))
            store.reset(WorkshopProfile(activeSeed=99,economyModel="products-v3"))
            assertEquals(0L,store.load(1).profile.workshopCredits)
            f.writeText("broken")
            val fallback=store.load(1)
            assertEquals(99L,fallback.profile.activeSeed); assertTrue(fallback.profile.purchased.isEmpty())
            assertEquals(0L,fallback.profile.workshopCredits); assertNotNull(fallback.warning)
        } finally { dir.deleteRecursively() }
    }
}

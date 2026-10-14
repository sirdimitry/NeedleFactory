package com.needlefactory.physics

import com.badlogic.gdx.utils.GdxNativesLoader
import com.needlefactory.content.FactoryConfig
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

class AugerReceiverTest {
    companion object { @JvmStatic @BeforeClass fun natives() { GdxNativesLoader.load() } }
    @Test fun `running starter auger cannot collect idle material`() {
        val session=FactorySession(FactoryConfig.grabProducts(),42)
        FactoryIntakePhysics(session).use { physics ->
            repeat(650) { session.step(); physics.step() }
            assertTrue(session.receiverTurns>0f)
            assertEquals(27,session.batch.count(Route.INTAKE))
            assertEquals(0,session.collectedStraw)
            assertEquals(0,session.recovered)
            assertFalse(session.conveyorOwned)
            assertFalse(session.magnetOwned)
        }
    }
    @Test fun `receiver stop and pause retain auger phase without changing material`() {
        val session=FactorySession(FactoryConfig.grabProducts(),42)
        repeat(25) { session.step() }; val phase=session.receiverTurns
        session.toggle(MachineId.VIBRATION)
        repeat(150) { session.step() }; assertEquals(phase,session.receiverTurns,0f)
        session.toggle(MachineId.VIBRATION); session.toggleRunning()
        repeat(150) { session.step() }; assertEquals(phase,session.receiverTurns,0f)
        session.synchronizeSnapshots(); assertEquals(phase,session.receiverTurns,0f)
        assertEquals(27,session.batch.count(Route.INTAKE))
        session.restore(42,1); assertEquals(0f,session.receiverTurns,0f)
    }
}

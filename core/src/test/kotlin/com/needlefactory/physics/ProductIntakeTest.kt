package com.needlefactory.physics

import com.badlogic.gdx.utils.GdxNativesLoader
import com.needlefactory.content.FactoryConfig
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

class ProductIntakeTest {
    companion object { @JvmStatic @BeforeClass fun natives() { GdxNativesLoader.load() } }
    @Test fun `manual receiver has no initial or released idle collection with every early tool state`() {
        for(rake in listOf(false,true)) for(guides in listOf(false,true)) {
            val s=FactorySession(FactoryConfig.products(),42); s.setTools(rake,guides)
            FactoryIntakePhysics(s).use { p ->
                repeat(600) { s.step(); p.step() }; assertEquals(123,s.batch.count(Route.INTAKE))
                p.begin(2f,4f)
                repeat(100) { i -> p.target(2f+8f*i/99f,4f); s.step(); p.step() }
                p.end(); val intake=s.batch.count(Route.INTAKE)
                repeat(600) { s.step(); p.step() }
                assertEquals(intake,s.batch.count(Route.INTAKE)); assertEquals(123-intake,s.collectedStraw+s.missed)
                assertFalse(s.batch.items.any { it.manuallyPushed }); assertEquals(0,s.bundledStraw)
            }
        }
    }
}

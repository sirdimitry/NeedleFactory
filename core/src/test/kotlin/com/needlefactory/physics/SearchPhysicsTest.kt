package com.needlefactory.physics

import com.needlefactory.content.SearchConfig
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.BeforeClass
import com.badlogic.gdx.utils.GdxNativesLoader
import kotlin.math.*

class SearchPhysicsTest {
    companion object {
        @JvmStatic @BeforeClass fun loadCoreNatives() { GdxNativesLoader.load() }
    }
    private val config = SearchConfig()
    @Test fun `native world has bounded body count pusher speed and no idle discovery`() {
        val round = RoundGenerator.generate(42, config)
        SearchPhysics(round, config).use { physics ->
            assertEquals(66, physics.bodyCount)
            repeat(60) { physics.step() }
            assertEquals(0f, NeedleExposure.visibleFraction(round, 15), 0f)
            physics.begin(1f, 1f)
            physics.target(100f, 100f)
            val x = physics.pusherX; val y = physics.pusherY
            physics.step()
            assertTrue(hypot(physics.pusherX - x, physics.pusherY - y) <= config.pusherSpeed * config.stepSeconds + 0.001f)
            physics.end()
            assertFalse(physics.pushing)
        }
    }

    @Test fun `slow precise drag moves nearby material`() {
        val round = RoundGenerator.generate(123, config)
        val p = round.clutter[0]
        val startX = p.x; val startY = p.y
        SearchPhysics(round, config).use { physics ->
            physics.begin(startX - 1f, startY)
            repeat(120) { i -> physics.target(startX - 1f + i * 0.02f, startY); physics.step() }
            assertTrue(hypot(p.x - startX, p.y - startY) > 0.1f)
            assertTrue(p.x.isFinite() && p.y.isFinite())
        }
    }

    @Test fun `fast clearing sweeps expose seeds without escaping tray or growing worlds`() {
        for (seed in listOf(1L, 42L, 123L, 999L, Long.MIN_VALUE, -1663341875487337535L)) {
            val round = RoundGenerator.generate(seed, config)
            SearchPhysics(round, config).use { physics ->
                var bestExposure = 0f
                for (row in 0..26) {
                    val y = 0.5f + row * 0.5f
                    val from = if (row % 2 == 0) 0.4f else 9.6f
                    val to = 10f - from
                    physics.begin(from, y)
                    physics.target(to, y)
                    repeat(100) {
                        physics.step()
                        bestExposure = max(bestExposure, NeedleExposure.visibleFraction(round, 15))
                        for (p in round.clutter + round.needle) {
                            assertTrue("seed $seed finite", p.x.isFinite() && p.y.isFinite() && p.angle.isFinite())
                            val hx = (abs(cos(p.angle)) * p.length + abs(sin(p.angle)) * p.width) / 2f
                            val hy = (abs(sin(p.angle)) * p.length + abs(cos(p.angle)) * p.width) / 2f
                            assertTrue("seed $seed x=${p.x}", p.x - hx >= 0f && p.x + hx <= 10f)
                            assertTrue("seed $seed y=${p.y}", p.y - hy >= 0f && p.y + hy <= 14f)
                        }
                    }
                    physics.end()
                }
                assertTrue("seed $seed can expose, best=$bestExposure", bestExposure >= config.discoveryFraction)
                assertEquals(66, physics.bodyCount)
            }
        }
    }
}

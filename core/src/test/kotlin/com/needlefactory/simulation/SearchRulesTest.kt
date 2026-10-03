package com.needlefactory.simulation

import com.needlefactory.content.SearchConfig
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SearchRulesTest {
    private val config = SearchConfig()
    private fun clear(round: SearchRound) {
        for (p in round.clutter) { p.x = 1f; p.y = 1f; p.previousX = 1f; p.previousY = 1f }
    }

    @Test fun `500 seeds create exactly one reachable fully covered needle and sixty strips`() {
        repeat(500) { seed ->
            val r = RoundGenerator.generate(seed.toLong(), config)
            assertEquals(60, r.clutter.size)
            assertEquals(0f, NeedleExposure.visibleFraction(r, config.exposureSamples), 0f)
            assertFalse(NeedleExposure.canFind(r, r.needle.x, r.needle.y, config))
            for (p in r.clutter + r.needle) {
                val radius = hypot(p.length, p.width) / 2f
                assertTrue(p.x - radius > 0f && p.x + radius < config.trayWidth)
                assertTrue(p.y - radius > 0f && p.y + radius < config.trayHeight)
            }
        }
    }

    @Test fun `same seed reproduces every dimension pose and tone`() {
        val a = RoundGenerator.generate(Long.MIN_VALUE, config)
        val b = RoundGenerator.generate(Long.MIN_VALUE, config)
        for ((p, q) in (a.clutter + a.needle).zip(b.clutter + b.needle)) {
            assertEquals(p.x, q.x, 0f); assertEquals(p.y, q.y, 0f); assertEquals(p.angle, q.angle, 0f)
            assertEquals(p.length, q.length, 0f); assertEquals(p.width, q.width, 0f); assertEquals(p.tone, q.tone)
        }
        assertNotEquals(a.needle.x, RoundGenerator.generate(8L, config).needle.x)
    }

    @Test fun `rotated visual footprint counts coverage independent of collision size`() {
        val p = SearchPiece(2f, 0.2f, 0, 3f, 4f, (PI / 2).toFloat())
        assertTrue(p.covers(3f, 4.8f)); assertFalse(p.covers(3.3f, 4f))
        assertTrue(p.covers(3.09f, 4.7f))
    }

    @Test fun `exposure threshold and uncovered local target are both required`() {
        val needle = SearchPiece(2f, 0.1f, 0, 5f, 7f, 0f)
        val cover = SearchPiece(0.9f, 1f, 0, 4.45f, 7f, 0f)
        val round = SearchRound(1, listOf(cover), needle)
        assertTrue(NeedleExposure.visibleFraction(round, config.exposureSamples) >= 0.5f)
        assertFalse(NeedleExposure.canFind(round, 4.3f, 7f, config))
        assertTrue(NeedleExposure.canFind(round, 5.6f, 7f, config))
        assertFalse(NeedleExposure.canFind(round, 5.6f, 7.5f, config))
        assertFalse(NeedleExposure.canFind(round, 7f, 7f, config))
        assertFalse(NeedleExposure.canFind(round, 5.6f, 7f, config.copy(discoveryFraction = 0.9f)))
    }

    @Test fun `one-frame-old cover blocks high refresh premature discovery`() {
        val round = RoundGenerator.generate(1, config)
        for (p in round.clutter) { p.x = 1f; p.y = 1f }
        assertEquals(1f, NeedleExposure.visibleFraction(round, 15), 0f)
        assertFalse(NeedleExposure.canFind(round, round.needle.x, round.needle.y, config))
        clear(round)
        assertTrue(NeedleExposure.canFind(round, round.needle.x, round.needle.y, config))
    }

    @Test fun `search found next is gated counts once and preserves session count`() {
        val s = SearchSession(config, Long.MAX_VALUE)
        assertFalse(s.next())
        s.update(3f); s.touch(); s.drag(2f)
        assertFalse(s.find(s.round.needle.x, s.round.needle.y))
        assertEquals(0, s.foundCount)
        clear(s.round)
        assertTrue(s.find(s.round.needle.x, s.round.needle.y))
        assertFalse(s.find(s.round.needle.x, s.round.needle.y))
        assertEquals(1, s.foundCount)
        assertEquals(3f, s.round.elapsed, 0f)
        assertFalse(s.next())
        s.update(config.successSeconds)
        assertTrue(s.next())
        assertNotEquals(Long.MAX_VALUE, s.round.seed)
        assertFalse(s.round.found); assertEquals(1, s.foundCount)
        assertEquals(0, s.round.touches); assertEquals(0f, s.round.dragDistance, 0f)
        assertEquals(0f, NeedleExposure.visibleFraction(s.round, 15), 0f)
    }

    @Test fun `invalid configuration is rejected`() {
        try { SearchConfig(clutterCount = 3); fail("Invalid pile") } catch (_: IllegalArgumentException) {}
    }
}

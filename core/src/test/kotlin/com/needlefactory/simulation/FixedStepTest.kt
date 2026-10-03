package com.needlefactory.simulation

import org.junit.Assert.*
import org.junit.Test

class FixedStepTest {
    @Test fun `30 60 and 120 Hz advance the same simulation duration`() {
        for (hz in listOf(30, 60, 120)) {
            val clock = FixedStep()
            var steps = 0
            repeat(hz * 10) { clock.advance(1f / hz) { steps++ } }
            assertEquals(600, steps)
            assertTrue(clock.alpha < 0.001f)
        }
    }
    @Test fun `half steps interpolate and reset discards lifecycle remainder`() {
        val clock = FixedStep()
        assertEquals(0, clock.advance(1f / 120f) {})
        assertEquals(0.5f, clock.alpha, 0.0001f)
        clock.reset()
        assertEquals(0f, clock.alpha, 0f)
        assertEquals(0, clock.advance(1f / 120f) {})
        assertEquals(1, clock.advance(1f / 120f) {})
    }
    @Test fun `debugger gap and invalid deltas cannot spiral`() {
        val clock = FixedStep()
        assertEquals(5, clock.advance(100f) {})
        assertTrue(clock.droppedSeconds > 99.8)
        assertTrue(clock.alpha in 0f..1f)
        assertEquals(0, clock.advance(Float.NaN) {})
        assertEquals(0, clock.advance(Float.POSITIVE_INFINITY) {})
        assertEquals(0, clock.advance(-1f) {})
    }
}

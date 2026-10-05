package com.needlefactory.presentation

import org.junit.Assert.*
import org.junit.Test

class NeedleLightingTest {
    @Test fun `filter settles equally at 30 60 and 120 Hz without overshoot`() {
        for (hz in listOf(30, 60, 120)) {
            val light = NeedleLighting()
            light.submit(.6f, -.4f)
            repeat(hz) { light.advance(1f / hz) }
            assertEquals(.6f * (1f - kotlin.math.exp(-1f / .18f)), light.x, .00001f)
            assertEquals(-.4f * (1f - kotlin.math.exp(-1f / .18f)), light.y, .00001f)
            val before = light.x
            light.submit(-1f, 1f)
            light.advance(1f / 120)
            assertTrue(light.x < before && light.x > .5f)
        }
    }

    @Test fun `display quarter turns remap sensor axes and invalid samples are ignored`() {
        val expected = listOf(.6f to -.4f, -.4f to -.6f, -.6f to .4f, .4f to .6f)
        for (turn in 0..3) {
            val light = NeedleLighting()
            light.submit(.6f, -.4f, turn)
            light.submit(Float.NaN, Float.POSITIVE_INFINITY, turn)
            repeat(120) { light.advance(1f / 60) }
            assertEquals(expected[turn].first, light.x, .0001f)
            assertEquals(expected[turn].second, light.y, .0001f)
        }
    }

    @Test fun `neutral fallback stays still and large tilt keeps reflection on shaft`() {
        val light = NeedleLighting()
        val neutral = light.strength(1f, 0f)
        repeat(600) { light.advance(1f / 60) }
        assertEquals(neutral, light.strength(1f, 0f), 0f)
        for (sx in listOf(-5f, 0f, 5f)) for (sy in listOf(-5f, 0f, 5f)) {
            light.submit(sx, sy)
            repeat(120) { light.advance(1f / 60) }
            for (a in 0..36) {
                val angle = a * .175f
                val c = kotlin.math.cos(angle); val s = kotlin.math.sin(angle)
                assertTrue(light.strength(c, s) in 0f..1f)
                assertTrue(light.center(c, s) - .20f >= -.5f)
                assertTrue(light.center(c, s) + .20f <= .34f)
            }
        }
    }

    @Test fun `visual width adds only one point two five actual pixels at any viewport scale`() {
        for (unitsPerPixel in listOf(400f / 1080, 480f / 1080, 400f / 720)) {
            val previous = .45f
            val actualPixels = (NeedleLighting.visualWidth(previous, unitsPerPixel) - previous) / unitsPerPixel
            assertEquals(1.25f, actualPixels, .00001f)
        }
    }
}

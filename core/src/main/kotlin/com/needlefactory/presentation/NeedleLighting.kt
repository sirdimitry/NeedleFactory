package com.needlefactory.presentation

import kotlin.math.abs
import kotlin.math.exp

/** Presentation-only tilt. One atomic, allocation-free sample crosses onto the GL thread. */
class NeedleLighting {
    @Volatile private var latest = 0L
    var x = 0f; private set
    var y = 0f; private set

    /** Device gravity projected onto display axes; quarterTurns follows Android Display.rotation. */
    fun submit(deviceX: Float, deviceY: Float, quarterTurns: Int = 0) {
        if (!deviceX.isFinite() || !deviceY.isFinite()) return
        val dx = deviceX.coerceIn(-1f, 1f)
        val dy = deviceY.coerceIn(-1f, 1f)
        val sx: Float
        val sy: Float
        when (quarterTurns and 3) {
            1 -> { sx = dy; sy = -dx }
            2 -> { sx = -dx; sy = -dy }
            3 -> { sx = -dy; sy = dx }
            else -> { sx = dx; sy = dy }
        }
        latest = (sx.toRawBits().toLong() shl 32) or (sy.toRawBits().toLong() and 0xffffffffL)
    }

    /** 180 ms exponential low-pass, independent of render cadence; no timer-driven shimmer. */
    fun advance(delta: Float) {
        if (!delta.isFinite() || delta <= 0f) return
        val sample = latest
        val targetX = Float.fromBits((sample shr 32).toInt())
        val targetY = Float.fromBits(sample.toInt())
        val blend = 1f - exp(-delta.coerceAtMost(.1f) / .18f)
        x += (targetX - x) * blend
        y += (targetY - y) * blend
    }

    // A broad cylindrical specular lobe responds to tilt across the shaft.
    fun strength(cosine: Float, sine: Float): Float {
        val alignment = (1f - abs(.35f - sine * x + cosine * y)).coerceIn(0f, 1f)
        return alignment * alignment
    }

    // Finite light source: the small reflection slides along, never outside, the shaft.
    fun center(cosine: Float, sine: Float): Float =
        ((cosine * x + sine * y) * .18f - .04f).coerceIn(-.22f, .14f)

    companion object {
        const val EXTRA_WIDTH_PIXELS = 1.25f
        fun visualWidth(previousWidth: Float, unitsPerPixel: Float): Float =
            previousWidth + EXTRA_WIDTH_PIXELS * unitsPerPixel
    }
}

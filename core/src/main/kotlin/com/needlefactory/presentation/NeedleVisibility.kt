package com.needlefactory.presentation

import com.needlefactory.simulation.FactoryItem
import kotlin.math.*

/** Straw occlusion at the exact interpolated/projection poses used by the renderer. Not a game rule. */
class NeedleVisibility(capacity: Int) {
    private val items = arrayOfNulls<FactoryItem>(capacity)
    private val cachedItems = arrayOfNulls<FactoryItem>(capacity)
    private val cachedRadius = FloatArray(capacity)
    private val px = FloatArray(capacity); private val py = FloatArray(capacity)
    private val cosine = FloatArray(capacity); private val sine = FloatArray(capacity)
    private val projection = FloatArray(capacity); private val widths = FloatArray(capacity)
    private val radius = FloatArray(capacity); private val candidates = IntArray(capacity)
    private var count = 0

    fun begin() { count = 0 }

    fun add(item: FactoryItem, x: Float, y: Float, angle: Float, projectedScale: Float, widthScale: Float) {
        val i = count++
        val id = item.id
        if (cachedItems[id] !== item) {
            var r = 0f
            for (f in item.strands) r = max(r, hypot(f.x, f.y) + f.length / 2 + abs(f.bend) + f.width)
            cachedRadius[id] = r; cachedItems[id] = item
        }
        items[i] = item; px[i] = x; py[i] = y
        cosine[i] = cos(angle); sine[i] = sin(angle)
        projection[i] = projectedScale; widths[i] = widthScale
        radius[i] = cachedRadius[id] * max(projectedScale, widthScale)
    }

    fun fraction(x: Float, y: Float, length: Float, angle: Float, edgePadding: Float = 0f): Float {
        var candidateCount = 0
        for (i in 0 until count) {
            val dx = x - px[i]; val dy = y - py[i]
            val r = radius[i] + length / 2 + edgePadding
            if (dx * dx + dy * dy <= r * r) candidates[candidateCount++] = i
        }
        val nc = cos(angle); val ns = sin(angle)
        var open = 0
        for (k in 0 until SAMPLES) {
            val along = (k.toFloat() / (SAMPLES - 1) - .5f) * length
            val sx = x + nc * along; val sy = y + ns * along
            var covered = false
            for (j in 0 until candidateCount) {
                val i = candidates[j]
                val dx = sx - px[i]; val dy = sy - py[i]
                val r = radius[i] + edgePadding
                if (dx * dx + dy * dy > r * r) continue
                val scale = projection[i]
                val lx = (cosine[i] * dx + sine[i] * dy) / scale
                val ly = (-sine[i] * dx + cosine[i] * dy) / scale
                val strands = items[i]!!.strands
                for (fIndex in strands.indices) {
                    val f = strands[fIndex]
                    val fx = f.cosine * (lx - f.x) + f.sine * (ly - f.y)
                    val fy = -f.sine * (lx - f.x) + f.cosine * (ly - f.y)
                    val t = fx / f.length
                    if (abs(t) > .5f) continue
                    val slope = -8 * f.bend * t / f.length
                    val offset = fy - f.bend * (1 - 4 * t * t)
                    val half = f.width * widths[i] / scale * .5f * min(1f, (.5f - abs(t)) * 12f) + edgePadding / scale
                    if (offset * offset <= half * half * (1 + slope * slope)) { covered = true; break }
                }
                if (covered) break
            }
            if (!covered) open++
        }
        return open.toFloat() / SAMPLES
    }

    companion object {
        const val SAMPLES = 31
        const val HIGHLIGHT_THRESHOLD = .60f
        fun highlightGain(visibility: Float): Float {
            if (!visibility.isFinite() || visibility <= HIGHLIGHT_THRESHOLD) return 0f
            // Late reveal earns the strongest finish; do not saturate at 75%.
            return when {
                visibility < .75f -> .32f * smooth((visibility - HIGHLIGHT_THRESHOLD) / .15f)
                visibility < .90f -> .32f + .38f * smooth((visibility - .75f) / .15f)
                else -> .70f + .30f * smooth((visibility - .90f) / .10f)
            }
        }
        private fun smooth(value: Float): Float {
            val t = value.coerceIn(0f, 1f)
            return t * t * (3 - 2 * t)
        }
    }
}

package com.needlefactory.simulation

import com.needlefactory.content.SearchConfig
import java.util.Random
import kotlin.math.*

/** Domain-owned poses. Physics publishes snapshots; rendering only reads them. */
class SearchPiece(val length: Float, val width: Float, val tone: Int,
                  var x: Float, var y: Float, var angle: Float) {
    var previousX = x
    var previousY = y
    var previousAngle = angle

    fun covers(px: Float, py: Float, previous: Boolean = false): Boolean {
        val a = if (previous) previousAngle else angle
        val dx = px - if (previous) previousX else x
        val dy = py - if (previous) previousY else y
        val c = cos(a); val s = sin(a)
        return abs(dx * c + dy * s) <= length / 2f && abs(-dx * s + dy * c) <= width / 2f
    }
}

class SearchRound(val seed: Long, val clutter: List<SearchPiece>, val needle: SearchPiece) {
    var elapsed = 0f
        internal set
    var touches = 0
        internal set
    var dragDistance = 0f
        internal set
    var found = false
        internal set
    var successAge = 0f
        internal set
}

object RoundGenerator {
    fun generate(seed: Long, config: SearchConfig): SearchRound {
        val rng = Random(seed)
        fun between(low: Float, high: Float) = low + rng.nextFloat() * (high - low)
        val needle = SearchPiece(1.3f, 0.1f, 0, between(2.5f, config.trayWidth - 2.5f),
            between(4f, config.trayHeight - 4f), between(-PI.toFloat(), PI.toFloat()))
        val pieces = ArrayList<SearchPiece>(config.clutterCount)
        // Deliberate cover: the first wide strip contains the entire needle axis.
        // Other cover pieces distribute the cover so one brush does not always solve it.
        repeat(9) { i ->
            val along = if (i == 0) 0f else between(-0.6f, 0.6f)
            val across = if (i == 0) 0f else between(-0.25f, 0.25f)
            pieces += SearchPiece(if (i == 0) 1.65f else between(1.1f, 1.9f),
                if (i == 0) 0.32f else between(0.2f, 0.32f), rng.nextInt(4),
                needle.x + along * cos(needle.angle) - across * sin(needle.angle),
                needle.y + along * sin(needle.angle) + across * cos(needle.angle),
                needle.angle + if (i == 0) 0f else between(-0.65f, 0.65f))
        }
        repeat(config.clutterCount - pieces.size) {
            pieces += SearchPiece(between(0.85f, 1.9f), between(0.16f, 0.32f), rng.nextInt(4),
                between(1.2f, config.trayWidth - 1.2f), between(2f, config.trayHeight - 2f),
                between(-PI.toFloat(), PI.toFloat()))
        }
        return SearchRound(seed, pieces, needle)
    }
}

object NeedleExposure {
    fun visibleFraction(round: SearchRound, samples: Int, previous: Boolean = false): Float {
        val n = round.needle
        val a = if (previous) n.previousAngle else n.angle
        val x = if (previous) n.previousX else n.x
        val y = if (previous) n.previousY else n.y
        var visible = 0
        for (i in 0 until samples) {
            val t = n.length * (i.toFloat() / (samples - 1) - 0.5f)
            if (!covered(round, x + cos(a) * t, y + sin(a) * t, previous)) visible++
        }
        return visible.toFloat() / samples
    }

    private fun covered(round: SearchRound, x: Float, y: Float, previous: Boolean): Boolean {
        for (p in round.clutter) if (p.covers(x, y, previous)) return true
        return false
    }

    fun canFind(round: SearchRound, x: Float, y: Float, config: SearchConfig): Boolean {
        if (round.found) return false
        // Conservative gate across both snapshots; a 120 Hz frame cannot expose a
        // covered needle merely because physics has advanced one frame ahead.
        for (previous in arrayOf(false, true)) {
            if (visibleFraction(round, config.exposureSamples, previous) < config.discoveryFraction) return false
            val n = round.needle
            val a = if (previous) n.previousAngle else n.angle
            val nx = if (previous) n.previousX else n.x
            val ny = if (previous) n.previousY else n.y
            val dx = x - nx; val dy = y - ny
            val along = dx * cos(a) + dy * sin(a)
            val across = -dx * sin(a) + dy * cos(a)
            val nearest = along.coerceIn(-n.length / 2f, n.length / 2f)
            if (hypot(along - nearest, across) > config.needleHitSlop + n.width / 2f) return false
            // Expanded touch target is allowed only over a visible part of the axis.
            if (covered(round, nx + cos(a) * nearest, ny + sin(a) * nearest, previous)) return false
        }
        return true
    }
}

class SearchSession(val config: SearchConfig, firstSeed: Long) {
    var round = RoundGenerator.generate(firstSeed, config)
        private set
    var foundCount = 0
        private set
    val canNext: Boolean get() = round.found && round.successAge >= config.successSeconds

    fun update(delta: Float) {
        if (round.found) round.successAge += delta else round.elapsed += delta
    }

    fun touch() { if (!round.found) round.touches++ }
    fun drag(distance: Float) { if (!round.found) round.dragDistance += distance }
    fun find(x: Float, y: Float): Boolean {
        if (!NeedleExposure.canFind(round, x, y, config)) return false
        round.found = true
        foundCount++
        return true
    }
    fun next(): Boolean {
        if (!canNext) return false
        round = RoundGenerator.generate(round.seed + 0x61C8864680B583EBL, config)
        return true
    }
}

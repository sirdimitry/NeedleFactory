package com.needlefactory.lwjgl3

import com.badlogic.gdx.*
import com.badlogic.gdx.backends.lwjgl3.*
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.needlefactory.game.*
import com.needlefactory.presentation.SearchScreen
import com.needlefactory.simulation.NeedleExposure
import kotlin.math.*

/** Actual GL/native world and screen input; accelerated time, synthetic gestures.
 * Uses no teleport/reveal cheat. Ten scripted rounds do not replace owner play. */
object PrototypeSmoke {
    @JvmStatic fun main(args: Array<String>) {
        val snapshots = mutableListOf<Pair<String, Pixmap>>()
        var haptics = 0
        val game = NeedleFactoryGame(BuildInfo("0.0.1", "debug", true), PlatformFeedback { haptics++ }, seed = 42L)
        lateinit var screen: SearchScreen
        var frame = 0
        var row = 0
        var pass = 0
        var sweepFrame = 0
        var waiting = 0
        var failure: Throwable? = null
        var activeRound = 0L
        fun capture(name: String) {
            snapshots += name to Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        }
        fun coordinates(x: Float, y: Float): Pair<Int, Int> {
            val scale = min(Gdx.graphics.width / 400f, Gdx.graphics.height / 760f)
            return ((Gdx.graphics.width - 400f * scale) / 2f + (25f + x * 35f) * scale).toInt() to
                (Gdx.graphics.height - ((Gdx.graphics.height - 760f * scale) / 2f + (160f + y * 35f) * scale)).toInt()
        }
        fun down(x: Float, y: Float) { val (sx, sy) = coordinates(x, y); check(Gdx.input.inputProcessor.touchDown(sx, sy, 0, 0)) }
        fun up(x: Float, y: Float) { val (sx, sy) = coordinates(x, y); check(Gdx.input.inputProcessor.touchUp(sx, sy, 0, 0)) }
        fun move(x: Float, y: Float) { val (sx, sy) = coordinates(x, y); check(Gdx.input.inputProcessor.touchDragged(sx, sy, 0)) }
        fun tap(x: Float, y: Float) { down(x, y); up(x, y) }
        val listener = object : ApplicationAdapter() {
            override fun create() {
                game.create()
                screen = game.screen as SearchScreen
            }
            override fun resize(width: Int, height: Int) = game.resize(width, height)
            override fun pause() = game.pause()
            override fun resume() = game.resume()
            override fun dispose() = game.dispose()
            override fun render() {
                try {
                    // Alternate half-step frames to exercise the 120 Hz interpolation path.
                    screen.render(1f / 120f)
                    if (!screen.isReady) return
                    frame++
                    check(frame < 150000) { "Smoke timed out" }
                    if (frame < 120) return
                    val r = screen.session.round
                    if (activeRound != r.seed) {
                        activeRound = r.seed; row = 0; pass = 0; sweepFrame = 0; waiting = 0
                        check(screen.bodyCount == 66)
                        check(NeedleExposure.visibleFraction(r, 15) == 0f)
                        tap(r.needle.x, r.needle.y)
                        check(!r.found) { "Blind tap won seed=${r.seed}" }
                        if (screen.session.foundCount == 0) capture("stage1-portrait-start.png")
                    }
                    if (frame == 180) {
                        val seed = r.seed; val elapsed = r.elapsed; val x = r.needle.x
                        game.pause(); screen.render(20f); game.resume()
                        check(screen.session.round.seed == seed && r.elapsed == elapsed && r.needle.x == x)
                        Gdx.graphics.setWindowedMode(960, 540)
                        sweepFrame = 0
                        return
                    }
                    if (frame == 210) { capture("stage1-landscape.png"); Gdx.graphics.setWindowedMode(480, 912); sweepFrame = 0; return }
                    if (frame in 181..211) return
                    if (r.found) {
                        waiting++
                        if (waiting == 100) {
                            check(screen.session.canNext)
                            if (screen.session.foundCount == 1) capture("stage1-found.png")
                            check(haptics == screen.session.foundCount)
                            if (screen.session.foundCount == 10) {
                                println("Stage 1 OpenGL smoke passed: 10 seeded rounds, blind-tap gate, physical sweeps, find, feedback, next, pause/resume and rotation. Frames=$frame bodies=${screen.bodyCount}")
                                Gdx.app.exit()
                            } else {
                                val scale = min(Gdx.graphics.width / 400f, Gdx.graphics.height / 760f)
                                val sx = (Gdx.graphics.width / 2f).toInt()
                                val sy = (Gdx.graphics.height - ((Gdx.graphics.height - 760f * scale) / 2f + 63f * scale)).toInt()
                                check(Gdx.input.inputProcessor.touchDown(sx, sy, 0, 0))
                                check(Gdx.input.inputProcessor.touchUp(sx, sy, 0, 0))
                                check(screen.session.round.seed != r.seed)
                            }
                        }
                    } else {
                        if (sweepFrame == 0) {
                            val n = r.needle
                            var foundTarget = false
                            for (i in 0..14) {
                                val along = n.length * (i / 14f - 0.5f)
                                val x = n.x + cos(n.angle) * along; val y = n.y + sin(n.angle) * along
                                if (NeedleExposure.canFind(r, x, y, screen.config)) {
                                    tap(x, y)
                                    if (r.found) { foundTarget = true; break }
                                }
                            }
                            if (foundTarget) return
                            if (row >= 27) { row = 0; pass++ }
                            check(pass < 3) { "Could not expose seed=${r.seed}" }
                            val y = if (pass % 2 == 0) 0.5f + row * 0.5f else 13.5f - row * 0.5f
                            down(if (row % 2 == 0) 0.4f else 9.6f, y)
                            // Bounded pusher must traverse a long target jump without tunnelling.
                            move(if (row % 2 == 0) 9.6f else 0.4f, y)
                        }
                        sweepFrame++
                        if (sweepFrame >= 130) {
                            val y = if (pass % 2 == 0) 0.5f + row * 0.5f else 13.5f - row * 0.5f
                            up(if (row % 2 == 0) 9.6f else 0.4f, y)
                            row++; sweepFrame = 0
                        }
                    }
                } catch (error: Throwable) { failure = error; Gdx.app.exit() }
            }
        }
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("NeedleFactory / Stage 1 smoke")
            setWindowedMode(480, 912); useVsync(false); setForegroundFPS(0); disableAudio(true)
        }
        try {
            Lwjgl3Application(listener, config)
            for ((name, pixmap) in snapshots) {
                val file = FileHandle("verification/$name"); file.parent().mkdirs()
                val png = PixmapIO.PNG()
                try { png.setFlipY(true); png.write(file, pixmap) } finally { png.dispose() }
            }
            failure?.let { throw it }
            check(haptics == 10 && snapshots.size == 3)
        } finally { snapshots.forEach { it.second.dispose() } }
    }
}

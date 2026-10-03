package com.needlefactory.presentation

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.ClasspathFileHandleResolver
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.needlefactory.content.SearchConfig
import com.needlefactory.game.BuildInfo
import com.needlefactory.game.PlatformFeedback
import com.needlefactory.physics.SearchPhysics
import com.needlefactory.simulation.*
import kotlin.math.*

/** Replaceable prototype geometry and a thin HUD; all game rules live outside it. */
class SearchScreen(private val build: BuildInfo, private val feedback: PlatformFeedback,
                   seed: Long) : ScreenAdapter() {
    val config = SearchConfig()
    val session = SearchSession(config, seed)
    private var physics = SearchPhysics(session.round, config)
    private val clock = FixedStep(config.stepSeconds, config.maxCatchUpSteps, config.maxFrameSeconds)
    private val viewport = FitViewport(400f, 760f)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val assets = AssetManager(ClasspathFileHandleResolver()).apply {
        load("com/badlogic/gdx/utils/lsans-15.fnt", BitmapFont::class.java)
    }
    private val point = Vector2()
    private val background = Color(0.055f, 0.075f, 0.09f, 1f)
    private val tray = Color(0.12f, 0.17f, 0.18f, 1f)
    private val rim = Color(0.24f, 0.3f, 0.3f, 1f)
    private val shadow = Color(0.04f, 0.06f, 0.055f, 1f)
    private val accent = Color(0.52f, 0.94f, 0.78f, 1f)
    private val muted = Color(0.65f, 0.74f, 0.75f, 1f)
    private val metal = Color(0.75f, 0.83f, 0.87f, 1f)
    private val straw = arrayOf(Color(0.73f, 0.49f, 0.23f, 1f), Color(0.85f, 0.65f, 0.34f, 1f),
        Color(0.64f, 0.42f, 0.21f, 1f), Color(0.91f, 0.75f, 0.45f, 1f))
    private var paused = false
    private var ignoreNextDelta = true
    private var ready = false
    val isReady: Boolean get() = ready
    private var activePointer = -1
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var travel = 0f
    private var hold = 0f
    private var candidate = false
    private var nextPressed = false
    private var metricsAge = 1f
    private var frameTotal = 0f
    private var frameCount = 0
    private var peakFrame = 0f
    private var logAge = 0f
    private var hud = ""
    private var seedText = ""
    private var metricsText = ""
    private var displayMetrics = ""
    private var roundText = ""
    private var exposure = 0f
    private val simulateStep: () -> Unit = { physics.step() }
    val bodyCount: Int get() = physics.bodyCount

    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (paused || !ready || activePointer != -1 || !insideViewport(screenX, screenY)) return false
            viewport.unproject(point.set(screenX.toFloat(), screenY.toFloat()))
            if (session.canNext && inNext(point.x, point.y)) {
                activePointer = pointer; nextPressed = true
                return true
            }
            if (session.round.found || !inTray(point.x, point.y)) return false
            activePointer = pointer
            nextPressed = false
            downX = worldX(point.x); downY = worldY(point.y)
            lastX = downX; lastY = downY; travel = 0f; hold = 0f
            session.touch()
            // Exposure is checked before a new pusher can disturb cover.
            candidate = NeedleExposure.canFind(session.round, downX, downY, config)
            if (!candidate) physics.begin(downX, downY)
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (pointer != activePointer || paused || nextPressed) return false
            viewport.unproject(point.set(screenX.toFloat(), screenY.toFloat()))
            val x = worldX(point.x).coerceIn(0f, config.trayWidth)
            val y = worldY(point.y).coerceIn(0f, config.trayHeight)
            val distance = hypot(x - lastX, y - lastY)
            travel += distance; session.drag(distance)
            lastX = x; lastY = y
            if (candidate && travel > config.tapMaxTravel) {
                candidate = false
                physics.begin(downX, downY)
            }
            physics.target(x, y)
            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (pointer != activePointer) return false
            viewport.unproject(point.set(screenX.toFloat(), screenY.toFloat()))
            if (nextPressed) {
                if (session.canNext && inNext(point.x, point.y) && insideViewport(screenX, screenY)) nextRound()
            } else {
                val releaseDistance = hypot(worldX(point.x) - downX, worldY(point.y) - downY)
                if (candidate && travel <= config.tapMaxTravel && releaseDistance <= config.tapMaxTravel &&
                    hold <= config.tapMaxSeconds && session.find(worldX(point.x), worldY(point.y))) found()
            }
            cancelInput()
            return true
        }

        override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (pointer != activePointer) return false
            cancelInput()
            return true
        }
    }

    private fun insideViewport(x: Int, y: Int): Boolean =
        x >= viewport.screenX && x < viewport.screenX + viewport.screenWidth &&
            Gdx.graphics.height - y >= viewport.screenY && Gdx.graphics.height - y < viewport.screenY + viewport.screenHeight
    private fun inTray(x: Float, y: Float) = x in 25f..375f && y in 160f..650f
    private fun inNext(x: Float, y: Float) = x in 70f..330f && y in 35f..91f
    private fun worldX(x: Float) = (x - 25f) / 35f
    private fun worldY(y: Float) = (y - 160f) / 35f

    private fun cancelInput() {
        physics.end()
        activePointer = -1; candidate = false; nextPressed = false
    }
    private fun found() {
        physics.end()
        physics.synchronizeSnapshots()
        feedback.found()
        if (build.diagnostics) {
            val r = session.round
            Gdx.app.log("NeedleFactory", "found seed=${r.seed} seconds=${r.elapsed} touches=${r.touches} " +
                "dragM=${r.dragDistance} bodies=$bodyCount fps=${Gdx.graphics.framesPerSecond} " +
                "frameMs=${if (frameCount > 0) frameTotal * 1000f / frameCount else 0f} peakMs=${peakFrame * 1000f}")
        }
        refreshHud()
    }

    private fun nextRound() {
        cancelInput()
        if (!session.next()) return
        physics.close()
        physics = SearchPhysics(session.round, config)
        clock.reset()
        peakFrame = 0f
        exposure = 0f; metricsAge = 1f; logAge = 0f; frameTotal = 0f; frameCount = 0
        roundText = "0s  0 touches  0.0m drag"
        logRound()
        refreshHud()
    }

    override fun show() {
        Gdx.input.inputProcessor = input
        logRound()
        refreshHud()
    }
    private fun logRound() {
        if (build.diagnostics) Gdx.app.log("NeedleFactory", "round seed=${session.round.seed} clutter=${config.clutterCount} bodies=$bodyCount")
    }
    override fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        cancelInput()
        clock.reset(); physics.synchronizeSnapshots()
        viewport.update(width, height, true)
        ignoreNextDelta = true
    }

    override fun render(delta: Float) {
        if (paused) return
        ready = assets.update()
        val frameDelta = if (!ignoreNextDelta && delta.isFinite()) delta.coerceAtLeast(0f) else 0f
        val dt = frameDelta.coerceAtMost(config.maxFrameSeconds)
        ignoreNextDelta = !ready
        if (ready) {
            session.update(frameDelta)
            if (activePointer != -1 && !nextPressed) {
                hold += dt
                if (candidate && hold > config.tapMaxSeconds) {
                    candidate = false; physics.begin(downX, downY); physics.target(lastX, lastY)
                }
            }
            if (!session.round.found) clock.advance(frameDelta, simulateStep)
        }
        if (build.diagnostics) {
            metricsAge += dt; logAge += dt
            frameTotal += frameDelta; frameCount++
            peakFrame = max(peakFrame, frameDelta)
            if (metricsAge >= 0.5f) {
                exposure = NeedleExposure.visibleFraction(session.round, config.exposureSamples)
                metricsText = "${Gdx.graphics.framesPerSecond} FPS  ${(frameTotal * 1000f / max(1, frameCount)).toInt()} ms  $bodyCount bodies"
                displayMetrics = "$metricsText   ${(exposure * 100f).toInt()}% visible"
                roundText = "${session.round.elapsed.toInt()}s  ${session.round.touches} touches  ${(session.round.dragDistance * 10f).toInt() / 10f}m drag"
                frameTotal = 0f; frameCount = 0; metricsAge = 0f
                refreshHud()
            }
            if (logAge >= 5f) {
                Gdx.app.log("NeedleFactory", "search seed=${session.round.seed} $metricsText $roundText " +
                    "exposure=${(exposure * 100).toInt()}% peakMs=${peakFrame * 1000f} droppedS=${clock.droppedSeconds} bounds=${physics.containmentCorrections}")
                logAge = 0f
            }
        }
        ScreenUtils.clear(background)
        viewport.apply()
        shapes.projectionMatrix = viewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        drawTray()
        if (!session.round.found) drawNeedle(clock.alpha, false)
        for (p in session.round.clutter) drawClutter(p, clock.alpha)
        if (session.round.found) drawNeedle(1f, true)
        if (physics.pushing) {
            shapes.color = muted
            val x = 25f + physics.pusherX * 35f; val y = 160f + physics.pusherY * 35f
            // Small contact marker is offset from the finger's centre, without hiding material.
            shapes.circle(x, y - config.pusherRadius * 35f - 4f, 2f, 12)
        }
        if (session.canNext) {
            shapes.color = if (nextPressed) muted else accent
            shapes.rect(70f, 35f, 260f, 56f)
        }
        shapes.end()
        if (ready) drawHud(assets.get("com/badlogic/gdx/utils/lsans-15.fnt", BitmapFont::class.java))
    }

    private fun drawTray() {
        shapes.color = shadow; shapes.rect(15f, 146f, 370f, 512f)
        shapes.color = rim; shapes.rect(17f, 152f, 366f, 506f)
        shapes.color = tray; shapes.rect(25f, 160f, 350f, 490f)
        shapes.color = muted
        shapes.circle(21f, 155f, 2f, 12); shapes.circle(379f, 155f, 2f, 12)
        shapes.circle(21f, 655f, 2f, 12); shapes.circle(379f, 655f, 2f, 12)
    }
    private fun interpolatedAngle(p: SearchPiece, alpha: Float): Float {
        val delta = atan2(sin(p.angle - p.previousAngle), cos(p.angle - p.previousAngle))
        return p.previousAngle + delta * alpha
    }
    private fun strip(x: Float, y: Float, length: Float, width: Float, radians: Float) {
        shapes.rect(x - length / 2f, y - width / 2f, length / 2f, width / 2f,
            length, width, 1f, 1f, radians * MathUtils.radiansToDegrees)
    }
    private fun drawClutter(p: SearchPiece, alpha: Float) {
        val x = 25f + (p.previousX + (p.x - p.previousX) * alpha) * 35f
        val y = 160f + (p.previousY + (p.y - p.previousY) * alpha) * 35f
        val angle = interpolatedAngle(p, alpha)
        shapes.color = shadow; strip(x + 1.5f, y - 2f, p.length * 35f, p.width * 35f, angle)
        shapes.color = straw[p.tone]; strip(x, y, p.length * 35f, p.width * 35f, angle)
        shapes.color = straw[(p.tone + 1) % straw.size]
        strip(x - sin(angle) * p.width * 7f, y + cos(angle) * p.width * 7f, p.length * 30f, 1.1f, angle)
    }
    private fun drawNeedle(alpha: Float, success: Boolean) {
        val p = session.round.needle
        val x = 25f + (p.previousX + (p.x - p.previousX) * alpha) * 35f
        val y = 160f + (p.previousY + (p.y - p.previousY) * alpha) * 35f
        val angle = interpolatedAngle(p, alpha)
        val beat = if (success) sin((session.round.successAge / config.successSeconds).coerceIn(0f, 1f) * PI.toFloat()) else 0f
        val length = p.length * 35f * (1f + beat * 0.18f)
        if (success) {
            shapes.color = accent
            val progress = (session.round.successAge / config.successSeconds).coerceIn(0f, 1f)
            if (progress < 1f) for (i in 0 until 12) {
                val a = i * PI.toFloat() / 6f
                val r = 15f + 45f * progress
                shapes.rectLine(x + cos(a) * r, y + sin(a) * r,
                    x + cos(a) * (r + 8f * (1f - progress)), y + sin(a) * (r + 8f * (1f - progress)), 2f)
            }
        }
        shapes.color = shadow; strip(x + 1.5f, y - 2f - beat * 3f, length, 4f, angle)
        shapes.color = if (success) accent else metal; strip(x, y + beat * 4f, length, 3.5f, angle)
        shapes.color = Color.WHITE; strip(x, y + beat * 4f, length * 0.9f, 1.1f, angle)
        val eyeX = x - cos(angle) * length * 0.39f
        val eyeY = y + beat * 4f - sin(angle) * length * 0.39f
        shapes.color = metal; shapes.circle(eyeX, eyeY, 3f, 12)
        shapes.color = tray; shapes.circle(eyeX, eyeY, 1.3f, 12)
    }

    private fun refreshHud() {
        hud = "Found: ${session.foundCount}"
        seedText = "Seed: ${session.round.seed}"
    }
    private fun drawHud(font: BitmapFont) {
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        font.color = Color.WHITE; font.data.setScale(1.7f)
        font.draw(batch, "NeedleFactory", 25f, 733f)
        font.color = accent; font.data.setScale(1.15f)
        font.draw(batch, hud, 275f, 730f)
        font.color = muted; font.data.setScale(1.05f)
        font.draw(batch, "Sweep the straw with your finger.", 25f, 696f)
        font.draw(batch, "Tap the needle when it is uncovered.", 25f, 675f)
        if (build.diagnostics) {
            font.data.setScale(0.86f)
            font.draw(batch, seedText, 25f, 139f)
            font.draw(batch, displayMetrics, 25f, 122f)
            font.draw(batch, roundText, 25f, 105f)
        }
        font.data.setScale(1.2f)
        if (session.canNext) {
            font.color = background; font.draw(batch, "Next pile", 160f, 72f)
        } else {
            font.color = if (session.round.found) accent else muted
            font.draw(batch, if (session.round.found) "Needle found!" else "Move gently. Make room to see.",
                if (session.round.found) 140f else 57f, 70f)
        }
        batch.end()
    }

    override fun pause() {
        paused = true; cancelInput(); clock.reset(); physics.synchronizeSnapshots()
        if (build.diagnostics) Gdx.app.log("NeedleFactory", "pause seed=${session.round.seed}")
    }
    override fun resume() {
        paused = false; ignoreNextDelta = true; clock.reset(); physics.synchronizeSnapshots()
        if (build.diagnostics) Gdx.app.log("NeedleFactory", "resume seed=${session.round.seed}")
    }
    override fun hide() {
        cancelInput()
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }
    override fun dispose() {
        hide(); physics.close(); shapes.dispose(); batch.dispose(); assets.dispose()
    }
}

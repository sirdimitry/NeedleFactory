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
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.needlefactory.debug.BootStatus
import com.needlefactory.game.BuildInfo

/** Replaceable Stage 0 diagnostic, deliberately without game objects or assets. */
class DiagnosticScreen(private val buildInfo: BuildInfo) : ScreenAdapter() {
    private val status = BootStatus()
    private val viewport = ScreenViewport()
    private val batch = SpriteBatch()
    private val shapes = ShapeRenderer()
    private val assets = AssetManager(ClasspathFileHandleResolver()).apply {
        // libGDX's async font/texture loaders read files on their worker thread.
        load("com/badlogic/gdx/utils/lsans-15.fnt", BitmapFont::class.java)
    }
    private val pointer = Vector2()
    private val background = Color(0.055f, 0.075f, 0.09f, 1f)
    private val panel = Color(0.13f, 0.19f, 0.22f, 1f)
    private val accent = Color(1f, 0.72f, 0.24f, 1f)
    private var metrics = "Waiting for first frame"
    private var viewportText = ""
    private var inputText = "Press anywhere to test input"
    private var lifecycleText = "Pauses: 0  Resumes: 0"
    private var metricsElapsed = 1f
    private var logElapsed = 0f
    private var frameSum = 0f
    private var frames = 0
    private var hasPointer = false
    private val buildText = "${buildInfo.version} / ${buildInfo.buildType}"

    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointerIndex: Int, button: Int): Boolean {
            if (status.paused) return false
            viewport.unproject(pointer.set(screenX.toFloat(), screenY.toFloat()))
            status.press()
            hasPointer = true
            inputText = "Presses: ${status.presses}  At: ${pointer.x.toInt()}, ${pointer.y.toInt()}"
            log("input $inputText")
            return true
        }
    }

    override fun show() {
        Gdx.input.inputProcessor = input
        log("boot ${buildInfo.version} ${buildInfo.buildType}")
    }

    override fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        // At least 360 logical units on the short edge; readable in either orientation.
        viewport.unitsPerPixel = 360f / minOf(width, height)
        viewport.update(width, height, true)
        hasPointer = false
        viewportText = "Viewport: $width x $height px"
        log("resize $viewportText")
    }

    override fun render(delta: Float) {
        if (status.paused) return
        val font = if (assets.update()) assets.get("com/badlogic/gdx/utils/lsans-15.fnt", BitmapFont::class.java) else null
        if (buildInfo.diagnostics) {
            metricsElapsed += delta
            logElapsed += delta
            frameSum += delta
            frames++
            if (metricsElapsed >= 0.5f) {
                val millis = if (frames > 0) frameSum * 1000f / frames else 0f
                metrics = "FPS: ${Gdx.graphics.framesPerSecond}  Frame: ${millis.toInt()} ms avg"
                metricsElapsed = 0f
                frameSum = 0f
                frames = 0
            }
            if (logElapsed >= 2f) {
                log("frame $metrics | $viewportText | $inputText | $lifecycleText")
                logElapsed = 0f
            }
        }

        ScreenUtils.clear(background)
        viewport.apply()
        val width = viewport.worldWidth
        val height = viewport.worldHeight
        shapes.projectionMatrix = viewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = panel
        shapes.rect(20f, 20f, width - 40f, 64f)
        shapes.color = accent
        shapes.rect(20f, height - 24f, width - 40f, 4f)
        if (hasPointer) shapes.circle(pointer.x, pointer.y, 12f, 24)
        shapes.end()

        if (font == null) return
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(1.65f)
        font.draw(batch, "NeedleFactory", 24f, height - 44f)
        font.data.setScale(1.1f)
        font.draw(batch, "Stage 0 / technical boot", 24f, height - 82f)
        font.draw(batch, "Renderer and font ready", 24f, height - 116f)
        font.draw(batch, inputText, 24f, height - 146f)
        font.draw(batch, lifecycleText, 24f, height - 176f)
        if (buildInfo.diagnostics) {
            font.color = accent
            font.draw(batch, metrics, 24f, height - 206f)
            font.draw(batch, viewportText, 24f, height - 230f)
            font.draw(batch, buildText, 24f, height - 254f)
        }
        font.color = Color.WHITE
        font.draw(batch, "Tap / click anywhere", 32f, 64f)
        font.draw(batch, "Rotate, resize, leave and return", 32f, 40f)
        batch.end()
    }

    override fun pause() {
        status.pause()
        updateLifecycle()
        log("pause $lifecycleText")
    }

    override fun resume() {
        status.resume()
        metricsElapsed = 1f
        frameSum = 0f
        frames = 0
        updateLifecycle()
        log("resume $lifecycleText")
    }

    private fun updateLifecycle() {
        lifecycleText = "Pauses: ${status.pauses}  Resumes: ${status.resumes}"
    }

    private fun log(message: String) {
        if (buildInfo.diagnostics) Gdx.app.log("NeedleFactory", message)
    }

    override fun hide() {
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        hide()
        batch.dispose()
        shapes.dispose()
        assets.dispose()
    }
}

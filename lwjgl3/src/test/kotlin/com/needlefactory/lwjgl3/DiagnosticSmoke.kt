package com.needlefactory.lwjgl3

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.needlefactory.game.BuildInfo
import com.needlefactory.game.NeedleFactoryGame

/** Opt-in backend smoke test; input/lifecycle callbacks here are synthetic. */
object DiagnosticSmoke {
    @JvmStatic fun main(args: Array<String>) {
        val snapshots = mutableListOf<Pair<String, Pixmap>>()
        val game = NeedleFactoryGame(BuildInfo("0.0.1", "debug", true), diagnostic = true)
        var frames = 0
        var failure: Throwable? = null
        val listener = object : ApplicationAdapter() {
            override fun create() = game.create()
            override fun resize(width: Int, height: Int) = game.resize(width, height)
            override fun pause() = game.pause()
            override fun resume() = game.resume()
            override fun dispose() = game.dispose()
            override fun render() {
                try {
                    game.render()
                    frames++
                    when (frames) {
                        120 -> {
                            check(Gdx.graphics.width == 960 && Gdx.graphics.height == 640)
                            snapshots += "desktop-landscape.png" to Pixmap.createFromFrameBuffer(0, 0, 960, 640)
                            Gdx.graphics.setWindowedMode(640, 960)
                        }
                        180 -> {
                            check(Gdx.graphics.width == 640 && Gdx.graphics.height == 960)
                            check(Gdx.input.inputProcessor.touchDown(320, 480, 0, 0))
                            game.pause()
                            game.resume()
                        }
                        240 -> {
                            snapshots += "desktop-portrait.png" to Pixmap.createFromFrameBuffer(0, 0, 640, 960)
                            Gdx.app.exit()
                        }
                    }
                } catch (error: Throwable) {
                    failure = error
                    Gdx.app.exit()
                }
            }
        }
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("NeedleFactory / diagnostic smoke")
            setWindowedMode(960, 640)
            useVsync(true)
            setForegroundFPS(60)
            disableAudio(true)
        }
        try {
            Lwjgl3Application(listener, config)
            failure?.let { throw it }
            check(frames >= 240 && snapshots.size == 2) { "Smoke test ended before completion" }
            // File I/O after the render loop has stopped, never on the render thread.
            for ((name, pixmap) in snapshots) {
                val file = FileHandle("verification/$name")
                file.parent().mkdirs()
                val png = PixmapIO.PNG()
                try { png.setFlipY(true); png.write(file, pixmap) } finally { png.dispose() }
            }
            println("Desktop OpenGL smoke passed: landscape, portrait, input and lifecycle callbacks; 240 frames.")
        } finally {
            snapshots.forEach { it.second.dispose() }
        }
    }
}

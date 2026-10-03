package com.needlefactory.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.needlefactory.game.BuildInfo
import com.needlefactory.game.NeedleFactoryGame

object DesktopLauncher {
    @JvmStatic fun main(args: Array<String>) {
        val diagnostics = System.getProperty("needlefactory.diagnostics", "true").toBoolean()
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("NeedleFactory / Stage 0")
            setWindowedMode(960, 640)
            setWindowSizeLimits(360, 360, -1, -1)
            useVsync(true)
            setForegroundFPS(60)
            setIdleFPS(30)
            disableAudio(true)
        }
        Lwjgl3Application(NeedleFactoryGame(BuildInfo("0.0.1", if (diagnostics) "debug" else "release", diagnostics)), config)
    }
}

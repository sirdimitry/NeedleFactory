package com.needlefactory.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.needlefactory.game.BuildInfo
import com.needlefactory.game.NeedleFactoryGame

object DesktopLauncher {
    @JvmStatic fun main(args: Array<String>) {
        val diagnostics = System.getProperty("needlefactory.diagnostics", "true").toBoolean()
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("NeedleFactory / Stage 3")
            setWindowedMode(480, 880)
            setWindowSizeLimits(360, 360, -1, -1)
            useVsync(true)
            setForegroundFPS(60)
            setIdleFPS(30)
            disableAudio(false)
        }
        Lwjgl3Application(NeedleFactoryGame(
            BuildInfo("0.0.1", if (diagnostics) "debug" else "release", diagnostics),
            diagnostic = System.getProperty("needlefactory.screen") == "diagnostic",
            search = System.getProperty("needlefactory.screen") == "search",
            seed = System.getProperty("needlefactory.seed", "").toLongOrNull() ?: System.nanoTime(),
            factorySession=com.needlefactory.simulation.FactorySession(com.needlefactory.content.FactoryConfig.pitProducts()),
            saveFile = java.io.File("../verification/desktop-workshop-v1.json")), config)
    }
}

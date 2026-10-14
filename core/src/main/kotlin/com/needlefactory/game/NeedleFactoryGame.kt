package com.needlefactory.game

import com.badlogic.gdx.Game
import com.needlefactory.presentation.DiagnosticScreen
import com.needlefactory.presentation.SearchScreen
import com.needlefactory.presentation.FactoryScreen
import com.needlefactory.presentation.NeedleLighting
import com.needlefactory.simulation.FactorySession
import com.needlefactory.content.FactoryConfig
import com.needlefactory.save.*
import java.io.File

class NeedleFactoryGame(private val buildInfo: BuildInfo,
                        private val feedback: PlatformFeedback = PlatformFeedback.NONE,
                        private val diagnostic: Boolean = false,
                        private val seed: Long = System.nanoTime(),
                        private val search: Boolean = false,
                        val productProgression:Boolean=true,
                        val factorySession: FactorySession = FactorySession(if(productProgression) FactoryConfig.grabProducts()
                            else FactoryConfig(needleCount=3), firstSeed = seed),
                        saveFile: File? = null,
                        val workshopRuntime: WorkshopRuntime? = saveFile?.let {
                            WorkshopRuntime(WorkshopPersistence(WorkshopStore(it),seed),seed,productProgression,factorySession.config.pitReceiver)
                        }) : Game() {
    val needleLighting = NeedleLighting()
    private var safeArea = SafeArea()
    fun updateSafeArea(area: SafeArea) {
        safeArea = area
        (screen as? FactoryScreen)?.setSafeArea(area)
    }
    override fun create() {
        setScreen(if (diagnostic) DiagnosticScreen(buildInfo) else if (search) SearchScreen(buildInfo, feedback, seed)
            else FactoryScreen(buildInfo, feedback, factorySession, needleLighting,workshopRuntime))
        (screen as? FactoryScreen)?.setSafeArea(safeArea)
    }

    override fun dispose() {
        val current = screen
        // Game.dispose() calls hide(); do that while native screen resources live.
        super.dispose()
        current?.dispose()
    }
}

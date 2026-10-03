package com.needlefactory.game

import com.badlogic.gdx.Game
import com.needlefactory.presentation.DiagnosticScreen
import com.needlefactory.presentation.SearchScreen

class NeedleFactoryGame(private val buildInfo: BuildInfo,
                        private val feedback: PlatformFeedback = PlatformFeedback.NONE,
                        private val diagnostic: Boolean = false,
                        private val seed: Long = System.nanoTime()) : Game() {
    override fun create() {
        setScreen(if (diagnostic) DiagnosticScreen(buildInfo) else SearchScreen(buildInfo, feedback, seed))
    }

    override fun dispose() {
        val current = screen
        // Game.dispose() calls hide(); do that while native screen resources live.
        super.dispose()
        current?.dispose()
    }
}

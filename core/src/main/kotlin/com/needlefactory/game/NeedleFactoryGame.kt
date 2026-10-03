package com.needlefactory.game

import com.badlogic.gdx.Game
import com.needlefactory.presentation.DiagnosticScreen

class NeedleFactoryGame(private val buildInfo: BuildInfo) : Game() {
    override fun create() {
        setScreen(DiagnosticScreen(buildInfo))
    }

    override fun dispose() {
        screen?.dispose()
        super.dispose()
    }
}

package com.needlefactory.game

/** Window-pixel obstructions. The surface/background stays full size; only content avoids them. */
data class SafeArea(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0)

package com.needlefactory.game

fun interface PlatformFeedback {
    fun found()
    fun strawImpact() = found()
    fun needleLost() = found()
    companion object { val NONE = PlatformFeedback {} }
}

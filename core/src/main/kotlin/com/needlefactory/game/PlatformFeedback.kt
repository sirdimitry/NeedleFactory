package com.needlefactory.game

fun interface PlatformFeedback {
    fun found()
    companion object { val NONE = PlatformFeedback {} }
}

package com.needlefactory.game

/** Immutable platform-supplied metadata; core does not know Android BuildConfig. */
data class BuildInfo(val version: String, val buildType: String, val diagnostics: Boolean)

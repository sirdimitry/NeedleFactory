package com.needlefactory.content

/** Stage 1 feel knobs, in Box2D metres and seconds. No economy values. */
data class SearchConfig(
    val clutterCount: Int = 60,
    val trayWidth: Float = 10f,
    val trayHeight: Float = 14f,
    val pusherRadius: Float = 0.38f,
    val pusherSpeed: Float = 10f,
    val pusherDensity: Float = 8f,
    val clutterDamping: Float = 5f,
    val angularDamping: Float = 7f,
    val clutterDensity: Float = 0.65f,
    val needleDensity: Float = 32f,
    val needleDamping: Float = 12f,
    val maxClutterSpeed: Float = 8f,
    val maxNeedleSpeed: Float = 1.5f,
    val maxAngularSpeed: Float = 5f,
    val fixtureScale: Float = 0.75f,
    val exposureSamples: Int = 15,
    val discoveryFraction: Float = 0.5f,
    val needleHitSlop: Float = 0.24f,
    val tapMaxTravel: Float = 0.28f,
    val tapMaxSeconds: Float = 0.45f,
    val successSeconds: Float = 0.7f,
    val stepSeconds: Float = 1f / 60f,
    val maxCatchUpSteps: Int = 5,
    val maxFrameSeconds: Float = 0.1f,
) {
    init {
        require(clutterCount in 12..100)
        require(trayWidth >= 8f && trayHeight >= 12f)
        require(exposureSamples >= 3 && discoveryFraction in 0.4f..1f)
        require(stepSeconds > 0f && maxCatchUpSteps > 0 && maxFrameSeconds >= stepSeconds)
        require(pusherRadius > 0f && pusherSpeed > 0f && fixtureScale in 0.1f..1f)
    }
}

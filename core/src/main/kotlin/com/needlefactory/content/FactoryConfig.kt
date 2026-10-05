package com.needlefactory.content

/** Stage 2 tunables. Lengths use the same metres as the Stage 1 needle. */
data class FactoryConfig(
    val strawCount: Int = 320,
    val width: Float = 10f,
    val height: Float = 8f,
    val needleLength: Float = 1.3f,
    val needleWidth: Float = .018f,
    val feedInterval: Float = .115f,
    val pusherRadius: Float = .62f,
    val pusherSpeed: Float = 10f,
    val beltSeconds: Float = 5f,
    val captureSeconds: Float = 1.1f,
    val chuteSeconds: Float = 1.2f,
    val packSeconds: Float = 1.8f,
    val stepSeconds: Float = 1f / 60f
) {
    init {
        require(strawCount in 24..360 && width >= 8f && height >= 6f)
        require(needleLength == 1.3f && needleWidth in .015f.. .04f)
        require(feedInterval > 0 && beltSeconds > 0 && captureSeconds > 0 && chuteSeconds > 0 && packSeconds > 0)
        require(stepSeconds > 0)
        require(pusherRadius in .3f.. .8f && pusherSpeed > 0f)
    }
}

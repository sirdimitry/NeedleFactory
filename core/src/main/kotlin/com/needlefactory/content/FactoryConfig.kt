package com.needlefactory.content

/** Stage 2 tunables. Lengths use the same metres as the Stage 1 needle. */
data class FactoryConfig(
    val strawCount: Int = 320,
    val needleCount: Int = 1,
    val width: Float = 10f,
    val height: Float = 8f,
    val needleLength: Float = 1.3f,
    val needleWidth: Float = .018f,
    val feedInterval: Float = .115f,
    val pusherRadius: Float = .62f,
    val pusherSpeed: Float = 10f,
    val releasedDamping: Float = 18f,
    val rakeContactBudget: Int = 24,
    val rakeCrowdingRadius: Float = .28f,
    val rakeCrowdingSpeed: Float = .45f,
    val manualProducts: Boolean = false,
    val strandsPerPatch: Int = 12,
    val grabClumps: Boolean = false,
    val grabRadius: Float = .28f,
    val grabFrequency: Float = 7f,
    val grabAcceleration: Float = 85f,
    val physicalNeedles:Boolean=false,
    val pitReceiver:Boolean=false,
    val clumpDensity:Int=1,
    val intakeMinY:Float=0f,
    val needleGrabAcceleration:Float=38f,
    val needleGrabFrequency:Float=4f,
    val metalDockX:Float=2.6f,
    val metalDockHalfWidth:Float=.65f,
    val productTransitSeconds:Float=1.2f,
    val conveyorIntakeX:Float=7.4f,
    val conveyorLaneX:Float=8.55f,
    val conveyorDischargeY:Float=.6f,
    val productBeltSeconds:Float=4f,
    val receiverSeconds: Float = .55f,
    val balerSeconds: Float = 1.1f,
    val balerUnitsPerCycle: Int = 12,
    val beltSeconds: Float = 5f,
    val captureSeconds: Float = 1.1f,
    val chuteSeconds: Float = 1.2f,
    val packSeconds: Float = 1.8f,
    val stepSeconds: Float = 1f / 60f
) {
    init {
        require(strawCount in 24..360 && width >= 8f && height >= 6f)
        require(needleCount in 1..3)
        require(needleLength == 1.3f && needleWidth in .015f.. .04f)
        require(feedInterval > 0 && beltSeconds > 0 && captureSeconds > 0 && chuteSeconds > 0 && packSeconds > 0)
        require(stepSeconds > 0)
        require(pusherRadius in .3f.. .8f && pusherSpeed > 0f)
        require(releasedDamping > 0f && rakeContactBudget in 2..32 && rakeCrowdingRadius > 0f && rakeCrowdingSpeed >= 0f)
        require(strandsPerPatch in 3..40 && receiverSeconds>0 && balerSeconds>0 && balerUnitsPerCycle in 1..24)
        require(!grabClumps || manualProducts)
        require(grabRadius>0 && grabFrequency>0 && grabAcceleration>0)
        require(!physicalNeedles || grabClumps)
        require(!pitReceiver || physicalNeedles)
        require(clumpDensity in 1..4)
        require(needleGrabAcceleration>0 && needleGrabFrequency>0 && productTransitSeconds>0)
        require(conveyorIntakeX>width/2 && conveyorLaneX>conveyorIntakeX && conveyorLaneX<width)
        require(conveyorDischargeY in .1f..1f && productBeltSeconds>0)
        require(metalDockX>metalDockHalfWidth && metalDockX+metalDockHalfWidth<width)
    }
    companion object {
        fun products() = FactoryConfig(strawCount=120,needleCount=3,manualProducts=true,strandsPerPatch=32)
        fun grabProducts() = products().copy(strawCount=24,strandsPerPatch=5,grabClumps=true,physicalNeedles=true)
        fun pitProducts() = grabProducts().copy(width=12.4f,height=9.5f,pitReceiver=true,clumpDensity=4,intakeMinY=-12.7f,metalDockX=11.1f,metalDockHalfWidth=.55f)
        const val PIT_X=1.35f
        const val PIT_Y=-11.6f
        const val PIT_HALF=.82f
        const val METAL_Y=-11.6f
    }
}

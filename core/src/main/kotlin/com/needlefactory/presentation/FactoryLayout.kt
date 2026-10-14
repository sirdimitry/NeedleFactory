package com.needlefactory.presentation

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.needlefactory.simulation.MachineId

/** Preset mounting metadata in canvas world units, never pixels or economic state. */
data class FactoryMount(val machine: MachineId,val anchor: Vector2,val footprint: Rectangle,
                        val input: Vector2,val output: Vector2)

/** Two authored compositions with shared machine ports and fixed-size control targets. */
class FactoryLayout(val width: Float, val height: Float,val manualProducts:Boolean=false,val pitReceiver:Boolean=false) {
    val landscape = width >= height
    val unit = 1f
    private val moduleOffset = if(landscape) ((width-814f)/2).coerceAtLeast(0f) else 0f
    val console = if(landscape) Rectangle(width-200f,16f,184f,height-32f)
        else Rectangle(0f,0f,width,if(pitReceiver) 136f else 164f)
    val handTool=if(landscape) Rectangle(console.x+10,console.y+console.height-58,164f,26f)
        else Rectangle(width-178,height-58,130f,24f)
    val table = if(pitReceiver) Rectangle(19f,if(landscape) height-251.6f else 140f+12.7f*24.8f,12.4f*24.8f,9.5f*24.8f)
        else if(landscape) Rectangle(30f+moduleOffset,height-214.4f,248f,198.4f)
        else Rectangle(30f,if(pitReceiver) maxOf(height-380f,172f+12.7f*24.8f) else height-380f,248f,198.4f)
    // A full-height receiving apron connects the entire physical feed edge.
    val start = Vector2(table.x+table.width+48f*unit,table.y+table.height/2)
    val end = if (landscape) Vector2(minOf(width,814f)-274f+moduleOffset,start.y) else Vector2(start.x,230f)
    val magnet = Vector2(start).lerp(end,(.55f-HANDOFF)/(DISCHARGE-HANDOFF))
    val capture = if(landscape) Vector2(magnet.x,150f) else Vector2(magnet.x-93f,magnet.y-4f)
    val buffer = if(landscape) Vector2(end.x-24f,80f) else Vector2(183f,312f)
    val packer = if(landscape) Vector2(buffer.x-116f,80f) else Vector2(183f,237f)
    val output = if(landscape) Vector2(packer.x-87f,80f) else Vector2(68f,237f)
    val residue = if(landscape) Vector2(end.x,height-46f) else Vector2(end.x,202f)
    val missed = if(landscape) Vector2(end.x-70f,height-46f) else Vector2(end.x-69f,202f)
    val floor = if(landscape) Rectangle(14f,29f,width-232f,height-37f)
        else Rectangle(14f,if(pitReceiver) 140f else 172f,width-28f,height-if(pitReceiver) 264f else 296f)
    val run = if(landscape && pitReceiver) Rectangle(console.x+10f,console.y+8f,78f,52f) else if(landscape) Rectangle(console.x+10f,console.y+72f,164f,52f) else Rectangle(16f,if(pitReceiver) 76f else 84f,132f,if(pitReceiver) 44f else 52f)
    val next = if(landscape && pitReceiver) Rectangle(console.x+96f,console.y+8f,78f,52f) else if(landscape) Rectangle(console.x+10f,console.y+8f,164f,52f) else Rectangle(160f,if(pitReceiver) 76f else 84f,width-176f,if(pitReceiver) 44f else 52f)
    val controls = Array(if(pitReceiver) 3 else 4) { i ->
        if(pitReceiver) {
            if(i==2) if(landscape) Rectangle(console.x+10,console.y+88,164f,52f)
                else Rectangle(16f,12f,width-32f,60f)
            else Rectangle()
        } else if(landscape) {
            Rectangle(console.x+10f+(i%2)*86f,console.y+128f+(1-i/2)*68f,78f,60f)
        } else {
            val count=if(pitReceiver) 3 else 4
            Rectangle(16f+i*(width-32f)/count,12f,(width-32f)/count-6f,60f)
        }
    }
    val productReceiver=Rectangle(table.x+table.width+2,table.y,30f,table.height)
    val materialScale=24.8f
    val yardAngle get()=if(pitReceiver && landscape) Math.PI.toFloat()/2 else 0f
    fun yardPoint(x:Float,y:Float,result:Vector2):Vector2 =
        if(pitReceiver && landscape) result.set(table.x+(9.5f-y)*materialScale,floor.y+4+x*materialScale)
        else result.set(table.x+x*materialScale,table.y+y*materialScale)
    fun yardCoordinates(x:Float,y:Float,result:Vector2):Vector2 =
        if(pitReceiver && landscape) result.set((y-floor.y-4)/materialScale,9.5f-(x-table.x)/materialScale)
        else result.set((x-table.x)/materialScale,(y-table.y)/materialScale)
    private fun yardRect(x:Float,y:Float,w:Float,h:Float):Rectangle {
        val a=yardPoint(x,y,Vector2()); val b=yardPoint(x+w,y+h,Vector2())
        return Rectangle(minOf(a.x,b.x),minOf(a.y,b.y),kotlin.math.abs(b.x-a.x),kotlin.math.abs(b.y-a.y))
    }
    val yard=yardRect(0f,-12.7f,if(pitReceiver) 12.4f else 10f,if(pitReceiver) 22.2f else 20.7f)
    val pileBin=yardRect(4/24.8f,64/24.8f,154/24.8f,(table.height-68)/24.8f)
    val pitHole=yardRect(com.needlefactory.content.FactoryConfig.PIT_X-com.needlefactory.content.FactoryConfig.PIT_HALF,
        com.needlefactory.content.FactoryConfig.PIT_Y-com.needlefactory.content.FactoryConfig.PIT_HALF,
        com.needlefactory.content.FactoryConfig.PIT_HALF*2,com.needlefactory.content.FactoryConfig.PIT_HALF*2)
    fun updatePit(width:Float) { pitHole.set(yardRect(com.needlefactory.content.FactoryConfig.PIT_X-com.needlefactory.content.FactoryConfig.PIT_HALF,com.needlefactory.content.FactoryConfig.PIT_Y-com.needlefactory.content.FactoryConfig.PIT_HALF,width,com.needlefactory.content.FactoryConfig.PIT_HALF*2)) }
    val needleReceiver=yardRect(11.1f-16/24.8f,com.needlefactory.content.FactoryConfig.METAL_Y-16/24.8f,32/24.8f,32/24.8f)
    val productBaler=if(landscape) Vector2(table.x+table.width+104,table.y+table.height/2-12)
        else Vector2(table.x+table.width-5,table.y-83)
    val productBalerInput=if(landscape) Vector2(productBaler.x-48,productBaler.y+5)
        else Vector2(productBaler.x,productBaler.y+38)
    val productBalerOutput=Vector2(productBaler.x+if(landscape) 55 else -55,productBaler.y-5)
    val productTray=Vector2(productBaler.x+if(landscape) 96 else -92,productBaler.y-5)
    val metalTray=if(landscape) Vector2(table.x+table.width-55,table.y-22) else Vector2(table.x+65,table.y-44)
    // Shared gameplay port at x=2.6 m; presentation chute reaches the authored tray.
    val metalDock=Vector2(table.x+table.width*.26f,table.y)
    // A screw discharges from its actual LOWER bearing in either composition.
    // This port is the authored attachment for later conveyor/receiver extensions.
    val productTransferStart=Vector2(productReceiver.x+productReceiver.width/2,productReceiver.y+8)
    val productLooseOutlet=Vector2(productTransferStart.x,productTransferStart.y-18)
    val productReject=Vector2(productReceiver.x+productReceiver.width/2,
        productReceiver.y-17)
    // Stable gameplay lane; only its onward connector changes with orientation.
    fun productLaneX(gameplayX:Float)=table.x+table.width*gameplayX/10f
    fun productLaneY(gameplayY:Float)=table.y+table.height*gameplayY/8f
    fun productBeltEnd(x:Float,y:Float,result:Vector2)=result.set(productLaneX(x),productLaneY(y))
    /** Lined pipe turns below the front nameplate rather than crossing its lettering. */
    fun productMetalPath(x:Float,y:Float,t:Float,result:Vector2):Vector2 {
        val lower=table.y-30
        val a=kotlin.math.abs(y-lower); val b=kotlin.math.abs(x-metalTray.x); val c=kotlin.math.abs(lower-metalTray.y)
        val distance=t.coerceIn(0f,1f)*(a+b+c)
        return when {
            distance<=a->result.set(x,y+(lower-y)*distance/a.coerceAtLeast(.001f))
            distance<=a+b->result.set(x+(metalTray.x-x)*(distance-a)/b.coerceAtLeast(.001f),lower)
            else->result.set(metalTray.x,lower+(metalTray.y-lower)*(distance-a-b)/c.coerceAtLeast(.001f))
        }
    }
    val balerFootprint=Rectangle(productBaler.x-52,productBaler.y-35,104f,74f)
    val telemetry = Rectangle(width-44f,height-40f,28f,28f)
    val statusX = if(landscape) console.x+10f else 29f
    val statusY = if(landscape) console.y+144f else if(pitReceiver) 132f else 158f
    val mounts = listOf(
        FactoryMount(MachineId.VIBRATION,Vector2(table.x+table.width/2,table.y+table.height/2),Rectangle(table),
            Vector2(table.x,table.y+table.height/2),Vector2(table.x+table.width,table.y+table.height/2)),
        FactoryMount(MachineId.CONVEYOR,Vector2(start).lerp(end,.5f),
            Rectangle(minOf(start.x,end.x)-26,minOf(start.y,end.y)-26,kotlin.math.abs(end.x-start.x)+52,kotlin.math.abs(end.y-start.y)+52),Vector2(start),Vector2(end)),
        FactoryMount(MachineId.SEPARATOR,Vector2(magnet),Rectangle(magnet.x-37,magnet.y-14,74f,57f),Vector2(magnet),Vector2(capture)),
        FactoryMount(MachineId.PACKER,Vector2(packer),Rectangle(packer.x-53,packer.y-34,106f,68f),Vector2(buffer),Vector2(output)))
    val guideHardpoints = arrayOf(Vector2(table.x+table.width,table.y),Vector2(table.x+table.width,table.y+table.height))
    val bufferFootprint = Rectangle(buffer.x-32,buffer.y-20,64f,40f)
    fun belt(t: Float,result: Vector2): Vector2 = result.set(start).lerp(end,
        ((t-HANDOFF)/(DISCHARGE-HANDOFF)).coerceIn(0f,1f))
    companion object {
        const val HANDOFF = .12f
        const val DISCHARGE = .90f
    }
}

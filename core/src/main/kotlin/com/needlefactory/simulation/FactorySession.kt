package com.needlefactory.simulation

import com.needlefactory.content.FactoryConfig
import java.util.Random
import kotlin.math.*

enum class MachineId { VIBRATION, CONVEYOR, SEPARATOR, PACKER }
enum class Material { STRAW, NEEDLE }
enum class Route { INTAKE, BELT, CAPTURE, CHUTE, BUFFER, PACKING, EJECT, OUTPUT, RESIDUE, MISSED;
    val terminal: Boolean get() = this == OUTPUT || this == RESIDUE || this == MISSED
}

/** Independent curved straws in a small crossing patch share one physical body. */
data class StrawStrand(val x:Float,val y:Float,val angle:Float,val length:Float,
                       val width:Float,val bend:Float,val tone:Int) {
    val cosine=cos(angle)
    val sine=sin(angle)
}
class FactoryItem(val id: Int, val material: Material, val length: Float, val width: Float,
                  val bend: Float, val tone: Int, var x: Float, var y: Float, var angle: Float,
                  val strands:List<StrawStrand> = emptyList()) {
    var vx = 0f; var vy = 0f; var angularVelocity = 0f
    var manuallyPushed = false; internal set
    /** Bounds of individual rotated strands, rather than an unnecessarily large rotated patch box. */
    fun extent(c:Float,s:Float,horizontal:Boolean):Float {
        if(material==Material.NEEDLE) return if(horizontal) abs(c)*length/2+abs(s)*width/2
            else abs(s)*length/2+abs(c)*width/2
        var result=0f
        for(f in strands) {
            val fc=c*f.cosine-s*f.sine; val fs=s*f.cosine+c*f.sine
            val offset=if(horizontal) c*f.x-s*f.y else s*f.x+c*f.y
            result=max(result,abs(offset)+(if(horizontal) abs(fc) else abs(fs))*f.length/2+
                (if(horizontal) abs(fs) else abs(fc))*(abs(f.bend)+f.width))
        }
        return result
    }
    var previousX = x; var previousY = y; var previousAngle = angle
    var route = Route.INTAKE; internal set
    var progress = 0f; internal set
    var previousProgress = 0f; internal set
    fun snapshot() {
        previousX = x; previousY = y; previousAngle = angle; previousProgress = progress
    }
}

class FactoryBatch(val seed: Long, val items: List<FactoryItem>) {
    var elapsed = 0f; internal set
    var touches = 0; internal set
    var dragDistance = 0f; internal set
    val complete: Boolean get() = items.all { it.route.terminal }
    fun count(route: Route): Int = items.count { it.route == route }
    val needle: FactoryItem get() = items.last()
}

object FactoryGenerator {
    fun generate(seed: Long, config: FactoryConfig): FactoryBatch {
        val r = Random(seed)
        val columns = ceil(sqrt(config.strawCount * config.width / config.height)).toInt()
        val rows = ceil(config.strawCount.toFloat() / columns).toInt()
        val items = ArrayList<FactoryItem>(config.strawCount + 1)
        for (i in 0 until config.strawCount) {
            var x = 1.1f + (config.width - 2.2f) * ((i % columns + r.nextFloat()) / columns)
            var y = 1.1f + (config.height - 2.2f) * ((i / columns + r.nextFloat()) / rows)
            // Irregular pockets superimposed on broad coverage, never a uniform grid or opaque mat.
            if(i%3==0) { x=x*.65f+2.8f*.35f; y=y*.65f+4.1f*.35f }
            val length=3f+r.nextFloat()*.9f; val width=.021f+r.nextFloat()*.009f
            val bend=(.08f+r.nextFloat()*.18f)*if(r.nextBoolean()) 1 else -1
            val tone=r.nextInt(4)
            val strands=ArrayList<StrawStrand>(12)
            strands.add(StrawStrand(0f,0f,0f,length,width,bend,tone))
            val edge=i%4==0
            repeat(11) {
                strands.add(StrawStrand((r.nextFloat()-.5f)*.8f,(r.nextFloat()-.5f)*1.1f,
                    (r.nextFloat()-.5f)*if(edge) 1.8f else PI.toFloat(),2.7f+r.nextFloat()*1.3f,
                    .021f+r.nextFloat()*.010f,(r.nextFloat()-.5f)*.40f,r.nextInt(4)))
            }
            var angle=(r.nextFloat()-.5f)*PI.toFloat()
            if(edge) {
                val side=(i/4)%4
                angle=(if(side<2) 0f else PI.toFloat()/2)+(r.nextFloat()-.5f)*.5f
                if(side<2) { x=2.5f+r.nextFloat()*(config.width-5f); y=if(side==0) .5f else config.height-.5f }
                else { x=if(side==2) .5f else config.width-.5f; y=2.5f+r.nextFloat()*(config.height-5f) }
            }
            val item=FactoryItem(i,Material.STRAW,length,width,bend,tone,x,y,
                angle,strands)
            val ex=item.extent(cos(item.angle),sin(item.angle),true)
            val ey=item.extent(cos(item.angle),sin(item.angle),false)
            item.x=item.x.coerceIn(ex,config.width-ex); item.y=item.y.coerceIn(ey,config.height-ey)
            item.snapshot(); items.add(item)
        }
        // The buried needle starts upstream in a dense pocket, not beside the early feed edge.
        val coverIndex = config.strawCount / 2
        val original = items[coverIndex]
        val angle=(r.nextFloat()-.5f)*.8f
        val coverStrands=original.strands.toMutableList()
        coverStrands[0]=StrawStrand(0f,0f,0f,3.8f,.036f,.012f,original.tone)
        val cover = FactoryItem(original.id, Material.STRAW, 3.8f, .036f, .012f,
            original.tone, 2.6f, config.height*.5f+(r.nextFloat()-.5f)*.7f, angle,coverStrands)
        val ex=cover.extent(cos(angle),sin(angle),true)
        val ey=cover.extent(cos(angle),sin(angle),false)
        cover.x=cover.x.coerceIn(ex,config.width-ex); cover.y=cover.y.coerceIn(ey,config.height-ey)
        cover.snapshot()
        items[coverIndex] = cover
        items.add(FactoryItem(config.strawCount, Material.NEEDLE, config.needleLength, config.needleWidth,
            0f, 0, cover.x - sin(cover.angle) * cover.bend,
            cover.y + cos(cover.angle) * cover.bend, cover.angle))
        return FactoryBatch(seed, items)
    }
}

/** Authoritative session survives screen/GL recreation. No rendering, native objects or persistence. */
class FactorySession(val config: FactoryConfig = FactoryConfig(), firstSeed: Long = 1L) {
    var batch = FactoryGenerator.generate(firstSeed, config); private set
    var batchNumber = 1; private set
    var recovered = 0; private set
    var missed = 0; private set
    var packaged = 0; private set
    var running = true; private set
    var conveyorSeconds = 0f; private set
    var feedGateAge = 0f; private set
    private val enabled = BooleanArray(MachineId.entries.size) { true }
    fun enabled(id: MachineId) = enabled[id.ordinal]
    fun operating(id: MachineId) = running && enabled(id)
    fun toggle(id: MachineId) { enabled[id.ordinal] = !enabled[id.ordinal] }
    fun toggleRunning() { running = !running; synchronizeSnapshots() }
    fun touch() { batch.touches++ }
    fun assist(distance: Float) {
        if (distance.isFinite()) batch.dragDistance += distance.coerceAtLeast(0f)
    }
    /** A real downstream-directed finger contact arms an intake item; idle vibration cannot. */
    fun manualPush(id: Int) {
        if (running && batch.items[id].route == Route.INTAKE) batch.items[id].manuallyPushed = true
    }
    /** Called only at the real intake gate by the native adapter; never by the renderer. */
    fun feed(id: Int): Boolean {
        if (!operating(MachineId.VIBRATION) || !operating(MachineId.CONVEYOR)) return false
        val item = batch.items[id]
        if (item.route != Route.INTAKE || !item.manuallyPushed) return false
        transition(item, Route.BELT)
        return true
    }
    fun gateFeed(id: Int): Boolean {
        if (feedGateAge < config.feedInterval || !feed(id)) return false
        feedGateAge = 0f
        return true
    }
    private fun transition(item: FactoryItem, route: Route) {
        item.route = route; item.progress = 0f; item.previousProgress = 0f
    }
    fun step() {
        if (!running || batch.complete) return
        val dt = config.stepSeconds
        batch.elapsed += dt
        if (operating(MachineId.CONVEYOR)) conveyorSeconds += dt
        if (operating(MachineId.VIBRATION) && operating(MachineId.CONVEYOR))
            feedGateAge = min(config.feedInterval,feedGateAge+dt)
        for (item in batch.items) {
            item.snapshot()
            when (item.route) {
                Route.BELT -> if (operating(MachineId.CONVEYOR)) {
                    val next = item.progress + dt / config.beltSeconds
                    if (item.material == Material.NEEDLE && item.progress < .55f && next >= .55f && enabled(MachineId.SEPARATOR)) {
                        transition(item, Route.CAPTURE); recovered++
                    } else if (next >= 1f) {
                        if (item.material == Material.STRAW) transition(item, Route.RESIDUE)
                        else { transition(item, Route.MISSED); missed++ }
                    } else item.progress = next
                }
                Route.CAPTURE -> if (operating(MachineId.CONVEYOR)) {
                    item.progress += dt / config.captureSeconds
                    if (item.progress >= 1f) transition(item, Route.CHUTE)
                }
                Route.CHUTE -> if (operating(MachineId.CONVEYOR)) {
                    item.progress += dt / config.chuteSeconds
                    if (item.progress >= 1f) transition(item, Route.BUFFER)
                }
                Route.BUFFER -> if (operating(MachineId.PACKER)) transition(item, Route.PACKING)
                Route.PACKING -> if (operating(MachineId.PACKER)) {
                    item.progress += dt / config.packSeconds
                    if (item.progress >= 1f) transition(item, Route.EJECT)
                }
                Route.EJECT -> if (operating(MachineId.CONVEYOR)) {
                    item.progress += dt
                    if (item.progress >= 1f) { transition(item, Route.OUTPUT); packaged++ }
                }
                else -> Unit
            }
        }
    }
    fun synchronizeSnapshots() { batch.items.forEach { it.snapshot() } }
    fun next(): Boolean {
        if (!batch.complete) return false
        batch = FactoryGenerator.generate(batch.seed + 0x61C8864680B583EBL, config)
        batchNumber++
        feedGateAge = 0f
        return true
    }
    fun summary(): String = "batch=$batchNumber seed=${batch.seed} seconds=${batch.elapsed} " +
        Route.entries.joinToString(" ") { "$it=${batch.count(it)}" } +
        " recovered=$recovered missed=$missed packaged=$packaged touches=${batch.touches} dragM=${batch.dragDistance} " +
        "feed=MANUAL armed=${batch.items.count { it.route==Route.INTAKE && it.manuallyPushed }} gateS=$feedGateAge " +
        MachineId.entries.joinToString(" ") { "$it=${enabled(it)}" }
}

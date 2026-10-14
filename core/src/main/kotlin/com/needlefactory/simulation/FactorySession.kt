package com.needlefactory.simulation

import com.needlefactory.content.FactoryConfig
import java.util.Random
import kotlin.math.*

enum class MachineId { VIBRATION, CONVEYOR, SEPARATOR, PACKER }
enum class Material { STRAW, NEEDLE }
enum class BatchPhase { ACTIVE, DRAINING, RESULTS }
enum class Route { INTAKE, BELT, CAPTURE, CHUTE, BUFFER, PACKING, EJECT, OUTPUT, RESIDUE, MISSED, DISCARDED,
    RECEIVING, BALER_QUEUE, BALING, LOOSE, BUNDLED, RECOVERED, TRANSFER, SORTED;
    val terminal: Boolean get() = this == OUTPUT || this == RESIDUE || this == MISSED || this == DISCARDED ||
        this==LOOSE || this==BUNDLED || this==RECOVERED || this==SORTED
}

/** Independent curved straws in a small crossing patch share one physical body. */
data class StrawStrand(val x:Float,val y:Float,val angle:Float,val length:Float,
                       val width:Float,val bend:Float,val tone:Int) {
    val cosine=cos(angle)
    val sine=sin(angle)
}
class FactoryItem(val id: Int, val material: Material, val length: Float, val width: Float,
                  val bend: Float, val tone: Int, var x: Float, var y: Float, var angle: Float,
                  val strands:List<StrawStrand> = emptyList(),val collisionStrandCount:Int=strands.size) {
    var vx = 0f; var vy = 0f; var angularVelocity = 0f
    var manuallyPushed = false; internal set
    /** Bounds of individual rotated strands, rather than an unnecessarily large rotated patch box. */
    fun extent(c:Float,s:Float,horizontal:Boolean,collisionOnly:Boolean=false):Float {
        if(material==Material.NEEDLE) return if(horizontal) abs(c)*length/2+abs(s)*width/2
            else abs(s)*length/2+abs(c)*width/2
        var result=0f
        for(fi in 0 until if(collisionOnly) collisionStrandCount else strands.size) {
            val f=strands[fi]
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
    val needles = items.filter { it.material == Material.NEEDLE }
    var elapsed = 0f; internal set
    var touches = 0; internal set
    var dragDistance = 0f; internal set
    val complete: Boolean get() = items.all { it.route.terminal }
    fun count(route: Route): Int = items.count { it.route == route }
    val needle: FactoryItem get() = needles.first()
}

object FactoryGenerator {
    fun generate(seed: Long, config: FactoryConfig): FactoryBatch {
        if(config.grabClumps) return com.needlefactory.content.StrawClumps.generate(seed,config)
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
            val strands=ArrayList<StrawStrand>(config.strandsPerPatch)
            strands.add(StrawStrand(0f,0f,0f,length,width,bend,tone))
            val edge=i%4==0
            repeat(config.strandsPerPatch-1) {
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
        for (n in 1 until config.needleCount) {
            val index = if (n == 1) config.strawCount / 4 else config.strawCount * 3 / 4
            val originalCover = items[index]
            val a = (r.nextFloat() - .5f) * .8f
            val strands = originalCover.strands.toMutableList()
            strands[0] = StrawStrand(0f,0f,0f,3.8f,.036f,.012f,originalCover.tone)
            val coverItem = FactoryItem(index,Material.STRAW,3.8f,.036f,.012f,originalCover.tone,
                config.width*.54f+(r.nextFloat()-.5f)*.3f,
                config.height*(if(n==1) .27f else .73f),a,strands)
            val cx=coverItem.extent(cos(a),sin(a),true); val cy=coverItem.extent(cos(a),sin(a),false)
            coverItem.x=coverItem.x.coerceIn(cx,config.width-cx)
            coverItem.y=coverItem.y.coerceIn(cy,config.height-cy)
            coverItem.snapshot(); items[index] = coverItem
            items.add(FactoryItem(config.strawCount+n,Material.NEEDLE,config.needleLength,config.needleWidth,
                0f,0,coverItem.x-sin(a)*coverItem.bend,coverItem.y+cos(a)*coverItem.bend,a))
        }
        return FactoryBatch(seed, items)
    }
}

/** Authoritative session survives screen/GL recreation. No rendering, native objects or persistence. */
class FactorySession(config: FactoryConfig = FactoryConfig(), firstSeed: Long = 1L) {
    var config=config; private set
    fun configureLot(count:Int) { if(config.pitReceiver) config=config.copy(strawCount=count) }
    var batch = FactoryGenerator.generate(firstSeed, config); private set
    var equipment:List<com.needlefactory.economy.FieldEquipment> = emptyList()
    /** Append new straw identities without moving/resetting existing physics or recovered steel. */
    fun growLot(count:Int):Boolean {
        if(!config.pitReceiver || phase!=BatchPhase.ACTIVE || count<=config.strawCount) return false
        val old=config.strawCount; config=config.copy(strawCount=count)
        val generated=FactoryGenerator.generate(batch.seed,config)
        val items=batch.items.take(old).toMutableList()
        items.addAll(generated.items.subList(old,count))
        for(n in batch.needles) {
            val copy=FactoryItem(count+n.id-old,n.material,n.length,n.width,n.bend,n.tone,n.x,n.y,n.angle,n.strands,n.collisionStrandCount)
            copy.route=n.route;copy.progress=n.progress;copy.vx=n.vx;copy.vy=n.vy;copy.angularVelocity=n.angularVelocity;copy.snapshot();items.add(copy)
        }
        batch=FactoryBatch(batch.seed,items).also { it.elapsed=batch.elapsed;it.touches=batch.touches;it.dragDistance=batch.dragDistance }
        return true
    }
    var batchNumber = 1; private set
    var recovered = 0; private set
    var missed = 0; private set
    var packaged = 0; private set
    var running = true; private set
    var conveyorSeconds = 0f; private set
    var receiverTurns = 0f; private set
    var strawImpactAge=10f; private set
    var metalImpactAge=10f; private set
    var strawImpacts=0L; private set
    var metalImpacts=0L; private set
    var receiverWidthLevel=0; private set
    val pitLeft get()=FactoryConfig.PIT_X-FactoryConfig.PIT_HALF
    val pitWidth get()=FactoryConfig.PIT_HALF*2+receiverWidthLevel
    val pitCenterX get()=pitLeft+pitWidth/2
    var feedGateAge = 0f; private set
    var phase = BatchPhase.ACTIVE; private set
    var rake = false; private set
    var guides = false; private set
    var baler = false; private set
    var balerRunning = true; private set
    var conveyorOwned = false; private set
    var magnetOwned = false; private set
    var metalReceiverOwned = !config.pitReceiver; private set
    var metalReceiverRunning = true; private set
    fun setMetalReceiver(owned:Boolean) { metalReceiverOwned=!config.pitReceiver || owned }
    fun toggleMetalReceiver() { if(metalReceiverOwned) metalReceiverRunning=!metalReceiverRunning }
    var handCapacity=1; private set
    var handReach=0f; private set
    var handSpeed=1f; private set
    var receiverSpeed=1f; private set
    var receiverCapacity=1; private set
    fun setReceiverLevels(speed:Int,capacity:Int,width:Int=0) { receiverSpeed=1f+speed*.15f;receiverCapacity=1+capacity;receiverWidthLevel=width.coerceIn(0,8) }
    fun setHandTool(capacity:Int,reach:Float,speed:Float=1f) {
        require(capacity in 1..360 && reach in 0f..2.5f && speed>=1f)
        handCapacity=capacity; handReach=reach; handSpeed=speed
    }
    val intakeBoundary get() = if(conveyorOwned) config.conveyorIntakeX else config.width
    private var balerQueueAge = 0f
    val receivingEnabled get() = operating(MachineId.VIBRATION) && (config.manualProducts || operating(MachineId.CONVEYOR))
    val collectedStraw get() = batch.items.count { it.material==Material.STRAW && it.route in PRODUCT_ROUTES }
    val looseStraw get() = batch.count(Route.LOOSE)
    val bundledStraw get() = batch.count(Route.BUNDLED)
    fun setBaler(owned:Boolean) { baler=owned && config.manualProducts }
    fun setTransport(conveyor:Boolean,magnet:Boolean) {
        val newBelt=conveyor && baler && config.physicalNeedles
        val newMagnet=magnet && newBelt
        if(newBelt!=conveyorOwned) enabled[MachineId.CONVEYOR.ordinal]=newBelt
        if(newMagnet!=magnetOwned) enabled[MachineId.SEPARATOR.ordinal]=newMagnet
        conveyorOwned=newBelt; magnetOwned=newMagnet
    }
    fun toggleBaler() { if(baler) balerRunning=!balerRunning }
    fun recoverNeedle(x:Float,y:Float):Boolean {
        if(!config.manualProducts || config.physicalNeedles || !running || phase!=BatchPhase.ACTIVE) return false
        val n=batch.needles.firstOrNull { it.route==Route.INTAKE && NeedleRecovery.canPick(batch,it,x,y) } ?: return false
        transition(n,Route.RECOVERED); recovered++; return true
    }
    /** Physical adapter verifies the bottom sorting port; touch acquisition never awards metal. */
    fun deliverMetal(id:Int):Boolean {
        val n=batch.items[id]
        if(!config.physicalNeedles || !metalReceiverOwned || !metalReceiverRunning || !running || phase!=BatchPhase.ACTIVE ||
            n.material!=Material.NEEDLE || n.route!=Route.INTAKE || !n.manuallyPushed) return false
        transition(n,Route.CHUTE); return true
    }
    fun setTools(rake: Boolean, guides: Boolean) { this.rake = rake; this.guides = guides }
    fun restore(seed: Long, number: Int, results: Boolean = false) {
        captureWaiting.clear()
        batch = FactoryGenerator.generate(seed, config); batchNumber = number
        if(results) batch.items.forEach { it.route = Route.DISCARDED }
        feedGateAge = 0f; balerQueueAge=0f; receiverTurns=0f; strawImpactAge=10f; metalImpactAge=10f; phase = if(results) BatchPhase.RESULTS else BatchPhase.ACTIVE
    }
    /** New game is distinct from lifecycle restore: reset counters and every manual switch. */
    fun startOver(seed:Long) {
        restore(seed,1)
        recovered=0; missed=0; packaged=0; conveyorSeconds=0f; running=true
        rake=false; guides=false; baler=false; balerRunning=true
        conveyorOwned=false; magnetOwned=false; handCapacity=1; handReach=0f
        metalReceiverOwned=!config.pitReceiver
        metalReceiverRunning=true
        for(i in enabled.indices) enabled[i]=!config.manualProducts || i==MachineId.VIBRATION.ordinal
    }
    fun finish(): Boolean {
        if (phase != BatchPhase.ACTIVE) return false
        batch.items.forEach { if(it.route == Route.INTAKE) transition(it,Route.DISCARDED) }
        phase = BatchPhase.DRAINING; running = true
        balerRunning=true
        if(conveyorOwned) enabled[MachineId.CONVEYOR.ordinal]=true // Drain committed cargo; do not enable magnet.
        if(!config.manualProducts) {
            enabled[MachineId.CONVEYOR.ordinal] = true; enabled[MachineId.PACKER.ordinal] = true
        }
        return true
    }
    fun showResults() { require(batch.complete || phase == BatchPhase.RESULTS); phase = BatchPhase.RESULTS }
    private val enabled = BooleanArray(MachineId.entries.size) { !config.manualProducts || it==MachineId.VIBRATION.ordinal }
    private val captureWaiting = java.util.ArrayDeque<Int>()
    fun enabled(id: MachineId) = enabled[id.ordinal]
    fun operating(id: MachineId) = running && enabled(id)
    fun toggle(id: MachineId) {
        if(phase==BatchPhase.DRAINING && config.manualProducts) return
        if(!config.manualProducts || id==MachineId.VIBRATION ||
            (id==MachineId.CONVEYOR && conveyorOwned) || (id==MachineId.SEPARATOR && magnetOwned))
            enabled[id.ordinal] = !enabled[id.ordinal]
    }
    fun toggleRunning() { running = !running; synchronizeSnapshots() }
    fun touch() { batch.touches++ }
    fun assist(distance: Float) {
        if (distance.isFinite()) batch.dragDistance += distance.coerceAtLeast(0f)
    }
    /** A live local tool contact authorizes this intake item; idle vibration cannot. */
    fun manualPush(id: Int) {
        if (running && phase == BatchPhase.ACTIVE && batch.items[id].route == Route.INTAKE) batch.items[id].manuallyPushed = true
    }
    /** Native manual adapter renews permission only for contacts in this fixed step. */
    fun clearManualPushes() { batch.items.forEach { it.manuallyPushed=false } }
    /** Called only at the real intake gate by the native adapter; never by the renderer. */
    fun feed(id: Int): Boolean {
        if (phase != BatchPhase.ACTIVE || !receivingEnabled) return false
        val item = batch.items[id]
        if (item.route != Route.INTAKE || !item.manuallyPushed) return false
        if(config.manualProducts) {
            if(item.material==Material.NEEDLE && !config.physicalNeedles) { transition(item,Route.MISSED); missed++ }
            else {
                transition(item,Route.RECEIVING)
                if(config.pitReceiver) {
                    if(item.material==Material.NEEDLE) {metalImpactAge=0f;metalImpacts++} else {strawImpactAge=0f;strawImpacts++}
                }
            }
        } else transition(item, Route.BELT)
        return true
    }
    fun gateFeed(id: Int): Boolean {
        if (feedGateAge < config.feedInterval/receiverCapacity || !feed(id)) return false
        feedGateAge = 0f
        return true
    }
    private fun transition(item: FactoryItem, route: Route) {
        item.route = route; item.progress = 0f; item.previousProgress = 0f
    }
    fun step() {
        if (!running || batch.complete || phase == BatchPhase.RESULTS) return
        val dt = config.stepSeconds
        batch.elapsed += dt
        strawImpactAge=min(10f,strawImpactAge+dt); metalImpactAge=min(10f,metalImpactAge+dt)
        if(config.manualProducts && receivingEnabled) receiverTurns=(receiverTurns+dt*.65f*receiverSpeed)%1f
        if (operating(MachineId.CONVEYOR)) conveyorSeconds += dt
        if (receivingEnabled)
            feedGateAge = min(config.feedInterval,feedGateAge+dt)
        if(config.manualProducts) {
            stepProducts(dt)
            return
        }
        for (item in batch.items) {
            item.snapshot()
            when (item.route) {
                Route.BELT -> if (operating(MachineId.CONVEYOR)) {
                    val next = item.progress + dt / config.beltSeconds
                    if (item.material == Material.NEEDLE && item.progress < .55f && next >= .55f && enabled(MachineId.SEPARATOR)) {
                        if(!captureWaiting.contains(item.id)) captureWaiting.addLast(item.id)
                        if (batch.count(Route.CAPTURE) == 0 && captureWaiting.peekFirst()==item.id) {
                            captureWaiting.removeFirst(); transition(item, Route.CAPTURE); recovered++
                        }
                        else item.progress = .55f - .0001f
                    } else if (next >= 1f) {
                        if (item.material == Material.STRAW) transition(item, Route.RESIDUE)
                        else { transition(item, Route.MISSED); missed++ }
                    } else { captureWaiting.remove(item.id); item.progress = next }
                }
                Route.CAPTURE -> if (operating(MachineId.CONVEYOR)) {
                    item.progress = min(1f,item.progress + dt / config.captureSeconds)
                    if (item.progress >= 1f && batch.count(Route.CHUTE)==0) transition(item, Route.CHUTE)
                }
                Route.CHUTE -> if (operating(MachineId.CONVEYOR)) {
                    item.progress = min(1f,item.progress + dt / config.chuteSeconds)
                    if (item.progress >= 1f && batch.count(Route.BUFFER)==0) transition(item, Route.BUFFER)
                }
                Route.BUFFER -> if (operating(MachineId.PACKER) && batch.count(Route.PACKING)==0 && batch.count(Route.EJECT)==0) transition(item, Route.PACKING)
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
    private fun stepProducts(dt:Float) {
        var receiverWorking=0
        for(item in batch.items) {
            item.snapshot()
            when(item.route) {
                Route.RECEIVING -> {
                    if(config.pitReceiver && receiverWorking++>=receiverCapacity) continue
                    item.progress=min(1f,item.progress+dt*receiverSpeed/config.receiverSeconds)
                    if(item.progress>=1f) {
                        transition(item,if(conveyorOwned) Route.BELT else if(config.physicalNeedles) Route.TRANSFER else if(baler) Route.BALER_QUEUE else Route.LOOSE)
                        if(item.route==Route.BELT) {
                            item.progress=((config.height-item.y)/(config.height-config.conveyorDischargeY)).coerceIn(0f,1f)
                            item.previousProgress=item.progress
                        }
                    }
                }
                Route.BELT -> if(conveyorOwned && operating(MachineId.CONVEYOR)) {
                    item.progress=min(1f,item.progress+dt/config.productBeltSeconds)
                    if(item.progress>=1f) {
                        if(item.material==Material.NEEDLE && operating(MachineId.SEPARATOR)) transition(item,Route.CAPTURE)
                        else transition(item,Route.TRANSFER)
                    }
                }
                Route.CAPTURE -> if(magnetOwned) {
                    item.progress=min(1f,item.progress+dt/config.captureSeconds)
                    if(item.progress>=1f) { transition(item,Route.SORTED); recovered++ }
                }
                Route.TRANSFER -> if(config.physicalNeedles) {
                    item.progress=min(1f,item.progress+dt*receiverSpeed/config.productTransitSeconds)
                    if(item.progress>=1f) {
                        if(item.material==Material.NEEDLE) { transition(item,Route.MISSED); missed++ }
                        else transition(item,if(baler) Route.BALER_QUEUE else Route.LOOSE)
                    }
                }
                Route.CHUTE -> if(config.physicalNeedles) {
                    item.progress=min(1f,item.progress+dt*receiverSpeed/config.receiverSeconds)
                    if(item.progress>=1f) { transition(item,Route.RECOVERED); recovered++ }
                }
                Route.BALING -> if(balerRunning) {
                    item.progress=min(1f,item.progress+dt/config.balerSeconds)
                    if(item.progress>=1f) transition(item,Route.BUNDLED)
                }
                else -> Unit
            }
        }
        val queued=batch.count(Route.BALER_QUEUE)
        balerQueueAge=if(queued>0) balerQueueAge+dt else 0f
        if(baler && balerRunning && batch.count(Route.BALING)==0 && queued>0 &&
            (queued>=config.balerUnitsPerCycle || balerQueueAge>=config.balerSeconds || phase==BatchPhase.DRAINING)) {
            var accepted=0
            for(item in batch.items) if(item.route==Route.BALER_QUEUE && accepted<config.balerUnitsPerCycle) {
                transition(item,Route.BALING); accepted++
            }
            balerQueueAge=0f
        }
    }
    fun synchronizeSnapshots() { batch.items.forEach { it.snapshot() } }
    fun next(): Boolean {
        if (!batch.complete && phase != BatchPhase.RESULTS) return false
        batch = FactoryGenerator.generate(batch.seed + 0x61C8864680B583EBL, config)
        batchNumber++
        feedGateAge = 0f
        balerQueueAge=0f
        phase = BatchPhase.ACTIVE
        captureWaiting.clear()
        return true
    }
    fun summary(): String = "batch=$batchNumber seed=${batch.seed} seconds=${batch.elapsed} " +
        Route.entries.joinToString(" ") { "$it=${batch.count(it)}" } +
        " recovered=$recovered missed=$missed packaged=$packaged touches=${batch.touches} dragM=${batch.dragDistance} " +
        "feed=MANUAL phase=$phase tools=RAKE:$rake,GUIDES:$guides armed=${batch.items.count { it.route==Route.INTAKE && it.manuallyPushed }} gateS=$feedGateAge " +
        MachineId.entries.joinToString(" ") { "$it=${enabled(it)}" }+
        if(config.manualProducts) " mode=PRODUCTS collected=$collectedStraw loose=$looseStraw bundled=$bundledStraw baler=$baler balerRun=$balerRunning beltOwned=$conveyorOwned magnetOwned=$magnetOwned augerTurns=$receiverTurns pit=${config.pitReceiver} metalPort=$metalReceiverOwned metalRun=$metalReceiverRunning" else ""
    companion object {
        private val PRODUCT_ROUTES=setOf(Route.RECEIVING,Route.BELT,Route.TRANSFER,Route.BALER_QUEUE,Route.BALING,Route.LOOSE,Route.BUNDLED)
    }
}

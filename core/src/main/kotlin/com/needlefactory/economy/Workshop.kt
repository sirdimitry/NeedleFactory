package com.needlefactory.economy

import com.needlefactory.simulation.*

/** Values are workshop credits, never renderer counters or pixels. */
data class EconomyConfig(val needleValue: Long = 15, val materialValue: Long = 30,
                         val perfectBonus: Long = 15, val earlyPercent: Int = 25,
                         val productValues:Boolean=false, val looseValue:Long=6, val bundledValue:Long=16,
                         val rakePrice:Long=28, val guidesPrice:Long=16, val balerPrice:Long=42,
                         val conveyorPrice:Long=48, val magnetPrice:Long=72, val sortedNeedleValue:Long=4,
                         val archiveTools:Boolean=false, val steelRakePrice:Long=450,
                         val pitProgression:Boolean=false,val metalReceiverPrice:Long=18,
                         val strawDeliveryBonus:Long=0,val clumpEconomy:Boolean=false,val allNeedlesBonus:Long=0) {
    init { require(needleValue >= 0 && materialValue >= 0 && perfectBonus >= 0 && earlyPercent in 0..100) }
    init { require(looseValue>=0 && bundledValue>looseValue && rakePrice>0 && guidesPrice>0 && balerPrice>0) }
    init { require(conveyorPrice>0 && magnetPrice>0 && sortedNeedleValue>=0 && (!productValues || sortedNeedleValue>=needleValue)) }
    init { require(steelRakePrice>0 && (!archiveTools || productValues)) }
    init { require(metalReceiverPrice>0 && (!pitProgression || archiveTools) && strawDeliveryBonus>=0) }
    companion object {
        fun products()=EconomyConfig(needleValue=3,perfectBonus=0,productValues=true)
        /** Archive-inspired prices, normalized to our 24-clump lot rather than 290 strands. */
        fun workshop()=EconomyConfig(needleValue=24,perfectBonus=0,productValues=true,
            looseValue=24,bundledValue=64,rakePrice=150,guidesPrice=180,balerPrice=600,
            conveyorPrice=250,magnetPrice=900,sortedNeedleValue=32,archiveTools=true)
        fun pit()=workshop().copy(needleValue=12,looseValue=6,pitProgression=true,
            metalReceiverPrice=240,allNeedlesBonus=36,strawDeliveryBonus=6,rakePrice=50,steelRakePrice=200,clumpEconomy=true)
    }
}

enum class HandTool(val key:String,val title:String,val capacity:Int,val reach:Float) {
    HAND("hand","Hand",1,0f), RAKE("feed-rake","Feed rake",2,.65f),
    STEEL_RAKE("steel-rake","Steel rake",3,.95f);
    companion object { fun fromKey(key:String)=entries.firstOrNull { it.key==key } ?: HAND }
}
enum class UpgradeId(val key: String, val title: String, val price: Long, val prerequisite: String?) {
    RAKE("feed-rake", "Feed rake", 30, null),
    GUIDES("feed-guides", "Feed guides", 65, "feed-rake"),
    BALER("straw-baler", "Straw baler", 42, "feed-rake"),
    CONVEYOR("short-conveyor", "Short conveyor", 48, "straw-baler"),
    MAGNET("magnetic-separator", "Magnetic separator", 72, "short-conveyor"),
    STEEL_RAKE("steel-rake", "Steel rake", 450, "feed-rake"),
    METAL_RECEIVER("needle-receiver", "Needle receiver", 18, null)
}
enum class PurchaseResult { BOUGHT, INSUFFICIENT, OWNED, LOCKED }
data class BatchReceipt(val batchId: Long, val secured: Int, val total: Int, val processed: Int,
                        val materialTotal: Int, val missed: Int, val discarded: Int,
                        val metal: Long, val material: Long, val bonus: Long,
                        val loose:Int=0,val bundled:Int=0,val economyModel:String="line-v1",val sorted:Int=0) {
    val earned: Long get() = metal + material + bonus
    val perfect: Boolean get() = secured == total && processed == materialTotal && missed == 0 && discarded == 0
    val percent: Int get() = processed * 100 / materialTotal
}
data class WorkshopProfile(val schemaVersion: Int = 3, val workshopCredits: Long = 0,
                           val purchased: Set<String> = emptySet(), val completedBatches: Long = 0,
                           val securedNeedles: Long = 0, val activeBatchId: Long = 1,
                           val activeSeed: Long = 1, val receipt: BatchReceipt? = null,
                           val economyModel:String="line-v1",val selectedTool:String="hand",
                           val levels:Map<String,Int> = emptyMap(), val delivered:Map<Int,Long> = emptyMap(),
                           val lotCount:Int=24,val lotValue:Long=1,val lotLuck:Int=0,
                           val equipment:List<FieldEquipment> = emptyList(),val nextEquipmentId:Int=1,val receiverInvestment:Long=0)

/** The first slice has no rare rolls, offline clock or dynamic contracts. */
class Workshop(initial: WorkshopProfile = WorkshopProfile(), val config: EconomyConfig = EconomyConfig()) {
    init { require(initial.workshopCredits>=0 && initial.activeBatchId>0) }
    val model=if(config.clumpEconomy) "products-v7" else if(config.pitProgression) "products-v5" else if(config.archiveTools) "products-v3" else if(config.productValues) "products-v2" else "line-v1"
    var profile = if(config.productValues) initial.copy(economyModel=model) else initial; private set
    val catalog=if(config.pitProgression) listOf(UpgradeId.METAL_RECEIVER,UpgradeId.RAKE,UpgradeId.STEEL_RAKE)
        else if(config.productValues) listOf(UpgradeId.RAKE,UpgradeId.GUIDES,UpgradeId.BALER,UpgradeId.CONVEYOR,UpgradeId.MAGNET)
        .let { if(config.archiveTools) it+UpgradeId.STEEL_RAKE else it }
        else listOf(UpgradeId.RAKE,UpgradeId.GUIDES)
    fun price(id:UpgradeId):Long = if(!config.productValues) id.price else when(id) {
        UpgradeId.RAKE->config.rakePrice; UpgradeId.GUIDES->config.guidesPrice; UpgradeId.BALER->config.balerPrice
        UpgradeId.CONVEYOR->config.conveyorPrice; UpgradeId.MAGNET->config.magnetPrice
        UpgradeId.STEEL_RAKE->config.steelRakePrice
        UpgradeId.METAL_RECEIVER->config.metalReceiverPrice
    }
    fun quote(batch: FactoryBatch): BatchReceipt {
        if(config.productValues) {
            val loose=batch.count(Route.LOOSE); val bundled=batch.count(Route.BUNDLED)
            val total=batch.items.count { it.material==Material.STRAW }
            val raw=batch.count(Route.RECOVERED)
            val sorted=batch.count(Route.SORTED); val secured=raw+sorted
            return BatchReceipt(profile.activeBatchId,secured,batch.needles.size,loose+bundled,total,
                batch.count(Route.MISSED),batch.count(Route.DISCARDED),raw*needlePrice+sorted*config.sortedNeedleValue,
                if(config.clumpEconomy) batch.items.filter { it.material==Material.STRAW && it.route==Route.LOOSE }.sumOf { profile.delivered[it.id] ?: clumpValue(it.id) } else (loose*config.looseValue+bundled*config.bundledValue)/total,
                (if(loose+bundled==total) config.strawDeliveryBonus else 0)+(if(config.clumpEconomy && secured==batch.needles.size) clearMetalBonus else 0),loose,bundled,model,sorted)
        }
        val processed = batch.items.count { it.material == Material.STRAW && it.route == Route.RESIDUE }
        val totalMaterial = batch.items.count { it.material == Material.STRAW }
        val secured = batch.needles.count { it.route == Route.OUTPUT }
        val missed = batch.needles.count { it.route == Route.MISSED }
        val discarded = batch.items.count { it.route == Route.DISCARDED }
        val perfect = secured == batch.needles.size && processed == totalMaterial && missed == 0 && discarded == 0
        return BatchReceipt(profile.activeBatchId,secured,batch.needles.size,processed,totalMaterial,missed,discarded,
            secured*config.needleValue,config.materialValue*processed*processed/(totalMaterial.toLong()*totalMaterial),
            if(perfect) config.perfectBonus else 0)
    }
    fun eligible(batch: FactoryBatch): Boolean {
        val q = quote(batch)
        if(config.productValues) return q.earned>0
        return q.secured == q.total || (q.secured > 0 && q.processed*100 >= q.materialTotal*config.earlyPercent)
    }
    fun settle(batch: FactoryBatch): BatchReceipt {
        require(batch.seed == profile.activeSeed) { "Batch does not match the active manifest" }
        profile.receipt?.let { return it }
        require(batch.complete) { "Drain the pipeline before settlement" }
        val result = quote(batch)
        profile = profile.copy(workshopCredits = Math.addExact(profile.workshopCredits,result.earned-(if(config.clumpEconomy) profile.delivered.values.sum() else 0)),
            completedBatches = profile.completedBatches+1,securedNeedles = profile.securedNeedles+result.secured,
            receipt = result)
        return result
    }
    fun purchase(id: UpgradeId): PurchaseResult {
        if(id !in catalog) return PurchaseResult.LOCKED
        if(id.key in profile.purchased) return PurchaseResult.OWNED
        if(id.prerequisite != null && id.prerequisite !in profile.purchased) return PurchaseResult.LOCKED
        val price=price(id)
        if(profile.workshopCredits < price) return PurchaseResult.INSUFFICIENT
        profile = profile.copy(workshopCredits = profile.workshopCredits-price,purchased = profile.purchased+id.key,
            receiverInvestment=if(id==UpgradeId.METAL_RECEIVER) price else profile.receiverInvestment,
            selectedTool=if(config.archiveTools && (id==UpgradeId.RAKE || id==UpgradeId.STEEL_RAKE)) id.key else profile.selectedTool)
        return PurchaseResult.BOUGHT
    }
    /** Steel stays valuable even after material-value purchases; lucky straw is at most 2x. */
    val needlePrice:Long get()=if(config.clumpEconomy) maxOf(config.needleValue,profile.lotValue*6) else config.needleValue
    val clearMetalBonus:Long get()=if(config.clumpEconomy) maxOf(config.allNeedlesBonus,profile.lotValue*18) else 0
    fun addPlaytestCredits(amount:Long=100):Boolean {
        if(amount !in listOf(100L,1_000L,10_000L) || profile.workshopCredits>1_000_000_000_000L-amount) return false
        profile=profile.copy(workshopCredits=profile.workshopCredits+amount);return true
    }
    fun level(key:String)=profile.levels[key] ?: 0
    fun clumpValue(id:Int):Long {
        val mixed=(profile.activeSeed xor (id.toLong()*0x61C8864680B583EBL)).ushr(17)
        val lucky=mixed%100 < (profile.lotLuck*2).coerceAtMost(25)
        return profile.lotValue*(if(lucky) 2 else 1)
    }
    /** Durable per-lot identities: restarting cannot resell the same clump. */
    fun creditDelivered(batch:FactoryBatch):Long {
        if(!config.clumpEconomy || profile.receipt!=null) return 0
        require(batch.seed==profile.activeSeed)
        val added=batch.items.filter { it.material==Material.STRAW && it.route==Route.LOOSE && it.id !in profile.delivered }
            .associate { it.id to clumpValue(it.id) }
        val earned=added.values.sum()
        if(added.isNotEmpty()) profile=profile.copy(workshopCredits=Math.addExact(profile.workshopCredits,earned),delivered=profile.delivered+added)
        return earned
    }
    fun upgradePrice(key:String):Long {
        val def=ProgressionUpgrade.entries.first { it.key==key }
        val n=level(key)
        val growth=if(def==ProgressionUpgrade.RECEIVER_WIDTH) Math.pow(1.5,minOf(n,4).toDouble())*Math.pow(2.0,maxOf(0,n-4).toDouble()) else Math.pow(1.75,n.toDouble())
        return (def.basePrice*growth).coerceAtMost(1_000_000_000_000.0).toLong()
    }
    fun upgrade(key:String):PurchaseResult {
        val def=ProgressionUpgrade.entries.firstOrNull { it.key==key } ?: return PurchaseResult.LOCKED
        if(!config.clumpEconomy || (def.tool!=null && def.tool.key !in profile.purchased)) return PurchaseResult.LOCKED
        if(level(key)>=1000 || (def==ProgressionUpgrade.RECEIVER_WIDTH && level(key)>=8)) return PurchaseResult.LOCKED
        val price=upgradePrice(key)
        if(profile.workshopCredits<price) return PurchaseResult.INSUFFICIENT
        profile=profile.copy(workshopCredits=profile.workshopCredits-price,levels=profile.levels+(key to (level(key)+1)))
        profile=profile.copy(lotValue=if(def==ProgressionUpgrade.VALUE) 1L+level("material-value") else profile.lotValue,lotLuck=if(def==ProgressionUpgrade.LUCK) level("material-luck") else profile.lotLuck,
            lotCount=if(profile.receipt==null) (24+level("material-count")*(level("material-count")+1)/2).coerceAtMost(360) else profile.lotCount)
        return PurchaseResult.BOUGHT
    }
    fun buyEquipment(kind:EquipmentKind):PurchaseResult {
        if(!config.pitProgression || (kind==EquipmentKind.ROBOT && profile.equipment.any { it.kind==kind })) return PurchaseResult.LOCKED
        if(profile.workshopCredits<kind.price) return PurchaseResult.INSUFFICIENT
        val i=profile.nextEquipmentId
        val e=FieldEquipment(i,kind,if(kind==EquipmentKind.ROBOT) 7.5f else 5.5f,-2f-(i%8)*.8f)
        profile=profile.copy(workshopCredits=profile.workshopCredits-kind.price,equipment=profile.equipment+e,nextEquipmentId=i+1)
        return PurchaseResult.BOUGHT
    }
    fun upgradeEquipment(id:Int,axis:String):PurchaseResult {
        val e=profile.equipment.firstOrNull { it.id==id } ?: return PurchaseResult.LOCKED
        if((e.kind==EquipmentKind.DIVIDER && (axis!="range" || e.range>=29)) || axis !in setOf("speed","width","range") ||
            (e.kind==EquipmentKind.ROBOT && axis!="speed") || (axis=="width" && e.width>=10) || maxOf(e.speed,e.range)>=1000) return PurchaseResult.LOCKED
        val price=e.price(axis)
        if(profile.workshopCredits<price) return PurchaseResult.INSUFFICIENT
        val n=e.copy(speed=e.speed+if(axis=="speed") 1 else 0,width=e.width+if(axis=="width") 1 else 0,
            range=e.range+if(axis=="range") 1 else 0,invested=e.invested+price)
        profile=profile.copy(workshopCredits=profile.workshopCredits-price,equipment=profile.equipment.map { if(it.id==id) n else it })
        return PurchaseResult.BOUGHT
    }
    fun moveEquipment(id:Int,x:Float,y:Float,degrees:Int):Boolean {
        if(!x.isFinite() || !y.isFinite()) return false
        val e=profile.equipment.firstOrNull { it.id==id } ?: return false
        val fixed=e.kind==EquipmentKind.FAN && e.anchored
        val angle=if(e.kind==EquipmentKind.FAN && !e.anchored) e.degrees else ((degrees%360)+360)%360
        profile=profile.copy(equipment=profile.equipment.map { if(it.id==id) it.copy(x=if(fixed) e.x else x.coerceIn(.9f,11.5f),y=if(fixed) e.y else y.coerceIn(-10.2f,7f),degrees=angle) else it })
        return true
    }
    fun toggleFanAnchor(id:Int):Boolean {
        val e=profile.equipment.firstOrNull {it.id==id && it.kind==EquipmentKind.FAN} ?: return false
        profile=profile.copy(equipment=profile.equipment.map {if(it.id==id) it.copy(anchored=!e.anchored) else it});return true
    }
    fun sellReceiver():Boolean {
        if(UpgradeId.METAL_RECEIVER.key !in profile.purchased) return false
        profile=profile.copy(workshopCredits=profile.workshopCredits+profile.receiverInvestment/2,receiverInvestment=0,purchased=profile.purchased-UpgradeId.METAL_RECEIVER.key)
        return true
    }
    fun sellEquipment(id:Int):Boolean {
        val e=profile.equipment.firstOrNull { it.id==id } ?: return false
        profile=profile.copy(workshopCredits=profile.workshopCredits+e.invested/2,equipment=profile.equipment.filter { it.id!=id })
        return true
    }
    val gripCapacity:Int get()=when(tool) {
        HandTool.HAND->1
        HandTool.RAKE->2+level("loosen-grip")
        HandTool.STEEL_RAKE->7+level("loosen-grip")+level("rake-grip")
    }.coerceAtMost(360)
    val gripSpeed:Float get()=when(tool) {
        HandTool.HAND->1f
        HandTool.RAKE->1.2f+level("loosen-speed")*.12f
        HandTool.STEEL_RAKE->1.8f+(level("loosen-speed")+level("rake-speed"))*.12f
    }
    fun select(tool:HandTool):Boolean {
        if(!config.archiveTools || (tool!=HandTool.HAND && tool.key !in profile.purchased)) return false
        profile=profile.copy(selectedTool=tool.key); return true
    }
    val tool:HandTool get()=HandTool.fromKey(profile.selectedTool).let {
        if(it==HandTool.HAND || it.key in profile.purchased) it else HandTool.HAND
    }
    fun cycleTool() {
        val tools=HandTool.entries.filter { it==HandTool.HAND || it.key in profile.purchased }
        select(tools[(tools.indexOf(tool)+1)%tools.size])
    }
    fun next(): Boolean {
        if(profile.receipt == null) return false
        profile = profile.copy(activeBatchId = profile.activeBatchId+1,
            activeSeed = profile.activeSeed+0x61C8864680B583EBL, receipt = null,delivered=emptyMap(),
            lotCount=(24+level("material-count")*(level("material-count")+1)/2).coerceAtMost(360),
            lotValue=1L+level("material-value"),lotLuck=level("material-luck"))
        return true
    }
    /** Explicit owner action; a new manifest prevents the abandoned lot being paid later. */
    fun startOver(seed:Long) {
        require(seed!=profile.activeSeed)
        profile=WorkshopProfile(activeSeed=seed,economyModel=model)
    }
}

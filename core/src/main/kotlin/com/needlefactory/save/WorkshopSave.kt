package com.needlefactory.save

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonReader
import com.needlefactory.economy.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.Future

data class SaveLoad(val profile: WorkshopProfile, val warning: String? = null, val readOnly: Boolean = false)

/** Small explicit schema. Unknown fields are tolerated, future schema is never overwritten. */
object WorkshopCodec {
    fun encode(p: WorkshopProfile): String {
        val j = Json(); j.setOutputType(com.badlogic.gdx.utils.JsonWriter.OutputType.json)
        val out = java.io.StringWriter(); j.setWriter(out); j.writeObjectStart()
        j.writeValue("schemaVersion",3); j.writeValue("contentVersion",8); j.writeValue("WorkshopCredits",p.workshopCredits)
        j.writeValue("economyModel",p.economyModel)
        j.writeValue("selectedTool",p.selectedTool)
        j.writeValue("lotCount",p.lotCount); j.writeValue("lotValue",p.lotValue); j.writeValue("lotLuck",p.lotLuck)
        j.writeValue("nextEquipmentId",p.nextEquipmentId);j.writeValue("receiverInvestment",p.receiverInvestment)
        j.writeArrayStart("equipment")
        for(e in p.equipment) { j.writeObjectStart();j.writeValue("id",e.id);j.writeValue("kind",e.kind.name)
            j.writeValue("x",e.x);j.writeValue("y",e.y);j.writeValue("degrees",e.degrees)
            j.writeValue("speed",e.speed);j.writeValue("width",e.width);j.writeValue("range",e.range);j.writeValue("invested",e.invested);j.writeValue("anchored",e.anchored);j.writeObjectEnd() }
        j.writeArrayEnd()
        j.writeObjectStart("levels"); p.levels.toSortedMap().forEach { (k,v)->j.writeValue(k,v) }; j.writeObjectEnd()
        j.writeObjectStart("delivered"); p.delivered.toSortedMap().forEach { (k,v)->j.writeValue(k.toString(),v) }; j.writeObjectEnd()
        j.writeArrayStart("purchasedUpgrades"); p.purchased.sorted().forEach { j.writeValue(it) }; j.writeArrayEnd()
        j.writeValue("completedBatches",p.completedBatches); j.writeValue("securedNeedles",p.securedNeedles)
        j.writeValue("activeBatchId",p.activeBatchId); j.writeValue("activeSeed",p.activeSeed)
        p.receipt?.let { r ->
            j.writeObjectStart("receipt")
            j.writeValue("batchId",r.batchId); j.writeValue("secured",r.secured); j.writeValue("total",r.total)
            j.writeValue("processed",r.processed); j.writeValue("materialTotal",r.materialTotal)
            j.writeValue("missed",r.missed); j.writeValue("discarded",r.discarded)
            j.writeValue("metal",r.metal); j.writeValue("material",r.material); j.writeValue("bonus",r.bonus)
            j.writeValue("loose",r.loose); j.writeValue("bundled",r.bundled); j.writeValue("economyModel",r.economyModel)
            j.writeValue("sorted",r.sorted)
            j.writeObjectEnd()
        }
        j.writeObjectEnd(); j.writer.close(); return out.toString()
    }
    fun decode(text: String, seed: Long): SaveLoad {
        val v = JsonReader().parse(text)
        require(v.isObject)
        val version = v.getInt("schemaVersion",0)
        if(version > 3 || v.getInt("contentVersion",1)>8)
            return SaveLoad(WorkshopProfile(activeSeed=seed),"Newer save version. Progress is read-only.",true)
        require(version in 0..3)
        // v0 development schema used credits/upgrades; migrate explicitly, not by guessing field names.
        val credits = v.getLong(if(version==0) "credits" else "WorkshopCredits",0)
        require(credits in 0..1_000_000_000_000L)
        val purchased = mutableSetOf<String>()
        var child = v.get(if(version==0) "upgrades" else "purchasedUpgrades")?.child
        while(child != null) {
            val key=child.asString(); require(key.matches(Regex("[a-z0-9-]{1,64}")))
            purchased.add(key); child=child.next // Preserve unknown IDs without granting their capability.
        }
        require(UpgradeId.GUIDES.key !in purchased || UpgradeId.RAKE.key in purchased)
        require(UpgradeId.BALER.key !in purchased || UpgradeId.RAKE.key in purchased)
        require(UpgradeId.CONVEYOR.key !in purchased || UpgradeId.BALER.key in purchased)
        require(UpgradeId.MAGNET.key !in purchased || UpgradeId.CONVEYOR.key in purchased)
        require(UpgradeId.STEEL_RAKE.key !in purchased || UpgradeId.RAKE.key in purchased)
        val selected=HandTool.fromKey(v.getString("selectedTool",if(UpgradeId.RAKE.key in purchased) "feed-rake" else "hand"))
            .let { if(it==HandTool.HAND || it.key in purchased) it.key else "hand" }
        val id=v.getLong("activeBatchId",1); require(id in 1..Int.MAX_VALUE.toLong())
        val completed=v.getLong("completedBatches",0); val secured=v.getLong("securedNeedles",0)
        require(completed>=0 && secured>=0)
        val receipt = v.get("receipt")?.let { r ->
            val q=BatchReceipt(r.getLong("batchId"),r.getInt("secured"),r.getInt("total"),r.getInt("processed"),
                r.getInt("materialTotal"),r.getInt("missed"),r.getInt("discarded"),
                r.getLong("metal"),r.getLong("material"),r.getLong("bonus"),
                r.getInt("loose",0),r.getInt("bundled",0),r.getString("economyModel","line-v1"),r.getInt("sorted",0))
            require(q.batchId==id && q.total==3 && q.secured in 0..3 && q.materialTotal in 24..360 &&
                q.processed in 0..q.materialTotal && q.missed in 0..3 && q.discarded in 0..363 &&
                q.metal in 0..100_000 && q.material in 0..1_000_000_000L && q.bonus in 0..100_000)
            require(q.economyModel in setOf("line-v1","products-v2","products-v3","products-v4","products-v5","products-v6","products-v7") && q.loose in 0..q.materialTotal && q.bundled in 0..q.materialTotal)
            if(q.economyModel.startsWith("products-")) {
                require(q.loose+q.bundled==q.processed)
                if(q.economyModel!="products-v7" && (q.economyModel !in setOf("products-v5","products-v6") || q.processed!=q.materialTotal)) require(q.bonus==0L)
                if(q.economyModel=="products-v7" && q.processed!=q.materialTotal && q.secured!=q.total) require(q.bonus==0L)
            }
            require(q.sorted in 0..q.secured)
            q
        }
        val model=v.getString("economyModel","line-v1"); require(model in setOf("line-v1","products-v2","products-v3","products-v4","products-v5","products-v6","products-v7"))
        val levels=mutableMapOf<String,Int>(); var l=v.get("levels")?.child
        while(l!=null) { require(ProgressionUpgrade.entries.any { it.key==l!!.name }); val n=l.asInt(); require(n in 0..1000); levels[l.name]=n;l=l.next }
        val lotCount=v.getInt("lotCount",24); val lotValue=v.getLong("lotValue",1); val lotLuck=v.getInt("lotLuck",0)
        require(lotCount in 24..360 && lotValue in 1..1001 && lotLuck in 0..1000)
        val delivered=mutableMapOf<Int,Long>(); var d=v.get("delivered")?.child
        while(d!=null) { val i=d.name.toInt(); val amount=d.asLong(); require(i in 0 until lotCount && amount in 1..2002); delivered[i]=amount;d=d.next }
        val equipment=mutableListOf<FieldEquipment>();var entry=v.get("equipment")?.child
        while(entry!=null) {
            val e=FieldEquipment(entry.getInt("id"),EquipmentKind.valueOf(entry.getString("kind")),entry.getFloat("x"),entry.getFloat("y"),
                entry.getInt("degrees"),entry.getInt("speed"),entry.getInt("width"),entry.getInt("range"),entry.getLong("invested"),entry.getBoolean("anchored",true))
            require(e.id>0 && e.x.isFinite() && e.y.isFinite() && e.x in .9f..11.5f && e.y in -10.2f..7f &&
                e.degrees in 0..359 && e.speed in 1..1000 && e.width in 1..10 && e.range in 1..1000 && e.invested in e.kind.price..9_000_000_000_000_000L)
            require(equipment.none { it.id==e.id } && (e.kind!=EquipmentKind.ROBOT || equipment.none { it.kind==e.kind }))
            equipment.add(e);entry=entry.next
        }
        val nextId=v.getInt("nextEquipmentId",1);require(nextId>(equipment.maxOfOrNull { it.id } ?: 0))
        val investment=v.getLong("receiverInvestment",if(UpgradeId.METAL_RECEIVER.key in purchased) 18L else 0L);require(investment in 0..1_000_000_000_000L)
        return SaveLoad(WorkshopProfile(receiverInvestment=investment,equipment=equipment,nextEquipmentId=nextId,workshopCredits=credits,purchased=purchased,completedBatches=completed,
            securedNeedles=secured,activeBatchId=id,activeSeed=v.getLong("activeSeed",seed),receipt=receipt,economyModel=model,selectedTool=selected,levels=levels,delivered=delivered,lotCount=lotCount,lotValue=lotValue,lotLuck=lotLuck),
            if(version<3) "Save v$version -> v3: balance/tools kept." else null)
    }
}

/** File work is serialized off the GL thread. fsync + same-directory rename + previous good backup. */
class WorkshopStore(private val file: File) {
    private val backup = File(file.path+".bak")
    fun load(seed: Long): SaveLoad {
        if(!file.exists() && !backup.exists()) return SaveLoad(WorkshopProfile(activeSeed=seed))
        for(candidate in listOf(file,backup)) if(candidate.exists()) {
            try { return WorkshopCodec.decode(candidate.readText(Charsets.UTF_8),seed).let {
                if(candidate==backup) it.copy(warning="Recovered previous valid save backup.") else it
            } } catch(_: Exception) { /* Try last known good copy. */ }
        }
        return SaveLoad(WorkshopProfile(activeSeed=seed),"Save damaged. Default profile loaded.")
    }
    fun write(p: WorkshopProfile) {
        file.parentFile?.mkdirs()
        if(file.exists()) {
            val existing=try { WorkshopCodec.decode(file.readText(Charsets.UTF_8),p.activeSeed) } catch(_: Exception) { null }
            if(existing?.readOnly==true) throw java.io.IOException("Refusing to overwrite a newer save")
        }
        val temp = File(file.path+".tmp")
        FileOutputStream(temp).use { stream -> stream.write(WorkshopCodec.encode(p).toByteArray(Charsets.UTF_8)); stream.fd.sync() }
        if(file.exists()) {
            // Copy only a valid file; a corrupt primary must not destroy a valid backup.
            try { if(!WorkshopCodec.decode(file.readText(Charsets.UTF_8),p.activeSeed).readOnly) file.copyTo(backup,true) }
            catch(_: Exception) { }
        }
        // POSIX replacement is atomic. Platforms refusing replacement use the valid backup
        // through the short replacement gap (also works below Android API 26, no NIO dependency).
        if(!temp.renameTo(file)) {
            if(file.exists() && !file.delete()) throw java.io.IOException("Cannot replace save")
            if(!temp.renameTo(file)) throw java.io.IOException("Cannot install save; backup retained")
        }
    }
    /** Once the new primary is durable, make fallback belong to the same new game. */
    fun reset(p:WorkshopProfile) {
        require(p.workshopCredits==0L && p.purchased.isEmpty() && p.receipt==null && p.activeBatchId==1L)
        write(p)
        file.copyTo(backup,true)
    }
}

/** Retained with the portable factory across Activity/screen recreation. */
class WorkshopRuntime(val persistence: WorkshopPersistence, seed: Long, private val products:Boolean=false,private val pit:Boolean=false) {
    private val economy:EconomyConfig=if(pit) EconomyConfig.pit() else if(products) EconomyConfig.workshop() else EconomyConfig()
    var workshop = Workshop(WorkshopProfile(activeSeed=seed),economy); private set
    var ready = false; private set
    var warning: String? = null; private set
    var readOnly = false; private set
    private var resetBackup=false
    val busy: Boolean get() = !ready || persistence.saving || persistence.error != null || readOnly
    fun poll(session: com.needlefactory.simulation.FactorySession): Boolean {
        persistence.poll()
        if(ready && !persistence.saving && persistence.error==null) resetBackup=false
        if(ready || !persistence.initial.isDone) return false
        val load = try { persistence.initial.get() } catch(_: Exception) {
            SaveLoad(WorkshopProfile(activeSeed=session.batch.seed),"Cannot read save. Progress is read-only.",true)
        }
        workshop=Workshop(load.profile,economy); warning=load.warning; readOnly=load.readOnly; ready=true
        if(pit) session.configureLot(load.profile.lotCount)
        session.restore(load.profile.activeSeed,load.profile.activeBatchId.toInt(),load.profile.receipt != null)
        if(pit && load.profile.receipt==null) for(id in load.profile.delivered.keys) session.batch.items[id].route=com.needlefactory.simulation.Route.LOOSE
        applyTools(session)
        return true
    }
    fun applyTools(session: com.needlefactory.simulation.FactorySession) {
        session.equipment=if(pit) workshop.profile.equipment else emptyList()
        session.setTools(UpgradeId.RAKE.key in workshop.profile.purchased,!pit && UpgradeId.GUIDES.key in workshop.profile.purchased)
        session.setBaler(!pit && UpgradeId.BALER.key in workshop.profile.purchased)
        session.setTransport(!pit && UpgradeId.CONVEYOR.key in workshop.profile.purchased,!pit && UpgradeId.MAGNET.key in workshop.profile.purchased)
        session.setMetalReceiver(UpgradeId.METAL_RECEIVER.key in workshop.profile.purchased)
        if(economy.archiveTools) session.setHandTool(if(pit) workshop.gripCapacity else workshop.tool.capacity,
            if(pit && workshop.tool!=HandTool.HAND) (.65f+(workshop.gripCapacity-2)*.10f).coerceAtMost(2.5f) else workshop.tool.reach,
            if(pit) workshop.gripSpeed else 1f)
        if(pit) session.setReceiverLevels(workshop.level("receiver-speed"),workshop.level("receiver-capacity"),workshop.level("receiver-width"))
    }
    fun startOver(session:com.needlefactory.simulation.FactorySession):Boolean {
        if(busy) return false
        val seed=workshop.profile.activeSeed+0x61C8864680B583EBL
        workshop.startOver(seed); if(pit) session.configureLot(workshop.profile.lotCount); session.startOver(seed); applyTools(session)
        warning=null; resetBackup=true; save(); return true
    }
    fun save() { if(ready && !readOnly) persistence.save(workshop.profile,resetBackup) }
}

class WorkshopPersistence(private val store: WorkshopStore, seed: Long) {
    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r,"workshop-save").apply { isDaemon=true } }
    val initial: Future<SaveLoad> = worker.submit<SaveLoad> { store.load(seed) }
    private var pending: Future<*>? = null
    var error: String? = null; private set
    val saving: Boolean get() = pending?.isDone == false
    fun poll() {
        val f=pending ?: return
        if(f.isDone) { try { f.get(); error=null } catch(_: Exception) { error="Save failed. Retry before continuing." }; pending=null }
    }
    fun save(profile: WorkshopProfile,reset:Boolean=false) { if(worker.isShutdown) return; pending=worker.submit {
        if(reset) store.reset(profile) else store.write(profile)
    } }
    fun close() { worker.shutdown() } // Queued snapshots complete; no render-thread wait.
}

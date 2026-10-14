package com.needlefactory.presentation

import com.badlogic.gdx.*
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.ClasspathFileHandleResolver
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.needlefactory.game.*
import com.needlefactory.physics.FactoryIntakePhysics
import com.needlefactory.simulation.*
import com.needlefactory.economy.*
import com.needlefactory.save.WorkshopRuntime
import kotlin.math.*

/** Replaceable procedural diorama. Production truth belongs exclusively to FactorySession. */
class FactoryScreen(private val build: BuildInfo, private val feedback: PlatformFeedback,
                    val session: FactorySession,
                    val needleLighting: NeedleLighting = NeedleLighting(),
                    val workshopRuntime: WorkshopRuntime? = null) : ScreenAdapter() {
    private val viewport = FitViewport(400f, 820f)
    var layout = FactoryLayout(400f, 820f); private set
    private val shapes = ShapeRenderer(12000)
    private val sprites = SpriteBatch()
    private val diorama = FactoryDiorama(shapes)
    private val hud = FactoryHud(shapes,sprites)
    private val payAges=FloatArray(8) { 10f }
    private val payLabels=Array(8) { "" }
    private var payCursor=0
    private val workshopPanel = WorkshopPanel(shapes,sprites)
    private val equipmentDrawing=FieldEquipmentDrawing(shapes,sprites)
    private val audio=FactoryAudio(build.diagnostics)
    private val audioPanel=AudioSettingsPanel(shapes,sprites,audio)
    private var soundPointer=-1
    private var pendingPurchase:String?=null
    private var shopPointer=-1
    private var objectPointer=-1
    private var objectStart=Vector2()
    private var objectDown=0L
    private var objectDragged=false
    private var objectCanRotate=false
    private var objectDegrees=0
    private var objectStartAngle=0
    private var objectAngleReady=false
    private var lastFanTapId=-1
    private var lastFanTapTime=0L
    private var metalImpactsSeen=session.metalImpacts
    private var strawImpactsSeen=session.strawImpacts
    private var lossFlashAge=10f
    private var objectBase=Vector2()
    var workshopView = if(session.phase==BatchPhase.RESULTS) WorkshopView.RESULTS else WorkshopView.NONE; private set
    private val assets = AssetManager(ClasspathFileHandleResolver()).apply {
        load("ui/needle-panel-ru.fnt", BitmapFont::class.java)
        load("ui/nf-logo.png", Texture::class.java)
    }
    private var physics = FactoryIntakePhysics(session)
    private val needleVisibility = NeedleVisibility(360)
    var needleVisibleFraction = 0f; private set
    private val clock = FixedStep(session.config.stepSeconds, 5, .1f)
    private var suspended = false
    private var ignoreDelta = true
    var isReady = false; private set
    val bodyCount get() = physics.bodyCount
    private val touch = Vector2(); private val pos = Vector2()
    private var pointer = -1; private var previousTouchX = 0f; private var previousTouchY = 0f
    private var packagedSeen = session.packaged
    private var loggedComplete = session.batch.complete
    private var metricsAge = 0f; private var logAge = 0f
    private var frameTotal = 0f; private var frames = 0; private var peak = 0f
    private var metrics = "Запуск приборов..."
    private val frameTimes = FloatArray(600)
    private var frameSamples = 0
    private var telemetryExpanded = false
    private var beforeSettings=WorkshopView.NONE
    private var safeArea = SafeArea()
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    fun setSafeArea(area: SafeArea) {
        if (safeArea == area) return
        safeArea = area
        resize(surfaceWidth, surfaceHeight)
    }
    private val bg = FactoryPalette.background
    private val dark = FactoryPalette.dark
    private val metal = Color(.47f, .51f, .50f, 1f)
    private val metalEdge = Color(.32f, .37f, .36f, 1f)
    private val metalReflection = Color()
    private val metalFalloff = Color()
    private val metalPolish = Color()
    private val straw = arrayOf(Color(.76f,.61f,.35f,1f), Color(.65f,.49f,.26f,1f),
        Color(.82f,.68f,.42f,1f), Color(.71f,.55f,.30f,1f))
    private val step: () -> Unit = { session.step(); physics.step() }
    private val input = object : InputAdapter() {
        override fun touchDown(x: Int, y: Int, p: Int, button: Int): Boolean {
            if (suspended || !isReady || pointer != -1 || shopPointer != -1 || objectPointer != -1 || soundPointer != -1) return false
            viewport.unproject(touch.set(x.toFloat(), y.toFloat()))
            val economy=workshopRuntime
            if(economy!=null) {
                if(!economy.ready) return true
                if(workshopView==WorkshopView.SOUND) {
                    audioPanel.geometry.layout(layout.width,layout.height,layout.landscape)
                    soundPointer=p;audioPanel.down(touch);return true
                }
                if(session.config.pitReceiver && workshopView==WorkshopView.NONE && layout.controls[2].contains(touch)) { openWorkshop();return true }
                if(layout.telemetry.contains(touch) && workshopView!=WorkshopView.RESET_CONFIRM && !economy.busy) {
                    audio.cue(AudioCue.CLICK);cancelTouch(); clock.reset()
                    if(workshopView==WorkshopView.SETTINGS) workshopView=beforeSettings
                    else { beforeSettings=workshopView; workshopView=WorkshopView.SETTINGS }
                    return true
                }
                if(workshopView!=WorkshopView.NONE) {
                    if(workshopView==WorkshopView.PURCHASE_CONFIRM) {
                        workshopPanel.layout(layout,workshopView)
                        audio.cue(AudioCue.CLICK)
                        if(workshopPanel.left.contains(touch)) { pendingPurchase=null;workshopView=WorkshopView.SHOP }
                        else if(workshopPanel.right.contains(touch) && !economy.busy) {
                            val action=pendingPurchase;pendingPurchase=null;workshopView=WorkshopView.SHOP
                            if(action!=null) executePurchase(action)
                        }
                        return true
                    }
                    if(workshopView==WorkshopView.SHOP && economy.workshop.config.clumpEconomy) {
                        workshopPanel.layout(layout,workshopView)
                        shopPointer=p;workshopPanel.liveShop.down(touch)
                        return true
                    }
                    workshopPanel.layout(layout,workshopView)
                    if(workshopView==WorkshopView.SETTINGS && workshopPanel.soundButton.contains(touch)) {
                        audio.cue(AudioCue.CLICK);workshopView=WorkshopView.SOUND
                        return true
                    }
                    audio.cue(AudioCue.CLICK)
                    if(workshopPanel.left.contains(touch)) {
                        if(economy.persistence.error!=null) economy.save()
                        else workshopView=if(workshopView==WorkshopView.RESET_CONFIRM) WorkshopView.SETTINGS
                            else if(workshopView==WorkshopView.ABOUT) WorkshopView.SETTINGS
                            else if(workshopView==WorkshopView.SETTINGS) beforeSettings
                            else if(workshopView==WorkshopView.RESULTS) WorkshopView.SHOP
                            else if(session.phase==BatchPhase.RESULTS) WorkshopView.RESULTS else WorkshopView.NONE
                    } else if(!economy.busy && workshopPanel.right.contains(touch)) {
                        when(workshopView) {
                            WorkshopView.SETTINGS->workshopView=WorkshopView.RESET_CONFIRM
                            WorkshopView.RESET_CONFIRM->startOver()
                            WorkshopView.FINISH->confirmFinish()
                            else->nextBatch()
                        }
                    } else if(workshopView==WorkshopView.SETTINGS && workshopPanel.cheatButtons.any { it.contains(touch) } && !economy.busy) {
                        val index=workshopPanel.cheatButtons.indexOfFirst { it.contains(touch) }
                        if(economy.workshop.addPlaytestCredits(workshopPanel.cheatAmounts[index])) {economy.save();feedback.found();refresh()}
                    } else if(workshopView==WorkshopView.SETTINGS && workshopPanel.aboutButton.contains(touch)) {
                        workshopView=WorkshopView.ABOUT
                    } else if(workshopView==WorkshopView.SETTINGS && workshopPanel.pageButton.contains(touch)) {
                        if(build.diagnostics) telemetryExpanded=!telemetryExpanded
                    } else if(workshopView==WorkshopView.SHOP && !economy.busy) {
                        if(workshopPanel.pageButton.contains(touch)) { workshopPanel.nextPage(); return true }
                        for(i in economy.workshop.catalog.indices) if(workshopPanel.purchases[i].contains(touch)) purchase(economy.workshop.catalog[i])
                    }
                    return true
                }
                if(session.phase==BatchPhase.DRAINING) return true
                if(economy.workshop.config.archiveTools && layout.handTool.contains(touch) && !economy.busy) {
                    audio.cue(AudioCue.CLICK);cancelTouch(); economy.workshop.cycleTool(); economy.applyTools(session); economy.save(); refresh(); return true
                }
                if(layout.next.contains(touch)) { requestFinish(); return true }
            }
            if(build.diagnostics && layout.telemetry.contains(touch)) {
                telemetryExpanded=!telemetryExpanded; return true
            }
            if (layout.run.contains(touch)) {
                audio.cue(AudioCue.CLICK);session.toggleRunning(); cancelTouch(); clock.reset(); ignoreDelta=true; refresh(); return true
            }
            if (layout.next.contains(touch) && session.batch.complete) { nextBatch(); return true }
            for (i in layout.controls.indices) if (layout.controls[i].width>0 && layout.controls[i].contains(touch)) {
                if(session.config.manualProducts) when(i) {
                    0->session.toggle(MachineId.VIBRATION); 1->if(session.config.pitReceiver) session.toggleMetalReceiver() else session.toggleBaler()
                    2->if(session.config.pitReceiver) openWorkshop() else session.toggle(MachineId.CONVEYOR); 3->if(session.config.pitReceiver) {openWorkshop();workshopPanel.liveShop.automation()} else session.toggle(MachineId.SEPARATOR)
                } else session.toggle(MachineId.entries[i])
                refresh(); return true
            }
            if(session.config.pitReceiver && workshopRuntime!=null) {
                if(equipmentDrawing.close.contains(touch)) {equipmentDrawing.selected=-1;return true}
                if(equipmentDrawing.sell.contains(touch)) { askPurchase(if(equipmentDrawing.selected==-2) "sell-receiver" else "sell:${equipmentDrawing.selected}");return true }
                if(session.metalReceiverOwned && layout.needleReceiver.contains(touch)) {equipmentDrawing.selected=-2;return true}
                val local=layout.yardCoordinates(touch.x,touch.y,yardTouch)
                val flying=session.equipment.firstOrNull {it.kind==EquipmentKind.ROBOT}
                if(flying!=null && hypot(local.x-physics.robotX,local.y-physics.robotY)<.85f) {equipmentDrawing.selected=flying.id;return true}
                val item=FieldEquipmentSelection.find(session.equipment.filter {it.kind!=EquipmentKind.ROBOT},local.x,local.y)
                if(item!=null) {
                    if(build.diagnostics) Gdx.app.log("EquipmentInput","select=${item.id} local=$local previous=${equipmentDrawing.selected}")
                    objectCanRotate=equipmentDrawing.selected==item.id && (item.kind!=EquipmentKind.FAN || item.anchored);equipmentDrawing.selected=item.id
                    objectPointer=p;objectDown=System.nanoTime();objectStart.set(local);objectBase.set(item.x,item.y)
                    objectAngleReady=local.dst(objectBase)>.30f
                    objectDegrees=item.degrees;objectStartAngle=FieldEquipment.angle(local.x-item.x,local.y-item.y);objectDragged=false
                    return true
                }
            }
            if (!session.running || session.batch.complete || !(if(session.config.pitReceiver) layout.yard else layout.table).contains(touch)) return false
            if(session.recoverNeedle(tableX(touch.x),tableY(touch.y))) { feedback.found(); refresh(); return true }
            pointer = p; val tx = tableX(touch.x); val ty = tableY(touch.y)
            previousTouchX = tx; previousTouchY = ty; session.touch()
            // Acquisition stays screen-space; bought tools add only nearby, touch-down grips.
            val pixelScale=Gdx.graphics.width/layout.width*layout.table.width/session.config.width
            physics.begin(tx,ty,if(session.config.grabClumps) (16f*Gdx.graphics.density/pixelScale).coerceIn(.18f,.48f)
                else session.config.grabRadius)
            return true
        }
        override fun touchDragged(x: Int, y: Int, p: Int): Boolean {
            if(p==soundPointer) {viewport.unproject(touch.set(x.toFloat(),y.toFloat()));audioPanel.drag(touch);return true}
            if(p==shopPointer) { viewport.unproject(touch.set(x.toFloat(),y.toFloat()));workshopPanel.liveShop.drag(touch);return true }
            if(p==objectPointer) {
                viewport.unproject(touch.set(x.toFloat(),y.toFloat()));val local=layout.yardCoordinates(touch.x,touch.y,yardTouch)
                val e=workshopRuntime ?: return true
                val id=equipmentDrawing.selected
                val hold=objectCanRotate && !objectDragged && (System.nanoTime()-objectDown)>300_000_000L
                if(build.diagnostics) Gdx.app.log("EquipmentInput","drag=$id local=$local rotating=${equipmentDrawing.rotating} hold=$hold start=$objectStartAngle angleReady=$objectAngleReady")
                if(equipmentDrawing.rotating || hold) {
                    equipmentDrawing.rotating=true
                    if(local.dst(objectBase)>.30f) {
                        val angle=FieldEquipment.angle(local.x-objectBase.x,local.y-objectBase.y)
                        if(!objectAngleReady) {objectStartAngle=angle;objectAngleReady=true}
                        e.workshop.moveEquipment(id,objectBase.x,objectBase.y,objectDegrees+angle-objectStartAngle)
                    }
                } else if(local.dst(objectStart)>.2f) {
                    objectDragged=true;e.workshop.moveEquipment(id,objectBase.x+local.x-objectStart.x,objectBase.y+local.y-objectStart.y,objectDegrees)
                }
                e.applyTools(session);return true
            }
            if (p != pointer) return false
            viewport.unproject(touch.set(x.toFloat(), y.toFloat()))
            val tx = tableX(touch.x); val ty = tableY(touch.y)
            session.assist(hypot(tx - previousTouchX, ty - previousTouchY))
            previousTouchX = tx; previousTouchY = ty; physics.target(tx, ty)
            return true
        }
        override fun touchUp(x: Int, y: Int, p: Int, button: Int): Boolean {
            if(p==soundPointer) {
                viewport.unproject(touch.set(x.toFloat(),y.toFloat()));soundPointer=-1
                if(audioPanel.up(touch)) {audio.stopPreview();workshopView=WorkshopView.SETTINGS;audio.cue(AudioCue.CLICK)}
                return true
            }
            if(p==shopPointer) {
                shopPointer=-1;viewport.unproject(touch.set(x.toFloat(),y.toFloat()))
                val action=workshopPanel.liveShop.up(touch)
                if(action!=null) audio.cue(AudioCue.CLICK)
                if(action=="close") workshopView=if(session.phase==BatchPhase.RESULTS) WorkshopView.RESULTS else WorkshopView.NONE
                else if(!action.isNullOrEmpty()) askPurchase(action)
                return true
            }
            if(p==objectPointer) {
                val runtime=workshopRuntime
                val id=equipmentDrawing.selected
                val now=System.nanoTime()
                val tap=!objectDragged && !equipmentDrawing.rotating && now-objectDown<280_000_000L
                if(tap && session.equipment.any {it.id==id && it.kind==EquipmentKind.FAN}) {
                    if(lastFanTapId==id && now-lastFanTapTime<360_000_000L) {
                        runtime?.workshop?.toggleFanAnchor(id);runtime?.applyTools(session);lastFanTapId=-1;feedback.found();audio.cue(AudioCue.PLACE)
                    } else {lastFanTapId=id;lastFanTapTime=now}
                } else lastFanTapId=-1
                if(objectDragged || equipmentDrawing.rotating) audio.cue(AudioCue.PLACE)
                objectPointer=-1;equipmentDrawing.rotating=false;runtime?.save();return true
            }
            if (p != pointer) return false
            cancelTouch(); return true
        }
        override fun touchCancelled(x: Int, y: Int, p: Int, button: Int):Boolean {lastFanTapId=-1;cancelTouch();return true}
    }
    private val yardTouch=Vector2()
    private fun tableX(x: Float) = if(session.config.pitReceiver) layout.yardCoordinates(touch.x,touch.y,yardTouch).x.coerceIn(0f,session.config.width) else ((x - layout.table.x) / layout.table.width * session.config.width).coerceIn(0f,session.config.width)
    private fun tableY(y: Float) = if(session.config.pitReceiver) layout.yardCoordinates(touch.x,touch.y,yardTouch).y.coerceIn(session.config.intakeMinY,session.config.height) else ((y - layout.table.y) / layout.table.height * session.config.height).coerceIn(0f,session.config.height)
    private fun cancelTouch() {
        if(soundPointer>=0) audio.saveLevels()
        soundPointer=-1;audioPanel.cancel();audio.stopPreview()
        physics.end();pointer=-1;shopPointer=-1
        if(objectPointer>=0) workshopRuntime?.save()
        objectPointer=-1;equipmentDrawing.rotating=false
    }
    private fun askPurchase(action:String) {
        val e=workshopRuntime ?: return
        if(e.busy) return
        val w=e.workshop
        if(action.startsWith("focus:")) {
            equipmentDrawing.selected=action.substringAfter(':').toInt();workshopView=WorkshopView.NONE;return
        }
        if(action.startsWith("buy:") && action.substringAfter(':') in w.profile.purchased) {
            purchase(UpgradeId.entries.first {it.key==action.substringAfter(':')});return
        }
        val parts=action.split(':')
        val price:Long;val title:String
        when(parts[0]) {
            "buy"->{val u=UpgradeId.entries.first {it.key==parts[1]};title=RussianUi.upgrade(u);price=w.price(u)}
            "level"->{val u=ProgressionUpgrade.entries.first {it.key==parts[1]};title=u.title;price=w.upgradePrice(u.key)}
            "place"->{val k=EquipmentKind.valueOf(parts[1]);title=k.title;price=k.price}
            "equip"->{val obj=w.profile.equipment.first {it.id==parts[1].toInt()};title="${obj.kind.title} №${obj.id} / ${when(parts[2]) {"range"->"дальность";"width"->"ширина";else->"скорость"}}";price=obj.price(parts[2])}
            "sell-receiver"->{title="Продать приёмник игл";price=-(w.profile.receiverInvestment/2)}
            "sell"->{val obj=w.profile.equipment.firstOrNull {it.id==parts[1].toInt()} ?: return;title="Продать: ${obj.kind.title}";price=-(obj.invested/2)}
            else->return
        }
        pendingPurchase=action;workshopPanel.confirmationTitle=title
        workshopPanel.confirmationDetail=if(parts[0].startsWith("sell")) "Вернуть ${-price} кр.?" else "Цена: $price кр.\n${if(w.profile.workshopCredits<price) "Не хватает кредитов" else "Купить это улучшение?"}"
        workshopView=WorkshopView.PURCHASE_CONFIRM;cancelTouch();clock.reset()
    }
    private fun executePurchase(action:String) {
        val e=workshopRuntime ?: return;val w=e.workshop;val p=action.split(':')
        val result=when(p[0]) {
            "buy"->w.purchase(UpgradeId.entries.first {it.key==p[1]})
            "level"->w.upgrade(p[1])
            "place"->w.buyEquipment(EquipmentKind.valueOf(p[1]))
            "equip"->w.upgradeEquipment(p[1].toInt(),p[2])
            "sell-receiver"->if(w.sellReceiver()) PurchaseResult.BOUGHT else PurchaseResult.LOCKED
            "sell"->if(w.sellEquipment(p[1].toInt())) PurchaseResult.BOUGHT else PurchaseResult.LOCKED
            else->PurchaseResult.LOCKED
        }
        if(result==PurchaseResult.BOUGHT) {
            audio.cue(AudioCue.PURCHASE)
            if(session.growLot(w.profile.lotCount)) {physics.close();physics=FactoryIntakePhysics(session)}
            e.applyTools(session);e.save();feedback.found();refresh()
        } else {
            audio.cue(AudioCue.REJECT)
            Gdx.app.log("Workshop","Purchase rejected: $result")
            pendingPurchase=action;workshopView=WorkshopView.PURCHASE_CONFIRM
            workshopPanel.confirmationDetail=when(result) {PurchaseResult.INSUFFICIENT->"Не хватает кредитов. Покупка не выполнена.";else->"Нужно предыдущее улучшение, либо предмет уже куплен."}
        }
    }
    fun startOver():Boolean {
        val e=workshopRuntime ?: return false
        if(e.busy) return false
        cancelTouch()
        if(!e.startOver(session)) return false
        physics.close(); physics=FactoryIntakePhysics(session)
        workshopView=WorkshopView.NONE; beforeSettings=WorkshopView.NONE
        packagedSeen=0; loggedComplete=false; clock.reset(); ignoreDelta=true; peak=0f
        feedback.found(); refresh(); return true
    }
    fun nextBatch(): Boolean {
        cancelTouch()
        workshopRuntime?.let { e ->
            if(e.busy || session.phase!=BatchPhase.RESULTS || !e.workshop.next()) return false
            e.save()
        }
        workshopRuntime?.let { if(session.config.pitReceiver) session.configureLot(it.workshop.profile.lotCount) }
        if (!session.next()) return false
        workshopView=WorkshopView.NONE
        physics.close(); physics = FactoryIntakePhysics(session)
        clock.reset(); loggedComplete = false; peak = 0f; refresh()
        return true
    }
    fun requestFinish(): Boolean {
        val e=workshopRuntime ?: return false
        if(e.busy || session.phase!=BatchPhase.ACTIVE || !e.workshop.eligible(session.batch)) return false
        cancelTouch(); clock.reset(); workshopView=WorkshopView.FINISH; audio.cue(AudioCue.ATTENTION); return true
    }
    fun confirmFinish(): Boolean {
        val e=workshopRuntime ?: return false
        if(e.busy || !e.workshop.eligible(session.batch) || !session.finish()) return false
        cancelTouch(); workshopView=WorkshopView.NONE; clock.reset(); return true
    }
    fun openWorkshop() { if(workshopRuntime?.ready==true) { audio.cue(AudioCue.CLICK);cancelTouch();clock.reset();workshopView=WorkshopView.SHOP } }
    fun purchase(id: UpgradeId): PurchaseResult {
        val e=workshopRuntime ?: return PurchaseResult.LOCKED
        if(e.busy) return PurchaseResult.LOCKED
        val result=e.workshop.purchase(id)
        if(result==PurchaseResult.BOUGHT) { e.applyTools(session); e.save(); feedback.found(); audio.cue(AudioCue.PURCHASE); refresh() }
        else if(result==PurchaseResult.OWNED && e.workshop.config.archiveTools) {
            val tool=HandTool.entries.firstOrNull { it.key==id.key }
            if(tool!=null && e.workshop.select(tool)) { e.applyTools(session); e.save(); refresh() }
        }
        return result
    }
    override fun show() { audio.resume();Gdx.input.inputProcessor = input; refresh() }
    override fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        surfaceWidth = width; surfaceHeight = height
        cancelTouch(); clock.reset(); session.synchronizeSnapshots(); ignoreDelta = true
        val canvas=FactoryCanvas.forWindow(width,height,safeArea)
        val unitsPerPixel=canvas.unitsPerPixel
        // No padding on the native GL surface and no fitted/letterboxed render region.
        // Shift content coordinates inside safe obstructions, clear the ENTIRE window.
        viewport.setWorldSize(width*unitsPerPixel, height*unitsPerPixel)
        viewport.update(width, height, true)
        viewport.camera.position.set(viewport.worldWidth/2-safeArea.left*unitsPerPixel,
            viewport.worldHeight/2-safeArea.bottom*unitsPerPixel,0f)
        viewport.camera.update()
        layout = FactoryLayout(canvas.width,canvas.height,session.config.manualProducts,session.config.pitReceiver)
        layout.updatePit(session.pitWidth)
        if(build.diagnostics) {
            val pixelsPerMetre=layout.table.width/session.config.width/unitsPerPixel
            Gdx.app.log("FactoryLayout","window=${width}x$height safe=$safeArea " +
                "unitsPerPixel=$unitsPerPixel fieldPx=${layout.table.width/unitsPerPixel}x${layout.table.height/unitsPerPixel} " +
                "needlePx=${session.config.needleLength*pixelsPerMetre} " +
                "needleWidthPx=${max(.4f,session.config.needleWidth*layout.table.width/session.config.width)/unitsPerPixel+NeedleLighting.EXTRA_WIDTH_PIXELS} " +
                "strawWidthPx=${.025f*pixelsPerMetre} machineUnitPx=${layout.unit/unitsPerPixel} " +
                "leftPx=${layout.table.x/unitsPerPixel+safeArea.left} " +
                "topPx=${height-(layout.table.y+layout.table.height)/unitsPerPixel-safeArea.bottom}")
        }
    }
    override fun render(delta: Float) {
        if (suspended) return
        val wasReady=isReady
        isReady = assets.update()
        if(isReady && !wasReady) assets.get("ui/needle-panel-ru.fnt",BitmapFont::class.java).region.texture
            .setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear)
        if(isReady && !wasReady) assets.get("ui/nf-logo.png",Texture::class.java)
            .setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear)
        val dt = if (ignoreDelta || !delta.isFinite()) 0f else delta.coerceAtLeast(0f)
        ignoreDelta = !isReady
        if(objectPointer>=0 && objectCanRotate && !objectDragged && (System.nanoTime()-objectDown)>300_000_000L) equipmentDrawing.rotating=true
        needleLighting.advance(dt)
        workshopRuntime?.let { if(it.poll(session)) {
            cancelTouch(); physics.close(); physics=FactoryIntakePhysics(session)
            workshopView=if(session.phase==BatchPhase.RESULTS) WorkshopView.RESULTS else WorkshopView.NONE
            clock.reset(); refresh()
            it.save()
        } }
        if (isReady && session.running && (workshopRuntime==null || workshopRuntime.ready) &&
            workshopView==WorkshopView.NONE) clock.advance(dt, step) else clock.reset()
        layout.updatePit(session.pitWidth)
        lossFlashAge=(lossFlashAge+dt).coerceAtMost(10f)
        audio.update(dt,session,physics,workshopView!=WorkshopView.NONE)
        if (session.packaged != packagedSeen) { packagedSeen = session.packaged; feedback.found();audio.cue(AudioCue.METAL); refresh() }
        if (session.batch.complete && !loggedComplete) {
            loggedComplete = true
            if (build.diagnostics) Gdx.app.log("Factory", "COMPLETE ${session.summary()} bodies=$bodyCount peakMs=${peak*1000}")
            refresh()
        }
        workshopRuntime?.let { e ->
            val paidBefore=e.workshop.profile.delivered
            if(e.ready && !e.readOnly && e.persistence.error==null && e.workshop.creditDelivered(session.batch)>0) {
                for((id,value) in e.workshop.profile.delivered) if(id !in paidBefore) {
                    audio.cue(AudioCue.CREDIT)
                    val slot=payCursor++%payAges.size;payAges[slot]=0f;payLabels[slot]="+$value кр."
                }
                e.save();refresh()
            }
            if(e.ready && session.batch.complete && e.workshop.profile.receipt==null) {
                val receipt=e.workshop.settle(session.batch); session.showResults(); e.save()
                audio.cue(if(receipt.perfect) AudioCue.PERFECT else AudioCue.RESULT)
                cancelTouch(); workshopView=WorkshopView.RESULTS; feedback.found(); refresh()
                if(build.diagnostics) Gdx.app.log("Workshop","SETTLED id=${receipt.batchId} secured=${receipt.secured} processed=${receipt.processed} earned=${receipt.earned} wallet=${e.workshop.profile.workshopCredits}")
            }
        }
        val lost=metalImpactsSeen<session.metalImpacts
        if(strawImpactsSeen<session.strawImpacts) {strawImpactsSeen=session.strawImpacts;if(!lost) {feedback.strawImpact();audio.cue(AudioCue.STRAW)}}
        while(metalImpactsSeen<session.metalImpacts) {metalImpactsSeen++;feedback.needleLost();audio.cue(AudioCue.LOST);lossFlashAge=0f}
        if(workshopView==WorkshopView.NONE) for(i in payAges.indices) payAges[i]+=dt
        metricsAge += dt; logAge += dt; frameTotal += dt; frames++; peak = max(peak,dt)
        if(build.diagnostics && dt>0f) { frameTimes[frameSamples%frameTimes.size]=dt*1000; frameSamples++ }
        if (metricsAge >= .5f) {
            metrics = "${Gdx.graphics.framesPerSecond} FPS / ${(frameTotal*1000/max(1,frames)).toInt()} ms / $bodyCount bodies"
            frameTotal = 0f; frames = 0; metricsAge = 0f; refresh()
        }
        if (build.diagnostics && logAge >= 5f) {
            val samples=frameTimes.copyOf(minOf(frameSamples,frameTimes.size)).apply { sort() }
            val p95=if(samples.isEmpty()) 0f else samples[((samples.size-1)*.95f).toInt()]
            val over33=samples.count { it>33.34f }
            Gdx.app.log("Factory", "${session.summary()} running=${session.running} $metrics peakMs=${peak*1000} " +
                "throughput=${(session.batch.items.size-session.batch.count(Route.INTAKE))/max(.01f,session.batch.elapsed)} droppedS=${clock.droppedSeconds} p95Ms=$p95 over33=$over33/${samples.size}")
            if(session.config.manualProducts) Gdx.app.log("NeedleProbe",session.batch.needles.joinToString("|") {
                "${it.id}:${it.x}:${it.y}:${it.angle}:${it.route}:${if(it.route==Route.INTAKE) NeedleRecovery.fraction(session.batch,it) else 1f}"
            })
            if(session.config.grabClumps) Gdx.app.log("GrabProbe", "held=${physics.grabbedId} " +
                session.batch.items.filter { it.material==Material.STRAW && it.route==Route.INTAKE }.joinToString("|") {
                    val f=it.strands.first(); val a=it.angle+f.angle
                    val x=it.x+cos(it.angle)*f.x-sin(it.angle)*f.y-sin(a)*f.bend
                    val y=it.y+sin(it.angle)*f.x+cos(it.angle)*f.y+cos(a)*f.bend
                    "${it.id}:$x:$y"
                })
            frameSamples=0
            logAge = 0f
        }
        ScreenUtils.clear(bg); viewport.apply()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = viewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        diorama.alpha=clock.alpha
        diorama.draw(layout,session)
        equipmentDrawing.receiverRefund=(workshopRuntime?.workshop?.profile?.receiverInvestment ?: 0)/2
        if(session.config.pitReceiver) equipmentDrawing.draw(layout,session,physics)
        // Collect the SAME rendered straw poses, including curved infeed/belt/drop projection.
        needleVisibility.begin()
        val materialScale=layout.table.width/session.config.width
        for(index in session.batch.items.indices) {
            val item=session.batch.items[index]
            if(item.material!=Material.STRAW || item.route.terminal) continue
            val angle=locateItem(item)
            if(angle.isFinite()) needleVisibility.add(item,pos.x,pos.y,angle,projectedStrawScale(item,materialScale),materialScale)
        }
        // Draw hidden metal first; straw remains the visual cover on the intake.
        for(needle in session.batch.needles) if (!needle.route.terminal) drawItem(needle)
        for (item in session.batch.items) if (item.material == Material.STRAW && !item.route.terminal) drawItem(item)
        diorama.overhead(layout,session)
        // Captured metal sits on the lifted carriage, above the transport gantry.
        for(needle in session.batch.needles) {
            if(needle.route==Route.CAPTURE) drawItem(needle)
            if(needle.route==Route.MISSED && !session.config.manualProducts) needle(layout.missed.x,layout.missed.y+needle.id%3*3,
                needle.length*layout.table.width/session.config.width,max(.4f,needle.width*layout.table.width/session.config.width),0f)
        }
        if(session.config.pitReceiver) equipmentDrawing.hand(layout,session,physics,if(workshopView==WorkshopView.NONE && session.running) dt else 0f)
        drawTools()
        if(session.config.pitReceiver && lossFlashAge<.65f) {
            // One smooth peripheral warning pulse, never a strobe over the search material.
            val t=lossFlashAge/.65f
            val alpha=.62f*sin(PI.toFloat()*t).coerceAtLeast(0f)
            shapes.setColor(.92f,.20f,.16f,alpha)
            val r=layout.floor;val w=2.4f
            shapes.rect(r.x,r.y,r.width,w);shapes.rect(r.x,r.y+r.height-w,r.width,w)
            shapes.rect(r.x,r.y,w,r.height);shapes.rect(r.x+r.width-w,r.y,w,r.height)
        }
        hud.shapes(layout,session,build.diagnostics,telemetryExpanded,workshopRuntime?.workshop)
        shapes.end()
        sprites.projectionMatrix=viewport.camera.combined
        if (isReady) {
            val font=assets.get("ui/needle-panel-ru.fnt", BitmapFont::class.java)
            val logo=assets.get("ui/nf-logo.png",Texture::class.java)
            hud.text(layout,session,font,metrics,build.diagnostics,telemetryExpanded,workshopRuntime?.workshop,logo)
            if(workshopView==WorkshopView.NONE && session.config.pitReceiver) equipmentDrawing.text(layout,session,font)
            if(session.config.pitReceiver && workshopView==WorkshopView.NONE) {
                sprites.begin();font.data.setScale(11f/48f);font.color=FactoryPalette.green
                for(i in payAges.indices) if(payAges[i]<1.8f) font.draw(sprites,payLabels[i],
                    layout.pitHole.x+layout.pitHole.width+4+(i%2)*22,
                    layout.pitHole.y+layout.pitHole.height+12+payAges[i]*14+(i%4)*12)
                sprites.end()
            }
            if(workshopView!=WorkshopView.NONE) workshopRuntime?.let {
                Gdx.gl.glEnable(GL20.GL_BLEND)
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA)
                shapes.begin(ShapeRenderer.ShapeType.Filled)
                workshopPanel.liveShop.viewport=viewport
                if(workshopView==WorkshopView.SHOP) {workshopPanel.liveShop.layout(layout);workshopPanel.liveShop.prepare(it.workshop)}
                if(workshopView==WorkshopView.SOUND) audioPanel.shape(layout)
                else workshopPanel.shape(layout,workshopView,it.workshop.config.clumpEconomy)
                shapes.end()
                if(workshopView==WorkshopView.SOUND) audioPanel.text(font)
                else workshopPanel.text(workshopView,it,session,font,telemetryExpanded,logo)
            }
        }
    }
    private fun drawTools() {
        val table=layout.table; val scale=table.width/session.config.width
        if(session.guides) {
            shapes.color=FactoryPalette.steel
            // The bought lane replaces the old full-width receiver cheeks.
            val lip=table.x+session.intakeBoundary*scale
            val reach=if(session.conveyorOwned) .6f else 2f
            val depth=if(session.conveyorOwned) .2f else .65f
            shapes.triangle(lip-reach*scale,table.y,lip,table.y,lip,table.y+depth*scale)
            shapes.triangle(lip-reach*scale,table.y+table.height,lip,table.y+table.height,lip,table.y+table.height-depth*scale)
        }
        if(session.config.grabClumps && physics.pushing) {
            val point=layout.yardPoint(physics.pusherX,physics.pusherY,pos)
            val x=point.x; val y=point.y
            shapes.color=FactoryPalette.steel
            shapes.circle(x,y,2.5f,16)
            shapes.color=FactoryPalette.dark; shapes.circle(x,y,1.25f,12)
            if(session.handCapacity>1) {
                shapes.color=FactoryPalette.steel
                val reach=session.handReach*scale
                shapes.rectLine(x,y-reach,x,y+reach,1.4f)
                val teeth=if(workshopRuntime?.workshop?.tool==HandTool.RAKE) 3 else minOf(session.handCapacity,12)
                for(i in 0 until teeth) {
                    val yy=y-reach+2*reach*i/(teeth-1)
                    shapes.rectLine(x-3,yy,x+3,yy,1.4f)
                }
            }
        } else if(session.rake && physics.pushing) {
            val x=table.x+physics.pusherX*scale; val y=table.y+physics.pusherY*scale
            shapes.color=FactoryPalette.steel
            val reach=session.config.pusherRadius*1.8f*scale
            shapes.rectLine(x+.084f*scale,y-reach,x+.24f*scale,y,3f)
            shapes.rectLine(x+.24f*scale,y,x+.084f*scale,y+reach,3f)
            for(i in -3..3) {
                val toothY=i*reach/3; val toothX=x+(.24f-.156f*kotlin.math.abs(i)/3)*scale
                shapes.rectLine(toothX-4,y+toothY,toothX+2,y+toothY,2f)
            }
            shapes.color=FactoryPalette.brass; shapes.rect(x-22,y-3,20f,6f)
        }
    }
    private fun drawItem(item: FactoryItem) {
        val angle=locateItem(item)
        if(!angle.isFinite()) return
        val scale=layout.table.width/session.config.width
        if(item.material==Material.NEEDLE) {
            val drawScale=if(session.config.physicalNeedles) projectedStrawScale(item,scale) else scale
            // Lifted capture is rendered above straw. Otherwise evaluate the current screen geometry.
            needleVisibleFraction=if(item.route==Route.CAPTURE) 1f else
                needleVisibility.fraction(pos.x,pos.y,item.length*drawScale,angle,
                    .25f*viewport.worldWidth/surfaceWidth.coerceAtLeast(1))
            needle(pos.x,pos.y,item.length*drawScale,max(.4f,item.width*drawScale),angle,needleVisibleFraction)
        } else tuft(item,pos.x,pos.y,scale,angle)
    }
    private fun locateItem(item: FactoryItem): Float {
        val alpha = clock.alpha
        val t = item.previousProgress+(item.progress-item.previousProgress)*alpha
        val scale = layout.table.width/session.config.width
        var angle = 0f
        if(session.config.manualProducts && item.route!=Route.INTAKE) {
            if(session.config.pitReceiver) {
                val scale=layout.table.width/session.config.width
                when(item.route) {
                    Route.RECEIVING -> {
                        val toX=layout.pitHole.x+layout.pitHole.width/2; val toY=layout.pitHole.y+layout.pitHole.height/2
                        layout.yardPoint(item.x,item.y,pos)
                        val fromX=pos.x; val fromY=pos.y
                        pos.set(fromX+(toX-fromX)*t,fromY+(toY-fromY)*t)
                        return item.angle+layout.yardAngle+t*1.4f
                    }
                    Route.TRANSFER -> { pos.set(layout.pitHole.x+layout.pitHole.width/2,layout.pitHole.y+layout.pitHole.height/2); return t*1.4f }
                    Route.CHUTE -> { pos.set(layout.needleReceiver.x+layout.needleReceiver.width/2,layout.needleReceiver.y+layout.needleReceiver.height/2); return item.angle+layout.yardAngle }
                    else -> return Float.NaN
                }
            }
            when(item.route) {
                Route.RECEIVING -> {
                    val receiver=layout.productReceiver
                    val fromX=layout.table.x+item.x*scale; val fromY=layout.table.y+item.y*scale
                    val toX=if(session.conveyorOwned) layout.productLaneX(session.config.conveyorLaneX) else receiver.x+receiver.width/2
                    val toY=if(session.conveyorOwned) layout.productLaneY(item.y.coerceAtLeast(session.config.conveyorDischargeY))
                        else if(session.baler || session.config.physicalNeedles) layout.productTransferStart.y else fromY
                    pos.set(fromX+(toX-fromX)*t,fromY+(toY-fromY)*t)
                    return item.angle*(1-t)
                }
                Route.BELT -> {
                    layout.productBeltEnd(session.config.conveyorLaneX,
                        session.config.height-(session.config.height-session.config.conveyorDischargeY)*t,pos)
                    return item.angle*(1-t)
                }
                Route.CAPTURE -> {
                    layout.productMetalPath(layout.productLaneX(session.config.conveyorLaneX),
                        layout.productLaneY(session.config.conveyorDischargeY),t*t*(3-2*t),pos)
                    return item.angle*(1-t)
                }
                Route.TRANSFER -> {
                    val destination=if(session.baler) layout.productBalerInput else layout.productLooseOutlet
                    val split=.75f
                    val from=if(session.conveyorOwned) layout.productBeltEnd(session.config.conveyorLaneX,session.config.conveyorDischargeY,pos) else layout.productTransferStart
                    if(item.material==Material.NEEDLE && t>split) {
                        pos.set(from).lerp(destination,split)
                        pos.lerp(layout.productReject,(t-split)/(1f-split))
                    } else pos.set(from).lerp(destination,t)
                    return 0f
                }
                Route.CHUTE -> {
                    layout.productMetalPath(layout.table.x+item.x*scale,layout.table.y+item.y*scale,t,pos)
                    return item.angle*(1-t)
                }
                Route.BALER_QUEUE,Route.BALING -> return Float.NaN // Contained product drawn by the machine.
                else -> return Float.NaN
            }
        }
        val beltAngle = atan2(layout.end.y-layout.start.y, layout.end.x-layout.start.x)
        when(item.route) {
            Route.INTAKE -> {
                if(session.config.pitReceiver) layout.yardPoint(item.previousX+(item.x-item.previousX)*alpha,
                    item.previousY+(item.y-item.previousY)*alpha,pos)
                else pos.set(layout.table.x+(item.previousX+(item.x-item.previousX)*alpha)*scale,
                    layout.table.y+(item.previousY+(item.y-item.previousY)*alpha)*scale)
                angle = item.previousAngle + atan2(sin(item.angle-item.previousAngle),cos(item.angle-item.previousAngle))*alpha + if(session.config.pitReceiver) layout.yardAngle else 0f
            }
            Route.BELT -> {
                layout.belt(t,pos)
                angle=beltAngle
                if(t<FactoryLayout.HANDOFF) {
                    val blend=(t/FactoryLayout.HANDOFF).coerceIn(0f,1f)
                    pos.x=layout.table.x+item.x*scale+(layout.start.x-layout.table.x-item.x*scale)*blend
                    pos.y=layout.table.y+item.y*scale+(layout.start.y-layout.table.y-item.y*scale)*blend
                    angle=item.angle+atan2(sin(beltAngle-item.angle),cos(beltAngle-item.angle))*blend
                } else if(t>FactoryLayout.DISCHARGE) {
                    val target=if(item.material==Material.STRAW) layout.residue else layout.missed
                    val blend=(t-FactoryLayout.DISCHARGE)/(1f-FactoryLayout.DISCHARGE)
                    pos.set(layout.end).lerp(target,blend)
                    val direction=atan2(target.y-layout.end.y,target.x-layout.end.x)
                    angle=beltAngle+atan2(sin(direction-beltAngle),cos(direction-beltAngle))*blend
                }
            }
            Route.CAPTURE -> {
                val smooth=t*t*(3-2*t)
                pos.set(layout.magnet).lerp(layout.capture,smooth)
                angle = beltAngle*(1-smooth)
            }
            Route.CHUTE -> {
                pos.set(layout.capture).lerp(layout.buffer,t)
                var direction=atan2(layout.buffer.y-layout.capture.y,layout.buffer.x-layout.capture.x)
                if(direction>PI.toFloat()/2) direction-=PI.toFloat()
                if(direction< -PI.toFloat()/2) direction+=PI.toFloat()
                angle=direction*min(1f,min(t/.15f,(1f-t)/.15f))
            }
            Route.BUFFER -> pos.set(layout.buffer)
            Route.PACKING -> {
                pos.set(layout.buffer).lerp(layout.packer,(t*3f).coerceIn(0f,1f))
                if(!layout.landscape) angle=PI.toFloat()/2*min(1f,t/.06f)*
                    (1f-((t-.32f)/.11f).coerceIn(0f,1f))
            }
            else -> return Float.NaN
        }
        return angle
    }
    private fun projectedStrawScale(item:FactoryItem,scale:Float):Float {
        val progress=item.previousProgress+(item.progress-item.previousProgress)*clock.alpha
        if(session.config.pitReceiver && item.route==Route.TRANSFER) return scale*.25f*(1f-progress)
        if(session.config.manualProducts && item.route==Route.RECEIVING) return scale*(1f-.75f*progress)
        if(session.config.manualProducts && (item.route==Route.TRANSFER || item.route==Route.BELT || item.route==Route.CAPTURE)) return scale*.25f
        // Straw tips disappear into the receiving hopper as material tilts down the drop.
        // This is projection/occlusion only; the terminal route still accounts for every patch.
        val drop=if(item.route==Route.BELT) ((progress-FactoryLayout.DISCHARGE)/(1f-FactoryLayout.DISCHARGE)).coerceIn(0f,1f) else 0f
        // Shallow perspective compresses a loose crossing patch into the narrow belt bed.
        // Retain crossing angles instead of turning every load into a parallel brush.
        val beltScale=if(item.route==Route.BELT) 1f-.55f*(progress/FactoryLayout.HANDOFF).coerceIn(0f,1f) else 1f
        return scale*beltScale*(1f-.55f*drop)
    }
    /** Smooth quadratic ribbons with continuous tangent normals and tapered tips, no kinked sticks. */
    private fun tuft(item:FactoryItem,x:Float,y:Float,scale:Float,a:Float) {
        if(session.config.pitReceiver && (item.route==Route.RECEIVING || item.route==Route.TRANSFER)) {
            val t=item.progress
            val crush=if(item.route==Route.TRANSFER) .65f+.35f*t else .65f*t
            shapes.color=straw[item.id%straw.size]
            for(i in 0 until 14) {
                val phase=i*2.399f+item.id*.37f
                val radius=(1f-crush)*minOf(layout.pitHole.width*.30f,11f)
                val ax=x+cos(phase+crush*3f)*radius;val ay=y+sin(phase+crush*3f)*radius*.55f
                val len=(1f-crush)*(7f+(i%4)*2f)
                shapes.rectLine(ax,ay,ax+cos(phase)*len,ay+sin(phase)*len,.65f)
            }
            return
        }
        val bodyCos=cos(a); val bodySin=sin(a)
        val projectedScale=projectedStrawScale(item,scale)
        val aligned=1f
        for(index in item.strands.indices) {
            val f=item.strands[index]
            val ca=cos(a+f.angle*aligned); val sa=sin(a+f.angle*aligned)
            val sx=x+(bodyCos*f.x-bodySin*f.y*aligned)*projectedScale
            val sy=y+(bodySin*f.x+bodyCos*f.y*aligned)*projectedScale
            val length=f.length*projectedScale
            val bend=f.bend*projectedScale
            val offset=0f
            val half=f.width*scale*.5f
            shapes.color=straw[f.tone]
            for(s in 0 until 10) {
                val u=s/10f-.5f; val v=(s+1)/10f-.5f
                val yy=offset+bend*(1-4*u*u); val zy=offset+bend*(1-4*v*v)
                val slope=-8*bend*u/length; val slope2=-8*bend*v/length
                val normal=half/sqrt(1+slope*slope); val normal2=half/sqrt(1+slope2*slope2)
                val h=normal*min(1f,(.5f-abs(u))*12f); val h2=normal2*min(1f,(.5f-abs(v))*12f)
                val ax=sx+ca*(u*length-slope*h)-sa*(yy+h); val ay=sy+sa*(u*length-slope*h)+ca*(yy+h)
                val bx=sx+ca*(u*length+slope*h)-sa*(yy-h); val by=sy+sa*(u*length+slope*h)+ca*(yy-h)
                val cx=sx+ca*(v*length-slope2*h2)-sa*(zy+h2); val cy=sy+sa*(v*length-slope2*h2)+ca*(zy+h2)
                val dx=sx+ca*(v*length+slope2*h2)-sa*(zy-h2); val dy=sy+sa*(v*length+slope2*h2)+ca*(zy-h2)
                shapes.triangle(ax,ay,bx,by,cx,cy); shapes.triangle(bx,by,dx,dy,cx,cy)
            }
        }
    }
    private fun needle(x:Float,y:Float,length:Float,width:Float,a:Float,visibility:Float=1f) {
        val visualWidth = NeedleLighting.visualWidth(width, viewport.worldWidth / surfaceWidth.coerceAtLeast(1))
        val c=cos(a); val s=sin(a); val h=visualWidth*.5f
        val backX=x-c*length*.5f; val backY=y-s*length*.5f
        val tipX=x+c*length*.5f; val tipY=y+s*length*.5f
        val shaftX=x+c*length*.34f; val shaftY=y+s*length*.34f
        val gain=NeedleVisibility.highlightGain(visibility)
        // Preserve the accepted early-search body exactly; polish is earned by exposure.
        metalPolish.set(metal).lerp(.56f,.62f,.65f,1f,gain)
        shapes.color=metalPolish
        shapes.rectLine(backX,backY,shaftX,shaftY,visualWidth)
        shapes.triangle(shaftX-s*h,shaftY+c*h,shaftX+s*h,shaftY-c*h,tipX,tipY)
        // Restrained cylindrical metal: dark lower facet and a short, tapered internal reflection.
        shapes.color=metalEdge
        shapes.rectLine(backX+s*h*.6f,backY-c*h*.6f,shaftX+s*h*.6f,shaftY-c*h*.6f,visualWidth*.18f)
        if(gain>0f) {
            val strength=needleLighting.strength(c,s)
            val center=needleLighting.center(c,s)*length
            val middleX=x+c*center-s*h*.12f; val middleY=y+s*center+c*h*.12f
            // Two nested, vertex-faded lobes: steel shoulder and a small silver core.
            // Entirely inside the shaft, no halo/FBO/shader or timed sparkle.
            metalReflection.set(.74f,.80f,.84f,gain*(.28f+.30f*strength))
            sheen(middleX,middleY,c,s,length*.20f,visualWidth*.34f)
            metalReflection.set(.91f,.94f,.96f,gain*(.40f+.55f*strength))
            sheen(middleX,middleY,c,s,length*.11f,visualWidth*(.14f+.03f*strength))
            // Owner's archive reference adds a small late-reveal star. Stage 2 stays unchanged.
            // Draw before straw; never display a star centred on a covered piece of shaft.
            if(workshopRuntime?.workshop?.config?.archiveTools==true && visibility>.75f &&
                needleVisibility.fraction(middleX,middleY,.01f,a)>.95f) {
                val starGain=NeedleGlint.gain(visibility,strength)
                val pixel=viewport.worldWidth/surfaceWidth.coerceAtLeast(1)
                metalReflection.set(.91f,.95f,1f,starGain)
                sheen(middleX,middleY,c,s,(if(visibility>=.999f) 17f else 9f)*pixel,1.1f*pixel)
                sheen(middleX,middleY,-s,c,(if(visibility>=.999f) 11f else 4.5f)*pixel,.85f*pixel)
            }
        }
        // Tiny dark eye; no glow, length pulse, external outline or through-straw highlight.
        shapes.color=dark; shapes.rectLine(backX+c*length*.07f,backY+s*length*.07f,
            backX+c*length*.14f,backY+s*length*.14f,max(.25f,visualWidth*.24f))
    }
    private fun sheen(x:Float,y:Float,c:Float,s:Float,span:Float,band:Float) {
        metalFalloff.set(metalReflection); metalFalloff.a=0f
        shapes.triangle(x-c*span,y-s*span,x-s*band,y+c*band,x+s*band,y-c*band,
            metalFalloff,metalReflection,metalReflection)
        shapes.triangle(x+c*span,y+s*span,x+s*band,y-c*band,x-s*band,y+c*band,
            metalFalloff,metalReflection,metalReflection)
    }
    private fun refresh() { hud.refresh(session,workshopRuntime?.workshop) }
    override fun pause() { audio.pause();suspended=true; cancelTouch(); clock.reset(); session.synchronizeSnapshots() }
    override fun resume() { audio.resume();suspended=false; ignoreDelta=true; clock.reset(); session.synchronizeSnapshots() }
    override fun hide() { audio.pause();cancelTouch(); if(Gdx.input.inputProcessor===input) Gdx.input.inputProcessor=null }
    override fun dispose() { hide();audio.dispose(); physics.close(); equipmentDrawing.dispose(); shapes.dispose(); sprites.dispose(); assets.dispose() }
}

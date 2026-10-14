package com.needlefactory.lwjgl3

import com.badlogic.gdx.*
import com.badlogic.gdx.backends.lwjgl3.*
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.needlefactory.game.*
import com.needlefactory.presentation.*
import com.needlefactory.economy.*
import com.needlefactory.simulation.*
import com.needlefactory.save.*
import java.io.File
import kotlin.math.*

/** New candidate through real input, GL, native physics and persistence; no route/credit injection. */
object TransportSmoke {
    @JvmStatic fun main(args:Array<String>) {
        val preview=System.getProperty("needlefactory.transport.preview","").isNotBlank()
        val save=if(preview) File(System.getProperty("needlefactory.transport.preview"))
            else File("verification/transport-smoke-${System.currentTimeMillis()}/profile.json")
        val game=NeedleFactoryGame(BuildInfo("clump-grab","debug",true),seed=42,saveFile=save)
        lateinit var screen:FactoryScreen
        var failure:Throwable?=null; var frames=0; var batch=0; var tick=0; var idle=0
        var pictured=false; var early=false; var handled=0; var fullSorted=false
        var beltPicture=false; var magnetPicture=false; var shopPicture=false
        var glintPicture=false
        var startX=0f; var startY=0f
        var endX=10f; var endY=4f
        fun point(x:Float,y:Float):Pair<Int,Int> {
            val l=screen.layout
            return ((l.table.x+x/10*l.table.width)/l.width*Gdx.graphics.width).roundToInt() to
                ((1-(l.table.y+y/8*l.table.height)/l.height)*Gdx.graphics.height).roundToInt()
        }
        fun touch(x:Float,y:Float,down:Boolean=false,up:Boolean=false) {
            val p=point(x,y); val input=Gdx.input.inputProcessor
            if(down) input.touchDown(p.first,p.second,0,0)
            else if(up) input.touchUp(p.first,p.second,0,0)
            else input.touchDragged(p.first,p.second,0)
        }
        fun picture(name:String,portrait:Boolean) {
            val w=if(portrait) 1080 else 2340; val h=if(portrait) 2340 else 1080
            val fbo=FrameBuffer(Pixmap.Format.RGBA8888,w,h,false)
            val before=screen.session.summary().replace(Regex("armed=\\d+"),"armed=0")
            try {
                fbo.begin(); screen.resize(w,h); screen.render(0f)
                val pixels=Pixmap.createFromFrameBuffer(0,0,w,h); val png=PixmapIO.PNG()
                try { png.setFlipY(true); png.write(FileHandle("verification/transport-$name-${if(portrait) "portrait" else "landscape"}.png"),pixels) }
                finally { pixels.dispose(); png.dispose() }
            } finally { fbo.end(); fbo.dispose(); screen.resize(Gdx.graphics.width,Gdx.graphics.height) }
            check(before==screen.session.summary())
        }
        val app=object:ApplicationAdapter() {
            override fun create() { game.create(); screen=game.screen as FactoryScreen }
            override fun resize(w:Int,h:Int)=game.resize(w,h)
            override fun dispose() { game.workshopRuntime?.persistence?.close(); game.dispose() }
            override fun render() {
                try {
                    screen.render(1f/60f); val runtime=game.workshopRuntime!!
                    if(!runtime.ready || !screen.isReady) return
                    val s=screen.session; val w=runtime.workshop
                    check(++frames<300000) { "Grab timeout ${s.summary()}" }
                    if(preview) {
                        check(s.conveyorOwned && s.magnetOwned) { "Preview needs legitimately purchased equipment" }
                        if(runtime.busy) return
                        if(s.phase==BatchPhase.RESULTS) {
                            if(w.config.archiveTools && UpgradeId.GUIDES.key !in w.profile.purchased &&
                                w.profile.workshopCredits>=w.price(UpgradeId.GUIDES)) {
                                check(screen.purchase(UpgradeId.GUIDES)==PurchaseResult.BOUGHT)
                                return
                            }
                            screen.openWorkshop(); picture("polished-shop-tools",true); picture("polished-shop-tools",false)
                            val l=screen.layout; val y=(l.height-430)/2+330
                            Gdx.input.inputProcessor.touchDown(Gdx.graphics.width/2,((1-y/l.height)*Gdx.graphics.height).toInt(),0,0)
                            picture("polished-shop-transport",true); picture("polished-shop-transport",false)
                            check(screen.nextBatch()); return
                        }
                        picture("polished-magnet",true); picture("polished-magnet",false)
                        check(s.batch.count(Route.INTAKE)==27 && s.batch.touches==0)
                        println("TRANSPORT_PREVIEW PASS real purchased profile / same state across orientations")
                        Gdx.app.exit(); return
                    }
                    if(batch!=s.batchNumber) { batch=s.batchNumber; idle=0; tick=0 }
                    if(!pictured) { pictured=true; picture("starter",true); picture("starter",false) }
                    if(s.phase==BatchPhase.ACTIVE && s.conveyorOwned && !beltPicture) {
                        beltPicture=true; picture("active-belt",true); picture("active-belt",false)
                    }
                    if(s.phase==BatchPhase.ACTIVE && s.magnetOwned && !magnetPicture) {
                        magnetPicture=true; picture("active-magnet",true); picture("active-magnet",false)
                    }
                    if(s.phase==BatchPhase.ACTIVE) {
                        if(idle++<120) check(s.batch.count(Route.INTAKE)==27)
                        else if(fullSorted && w.eligible(s.batch)) {
                            early=true; check(screen.requestFinish()); check(screen.confirmFinish())
                        } else {
                                if(tick==0) {
                                    val metal=if(s.conveyorOwned && s.batch.items.any { it.material==Material.STRAW && it.route==Route.INTAKE }) null else s.batch.needles.firstOrNull { it.route==Route.INTAKE && NeedleRecovery.canPick(s.batch,it,it.x,it.y) }
                                    val item=metal ?: s.batch.items.lastOrNull { it.material==Material.STRAW && it.route==Route.INTAKE }
                                    if(item!=null) {
                                        if(metal!=null) {
                                            if(!glintPicture && NeedleRecovery.fraction(s.batch,metal)>.95f) {
                                                glintPicture=true; picture("archive-reveal",true); picture("archive-reveal",false)
                                            }
                                            startX=metal.x; startY=metal.y; endX=if(s.conveyorOwned) 8.6f else s.config.metalDockX; endY=if(s.conveyorOwned) 4f else -.85f
                                        } else {
                                            val f=item.strands.first(); val a=item.angle+f.angle
                                            startX=item.x+cos(item.angle)*f.x-sin(item.angle)*f.y-sin(a)*f.bend
                                            startY=item.y+sin(item.angle)*f.x+cos(item.angle)*f.y+cos(a)*f.bend
                                            endX=if(s.conveyorOwned) 8.6f else 10f; endY=4f
                                        }
                                        touch(startX,startY,down=true)
                                    }
                                }
                                if(tick in 1..119) {
                                    val t=(tick/90f).coerceAtMost(1f)
                                    touch(startX+(endX-startX)*t,startY+(endY-startY)*t)
                                }
                                if(tick==120) touch(endX,endY,up=true)
                                tick=(tick+1)%132
                        }
                    }
                    if(frames%900==0) {
                        val before=s.summary().replace(Regex("armed=\\d+"),"armed=0")
                        game.pause(); game.resume(); check(before==s.summary()); tick=0
                    }
                    if(s.phase==BatchPhase.RESULTS && !runtime.busy) {
                        if(handled!=batch) {
                            handled=batch; val r=w.profile.receipt!!
                            check(r.material==(r.loose*w.config.looseValue+r.bundled*w.config.bundledValue)/24)
                            val credits=w.profile.workshopCredits
                            check(w.settle(s.batch)==r && w.profile.workshopCredits==credits)
                            println("TRANSPORT_BATCH batch=$batch seconds=${s.batch.elapsed} receipt=$r wallet=$credits")
                            if(!early) { check(r.secured+r.missed==3 && r.processed==24); check(r.earned==(if(s.baler) w.config.bundledValue else w.config.looseValue)+w.config.needleValue*(r.secured-r.sorted)+w.config.sortedNeedleValue*r.sorted) }
                            if(s.magnetOwned && !early) { check(r.sorted==3); fullSorted=true; picture("magnet",true); picture("magnet",false) }
                            else if(s.conveyorOwned && !early) { check(r.missed==3); picture("belt",true); picture("belt",false) }
                            if(UpgradeId.RAKE.key !in w.profile.purchased && credits>=w.price(UpgradeId.RAKE)) check(screen.purchase(UpgradeId.RAKE)==PurchaseResult.BOUGHT)
                            else if(w.config.archiveTools && UpgradeId.RAKE.key in w.profile.purchased &&
                                UpgradeId.STEEL_RAKE.key !in w.profile.purchased && credits>=w.price(UpgradeId.STEEL_RAKE)) {
                                check(screen.purchase(UpgradeId.STEEL_RAKE)==PurchaseResult.BOUGHT)
                                check(w.tool==HandTool.STEEL_RAKE && s.handCapacity==3)
                            }
                            else if(!s.baler && UpgradeId.RAKE.key in w.profile.purchased && credits>=w.price(UpgradeId.BALER)) check(screen.purchase(UpgradeId.BALER)==PurchaseResult.BOUGHT)
                            else if(s.baler && !s.conveyorOwned && credits>=w.price(UpgradeId.CONVEYOR)) check(screen.purchase(UpgradeId.CONVEYOR)==PurchaseResult.BOUGHT)
                            else if(s.conveyorOwned && !s.magnetOwned && credits>=w.price(UpgradeId.MAGNET)) check(screen.purchase(UpgradeId.MAGNET)==PurchaseResult.BOUGHT)
                            if(s.baler && !shopPicture) {
                                shopPicture=true; screen.openWorkshop(); picture("shop-tools",true); picture("shop-tools",false)
                                val l=screen.layout; val y=(l.height-430)/2+330
                                Gdx.input.inputProcessor.touchDown((Gdx.graphics.width/2),((1-y/l.height)*Gdx.graphics.height).toInt(),0,0)
                                picture("shop-transport",true); picture("shop-transport",false)
                            }
                            if(early) {
                                check(r.discarded>0); check(WorkshopStore(save).load(42).profile==w.profile)
                                if(w.config.archiveTools) check(UpgradeId.STEEL_RAKE.key in w.profile.purchased)
                                picture("early",true)
                                println("TRANSPORT_SMOKE PASS batches=$batch frames=$frames purchased=${w.profile.purchased}")
                                Gdx.app.exit(); return
                            }
                        }
                        if(!runtime.busy) check(screen.nextBatch())
                    }
                } catch(t:Throwable) { failure=t; t.printStackTrace(); Gdx.app.exit() }
            }
        }
        Lwjgl3Application(app,Lwjgl3ApplicationConfiguration().apply {
            setTitle("NeedleFactory grab QA"); setWindowedMode(480,880); setInitialVisible(false)
            setPauseWhenMinimized(false); setPauseWhenLostFocus(false); useVsync(false); setForegroundFPS(0); disableAudio(true)
        })
        failure?.let { throw IllegalStateException("Grab smoke failed",it) }
    }
}

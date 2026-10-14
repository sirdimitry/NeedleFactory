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

/** Real UI/native input, no injected credits, no direct feed/route/recovery calls. */
object ProductSmoke {
    @JvmStatic fun main(args:Array<String>) {
        val save=File("verification/product-smoke-${System.currentTimeMillis()}/profile.json")
        val game=NeedleFactoryGame(BuildInfo("products-v2","debug",true),seed=42,
            factorySession=FactorySession(com.needlefactory.content.FactoryConfig.products(),42),saveFile=save)
        lateinit var screen:FactoryScreen
        var failure:Throwable?=null; var frames=0; var active=0; var tick=0; var passes=0; var idle=0
        var resultHandled=0; var purchasedBaler=false; var bundledBatch=0; var early=false
        var strokeY=.65f; var search=true
        fun point(x:Float,y:Float):Pair<Int,Int> {
            val l=screen.layout
            return ((l.table.x+x/10*l.table.width)/l.width*Gdx.graphics.width).roundToInt() to
                ((1-(l.table.y+y/8*l.table.height)/l.height)*Gdx.graphics.height).roundToInt()
        }
        fun picture(name:String,portrait:Boolean) {
            val w=if(portrait) 1080 else 2340; val h=if(portrait) 2340 else 1080
            val before=screen.session.summary(); val fbo=FrameBuffer(Pixmap.Format.RGBA8888,w,h,false)
            try {
                fbo.begin(); screen.resize(w,h); screen.setSafeArea(SafeArea(top=96,bottom=45)); screen.render(0f)
                val pixmap=Pixmap.createFromFrameBuffer(0,0,w,h); val png=PixmapIO.PNG()
                try { png.setFlipY(true); png.write(FileHandle("verification/products-$name-${if(portrait) "portrait" else "landscape"}.png"),pixmap) }
                finally { png.dispose(); pixmap.dispose() }
            } finally { fbo.end(); fbo.dispose(); screen.setSafeArea(SafeArea()); screen.resize(Gdx.graphics.width,Gdx.graphics.height) }
            check(before.replace(Regex("armed=\\d+"),"armed=0")==screen.session.summary())
        }
        fun pick():Boolean {
            val s=screen.session
            for(n in s.batch.needles) if(n.route==Route.INTAKE && NeedleRecovery.fraction(s.batch,n)>.6f) {
                for(k in 0..30) {
                    val a=(k/30f-.5f)*n.length; val x=n.x+cos(n.angle)*a; val y=n.y+sin(n.angle)*a
                    if(!NeedleRecovery.canPick(s.batch,n,x,y)) continue
                    val p=point(x,y); val input=Gdx.input.inputProcessor
                    input.touchUp(p.first,p.second,0,0)
                    input.touchDown(p.first,p.second,0,0); input.touchUp(p.first,p.second,0,0)
                    if(n.route==Route.RECOVERED) { tick=0; passes++; return true }
                }
            }
            return false
        }
        val app=object:ApplicationAdapter() {
            override fun create() { game.create(); screen=game.screen as FactoryScreen }
            override fun resize(w:Int,h:Int)=game.resize(w,h)
            override fun dispose() { game.workshopRuntime?.persistence?.close(); game.dispose() }
            override fun render() {
                try {
                    screen.render(1f/60f); val runtime=game.workshopRuntime!!
                    if(!runtime.ready || !screen.isReady) return
                    check(++frames<120000) { "Product timeout ${screen.session.summary()}" }
                    val s=screen.session; val w=runtime.workshop
                    if(active!=s.batchNumber) {
                        active=s.batchNumber; idle=0; tick=0; passes=0; search=true
                        if(active==1) { picture("starter",true); picture("starter",false) }
                        if(s.baler && bundledBatch==0) { bundledBatch=active; picture("baler",true); picture("baler",false) }
                    }
                    if(s.phase==BatchPhase.ACTIVE) {
                        if(idle++<120) check(s.batch.count(Route.INTAKE)==123)
                        else if(purchasedBaler && active>bundledBatch && !early && w.eligible(s.batch)) {
                            early=true; check(screen.requestFinish()); picture("early-preview",true); check(screen.confirmFinish())
                        } else if(!pick()) {
                            check(passes<100) { "Product manual drain stalled ${s.summary()}" }
                            val i=tick%120
                            if(i==0) {
                                search=s.batch.needles.any { it.route==Route.INTAKE } && passes<35
                                val remaining=s.batch.items.firstOrNull { it.material==Material.STRAW && it.route==Route.INTAKE }
                                strokeY=if(!search && passes>=35 && remaining!=null) remaining.y else .65f+(passes%7)*1.1f
                            }
                            val a=i.coerceAtMost(99)/99f
                            val x=if(search) 9.35f-8.7f*a else .65f+9.35f*a
                            val p=point(x,strokeY); val input=Gdx.input.inputProcessor
                            if(i==0) check(input.touchDown(p.first,p.second,0,0))
                            if(i in 1..99) input.touchDragged(p.first,p.second,0)
                            if(i==100) { input.touchUp(p.first,p.second,0,0); passes++ }
                            tick++
                        }
                    }
                    if(frames%900==0) {
                        val before=s.summary(); game.pause(); game.resume()
                        check(before.replace(Regex("armed=\\d+"),"armed=0")==s.summary())
                    }
                    if(s.phase==BatchPhase.RESULTS && !runtime.busy) {
                        if(resultHandled!=active) {
                            resultHandled=active
                            val r=w.profile.receipt!!
                            println("PRODUCT_BATCH id=$active seconds=${s.batch.elapsed} receipt=$r earned=${r.earned} wallet=${w.profile.workshopCredits}")
                            check(r.economyModel=="products-v2" && r.bonus==0L)
                            check(r.material==(r.loose*6L+r.bundled*16L)/120)
                            val wallet=w.profile.workshopCredits; check(w.settle(s.batch)==r && w.profile.workshopCredits==wallet)
                            if(!s.baler) check(r.bundled==0)
                            if(UpgradeId.RAKE.key !in w.profile.purchased && wallet>=w.price(UpgradeId.RAKE)) {
                                check(screen.purchase(UpgradeId.RAKE)==PurchaseResult.BOUGHT)
                            } else if(UpgradeId.GUIDES.key !in w.profile.purchased && wallet>=w.price(UpgradeId.GUIDES)) {
                                check(screen.purchase(UpgradeId.GUIDES)==PurchaseResult.BOUGHT)
                            } else if(!s.baler && UpgradeId.RAKE.key in w.profile.purchased && wallet>=w.price(UpgradeId.BALER)) {
                                screen.openWorkshop(); picture("shop",true); picture("shop",false)
                                check(screen.purchase(UpgradeId.BALER)==PurchaseResult.BOUGHT); purchasedBaler=true
                            }
                            if(active==bundledBatch) { check(r.bundled==120 && r.material==16L); picture("bundled-results",true) }
                            if(early) {
                                check(r.discarded>0)
                                val loaded=WorkshopStore(save).load(42).profile; check(loaded==w.profile)
                                picture("early-results",false)
                                println("PRODUCT_SMOKE PASS batches=$active frames=$frames wallet=${loaded.workshopCredits} purchased=${loaded.purchased}")
                                Gdx.app.exit(); return
                            }
                        }
                        if(!runtime.busy) check(screen.nextBatch())
                    }
                } catch(t:Throwable) { failure=t; t.printStackTrace(); Gdx.app.exit() }
            }
        }
        Lwjgl3Application(app,Lwjgl3ApplicationConfiguration().apply {
            setTitle("NeedleFactory product QA"); setWindowedMode(480,880); setInitialVisible(false)
            setPauseWhenMinimized(false); setPauseWhenLostFocus(false); useVsync(false); setForegroundFPS(0); disableAudio(true)
        })
        failure?.let { throw IllegalStateException("Product smoke failed",it) }
    }
}

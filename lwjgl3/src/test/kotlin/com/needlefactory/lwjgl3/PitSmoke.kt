package com.needlefactory.lwjgl3

import com.badlogic.gdx.*
import com.badlogic.gdx.backends.lwjgl3.*
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.needlefactory.game.*
import com.needlefactory.content.FactoryConfig
import com.needlefactory.presentation.*
import com.needlefactory.economy.*
import com.needlefactory.simulation.*
import com.needlefactory.save.*
import java.io.File
import kotlin.math.*

/** Real GL/input/physics, no injected money or item routes. Three earned lots buy a port. */
object PitSmoke {
    @JvmStatic fun main(args:Array<String>) {
        val save=File("verification/pit-smoke-${System.currentTimeMillis()}/profile.json")
        val game=NeedleFactoryGame(BuildInfo("pit","debug",true),seed=42,saveFile=save,
            factorySession=FactorySession(FactoryConfig.pitProducts(),42))
        lateinit var screen:FactoryScreen; var failure:Throwable?=null
        var frames=0; var tick=0; var handled=0; var pictured=false; var batch=0; var idle=0
        var sx=0f; var sy=0f; var tx=0f; var ty=0f
        fun input(x:Float,y:Float,action:Int) {
            val l=screen.layout
            val point=l.yardPoint(x,y,com.badlogic.gdx.math.Vector2())
            val px=(point.x/l.width*Gdx.graphics.width).roundToInt()
            val py=((1-point.y/l.height)*Gdx.graphics.height).roundToInt()
            when(action) { 0->Gdx.input.inputProcessor.touchDown(px,py,0,0)
                1->Gdx.input.inputProcessor.touchDragged(px,py,0)
                else->Gdx.input.inputProcessor.touchUp(px,py,0,0) }
        }
        fun picture(name:String,wide:Boolean) {
            val w=if(wide) 2340 else 1080; val h=if(wide) 1080 else 2340
            val fbo=FrameBuffer(Pixmap.Format.RGBA8888,w,h,false)
            val before=screen.session.summary().replace(Regex("armed=\\d+"),"armed=0")
            try {
                fbo.begin(); screen.resize(w,h); screen.render(0f)
                val pix=Pixmap.createFromFrameBuffer(0,0,w,h); val png=PixmapIO.PNG()
                try { png.setFlipY(true); png.write(FileHandle("verification/pit-$name-${if(wide) "landscape" else "portrait"}.png"),pix) }
                finally { pix.dispose(); png.dispose() }
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
                    check(++frames<160000) { "Pit timeout ${s.summary()}" }
                    if(s.batchNumber!=batch) { batch=s.batchNumber; tick=0; idle=0; pictured=false }
                    if(!pictured) {
                        pictured=true; picture(if(s.metalReceiverOwned) "with-receiver" else "starter",false)
                        picture(if(s.metalReceiverOwned) "with-receiver" else "starter",true)
                    }
                    if(s.phase==BatchPhase.ACTIVE) {
                        if(idle++<120) check(s.batch.count(Route.INTAKE)==27)
                        else {
                            if(tick==0) {
                                val metal=if(s.metalReceiverOwned) s.batch.needles.firstOrNull { it.route==Route.INTAKE && NeedleRecovery.canPick(s.batch,it,it.x,it.y) } else null
                                val item=metal ?: s.batch.items.lastOrNull { it.material==Material.STRAW && it.route==Route.INTAKE }
                                    ?: s.batch.needles.firstOrNull { it.route==Route.INTAKE && NeedleRecovery.canPick(s.batch,it,it.x,it.y) }
                                if(item!=null) {
                                    if(item.material==Material.NEEDLE) { sx=item.x; sy=item.y }
                                    else { val f=item.strands.first(); val a=item.angle+f.angle
                                        sx=item.x+cos(item.angle)*f.x-sin(item.angle)*f.y-sin(a)*f.bend
                                        sy=item.y+sin(item.angle)*f.x+cos(item.angle)*f.y+cos(a)*f.bend }
                                    tx=if(metal!=null) s.config.metalDockX else FactoryConfig.PIT_X
                                    ty=if(metal!=null) FactoryConfig.METAL_Y else FactoryConfig.PIT_Y
                                    input(sx,sy,0)
                                }
                            }
                            if(tick in 1..159) { val t=(tick/120f).coerceAtMost(1f); input(sx+(tx-sx)*t,sy+(ty-sy)*t,1) }
                            if(tick==160) input(tx,ty,2)
                            tick=(tick+1)%180
                        }
                    }
                    if(s.phase==BatchPhase.RESULTS && !runtime.busy) {
                        if(handled!=batch) {
                            handled=batch; val r=w.profile.receipt!!; val credits=w.profile.workshopCredits
                            check(r.processed==24 && r.secured+r.missed==3 && r.discarded==0)
                            check(r.earned==30L+r.secured*w.needlePrice+(if(r.secured==3) w.clearMetalBonus else 0))
                            check(w.settle(s.batch)==r && w.profile.workshopCredits==credits)
                            println("PIT_BATCH $batch receipt=$r wallet=$credits")
                            if(s.metalReceiverOwned) {
                                // A naive carry-to-pit policy can miss covered steel even with a port.
                                // Ownership unlocks recovery; it is not a free magnetic separator.
                                check(r.secured>0); check(WorkshopStore(save).load(42).profile==w.profile)
                                picture("paid-results",false); picture("paid-results",true)
                                println("PIT_SMOKE PASS starter lots + paid receiver full batch / save / scale / settlement once")
                                Gdx.app.exit(); return
                            }
                            if(credits>=w.price(UpgradeId.METAL_RECEIVER)) {
                                screen.openWorkshop(); picture("shop",false); picture("shop",true)
                                check(screen.purchase(UpgradeId.METAL_RECEIVER)==PurchaseResult.BOUGHT)
                            }
                        }
                        if(!runtime.busy) check(screen.nextBatch())
                    }
                } catch(t:Throwable) { failure=t; t.printStackTrace(); Gdx.app.exit() }
            }
        }
        Lwjgl3Application(app,Lwjgl3ApplicationConfiguration().apply {
            setTitle("NeedleFactory pit QA"); setWindowedMode(480,880); setInitialVisible(false)
            setPauseWhenMinimized(false); setPauseWhenLostFocus(false); useVsync(false); setForegroundFPS(0); disableAudio(true)
        })
        failure?.let { throw IllegalStateException("Pit smoke failed",it) }
    }
}

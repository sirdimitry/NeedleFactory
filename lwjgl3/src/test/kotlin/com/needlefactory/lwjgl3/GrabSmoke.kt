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
object GrabSmoke {
    @JvmStatic fun main(args:Array<String>) {
        val save=File("verification/grab-smoke-${System.currentTimeMillis()}/profile.json")
        val game=NeedleFactoryGame(BuildInfo("clump-grab","debug",true),seed=42,saveFile=save)
        lateinit var screen:FactoryScreen
        var failure:Throwable?=null; var frames=0; var batch=0; var tick=0; var idle=0
        var pictured=false; var early=false; var handled=0; var fullBundled=false
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
                try { png.setFlipY(true); png.write(FileHandle("verification/grab-$name-${if(portrait) "portrait" else "landscape"}.png"),pixels) }
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
                    check(++frames<100000) { "Grab timeout ${s.summary()}" }
                    if(batch!=s.batchNumber) { batch=s.batchNumber; idle=0; tick=0 }
                    if(!pictured) { pictured=true; picture("starter",true); picture("starter",false) }
                    if(s.phase==BatchPhase.ACTIVE) {
                        if(idle++<120) check(s.batch.count(Route.INTAKE)==27)
                        else if(fullBundled && w.eligible(s.batch)) {
                            early=true; check(screen.requestFinish()); check(screen.confirmFinish())
                        } else {
                                if(tick==0) {
                                    val metal=s.batch.needles.firstOrNull { it.route==Route.INTAKE && NeedleRecovery.canPick(s.batch,it,it.x,it.y) }
                                    val item=metal ?: s.batch.items.lastOrNull { it.material==Material.STRAW && it.route==Route.INTAKE }
                                    if(item!=null) {
                                        if(metal!=null) {
                                            startX=metal.x; startY=metal.y; endX=s.config.metalDockX; endY=-.85f
                                        } else {
                                            val f=item.strands.first(); val a=item.angle+f.angle
                                            startX=item.x+cos(item.angle)*f.x-sin(item.angle)*f.y-sin(a)*f.bend
                                            startY=item.y+sin(item.angle)*f.x+cos(item.angle)*f.y+cos(a)*f.bend
                                            endX=10f; endY=4f
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
                            check(r.material==(r.loose*6L+r.bundled*16L)/24)
                            val credits=w.profile.workshopCredits
                            check(w.settle(s.batch)==r && w.profile.workshopCredits==credits)
                            println("GRAB_BATCH batch=$batch seconds=${s.batch.elapsed} receipt=$r wallet=$credits")
                            if(!early) { check(r.secured+r.missed==3 && r.processed==24); check(r.earned==(if(s.baler) 16L else 6L)+3L*r.secured) }
                            if(s.baler && !early) { fullBundled=true; picture("bundled",true); picture("bundled",false) }
                            if(UpgradeId.RAKE.key !in w.profile.purchased && credits>=28) check(screen.purchase(UpgradeId.RAKE)==PurchaseResult.BOUGHT)
                            else if(!s.baler && UpgradeId.RAKE.key in w.profile.purchased && credits>=42) check(screen.purchase(UpgradeId.BALER)==PurchaseResult.BOUGHT)
                            if(early) {
                                check(r.discarded>0); check(WorkshopStore(save).load(42).profile==w.profile)
                                picture("early",true)
                                println("GRAB_SMOKE PASS batches=$batch frames=$frames purchased=${w.profile.purchased}")
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

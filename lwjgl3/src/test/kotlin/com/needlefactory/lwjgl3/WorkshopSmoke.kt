package com.needlefactory.lwjgl3

import com.badlogic.gdx.*
import com.badlogic.gdx.backends.lwjgl3.*
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.files.FileHandle
import com.needlefactory.game.*
import com.needlefactory.presentation.*
import com.needlefactory.economy.*
import com.needlefactory.simulation.*
import java.io.File

/** Actual GL/input/Box2D and durable IO, with no feed/terminal/reward shortcuts. */
object WorkshopSmoke {
    @JvmStatic fun main(args: Array<String>) {
        val dir=File("verification/workshop-smoke-${System.currentTimeMillis()}").apply { mkdirs() }
        val save=File(dir,"profile.json")
        val game=NeedleFactoryGame(BuildInfo("stage3-slice","debug",true),seed=42,saveFile=save,productProgression=false)
        lateinit var screen: FactoryScreen
        var failed: Throwable?=null; var frames=0; var active=0; var tick=0; var pass=0
        var initialCaptured=false; var earlyRequested=false; var idle=0; var handled=0
        var strokeY=.65f
        fun capture(name: String,portrait: Boolean) {
            val w=if(portrait) 1080 else 2340; val h=if(portrait) 2340 else 1080
            val state=screen.session.summary()
            val target=FrameBuffer(Pixmap.Format.RGBA8888,w,h,false)
            try {
                target.begin(); screen.resize(w,h); screen.setSafeArea(SafeArea(top=96,bottom=45))
                screen.render(0f)
                val image=Pixmap.createFromFrameBuffer(0,0,w,h)
                val png=PixmapIO.PNG()
                try { png.setFlipY(true); png.write(FileHandle("verification/stage3-$name-${if(portrait) "portrait" else "landscape"}.png"),image) }
                finally { png.dispose(); image.dispose() }
            } finally { target.end(); target.dispose(); screen.setSafeArea(SafeArea()); screen.resize(Gdx.graphics.width,Gdx.graphics.height) }
            check(screen.session.summary()==state) { "Capture changed gameplay" }
        }
        fun swipe() {
            val i=tick%120; val table=screen.layout.table
            // Initial broad passes, then deliberate cleanup at remaining material's row.
            // This still goes through real input/Box2D, with no feed or route shortcut.
            if(i==0) {
                val remaining=screen.session.batch.items.firstOrNull { it.route==Route.INTAKE }
                strokeY=if(active==4 && pass<14) 1.3f+(pass%2)*.9f
                    else if(pass>=28 && remaining!=null) remaining.y else .65f+(pass%7)*1.1f
            }
            val y=strokeY
            // Deliberately push to the physical mouth, not stop short and rely on
            // the removed post-release gate queue to finish the stroke.
            val x=.65f+9.35f*minOf(i,99)/99f
            val sx=((table.x+x/10f*table.width)/screen.layout.width*Gdx.graphics.width).toInt()
            val sy=((1-(table.y+y/8f*table.height)/screen.layout.height)*Gdx.graphics.height).toInt()
            if(i==0) check(Gdx.input.inputProcessor.touchDown(sx,sy,0,0)) { "Input rejected ${screen.session.summary()} view=${screen.workshopView}" }
            if(i in 1..99) Gdx.input.inputProcessor.touchDragged(sx,sy,0)
            if(i==100) { Gdx.input.inputProcessor.touchUp(sx,sy,0,0); pass++ }
            tick++
        }
        val app=object: ApplicationAdapter() {
            override fun create() { game.create(); screen=game.screen as FactoryScreen }
            override fun resize(w:Int,h:Int)=game.resize(w,h)
            override fun pause()=game.pause()
            override fun resume()=game.resume()
            override fun dispose() { game.workshopRuntime?.persistence?.close(); game.dispose() }
            override fun render() {
                try {
                    screen.render(1f/60f)
                    val runtime=game.workshopRuntime!!
                    if(!screen.isReady || !runtime.ready) return
                    check(++frames<100000) { "Timeout ${screen.session.summary()}" }
                    val s=screen.session; val w=runtime.workshop
                    if(active!=s.batchNumber) {
                        active=s.batchNumber; tick=0; pass=0; idle=0
                        if(active==3) s.toggle(MachineId.SEPARATOR)
                        if(active==4) s.toggle(MachineId.SEPARATOR)
                    }
                    if(!initialCaptured) {
                        initialCaptured=true; capture("start",true); capture("start",false)
                    }
                    if(idle++<180 && s.phase==BatchPhase.ACTIVE) {
                        check(s.batch.count(Route.INTAKE)==323); check(s.batch.touches==0)
                    } else if(s.phase==BatchPhase.ACTIVE) {
                        // Match the bounded 100-stroke native oracle. Once intake is empty,
                        // allow committed belt/packer work to drain within the global timeout.
                        check(pass<100 || s.batch.count(Route.INTAKE)==0) { "Undrained ${s.summary()} poses=${s.batch.items.filter { it.route==Route.INTAKE }.map { listOf(it.id,it.x,it.y,it.angle) }}" }
                        if(active==4 && !earlyRequested && w.eligible(s.batch)) {
                            earlyRequested=true; check(screen.requestFinish()); capture("early",true)
                            check(screen.confirmFinish())
                        } else swipe()
                    }
                    if(frames%600==0) {
                        val summary=s.summary(); game.pause(); game.resume()
                        // Pause cancels the physical finger contact, never leaving stale feed
                        // permission behind. All batch/routes/machines/counters must stay.
                        game.resize(Gdx.graphics.width,Gdx.graphics.height)
                        check(summary.replace(Regex("armed=\\d+"),"armed=0")==s.summary())
                    }
                    check(s.batch.count(Route.PACKING)+s.batch.count(Route.EJECT)<=1)
                    if(s.phase==BatchPhase.RESULTS && !runtime.busy) {
                        if(handled!=active) {
                        handled=active
                        val receipt=w.profile.receipt!!
                        println("WORKSHOP_BATCH id=$active receipt=$receipt balance=${w.profile.workshopCredits}")
                        if(active==1) {
                            check(receipt.earned==90L); capture("results",true); capture("results",false)
                            screen.openWorkshop(); capture("shop",true); capture("shop",false)
                            check(screen.purchase(UpgradeId.RAKE)==PurchaseResult.BOUGHT)
                        } else if(active==2) {
                            check(receipt.earned==90L); check(screen.purchase(UpgradeId.GUIDES)==PurchaseResult.BOUGHT)
                        } else if(active==3) { check(receipt.earned==30L); check(receipt.missed==3) }
                        else if(active==4) {
                            check(earlyRequested); check(receipt.earned>=15 && receipt.discarded>0 && receipt.secured in 1..3 && receipt.bonus==0L)
                            capture("early-results",false)
                        }
                        else if(active==5) {
                            check(receipt.earned==90L); check(s.rake && s.guides)
                            val loaded=com.needlefactory.save.WorkshopStore(save).load(42).profile
                            check(loaded==w.profile)
                            println("WORKSHOP_SMOKE PASS batches=5 frames=$frames wallet=${loaded.workshopCredits} upgrades=${loaded.purchased} save=$save")
                            Gdx.app.exit(); return
                        }
                        }
                        if(!runtime.busy) check(screen.nextBatch())
                    }
                } catch(t: Throwable) { failed=t; t.printStackTrace(); Gdx.app.exit() }
            }
        }
        Lwjgl3Application(app,Lwjgl3ApplicationConfiguration().apply {
            setTitle("NeedleFactory Stage 3 verification"); setWindowedMode(480,880)
            setInitialVisible(false); setPauseWhenMinimized(false); setPauseWhenLostFocus(false)
            useVsync(false); setForegroundFPS(0); disableAudio(true)
        })
        failed?.let { throw IllegalStateException("Workshop smoke failed",it) }
    }
}

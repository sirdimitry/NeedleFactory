package com.needlefactory.lwjgl3

import com.badlogic.gdx.*
import com.badlogic.gdx.backends.lwjgl3.*
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Vector2
import com.needlefactory.content.FactoryConfig
import com.needlefactory.economy.*
import com.needlefactory.game.*
import com.needlefactory.presentation.*
import com.needlefactory.save.*
import com.needlefactory.simulation.*
import java.io.File
import kotlin.math.roundToInt

/** Private funded fixture: native UI transaction/GL verification, not natural earning evidence. */
object EquipmentSmoke {
    @JvmStatic fun main(args:Array<String>) {
        val save=File("verification/equipment-gl-profile.json")
        WorkshopStore(save).write(WorkshopProfile(activeSeed=42,workshopCredits=5000))
        val game=NeedleFactoryGame(BuildInfo("equipment","debug",true),seed=42,saveFile=save,
            factorySession=FactorySession(FactoryConfig.pitProducts(),42))
        lateinit var screen:FactoryScreen;var error:Throwable?=null;var stage=0;var frames=0;var cheatStep=0
        fun input(x:Float,y:Float,kind:Int) {
            val l=screen.layout;val px=(x/l.width*Gdx.graphics.width).roundToInt();val py=((1-y/l.height)*Gdx.graphics.height).roundToInt()
            when(kind) {0->Gdx.input.inputProcessor.touchDown(px,py,0,0);1->Gdx.input.inputProcessor.touchDragged(px,py,0);else->Gdx.input.inputProcessor.touchUp(px,py,0,0)}
        }
        fun tap(x:Float,y:Float) {input(x,y,0);input(x,y,2)}
        fun confirm(yes:Boolean) {val l=screen.layout;val w=minOf(350f,l.width-24);val bw=(w-44)/2
            tap((l.width-w)/2+if(yes) w-16-bw/2 else 16+bw/2,(l.height-332)/2+38)}
        fun picture(name:String,wide:Boolean=false) {
            val w=if(wide) 2340 else 1080;val h=if(wide) 1080 else 2340
            val fbo=FrameBuffer(Pixmap.Format.RGBA8888,w,h,false)
            try {fbo.begin();screen.resize(w,h);screen.render(0f)
                val pix=Pixmap.createFromFrameBuffer(0,0,w,h);val png=PixmapIO.PNG()
                try {png.setFlipY(true);png.write(FileHandle("verification/equipment-$name-${if(wide) "landscape" else "portrait"}.png"),pix)}
                finally {pix.dispose();png.dispose()}
            } finally {fbo.end();fbo.dispose();screen.resize(Gdx.graphics.width,Gdx.graphics.height)}
        }
        val app=object:ApplicationAdapter() {
            override fun create() {game.create();screen=game.screen as FactoryScreen}
            override fun resize(w:Int,h:Int)=game.resize(w,h)
            override fun dispose() {game.workshopRuntime?.persistence?.close();game.dispose()}
            override fun render() {try {
                screen.render(0f);val r=game.workshopRuntime!!;if(!screen.isReady || !r.ready || r.busy) return
                check(frames++<1000);val w=r.workshop;val l=screen.layout
                when(stage++) {
                    0->{screen.openWorkshop()}
                    1->{picture("shop");picture("shop",true)}
                    2->{tap(l.width/2,l.height-183)}
                    3->{check(screen.workshopView==WorkshopView.PURCHASE_CONFIRM);picture("confirmation");confirm(false)}
                    4->{check(w.profile.workshopCredits==5000L && w.profile.purchased.isEmpty());tap(l.width/2,l.height-183)}
                    5->{check(screen.workshopView==WorkshopView.PURCHASE_CONFIRM);confirm(true)}
                    6->{check(w.profile.workshopCredits==4950L && "feed-rake" in w.profile.purchased)
                        check(w.buyEquipment(EquipmentKind.ROBOT)==PurchaseResult.BOUGHT)
                        check(w.buyEquipment(EquipmentKind.FAN)==PurchaseResult.BOUGHT)
                        check(w.buyEquipment(EquipmentKind.DIVIDER)==PurchaseResult.BOUGHT)
                        w.moveEquipment(2,7f,-7f,270);w.moveEquipment(3,6f,-6f,90);r.applyTools(screen.session);r.save()
                    }
                    7->{tap((l.width-minOf(370f,l.width-24))/2+86,47f)}
                    8->{check(screen.workshopView==WorkshopView.NONE);picture("field");picture("field",true)
                        val p=l.yardPoint(6f,-6f,Vector2());tap(p.x,p.y)}
                    9->{val p=l.yardPoint(6f,-5.6f,Vector2());input(p.x,p.y,0);Thread.sleep(350)}
                    10->{val p=l.yardPoint(7f,-6f,Vector2());input(p.x,p.y,1)
                        check(w.profile.equipment.first {it.id==3}.degrees.let {it<=3 || it>=356});picture("rotation");input(p.x,p.y,2)}
                    11->{check(WorkshopStore(save).load(42).profile==w.profile)
                        val p=l.yardPoint(7f,-7f,Vector2());tap(p.x,p.y);tap(p.x,p.y)}
                    12->{check(w.profile.equipment.first {it.id==2}.anchored)
                        val p=l.yardPoint(7f,-7f,Vector2());input(p.x,p.y,0)
                        val q=l.yardPoint(9f,-4f,Vector2());input(q.x,q.y,1);input(q.x,q.y,2)
                        check(w.profile.equipment.first {it.id==2}.x==7f);picture("anchored")}
                    13->{val p=l.yardPoint(7f,-7f,Vector2());tap(p.x,p.y);tap(p.x,p.y)}
                    14->{check(!w.profile.equipment.first {it.id==2}.anchored)
                        check(w.upgrade("receiver-width")==PurchaseResult.BOUGHT);r.applyTools(screen.session);r.save()
                        picture("wide-auger");picture("wide-auger",true)
                        check(kotlin.math.abs(screen.layout.pitHole.width-2.64f*24.8f)<.02f)
                        }
                    15->{tap(l.telemetry.x+14,l.telemetry.y+14)}
                    16->{check(screen.workshopView==WorkshopView.SETTINGS)
                        val w=l.width-24;val h=minOf(410f,l.height-24)
                        tap(12+16+(w-48)/6,(l.height+h)/2-164)}
                    17->{
                        val expected=when(cheatStep) {0->4235L;1->5235L;else->15235L}
                        check(w.profile.workshopCredits==expected)
                        if(cheatStep<2) {
                            val panelWidth=l.width-24;val panelHeight=minOf(410f,l.height-24)
                            val bw=(panelWidth-48)/3
                            tap(28+bw/2+(cheatStep+1)*(bw+8),(l.height+panelHeight)/2-164)
                            cheatStep++;stage=17;return
                        }
                        picture("cheat");picture("cheat",true)
                        check(WorkshopStore(save).load(42).profile==w.profile)
                        val ww=minOf(350f,l.width-24);val bw=(ww-44)/2
                        tap((l.width-ww)/2+16+bw/2,(l.height-332)/2+38)}
                    18->{
                        // Explicit visual fixture: expose all three real needles, no route or reward edits.
                        for(item in screen.session.batch.items) if(item.material==Material.STRAW) {item.x=3f;item.y=5f}
                        for((i,n) in screen.session.batch.needles.withIndex()) {n.x=4f+i*2;n.y=0f;n.angle=.3f+i*.4f}
                        screen.session.synchronizeSnapshots();picture("fully-exposed-steel");picture("fully-exposed-steel",true)
                        check(screen.purchase(UpgradeId.METAL_RECEIVER)==PurchaseResult.BOUGHT)}
                    19->{picture("right-collector");picture("right-collector",true);screen.openWorkshop()}
                    20->{tap(l.width/2,l.height-97-24)}
                    21->{tap(l.width/2,l.height-97-168)}
                    22->{val x=l.width/2;input(x,160f,0);input(x,410f,1);input(x,410f,2)}
                    23->{check(screen.workshopView==WorkshopView.SHOP);picture("per-instance-upgrades");picture("per-instance-upgrades",true)
                        check(WorkshopStore(save).load(42).profile==w.profile)
                        val ww=minOf(370f,l.width-24);tap((l.width-ww)/2+86,47f)}
                    24->{val n=screen.session.batch.needles.first {it.route==Route.INTAKE}
                        // Isolated admission event visual oracle, not a touch/boundary claim.
                        screen.session.manualPush(n.id);check(screen.session.feed(n.id));screen.render(0f)}
                    25->{screen.render(.22f);picture("needle-loss-rim");picture("needle-loss-rim",true)
                        repeat(103) {check(w.addPlaytestCredits(10000))}
                        tap(l.telemetry.x+14,l.telemetry.y+14)}
                    26->{check(screen.workshopView==WorkshopView.SETTINGS)
                        val ww=l.width-24;val hh=minOf(410f,l.height-24)
                        tap(28+(ww-48)/6,(l.height+hh)/2-164)}
                    27->{check(w.profile.workshopCredits>1_000_000L)
                        val ww=l.width-24;val hh=minOf(410f,l.height-24)
                        tap(28+minOf((ww-44)/2,168f)/2,(l.height-hh)/2+38)
                        check(screen.workshopView==WorkshopView.NONE)
                        picture("million-wallet");picture("million-wallet",true)
                        println("EQUIPMENT_GL PASS Yes/No, fan double tap feet/lock/unlock, width both layouts, cheats +100 +1000 +10000, save, exposure and collector art, grouped upgrades, loss frame");Gdx.app.exit()}
                }
            } catch(t:Throwable) {error=t;t.printStackTrace();Gdx.app.exit()} }
        }
        Lwjgl3Application(app,Lwjgl3ApplicationConfiguration().apply {setTitle("Equipment QA");setWindowedMode(480,980);setInitialVisible(false);setForegroundFPS(120)})
        error?.let {throw it}
    }
}

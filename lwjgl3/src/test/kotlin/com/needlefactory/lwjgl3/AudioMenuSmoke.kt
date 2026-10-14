package com.needlefactory.lwjgl3

import com.badlogic.gdx.*
import com.badlogic.gdx.backends.lwjgl3.*
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.needlefactory.content.FactoryConfig
import com.needlefactory.economy.WorkshopProfile
import com.needlefactory.game.*
import com.needlefactory.presentation.*
import com.needlefactory.save.WorkshopStore
import com.needlefactory.simulation.FactorySession
import java.io.File
import kotlin.math.roundToInt

/** Real GL/modal touch oracle, private progression fixture and restored audio preferences. */
object AudioMenuSmoke {
    @JvmStatic fun main(args:Array<String>) {
        val save=File("verification/audio-menu-gl-profile.json")
        WorkshopStore(save).write(WorkshopProfile(activeSeed=42,workshopCredits=1500,purchased=setOf("feed-rake","steel-rake")))
        val game=NeedleFactoryGame(BuildInfo("audio","debug",true),seed=42,saveFile=save,
            factorySession=FactorySession(FactoryConfig.pitProducts(),42))
        lateinit var s:FactoryScreen;lateinit var prefs:Preferences
        var prior:Map<String,*> = emptyMap<String,Any>();var error:Throwable?=null;var stage=0;var frames=0
        fun input(x:Float,y:Float,kind:Int) {
            val px=(x/s.layout.width*Gdx.graphics.width).roundToInt()
            val py=((1-y/s.layout.height)*Gdx.graphics.height).roundToInt()
            when(kind) {0->Gdx.input.inputProcessor.touchDown(px,py,0,0);1->Gdx.input.inputProcessor.touchDragged(px,py,0);else->Gdx.input.inputProcessor.touchUp(px,py,0,0)}
        }
        fun tap(x:Float,y:Float) {input(x,y,0);input(x,y,2)}
        fun geometry()=AudioSettingsLayout().apply {layout(s.layout.width,s.layout.height,s.layout.landscape)}
        fun picture(wide:Boolean,name:String="audio-menu-gl") {
            val w=if(wide) 2340 else 1080;val h=if(wide) 1080 else 2340
            val fbo=FrameBuffer(Pixmap.Format.RGBA8888,w,h,false)
            try {fbo.begin();s.resize(w,h);s.render(0f)
                val pix=Pixmap.createFromFrameBuffer(0,0,w,h);val png=PixmapIO.PNG()
                try {png.setFlipY(true);png.write(FileHandle("verification/$name-${if(wide) "landscape" else "portrait"}.png"),pix)}
                finally {pix.dispose();png.dispose()}
            } finally {fbo.end();fbo.dispose();s.resize(Gdx.graphics.width,Gdx.graphics.height)}
        }
        val app=object:ApplicationAdapter() {
            override fun create() {prefs=Gdx.app.getPreferences("needlefactory-audio-v1");prior=prefs.get().toMap();game.create();s=game.screen as FactoryScreen}
            override fun resize(w:Int,h:Int)=game.resize(w,h)
            override fun dispose() {game.workshopRuntime?.persistence?.close();game.dispose();prefs.clear();prefs.put(prior);prefs.flush()}
            override fun render() {try {
                s.render(.008f);check(frames++<1000)
                if(!s.isReady || game.workshopRuntime?.ready!=true || game.workshopRuntime?.busy==true || frames%12!=0) return
                val l=s.layout
                when(stage++) {
                    0->{
                        tap(l.controls[2].x+l.controls[2].width/2,l.controls[2].y+l.controls[2].height/2)
                        check(s.workshopView==WorkshopView.SHOP)
                        picture(false,"tools-shop");picture(true,"tools-shop");s.render(0f)
                        val sw=minOf(370f,l.width-24);val sx=(l.width-sw)/2+26+(sw-52)/4-3
                        tap(sx,l.height-97-48-138+35.5f)
                        check(s.workshopView==WorkshopView.PURCHASE_CONFIRM)
                        val cw=minOf(350f,l.width-24);val bx=(l.width-cw)/2
                        tap(bx+16+(cw-44)/4,(l.height-minOf(332f,l.height-24))/2+38)
                        check(s.workshopView==WorkshopView.SHOP);s.render(0f)
                        tap(sx,l.height-97-48-276+35.5f)
                        check(s.workshopView==WorkshopView.PURCHASE_CONFIRM)
                        tap(bx+16+(cw-44)/4,(l.height-minOf(332f,l.height-24))/2+38)
                        check(s.workshopView==WorkshopView.SHOP);s.render(0f)
                        tap((l.width-sw)/2+86,47f);check(s.workshopView==WorkshopView.NONE)
                        check(game.workshopRuntime!!.workshop.profile.levels.isEmpty())
                        picture(false,"branding-hud");picture(true,"branding-hud");tap(l.telemetry.x+l.telemetry.width/2,l.telemetry.y+l.telemetry.height/2)
                    }
                    1->{check(s.workshopView==WorkshopView.SETTINGS);val h=minOf(410f,l.height-24)
                        tap(l.width/2,(l.height-h)/2+116);check(s.workshopView==WorkshopView.ABOUT)
                        picture(false,"branding-about");picture(true,"branding-about")
                        val ah=minOf(332f,l.height-24);tap(l.width/2,(l.height-ah)/2+38)
                        check(s.workshopView==WorkshopView.SETTINGS)
                        tap(l.width/2,(l.height-h)/2+82)}
                    2->{check(s.workshopView==WorkshopView.SOUND);val g=geometry()
                        for(i in g.tracks.indices) {val r=g.tracks[i];tap(r.x+r.width*(20+i*5)/100,r.y+r.height/2)}
                        check(prefs.getInteger("volume.AUGER")==35);check(prefs.getInteger("volume.FAN")==40)
                        picture(false);Gdx.graphics.setWindowedMode(980,480)}
                    3->{check(s.layout.landscape && s.workshopView==WorkshopView.SOUND);val r=geometry().tracks[4]
                        input(r.x+r.width*.4f,r.y+r.height/2,0);input(r.x,r.y+r.height/2,1);input(r.x,r.y+r.height/2,2)
                        check(prefs.getInteger("volume.FAN")==0);check(prefs.getInteger("volume.AUGER")==35)
                        picture(true);val b=geometry().back;tap(b.x+b.width/2,b.y+b.height/2)}
                    4->{check(s.workshopView==WorkshopView.SETTINGS);val h=minOf(410f,l.height-24);val w=l.width-24
                        tap(28+minOf((w-44)/2,168f)/2,(l.height-h)/2+38)}
                    5->{check(s.workshopView==WorkshopView.NONE);check(game.workshopRuntime!!.workshop.profile.workshopCredits==1500L)
                        println("AUDIO_MENU_GL PASS entry, ten sliders, drag, independent mute, Back both layouts, wallet unchanged");Gdx.app.exit()}
                }
            } catch(t:Throwable) {error=t;t.printStackTrace();Gdx.app.exit()} }
        }
        Lwjgl3Application(app,Lwjgl3ApplicationConfiguration().apply {setTitle("Audio menu QA");setWindowedMode(480,980);setInitialVisible(false);setForegroundFPS(120)})
        error?.let {throw it}
    }
}

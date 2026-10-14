package com.needlefactory.lwjgl3

import com.badlogic.gdx.*
import com.badlogic.gdx.backends.lwjgl3.*
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.needlefactory.game.*
import com.needlefactory.presentation.FactoryScreen
import com.needlefactory.presentation.FactoryCanvas
import com.needlefactory.simulation.*
import kotlin.math.*

/** Real GL and Box2D; ten batches through the real gate, no synthetic feed shortcut. */
object FactorySmoke {
    @JvmStatic fun main(args:Array<String>) {
        val pictures=mutableListOf<Pair<String,Pixmap>>()
        var haptics=0; var failure:Throwable?=null
        val game=NeedleFactoryGame(BuildInfo("0.0.1","debug",true),PlatformFeedback { haptics++ },seed=42,
            factorySession=FactorySession(firstSeed=42)) // Retained Stage 2 regression harness.
        lateinit var screen:FactoryScreen
        var frames=0; var active=0; var wait=0; var batchFrames=0; var gestureTick=0; var pass=0
        var partialNeedleCaptured=false
        var eligibleNeedleCaptured=false
        var referenceMaterialPixels=0f
        fun intakeCover(): Float {
            val n=screen.session.batch.needle
            var covered=0
            for(k in 0..30) {
                val along=(k/30f-.5f)*n.length
                val px=n.x+cos(n.angle)*along; val py=n.y+sin(n.angle)*along
                val hidden=screen.session.batch.items.any { item ->
                    if(item.material!=Material.STRAW || item.route!=Route.INTAKE) false else {
                        val dx=px-item.x; val dy=py-item.y
                        val lx=cos(item.angle)*dx+sin(item.angle)*dy
                        val ly= -sin(item.angle)*dx+cos(item.angle)*dy
                        item.strands.any { f ->
                            val x=f.cosine*(lx-f.x)+f.sine*(ly-f.y)
                            val y= -f.sine*(lx-f.x)+f.cosine*(ly-f.y)
                            val t=x/f.length; val slope= -8*f.bend*t/f.length
                            abs(t)<=.5f && abs(y-f.bend*(1-4*t*t))/sqrt(1+slope*slope)<=f.width/2
                        }
                    }
                }
                if(hidden) covered++
            }
            return covered/31f
        }
        fun capture(name:String) {
            check(pictures.none { it.first==name }) { "Duplicate visual capture: $name" }
            // OS may constrain a desktop window's height. Use a real GL target at phone
            // resolution, rather than silently reviewing a square/clipped "portrait".
            val portrait=name.contains("portrait")
            val w=if(portrait) 1080 else 2340; val h=if(portrait) 2340 else 1080
            val before=screen.session.summary()
            val target=FrameBuffer(Pixmap.Format.RGBA8888,w,h,false)
            try {
                target.begin(); game.resize(w,h)
                val safe=if(portrait) SafeArea(top=96,bottom=45) else SafeArea(left=96,right=45)
                screen.setSafeArea(safe)
                screen.render(0f)
                if(name.startsWith("stage2-needle-")) {
                    val canvas=FactoryCanvas.forWindow(w,h,safe)
                    val pixelsPerMetre=screen.layout.table.width/screen.session.config.width/canvas.unitsPerPixel
                    if(referenceMaterialPixels==0f) referenceMaterialPixels=pixelsPerMetre
                    check(abs(pixelsPerMetre-referenceMaterialPixels)<.001f) { "Orientation shrank gameplay field" }
                    val table=screen.layout.table
                    println("MODULE_SCALE capture=$name fieldPx=${table.width/canvas.unitsPerPixel} " +
                        "heightPx=${table.height/canvas.unitsPerPixel} needlePx=${screen.session.batch.needle.length*pixelsPerMetre} " +
                        "needleWidthPx=${max(.4f,screen.session.batch.needle.width*table.width/screen.session.config.width)/canvas.unitsPerPixel+1.25f} " +
                        "strawWidthPx=${.025f*pixelsPerMetre} machineUnitPx=${screen.layout.unit/canvas.unitsPerPixel} " +
                        "leftPx=${table.x/canvas.unitsPerPixel+safe.left} topPx=${h-(table.y+table.height)/canvas.unitsPerPixel-safe.bottom}")
                    val visible=screen.needleVisibleFraction
                    val gain=com.needlefactory.presentation.NeedleVisibility.highlightGain(visible)
                    if(name.contains("hidden") || name.contains("partial")) check(gain==0f) { "Covered metal highlighted: $visible" }
                    println("NEEDLE_VISUAL capture=$name visible=$visible gain=$gain")
                }
                pictures.add(name to Pixmap.createFromFrameBuffer(0,0,w,h))
            } finally {
                target.end(); target.dispose(); screen.setSafeArea(SafeArea()); game.resize(Gdx.graphics.width,Gdx.graphics.height)
            }
            check(before==screen.session.summary())
        }
        fun captureTiltPair(prefix: String) {
            val before=screen.session.summary()
            for ((label,x,y) in listOf(Triple("left",-.65f,-.55f),Triple("right",.65f,.35f))) {
                screen.needleLighting.submit(x,y)
                repeat(60) { screen.needleLighting.advance(1f/60f) }
                capture("$prefix-portrait-$label.png")
                capture("$prefix-landscape-$label.png")
            }
            screen.needleLighting.submit(0f,0f)
            repeat(60) { screen.needleLighting.advance(1f/60f) }
            check(before==screen.session.summary()) { "Lighting changed gameplay state" }
        }
        fun tap(x:Float,y:Float) {
            val sx=(x/screen.layout.width*Gdx.graphics.width).toInt()
            val sy=((1-y/screen.layout.height)*Gdx.graphics.height).toInt()
            check(Gdx.input.inputProcessor.touchDown(sx,sy,0,0))
            Gdx.input.inputProcessor.touchUp(sx,sy,0,0)
        }
        fun feedGesture() {
            val i=gestureTick%120; val y=.65f+(pass%7)*1.1f
            val t=screen.layout.table
            val x=.65f+8.7f*minOf(i,99)/99f
            val sx=((t.x+x/10f*t.width)/screen.layout.width*Gdx.graphics.width).toInt()
            val sy=((1-(t.y+y/8f*t.height)/screen.layout.height)*Gdx.graphics.height).toInt()
            if(i==0) check(Gdx.input.inputProcessor.touchDown(sx,sy,0,0)) {
                "manual gesture rejected tick=$gestureTick pass=$pass at=$sx,$sy ${screen.session.summary()}"
            }
            if(i in 1..99) Gdx.input.inputProcessor.touchDragged(sx,sy,0)
            if(i==100) { Gdx.input.inputProcessor.touchUp(sx,sy,0,0); pass++ }
            gestureTick++
        }
        val listener=object:ApplicationAdapter() {
            override fun create() { game.create(); screen=game.screen as FactoryScreen }
            override fun resize(w:Int,h:Int) = game.resize(w,h)
            override fun pause()=game.pause()
            override fun resume()=game.resume()
            override fun dispose()=game.dispose()
            override fun render() {
                try {
                    screen.render(1f/60f)
                    if(!screen.isReady) return
                    frames++; check(frames<100000) { "timeout ${screen.session.summary()}" }
                    val s=screen.session
                    if(active!=s.batchNumber) {
                        active=s.batchNumber; wait=0; batchFrames=0; gestureTick=0; pass=0
                        if(active==1) {
                            capture("stage2-portrait-start.png")
                            captureTiltPair("stage2-needle-hidden")
                            s.toggle(MachineId.PACKER)
                        }
                        if(active==3) s.toggle(MachineId.SEPARATOR)
                        if(active==4) s.toggle(MachineId.SEPARATOR)
                    }
                    batchFrames++
                    val idleFrames=if(active==1) 600 else 180
                    if(batchFrames<=idleFrames) {
                        check(s.batch.count(Route.INTAKE)==s.config.strawCount+1 && s.packaged==s.batchNumber-if(s.batchNumber>=4) 2 else 1)
                    } else if(!s.batch.complete) feedGesture()
                    if(active==1 && (!partialNeedleCaptured || !eligibleNeedleCaptured) && gestureTick%120==101 && s.batch.needle.route==Route.INTAKE) {
                        val cover=intakeCover()
                        if(!partialNeedleCaptured && cover in .40f.. .85f) {
                            captureTiltPair("stage2-needle-partial")
                            println("NEEDLE_VISUAL partial seed=${s.batch.seed} gestures=$pass coveredFraction=$cover")
                            partialNeedleCaptured=true
                        }
                        if(!eligibleNeedleCaptured && cover<.25f) {
                            captureTiltPair("stage2-needle-eligible")
                            eligibleNeedleCaptured=true
                        }
                    }
                    if(frames==100) {
                        val before=s.summary(); game.pause(); screen.render(100f); game.resume()
                        check(before==s.summary())
                        Gdx.graphics.setWindowedMode(1100,540)
                    }
                    if(frames==65) {
                        val r=screen.layout.run; tap(r.x+r.width/2,r.y+r.height/2)
                        val before=s.summary()
                        repeat(12) { screen.render(1f/120f) }
                        check(before==s.summary() && !s.running)
                        check(s.batch.items.all { it.x==it.previousX && it.progress==it.previousProgress })
                        capture("stage2-portrait-paused.png")
                        tap(r.x+r.width/2,r.y+r.height/2)
                    }
                    if(frames==80) {
                        val r=screen.layout.telemetry; val before=s.summary()
                        tap(r.x+r.width/2,r.y+r.height/2)
                        capture("stage2-portrait-telemetry.png")
                        tap(r.x+r.width/2,r.y+r.height/2)
                        check(before==s.summary())
                    }
                    if(frames==690) capture("stage2-landscape-flow.png")
                    if(frames==180) {
                        val batch=s.batch; val counts=s.packaged; val before=s.summary()
                        game.resize(Gdx.graphics.width,Gdx.graphics.height); check(s.batch===batch && s.packaged==counts && s.summary()==before)
                    }
                    if(frames==200) {
                        val before=s.summary(); val old=screen
                        screen=FactoryScreen(BuildInfo("0.0.1","debug",true),PlatformFeedback { haptics++ },s)
                        game.screen=screen; old.dispose()
                        check(screen.session===s && s.summary()==before)
                    }
                    if(active==4 && s.batch.elapsed>=5f && wait==0) {
                        capture("stage2-portrait-flow.png"); wait=1
                    }
                    if(active==1 && s.batch.needle.route==Route.CAPTURE && s.batch.needle.progress>=.45f && wait==0) {
                        capture("stage2-capture.png")
                        captureTiltPair("stage2-needle-metal")
                        wait=1
                    }
                    if(active==1 && s.batch.needle.route==Route.BUFFER && wait<2) {
                        check(s.packaged==0); capture("stage2-buffer.png")
                        val r=screen.layout.controls[3]; tap(r.x+r.width/2,r.y+r.height/2)
                        wait=2
                    }
                    if(s.batch.complete) {
                        Gdx.input.inputProcessor.touchUp(0,0,0,0)
                        check(s.batch.count(Route.RESIDUE)==s.config.strawCount)
                        check(s.packaged==s.batchNumber-if(s.batchNumber>=3) 1 else 0)
                        if(active==1) capture("stage2-packaged.png")
                        if(active==3) { check(s.missed==1); capture("stage2-missed.png") }
                        if(active==10) {
                            capture("stage2-portrait-packaged.png")
                            check(s.packaged==9 && s.missed==1 && haptics==9)
                            println("FACTORY_SMOKE PASS batches=10 packaged=9 missed=1 haptics=9 frames=$frames partialNeedleCaptured=$partialNeedleCaptured eligibleNeedleCaptured=$eligibleNeedleCaptured")
                            Gdx.app.exit()
                        } else {
                            val r=screen.layout.next; tap(r.x+r.width/2,r.y+r.height/2)
                            if(active==3) Gdx.graphics.setWindowedMode(480,880)
                        }
                    }
                } catch(t:Throwable) { failure=t; Gdx.app.exit() }
            }
        }
        val config=Lwjgl3ApplicationConfiguration().apply {
            setTitle("Stage 2 verification"); setWindowedMode(480,880)
            setInitialVisible(false)
            setPauseWhenMinimized(false); setPauseWhenLostFocus(false)
            useVsync(false); setForegroundFPS(0); disableAudio(true)
        }
        Lwjgl3Application(listener,config)
        for((name,pixmap) in pictures) {
            val file=FileHandle("verification/$name"); file.parent().mkdirs()
            val png=PixmapIO.PNG()
            try { png.setFlipY(true); png.write(file,pixmap) } finally { png.dispose(); pixmap.dispose() }
        }
        failure?.let { throw it }
    }
}

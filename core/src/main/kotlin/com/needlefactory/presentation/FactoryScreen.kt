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
import kotlin.math.*

/** Replaceable procedural diorama. Production truth belongs exclusively to FactorySession. */
class FactoryScreen(private val build: BuildInfo, private val feedback: PlatformFeedback,
                    val session: FactorySession,
                    val needleLighting: NeedleLighting = NeedleLighting()) : ScreenAdapter() {
    private val viewport = FitViewport(400f, 820f)
    var layout = FactoryLayout(400f, 820f); private set
    private val shapes = ShapeRenderer(12000)
    private val sprites = SpriteBatch()
    private val diorama = FactoryDiorama(shapes)
    private val hud = FactoryHud(shapes,sprites)
    private val assets = AssetManager(ClasspathFileHandleResolver()).apply {
        load("ui/needle-panel.fnt", BitmapFont::class.java)
    }
    private var physics = FactoryIntakePhysics(session)
    private val needleVisibility = NeedleVisibility(session.config.strawCount)
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
    private var metrics = "Starting instruments..."
    private var telemetryExpanded = false
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
            if (suspended || !isReady || pointer != -1) return false
            viewport.unproject(touch.set(x.toFloat(), y.toFloat()))
            if(build.diagnostics && layout.telemetry.contains(touch)) {
                telemetryExpanded=!telemetryExpanded; return true
            }
            if (layout.run.contains(touch)) {
                session.toggleRunning(); cancelTouch(); clock.reset(); ignoreDelta=true; refresh(); return true
            }
            if (layout.next.contains(touch) && session.batch.complete) { nextBatch(); return true }
            for (i in layout.controls.indices) if (layout.controls[i].contains(touch)) {
                session.toggle(MachineId.entries[i]); refresh(); return true
            }
            if (!session.running || session.batch.complete || !layout.table.contains(touch)) return false
            pointer = p; val tx = tableX(touch.x); val ty = tableY(touch.y)
            previousTouchX = tx; previousTouchY = ty; session.touch(); physics.begin(tx, ty)
            return true
        }
        override fun touchDragged(x: Int, y: Int, p: Int): Boolean {
            if (p != pointer) return false
            viewport.unproject(touch.set(x.toFloat(), y.toFloat()))
            val tx = tableX(touch.x); val ty = tableY(touch.y)
            session.assist(hypot(tx - previousTouchX, ty - previousTouchY))
            previousTouchX = tx; previousTouchY = ty; physics.target(tx, ty)
            return true
        }
        override fun touchUp(x: Int, y: Int, p: Int, button: Int): Boolean {
            if (p != pointer) return false
            cancelTouch(); return true
        }
        override fun touchCancelled(x: Int, y: Int, p: Int, button: Int) = touchUp(x,y,p,button)
    }
    private fun tableX(x: Float) = ((x - layout.table.x) / layout.table.width * session.config.width).coerceIn(0f,session.config.width)
    private fun tableY(y: Float) = ((y - layout.table.y) / layout.table.height * session.config.height).coerceIn(0f,session.config.height)
    private fun cancelTouch() { physics.end(); pointer = -1 }
    fun nextBatch(): Boolean {
        cancelTouch()
        if (!session.next()) return false
        physics.close(); physics = FactoryIntakePhysics(session)
        clock.reset(); loggedComplete = false; peak = 0f; refresh()
        return true
    }
    override fun show() { Gdx.input.inputProcessor = input; refresh() }
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
        layout = FactoryLayout(canvas.width,canvas.height)
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
        if(isReady && !wasReady) assets.get("ui/needle-panel.fnt",BitmapFont::class.java).region.texture
            .setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear)
        val dt = if (ignoreDelta || !delta.isFinite()) 0f else delta.coerceAtLeast(0f)
        ignoreDelta = !isReady
        needleLighting.advance(dt)
        if (isReady && session.running) clock.advance(dt, step) else clock.reset()
        if (session.packaged != packagedSeen) { packagedSeen = session.packaged; feedback.found(); refresh() }
        if (session.batch.complete && !loggedComplete) {
            loggedComplete = true
            if (build.diagnostics) Gdx.app.log("Factory", "COMPLETE ${session.summary()} bodies=$bodyCount peakMs=${peak*1000}")
            refresh()
        }
        metricsAge += dt; logAge += dt; frameTotal += dt; frames++; peak = max(peak,dt)
        if (metricsAge >= .5f) {
            metrics = "${Gdx.graphics.framesPerSecond} FPS / ${(frameTotal*1000/max(1,frames)).toInt()} ms / $bodyCount bodies"
            frameTotal = 0f; frames = 0; metricsAge = 0f; refresh()
        }
        if (build.diagnostics && logAge >= 5f) {
            Gdx.app.log("Factory", "${session.summary()} running=${session.running} $metrics peakMs=${peak*1000} " +
                "throughput=${(session.batch.items.size-session.batch.count(Route.INTAKE))/max(.01f,session.batch.elapsed)} droppedS=${clock.droppedSeconds}")
            logAge = 0f
        }
        ScreenUtils.clear(bg); viewport.apply()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = viewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        diorama.alpha=clock.alpha
        diorama.draw(layout,session)
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
        val needle = session.batch.needle
        if (needle.route != Route.OUTPUT && needle.route != Route.MISSED) drawItem(needle)
        for (item in session.batch.items) if (item.material == Material.STRAW && !item.route.terminal) drawItem(item)
        diorama.overhead(layout,session)
        // Captured metal sits on the lifted carriage, above the transport gantry.
        if(needle.route==Route.CAPTURE) drawItem(needle)
        if(needle.route==Route.MISSED) needle(layout.missed.x,layout.missed.y,
            needle.length*layout.table.width/session.config.width,max(.4f,needle.width*layout.table.width/session.config.width),0f)
        hud.shapes(layout,session,build.diagnostics,telemetryExpanded)
        shapes.end()
        sprites.projectionMatrix=viewport.camera.combined
        if (isReady) hud.text(layout,session,assets.get("ui/needle-panel.fnt", BitmapFont::class.java),metrics,build.diagnostics,telemetryExpanded)
    }
    private fun drawItem(item: FactoryItem) {
        val angle=locateItem(item)
        if(!angle.isFinite()) return
        val scale=layout.table.width/session.config.width
        if(item.material==Material.NEEDLE) {
            // Lifted capture is rendered above straw. Otherwise evaluate the current screen geometry.
            needleVisibleFraction=if(item.route==Route.CAPTURE) 1f else
                needleVisibility.fraction(pos.x,pos.y,item.length*scale,angle,
                    .25f*viewport.worldWidth/surfaceWidth.coerceAtLeast(1))
            needle(pos.x,pos.y,item.length*scale,max(.4f,item.width*scale),angle,needleVisibleFraction)
        } else tuft(item,pos.x,pos.y,scale,angle)
    }
    private fun locateItem(item: FactoryItem): Float {
        val alpha = clock.alpha
        val t = item.previousProgress+(item.progress-item.previousProgress)*alpha
        val scale = layout.table.width/session.config.width
        var angle = 0f
        val beltAngle = atan2(layout.end.y-layout.start.y, layout.end.x-layout.start.x)
        when(item.route) {
            Route.INTAKE -> {
                pos.set(layout.table.x+(item.previousX+(item.x-item.previousX)*alpha)*scale,
                    layout.table.y+(item.previousY+(item.y-item.previousY)*alpha)*scale)
                angle = item.previousAngle + atan2(sin(item.angle-item.previousAngle),cos(item.angle-item.previousAngle))*alpha
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
    private fun refresh() { hud.refresh(session) }
    override fun pause() { suspended=true; cancelTouch(); clock.reset(); session.synchronizeSnapshots() }
    override fun resume() { suspended=false; ignoreDelta=true; clock.reset(); session.synchronizeSnapshots() }
    override fun hide() { cancelTouch(); if(Gdx.input.inputProcessor===input) Gdx.input.inputProcessor=null }
    override fun dispose() { hide(); physics.close(); shapes.dispose(); sprites.dispose(); assets.dispose() }
}

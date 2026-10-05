package com.needlefactory.presentation

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.needlefactory.simulation.*
import kotlin.math.*

/** Authored mechanical construction. Attachment coordinates also drive all material paths. */
internal class FactoryDiorama(shapes:ShapeRenderer) {
    private val d=FactoryDrawing(shapes)
    private val s=shapes
    private val p=Vector2()
    fun draw(l:FactoryLayout,session:FactorySession) {
        val u=l.unit
        floor(l)
        // Common support frame ties the belt and elevated sorting branch together.
        belt(l,session)
        apron(l,session)
        table(l,session)
        d.channel(l.end.x,l.end.y,l.residue.x,l.residue.y,25*u,u)
        d.channel(l.end.x,l.end.y,l.missed.x,l.missed.y,13*u,u)
        bin(l.residue.x,l.residue.y,53*u,35*u,u,false,session.batch.count(Route.RESIDUE))
        bin(l.missed.x,l.missed.y,47*u,31*u,u,true,0)
        // Continuous metal path: extraction saddle -> lined chute -> buffer -> jaws -> output.
        d.channel(l.magnet.x,l.magnet.y,l.capture.x,l.capture.y,18*u,u)
        d.channel(l.capture.x,l.capture.y,l.buffer.x,l.buffer.y,18*u,u)
        d.channel(l.buffer.x,l.buffer.y,l.packer.x,l.packer.y,19*u,u)
        d.channel(l.packer.x,l.packer.y,l.output.x,l.output.y,24*u,u)
        buffer(l,session)
        packer(l,session)
        bin(l.output.x,l.output.y,53*u,40*u,u,false,0)
        val n=session.batch.needle
        if(n.route==Route.EJECT || n.route==Route.OUTPUT) {
            val t=if(n.route==Route.OUTPUT) 1f else n.previousProgress+(n.progress-n.previousProgress)*alpha
            p.set(l.packer).lerp(l.output,t)
            parcel(p.x,p.y,u)
        }
    }
    /** Raised housings occlude material travelling underneath the gantry. */
    fun overhead(l:FactoryLayout,session:FactorySession) = separator(l,session)
    var alpha=1f
    private fun floor(l:FactoryLayout) {
        val f=l.floor
        d.cabinet(f.x,f.y,f.width,f.height,12f,FactoryPalette.floor)
        d.round(f.x+7,f.y+6,f.width-14,f.height-12,9f,FactoryPalette.floor)
        // Subtle scored service grid, not a flat empty background.
        for(i in 1..(f.width/52).toInt())
            d.line(f.x+i*52,f.y+7,f.x+i*52,f.y+f.height-7,.5f,FactoryPalette.top)
        for(i in 1..(f.height/52).toInt())
            d.line(f.x+8,f.y+i*52,f.x+f.width-8,f.y+i*52,.5f,FactoryPalette.top)
        for(i in 0..1) for(j in 0..1) d.bolt(f.x+10+i*(f.width-20),f.y+10+j*(f.height-20),2.5f)
        // Inset plinth trim and short safety marks at the service edge.
        d.line(f.x+20,f.y+2,f.x+f.width-20,f.y+2,2f,FactoryPalette.brass)
        for(i in 0..5) d.line(f.x+20+i*8,f.y-8,f.x+24+i*8,f.y-3,3f,FactoryPalette.dark)
    }
    private fun apron(l:FactoryLayout,session:FactorySession) {
        val t=l.table; val x=t.x+t.width; val y=l.start.y; val u=l.unit
        val length=hypot(l.end.x-l.start.x,l.end.y-l.start.y)
        val nx=-(l.end.y-l.start.y)/length; val ny=(l.end.x-l.start.x)/length
        val upperX=l.start.x+nx*22*u; val upperY=y+ny*22*u
        val lowerX=l.start.x-nx*22*u; val lowerY=y-ny*22*u
        // The folded receiving pan terminates at the actual drum's two shaft ends.
        // Portrait turns the throat with the belt, instead of leaving diagonal rails in air.
        fun pan(inset:Float,drop:Float) {
            s.triangle(x,t.y+inset-drop,x,t.y+t.height-inset-drop,upperX,upperY-drop)
            s.triangle(x,t.y+inset-drop,upperX,upperY-drop,lowerX,lowerY-drop)
        }
        s.color=FactoryPalette.front
        pan(0f,7*u)
        s.color=FactoryPalette.top
        pan(0f,0f)
        s.color=FactoryPalette.body
        pan(5*u,0f)
        d.line(x,t.y,lowerX,lowerY,3*u,FactoryPalette.steel)
        d.line(x,t.y+t.height,upperX,upperY,3*u,FactoryPalette.steel)
        d.bolt(lowerX,lowerY,2*u); d.bolt(upperX,upperY,2*u)
        // Folded guide vanes on the receiving pan point toward the drive drum.
        for(i in 1..4) {
            val yy=t.y+t.height*i/5
            d.line(x+6*u,yy,l.start.x-8*u,y+(yy-y)*.18f,.8f*u,FactoryPalette.top)
        }
        d.foot(l.start.x-13*u,y-16*u,18*u,u)
        // The gate is visibly across the intake; its switch holds hand-delivered material.
        if(!session.enabled(MachineId.VIBRATION))
            d.line(x+7*u,t.y+5*u,x+7*u,t.y+t.height-5*u,5*u,FactoryPalette.copper)
    }
    private fun table(l:FactoryLayout,session:FactorySession) {
        val t=l.table; val u=l.unit
        for(i in 0..2) {
            val x=t.x+14*u+i*(t.width-28*u)/2
            d.foot(x,t.y-4*u,25*u,u)
            for(j in 0..5) d.line(x+(if(j%2==0) -4 else 4)*u,t.y-20*u+j*2.6f*u,
                x+(if(j%2==0) 4 else -4)*u,t.y-17.4f*u+j*2.6f*u,1.5f*u,FactoryPalette.steel)
        }
        d.cabinet(t.x-9*u,t.y-7*u,t.width+16*u,t.height+15*u,13*u,FactoryPalette.body)
        d.round(t.x-3*u,t.y-3*u,t.width+6*u,t.height+6*u,7*u,FactoryPalette.steel)
        d.round(t.x,t.y,t.width,t.height,5*u,FactoryPalette.dark)
        // Recessed tray ribs reinforce depth and make the thinning pile readable.
        for(i in 1..7) d.line(t.x+4*u,t.y+t.height*i/8,t.x+t.width-3*u,t.y+t.height*i/8,.65f*u,FactoryPalette.front)
        // Right rim is intentionally open along the full receiving apron.
        d.line(t.x-4*u,t.y+4*u,t.x-4*u,t.y+t.height-4*u,2*u,FactoryPalette.top)
        for(i in 0..1) for(j in 0..1) d.bolt(t.x-5*u+i*(t.width+10*u),t.y-3*u+j*(t.height+6*u),1.6f*u)
        // Eccentric drive: round motor housing, mounting ears, spindle and moving weight.
        val mx=t.x+24*u; val my=t.y-17*u
        d.round(mx-18*u,my-9*u,36*u,18*u,5*u,FactoryPalette.front)
        s.color=FactoryPalette.body; s.ellipse(mx-12*u,my-9*u,24*u,18*u,24)
        s.color=FactoryPalette.steel; s.ellipse(mx-8*u,my-6*u,16*u,12*u,20)
        val a=if(session.operating(MachineId.VIBRATION)) session.batch.elapsed*23 else 0f
        s.color=FactoryPalette.brass; s.circle(mx+cos(a)*4*u,my+sin(a)*3*u,3*u,12)
    }
    private fun belt(l:FactoryLayout,session:FactorySession) {
        val u=l.unit; val ax=l.start.x; val ay=l.start.y; val bx=l.end.x; val by=l.end.y
        val length=hypot(bx-ax,by-ay); val nx=-(by-ay)/length; val ny=(bx-ax)/length
        // Joined chassis, return-belt depth and attached bolted support trestles.
        val supports=max(2,(length/(110*u)).toInt())
        for(i in 0..supports) {
            val t=i.toFloat()/supports; val x=ax+(bx-ax)*t; val y=ay+(by-ay)*t
            d.foot(x-nx*18*u,y-ny*18*u-5*u,18*u,u)
            d.foot(x+nx*18*u,y+ny*18*u-5*u,18*u,u)
            d.line(x-nx*22*u,y-ny*22*u-10*u,x+nx*22*u,y+ny*22*u-10*u,4*u,FactoryPalette.front)
        }
        d.line(ax+4*u,ay-9*u,bx+4*u,by-9*u,57*u,FactoryPalette.floorEdge)
        d.line(ax,ay-6*u,bx,by-6*u,54*u,FactoryPalette.front)
        d.line(ax,ay,bx,by,52*u,FactoryPalette.body)
        d.line(ax,ay,bx,by,36*u,FactoryPalette.belt)
        val motion=session.conveyorSeconds*34*u
        for(i in 0..(length/(15*u)).toInt()) {
            val t=((i*15*u+motion)%length)/length
            val x=ax+(bx-ax)*t; val y=ay+(by-ay)*t
            d.line(x-nx*16*u,y-ny*16*u,x+nx*16*u,y+ny*16*u,1.2f*u,FactoryPalette.front)
            // A small chevron communicates direction even without a label.
            if(i%4==0) {
                val tx=(bx-ax)/length; val ty=(by-ay)/length
                d.line(x-nx*4*u-tx*2*u,y-ny*4*u-ty*2*u,x+tx*3*u,y+ty*3*u,.8f*u,FactoryPalette.top)
                d.line(x+nx*4*u-tx*2*u,y+ny*4*u-ty*2*u,x+tx*3*u,y+ty*3*u,.8f*u,FactoryPalette.top)
            }
        }
        for(side in -1..1 step 2) {
            d.line(ax+nx*23*u*side,ay+ny*23*u*side,bx+nx*23*u*side,by+ny*23*u*side,6*u,FactoryPalette.top)
            d.line(ax+nx*23*u*side,ay+ny*23*u*side+1*u,bx+nx*23*u*side,by+ny*23*u*side+1*u,1.2f*u,FactoryPalette.steel)
        }
        drum(ax,ay,nx,ny,motion/(13*u),u); drum(bx,by,nx,ny,motion/(13*u),u)
        // Compact drive gearbox is attached to the discharge shaft.
        d.cabinet(bx+nx*30*u-10*u,by+ny*30*u-8*u,20*u,16*u,5*u,FactoryPalette.body)
        d.bolt(bx+nx*30*u,by+ny*30*u,3*u)
    }
    private fun drum(x:Float,y:Float,nx:Float,ny:Float,a:Float,u:Float) {
        d.line(x-nx*24*u,y-ny*24*u,x+nx*24*u,y+ny*24*u,16*u,FactoryPalette.dark)
        d.line(x-nx*18*u,y-ny*18*u,x+nx*18*u,y+ny*18*u,12*u,FactoryPalette.steel)
        for(i in -2..2) d.line(x+nx*i*6*u-ny*cos(a)*4*u,y+ny*i*6*u+nx*cos(a)*4*u,
            x+nx*i*6*u+ny*cos(a)*4*u,y+ny*i*6*u-nx*cos(a)*4*u,1.5f*u,FactoryPalette.body)
        for(side in -1..1 step 2) {
            val xx=x+nx*25*u*side; val yy=y+ny*25*u*side
            s.color=FactoryPalette.front; s.circle(xx,yy,7*u,20)
            s.color=FactoryPalette.steel; s.circle(xx,yy+1*u,4.5f*u,16)
            d.bolt(xx,yy+1*u,1.5f*u)
        }
    }
    private fun separator(l:FactoryLayout,session:FactorySession) {
        val u=l.unit; val x=l.magnet.x; val y=l.magnet.y
        // The extraction saddle is anchored to both lanes; the magnet never floats.
        val dx=l.capture.x-l.magnet.x; val dy=l.capture.y-l.magnet.y
        val len=hypot(dx,dy); val nx=-dy/len; val ny=dx/len
        for(side in -1..1 step 2) {
            val ox=nx*26*u*side; val oy=ny*26*u*side
            d.foot(l.capture.x+ox,l.capture.y+oy,22*u,u)
            d.line(l.capture.x+ox,l.capture.y+oy-5*u,x+ox,y+oy+14*u,5*u,FactoryPalette.front)
            d.line(l.capture.x+ox,l.capture.y+oy,x+ox,y+oy+17*u,2*u,FactoryPalette.steel)
        }
        // Bolted feet and tall uprights span the live belt, not a detached magnet icon.
        for(side in -1..1 step 2) {
            d.foot(x+side*34*u,y-15*u,23*u,u)
            d.round(x+side*34*u-4*u,y-15*u,8*u,47*u,3*u,FactoryPalette.front)
            d.line(x+side*34*u-2*u,y-9*u,x+side*34*u-2*u,y+29*u,1.5f*u,FactoryPalette.steel)
        }
        d.cabinet(x-37*u,y+14*u,74*u,29*u,11*u,FactoryPalette.body)
        // Soft U yoke with real pole shoes, wound copper core and mounting bolts.
        d.round(x-29*u,y+4*u,13*u,32*u,3*u,FactoryPalette.steel)
        d.round(x+16*u,y+4*u,13*u,32*u,3*u,FactoryPalette.steel)
        d.round(x-26*u,y+28*u,52*u,12*u,4*u,FactoryPalette.top)
        d.round(x-16*u,y+16*u,32*u,20*u,6*u,FactoryPalette.copper)
        for(i in -5..5) d.line(x+i*2.8f*u,y+18*u,x+i*2.8f*u,y+33*u,1*u,FactoryPalette.brass)
        d.bolt(x-29*u,y+37*u,2f*u); d.bolt(x+29*u,y+37*u,2f*u)
        // Pole shoes straddle a visible dark aperture over the actual capture point.
        d.round(x-24*u,y-7*u,10*u,13*u,3*u,FactoryPalette.steel)
        d.round(x+14*u,y-7*u,10*u,13*u,3*u,FactoryPalette.steel)
        val n=session.batch.needle
        val lift=if(n.route==Route.CAPTURE) (n.previousProgress+(n.progress-n.previousProgress)*alpha).coerceIn(0f,1f) else 0f
        p.set(l.magnet).lerp(l.capture,lift*lift*(3-2*lift))
        // Guide carriage travels with capture; rails and discharge saddle stay fixed.
        d.line(p.x-nx*12*u,p.y-ny*12*u,p.x+nx*12*u,p.y+ny*12*u,3*u,FactoryPalette.brass)
        d.bolt(l.capture.x-nx*16*u,l.capture.y-ny*16*u,2f*u)
        d.bolt(l.capture.x+nx*16*u,l.capture.y+ny*16*u,2f*u)
        // Insulated power cable follows the gantry rather than hanging in empty space.
        d.line(x+24*u,y+30*u,x+32*u,y+23*u,2*u,FactoryPalette.dark)
        d.line(x+32*u,y+23*u,l.magnet.x+nx*29*u,l.magnet.y+ny*29*u,2*u,FactoryPalette.dark)
        indicator(x+30*u,y+38*u,session.operating(MachineId.SEPARATOR),u)
    }
    private fun buffer(l:FactoryLayout,session:FactorySession) {
        val x=l.buffer.x; val y=l.buffer.y; val u=l.unit
        d.foot(x-23*u,y-16*u,21*u,u); d.foot(x+23*u,y-16*u,21*u,u)
        d.cabinet(x-32*u,y-20*u,64*u,40*u,11*u,FactoryPalette.body)
        // Folded hopper walls taper to a machined stop pocket, rather than an outlined square.
        s.color=FactoryPalette.steel
        s.triangle(x-29*u,y+17*u,x-18*u,y+8*u,x+29*u,y+17*u)
        s.triangle(x+29*u,y+17*u,x-18*u,y+8*u,x+18*u,y+8*u)
        d.round(x-21*u,y-11*u,42*u,23*u,4*u,FactoryPalette.dark)
        s.color=FactoryPalette.top
        s.triangle(x-29*u,y+17*u,x-21*u,y+8*u,x-21*u,y-10*u)
        s.triangle(x+29*u,y+17*u,x+21*u,y-10*u,x+21*u,y+8*u)
        d.line(x-29*u,y+17*u,x-21*u,y-10*u,1f*u,FactoryPalette.steel)
        d.line(x+29*u,y+17*u,x+21*u,y-10*u,1f*u,FactoryPalette.steel)
        d.line(x-29*u,y-15*u,x-21*u,y-8*u,5*u,FactoryPalette.top)
        d.line(x+29*u,y-15*u,x+21*u,y-8*u,5*u,FactoryPalette.top)
        // Queue stop is a raised finger, with an explicit occupied marker.
        val occupied=session.batch.count(Route.BUFFER)>0
        val gx=x+if(l.landscape) -19*u else 19*u
        d.round(gx-2*u,y-10*u,4*u,20*u,1*u,FactoryPalette.brass)
        // Visible external pneumatic ram holds/releases the stop finger.
        d.round(x+22*u,y-8*u,17*u,8*u,3*u,FactoryPalette.steel)
        d.line(x+14*u,y-4*u,x+24*u,y-4*u,2*u,FactoryPalette.brass)
        d.bolt(x-25*u,y-15*u,1.7f*u); d.bolt(x+25*u,y-15*u,1.7f*u)
        indicator(x-24*u,y+15*u,occupied,u)
    }
    private fun packer(l:FactoryLayout,session:FactorySession) {
        val x=l.packer.x; val y=l.packer.y; val u=l.unit
        d.foot(x-37*u,y-29*u,20*u,u); d.foot(x+37*u,y-29*u,20*u,u)
        d.cabinet(x-53*u,y-34*u,106*u,68*u,15*u,FactoryPalette.body)
        d.round(x-34*u,y-21*u,68*u,43*u,7*u,FactoryPalette.steel)
        d.round(x-29*u,y-17*u,58*u,33*u,5*u,FactoryPalette.dark)
        // Machined open ports connect the chassis to the actual transfer channels.
        if(l.landscape) {
            val side=if(l.buffer.x>x) 1 else -1
            d.round(x+if(side==1) 28*u else -54*u,y-12*u,26*u,24*u,3*u,FactoryPalette.dark)
            d.line(x+side*30*u,y-13*u,x+side*54*u,y-13*u,2*u,FactoryPalette.steel)
            d.line(x+side*30*u,y+13*u,x+side*54*u,y+13*u,2*u,FactoryPalette.steel)
        } else {
            d.round(x-13*u,y+22*u,26*u,14*u,3*u,FactoryPalette.dark)
            d.line(x-14*u,y+23*u,x-14*u,y+33*u,2*u,FactoryPalette.steel)
            d.line(x+14*u,y+23*u,x+14*u,y+33*u,2*u,FactoryPalette.steel)
        }
        val outSide=if(l.output.x>x) 1 else -1
        d.round(x+if(outSide==1) 28*u else -54*u,y-12*u,26*u,24*u,3*u,FactoryPalette.dark)
        d.line(x+outSide*30*u,y-13*u,x+outSide*54*u,y-13*u,2*u,FactoryPalette.steel)
        d.line(x+outSide*30*u,y+13*u,x+outSide*54*u,y+13*u,2*u,FactoryPalette.steel)
        // Drive housing, ventilation, feed rollers and jaw guide cylinders read without text.
        d.round(x+38*u,y-19*u,12*u,37*u,4*u,FactoryPalette.front)
        for(i in 0..4) d.line(x+40*u,y-13*u+i*5*u,x+48*u,y-13*u+i*5*u,1f*u,FactoryPalette.top)
        for(side in -1..1 step 2) {
            d.round(x+side*24*u-3*u,y-12*u,6*u,24*u,3*u,FactoryPalette.steel)
            d.round(x+side*37*u-4*u,y+20*u,8*u,9*u,3*u,FactoryPalette.brass)
        }
        // Wrapper spindle, paper roll and threaded rails differentiate it from the table.
        d.line(x-32*u,y+21*u,x-47*u,y+28*u,4*u,FactoryPalette.front)
        d.round(x-50*u,y+23*u,30*u,16*u,6*u,FactoryPalette.carton)
        s.color=FactoryPalette.text; s.ellipse(x-53*u,y+23*u,9*u,16*u,20)
        s.color=FactoryPalette.front; s.ellipse(x-23*u,y+23*u,9*u,16*u,20)
        d.line(x-31*u,y-13*u,x+31*u,y-13*u,2*u,FactoryPalette.top)
        d.line(x-31*u,y+12*u,x+31*u,y+12*u,2*u,FactoryPalette.top)
        // Threaded wrapper web and a narrow inspection guard tie reel, rollers and jaw bed together.
        d.line(x-26*u,y+25*u,x-21*u,y+14*u,8*u,FactoryPalette.carton)
        d.line(x-21*u,y+14*u,x-12*u,y+10*u,6*u,FactoryPalette.carton)
        d.line(x-33*u,y-23*u,x+33*u,y-23*u,2*u,FactoryPalette.front)
        d.bolt(x-30*u,y-23*u,1.5f*u); d.bolt(x+30*u,y-23*u,1.5f*u)
        val n=session.batch.needle
        val jaw=if(n.route==Route.PACKING) sin(((n.progress-.45f)/.55f).coerceIn(0f,1f)*PI.toFloat())*7*u else 0f
        // Open jaws clear the horizontal exit; they close only after the infeed/align phase.
        d.round(x-27*u,y-20*u+jaw,54*u,6*u,2*u,FactoryPalette.steel)
        d.round(x-27*u,y+14*u-jaw,54*u,6*u,2*u,FactoryPalette.steel)
        d.line(x-24*u,y-17*u+jaw,x+24*u,y-17*u+jaw,1.4f*u,FactoryPalette.top)
        d.line(x-24*u,y+17*u-jaw,x+24*u,y+17*u-jaw,1.4f*u,FactoryPalette.top)
        d.bolt(x-46*u,y-27*u,2f*u); d.bolt(x+46*u,y-27*u,2f*u)
        indicator(x+32*u,y+24*u,session.operating(MachineId.PACKER),u)
    }
    private fun bin(x:Float,y:Float,w:Float,h:Float,u:Float,missed:Boolean,count:Int) {
        d.cabinet(x-w/2,y-h/2,w,h,10*u,FactoryPalette.body)
        d.round(x-w/2+5*u,y-h/2+5*u,w-10*u,h-10*u,5*u,FactoryPalette.dark)
        d.line(x-w/2+8*u,y-h/2-5*u,x+w/2-8*u,y-h/2-5*u,2*u,
            if(missed) FactoryPalette.copper else FactoryPalette.brass)
        d.round(x-w/2+13*u,y-h/2-8*u,w-26*u,4*u,2*u,FactoryPalette.dark)
        for(i in 0 until min(count,24)) {
            val xx=x-w*.35f+(i*17%37)*u; val yy=y-h*.27f+(i%5)*3*u
            d.line(xx,yy,xx+12*u,yy+3*u,1f*u,FactoryPalette.brass)
        }
    }
    private fun parcel(x:Float,y:Float,u:Float) {
        d.cabinet(x-21*u,y-10*u,42*u,20*u,6*u,FactoryPalette.carton)
        d.line(x,y-9*u,x,y+9*u,4*u,FactoryPalette.brass)
        d.round(x-11*u,y-4*u,7*u,8*u,1*u,FactoryPalette.text)
        d.line(x-10*u,y-2*u,x-6*u,y-2*u,.7f*u,FactoryPalette.body)
    }
    private fun indicator(x:Float,y:Float,on:Boolean,u:Float) {
        s.color=FactoryPalette.dark; s.circle(x,y,3.3f*u,12)
        s.color=if(on) FactoryPalette.green else FactoryPalette.copper; s.circle(x,y+.4f*u,2*u,12)
    }
}

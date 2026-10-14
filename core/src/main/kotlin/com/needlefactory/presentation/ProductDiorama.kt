package com.needlefactory.presentation

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.needlefactory.simulation.*
import kotlin.math.*

/** Mounted receiver and one real processing press, using the accepted cabinet projection. */
internal class ProductDiorama(private val s:ShapeRenderer) {
    private val d=FactoryDrawing(s)
    fun draw(l:FactoryLayout,session:FactorySession,alpha:Float) {
        if(session.config.pitReceiver) { pitYard(l,session); return }
        val r=l.productReceiver
        d.foot(r.x+7,r.y-5,24f,1f); d.foot(r.x+r.width-7,r.y-5,24f,1f)
        d.cabinet(r.x,r.y,r.width,r.height,8f,FactoryPalette.body)
        d.round(r.x+3,r.y+4,r.width-6,r.height-8,3f,FactoryPalette.dark)
        // A continuous open lip physically adjoins the full manual table boundary.
        d.line(l.table.x+l.table.width,r.y,r.x+4,r.y,4f,FactoryPalette.steel)
        d.line(l.table.x+l.table.width,r.y+r.height,r.x+4,r.y+r.height,4f,FactoryPalette.steel)
        auger(l,session)
        if(!session.enabled(MachineId.VIBRATION)) d.line(r.x+2,r.y+4,r.x+2,r.y+r.height-4,4f,FactoryPalette.copper)
        if(!session.baler) {
            val amount=(session.looseStraw.toFloat()/session.config.strawCount).coerceIn(0f,1f)
            // Loose collection remains visibly unbound, not a free packaged product.
            for(i in 0 until min(12,ceil(amount*12).toInt())) {
                val y=r.y+5+i*1.3f
                d.line(r.x+6+(i%3),y,r.x+r.width-6,y+sin(i.toFloat())*2,.7f,FactoryPalette.brass)
            }
        }
        val m=l.metalTray
        if(session.config.physicalNeedles) {
            // A small gravity sorting pipe, not a free conveyor across the nameplate.
            val lower=l.table.y-30
            d.line(l.metalDock.x,l.metalDock.y-2,l.metalDock.x,lower,4f,FactoryPalette.front)
            d.line(l.metalDock.x,lower,m.x,lower,4f,FactoryPalette.front)
            d.line(m.x,lower,m.x,m.y,4f,FactoryPalette.front)
            d.line(l.metalDock.x-16,l.metalDock.y+1,l.metalDock.x+16,l.metalDock.y+1,5f,FactoryPalette.steel)
            d.line(l.metalDock.x-13,l.metalDock.y+1,l.metalDock.x+13,l.metalDock.y+1,2.5f,FactoryPalette.dark)
            val reject=l.productReject
            d.cabinet(reject.x-13,reject.y-8,26f,16f,5f,FactoryPalette.front)
            d.round(reject.x-10,reject.y-5,20f,10f,2f,FactoryPalette.dark)
            for(i in 0 until session.batch.count(Route.MISSED))
                d.line(reject.x-7+i*5,reject.y-2,reject.x-3+i*5,reject.y+2,1f,FactoryPalette.steel)
        } else d.line(m.x,l.table.y-5,m.x,m.y+11,4f,FactoryPalette.front)
        d.cabinet(m.x-36,m.y-11,72f,22f,7f,FactoryPalette.top)
        d.round(m.x-31,m.y-6,62f,12f,3f,FactoryPalette.dark)
        for(i in 0 until session.batch.count(Route.RECOVERED)+session.batch.count(Route.SORTED)) {
            d.line(m.x-23+i*17,m.y-3,m.x-13+i*17,m.y+3,1f,FactoryPalette.steel)
        }
        if(!session.baler) {
            val f=l.balerFootprint
            d.round(f.x,f.y-4,f.width,f.height,7f,FactoryPalette.floorEdge)
            d.round(f.x+3,f.y,f.width-6,f.height-7,5f,FactoryPalette.floor)
            for(i in 0..1) for(j in 0..1) d.bolt(f.x+8+i*(f.width-16),f.y+6+j*(f.height-16),2f)
            // A quiet dashed mounting outline, not a free working ghost machine.
            for(i in 0..7) {
                d.line(f.x+8+i*11,f.y+13,f.x+13+i*11,f.y+13,1f,FactoryPalette.muted)
                d.line(f.x+8+i*11,f.y+f.height-16,f.x+13+i*11,f.y+f.height-16,1f,FactoryPalette.muted)
            }
            return
        }
        val b=l.productBaler
        val ax=if(session.conveyorOwned) l.productLaneX(session.config.conveyorLaneX) else l.productTransferStart.x
        val ay=if(session.conveyorOwned) l.productLaneY(session.config.conveyorDischargeY) else l.productTransferStart.y
        if(session.conveyorOwned) belt(l,session)
        d.channel(ax,ay,l.productBalerInput.x,l.productBalerInput.y,22f,1f)
        val reject=l.productReject
        d.channel(ax+(l.productBalerInput.x-ax)*.75f,ay+(l.productBalerInput.y-ay)*.75f,reject.x,reject.y,10f,1f)
        d.channel(l.productBalerOutput.x,l.productBalerOutput.y,l.productTray.x,l.productTray.y,24f,1f)
        d.foot(b.x-37,b.y-27,19f,1f); d.foot(b.x+37,b.y-27,19f,1f)
        d.cabinet(b.x-52,b.y-32,104f,68f,12f,FactoryPalette.body)
        d.round(b.x-30,b.y-22,60f,40f,4f,FactoryPalette.dark)
        d.line(b.x-36,b.y-23,b.x-36,b.y+22,5f,FactoryPalette.steel)
        d.line(b.x+36,b.y-23,b.x+36,b.y+22,5f,FactoryPalette.steel)
        val active=session.batch.items.firstOrNull { it.route==Route.BALING }
        val t=active?.let { it.previousProgress+(it.progress-it.previousProgress)*alpha } ?: 0f
        val press=if(active!=null) sin(t*PI.toFloat()).coerceAtLeast(0f) else 0f
        d.round(b.x-28,b.y+13-press*20,56f,9f,2f,FactoryPalette.steel)
        d.round(b.x-5,b.y+17-press*20,10f,24f+press*20,2f,FactoryPalette.front)
        d.cabinet(b.x-14,b.y+28,28f,13f,5f,FactoryPalette.top)
        for(i in 0..1) d.bolt(b.x-39+i*78,b.y-24,2f)
        if(active!=null) bundle(b.x,b.y-7,1f)
        // Binding spool, drive crank and attached casing explain the product transformation.
        s.color=FactoryPalette.dark; s.circle(b.x+43,b.y+19,11f,24)
        s.color=FactoryPalette.brass; s.circle(b.x+43,b.y+19,8f,24)
        s.color=FactoryPalette.steel; s.circle(b.x+43,b.y+19,3f,16)
        d.line(b.x+43,b.y+19,b.x+29,b.y+9,1.4f,FactoryPalette.carton)
        d.round(b.x-48,b.y-20,14f,22f,3f,FactoryPalette.front)
        val angle=t*PI.toFloat()*2
        d.line(b.x-41,b.y-9,b.x-41+cos(angle)*4,b.y-9+sin(angle)*4,2f,FactoryPalette.brass)
        val out=l.productTray
        d.foot(out.x,out.y-12,20f,1f)
        d.cabinet(out.x-25,out.y-17,50f,34f,8f,FactoryPalette.top)
        d.round(out.x-21,out.y-13,42f,26f,4f,FactoryPalette.dark)
        val bundles=(session.bundledStraw+session.config.balerUnitsPerCycle-1)/session.config.balerUnitsPerCycle
        for(i in 0 until min(4,bundles)) bundle(out.x,out.y-7+i*5,1f)
    }
    /** Starter yard: upper-left source bin, open floor, distant square auger pit. */
    private fun pitYard(l:FactoryLayout,session:FactorySession) {
        val t=l.yard
        d.round(t.x-5,t.y-5,t.width+10,t.height+10,4f,FactoryPalette.floorEdge)
        d.round(t.x,t.y,t.width,t.height,3f,FactoryPalette.floor)
        for(i in 1 until (t.width/24.8f).toInt()) d.line(t.x+i*24.8f,t.y,t.x+i*24.8f,t.y+t.height,.45f,FactoryPalette.top)
        for(i in 1 until (t.height/24.8f).toInt()) d.line(t.x,t.y+i*24.8f,t.x+t.width,t.y+i*24.8f,.45f,FactoryPalette.top)
        val b=l.pileBin
        d.cabinet(b.x-4,b.y-3,b.width+8,b.height+6,5f,FactoryPalette.body)
        d.round(b.x,b.y,b.width,b.height,3f,FactoryPalette.dark)
        d.line(b.x-2,b.y,b.x-2,b.y+b.height,2f,FactoryPalette.steel)
        d.line(b.x,b.y+b.height+1,b.x+b.width,b.y+b.height+1,3f,FactoryPalette.steel)
        // Open front allows extraction; no invisible bin wall traps a dragged clump.
        d.line(b.x,b.y,b.x+17,b.y,3f,FactoryPalette.steel)
        d.line(b.x+b.width-17,b.y,b.x+b.width,b.y,3f,FactoryPalette.steel)
        d.bolt(b.x-1,b.y+b.height,1.5f); d.bolt(b.x+b.width+1,b.y+b.height,1.5f)
        val r=l.pitHole; val cx=r.x+r.width/2; val cy=r.y+r.height/2
        d.shadow(r.x-5,r.y-6,r.width+10,r.height+11,4f)
        d.round(r.x-4,r.y-4,r.width+8,r.height+8,3f,FactoryPalette.front)
        d.round(r.x-2,r.y-2,r.width+4,r.height+4,2f,FactoryPalette.steel)
        d.round(r.x,r.y,r.width,r.height,1f,FactoryPalette.dark)
        d.round(r.x+3,r.y+3,r.width-6,r.height-6,1f,FactoryPalette.background)
        // Deep sloping walls and black recessed floor explain the dangerous opening.
        s.color=FactoryPalette.front
        s.triangle(r.x+2,r.y+r.height-2,r.x+8,r.y+r.height-11,r.x+r.width-8,r.y+r.height-11)
        s.triangle(r.x+2,r.y+r.height-2,r.x+r.width-8,r.y+r.height-11,r.x+r.width-2,r.y+r.height-2)
        s.color=FactoryPalette.body
        s.triangle(r.x+2,r.y+2,r.x+8,r.y+9,r.x+8,r.y+r.height-11)
        s.triangle(r.x+2,r.y+2,r.x+8,r.y+r.height-11,r.x+2,r.y+r.height-2)
        d.round(r.x+8,r.y+9,r.width-16,r.height-20,1f,FactoryPalette.background)
        // Recess depth and restrained hazard stripes mark a dangerous floor opening.
        d.line(r.x+2,r.y+2,r.x+2,r.y+r.height-2,3f,FactoryPalette.front)
        d.line(r.x+2,r.y+2,r.x+r.width-2,r.y+2,3f,FactoryPalette.body)
        for(i in 0..4) {
            d.line(r.x-3+i*10,r.y-5,r.x+2+i*10,r.y-2,2f,FactoryPalette.brass)
            d.line(r.x-3+i*10,r.y+r.height+2,r.x+2+i*10,r.y+r.height+5,2f,FactoryPalette.brass)
        }
        // Twin-shaft shredder: two parallel full-length shafts with staggered
        // hooked cutter discs. Width adds discs, never another pair of motors.
        val phase=session.receiverTurns*2*PI.toFloat()
        shredder(l,phase)
        // Shortening dry fibres collapse toward the milling nip after admission.
        val chew=(1-session.strawImpactAge/.65f).coerceIn(0f,1f)
        if(session.receivingEnabled && chew>0f) for(i in 0..11) {
            val a=i*2.39996f+phase*.3f
            val x=cx+cos(a)*(3+chew*8);val y=cy+sin(a)*(2+chew*5)
            val length=chew*(2+i%4)
            d.line(x,y,x+cos(a)*length,y+sin(a)*length,.65f,FactoryPalette.brass)
        }
        if(!session.receivingEnabled) d.line(r.x+3,cy,r.x+r.width-3,cy,4f,FactoryPalette.copper)
        if(session.receivingEnabled) for(i in 0..2) {
            val life=(session.receiverTurns+i*.31f)%1f
            if(life<.26f) {
                val f=life/.26f; val x=cx+(i-1)*9*f; val y=cy+7+f*20-f*f*20
                d.line(x,y,x-1.4f*(i-1),y+1.8f,if(i==1) .8f else .55f,FactoryPalette.brass)
            }
        }
        // Fixed-step admitted-material events, bounded and allocation-free.
        val metal=session.metalImpactAge<1.05f
        val age=if(metal) session.metalImpactAge else session.strawImpactAge
        val lifetime=if(metal) 1.05f else .48f
        if(session.receivingEnabled && age<lifetime) {
            val count=if(metal) 32 else 12
            for(i in 0 until count) {
                val life=age-(i%5)*.022f
                if(life<0f || life>lifetime*.8f) continue
                val angle=i*2.39996f
                val speed=if(metal) 27f+(i%7)*6 else 13f+(i%4)*4
                val x=cx+cos(angle)*speed*life
                val y=cy+4+sin(angle)*speed*life+18*life-26*life*life
                val tail=(if(metal) 3.8f else 1.8f)*(1-life/lifetime)
                d.line(x,y,x-cos(angle)*tail,y-sin(angle)*tail,
                    if(metal) 1.15f else .7f,if(metal && i%3==0) FactoryPalette.top else FactoryPalette.brass)
            }
        }
        val m=l.needleReceiver
        if(session.metalReceiverOwned) {
            d.cabinet(m.x,m.y,m.width,m.height,5f,FactoryPalette.body)
            d.round(m.x+3,m.y+4,m.width-6,m.height-8,3f,FactoryPalette.dark)
            for(i in 0 until session.batch.count(Route.RECOVERED)) d.line(m.x+6,m.y+9+i*6,m.x+25,m.y+13+i*6,.8f,FactoryPalette.steel)
            if(!session.metalReceiverRunning) d.line(m.x+4,m.y+16,m.x+m.width-4,m.y+16,3f,FactoryPalette.copper)
            d.bolt(m.x+2,m.y+2,1f); d.bolt(m.x+m.width-2,m.y+2,1f)
        }
    }
    /** Top/2.5D view of two steel cutter shafts, referenced from owner photo. */
    private fun shredder(l:FactoryLayout,phase:Float) {
        val r=l.pitHole;val cx=r.x+r.width/2;val cy=r.y+r.height/2
        val ux=if(l.landscape) 0f else 1f;val uy=if(l.landscape) 1f else 0f
        val vx=-uy;val vy=ux
        val span=(if(l.landscape) r.height else r.width)-13
        val radius=(min(r.width,r.height)-16)*.27f
        val offset=radius*.89f
        val sections=max(3,(span/6.2f).roundToInt())
        val pitch=span/sections
        for(shaft in 0..1) {
            val side=if(shaft==0) -1f else 1f
            val x=cx+vx*offset*side;val y=cy+vy*offset*side
            val x0=x-ux*span/2;val y0=y-uy*span/2
            val x1=x+ux*span/2;val y1=y+uy*span/2
            d.line(x0,y0,x1,y1,radius*2+2,FactoryPalette.background)
            d.line(x0,y0,x1,y1,radius*2,FactoryPalette.front)
            d.line(x0-vx,y0-vy,x1-vx,y1-vy,radius*1.6f,FactoryPalette.body)
            // Interleaved steel discs and dark spacers; no stretching of teeth.
            for(i in 0 until sections) {
                val along=-span/2+(i+.5f+shaft*.22f)*pitch
                val px=x+ux*along;val py=y+uy*along
                // Curved disc shoulders, not rectangular tabs: projected rounded
                // perimeter retains a thick dark side face and a worn steel rim.
                for(band in 0 until 12) {
                    val q=-1f+(band+.5f)/6f
                    val shoulder=sqrt((1-q*q).coerceAtLeast(0f))
                    val half=pitch*(.27f+.15f*shoulder)
                    val bx=px+vx*q*radius;val by=py+vy*q*radius
                    d.line(bx-ux*half,by-uy*half,bx+ux*half,by+uy*half,
                        radius/6+.15f,if(band<4) FactoryPalette.front else FactoryPalette.body)
                }
                d.line(px+ux*pitch*.34f-vx*radius,py+uy*pitch*.34f-vy*radius,
                    px+ux*pitch*.34f+vx*radius,py+uy*pitch*.34f+vy*radius,
                    1.2f,FactoryPalette.dark)
                for(edge in 0 until 8) {
                    val q=-.9f+edge*.225f;val next=q+.225f
                    val ax=px-ux*pitch*(.27f+.15f*sqrt(1-q*q))+vx*q*radius
                    val ay=py-uy*pitch*(.27f+.15f*sqrt(1-q*q))+vy*q*radius
                    val bx=px-ux*pitch*(.27f+.15f*sqrt(1-next*next))+vx*next*radius
                    val by=py-uy*pitch*(.27f+.15f*sqrt(1-next*next))+vy*next*radius
                    d.line(ax,ay,bx,by,.7f,FactoryPalette.steel)
                }
                // Fixed cleaning comb fingers enter the outer disc gaps.
                val comb=side*(offset+radius+2)
                val rootX=cx+ux*(along-pitch*.45f)+vx*comb
                val rootY=cy+uy*(along-pitch*.45f)+vy*comb
                d.line(rootX,rootY,rootX-vx*side*3.5f,rootY-vy*side*3.5f,
                    pitch*.24f,FactoryPalette.steel)
                // Restrained corrosion in cutter roots; worn cutting faces stay steel.
                if((i+shaft)%3==0) {
                    val rust=sin(-phase*side+i)*radius*.45f
                    d.line(px-vx*rust-ux*pitch*.16f,py-vy*rust-uy*pitch*.16f,
                        px-vx*rust+ux*pitch*.19f,py-vy*rust+uy*pitch*.19f,1.3f,FactoryPalette.copper)
                }
                // Two hooked lobes move around each shaft toward the nip.
                // On the visible upper semicircle cos(angle)>0, radial velocity
                // is -side*cos(angle): both surfaces travel toward the centre.
                val turn=-phase*side+i*.7f+shaft*1.2f
                for(tooth in 0..1) {
                    val angle=turn+tooth*PI.toFloat()
                    if(cos(angle)<0f) continue // hidden underside returns outside
                    val radial=sin(angle)*(radius+1.2f)
                    val hook=side*cos(angle)*pitch*.42f
                    val tx=px+vx*radial;val ty=py+vy*radial
                    val face=cos(angle).coerceAtLeast(0f)
                    val thick=3.3f*face
                    val ax=tx-ux*pitch*.44f;val ay=ty-uy*pitch*.44f
                    val bx=tx+ux*pitch*.44f;val by=ty+uy*pitch*.44f
                    val ex=tx+ux*(hook+pitch*.30f)-vx*side*thick
                    val ey=ty+uy*(hook+pitch*.30f)-vy*side*thick
                    val fx=tx+ux*(hook-pitch*.30f)-vx*side*thick
                    val fy=ty+uy*(hook-pitch*.30f)-vy*side*thick
                    // Curved hooked tip: broad root, projecting shoulder and
                    // shorter inward cutting lip rather than a flat paddle.
                    val hx=ex+ux*side*pitch*.14f-vx*side*thick*.2f
                    val hy=ey+uy*side*pitch*.14f-vy*side*thick*.2f
                    val ix=fx+ux*side*pitch*.2f+vx*side*thick*.3f
                    val iy=fy+uy*side*pitch*.2f+vy*side*thick*.3f
                    s.color=FactoryPalette.body
                    s.triangle(ax,ay,bx,by,ex,ey);s.triangle(ax,ay,ex,ey,hx,hy)
                    s.triangle(ax,ay,hx,hy,ix,iy);s.triangle(ax,ay,ix,iy,fx,fy)
                    d.line(ix,iy,hx,hy,.25f+1.05f*face,FactoryPalette.steel)
                }
            }
            // Both shafts terminate in bearings at the pit walls.
            for(end in 0..1) {
                val ex=if(end==0) x0 else x1;val ey=if(end==0) y0 else y1
                d.line(ex-vx*(radius+1),ey-vy*(radius+1),ex+vx*(radius+1),ey+vy*(radius+1),3f,FactoryPalette.front)
                d.bolt(ex,ey,1f)
            }
        }
    }
    /** Project a real helix around the vertical shaft; back flights sit behind it. */
    private fun auger(l:FactoryLayout,session:FactorySession) {
        val r=l.productReceiver; val cx=r.x+r.width/2
        val y0=r.y+13; val y1=r.y+r.height-13; val radius=(r.width-12)/2
        val pitch=25f; val phase=session.receiverTurns*2*PI.toFloat()
        val steps=((y1-y0)/pitch*18).toInt().coerceAtLeast(18)
        // Back half of the spiral, shaft, then front half: a screw instead of diagonal bars.
        for(front in 0..1) {
            if(front==1) {
                d.line(cx+1,y0,cx+1,y1,5f,FactoryPalette.front)
                d.line(cx,y0,cx,y1,3.5f,FactoryPalette.steel)
                d.line(cx-1,y0,cx-1,y1,.8f,FactoryPalette.top)
            }
            for(i in 0 until steps) {
                val a=phase+(y1-y0)*i/steps/pitch*2*PI.toFloat()
                val b=phase+(y1-y0)*(i+1)/steps/pitch*2*PI.toFloat()
                if((cos((a+b)/2)>=0)!=(front==1)) continue
                val ax=cx+radius*sin(a); val bx=cx+radius*sin(b)
                val ay=y0+(y1-y0)*i/steps; val by=y0+(y1-y0)*(i+1)/steps
                // A flight is a continuous metal PLATE attached to the hub, not a coil of wire.
                val ha=cx+1.8f*sin(a); val hb=cx+1.8f*sin(b)
                s.color=if(front==1) FactoryPalette.steel else FactoryPalette.body
                s.triangle(ha,ay,ax,ay,bx,by)
                s.triangle(ha,ay,bx,by,hb,by)
                d.line(ax,ay-.8f,bx,by-.8f,1.5f,FactoryPalette.front)
                d.line(ax,ay,bx,by,1.2f,if(front==1) FactoryPalette.steel else FactoryPalette.body)
                if(front==1) d.line(ax,ay+.35f,bx,by+.35f,.45f,FactoryPalette.top)
            }
        }
        // End bearings, coupling, gearbox and mounts physically support the rotating shaft.
        for(end in 0..1) {
            val y=if(end==0) y0-3 else y1+3
            d.round(cx-10,y-4,20f,8f,2f,FactoryPalette.front)
            d.round(cx-4,y-3,8f,6f,2f,FactoryPalette.body)
            d.bolt(cx-8,y,1.1f); d.bolt(cx+8,y,1.1f)
        }
        d.cabinet(r.x+r.width-6,r.y+14,16f,24f,5f,FactoryPalette.body)
        for(i in 0..3) d.line(r.x+r.width-3,r.y+18+i*4,r.x+r.width+7,r.y+18+i*4,.8f,FactoryPalette.front)
        d.line(cx,y0-4,r.x+r.width+2,r.y+15,3.5f,FactoryPalette.front)
        d.line(cx,y0-4,r.x+r.width+2,r.y+15,1.5f,FactoryPalette.steel)
        s.color=if(session.receivingEnabled) FactoryPalette.green else FactoryPalette.muted
        s.circle(r.x+r.width+5,r.y+34,1.5f,12)
        // A bolted output collar is ready for future attachments, not free belt equipment.
        d.round(cx-10,r.y-3,20f,9f,2f,FactoryPalette.front)
        d.round(cx-7,r.y-1,14f,5f,1f,FactoryPalette.dark)
        d.bolt(cx-9,r.y+2,1f); d.bolt(cx+9,r.y+2,1f)
    }
    /** A bolted, low-profile lane occupies the paid right-hand portion of the table. */
    private fun belt(l:FactoryLayout,session:FactorySession) {
        val x=l.productLaneX(session.config.conveyorLaneX)
        val bottom=l.productLaneY(session.config.conveyorDischargeY); val top=l.table.y+l.table.height-5
        val w=(session.config.conveyorLaneX-session.config.conveyorIntakeX)*l.table.width/10
        val h=top-bottom
        d.shadow(x-w-3,bottom-7,w*2+6,h+10,5f)
        d.cabinet(x-w-2,bottom-5,w*2+4,h+9,7f,FactoryPalette.body)
        d.round(x-w+3,bottom,w*2-6,h,4f,FactoryPalette.belt)
        for(i in 0..(h/14).toInt()) {
            val y=top-((i*14+session.conveyorSeconds*25)%h)
            d.line(x-w+5,y,x+w-5,y,1f,FactoryPalette.front)
        }
        for(side in -1..1 step 2) {
            d.line(x+side*w,bottom,x+side*w,top,3f,FactoryPalette.steel)
            for(i in 0..2) d.bolt(x+side*w,bottom+h*i/2,1.5f)
        }
        for(y in listOf(bottom,top)) {
            d.line(x-w-1,y,x+w+1,y,9f,FactoryPalette.dark)
            d.line(x-w+4,y+1,x+w-4,y+1,6f,FactoryPalette.steel)
            for(i in -2..2) d.line(x+i*8,y-3,x+i*8,y+3,.8f,FactoryPalette.body)
        }
        d.cabinet(x+w-5,bottom-17,19f,15f,5f,FactoryPalette.front)
        d.bolt(x+w+4,bottom-9,3f)
        // Downward chevron gives the transport a direction even while paused.
        d.line(x-5,top-20,x,top-25,1.3f,FactoryPalette.brass)
        d.line(x+5,top-20,x,top-25,1.3f,FactoryPalette.brass)
    }
    fun overhead(l:FactoryLayout,session:FactorySession) {
        if(session.config.pitReceiver) return
        if(!session.magnetOwned) return
        val x=l.productLaneX(session.config.conveyorLaneX)
        val y=l.productLaneY(session.config.conveyorDischargeY)
        // Extracted metal travels on a lined chute attached to the receiver tray.
        val lower=l.table.y-30
        d.channel(x,y,x,lower,10f,1f)
        d.channel(x,lower,l.metalTray.x,lower,10f,1f)
        d.channel(l.metalTray.x,lower,l.metalTray.x,l.metalTray.y,10f,1f)
        for(side in -1..1 step 2) {
            d.foot(x+side*32,y-5,12f,1f)
            d.round(x+side*32-3,y-4,6f,37f,2f,FactoryPalette.front)
            d.bolt(x+side*32,y+28,2f)
        }
        d.cabinet(x-35,y+20,70f,15f,7f,FactoryPalette.body)
        d.round(x-23,y+3,9f,20f,2f,FactoryPalette.steel)
        d.round(x+14,y+3,9f,20f,2f,FactoryPalette.steel)
        d.round(x-13,y+14,26f,14f,3f,FactoryPalette.copper)
        for(i in -4..4) d.line(x+i*2.5f,y+16,x+i*2.5f,y+26,.8f,FactoryPalette.brass)
        d.round(x-21,y,8f,6f,2f,FactoryPalette.steel)
        d.round(x+13,y,8f,6f,2f,FactoryPalette.steel)
        s.color=if(session.operating(MachineId.SEPARATOR)) FactoryPalette.green else FactoryPalette.muted
        s.circle(x+28,y+27,2.2f,12)
    }
    private fun bundle(x:Float,y:Float,u:Float) {
        d.shadow(x-16*u,y-6*u,32*u,14*u,3f)
        d.round(x-16*u,y-6*u,32*u,13*u,3f,FactoryPalette.carton)
        for(i in 0..5) d.line(x-14*u,y-4*u+i*1.8f*u,x+14*u,y-3*u+i*1.8f*u,.6f*u,FactoryPalette.brass)
        d.line(x-7*u,y-6*u,x-7*u,y+7*u,2.2f*u,FactoryPalette.front)
        d.line(x+7*u,y-6*u,x+7*u,y+7*u,2.2f*u,FactoryPalette.front)
        d.line(x-15*u,y+7*u,x+15*u,y+7*u,.8f*u,FactoryPalette.text)
    }
}

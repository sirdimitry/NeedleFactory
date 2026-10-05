package com.needlefactory.presentation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.needlefactory.simulation.*

/** Compact industrial instrument panel, with named states and optional subordinate telemetry. */
internal class FactoryHud(shapes:ShapeRenderer,private val batch:SpriteBatch) {
    private val d=FactoryDrawing(shapes)
    private val s=shapes
    private val names=arrayOf("Feed gate","Conveyor","Magnet","Packer")
    private val statNames=arrayOf("BATCH","RECOVERED","MISSED","PACKAGED")
    private val values=Array(4) { "0" }
    private var status=""
    private var detail=""
    fun refresh(session:FactorySession) {
        values[0]=session.batchNumber.toString(); values[1]=session.recovered.toString()
        values[2]=session.missed.toString(); values[3]=session.packaged.toString()
        status=when {
            session.batch.complete -> if(session.batch.needle.route==Route.OUTPUT) "Needle packaged. Ready for the next batch." else "Metal missed. Enable magnet for the next batch."
            !session.running -> "Line paused. Material and progress held."
            !session.enabled(MachineId.CONVEYOR) -> "Conveyor off. Transport held."
            !session.enabled(MachineId.VIBRATION) && session.batch.count(Route.INTAKE)>0 -> "Feed gate closed. Material held."
            session.batch.needle.route==Route.BUFFER -> "Needle waiting. Switch on the packer."
            session.batch.count(Route.INTAKE)>0 -> "Push straw right into the feed mouth."
            else -> "Material in transit. Watch the machines."
        }
        detail="Seed ${session.batch.seed}  /  ${session.batch.elapsed.toInt()}s  /  Buffer ${session.batch.count(Route.BUFFER)}"
    }
    private fun cardsX(l:FactoryLayout) = if(l.landscape) l.table.x-10f else 16f
    private fun cardsY(l:FactoryLayout) = if(l.landscape) l.table.y-130f else l.height-114f
    private fun cardWidth(l:FactoryLayout) = if(l.landscape) 58f else (l.width-50f)/4
    fun shapes(l:FactoryLayout,session:FactorySession,diagnostics:Boolean,expanded:Boolean) {
        if(l.landscape) {
            // Identity/counters use the free bay below the search module, not its height.
            d.round(l.table.x-16f,(cardsY(l)-6f).coerceAtLeast(0f),268f,100f,8f,FactoryPalette.background)
        } else d.round(0f,l.height-122f,l.width,122f,0f,FactoryPalette.background)
        val y=cardsY(l); val w=cardWidth(l); val x=cardsX(l)
        d.round(x,y,4*w+18,48f,7f,FactoryPalette.panelEdge)
        d.round(x+1,y+1,4*w+16,46f,6f,FactoryPalette.panel)
        for(i in 1..3) d.line(x+i*(w+6)-3,y+9,x+i*(w+6)-3,y+39,.7f,FactoryPalette.panelEdge)
        d.line(x+3*(w+6)+10,y+3,x+4*w+8,y+3,2f,FactoryPalette.green)
        // Recessed control console with consistent grouping and thumb-sized targets.
        val panel=l.console
        d.round(panel.x,panel.y,panel.width,panel.height,if(l.landscape) 10f else 0f,FactoryPalette.panel)
        d.line(panel.x+10,panel.y+panel.height,panel.x+panel.width-10,panel.y+panel.height,1f,FactoryPalette.panelEdge)
        for(i in 0..3) {
            val r=l.controls[i]; val on=session.enabled(MachineId.entries[i])
            d.round(r.x,r.y,r.width,r.height,7f,FactoryPalette.panelEdge)
            d.round(r.x+1,r.y+1,r.width-2,r.height-2,6f,FactoryPalette.background)
            // An actual two-position switch and ON/OFF text both convey state.
            val cx=r.x+r.width-21; val cy=r.y+17
            d.round(cx-12,cy-6,24f,12f,6f,if(on) FactoryPalette.body else FactoryPalette.panelEdge)
            s.color=if(on) FactoryPalette.green else FactoryPalette.muted
            s.circle(cx+if(on) 6f else -6f,cy,4.5f,16)
        }
        val r=l.run; val n=l.next
        d.round(r.x,r.y,r.width,r.height,8f,FactoryPalette.steel)
        d.round(r.x+1,r.y+1,r.width-2,r.height-2,7f,FactoryPalette.panel)
        if(session.running) {
            d.round(r.x+15,r.y+r.height/2-6,3.5f,12f,1f,FactoryPalette.text)
            d.round(r.x+22,r.y+r.height/2-6,3.5f,12f,1f,FactoryPalette.text)
        } else {
            s.color=FactoryPalette.green
            s.triangle(r.x+16,r.y+r.height/2-7,r.x+16,r.y+r.height/2+7,r.x+26,r.y+r.height/2)
        }
        d.round(n.x,n.y,n.width,n.height,8f,if(session.batch.complete) FactoryPalette.green else FactoryPalette.panelEdge)
        if(!session.batch.complete) d.round(n.x+1,n.y+1,n.width-2,n.height-2,7f,FactoryPalette.panel)
        val c=if(session.batch.complete) FactoryPalette.background else FactoryPalette.muted
        d.line(n.x+n.width-26,n.y+n.height/2,n.x+n.width-14,n.y+n.height/2,1.8f,c)
        d.line(n.x+n.width-19,n.y+n.height/2+5,n.x+n.width-14,n.y+n.height/2,1.8f,c)
        d.line(n.x+n.width-19,n.y+n.height/2-5,n.x+n.width-14,n.y+n.height/2,1.8f,c)
        // A quiet status bullet and telemetry drawer, outside material/machine space.
        s.color=if(session.running && !session.batch.complete) FactoryPalette.green else FactoryPalette.brass
        if(!l.landscape) s.circle(20f,151f,2.7f,14)
        if(diagnostics) {
            val t=l.telemetry; d.round(t.x,t.y,t.width,t.height,7f,if(expanded) FactoryPalette.body else FactoryPalette.panel)
            if(expanded) d.round(if(l.landscape) panel.x+8 else 15f,
                if(l.landscape) panel.y+panel.height-69 else 139f,
                if(l.landscape) panel.width-16 else l.width-30,if(l.landscape) 44f else 24f,6f,FactoryPalette.background)
        }
    }
    fun text(l:FactoryLayout,session:FactorySession,font:BitmapFont,metrics:String,diagnostics:Boolean,expanded:Boolean) {
        batch.begin()
        fun text(value:String,x:Float,y:Float,size:Float,c:Color=FactoryPalette.text) {
            font.data.setScale(size/48f); font.color=c; font.draw(batch,value,x,y)
        }
        text("NEEDLE / FACTORY",if(l.landscape) l.table.x-4f else 20f,
            if(l.landscape) l.table.y-50f else l.height-18f,if(l.landscape) 16f else 19f)
        text("LINE 01   /   MANUAL FEED",if(l.landscape) l.table.x-3f else 21f,
            if(l.landscape) l.table.y-70f else l.height-46f,10.5f,FactoryPalette.muted)
        val x=cardsX(l); val y=cardsY(l); val w=cardWidth(l)
        for(i in 0..3) {
            text(statNames[i],x+i*(w+6)+if(l.landscape) 5f else 10f,y+42,
                if(l.landscape) 9f else 11.5f,if(i==3) FactoryPalette.green else FactoryPalette.muted)
            text(values[i],x+i*(w+6)+10,y+26,22f,if(i==3) FactoryPalette.green else FactoryPalette.text)
        }
        if(l.landscape) {
            text("LINE CONTROL",l.console.x+10,l.console.y+l.console.height-12,12f,FactoryPalette.muted)
            if(!expanded) text(when {
                session.batch.complete -> "Batch complete / ready"
                !session.running -> "Paused / state held"
                !session.enabled(MachineId.CONVEYOR) -> "Transport held / belt off"
                session.batch.needle.route==Route.BUFFER -> "Needle queued / packer off"
                session.batch.count(Route.INTAKE)>0 -> "Push right / manual feed"
                else -> "Processing / line running"
            },l.console.x+10,l.console.y+l.console.height-35,10.5f,FactoryPalette.muted)
        } else if(!expanded) text(status,l.statusX,l.statusY,13f,FactoryPalette.muted)
        for(i in 0..3) {
            val r=l.controls[i]
            text(names[i],r.x+8,r.y+45,if(l.landscape) 13f else 14f)
            text(if(session.enabled(MachineId.entries[i])) "ON" else "OFF",r.x+10,r.y+23,10f,FactoryPalette.muted)
        }
        text(if(session.running) "Pause line" else "Run line",l.run.x+36,l.run.y+l.run.height/2+8,14f)
        text("Next batch",l.next.x+15,l.next.y+l.next.height/2+8,15f,
            if(session.batch.complete) FactoryPalette.background else FactoryPalette.muted)
        // Nameplates are on front faces, never floating over straw or moving metal.
        val u=l.unit
        text("01 / FEED",l.table.x+52*u,l.table.y-11*u,9.5f*u,FactoryPalette.text)
        text("03 / MAGNET",l.magnet.x-26*u,l.magnet.y+42*u,8.5f*u)
        text("BUFFER",l.buffer.x-18*u,l.buffer.y-24*u,8.5f*u)
        text("04 / PACKER",l.packer.x-31*u,l.packer.y-33*u,9f*u)
        text("OUTPUT",l.output.x-18*u,l.output.y-23*u,8.5f*u)
        text("STRAW",l.residue.x-15*u,l.residue.y-21*u,8.5f*u)
        text("MISSED",l.missed.x-16*u,l.missed.y-20*u,8.5f*u)
        if(diagnostics) {
            text("i",l.telemetry.x+11,l.telemetry.y+23,18f,FactoryPalette.muted)
            if(expanded) {
                val y0=if(l.landscape) l.console.y+l.console.height-69 else 139f
                val xx=if(l.landscape) l.console.x+12 else 25f
                if(l.landscape) {
                    font.data.setScale(9f/48f); font.color=FactoryPalette.green
                    font.draw(batch,metrics,xx,y0+40,158f,com.badlogic.gdx.utils.Align.left,true)
                    text(detail,xx,y0+12,8f,FactoryPalette.muted)
                } else {
                    text(metrics,xx,y0+23,10.5f,FactoryPalette.green)
                    text(detail,xx,y0+11,9f,FactoryPalette.muted)
                }
            }
        }
        batch.end()
    }
}

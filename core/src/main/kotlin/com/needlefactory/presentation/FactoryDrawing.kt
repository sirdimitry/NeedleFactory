package com.needlefactory.presentation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.*

/** Shared material/lighting vocabulary for the diorama and its instrument panel. */
internal object FactoryPalette {
    val background=Color.valueOf("101E24")
    val panel=Color.valueOf("1C2D33")
    val panelEdge=Color.valueOf("33474D")
    val text=Color.valueOf("E8EBDE")
    val muted=Color.valueOf("96ADA9")
    val floor=Color.valueOf("6B7975")
    val floorEdge=Color.valueOf("424F4D")
    val body=Color.valueOf("547570")
    val top=Color.valueOf("77918A")
    val front=Color.valueOf("36544F")
    val dark=Color.valueOf("243532")
    val belt=Color.valueOf("303E3B")
    val steel=Color.valueOf("A4B2A7")
    val brass=Color.valueOf("C79756")
    val copper=Color.valueOf("B96945")
    val green=Color.valueOf("9AC6A2")
    val warning=Color.valueOf("D39A68")
    val carton=Color.valueOf("B79C6C")
}

internal class FactoryDrawing(val shapes:ShapeRenderer) {
    fun round(x:Float,y:Float,w:Float,h:Float,radius:Float,color:Color) {
        val r=min(radius,min(w,h)/2).coerceAtLeast(0f)
        shapes.color=color
        shapes.rect(x+r,y,w-2*r,h); shapes.rect(x,y+r,w,h-2*r)
        if(r>0) {
            shapes.circle(x+r,y+r,r,12); shapes.circle(x+w-r,y+r,r,12)
            shapes.circle(x+r,y+h-r,r,12); shapes.circle(x+w-r,y+h-r,r,12)
        }
    }
    fun line(x:Float,y:Float,xx:Float,yy:Float,w:Float,c:Color) {
        shapes.color=c; shapes.rectLine(x,y,xx,yy,w)
    }
    fun shadow(x:Float,y:Float,w:Float,h:Float,r:Float) {
        for(i in 3 downTo 0) {
            shapes.setColor(.035f,.06f,.055f,.035f+(3-i)*.02f)
            val s=i*2f
            // Preserve alpha; round() would assign a palette colour.
            val rr=r+s
            shapes.rect(x+7-s+rr,y-9-s,w+2*s-2*rr,h+2*s)
            shapes.rect(x+7-s,y-9-s+rr,w+2*s,h+2*s-2*rr)
            shapes.circle(x+7-s+rr,y-9-s+rr,rr,12)
            shapes.circle(x+7+w+s-rr,y-9-s+rr,rr,12)
            shapes.circle(x+7-s+rr,y-9+h+s-rr,rr,12)
            shapes.circle(x+7+w+s-rr,y-9+h+s-rr,rr,12)
        }
    }
    fun cabinet(x:Float,y:Float,w:Float,h:Float,depth:Float,top:Color=FactoryPalette.top) {
        shadow(x,y-depth,w,h+depth,8f)
        round(x,y-depth,w,h,8f,FactoryPalette.front)
        round(x,y-2,w,h+2,8f,FactoryPalette.body)
        round(x,y,w,h,8f,top)
        line(x+9,y+h-2,x+w-9,y+h-2,1.3f,FactoryPalette.steel)
        line(x+10,y-depth+3,x+w-10,y-depth+3,1f,FactoryPalette.dark)
    }
    fun bolt(x:Float,y:Float,r:Float=2f) {
        shapes.color=FactoryPalette.dark; shapes.circle(x+.6f,y-.8f,r+.6f,10)
        shapes.color=FactoryPalette.steel; shapes.circle(x,y,r,10)
        line(x-r*.5f,y,x+r*.5f,y,.6f,FactoryPalette.front)
    }
    fun foot(x:Float,y:Float,height:Float,u:Float) {
        shadow(x-9*u,y-height-3*u,18*u,7*u,3*u)
        round(x-8*u,y-height-2*u,16*u,6*u,3*u,FactoryPalette.dark)
        round(x-3*u,y-height,6*u,height,2*u,FactoryPalette.front)
        line(x-2*u,y-height+3*u,x-2*u,y,1.4f*u,FactoryPalette.top)
    }
    fun channel(ax:Float,ay:Float,bx:Float,by:Float,width:Float,u:Float) {
        val length=hypot(bx-ax,by-ay)
        if(length<.001f) return
        val nx=-(by-ay)/length; val ny=(bx-ax)/length
        line(ax+5*u,ay-7*u,bx+5*u,by-7*u,width+7*u,FactoryPalette.floorEdge)
        line(ax,ay-4*u,bx,by-4*u,width+8*u,FactoryPalette.front)
        line(ax,ay,bx,by,width+6*u,FactoryPalette.top)
        line(ax,ay,bx,by,width-3*u,FactoryPalette.dark)
        line(ax+nx*width/2,ay+ny*width/2,bx+nx*width/2,by+ny*width/2,2.5f*u,FactoryPalette.steel)
        line(ax-nx*width/2,ay-ny*width/2,bx-nx*width/2,by-ny*width/2,3.5f*u,FactoryPalette.body)
        // End collars read as bolted transfer mouths, not line segments ending in space.
        for(i in 0..1) {
            val x=if(i==0) ax else bx; val y=if(i==0) ay else by
            line(x-nx*(width/2+3*u),y-ny*(width/2+3*u),x+nx*(width/2+3*u),y+ny*(width/2+3*u),3*u,FactoryPalette.steel)
            bolt(x+nx*(width/2+2*u),y+ny*(width/2+2*u),1.3f*u)
            bolt(x-nx*(width/2+2*u),y-ny*(width/2+2*u),1.3f*u)
        }
        // Braced, floor-mounted saddles make diagonal ramps supported objects.
        for(i in 1..max(1,(length/(75*u)).toInt())) {
            val t=i.toFloat()/(max(1,(length/(75*u)).toInt())+1)
            val x=ax+(bx-ax)*t; val y=ay+(by-ay)*t
            foot(x+nx*(width/2+4*u),y+ny*(width/2+4*u)-5*u,14*u,u)
            line(x-nx*(width/2+3*u),y-ny*(width/2+3*u)-4*u,
                x+nx*(width/2+3*u),y+ny*(width/2+3*u)-4*u,3*u,FactoryPalette.front)
        }
    }
}

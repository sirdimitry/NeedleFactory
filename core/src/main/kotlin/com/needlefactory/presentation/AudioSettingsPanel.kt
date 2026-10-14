package com.needlefactory.presentation

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import kotlin.math.roundToInt

/** Ten large controls: one column portrait, two landscape; no hidden selection pane. */
class AudioSettingsLayout {
    val box=Rectangle();val back=Rectangle()
    val rows=Array(AudioChannel.entries.size) {Rectangle()}
    val tracks=Array(AudioChannel.entries.size) {Rectangle()}
    val icons=Array(AudioChannel.entries.size) {Rectangle()}
    fun layout(width:Float,height:Float,landscape:Boolean) {
        val columns=if(landscape) 2 else 1
        val w=minOf(if(landscape) 760f else 390f,width-24)
        val h=minOf(if(landscape) 410f else 760f,height-24)
        box.set((width-w)/2,(height-h)/2,w,h)
        back.set(box.x+16,box.y+12,box.width-32,44f)
        val n=(rows.size+columns-1)/columns;val rh=(h-125)/n;val cw=(w-40)/columns
        for(i in rows.indices) {
            val col=i/n;val row=i%n
            rows[i].set(box.x+16+col*(cw+8),box.y+h-66-(row+1)*rh,cw,rh-3)
            val r=rows[i]
            icons[i].set(r.x+2,r.y,32f,r.height)
            tracks[i].set(r.x+44,r.y+1,r.width-58,maxOf(13f,r.height-22))
        }
    }
    fun percent(i:Int,x:Float)=(((x-tracks[i].x)/tracks[i].width).coerceIn(0f,1f)*100).roundToInt()
}

internal class AudioSettingsPanel(shapes:ShapeRenderer,private val sprites:SpriteBatch,private val audio:FactoryAudio) {
    val geometry=AudioSettingsLayout()
    private val d=FactoryDrawing(shapes)
    private var held=-1;private var backHeld=false
    fun down(p:Vector2) {
        backHeld=geometry.back.contains(p);held=-1
        for(i in geometry.rows.indices) {
            if(geometry.icons[i].contains(p)) {audio.preview(AudioChannel.entries[i]);return}
            if(geometry.tracks[i].contains(p)) {held=i;drag(p);return}
        }
    }
    fun drag(p:Vector2) {if(held>=0) audio.setLevel(AudioChannel.entries[held],geometry.percent(held,p.x))}
    fun up(p:Vector2):Boolean {audio.saveLevels();val back=backHeld && geometry.back.contains(p);cancel();return back}
    fun cancel() {held=-1;backHeld=false}
    fun shape(l:FactoryLayout) {
        geometry.layout(l.width,l.height,l.landscape)
        val b=geometry.box
        d.shapes.setColor(0f,0f,0f,.65f);d.shapes.rect(0f,0f,l.width,l.height)
        d.cabinet(b.x,b.y,b.width,b.height,5f,FactoryPalette.panel)
        for(i in geometry.rows.indices) {
            val r=geometry.rows[i];val t=geometry.tracks[i];val icon=geometry.icons[i]
            d.round(r.x,r.y,r.width,r.height,5f,FactoryPalette.background)
            val x=icon.x+13;val y=icon.y+icon.height/2
            symbol(AudioChannel.entries[i],x,y)
            val yy=t.y+t.height/2;val fraction=audio.levels[AudioChannel.entries[i]]/100f
            d.round(t.x,yy-2,t.width,4f,2f,FactoryPalette.panelEdge)
            if(fraction>0) d.round(t.x,yy-2,t.width*fraction,4f,2f,FactoryPalette.green)
            d.shapes.color=FactoryPalette.text;d.shapes.circle(t.x+t.width*fraction,yy,5f,16)
        }
        val r=geometry.back;d.round(r.x,r.y,r.width,r.height,6f,FactoryPalette.panelEdge)
    }
    private fun symbol(c:AudioChannel,x:Float,y:Float) {
        val color=FactoryPalette.brass;val s=d.shapes;s.color=color
        when(c) {
            AudioChannel.FAN->{s.circle(x,y,2f,12);for(i in 0..2) {val a=i*2.094f;s.circle(x+kotlin.math.cos(a)*5,y+kotlin.math.sin(a)*5,3f,12)}}
            AudioChannel.AUGER->{d.line(x-9,y,x+9,y,1.4f,color);for(i in -1..1) d.line(x+i*5-2,y-6,x+i*5+2,y+6,2f,color)}
            AudioChannel.STRAW->{for(i in -1..1) d.line(x-7,y+i*4,x+7,y-i*3,1f,color)}
            AudioChannel.METAL->{d.line(x-8,y-5,x+8,y+5,1.5f,color);s.circle(x-6,y-4,1.8f,10)}
            AudioChannel.REWARDS->{s.circle(x,y,7f,20);s.color=FactoryPalette.background;s.circle(x,y,4f,16)}
            AudioChannel.ROBOT->{d.round(x-6,y-4,12f,9f,3f,color);for(i in 0..2) d.round(x-6+i*4,y+2,3f,6f,1f,color)}
            AudioChannel.UI->{d.round(x-8,y-6,16f,12f,2f,color);s.color=FactoryPalette.background;s.rect(x-5,y-2,10f,4f)}
            AudioChannel.AMBIENT->{for(i in -1..1) d.line(x+i*6,y-4,x+i*6+2,y+5,1.5f,color)}
            else->{s.rect(x-8,y-3,5f,6f);s.triangle(x-3,y+3,x+3,y+7,x+3,y-7);d.line(x+7,y-4,x+9,y+4,1.4f,color)}
        }
    }
    fun text(font:BitmapFont) {
        val b=geometry.box;sprites.begin();font.color=FactoryPalette.brass;font.data.setScale(18f/48)
        font.draw(sprites,"ЗВУК",b.x+18,b.y+b.height-18)
        font.color=FactoryPalette.muted;font.data.setScale(10f/48)
        font.draw(sprites,"Значок: послушать  /  0%: выключить",b.x+18,b.y+b.height-42)
        for(i in geometry.rows.indices) {
            val r=geometry.rows[i];font.color=FactoryPalette.text;font.data.setScale(11f/48)
            font.draw(sprites,AudioChannel.entries[i].title,r.x+42,r.y+r.height-3,r.width-92,Align.left,false)
            font.color=FactoryPalette.green
            font.draw(sprites,"${audio.levels[AudioChannel.entries[i]]}%",r.x+r.width-45,r.y+r.height-3,40f,Align.right,false)
        }
        val r=geometry.back;font.data.setScale(14f/48);font.color=FactoryPalette.text
        font.draw(sprites,"Назад",r.x+12,r.y+28,r.width-24,Align.center,false);sprites.end()
    }
}

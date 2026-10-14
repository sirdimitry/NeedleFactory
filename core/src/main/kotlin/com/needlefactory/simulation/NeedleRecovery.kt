package com.needlefactory.simulation

import kotlin.math.*

/** Gameplay-space exposure/pickup, independent of camera, shader and rendered caches. */
object NeedleRecovery {
    private fun covered(batch:FactoryBatch,x:Float,y:Float,previous:Boolean=false):Boolean {
        for(item in batch.items) if(item.material==Material.STRAW && (item.route==Route.INTAKE || item.route==Route.RECEIVING)) {
            val angle=if(previous) item.previousAngle else item.angle
            val c=cos(angle); val s=sin(angle)
            val dx=x-if(previous) item.previousX else item.x
            val dy=y-if(previous) item.previousY else item.y
            val lx=c*dx+s*dy; val ly=-s*dx+c*dy
            for(f in item.strands) {
                val fx=f.cosine*(lx-f.x)+f.sine*(ly-f.y)
                val fy=-f.sine*(lx-f.x)+f.cosine*(ly-f.y)
                val t=fx/f.length
                if(abs(t)>.5f) continue
                val slope=-8*f.bend*t/f.length
                val offset=fy-f.bend*(1-4*t*t)
                val half=f.width*.5f*min(1f,(.5f-abs(t))*12f)
                if(offset*offset<=half*half*(1+slope*slope)) return true
            }
        }
        return false
    }
    fun fraction(batch:FactoryBatch,n:FactoryItem,previous:Boolean=false):Float {
        var open=0
        val angle=if(previous) n.previousAngle else n.angle
        val c=cos(angle); val s=sin(angle)
        for(k in 0..30) {
            val a=(k/30f-.5f)*n.length
            val x=(if(previous) n.previousX else n.x)+c*a
            val y=(if(previous) n.previousY else n.y)+s*a
            if(!covered(batch,x,y,previous)) open++
        }
        return open/31f
    }
    fun canPick(batch:FactoryBatch,n:FactoryItem,x:Float,y:Float):Boolean {
        val c=cos(n.angle); val s=sin(n.angle)
        val along=((x-n.x)*c+(y-n.y)*s).coerceIn(-n.length/2,n.length/2)
        val px=n.x+c*along; val py=n.y+s*along
        return hypot(x-px,y-py)<=.4f && !covered(batch,px,py) && fraction(batch,n)>.6f && fraction(batch,n,true)>.6f
    }
}

package com.needlefactory.simulation

import kotlin.math.*

/** Input-only allocation; pick actual curved stalks, not a large body bounding box. */
object StrawGrab {
    data class Hit(val id:Int,val x:Float,val y:Float,val distance:Float)
    fun pick(batch:FactoryBatch,x:Float,y:Float,tolerance:Float,metal:Boolean=false):Hit? {
        if(metal) for(n in batch.needles) if(n.route==Route.INTAKE && NeedleRecovery.canPick(batch,n,x,y)) {
            val c=cos(n.angle); val s=sin(n.angle)
            val a=((x-n.x)*c+(y-n.y)*s).coerceIn(-n.length/2,n.length/2)
            return Hit(n.id,n.x+c*a,n.y+s*a,hypot(x-n.x-c*a,y-n.y-s*a))
        }
        var best:Hit?=null
        // Renderer draws ascending IDs: the highest visible stem wins direct overlap.
        for(item in batch.items.asReversed()) {
            if(item.material!=Material.STRAW || item.route!=Route.INTAKE) continue
            val c=cos(item.angle); val s=sin(item.angle)
            for(f in item.strands) {
                val fc=cos(item.angle+f.angle); val fs=sin(item.angle+f.angle)
                val ox=item.x+c*f.x-s*f.y; val oy=item.y+s*f.x+c*f.y
                for(k in 0 until 12) {
                    val u=k/12f-.5f; val v=(k+1)/12f-.5f
                    val ax=ox+fc*u*f.length-fs*f.bend*(1-4*u*u)
                    val ay=oy+fs*u*f.length+fc*f.bend*(1-4*u*u)
                    val bx=ox+fc*v*f.length-fs*f.bend*(1-4*v*v)
                    val by=oy+fs*v*f.length+fc*f.bend*(1-4*v*v)
                    val dx=bx-ax; val dy=by-ay
                    val t=((x-ax)*dx+(y-ay)*dy).div(dx*dx+dy*dy).coerceIn(0f,1f)
                    val px=ax+t*dx; val py=ay+t*dy; val distance=hypot(x-px,y-py)
                    if(distance<=f.width/2+.025f) return Hit(item.id,px,py,distance)
                    if(distance<=tolerance && (best==null || distance<best.distance)) best=Hit(item.id,px,py,distance)
                }
            }
        }
        return best
    }
}

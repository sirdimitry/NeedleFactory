package com.needlefactory.content

import com.needlefactory.simulation.*
import java.util.Random
import kotlin.math.*

/** Ten authored crossing families. Mirrors change both bend and local geometry. */
object StrawClumps {
    data class Stem(val x:Float,val y:Float,val angle:Float,val length:Float,val bend:Float)
    val templates:List<List<Stem>> = (0 until 10).map { t ->
        val count=3+t%3
        (0 until count).map { i ->
            // One near-straight long shaft conceals metal; remaining stalks cross asymmetrically.
            // Stagger crossings along the shaft: no common pinched bow-tie centre.
            Stem(if(i==0) 0f else ((i*7+t*3)%9-4)*.09f,
                if(i==0) 0f else ((i*3+t*5)%7-3)*.045f,
                if(i==0) 0f else (if((i+t)%2==0) 1f else -1f)*(.07f+((i*5+t)%7)*.027f),
                2.15f+((i*7+t*3)%9)*.085f,
                if(i==0) .004f else ((i*3+t)%7-3)*.024f)
        }
    }
    fun generate(seed:Long,c:FactoryConfig):FactoryBatch {
        val r=Random(seed)
        val items=ArrayList<FactoryItem>(c.strawCount+c.needleCount)
        for(id in 0 until c.strawCount) {
            val template=templates[r.nextInt(templates.size)]
            val mx=if(r.nextBoolean()) 1f else -1f; val my=if(r.nextBoolean()) 1f else -1f
            val lengthScale=.95f+r.nextFloat()*.1f
            val primary=template.map { f ->
                StrawStrand(f.x*mx,f.y*my,atan2(sin(f.angle)*my,cos(f.angle)*mx),
                    f.length*lengthScale,.025f+r.nextFloat()*.006f,f.bend*mx*my,r.nextInt(4))
            }
            val stems=primary.toMutableList()
            repeat(c.clumpDensity-1) {
                val extra=templates[r.nextInt(templates.size)]
                val offsetX=(r.nextFloat()-.5f)*.75f; val offsetY=(r.nextFloat()-.5f)*.75f
                val turn=(r.nextFloat()-.5f)*.9f
                for(f in extra) stems.add(StrawStrand(f.x+offsetX,f.y+offsetY,f.angle+turn,
                    f.length*(.85f+r.nextFloat()*.23f),.023f+r.nextFloat()*.009f,f.bend,r.nextInt(4)))
            }
            val item=FactoryItem(id,Material.STRAW,stems.maxOf { it.length },.03f,0f,r.nextInt(4),
                2.1f+r.nextFloat()*2.7f,1.65f+r.nextFloat()*(c.height-3.3f),
                (r.nextFloat()-.5f)*PI.toFloat(),stems,primary.size)
            val ex=item.extent(cos(item.angle),sin(item.angle),true)
            val ey=item.extent(cos(item.angle),sin(item.angle),false)
            item.x=item.x.coerceIn(ex+.15f,c.width-ex-.15f)
            item.y=item.y.coerceIn(ey+.15f,c.height-ey-.15f)
            if(c.pitReceiver) {
                // A real upper-left source pile with empty floor separating it from the pit.
                item.x=(1.3f+r.nextFloat()*3.7f).coerceIn(ex+.2f,6.35f-ex)
                item.y=(3.6f+(c.height-8f)+r.nextFloat()*3.2f).coerceIn(2.7f+ey,c.height-ey-.2f)
            }
            item.snapshot(); items.add(item)
        }
        // Independent needles under different REAL movable cover stems, never synthetic cover.
        for(n in 0 until c.needleCount) {
            val cover=items[c.strawCount-1-n]
            val f=cover.strands.first(); val a=cover.angle+f.angle
            val x=cover.x+cos(cover.angle)*f.x-sin(cover.angle)*f.y-sin(a)*f.bend
            val y=cover.y+sin(cover.angle)*f.x+cos(cover.angle)*f.y+cos(a)*f.bend
            items.add(FactoryItem(c.strawCount+n,Material.NEEDLE,c.needleLength,c.needleWidth,
                0f,0,x,y,a))
        }
        return FactoryBatch(seed,items)
    }
}

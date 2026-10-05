package com.needlefactory.presentation

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

/** Two authored compositions with shared machine ports and fixed-size control targets. */
class FactoryLayout(val width: Float, val height: Float) {
    val landscape = width >= height
    val unit = 1f
    private val moduleOffset = if(landscape) ((width-814f)/2).coerceAtLeast(0f) else 0f
    val console = if(landscape) Rectangle(width-200f,16f,184f,height-32f)
        else Rectangle(0f,0f,width,164f)
    val table = if(landscape) Rectangle(30f+moduleOffset,height-214.4f,248f,198.4f)
        else Rectangle(30f,height-380f,248f,198.4f)
    // A full-height receiving apron connects the entire physical feed edge.
    val start = Vector2(table.x+table.width+48f*unit,table.y+table.height/2)
    val end = if (landscape) Vector2(minOf(width,814f)-274f+moduleOffset,start.y) else Vector2(start.x,230f)
    val magnet = Vector2(start).lerp(end,(.55f-HANDOFF)/(DISCHARGE-HANDOFF))
    val capture = if(landscape) Vector2(magnet.x,150f) else Vector2(magnet.x-93f,magnet.y-4f)
    val buffer = if(landscape) Vector2(end.x-24f,80f) else Vector2(183f,312f)
    val packer = if(landscape) Vector2(buffer.x-116f,80f) else Vector2(183f,237f)
    val output = if(landscape) Vector2(packer.x-87f,80f) else Vector2(68f,237f)
    val residue = if(landscape) Vector2(end.x,height-46f) else Vector2(end.x,202f)
    val missed = if(landscape) Vector2(end.x-70f,height-46f) else Vector2(end.x-69f,202f)
    val floor = if(landscape) Rectangle(14f,29f,width-232f,height-37f)
        else Rectangle(14f,172f,width-28f,height-296f)
    val run = if(landscape) Rectangle(console.x+10f,console.y+72f,164f,52f) else Rectangle(16f,84f,132f,52f)
    val next = if(landscape) Rectangle(console.x+10f,console.y+8f,164f,52f) else Rectangle(160f,84f,width-176f,52f)
    val controls = Array(4) { i ->
        if(landscape) {
            Rectangle(console.x+10f+(i%2)*86f,console.y+128f+(1-i/2)*68f,78f,60f)
        } else Rectangle(16f+i*(width-32f)/4,12f,(width-32f)/4-6f,60f)
    }
    val telemetry = Rectangle(width-44f,height-40f,28f,28f)
    val statusX = if(landscape) console.x+10f else 29f
    val statusY = if(landscape) console.y+144f else 158f
    fun belt(t: Float,result: Vector2): Vector2 = result.set(start).lerp(end,
        ((t-HANDOFF)/(DISCHARGE-HANDOFF)).coerceIn(0f,1f))
    companion object {
        const val HANDOFF = .12f
        const val DISCHARGE = .90f
    }
}

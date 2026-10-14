package com.needlefactory.economy

import kotlin.math.*

enum class EquipmentKind(val title:String,val price:Long) {
    ROBOT("Роботизированная рука",200), FAN("Вентилятор",500), DIVIDER("Перемычка",40)
}
/** Owned world-space fixtures. Presentation/rotation never owns their economy. */
data class FieldEquipment(val id:Int,val kind:EquipmentKind,val x:Float,val y:Float,
                          val degrees:Int=270,val speed:Int=1,val width:Int=1,val range:Int=1,
                          val invested:Long=kind.price,val anchored:Boolean=false) {
    val dividerLength:Float get()=1.3f+(range.coerceIn(1,29)-1)*.3f
    val coneDegrees:Float get()=18f+(width.coerceIn(1,10)-1)*8f
    val reach:Float get()=3f+(range-1)*.45f
    /** Straw power grows 4x per tier through V (256x base), then plateaus safely. */
    val power:Float get()=28f*4f.pow((speed-1).coerceIn(0,4))
    val needlePower:Float get()=(1.5f+(speed-1).coerceAtLeast(0)*.10f).coerceAtMost(2.5f)
    fun price(axis:String):Long {
        val n=when(axis) { "width"->width; "range"->range; else->speed }
        val base=when(kind) {EquipmentKind.ROBOT->200.0;EquipmentKind.DIVIDER->40.0;else->500.0}
        return (if(kind!=EquipmentKind.FAN) base*1.5.pow(min(n,4)) * 2.0.pow(max(0,n-4))
            else (base+250*min(n,4))*2.0.pow(max(0,n-4))).coerceAtMost(1e12).toLong()
    }
    fun hit(px:Float,py:Float,padded:Boolean=false):Boolean {
        val dx=px-x;val dy=py-y;val a=degrees*PI/180
        return if(kind==EquipmentKind.DIVIDER) abs(dx*cos(a)+dy*sin(a))<=dividerLength/2+(if(padded) .2 else 0.0) &&
            abs(-dx*sin(a)+dy*cos(a))<(if(padded) .4 else .13)
        else hypot(dx,dy)<.7f
    }
    fun containsWind(px:Float,py:Float):Boolean {
        val dx=px-x; val dy=py-y; val distance=hypot(dx,dy)
        if(distance<.2f || distance>reach) return false
        val a=degrees*PI/180; return (dx*cos(a)+dy*sin(a))/distance>=cos(coneDegrees*PI/360)
    }
    companion object {
        fun angle(x:Float,y:Float)=((atan2(y,x)*180/PI).roundToInt()%360+360)%360
    }
}

/** Actual visible geometry wins over another fixture's accessibility padding. */
object FieldEquipmentSelection {
    fun find(items:List<FieldEquipment>,x:Float,y:Float):FieldEquipment? {
        val top=items.asReversed()
        return top.firstOrNull {it.hit(x,y)} ?: top.firstOrNull {it.hit(x,y,true)}
    }
}

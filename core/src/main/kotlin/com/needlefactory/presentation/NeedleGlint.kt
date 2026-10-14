package com.needlefactory.presentation

/** Archive-inspired late discovery accent. No repeating clock, glow, or gameplay hint. */
object NeedleGlint {
    fun gain(visibility:Float,tiltStrength:Float):Float {
        if(!visibility.isFinite() || !tiltStrength.isFinite() || visibility<=.75f) return 0f
        val t=((visibility-.75f)/.25f).coerceIn(0f,1f)
        return if(visibility>=.999f) 1f else t*t*(3-2*t)*(.48f+.32f*tiltStrength.coerceIn(0f,1f))
    }
}

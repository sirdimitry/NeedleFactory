package com.needlefactory.presentation

enum class AudioChannel(val title:String) {
    MASTER("Общая громкость"), EFFECTS("Все эффекты"), AMBIENT("Эмбиент"),
    AUGER("Шреддер"), FAN("Вентиляторы"), STRAW("Солома"), ROBOT("Рука и инструменты"),
    METAL("Иглы"), UI("Интерфейс"), REWARDS("Деньги и награды")
}

/** Independent mixer values; zero mutes without losing other channels' settings. */
class AudioLevels(values:Map<AudioChannel,Int> = emptyMap(), legacyEffects:Boolean=true, legacyAmbient:Boolean=true) {
    private val levels=IntArray(AudioChannel.entries.size) { i ->
        values[AudioChannel.entries[i]]?.coerceIn(0,100) ?: when(AudioChannel.entries[i]) {
            AudioChannel.EFFECTS->if(legacyEffects) 100 else 0
            AudioChannel.AMBIENT->if(legacyAmbient) 100 else 0
            else->100
        }
    }
    operator fun get(c:AudioChannel)=levels[c.ordinal]
    fun set(c:AudioChannel,percent:Int) {levels[c.ordinal]=percent.coerceIn(0,100)}
    fun gain(c:AudioChannel):Float {
        val master=levels[AudioChannel.MASTER.ordinal]/100f
        return when(c) {
            AudioChannel.MASTER->master
            AudioChannel.EFFECTS,AudioChannel.AMBIENT->master*levels[c.ordinal]/100f
            else->master*levels[AudioChannel.EFFECTS.ordinal]/100f*levels[c.ordinal]/100f
        }
    }
    companion object {
        fun channel(c:AudioCue)=when(c) {
            AudioCue.METAL,AudioCue.LOST->AudioChannel.METAL
            AudioCue.STRAW->AudioChannel.STRAW
            AudioCue.GRIP,AudioCue.RELEASE->AudioChannel.ROBOT
            AudioCue.CREDIT,AudioCue.PURCHASE,AudioCue.RESULT,AudioCue.PERFECT->AudioChannel.REWARDS
            else->AudioChannel.UI
        }
    }
}

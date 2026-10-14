package com.needlefactory.presentation

import com.needlefactory.economy.*

/** Presentation labels only. Save IDs, prices and simulation diagnostics remain stable. */
internal object RussianUi {
    const val GAME_TITLE="Стог и сталь"
    /** Truncate, never round up a price; exact authoritative Long remains unchanged. */
    fun compact(value:Long):String {
        val magnitude=if(value==Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(value)
        val (unit,suffix)=when {
            magnitude>=1_000_000_000_000L->1_000_000_000_000L to "т"
            magnitude>=1_000_000_000L->1_000_000_000L to "млрд"
            magnitude>=1_000_000L->1_000_000L to "м"
            magnitude>=1_000L->1_000L to "к"
            else->return value.toString()
        }
        val whole=value/unit;val tenth=kotlin.math.abs(value%unit)/(unit/10)
        return "$whole"+(if(magnitude/unit<100 && tenth>0) ",$tenth" else "")+suffix
    }
    fun credits(value:Long):String {
        val hundred=value%100; val ten=value%10
        val suffix=when {
            hundred in 11L..14L -> "кредитов"
            ten==1L -> "кредит"
            ten in 2L..4L -> "кредита"
            else -> "кредитов"
        }
        return "${compact(value)} $suffix"
    }
    fun tool(t:HandTool)=when(t) {
        HandTool.HAND->"Рука"; HandTool.RAKE->"Рыхлитель"; HandTool.STEEL_RAKE->"Грабли"
    }
    fun upgrade(u:UpgradeId)=when(u) {
        UpgradeId.RAKE->tool(HandTool.RAKE); UpgradeId.GUIDES->"Направляющие"; UpgradeId.BALER->"Пресс-тюковщик"
        UpgradeId.CONVEYOR->"Короткий конвейер"; UpgradeId.MAGNET->"Магнитный сепаратор"; UpgradeId.STEEL_RAKE->tool(HandTool.STEEL_RAKE)
        UpgradeId.METAL_RECEIVER->"Приёмник игл"
    }
    fun warning(w:String?):String=when {
        w==null->""
        w.startsWith("Save v")->"Сохранение обновлено. Деньги и покупки на месте."
        w.startsWith("Recovered previous")->"Восстановлена резервная копия."
        w.startsWith("Save damaged")->"Сохранение повреждено. Новый профиль."
        w.startsWith("Newer save")->"Новая версия сохранения. Изменения запрещены."
        w.startsWith("Cannot read")->"Сохранение недоступно. Изменения запрещены."
        else->"Проверьте сохранение мастерской."
    }
}

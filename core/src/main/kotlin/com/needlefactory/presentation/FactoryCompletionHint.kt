package com.needlefactory.presentation

/** Economic dispositions, not one arbitrarily chosen needle's rendering route. */
internal object FactoryCompletionHint {
    fun text(secured:Int,missed:Int,discarded:Int):String = when {
        missed>0 -> "Пропущено игл: $missed. Проверьте следующую партию."
        discarded>0 -> "Раннее завершение. Выручка сохранена."
        secured>0 -> "Иглы сохранены. Можно начать новую партию."
        else -> "Партия завершена. Можно начать новую."
    }
}

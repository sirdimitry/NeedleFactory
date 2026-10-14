package com.needlefactory.presentation

import org.junit.Assert.*
import org.junit.Test

class FactoryCompletionHintTest {
    @Test fun `early finish and restored receipt never claim a zero missed needle`() {
        assertEquals("Раннее завершение. Выручка сохранена.",FactoryCompletionHint.text(1,0,225))
        assertEquals("Раннее завершение. Выручка сохранена.",FactoryCompletionHint.text(3,0,180))
        assertFalse(FactoryCompletionHint.text(1,0,225).contains("Пропущено"))
    }
    @Test fun `complete and genuinely missed batches have distinct hints`() {
        assertTrue(FactoryCompletionHint.text(3,0,0).startsWith("Иглы сохранены"))
        assertTrue(FactoryCompletionHint.text(2,1,0).startsWith("Пропущено игл: 1"))
        assertFalse(FactoryCompletionHint.text(2,1,0).contains("Включите магнит"))
    }
}

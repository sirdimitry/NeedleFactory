package com.needlefactory.presentation

import org.junit.Assert.*
import org.junit.Test

class RussianFontTest {
    @Test fun `compact values stay short truncate without changing their units`() {
        assertEquals("999",RussianUi.compact(999))
        assertEquals("10к",RussianUi.compact(10_000))
        assertEquals("100к",RussianUi.compact(100_000))
        assertEquals("1м",RussianUi.compact(1_000_000))
        assertEquals("1,5к",RussianUi.compact(1_599))
        assertEquals("999к",RussianUi.compact(999_999))
        assertEquals("1млрд",RussianUi.compact(1_000_000_000))
        assertEquals("1т",RussianUi.compact(1_000_000_000_000))
        assertTrue(RussianUi.compact(Long.MIN_VALUE).startsWith("-"))
    }
    @Test fun `tool names agree between shop confirmation and active hand`() {
        assertEquals("Рыхлитель",RussianUi.upgrade(com.needlefactory.economy.UpgradeId.RAKE))
        assertEquals(RussianUi.tool(com.needlefactory.economy.HandTool.RAKE),RussianUi.upgrade(com.needlefactory.economy.UpgradeId.RAKE))
        assertEquals("Грабли",RussianUi.upgrade(com.needlefactory.economy.UpgradeId.STEEL_RAKE))
    }
    @Test fun `credit labels handle one few many and teen endings`() {
        assertEquals("1 кредит",RussianUi.credits(1))
        assertEquals("24 кредита",RussianUi.credits(24))
        assertEquals("11 кредитов",RussianUi.credits(11))
        assertEquals("112 кредитов",RussianUi.credits(112))
        assertEquals("0 кредитов",RussianUi.credits(0))
    }
    @Test fun `shipped bitmap atlas covers Russian UI and numeric labels`() {
        val descriptor=javaClass.getResourceAsStream("/ui/needle-panel-ru.fnt")!!.bufferedReader().use { it.readText() }
        val ids=Regex("char id=(\\d+)").findAll(descriptor).map { it.groupValues[1].toInt() }.toSet()
        for(c in "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя0123456789%+/") {
            assertTrue("Missing glyph: $c",c.code in ids)
        }
        assertNotNull(javaClass.getResource("/ui/needle-panel-ru.png"))
        assertNotNull(javaClass.getResource("/ui/DejaVu-LICENSE.txt"))
    }
}

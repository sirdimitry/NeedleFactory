package com.needlefactory.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootStatusTest {
    @Test fun `input is ignored while paused and works after resume`() {
        val status = BootStatus()
        status.press()
        status.pause()
        status.press()
        assertTrue(status.paused)
        assertEquals(1, status.presses)
        status.resume()
        status.press()
        assertFalse(status.paused)
        assertEquals(2, status.presses)
        assertEquals(1, status.pauses)
        assertEquals(1, status.resumes)
    }

    @Test fun `duplicate lifecycle notifications do not invent transitions`() {
        val status = BootStatus()
        status.resume()
        status.pause()
        status.pause()
        status.resume()
        status.resume()
        assertEquals(1, status.pauses)
        assertEquals(1, status.resumes)
    }
}

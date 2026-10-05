package com.needlefactory.presentation

import com.needlefactory.game.SafeArea
import org.junit.Assert.*
import org.junit.Test

class FactoryCanvasTest {
    @Test fun `Samsung rotation keeps physical field needle straw and machine scale identical`() {
        val p=FactoryCanvas.forWindow(1080,2340,SafeArea(top=96,bottom=45))
        for(safe in listOf(SafeArea(left=96,right=45),SafeArea(top=96,bottom=45),SafeArea(right=96,bottom=45))) {
            val w=FactoryCanvas.forWindow(2340,1080,safe)
            val portrait=FactoryLayout(p.width,p.height); val wide=FactoryLayout(w.width,w.height)
            assertEquals(p.unitsPerPixel,w.unitsPerPixel,.00001f)
            assertEquals(portrait.table.width/p.unitsPerPixel,wide.table.width/w.unitsPerPixel,.001f)
            assertEquals(portrait.table.height/p.unitsPerPixel,wide.table.height/w.unitsPerPixel,.001f)
            val ps=portrait.table.width/10/p.unitsPerPixel; val ws=wide.table.width/10/w.unitsPerPixel
            assertEquals(1.3f*ps,1.3f*ws,.001f)
            assertEquals(.025f*ps,.025f*ws,.001f)
            assertEquals(portrait.unit/p.unitsPerPixel,wide.unit/w.unitsPerPixel,.001f)
            assertTrue(wide.floor.contains(wide.table))
            assertTrue(wide.console.contains(wide.controls[0]))
            assertTrue(wide.capture.dst(wide.magnet)>40f)
            for(center in listOf(wide.capture,wide.buffer,wide.packer,wide.output,wide.residue,wide.missed))
                assertTrue(wide.floor.contains(center))
        }
    }
    @Test fun `compact aspect ratios calibrate the same device baseline before orientation layout`() {
        for((short,long) in listOf(1080 to 1920,600 to 900,720 to 1600,1080 to 2400)) {
            val p=FactoryCanvas.forWindow(short,long,SafeArea())
            val w=FactoryCanvas.forWindow(long,short,SafeArea())
            assertEquals(p.unitsPerPixel,w.unitsPerPixel,0f)
            assertTrue(w.width>=813.99f)
            val pl=FactoryLayout(p.width,p.height); val wl=FactoryLayout(w.width,w.height)
            assertEquals(pl.table.width/p.unitsPerPixel,wl.table.width/w.unitsPerPixel,.001f)
            assertTrue(wl.floor.contains(wl.table))
            assertTrue(wl.end.x>wl.start.x+100f)
            assertFalse(wl.console.overlaps(wl.floor))
        }
    }
    @Test fun `safe areas reserve coordinates without shrinking Samsung modules`() {
        val reference=FactoryCanvas.forWindow(1080,2340,SafeArea())
        val safe=FactoryCanvas.forWindow(1080,2340,SafeArea(top=96,bottom=45))
        assertEquals(reference.unitsPerPixel,safe.unitsPerPixel,0f)
        assertEquals(reference.width,safe.width,0f)
        assertEquals(reference.height-141*reference.unitsPerPixel,safe.height,.0001f)
    }
    @Test fun `compact device calibration does not depend on which axis carries bars`() {
        val p=FactoryCanvas.forWindow(600,900,SafeArea(top=96,bottom=45))
        for(safe in listOf(SafeArea(top=96,bottom=45),SafeArea(left=96,right=45))) {
            val w=FactoryCanvas.forWindow(900,600,safe)
            assertEquals(p.unitsPerPixel,w.unitsPerPixel,0f)
            assertTrue(w.width>=813.99f)
            assertEquals(FactoryLayout(p.width,p.height).table.width/p.unitsPerPixel,
                FactoryLayout(w.width,w.height).table.width/w.unitsPerPixel,.001f)
        }
    }
}

package com.needlefactory.presentation

import com.badlogic.gdx.math.Vector2
import org.junit.Assert.*
import org.junit.Test

class FactoryLayoutTest {
    @Test fun `authored phone layouts keep controls and machinery separate`() {
        for((w,h) in listOf(400f to 800f,400f to 867f,400f to 960f,978f to 480f,1060f to 480f,1200f to 480f,1360f to 480f,900f to 600f)) {
            val l=FactoryLayout(w,h)
            assertTrue(l.floor.contains(l.table))
            for(r in l.controls.toList()+listOf(l.run,l.next)) {
                assertTrue(r.width>=54f && r.height>=48f)
                assertTrue(r.x>=0 && r.x+r.width<=w)
                assertFalse("Control overlaps material table $w x $h",r.overlaps(l.table))
            }
            val headerBottom=h-if(l.landscape) 76 else 122
            assertTrue(l.capture.y+40*l.unit<headerBottom)
            assertTrue(l.packer.y+40*l.unit<headerBottom)
            if(!l.landscape) assertTrue("magnet hidden by intake",l.magnet.x-37>l.table.x+l.table.width)
            assertTrue(l.packer.y-61*l.unit>=l.floor.y-13)
            assertTrue(l.start.x>l.table.x+l.table.width)
            if(l.landscape) {
                assertTrue(l.controls.all { it.width==78f && it.height==60f })
                assertTrue(l.run.width<=164f && l.next.width<=164f)
                assertTrue(l.controls.all { l.console.contains(it) })
                assertFalse(l.console.overlaps(l.floor))
                assertTrue(l.packer.y<l.table.y-40f)
                assertTrue(l.end.x-l.start.x<w*.5f)
            }
        }
    }
    @Test fun `attachment coordinates make the capture and feed handoffs continuous`() {
        for(l in listOf(FactoryLayout(400f,867f),FactoryLayout(1060f,480f))) {
            val p=Vector2()
            assertEquals(l.start,l.belt(FactoryLayout.HANDOFF,p))
            assertEquals(l.magnet,l.belt(.55f,p))
            assertEquals(l.end,l.belt(FactoryLayout.DISCHARGE,p))
            assertTrue(l.magnet.dst(l.capture)>40*l.unit)
            assertTrue(l.residue.dst(l.missed)>40*l.unit)
        }
    }
}

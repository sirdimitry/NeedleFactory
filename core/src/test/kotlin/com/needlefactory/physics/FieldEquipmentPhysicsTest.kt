package com.needlefactory.physics

import com.badlogic.gdx.utils.GdxNativesLoader
import com.needlefactory.content.FactoryConfig
import com.needlefactory.economy.*
import com.needlefactory.simulation.*
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import kotlin.math.*

class FieldEquipmentPhysicsTest {
    companion object { @JvmStatic @BeforeClass fun natives()=GdxNativesLoader.load() }
    @Test fun `tier five fan moves straw strongly but steel remains slow and finite`() {
        fun distance(material:Material,tier:Int):Float {
            val s=FactorySession(FactoryConfig.pitProducts(),42)
            val item=s.batch.items.first {it.material==material}
            item.x=5f;item.y=-3f;item.angle=0f;item.snapshot()
            s.equipment=listOf(FieldEquipment(1,EquipmentKind.FAN,4f,-3f,0,speed=tier,anchored=true))
            FactoryIntakePhysics(s).use {p->repeat(60) {s.step();p.step()}}
            assertTrue(item.x.isFinite());assertTrue(abs(item.vx)<=7.01f)
            assertEquals(0,s.collectedStraw)
            return item.x-5f
        }
        val base=distance(Material.STRAW,1);val strong=distance(Material.STRAW,5)
        val steel=distance(Material.NEEDLE,5)
        assertTrue("base=$base strong=$strong",strong>base+1f)
        assertTrue("straw=$strong steel=$steel",strong>steel*5)
        assertTrue("steel=$steel",steel<.6f)
        val fan=FieldEquipment(1,EquipmentKind.FAN,0f,0f,speed=5)
        assertEquals(256f,fan.power/fan.copy(speed=1).power,.001f)
    }
    @Test fun `retiring pre-load native world tolerates saved larger manifest`() {
        val s=FactorySession(FactoryConfig.pitProducts(),42)
        FactoryIntakePhysics(s).use {p ->
            s.configureLot(30);s.restore(42,1);p.end()
            assertEquals(33,s.batch.items.size)
        }
        FactoryIntakePhysics(s).use {p -> repeat(120) {s.step();p.step()};assertEquals(33,s.batch.count(Route.INTAKE)) }
    }
    @Test fun `fan is local purchased power that stops after sale and never creates product remotely`() {
        val s=FactorySession(FactoryConfig.pitProducts(),42)
        val straw=s.batch.items[0];straw.x=8f;straw.y=-3f;straw.snapshot()
        s.equipment=listOf(FieldEquipment(1,EquipmentKind.FAN,7f,-3f,0,anchored=true))
        FactoryIntakePhysics(s).use {p->
            repeat(120) {s.step();p.step()};assertTrue(straw.x>8.2f);assertEquals(0,s.collectedStraw)
            s.equipment=emptyList();repeat(120) {s.step();p.step()};val x=straw.x
            repeat(300) {s.step();p.step()};assertEquals(x,straw.x,.01f);assertEquals(27,s.batch.count(Route.INTAKE))
        }
    }
    @Test fun `loose fan is off anchored fan blows and unlocking removes physical power`() {
        val s=FactorySession(FactoryConfig.pitProducts(),42)
        val straw=s.batch.items[0];straw.x=8f;straw.y=-3f;straw.snapshot()
        val fan=FieldEquipment(1,EquipmentKind.FAN,7f,-3f,0)
        s.equipment=listOf(fan)
        FactoryIntakePhysics(s).use {p->
            repeat(120) {s.step();p.step()}
            assertEquals(8f,straw.x,.01f);assertEquals(0,s.collectedStraw)
            s.equipment=listOf(fan.copy(anchored=true))
            repeat(120) {s.step();p.step()};assertTrue(straw.x>8.2f)
            s.equipment=listOf(fan)
            repeat(180) {s.step();p.step()};val resting=straw.x
            repeat(180) {s.step();p.step()};assertEquals(resting,straw.x,.01f)
            assertEquals(0,s.collectedStraw)
        }
    }
    @Test fun `robot must travel and carry physical straw not award a remote teleport`() {
        val s=FactorySession(FactoryConfig.pitProducts(),42)
        s.equipment=listOf(FieldEquipment(1,EquipmentKind.ROBOT,7.5f,-2f))
        FactoryIntakePhysics(s).use {p->
            repeat(60) {s.step();p.step()};assertEquals(0,s.collectedStraw)
            repeat(2200) {s.step();p.step()};assertTrue(s.collectedStraw>0)
            s.equipment=emptyList();repeat(200) {s.step();p.step()};val count=s.collectedStraw
            repeat(900) {s.step();p.step()};assertEquals(count,s.collectedStraw)
        }
    }
    @Test fun `robot closes pinch before travelling and opens before returning`() {
        val s=FactorySession(FactoryConfig.pitProducts(),42)
        s.equipment=listOf(FieldEquipment(1,EquipmentKind.ROBOT,7.5f,-2f))
        FactoryIntakePhysics(s).use {p->
            var steps=0
            while(!p.robotCarrying && steps++<1500) {s.step();p.step()}
            assertTrue(p.robotCarrying);assertTrue(p.robotGripProgress<1f)
            val x=p.robotX;val y=p.robotY
            repeat(5) {s.step();p.step()}
            assertEquals(x,p.robotX,.001f);assertEquals(y,p.robotY,.001f)
            steps=0
            while(p.robotCarrying && steps++<2000) {s.step();p.step()}
            assertFalse(p.robotCarrying)
            val rx=p.robotX;val ry=p.robotY
            val closing=p.robotGripProgress
            repeat(5) {s.step();p.step()}
            assertEquals(rx,p.robotX,.001f);assertEquals(ry,p.robotY,.001f)
            assertTrue(p.robotGripProgress<closing)
        }
    }
    @Test fun `divider blocks held material and sale removes its physical fixture`() {
        val s=FactorySession(FactoryConfig.pitProducts(),42)
        val straw=s.batch.items[0];straw.x=7f;straw.y=-4f;straw.angle=0f;straw.snapshot()
        s.equipment=listOf(FieldEquipment(1,EquipmentKind.DIVIDER,8.5f,-4f,90))
        FactoryIntakePhysics(s).use {p->
            p.begin(7f,-4f);assertEquals(0,p.grabbedId)
            repeat(240) {i->p.target(7+min(i/120f,1f)*3,-4f);s.step();p.step()}
            assertTrue("Barrier ignored: ${straw.x}",straw.x<8.6f)
            s.equipment=emptyList();repeat(240) {p.target(10f,-4f);s.step();p.step()}
            assertTrue(straw.x>9f);p.end()
        }
    }
}

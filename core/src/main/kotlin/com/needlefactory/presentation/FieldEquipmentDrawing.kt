package com.needlefactory.presentation

import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.*
import com.needlefactory.economy.*
import com.needlefactory.physics.FactoryIntakePhysics
import com.needlefactory.simulation.FactorySession
import kotlin.math.*

internal class FieldEquipmentDrawing(shapes:ShapeRenderer,private val sprites:SpriteBatch) {
    private val d=FactoryDrawing(shapes);private val point=Vector2();private val end=Vector2()
    private class FanMotion(anchored:Boolean) {
        var drive=if(anchored) 1f else 0f
        var angle=0f;var windAge=0f
        fun step(dt:Float,on:Boolean,speed:Int) {
            val target=if(on) 1f else 0f
            drive+=(target-drive)*(1-exp(-dt*if(on) 10f else 7f))
            if(!on && drive<.005f) drive=0f
            angle=(angle+dt*(5f+speed*.35f)*drive)%(2*PI.toFloat())
            windAge=(windAge+dt*(.55f+speed*.035f)*drive)%1f
        }
    }
    private val fanMotion=mutableMapOf<Int,FanMotion>()
    private var motionEquipment:List<FieldEquipment>?=null
    private var lastFanTime=Float.NaN
    var selected=-1;var rotating=false
    var receiverRefund=0L
    val sell=Rectangle();val close=Rectangle()
    fun draw(l:FactoryLayout,s:FactorySession,physics:FactoryIntakePhysics) {
        val scale=l.materialScale
        val fanDt=if(lastFanTime.isNaN()) 0f else (s.batch.elapsed-lastFanTime).coerceIn(0f,.1f)
        lastFanTime=s.batch.elapsed
        if(motionEquipment!==s.equipment) {
            motionEquipment=s.equipment
            fanMotion.keys.removeAll {id->s.equipment.none {it.id==id && it.kind==EquipmentKind.FAN}}
        }
        for(e in s.equipment) {
            if(e.kind==EquipmentKind.ROBOT) l.yardPoint(physics.robotX,physics.robotY,point) else l.yardPoint(e.x,e.y,point)
            val x=point.x;val y=point.y;val a=e.degrees*PI.toFloat()/180+l.yardAngle
            when(e.kind) {
                EquipmentKind.DIVIDER->{
                    val dx=cos(a)*e.dividerLength*scale/2;val dy=sin(a)*e.dividerLength*scale/2
                    d.line(x-dx+3,y-dy-3,x+dx+3,y+dy-3,7f,FactoryPalette.floorEdge)
                    d.line(x-dx,y-dy,x+dx,y+dy,6f,FactoryPalette.front)
                    d.line(x-dx,y-dy+2,x+dx,y+dy+2,2f,FactoryPalette.brass)
                    for(i in -1..1) d.bolt(x+dx*i,y+dy*i,2f)
                }
                EquipmentKind.FAN->{
                    val ux=cos(a);val uy=sin(a);val vx=-uy;val vy=ux
                    val cx=x+ux*2;val cy=y+uy*2
                    fun wire(ax:Float,ay:Float,bx:Float,by:Float,width:Float,colour:com.badlogic.gdx.graphics.Color) =
                        d.line(cx+ux*ax+vx*ay,cy+uy*ax+vy*ay,cx+ux*bx+vx*by,cy+uy*bx+vy*by,width,colour)
                    d.shadow(x-19,y-13,38f,26f,5f)
                    // One continuous bent U-stand, only when locked; no folded remnants when loose.
                    if(e.anchored) {
                        for(side in -1..1 step 2) {
                            wire(0f,side*19f,12f,side*19f,2.4f,FactoryPalette.front)
                            wire(0f,side*19f,12f,side*19f,1.25f,FactoryPalette.steel)
                            for(i in 0..7) {
                                val t=i*PI.toFloat()/16;val z=(i+1)*PI.toFloat()/16
                                wire(12+3*sin(t),side*(16+3*cos(t)),12+3*sin(z),side*(16+3*cos(z)),1.25f,FactoryPalette.steel)
                            }
                            wire(11f,side*19f,14f,side*18f,2.6f,FactoryPalette.dark)
                        }
                        wire(15f,-16f,15f,16f,2.2f,FactoryPalette.front)
                        wire(15f,-16f,15f,16f,1.2f,FactoryPalette.steel)
                        wire(15f,-2f,15f,2f,2.8f,FactoryPalette.dark)
                    }
                    // Rear motor and shaft are mechanically joined to the guard hub.
                    wire(0f,0f,-13f,0f,4f,FactoryPalette.front)
                    wire(-8f,0f,-14f,0f,9f,FactoryPalette.body)
                    wire(-8f,-2f,-14f,-2f,1.1f,FactoryPalette.top)
                    for(i in 0..3) wire(-8f-i*1.5f,-2.7f,-8f-i*1.5f,2.7f,.5f,FactoryPalette.front)
                    // Carry handle is on the motor, not a floor foot.
                    wire(-15f,-3.5f,-18f,-3.5f,.75f,FactoryPalette.steel)
                    wire(-18f,-3.5f,-18f,3.5f,.75f,FactoryPalette.steel)
                    wire(-18f,3.5f,-15f,3.5f,.75f,FactoryPalette.steel)
                    val motion=fanMotion.getOrPut(e.id) {FanMotion(e.anchored)}
                    motion.step(fanDt,e.anchored,e.speed)
                    val turn=motion.angle
                    for(i in 0..2) {
                        val t=turn+i*2*PI.toFloat()/3
                        d.shapes.color=FactoryPalette.steel
                        d.shapes.triangle(cx,cy,cx+ux*cos(t)*5.2f+vx*sin(t)*15,cy+uy*cos(t)*5.2f+vy*sin(t)*15,
                            cx+ux*cos(t+.7f)*4.2f+vx*sin(t+.7f)*12,cy+uy*cos(t+.7f)*4.2f+vy*sin(t+.7f)*12)
                    }
                    // Broad elliptical double guard with curved horizontal wires like the reference.
                    for(i in 0..39) {
                        val t=i*PI.toFloat()/20;val z=(i+1)*PI.toFloat()/20
                        wire(cos(t)*7-1.8f,sin(t)*18,cos(z)*7-1.8f,sin(z)*18,.65f,FactoryPalette.front)
                        wire(cos(t)*7,sin(t)*18,cos(z)*7,sin(z)*18,1.0f,FactoryPalette.steel)
                    }
                    for(row in -4..4) {
                        val r=row/5f;val half=sqrt(1-r*r)*18
                        for(i in 0..7) {
                            val t=i/8f;val z=(i+1)/8f
                            wire(r*7+.8f*sin(t*PI.toFloat())*(1-abs(r)),(t*2-1)*half,
                                r*7+.8f*sin(z*PI.toFloat())*(1-abs(r)),(z*2-1)*half,.42f,FactoryPalette.steel)
                        }
                    }
                    for(v in -2..2) {
                        val side=v*5f;val depth=sqrt(1-side*side/324)*7
                        wire(-depth,side,depth,side,.45f,FactoryPalette.top)
                    }
                    // Two side pivot caps and central hub remain with the unmounted fan body.
                    for(side in -1..1 step 2) d.bolt(cx+vx*side*19,cy+vy*side*19,1.6f)
                    d.bolt(cx,cy,2f)
                    wire(8f,0f,10f,0f,.8f,FactoryPalette.brass)
                    for(i in 0..8) {
                        val t=((motion.windAge+i*.173f)%1f)
                        val distance=scale*(.65f+t*(e.reach-.65f))
                        val spread=(i%3-1)*e.coneDegrees*PI.toFloat()/360
                        val curve=sin(t*PI.toFloat())*1.3f
                        if(!l.yard.contains(x+cos(a+spread)*(distance+8),y+sin(a+spread)*(distance+8))) continue
                        d.shapes.setColor(.77f,.88f,.85f,.23f*sin(t*PI.toFloat())*motion.drive)
                        d.shapes.rectLine(x+cos(a+spread)*distance-sin(a)*curve,y+sin(a+spread)*distance+cos(a)*curve,
                            x+cos(a+spread)*(distance+8),y+sin(a+spread)*(distance+8),.8f)
                    }
                }
                EquipmentKind.ROBOT->Unit
            }
            if(e.kind==EquipmentKind.FAN) for(i in 0 until minOf(e.speed,5))
                d.line(x-12+i*5,y-20,x-9+i*5,y-20,1.7f,FactoryPalette.brass)
            if(e.id==selected) {
                d.shapes.setColor(.9f,.77f,.4f,.65f)
                for(i in 0..35) { val a0=i*PI.toFloat()/18;val a1=a0+.07f
                    d.shapes.rectLine(x+cos(a0)*25,y+sin(a0)*25,x+cos(a1)*25,y+sin(a1)*25,1f) }
                if(rotating) {d.line(x,y,x+cos(a)*34,y+sin(a)*34,1f,FactoryPalette.green);d.bolt(x,y,3f)}
            }
        }
        if((selected==-2 && s.metalReceiverOwned) || (selected>=0 && s.equipment.any { it.id==selected })) {
            val x=if(l.landscape) l.floor.x+l.floor.width-244 else l.floor.x+90
            val y=if(l.landscape) l.floor.y+l.floor.height-42 else l.floor.y+8
            sell.set(x,y,144f,34f);close.set(x+150,y,66f,34f)
            d.round(sell.x,sell.y,sell.width,sell.height,5f,FactoryPalette.panel)
            d.round(close.x,close.y,close.width,close.height,5f,FactoryPalette.panelEdge)
        } else {sell.set(0f,0f,0f,0f);close.set(0f,0f,0f,0f)}
    }
    // Owner illustration, recolored only; sprite deformation is presentation, never physics.
    private val glove=com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.classpath("equipment/glove-reference.png"))
        .also {it.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear,com.badlogic.gdx.graphics.Texture.TextureFilter.Linear)}
    private val glovePoint=Vector2()
    private val gloveVertices=FloatArray(32*32*20)
    fun dispose() = glove.dispose()
    fun hand(l:FactoryLayout,s:FactorySession,physics:FactoryIntakePhysics,dt:Float) {
        if(s.equipment.none {it.kind==EquipmentKind.ROBOT}) return
        val phase=physics.robotGripProgress
        val grip=phase*phase*(3-2*phase)
        l.yardPoint(physics.robotX,physics.robotY,point);glovePoint.set(point)
        // Flying glove is screen-facing. Only its physical grip follows the yard transform.
        val size=.062f
        var n=0
        // Soft localized bends move opposing pads, leaving palm/cuff silhouette intact.
        fun vertex(u:Float,v:Float) {
            val px=u*1280;val py=v*1280
            val index=exp(-((px-120)*(px-120)/(240f*240)+(py-640)*(py-640)/(160f*160)))
            val thumb=exp(-((px-380)*(px-380)/(230f*230)+(py-855)*(py-855)/(130f*130)))
            val x=(px+grip*(140*index-150*thumb)-245)*size
            val y=(750-py-grip*(110*index-110*thumb))*size
            gloveVertices[n++]=point.x+x
            gloveVertices[n++]=point.y+y
            gloveVertices[n++]=com.badlogic.gdx.graphics.Color.WHITE_FLOAT_BITS
            gloveVertices[n++]=u;gloveVertices[n++]=v
        }
        for(row in 0 until 32) for(col in 0 until 32) {
            val u=col/32f;val v=row/32f;val uu=(col+1)/32f;val vv=(row+1)/32f
            vertex(u,vv);vertex(u,v);vertex(uu,v);vertex(uu,vv)
        }
        d.shapes.end()
        sprites.projectionMatrix=d.shapes.projectionMatrix
        sprites.begin();sprites.draw(glove,gloveVertices,0,n);sprites.end()
        d.shapes.begin(ShapeRenderer.ShapeType.Filled)
    }

    fun text(l:FactoryLayout,s:FactorySession,font:BitmapFont) {
        val e=s.equipment.firstOrNull { it.id==selected }
        if(e==null && !(selected==-2 && s.metalReceiverOwned)) return
        sprites.begin();font.data.setScale(11f/48f);font.color=FactoryPalette.text
        font.draw(sprites,"Продать / ${e?.invested?.div(2) ?: receiverRefund} кр.",sell.x+8,sell.y+23)
        font.draw(sprites,"Скрыть",close.x+8,close.y+23)
        if(e==null) {sprites.end();return}
        if(e.kind==EquipmentKind.ROBOT) point.set(glovePoint) else l.yardPoint(e.x,e.y,point)
        if(rotating) {font.color=FactoryPalette.brass;font.draw(sprites,"${e.degrees}°",point.x+28,point.y+15)}
        else font.draw(sprites,if(e.kind==EquipmentKind.ROBOT) "Авто: уровень ${e.speed}" else if(e.kind==EquipmentKind.FAN && !e.anchored) "2 тапа: закрепить" else "Удержать: поворот",point.x-48,point.y+35)
        sprites.end()
    }
}

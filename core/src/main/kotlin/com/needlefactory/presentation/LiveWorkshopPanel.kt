package com.needlefactory.presentation

import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.*
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.badlogic.gdx.utils.viewport.Viewport
import com.needlefactory.economy.*

/** One vertically scrolling document, with independent expandable sections. */
internal class LiveWorkshopPanel(private val shapes:ShapeRenderer,private val sprites:SpriteBatch) {
    private val d=FactoryDrawing(shapes)
    var viewport:Viewport?=null
    private val box=Rectangle();private val clip=Rectangle();private val scissors=Rectangle();private val back=Rectangle()
    private val expanded=booleanArrayOf(true,false,false,false)
    private var scroll=0f;private var total=0f;private var startY=0f;private var startScroll=0f;private var dragged=false
    private var jumpTo=-1
    private data class IconButton(val action:String,val axis:String,val level:Int,val price:Long,val bounds:Rectangle,val enabled:Boolean=true)
    private data class Row(val action:String,val title:String,val detail:String,val footer:String,val bounds:Rectangle,val section:Int=-1,val buttons:List<IconButton> = emptyList(),val bold:Boolean=false)
    private var rows=emptyList<Row>()
    fun layout(l:FactoryLayout) {
        val w=minOf(if(l.landscape) 560f else 370f,l.width-24)
        val h=l.height-24
        box.set((l.width-w)/2,12f,w,h);clip.set(box.x+16,box.y+66,w-32,h-151)
        back.set(box.x+16,box.y+14,140f,42f)
    }
    fun prepare(w:Workshop) {
        val data=mutableListOf<Row>();var y=clip.y+clip.height+scroll
        fun row(action:String,title:String,detail:String="",footer:String="",section:Int=-1) {
            val h=if(section>=0) 48f else 76f
            y-=h;data.add(Row(action,title,detail,footer,Rectangle(clip.x,y,clip.width,h-5),section))
        }
        fun item(action:String,title:String,detail:String,price:Long,owned:Boolean=false,locked:Boolean=false) {
            row(action,title,detail,if(owned) "Установлено / выбрать" else if(locked) "Недоступно" else "${RussianUi.compact(price)} кр. / купить")
        }
        val categories=arrayOf("Инструменты","Материалы","Приёмник","Автоматизация")
        for(c in categories.indices) {
            row("section:$c","${if(expanded[c]) "-" else "+"}  ${categories[c]}",section=c)
            if(!expanded[c]) continue
            val ids=when(c) { 0->listOf(UpgradeId.RAKE,UpgradeId.STEEL_RAKE);2->listOf(UpgradeId.METAL_RECEIVER);else->emptyList() }
            for(id in ids) {
                val title=RussianUi.upgrade(id)
                if(c==0) {
                    val owned=id.key in w.profile.purchased
                    val tool=if(id==UpgradeId.RAKE) HandTool.RAKE else HandTool.STEEL_RAKE
                    val defs=ProgressionUpgrade.entries.filter {it.tool==tool}
                    val h=138f;y-=h;val r=Rectangle(clip.x,y,clip.width,h-5)
                    val buttons=defs.mapIndexed {i,def->
                        IconButton("level:${def.key}",if(def.key.endsWith("grip")) "grip" else "speed",
                            w.level(def.key),w.upgradePrice(def.key),
                            Rectangle(r.x+10+i*(r.width-20)/2,r.y+8,(r.width-20)/2-6,55f),owned)
                    }
                    data.add(Row("buy:${id.key}",title,
                        if(id==UpgradeId.RAKE) "2 пучка / скорость +20%" else "+5 пучков и ступеней скорости",
                        if(owned) if(w.tool==tool) "Выбран" else "Нажмите название, чтобы выбрать"
                        else "${RussianUi.compact(w.price(id))} кр. / купить инструмент",r,buttons=buttons,bold=true))
                    continue
                }
                item("buy:${id.key}",title,when(id) {UpgradeId.RAKE->"2 пучка / скорость +20%";UpgradeId.STEEL_RAKE->"+5 пучков и ступеней скорости";else->"Иглы отдельно / ${w.needlePrice} кр. / все 3: +${w.clearMetalBonus}"},w.price(id),id.key in w.profile.purchased)
            }
            for(def in ProgressionUpgrade.entries.filter { it.category==c && it.tool==null }) {
                val detail=when(def) {
                    ProgressionUpgrade.VALUE->"Пучок: ${w.profile.lotValue} -> ${w.profile.lotValue+1} кр. / сразу"
                    ProgressionUpgrade.COUNT->"+${w.level(def.key)+1} пучков в текущую кучу"
                    ProgressionUpgrade.RECEIVER_WIDTH->"[ ${w.level(def.key)+1} ]  +1 м / до 9,64 м"
                    ProgressionUpgrade.LUCK->"Ценные пучки x2 / шанс +2% / сразу"
                    else->"Уровень ${w.level(def.key)} -> ${w.level(def.key)+1} / сразу"
                }
                item("level:${def.key}",def.title,detail,w.upgradePrice(def.key),locked=def.tool!=null && def.tool.key !in w.profile.purchased)
            }
            if(c==3) {
                for(kind in EquipmentKind.entries) item("place:${kind.name}",kind.title,
                    when(kind) {EquipmentKind.ROBOT->"Одна рука / переносит по одному пучку";EquipmentKind.FAN->"Конус 18° / дальность 3 м";EquipmentKind.DIVIDER->"1,3 м / поворот 1° / физическая преграда"},kind.price,
                    locked=kind==EquipmentKind.ROBOT && w.profile.equipment.any { it.kind==kind })
                for(e in w.profile.equipment) {
                    val axes=when(e.kind) {EquipmentKind.DIVIDER->listOf("range");EquipmentKind.ROBOT->listOf("speed");else->listOf("speed","width","range")}
                    val h=if(axes.isEmpty()) 64f else 114f;y-=h
                    val r=Rectangle(clip.x,y,clip.width,h-5)
                    val buttons=axes.mapIndexed {i,axis->
                        val level=when(axis) {"width"->e.width;"range"->e.range;else->e.speed}
                        IconButton("equip:${e.id}:$axis",axis,level,if((axis=="width" && level>=10) || (e.kind==EquipmentKind.DIVIDER && level>=29)) 0 else e.price(axis),Rectangle(r.x+10+i*(r.width-20)/3,r.y+8,(r.width-20)/3-6,55f))
                    }
                    data.add(Row("focus:${e.id}","${e.kind.title} ${w.profile.equipment.filter {it.kind==e.kind}.indexOf(e)+1}",if(e.kind==EquipmentKind.FAN) if(e.anchored) "Работает / двойной тап: выключить" else "Выключен / двойной тап: включить" else if(e.kind==EquipmentKind.DIVIDER) "${"%.1f".format(e.dividerLength)} м / ${e.degrees}° / выбрать на поле" else "Щипок / перенос / отпускание", "",r,buttons=buttons,bold=true))
                }
            }
        }
        total=clip.y+clip.height+scroll-y
        val previous=scroll
        if(jumpTo>=0) {
            val header=data.first {it.section==jumpTo}
            scroll=clip.y+clip.height+previous-header.bounds.y-header.bounds.height-5
            jumpTo=-1
        }
        scroll=scroll.coerceIn(0f,(total-clip.height).coerceAtLeast(0f))
        if(scroll!=previous) for(row in data) {row.bounds.y+=scroll-previous;for(b in row.buttons) b.bounds.y+=scroll-previous}
        rows=data
    }
    fun automation() { expanded[3]=true;jumpTo=3 }
    fun down(p:Vector2) { startY=p.y;startScroll=scroll;dragged=false }
    fun drag(p:Vector2) { if(kotlin.math.abs(p.y-startY)>5f) dragged=true;if(dragged) scroll=(startScroll+p.y-startY).coerceIn(0f,(total-clip.height).coerceAtLeast(0f)) }
    fun up(p:Vector2):String? {
        if(dragged) return null
        if(back.contains(p)) return "close"
        if(!clip.contains(p)) return null
        val row=rows.firstOrNull { it.bounds.contains(p) } ?: return null
        if(row.section>=0) { expanded[row.section]=!expanded[row.section];return "" }
        return row.buttons.firstOrNull {it.bounds.contains(p)}?.let {if(it.enabled && it.price>0) it.action else ""} ?: row.action
    }
    private fun clipping():Boolean { viewport?.calculateScissors(sprites.transformMatrix,clip,scissors);return viewport!=null && ScissorStack.pushScissors(scissors) }
    fun shape(l:FactoryLayout) {
        layout(l)
        shapes.setColor(0f,0f,0f,.7f);shapes.rect(0f,0f,l.width,l.height)
        d.cabinet(box.x,box.y,box.width,box.height,4f,FactoryPalette.panel)
        d.line(box.x+16,box.y+box.height-74,box.x+box.width-16,box.y+box.height-74,1f,FactoryPalette.brass)
        shapes.flush();val clipped=clipping()
        for(row in rows) if(row.bounds.overlaps(clip)) {
            val r=row.bounds;d.round(r.x,r.y,r.width,r.height,5f,if(row.section>=0) FactoryPalette.panelEdge else FactoryPalette.background)
            for(b in row.buttons) {
                val v=b.bounds;d.round(v.x,v.y,v.width,v.height,5f,FactoryPalette.panelEdge)
                val x=v.x+14;val y=v.y+v.height-14
                when(b.axis) {
                    "grip"->{d.line(x,y+6,x,y-5,1.6f,FactoryPalette.brass);d.line(x-6,y-1,x+6,y-1,1.6f,FactoryPalette.brass);for(k in -1..1) d.line(x+k*6,y-1,x+k*6,y-7,1.6f,FactoryPalette.brass)}
                    "speed"->{d.line(x-5,y+5,x+1,y,1.6f,FactoryPalette.brass);d.line(x+1,y,x-5,y-5,1.6f,FactoryPalette.brass);d.line(x+1,y+5,x+7,y,1.6f,FactoryPalette.brass);d.line(x+7,y,x+1,y-5,1.6f,FactoryPalette.brass)}
                    "width"->{d.line(x-6,y+6,x,y-4,1.6f,FactoryPalette.brass);d.line(x,y-4,x+6,y+6,1.6f,FactoryPalette.brass)}
                    else->{d.line(x-6,y,x+6,y,1.6f,FactoryPalette.brass);d.line(x+3,y+3,x+6,y,1.6f,FactoryPalette.brass);d.line(x+6,y,x+3,y-3,1.6f,FactoryPalette.brass)}
                }
            }
        }
        shapes.flush();if(clipped) ScissorStack.popScissors()
        d.round(back.x,back.y,back.width,back.height,6f,FactoryPalette.panelEdge)
        if(total>clip.height) {
            val h=clip.height*clip.height/total;val y=clip.y+clip.height-h-scroll/(total-clip.height)*(clip.height-h)
            d.round(box.x+box.width-10,y,3f,h,1f,FactoryPalette.brass)
        }
    }
    fun text(w:Workshop,font:BitmapFont) {
        sprites.begin()
        fun text(t:String,x:Float,y:Float,width:Float,size:Float,color:com.badlogic.gdx.graphics.Color=FactoryPalette.text) {
            font.data.setScale(size/48f);font.color=color;font.draw(sprites,t,x,y,width,com.badlogic.gdx.utils.Align.left,true)
        }
        text("УЛУЧШЕНИЯ",box.x+20,box.y+box.height-20,box.width-40,19f,FactoryPalette.brass)
        text("Кредиты мастерской: ${RussianUi.compact(w.profile.workshopCredits)}",box.x+20,box.y+box.height-51,box.width-40,12f)
        sprites.flush();val clipped=clipping()
        for(row in rows) if(row.bounds.overlaps(clip)) {
            val r=row.bounds
            if(row.section>=0) {
                text(row.title,r.x+10,r.y+31,r.width-20,17f,FactoryPalette.brass)
                // Slight second strike provides a strong section hierarchy with the existing atlas.
                text(row.title,r.x+10.25f,r.y+31,r.width-20,17f,FactoryPalette.brass)
            } else if(row.bold) {
                text(row.title,r.x+10,r.y+r.height-12,r.width-20,14f)
                text(row.title,r.x+10.3f,r.y+r.height-12,r.width-20,14f)
                text(row.detail,r.x+10,r.y+r.height-32,r.width-20,9f,FactoryPalette.muted)
                if(row.footer.isNotEmpty()) text(row.footer,r.x+10,r.y+r.height-49,r.width-20,10f,FactoryPalette.green)
                for(b in row.buttons) {
                    val v=b.bounds
                    text("${b.level}",v.x+28,v.y+v.height-9,v.width-32,13f,FactoryPalette.brass)
                    text(if(!b.enabled) "Сначала купить" else if(b.price>0) "${RussianUi.compact(b.price)} кр." else "MAX",v.x+6,v.y+20,v.width-12,10f,if(b.enabled) FactoryPalette.text else FactoryPalette.muted)
                }
            } else {
                text(row.title,r.x+10,r.y+56,r.width-20,12f)
                if(row.action=="level:receiver-width") text(row.title,r.x+10.3f,r.y+56,r.width-20,12f)
                text(row.detail,r.x+10,r.y+37,r.width-20,10f,FactoryPalette.muted)
                text(row.footer,r.x+10,r.y+15,r.width-20,11f,FactoryPalette.green)
            }
        }
        sprites.flush();if(clipped) ScissorStack.popScissors()
        text("Назад",back.x+12,back.y+28,back.width-24,14f);sprites.end()
    }
}

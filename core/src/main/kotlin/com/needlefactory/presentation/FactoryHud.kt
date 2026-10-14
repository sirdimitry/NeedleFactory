package com.needlefactory.presentation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.needlefactory.simulation.*
import com.needlefactory.economy.Workshop

/** Compact industrial instrument panel, with named states and optional subordinate telemetry. */
internal class FactoryHud(shapes:ShapeRenderer,private val batch:SpriteBatch) {
    private val d=FactoryDrawing(shapes)
    private val s=shapes
    private val names=arrayOf("Вход","Конвейер","Магнит","Упаковщик")
    private val statNames=arrayOf("ПАРТИЯ","НАЙДЕНО","ПРОПУЩЕНО","УПАКОВАНО")
    private val values=Array(4) { "0" }
    private var status=""
    private var detail=""
    private var productValue=""
    private var balerPrice=""
    private var needleReceiverPrice=""
    fun refresh(session:FactorySession,workshop:Workshop?=null) {
        values[0]=session.batchNumber.toString(); values[1]=session.recovered.toString()
        values[2]=session.missed.toString(); values[3]=session.packaged.toString()
        if(workshop!=null) {
            productValue=if(session.baler) "Тюки / ${workshop.config.bundledValue} кр. за партию"
                else if(workshop.config.clumpEconomy) "Пучок / ${workshop.profile.lotValue} кр." else "Солома / ${workshop.config.looseValue} кр. за партию"
            balerPrice=workshop.price(com.needlefactory.economy.UpgradeId.BALER).toString()
            needleReceiverPrice=workshop.price(com.needlefactory.economy.UpgradeId.METAL_RECEIVER).toString()
            val q=workshop.profile.receipt ?: workshop.quote(session.batch)
            statNames[0]="КРЕДИТЫ"; statNames[1]="ИГЛЫ"; statNames[2]="ПРОПУЩЕНО"; statNames[3]="СОЛОМА"
            values[0]=RussianUi.compact(workshop.profile.workshopCredits); values[1]="${q.secured}/${q.total}"
            values[2]=q.missed.toString(); values[3]="${q.percent}%"
            if(session.config.manualProducts) {
                statNames[2]="ВЫРУЧКА"; values[2]="+${RussianUi.compact(q.earned)}"
                statNames[3]="СОБРАНО"; values[3]="${session.collectedStraw*100/session.config.strawCount}%"
                if(session.phase==BatchPhase.RESULTS) values[3]="${q.percent}%"
            }
        }
        status=when {
            session.batch.complete -> {
                val q=workshop?.profile?.receipt
                FactoryCompletionHint.text(q?.secured ?: (session.batch.count(Route.OUTPUT)+session.batch.count(Route.RECOVERED)),
                    q?.missed ?: session.batch.count(Route.MISSED),q?.discarded ?: session.batch.count(Route.DISCARDED))
            }
            !session.running -> "Пауза. Материал и прогресс сохранены."
            !session.enabled(MachineId.CONVEYOR) -> "Конвейер выключен. Материал удержан."
            !session.enabled(MachineId.VIBRATION) && session.batch.count(Route.INTAKE)>0 -> "Вход закрыт. Материал удержан."
            session.batch.count(Route.BUFFER)>0 -> "Игла ждёт. Включите упаковщик."
            session.batch.count(Route.INTAKE)>0 -> "Толкайте солому вправо, к приёмнику."
            else -> "Материал в пути. Следите за станками."
        }
        detail="Партия ${session.batch.seed} / ${session.batch.elapsed.toInt()} с / Буфер ${session.batch.count(Route.BUFFER)}"
        if(session.config.manualProducts) {
            detail="Солома ${session.looseStraw} / Тюки ${session.bundledStraw}"
            if(session.phase==BatchPhase.ACTIVE) status=when {
                !session.running -> "Пауза / материал удержан"
                session.baler && !session.balerRunning -> "Пресс остановлен / солома ждёт"
                !session.receivingEnabled -> "Бункер закрыт / материал удержан"
                session.config.pitReceiver -> if(session.metalReceiverOwned) "Солому — в яму. Иглы — в отдельный приёмник." else "Солому — в яму. Иглы пока не оплачиваются."
                else -> if(session.conveyorOwned) "Подавайте на ленту вручную. ${if(session.operating(MachineId.SEPARATOR)) "Магнит собирает металл." else "Металл можно пропустить."}"
                    else if(session.config.grabClumps) "Солому — вправо. Металл — в нижний лоток."
                    else "Солому — вправо. Открытый металл — в лоток."
            }
        }
        if(workshop!=null && session.phase==BatchPhase.ACTIVE && session.running && session.receivingEnabled &&
            (!session.baler || session.balerRunning) && !workshop.eligible(session.batch)) {
            status=if(session.config.pitReceiver) if(session.metalReceiverOwned) "Солому — в яму. Иглы — в отдельный приёмник." else "Солому — в яму. Иглы пока не оплачиваются."
                else if(session.conveyorOwned) "Подавайте на ленту вручную. ${if(session.operating(MachineId.SEPARATOR)) "Магнит собирает металл." else "Следите за металлом."}"
                else if(session.config.grabClumps) "Солому — вправо. Металл — в нижний лоток."
                else if(session.config.manualProducts) "Собирайте солому, чтобы заработать."
                else if(workshop.quote(session.batch).secured>0) "Завершение: 25% соломы или все 3 иглы."
                else "Подавайте вручную. Сохраните первую иглу."
        }
    }
    private fun cardsX(l:FactoryLayout) = if(l.landscape && l.pitReceiver) l.console.x+10 else if(l.landscape) l.table.x-10f else 16f
    private fun cardsY(l:FactoryLayout) = if(l.landscape) if(l.pitReceiver) l.console.y+144f else l.table.y-130f else l.height-114f
    private fun cardWidth(l:FactoryLayout) = if(l.landscape && l.pitReceiver) 76f else if(l.landscape) 58f else (l.width-50f)/4
    fun shapes(l:FactoryLayout,session:FactorySession,diagnostics:Boolean,expanded:Boolean,workshop:Workshop?=null) {
        val finishReady=workshop?.eligible(session.batch) ?: session.batch.complete
        val panel=l.console
        d.round(panel.x,panel.y,panel.width,panel.height,if(l.landscape) 10f else 0f,FactoryPalette.panel)
        if(l.landscape && !l.pitReceiver) {
            // Identity/counters use the free bay below the search module, not its height.
            d.round(l.table.x-16f,(cardsY(l)-6f).coerceAtLeast(0f),268f,100f,8f,FactoryPalette.background)
        } else if(!l.landscape) d.round(0f,l.height-122f,l.width,122f,0f,FactoryPalette.background)
        val y=cardsY(l); val w=cardWidth(l); val x=cardsX(l)
        val stacked=l.landscape && l.pitReceiver
        if(stacked) {
            for(i in 0..3) {
                val xx=x+(i%2)*84;val yy=y+(1-i/2)*54
                d.round(xx,yy,76f,48f,6f,FactoryPalette.background)
            }
        } else {
            d.round(x,y,4*w+18,48f,7f,FactoryPalette.panelEdge)
            d.round(x+1,y+1,4*w+16,46f,6f,FactoryPalette.panel)
            for(i in 1..3) d.line(x+i*(w+6)-3,y+9,x+i*(w+6)-3,y+39,.7f,FactoryPalette.panelEdge)
        }
        for(i in l.controls.indices) {
            val r=l.controls[i]; if(r.width<=0) continue; val on=if(session.config.manualProducts) when(i) {
                0->session.receivingEnabled; 1->if(session.config.pitReceiver) session.metalReceiverOwned && session.metalReceiverRunning else session.baler && session.balerRunning
                2->session.operating(MachineId.CONVEYOR); else->session.operating(MachineId.SEPARATOR)
            } else session.enabled(MachineId.entries[i])
            d.round(r.x,r.y,r.width,r.height,7f,FactoryPalette.panelEdge)
            d.round(r.x+1,r.y+1,r.width-2,r.height-2,6f,FactoryPalette.background)
            if(session.config.pitReceiver && i==2) {
                val sx=r.x+30;val sy=r.y+r.height/2
                // Basket + handle + visible goods: explicit shopping symbol.
                d.line(sx-12,sy+7,sx+12,sy+7,2f,FactoryPalette.steel)
                d.line(sx-12,sy+7,sx-8,sy-10,2f,FactoryPalette.steel)
                d.line(sx+12,sy+7,sx+8,sy-10,2f,FactoryPalette.steel)
                d.line(sx-8,sy-10,sx+8,sy-10,2f,FactoryPalette.steel)
                d.line(sx-7,sy+8,sx-3,sy+16,2f,FactoryPalette.brass)
                d.line(sx+7,sy+8,sx+3,sy+16,2f,FactoryPalette.brass)
                for(k in -1..1) d.line(sx+k*5,sy+3,sx+k*4,sy-6,1f,FactoryPalette.top)
                continue
            }
            // An actual two-position switch and ON/OFF text both convey state.
            val cx=r.x+r.width-21; val cy=r.y+17
            val locked=session.config.manualProducts && when(i) {
                1->if(session.config.pitReceiver) !session.metalReceiverOwned else !session.baler
                2->!session.config.pitReceiver && !session.conveyorOwned; 3->!session.config.pitReceiver && !session.magnetOwned; else->false
            }
            if(locked) {
                // Unowned equipment has a lock, not a switch implying it can be enabled.
                d.round(cx-4,cy-4,8f,7f,1f,FactoryPalette.muted)
                d.line(cx-3,cy+3,cx-3,cy+6,1f,FactoryPalette.muted)
                d.line(cx-3,cy+6,cx+3,cy+6,1f,FactoryPalette.muted)
                d.line(cx+3,cy+6,cx+3,cy+3,1f,FactoryPalette.muted)
            } else if(session.config.pitReceiver && i>=2) {
                d.line(cx-9,cy,cx+5,cy,1.4f,FactoryPalette.brass)
                d.line(cx,cy+5,cx+5,cy,1.4f,FactoryPalette.brass)
                d.line(cx,cy-5,cx+5,cy,1.4f,FactoryPalette.brass)
            } else {
                d.round(cx-12,cy-6,24f,12f,6f,if(on) FactoryPalette.body else FactoryPalette.panelEdge)
                s.color=if(on) FactoryPalette.green else FactoryPalette.muted
                s.circle(cx+if(on) 6f else -6f,cy,4.5f,16)
            }
        }
        val r=l.run; val n=l.next
        d.round(r.x,r.y,r.width,r.height,8f,FactoryPalette.steel)
        d.round(r.x+1,r.y+1,r.width-2,r.height-2,7f,FactoryPalette.panel)
        if(session.running) {
            d.round(r.x+15,r.y+r.height/2-6,3.5f,12f,1f,FactoryPalette.text)
            d.round(r.x+22,r.y+r.height/2-6,3.5f,12f,1f,FactoryPalette.text)
        } else {
            s.color=FactoryPalette.green
            s.triangle(r.x+16,r.y+r.height/2-7,r.x+16,r.y+r.height/2+7,r.x+26,r.y+r.height/2)
        }
        d.round(n.x,n.y,n.width,n.height,8f,if(finishReady) FactoryPalette.green else FactoryPalette.panelEdge)
        if(!finishReady) d.round(n.x+1,n.y+1,n.width-2,n.height-2,7f,FactoryPalette.panel)
        val c=if(finishReady) FactoryPalette.background else FactoryPalette.muted
        if(!(l.landscape && l.pitReceiver)) {
        d.line(n.x+n.width-26,n.y+n.height/2,n.x+n.width-14,n.y+n.height/2,1.8f,c)
        d.line(n.x+n.width-19,n.y+n.height/2+5,n.x+n.width-14,n.y+n.height/2,1.8f,c)
        d.line(n.x+n.width-19,n.y+n.height/2-5,n.x+n.width-14,n.y+n.height/2,1.8f,c)
        }
        // A quiet status bullet and telemetry drawer, outside material/machine space.
        s.color=if(session.running && !session.batch.complete) FactoryPalette.green else FactoryPalette.brass
        if(!l.landscape) s.circle(20f,151f,2.7f,14)
        if(workshop?.config?.archiveTools==true) {
            val t=l.handTool; d.round(t.x,t.y,t.width,t.height,5f,FactoryPalette.panelEdge)
        }
        if(diagnostics || workshop!=null) {
            val t=l.telemetry; d.round(t.x,t.y,t.width,t.height,7f,if(expanded) FactoryPalette.body else FactoryPalette.panel)
            if(diagnostics && expanded) d.round(if(l.landscape) panel.x+8 else 15f,
                if(l.landscape) panel.y+panel.height-69 else 139f,
                if(l.landscape) panel.width-16 else l.width-30,if(l.landscape) 44f else 24f,6f,FactoryPalette.background)
        }
    }
    fun text(l:FactoryLayout,session:FactorySession,font:BitmapFont,metrics:String,diagnostics:Boolean,expanded:Boolean,workshop:Workshop?=null,logo:com.badlogic.gdx.graphics.Texture?=null) {
        batch.begin()
        fun text(value:String,x:Float,y:Float,size:Float,c:Color=FactoryPalette.text) {
            font.data.setScale(size/48f); font.color=c; font.draw(batch,value,x,y)
        }
        val brandX=if(l.landscape && l.pitReceiver) l.console.x+10 else if(l.landscape) l.table.x-4f else 20f
        val brandY=if(l.landscape) if(l.pitReceiver) l.console.y+l.console.height-12 else l.table.y-50f else l.height-18f
        val brandSize=if(l.landscape) 22f else 28f
        if(logo!=null) {batch.color=Color.WHITE;batch.draw(logo,brandX,brandY-brandSize+5,brandSize,brandSize)}
        text(RussianUi.GAME_TITLE.uppercase(java.util.Locale.ROOT),brandX+brandSize+7,brandY,if(l.landscape && l.pitReceiver) 10f else if(l.landscape) 13f else 15f)
        if(!(l.landscape && l.pitReceiver)) text(if(workshop==null) "ЛИНИЯ 01 / РУЧНАЯ ПОДАЧА" else "ЭТАП 03 / ВРУЧНУЮ",if(l.landscape && l.pitReceiver) l.console.x+10 else if(l.landscape) l.table.x-3f else 21f,
            if(l.landscape) if(l.pitReceiver) l.console.y+l.console.height-34 else l.table.y-70f else l.height-46f,10.5f,FactoryPalette.muted)
        val x=cardsX(l); val y=cardsY(l); val w=cardWidth(l)
        for(i in 0..3) {
            val xx=if(l.landscape && l.pitReceiver) x+(i%2)*84 else x+i*(w+6)
            val yy=if(l.landscape && l.pitReceiver) y+(1-i/2)*54 else y
            text(statNames[i],xx+if(l.landscape) 5f else 10f,yy+42,
                if(l.landscape) 8f else 10f,if(i==3) FactoryPalette.green else FactoryPalette.muted)
            val advance=values[i].sumOf { (font.data.getGlyph(it)?.xadvance ?: 0).toDouble() }.toFloat()
            val fitted=minOf(22f,48f*(w-16)/advance.coerceAtLeast(1f))
            text(values[i],xx+8,yy+26,fitted,if(i==3) FactoryPalette.green else FactoryPalette.text)
        }
        if(l.landscape && !l.pitReceiver) {
            text("УПРАВЛЕНИЕ",l.console.x+10,l.console.y+l.console.height-12,12f,FactoryPalette.muted)
            if(!expanded && workshop?.config?.archiveTools!=true) text(when {
                session.batch.complete -> "Партия завершена"
                !session.running -> "Пауза / прогресс сохранён"
                session.config.manualProducts -> productValue
                !session.enabled(MachineId.CONVEYOR) -> "Конвейер выключен"
                session.batch.count(Route.BUFFER)>0 -> "Игла ждёт упаковки"
                session.batch.count(Route.INTAKE)>0 -> "Подавайте вправо вручную"
                else -> "Линия работает"
            },l.console.x+10,l.console.y+l.console.height-35,10.5f,FactoryPalette.muted)
        } else if(!l.landscape && !expanded) {
            font.data.setScale(11f/48f); font.color=FactoryPalette.muted
            font.draw(batch,status,l.statusX,l.statusY,l.width-l.statusX-15,com.badlogic.gdx.utils.Align.left,true)
        }
        if(workshop?.config?.archiveTools==true) {
            val t=l.handTool
            text("${RussianUi.tool(workshop.tool)} / ${session.handCapacity} >",t.x+7,t.y+17,11f,FactoryPalette.text)
        }
        for(i in l.controls.indices) {
            val r=l.controls[i]
            if(r.width<=0) continue
            if(session.config.pitReceiver && i==2) {
                text("Магазин",r.x+56,r.y+r.height/2+7,16f)
                continue
            }
            text(if(session.config.pitReceiver) arrayOf("Шреддер","Иглы")[i]
                else if(session.config.manualProducts) arrayOf("Бункер","Пресс","Лента","Магнит")[i] else names[i],r.x+8,r.y+45,12f)
            val state=if(session.config.manualProducts) {
                when(i) {
                    0->if(session.receivingEnabled) "ОТКР" else "ЗАКР"
                    1->if(session.config.pitReceiver) if(!session.metalReceiverOwned) "НЕТ" else if(session.metalReceiverRunning) "ВКЛ" else "ВЫКЛ"
                        else if(!session.baler) "НЕТ" else if(session.balerRunning) "ВКЛ" else "ВЫКЛ"
                    2->if(!session.conveyorOwned) "НЕТ" else if(session.enabled(MachineId.CONVEYOR)) "ВКЛ" else "ВЫКЛ"
                    else->if(!session.magnetOwned) "НЕТ" else if(session.enabled(MachineId.SEPARATOR)) "ВКЛ" else "ВЫКЛ"
                }
            } else if(session.enabled(MachineId.entries[i])) "ВКЛ" else "ВЫКЛ"
            text(if(session.config.pitReceiver && i==2) "ОТКРЫТЬ" else if(session.config.pitReceiver && i==3) "ОТКРЫТЬ" else state,r.x+8,r.y+23,if(r.width<76f) 8f else 9f,FactoryPalette.muted)
        }
        text(if(session.running) "Пауза" else "Работать",l.run.x+if(l.landscape && l.pitReceiver) 30 else 36,l.run.y+l.run.height/2+8,if(l.landscape && l.pitReceiver) 9f else 14f)
        text(if(workshop==null) "Новая партия" else if(session.phase==BatchPhase.DRAINING) "Обработка..." else "Завершить",l.next.x+6,l.next.y+l.next.height/2+8,if(l.landscape && l.pitReceiver) 9f else 15f,
            if(workshop?.eligible(session.batch) ?: session.batch.complete) FactoryPalette.background else FactoryPalette.muted)
        // Nameplates are on front faces, never floating over straw or moving metal.
        val u=l.unit
        if(session.config.manualProducts) {
            if(session.config.pitReceiver) {
                text("СОЛОМА / ВЗЯТЬ РУКОЙ",l.pileBin.x+5,l.pileBin.y-12,7f)
                text("ШРЕДДЕР / ЯМА",l.pitHole.x-2,l.pitHole.y+l.pitHole.height+16,8f)
                text("ШРЕДДЕР / ЯМА",l.pitHole.x-1.8f,l.pitHole.y+l.pitHole.height+16,8f)
                text("[ ${session.receiverWidthLevel+1} ]",l.pitHole.x,l.pitHole.y+l.pitHole.height+5,7f,FactoryPalette.brass)
                text(productValue,l.pitHole.x-2,l.pitHole.y+l.pitHole.height+29,7f,FactoryPalette.brass)
                if(session.metalReceiverOwned) text("ИГЛЫ",l.needleReceiver.x-4,l.needleReceiver.y+l.needleReceiver.height+10,5.5f)
            } else {
            text("ПОИСК / РУЧНАЯ ПОДАЧА",l.table.x+22,l.table.y-11,8f,FactoryPalette.text)
            text(if(session.magnetOwned) "МЕТАЛЛ / СОРТ." else "МЕТАЛЛ",l.metalTray.x-29,l.metalTray.y-16,7.5f)
            text(if(session.baler) "ПРЕСС / ТЮКИ" else "МЕСТО ПРЕССА / $balerPrice",l.productBaler.x-44,l.productBaler.y-27,7f)
            }
        } else {
        text("01 / ПОДАЧА",l.table.x+52*u,l.table.y-11*u,9.5f*u,FactoryPalette.text)
        text("03 / МАГНИТ",l.magnet.x-26*u,l.magnet.y+42*u,8.5f*u)
        text("БУФЕР",l.buffer.x-18*u,l.buffer.y-24*u,8.5f*u)
        text("04 / УПАКОВКА",l.packer.x-31*u,l.packer.y-33*u,9f*u)
        text("ВЫХОД",l.output.x-18*u,l.output.y-23*u,8.5f*u)
        text("СОЛОМА",l.residue.x-15*u,l.residue.y-21*u,8.5f*u)
        text("ПРОПУЩЕНО",l.missed.x-16*u,l.missed.y-20*u,8.5f*u)
        }
        if(diagnostics || workshop!=null) {
            text("i",l.telemetry.x+11,l.telemetry.y+23,18f,FactoryPalette.muted)
            if(diagnostics && expanded) {
                val y0=if(l.landscape) l.console.y+l.console.height-69 else 139f
                val xx=if(l.landscape) l.console.x+12 else 25f
                if(l.landscape) {
                    font.data.setScale(9f/48f); font.color=FactoryPalette.green
                    font.draw(batch,metrics.replace("bodies","тел"),xx,y0+40,158f,com.badlogic.gdx.utils.Align.left,true)
                    text(detail,xx,y0+12,8f,FactoryPalette.muted)
                } else {
                    text(metrics.replace("bodies","тел"),xx,y0+23,10.5f,FactoryPalette.green)
                    text(detail,xx,y0+11,9f,FactoryPalette.muted)
                }
            }
        }
        batch.end()
    }
}

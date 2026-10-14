package com.needlefactory.presentation

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.needlefactory.economy.*
import com.needlefactory.save.WorkshopRuntime
import com.needlefactory.simulation.*

enum class WorkshopView { NONE, FINISH, RESULTS, SHOP, SETTINGS, SOUND, ABOUT, RESET_CONFIRM, PURCHASE_CONFIRM }

/** Fixed-size modal instruments; the factory camera/module scale never changes for a panel. */
internal class WorkshopPanel(shapes: ShapeRenderer, private val sprites: SpriteBatch) {
    val liveShop=LiveWorkshopPanel(shapes,sprites)
    var confirmationTitle="";var confirmationDetail=""
    private val d=FactoryDrawing(shapes)
    val left=Rectangle(); val right=Rectangle()
    val purchases=Array(UpgradeId.entries.size) { Rectangle() }
    val pageButton=Rectangle()
    val cheatButtons=Array(3) { Rectangle() }
    val soundButton=Rectangle()
    val aboutButton=Rectangle()
    val cheatAmounts=longArrayOf(100,1_000,10_000)
    private var page=0
    fun nextPage() { page=1-page }
    private val box=Rectangle()
    private var columns=false
    private val shade=com.badlogic.gdx.graphics.Color(0f,0f,0f,.52f)
    fun layout(l: FactoryLayout,view:WorkshopView=WorkshopView.RESULTS) {
        if(view==WorkshopView.SHOP) liveShop.layout(l)
        columns=view==WorkshopView.SHOP && l.landscape
        val w=if(view==WorkshopView.SETTINGS) l.width-24f else minOf(if(columns) 600f else 350f,l.width-24f)
        val h=minOf(when(view) {WorkshopView.SHOP->430f;WorkshopView.SETTINGS->410f;else->332f},l.height-24f)
        box.set((l.width-w)/2,(l.height-h)/2,w,h)
        val buttonWidth=minOf((w-44)/2,168f)
        left.set(box.x+16,box.y+14,buttonWidth,48f)
        right.set(box.x+w-16-buttonWidth,left.y,buttonWidth,48f)
        if(view==WorkshopView.ABOUT) {left.width=w-32;right.set(0f,0f,0f,0f)}
        pageButton.set(box.x+16,box.y+h-114,box.width-32,28f)
        val cheatWidth=(box.width-48)/3
        cheatButtons.forEachIndexed { i,r -> r.set(box.x+16+i*(cheatWidth+8),box.y+h-202,cheatWidth,76f) }
        soundButton.set(box.x+16,box.y+68,box.width-32,28f)
        aboutButton.set(box.x+16,box.y+102,box.width-32,28f)
        purchases.forEachIndexed { i,r ->
            val slot=i-page*3
            if(slot !in 0..2) r.set(0f,0f,0f,0f)
            else if(columns) {
                val count=3
                r.set(box.x+16+slot*(w-32)/count,box.y+86,(w-32)/count-8,h-212)
            }
            else r.set(box.x+16,box.y+box.height-184-slot*68,w-32,62f)
        }
    }
    fun shape(l: FactoryLayout,view: WorkshopView,modern:Boolean=false) {
        if(view==WorkshopView.SHOP && modern) { liveShop.shape(l); return }
        layout(l,view)
        d.shapes.color=shade; d.shapes.rect(0f,0f,l.width,l.height)
        d.cabinet(box.x,box.y,box.width,box.height,5f,FactoryPalette.panel)
        d.line(box.x+16,box.y+box.height-51,box.x+box.width-16,box.y+box.height-51,1f,FactoryPalette.brass)
        if(view==WorkshopView.SHOP || view==WorkshopView.SETTINGS) {
            if(view==WorkshopView.SHOP)
            for(r in purchases) if(r.width>0) d.round(r.x,r.y,r.width,r.height,6f,FactoryPalette.background)
            d.round(pageButton.x,pageButton.y,pageButton.width,pageButton.height,5f,FactoryPalette.panelEdge)
            if(view==WorkshopView.SETTINGS) for(r in cheatButtons)
                d.round(r.x,r.y,r.width,r.height,7f,FactoryPalette.panelEdge)
            if(view==WorkshopView.SETTINGS) d.round(soundButton.x,soundButton.y,soundButton.width,soundButton.height,5f,FactoryPalette.panelEdge)
            if(view==WorkshopView.SETTINGS) d.round(aboutButton.x,aboutButton.y,aboutButton.width,aboutButton.height,5f,FactoryPalette.panelEdge)
        }
        d.round(left.x,left.y,left.width,left.height,7f,FactoryPalette.panelEdge)
        if(right.width>0) d.round(right.x,right.y,right.width,right.height,7f,FactoryPalette.green)
    }
    fun text(view: WorkshopView,runtime: WorkshopRuntime,session: FactorySession,font: BitmapFont,diagnostics:Boolean=false,logo:com.badlogic.gdx.graphics.Texture?=null) {
        if(view==WorkshopView.SHOP && runtime.workshop.config.clumpEconomy) { liveShop.text(runtime.workshop,font);return }
        val workshop=runtime.workshop; val q=workshop.profile.receipt ?: workshop.quote(session.batch)
        sprites.begin()
        fun text(t:String,y:Float,size:Float=14f,color:com.badlogic.gdx.graphics.Color=FactoryPalette.text) {
            font.data.setScale(size/48f); font.color=color
            font.draw(sprites,t,box.x+20,y,box.width-40,com.badlogic.gdx.utils.Align.left,true)
        }
        val top=box.y+box.height
        text(when(view) { WorkshopView.ABOUT->"ОБ ИГРЕ"; WorkshopView.SETTINGS->"НАСТРОЙКИ"; WorkshopView.RESET_CONFIRM->"НАЧАТЬ СНАЧАЛА?"
            WorkshopView.PURCHASE_CONFIRM->"ПОДТВЕРДИТЬ?"; WorkshopView.SHOP->"МАСТЕРСКАЯ"; WorkshopView.FINISH->"ЗАВЕРШИТЬ ПАРТИЮ?"; else->if(q.perfect) "ЧИСТАЯ ПАРТИЯ!" else "ИТОГИ ПАРТИИ" },top-18,18f,FactoryPalette.brass)
        if(view==WorkshopView.ABOUT) {
            if(logo!=null) {sprites.color=com.badlogic.gdx.graphics.Color.WHITE;sprites.draw(logo,box.x+box.width/2-42,top-150,84f,84f)}
            font.data.setScale(18f/48f);font.color=FactoryPalette.text
            font.draw(sprites,RussianUi.GAME_TITLE,box.x+16,top-166,box.width-32,com.badlogic.gdx.utils.Align.center,false)
            text("NeedleFactory\nПоиск, ручной труд и автоматизация.\nЭтап 3 — тестовая версия.",top-195,11f,FactoryPalette.muted)
        } else if(view==WorkshopView.PURCHASE_CONFIRM) {
            text(confirmationTitle,top-76,18f)
            text(confirmationDetail,top-122,14f,FactoryPalette.brass)
            text("Баланс: ${workshop.profile.workshopCredits} кр.\nПокупка применяется сразу.",top-184,12f,FactoryPalette.muted)
        } else if(view==WorkshopView.SETTINGS) {
            text("Баланс: ${RussianUi.credits(workshop.profile.workshopCredits)}",top-70,15f)
            text("Диагностика: ${if(diagnostics) "вкл" else "выкл"} / переключить",top-96,11f)
            val r=soundButton;font.data.setScale(12f/48f);font.color=FactoryPalette.text
            font.draw(sprites,"Звук  >",r.x+6,r.y+19,r.width-12,com.badlogic.gdx.utils.Align.center,false)
            val a=aboutButton
            font.draw(sprites,"Об игре  >",a.x+6,a.y+19,a.width-12,com.badlogic.gdx.utils.Align.center,false)
            val labels=arrayOf("+100","+1 000","+10 000")
            for(i in cheatButtons.indices) {
                val r=cheatButtons[i];font.data.setScale(21f/48f);font.color=FactoryPalette.brass
                font.draw(sprites,labels[i],r.x+8,r.y+51,r.width-16,com.badlogic.gdx.utils.Align.center,false)
                font.data.setScale(12f/48f);font.color=FactoryPalette.text
                font.draw(sprites,"денег",r.x+8,r.y+25,r.width-16,com.badlogic.gdx.utils.Align.center,false)
            }
        } else if(view==WorkshopView.RESET_CONFIRM) {
            text("Весь прогресс будет сброшен:",top-72,14f)
            text("Кредиты: ${workshop.profile.workshopCredits}\nПокупки: ${workshop.profile.purchased.size}\nПройдено партий: ${workshop.profile.completedBatches}",top-108,14f,FactoryPalette.muted)
            text("Вы начнёте без купленных станков. Отменить сброс нельзя.",top-185,14f,FactoryPalette.brass)
        } else if(view==WorkshopView.SHOP) {
            text("Баланс: ${RussianUi.credits(workshop.profile.workshopCredits)}",top-72,14f)
            text(if(workshop.config.pitProgression) "Приёмник игл и ручные инструменты" else if(page==0) "Инструменты и пресс / далее >" else "Лента, магнит и грабли / назад >",top-94,11f)
            workshop.catalog.forEachIndexed { i,u ->
                val r=purchases[i]
                if(r.width<=0) return@forEachIndexed
                fun card(t:String,y:Float,size:Float,color:com.badlogic.gdx.graphics.Color=FactoryPalette.text) {
                    font.data.setScale(size/48f); font.color=color
                    font.draw(sprites,t,r.x+8,y,r.width-16,com.badlogic.gdx.utils.Align.left,true)
                }
                card("${RussianUi.upgrade(u)} / ${workshop.price(u)} кр.",if(columns) r.y+r.height-17 else r.y+47,if(columns) 12f else 13f)
                card(when(u) {
                    UpgradeId.RAKE->if(workshop.config.archiveTools) "До 2 соседних пучков. Всё вручную." else "Ручной инструмент. Один пучок."
                    UpgradeId.GUIDES->"Меньше рассыпания у входа в бункер."
                    UpgradeId.BALER->"Тюки: ${workshop.config.looseValue} -> ${workshop.config.bundledValue} кр. за объём партии."
                    UpgradeId.CONVEYOR->"Доставляет ручную загрузку к прессу."
                    UpgradeId.MAGNET->"Металл с ленты: ${workshop.config.sortedNeedleValue} кр. за иглу."
                    UpgradeId.STEEL_RAKE->"До 3 пучков. Отпустили - тяги нет."
                    UpgradeId.METAL_RECEIVER->"Отдельный лоток рядом с ямой. ${workshop.config.needleValue} кр. за иглу."
                },if(columns) r.y+r.height-54 else r.y+28,11f,FactoryPalette.muted)
                card(when { u.key in workshop.profile.purchased->if(workshop.config.archiveTools && HandTool.entries.any { it.key==u.key })
                    if(workshop.tool.key==u.key) "ВЫБРАНО" else "Нажмите: взять" else "УСТАНОВЛЕНО";
                    u.prerequisite!=null && u.prerequisite !in workshop.profile.purchased->"Нужно: ${RussianUi.upgrade(UpgradeId.entries.first { it.key==u.prerequisite })}";
                    workshop.profile.workshopCredits<workshop.price(u)->"Не хватает кредитов"; else->"Нажмите: купить" },r.y+if(columns) 19 else 10,9f,FactoryPalette.green)
            }
        } else {
            text("ИГЛЫ ${q.secured} / ${q.total}     СОЛОМА ${q.percent}%",top-72,14f)
            text(if(q.economyModel.startsWith("products-")) "Солома +${q.material} / металл +${q.metal}"
                else "Металл +${q.metal} / солома +${q.material} / бонус +${q.bonus}",top-104,12f,FactoryPalette.muted)
            if(q.economyModel=="products-v5") text(
                if(q.bonus>0) "Полная доставка соломы +${q.bonus}"
                else if(view==WorkshopView.RESULTS) "Полная доставка соломы не выполнена"
                else "До полной доставки соломы: +${workshop.config.strawDeliveryBonus}",
                top-119,10f,if(q.bonus>0) FactoryPalette.green else FactoryPalette.muted)
            if(q.economyModel in setOf("products-v6","products-v7")) text("Пучки оплачены: ${workshop.profile.delivered.values.sum()} / бонусы: +${q.bonus}",top-119,10f,FactoryPalette.muted)
            text(if(view==WorkshopView.FINISH && q.economyModel in setOf("products-v6","products-v7")) "Ещё: +${RussianUi.credits(q.earned-workshop.profile.delivered.values.sum())}"
                else if(view==WorkshopView.FINISH) "Гарантия: +${RussianUi.credits(q.earned)}" else "+${RussianUi.credits(q.earned).uppercase(java.util.Locale.ROOT)}",top-145,20f,FactoryPalette.green)
            text(if(view==WorkshopView.FINISH && session.config.manualProducts) "Собранное будет оплачено. Бункер закончит принятую солому. Остаток кучи и скрытый металл останутся."
                else if(view==WorkshopView.FINISH) "Линия завершит принятую загрузку. Остаток партии не оплачивается. Магнит работает как настроен."
                else "Пропущено игл: ${q.missed}\nНе обработано: ${q.discarded}",top-174,12f,FactoryPalette.muted)
        }
        if(view==WorkshopView.RESULTS || view==WorkshopView.FINISH) text("Баланс: ${RussianUi.credits(workshop.profile.workshopCredits)}",box.y+92,14f)
        text(when { !runtime.ready->"Загрузка мастерской..."; runtime.readOnly->RussianUi.warning(runtime.warning);
            runtime.persistence.error!=null->"Ошибка записи / нажмите Повторить"; runtime.persistence.saving->"Сохранение...";
            else->RussianUi.warning(runtime.warning) },box.y+73,9f,FactoryPalette.muted)
        fun button(t:String,r:Rectangle) {
            val advance=t.sumOf { (font.data.getGlyph(it)?.xadvance ?: 0).toDouble() }.toFloat()
            font.data.setScale(minOf(14f,48f*(r.width-20)/advance.coerceAtLeast(1f))/48f)
            font.color=if(r===right) FactoryPalette.background else FactoryPalette.text; font.draw(sprites,t,r.x+10,r.y+31)
        }
        button(if(runtime.persistence.error!=null) "Повторить" else when(view) { WorkshopView.ABOUT->"Назад"; WorkshopView.PURCHASE_CONFIRM->"Нет"; WorkshopView.RESET_CONFIRM->"Отмена"; WorkshopView.SETTINGS->"Назад"; WorkshopView.FINISH->"Продолжить"; WorkshopView.SHOP->"Назад"; else->"Мастерская" },left)
        if(right.width>0) button(when(view) { WorkshopView.PURCHASE_CONFIRM->"Да"; WorkshopView.SETTINGS->"Начать сначала"; WorkshopView.RESET_CONFIRM->"Сбросить прогресс"; WorkshopView.FINISH->"Завершить"; else->"Новая партия" },right)
        sprites.end()
    }
    fun contains(p:Vector2) = box.contains(p)
}

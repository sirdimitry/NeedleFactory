package com.needlefactory.economy

/** Small balance table. Level costs grow by 1.75; no timer or automatic feed. */
enum class ProgressionUpgrade(val key:String,val title:String,val category:Int,val basePrice:Long,val tool:HandTool?=null) {
    LOOSEN_GRIP("loosen-grip","Рыхлитель / ещё один пучок",0,35,HandTool.RAKE),
    LOOSEN_SPEED("loosen-speed","Рыхлитель / скорость",0,30,HandTool.RAKE),
    RAKE_GRIP("rake-grip","Грабли / ещё один пучок",0,100,HandTool.STEEL_RAKE),
    RAKE_SPEED("rake-speed","Грабли / скорость",0,90,HandTool.STEEL_RAKE),
    VALUE("material-value","Стоимость пучка",1,55),
    COUNT("material-count","Пучки в стоге",1,45),
    LUCK("material-luck","Шанс ценного материала",1,120),
    RECEIVER_SPEED("receiver-speed","Шреддер / скорость обработки",2,40),
    RECEIVER_CAPACITY("receiver-capacity","Шреддер / приём нескольких пучков",2,55),
    RECEIVER_WIDTH("receiver-width","Ширина бункера шреддера",2,75)
}

# NeedleFactory — Stage 3 Design / Planning

Дата: 2026-10-05. Статус: **OWNER APPROVED DESIGN**.
Owner отдельно разрешил первый playable implementation slice, ограниченный
[STAGE_3.md](STAGE_3.md). Полный дизайн не реализуется одним проходом.
Числа ниже — initial tuning, не результаты Stage 3 owner playtest.
Исторические design-only stop points ниже описывают предыдущий проход;
текущая runtime authorization и обязательный review stop определены в STAGE_3.md.

**Toolkit: GAMEPLAY_CORE + ECONOMY_PROGRESSION + GAME_FEEL + UI_UX + QA_VERIFY.**
ECONOMY_PROGRESSION используется только для дизайна. Role lenses: Game Director /
Systems Designer, Game Economy Designer, Senior Gameplay Designer, Game UI/UX
Designer, QA / Balance Designer. Новые внешние инструменты/API не подключаются.

Основа: [AGENTS](../AGENTS.md), [ROADMAP](ROADMAP.md), [PROGRESS](PROGRESS.md),
[Stage 2 scope](STAGE_2.md), [Stage 2 report](STAGE_2_REPORT.md),
[предыдущий proposal](GAMEPLAY_ECONOMY_PROPOSAL.md), [GDD](GDD.md),
[Architecture](ARCHITECTURE.md), [Art Bible](ART_BIBLE.md), [Skills](SKILLS.md)
и [Toolkit](NEEDLEFACTORY_GAMEDEV_TOOLKIT.md). Дополнительно учтён ECONOMY.md.
Accepted baseline: **v0.2-stage2 → d75c779939f1ab02f7b02382e94b5ae1aef9f380**.

## 1. Core Stage 3 loop

**Выбрать работу → вручную разобрать/подать материал → сохранить находки →
решить, продолжать ли → завершить расчёт → купить механизм → взять следующую
работу и почувствовать, какую обязанность механизм снял.**

Рекомендуемый старт — уже построенная Stage 2 линия с ручным intake. Conveyor,
magnet, buffer и автоматически циклирующий packer уже принадлежат игроку.
Не продаём их повторно, не отключаем принятые возможности ради искусственного
grind. Первая новая покупка улучшает работу рук; затем механизмы постепенно
заменяют длинные повторные движения. Полная автоподача — видимая поздняя цель
этого небольшого progression slice, без автоматического заказа следующих партий.

Важное различие: сейчас magnet захватывает иглу независимо от её открытости.
NeedleVisibility — **presentation**, не правило обнаружения/награды. Предлагаю
сохранить это: можно внимательно искать и прицельно подать металл для раннего
выхода, либо добросовестно переработать всю кучу. Не вводить обязательный тап
«я увидел иглу» перед magnet. Классический обязательный ручной поиск — отдельный
вариант для owner, не скрытая смена accepted factory loop.

### Соответствие существующей архитектуре

Проверены FactorySession/FactoryBatch/FactoryGenerator, FactoryIntakePhysics,
FactoryConfig, FactoryScreen/Hud/Layout, NeedleVisibility, Game и текущие tests.
Economy/save пока содержат только package notes. Предлагаемые изменения после
approval адресные, в существующих core/android/lwjgl3:

| Сейчас | Потребность Stage 3 / ограниченное расширение |
| --- | --- |
| `batch.needle = items.last()`; HUD/render ориентированы на одну иглу | Список critical IDs из manifest; per-batch secured/missed, рендер нескольких игл. Индексы ID должны оставаться согласованными с native arrays. |
| Каждая BUFFER игла может перейти в PACKING в том же tick | FIFO и **один** активный packer; busy capture/chute/packer требуют видимого backpressure. Нельзя просто добавить 3 иглы в старый generator. |
| Счётчики recovered/packaged накопительные на сессию | Settlement по уникальным OUTPUT IDs конкретной партии, не по накопительным HUD counters. |
| Native finger контакт выставляет `manuallyPushed`; затем реальный gate handoff | Начальный режим сохранить. Покупные механизмы физически двигают intake; разрешение подачи различает MANUAL/ASSISTED/AUTO, не подделывает touch. |
| Next только после всех terminal items | ACTIVE → DRAINING → SETTLED → RESULTS; явные unresolved/discarded dispositions, а не удаление оставшихся объектов. |
| Portable session переживает screen/Activity recreation | Отдельный профиль/расчёт/снимок save; не делать renderer владельцем wallet. |

Экономика читает immutable итог партии; не читает экранные пиксели, датчики или
анимации. Physics остаётся intake-only; transport — fixed-step. Canvas/anchors,
fullscreen, safe areas, длина/толщина иглы и >60% metallic gate сохраняются.
Не нужны ECS, DI, Compose, новый движок или отдельный Gradle module на систему.

## 2. Multi-needle model

| Вариант | Польза | Риск | Решение |
| --- | --- | --- | --- |
| A: 3–5 с начала | Частые маленькие успехи | Случайный пучок сразу отдаёт всё; длинная уборка хвоста | Начать с **3**, разнести зоны, 5 не выдавать сразу. |
| B: чем дальше, тем меньше и дороже | Ценность внимательного поиска | Прогресс превращается в длинные сухие интервалы | Не использовать как обязательную лестницу. |
| C: обычные + challenge + поздние специальные работы | Разные ритмы и выбор | Слишком много типов до обучения | **Рекомендовано:** 3 обычные; добровольная 1 ценная; 4–5/особые условия позже. |

Stage 3 MUST: ordinary recipe с 3 иглами, затем второй cover-profile с теми же
3 иглами после первого contact assist. Добровольный precision challenge с
1 иглой — SHOULD при дешёвом использовании общего pickup interaction.
3–4 вариативных ordinary/2-needle challenge можно добавить, только если основной
баланс устойчив. Количество — параметр
recipe, не функция уровня. Принятый размер 1.3 m / текущая ширина сохраняется.

- Минимальный успех: **1 игла в OUTPUT**, не просто замечена/схвачена магнитом.
  Для optional precision recipe также требуется объявленный manual provenance.
- Perfect clear: все обязательные иглы в contract-eligible OUTPUT, весь straw в RESIDUE,
  нет discarded/missed critical IDs. Остаток сырья на столе исключает perfect.
- Ненайденные иглы после finish имеют явный UNRECOVERED disposition и дают 0;
  иглы, реально прошедшие в missed bin, отдельно отмечаются MISSED. Не называем
  оба случая «исчезли». Редкость не входит в условие needle-perfect.
- До старта показываем точное N. HUD: «Сохранено 1/3»; отдельный missed indicator.
  «Не сохранено ещё 2» не раскрывает координаты и не обещает, что обе в intake.
  Прогресс CAPTURE/BUFFER — «металл в обработке», без награды до OUTPUT.
- Manifest фиксирует seed, recipe/version, item IDs, ценности, work units и
  rare ticket. Иголки размещаются в разных локальных зонах; ориентир separation
  >=1.5 длины иглы. Первые позиции не образуют гарантированную лёгкую полосу у mouth.
- 320 patches сохраняют текущую плотность. До 5 needles + 1 collectible + finger:
  **327 bodies** до добавления привода. Зарезервировать максимум ещё 2 meaningful
  feeder bodies, общий бюджет **329**, не тысячи декоративных strand bodies.
  Это бюджет для проверки, не измеренная производительность Stage 3.

Baseline buffer: один waiting slot + отдельный active packer slot. Busy magnet
не теряет следующую иглу: поток удерживается перед capture/на upstream gate.
FIFO по поступлению и стабильный tie-break по ID. Без parallel PACKING, вытеснения
или teleport. Пауза/rotation не меняют порядок. Сохранение предметов — базовая
корректность, её нельзя продавать как upgrade. Capacity upgrades уменьшают остановки.

## 3. Early finish / риск против награды

Рекомендованная eligibility: хотя бы **1 OUTPUT needle (либо secured medallion)
и >=25% обработанного материала**, либо **все обязательные иглы в contract-eligible OUTPUT**.
Для precision recipe первая найденная игла тоже должна выполнять ручное условие.
Первый обычный успех часто
уже проходит этот gate. Если игла пришла очень рано, кнопка объясняет «Для
поставки осталось обработать X%», не прячет причину. Минимальная work-доля —
один явный компромисс против спама коротких runs; owner может выбрать instant
cash-out после первой иглы, но тогда потребуется отдельная проверка дохода/минуту.

Полностью drained партия допускает расчёт даже при 0 найденных: остаётся service
reward за реально обработанное сырьё, но это не recovery success. Полный flow
не закрывает инспекцию редкости автоматически: есть краткое состояние «готово
к завершению», затем явный Finish. Rare collection не нужна для needle-perfect.
До eligibility
разрешены Pause/выход из приложения с сохранением работы; отмена без находок
не приносит денег. **Secured находки не конфискуются**: аварийный выход сохраняет
ту же партию, а не превращает её в бесплатный reroll. При >120 active seconds
с хотя бы одной secured находкой можно завершить без work gate; время не даёт бонуса.

Когда finish доступен, открывается компактная панель:
«Гарантировано X кр.; ещё N не сохранено; бонус полного clear +15 не получен».
Действия «Продолжить» и «Завершить за не менее X». Удалённый остаток и missed
показаны отдельно. Никакого таймера, обнуления уже сохранённых находок или
принудительного streak-рискования. Риск — упустить оставшийся доход/редкость и
потратить ещё время; случайный miss другой иглы не отнимает secured value.

Подтверждение закрывает новый intake, отменяет finger, замораживает размер lot.
Уже поданное доходит до терминалов. Для drain UI явно разрешает временно включить
belt/packer; состояние magnet сохраняется, уже MISSED не возвращается чудом.
Необработанное в intake считается discarded, **не** увеличивает C. Preview —
гарантированный минимум по OUTPUT и уже RESIDUE; доплата возможна после drain.
Нельзя показывать pending иглу как уже оплаченный доход.

Обычный drain ожидается за несколько секунд; hard diagnostic bound — 30 active
seconds для до 5 игл при предложенных timings. Watchdog не выдаёт фиктивный OUTPUT:
при баге нужен recovery/checkpoint или явный безопасный аварийный итог по уже
secure IDs с отдельным error disposition. Такой случай — баг exit gate, не
механика баланса. Settlement/rare award/progression фиксируются **ровно один раз**.

Full bonus — умеренная доплата, не jackpot. Streak/perfect multiplier пока не
нужен: он делает одну ошибку чрезмерно болезненной. Cash-out должен иногда быть
разумным: например, хватает на инструмент, а оставшаяся зона трудная.

## 4. Main currency

| Название | Оценка |
| --- | --- |
| Factory / Production Credits | Понятно, масштабируется, немного безлично. |
| Scrap Credits / стоимость утиля | Тематично, но делает иглы/упаковку похожими на мусор. |
| Bales / стоимость тюков | Хорошо для соломы; плохо для будущих материалов. |
| Tokens / жетоны | Легко спутать с редкой находкой/premium currency. |
| **Кредиты мастерской** | Рекомендуется: оплата реальной работы, пригодна для всей фабрики. |

Одна основная валюта, HUD «120 кр.» рядом с маленьким ledger/мастерской знаком.
Никаких gems, энергии, IAP, долга, курса валют или real-money mechanics.
Первый профиль: wallet=0, базовая Stage 2 линия принадлежит игроку. Работы бесплатны:
нет SupplyCost, счетов за электричество, обязательного upkeep или штрафа за время.

## 5. Reward formula

Считаем три строки: **сохранённый металл + работа с сырьём + полный clear**.

```text
Reward = floor(Sum of secured needle values + ServiceFee * C² + PerfectBonus * P)
Ordinary: needle value = 15; ServiceFee = 30; PerfectBonus = 15.
Challenge (1 needle): needle value = 65; ServiceFee = 25; PerfectBonus = 20.
```

- Base Reward: сумма объявленных V_i только по eligible OUTPUT IDs этой партии.
  Ordinary допускает любой корректный capture; optional precision требует
  объявленного ручного извлечения. Нарушенное условие не даёт premium 65.
- Completion component: C — processed straw work units / исходные work units.
  Пока каждый patch = 1 unit; учитываются terminal RESIDUE IDs, не площадь
  экрана, Box2D mass, свайпы, feed arms или pending belt. Discarded даёт 0.
- Recovery/Quality: пропущенная/не соответствующая условию игла даёт 0 своей
  стоимости; повторно умножать
  всю выплату на recovered/N нельзя. В Stage 3 нет damage/q_i качества.
- Optional bonus: P=1 только для perfect clear, включая объявленный способ
  извлечения в precision recipe; иначе 0. No timed/skill/streak
  multiplier. Редкость начисляется отдельно, не увеличивает credit payout.

C² сохраняет идею старого proposal: последние участки оплачиваются лучше первых.
Игроку не нужно вычислять квадрат: панель показывает текущую сумму service reward,
а results содержит всего три строки. Если эта зависимость непонятна owner,
альтернатива — линейная service fee; сравнить short/full traces до выбора.

Округление один раз в конце, валюта целочисленная; расчёт из целых work counts
можно сделать без денежных float. Значения recipe известны до старта и не меняются
из-за upgrade/rotation во время партии. Награда монотонна по secure IDs и C:
внимательный медленный игрок не теряет деньги из-за времени. Upgrade ускоряет
или меняет работу, а не молча умножает цену той же иглы.

С ростом сложности открываются новые объявленные ценности/условия, а не бесконечный
level multiplier. Prices привязаны к измеренному earned credits/active minute.
Никакого «экономика доказанно exploit-proof» до реальных owner traces.

## 6. Rare finds

Рекомендуем физические **заводские медальоны/эмалевые знаки**, с характерной
чеканкой. Stage 3 — один латунный круглый медальон. Серебристый многогранный знак
и эмалевая звезда с платиновым ободом — следующие content варианты, не три валюты.
Круг/полигон/звезда, рельеф и цвет отличают их от длинной острой иглы.

| Редкость | Stage 3 | Поздняя гипотеза среди rare drops |
| --- | --- | --- |
| Латунный медальон | 100% rare находок; 1 печать | 80%; 1 печать |
| Серебристый заводской знак | Позже | 18%; 3 печати |
| Эмалевая звезда | Позже | 2%; 8 печатей |

Первый демонстрационный rare ticket — после 2 полных эквивалентов обработанной
работы, в следующем lot. Затем **12% на eligible work-roll**, максимум 1 предмет
на manifest. Каждый новый roll требует ещё 1 полного эквивалента работы;
partial C суммируются. После 7 eligible попыток без secured rare следующая
гарантирована. Счётчик сбрасывается при сохранении, не при скрытом spawn.
Если все предметы сохранены, ожидаемый интервал с pity около **5.34 eligible
work-rolls**; это расчёт вероятности, не частота human finds.

Work credit и pity durable; выдача нового manifest тратит ticket один раз.
Seed/placement/rarity фиксируются при старте. Abort/Next/process restart не
создают новый бесплатный roll; hidden spawn не переезжает вслед за пальцем.
Если guaranteed редкость оставлена, pity/introGuarantee остаётся гарантированным
для следующего eligible lot. До первого демонстрационного ticket random rare
не выдаются. Unspent work credit ограничен одним roll (320 units); после
settlement добавляются реальные processed units, не число завершений. Eligible
неудача увеличивает pity один раз при закрытии manifest; нет бесплатных попыток
на каждый запуск. Редкость не гарантируется в последнем клочке и не лежит всегда у mouth.

Медальон скрыт теми же straws; рекомендуемый диаметр 0.45–0.60 m, примерно
30–40 px на Samsung, заметно шире игольного shaft и короче needle axis.
Не locator-glow. До существенного раскрытия — приглушённый материал. После >60%
открытого силуэта — polished brass, мягкий motion-dependent shimmer; после >90%
выразительный payoff. Это отдельная area-coverage эвристика для компактного
объекта, не использование 31-point needle axis как площади монеты.

Извлечение — короткое подтверждённое поднятие/drag в inspection tray после
открытия >60% и попадания в неперекрытый участок. Не blind tap, не tilt-to-collect.
Domain eligibility считается в координатах simulation; рендерный highlight
не выдаёт inventory. Drag использует forgiving touch target, не подсветку сквозь
солому. Тот же filtered tilt имеет спокойный sensor fallback, без новых shaders.
Проверка открытого участка предшествует воздействию нового drag: касание само
не превращает закрытый предмет в допустимую находку в том же input event.

Латунь не захватывается обычным magnet: предмет физически подают/поднимают вручную.
Если прошёл по линии, остаётся в доступном residue inspection pocket **до finish**,
откуда можно забрать его тем же действием. Это не автоматическая награда по spawn.
При cash-out secured medallion сохранён; непринятый остаётся lost в итогах.
Подбор закрывается при окончательном finish, с понятным предупреждением после
того, как игрок уже видел предмет. Скрытую редкость не выдаём преждевременно через HUD.

После secure — краткий lift в лоток, название/силуэт «Латунный медальон · +1 печать».
Малый haptic уважает настройки; отдельный satisfying sound и несколько частиц
добавляются позже/если дёшево, не как новый audio framework. Нельзя ослеплять
игрока вспышкой или превращать редкость в обычную иглу другого цвета.

## 7. Rare currency

**Печати реставратора** — один gameplay-only счётчик, связанный с коллекцией
заводских находок. Поднятый предмет остаётся в альбоме; расход печати его не
уничтожает. Дубликат даёт ту же 1 печать, не cash jackpot. Нет обмена на credits,
магазина за реальные деньги, отдельного gold/platinum wallet или платного pity.

Stage 3: одна опциональная покупка за **1 печать** — латунная именная табличка
на factory frame. Первый медальон сразу имеет понятное применение. Позже:
эмалевые корпуса/темы за 3–6; коллекционная витрина за 6–10; необычные sidegrade
modules/спецработы за 8–15. Последние числа требуют проверки drop cadence.
Модули могут давать другой способ игры, но не лучший обязательный магнит или
множитель дохода. Все необходимые feeder/queue upgrades оплачиваются credits.
Игрок без удачи редкого дропа обязан дойти до automatic feeder тем же основным путём.

## 8. Automation progression

| Ступень | Ответственность игрока | Что заработанная покупка снимает |
| --- | --- | --- |
| 0: ручная подача | Разбирает/подводит всю кучу пальцем | Ничего; Stage 2 baseline без idle suction. Downstream уже работает. |
| 1: инструмент и mouth | Управляет направлением и выбирает участки | Меньше повторного сгребания и ловли материала у края. |
| 2: contact assist | Прикасается/ведёт инструмент | Локальный assist помогает во время контакта; отпускание останавливает помощь. |
| 3: metered paddle | Удерживает широкий рычаг; останавливается ради поиска | Убирает длинные повторные свайпы; дозировка ещё под рукой игрока. |
| 4: scan-pass feeder | Выбирает участок и запускает один механический проход | Механизм сам проходит выбранную полосу; не подаёт всю кучу бесконечно. |
| 5: automatic feeder | Выбирает работу/режим; может перехватить ручной контроль | Самостоятельно подаёт обычную партию до конца; игрок больше не обязан сгребать каждую порцию. |

Каждый привод двигает сырьё ограниченным физическим контактом/усилием в intake и
передаёт его через тот же mouth. Не заменять физическую fantasy timer-only
teleport или присваиванием OUTPUT. Ни один приобретённый механизм не должен
подавать материал вне выбранного режима или при закрытом gate/глобальной Pause.

Feed, conveyor, magnet, buffer, packer растут через конкретные проблемы:
feed — утомительные движения; conveyor — выбор осмотра/потока; magnet — возврат
случайного miss; buffer — короткая очередь; packer — блокировка входа во время
ejection. Baseline downstream safety обязателен до любых покупок. Не вводить
случайные jams/износ, чтобы вынуждать игрока постоянно нажимать «починить».

Manual остаётся selectable даже с full feeder. Auto не включает auto-next.
Игрок может остановить gate ради ручной редкости, либо пропустить её и закончить
работу. Добровольные manual-feed challenge поддерживают значение рук; обычные
партии не обесценивают automation обязательным кликом каждые пять секунд.

## 9. First purchases

Все цены — гипотезы для ordinary payouts порядка 49–90 кр. M = MUST; S = SHOULD
при небольшой стоимости реализации. S-покупки не prerequisite для M-лестницы.
Базовые conveyor/magnet/packer в магазине показываются «В мастерской», не ценником.

| Покупка / scope | Цена | Prerequisite / открытие | Почувствованная проблема → функция и изменение gameplay | Видимое изменение |
| --- | ---: | --- | --- | --- |
| 1. Feed rake / M | 30 кр. | Первый settlement | Finger делает много коротких перемещений → переключаемая rake с контролируемой рабочей кромкой. Жест переносит группу, а не даёт +income. | Узкий металлический гребень/держатель, видимый active tool. |
| 2. Guided feed mouth / M | 65 кр. | Rake | Нужно догонять материал у углов → пассивные сходящиеся направляющие только у mouth. Никакой тяги через весь стол. | Две прикреплённые боковые направляющие/воронка. |
| 3. Contact oscillator / M | 120 кр. | Guided mouth; 2 work equivalents | Приходится снова разрыхлять локальную зону → assist движет контактный участок, пока игрок работает инструментом. Без контакта нет автоподачи. | Малый eccentric drive и локальный ход подпружиненной кромки. |
| 4. Conveyor inspection clutch / S | 90 кр. | 2 settled работы | Быстрый поток неудобен для осмотра редкости → режимы Inspect/Flow, видимый slower inspection window. Это выбор, не Speed II. | Двухпозиционная муфта/рычаг; маркировка belt mode. |
| 5. Magnet recall diverter / S | 100 кр. | 3 работы или первый miss | Случайно выключенный magnet теряет металл → один ручной возврат из missed pocket в intake на иглу до settlement. Тот же ID, нет второй награды. | Возвратный лоток/рычаг с физически непрерывным коротким handoff. |
| 6. Three-slot buffer cassette / M | 140 кр. | 5 secured игл | Вторая игла останавливает upstream → 3 waiting slots вместо 1, отдельный active packer. Реальная очередь/свободные гнёзда видны. | Съёмная трёхгнёздная кассета в том же корпусе. |
| 7. Metered paddle / M | 220 кр. | Contact oscillator + cassette | Длинные повторы свайпа утомляют → удержание крупного рычага приводит локальный дозирующий paddle; release сразу останавливает intake assistance. | Шарнирный paddle, привод и удобный hold control. |
| 8. Packer receiving cradle / S | 180 кр. | Cassette; 8 secured игл | Во время ejection приём ждёт → отдельный waiting pocket для следующей иглы. Один jaw cycle остаётся одинарным; безопасно перекрываются загрузка/выход. | Индексируемый приёмный карман перед jaws. |
| 9. Scan-pass feeder / M | 360 кр. | Paddle + cassette; 2 успешные assisted работы | Удержание рычага занимает руку → выбранный механический проход одной полосы с остановкой в конце. Нужен выбор следующего участка. | Поперечная каретка/винтовая направляющая с limit stops. |
| 10. Automatic feeder governor / M | 480 кр. | Scan-pass + cassette; 3 успешные assisted работы | Всё ещё нужно запускать полосы → автономный ограниченный intake drive для текущей работы, с downstream backpressure и ручным override. | Полный ход каретки и видимый governor/датчик свободного pocket. |
| 11. Brass nameplate / M, cosmetic | 1 печать | Первый secured medallion | Хочется выставить находку/персонализировать цех → декоративная factory табличка, без влияния на yield. | Небольшая латунная пластина на frame, не новый HUD badge. |

Успешная assisted работа для learning gate: все её обязательные иглы OUTPUT,
C>=75%, без settled miss. Не требуется perfect и редкий drop. Work equivalent
суммируется из processed units; ранние выходы не обнуляют обучение.
Все gates объясняются заранее. Пакер уже auto-cycle: нельзя продавать существующую
автоупаковку ещё раз. Receiving cradle меняет capacity/handoff, а не доступ к jaws.

M-credit путь до auto стоит **1,415 кр.**. При иллюстративных 70 кр./работу и
покупках по мере доступности: rake после 1, mouth после 2, oscillator после 4,
cassette после 6, paddle после 9, scan-pass после 14, auto после 21 работы.
Это арифметический сценарий, не обещание при любых early exits/ошибках.
Открытие auto — покупка и learning gates, а не скрытый wall-clock timer.
При достижении affordability новый корпус появляется в той же module scale;
camera не zoom-out ради upgrade. Новые ports/рамы проверяются в обеих ориентациях.

## 10. Difficulty progression

Stage 3 разделяет трудность поиска и трудность обслуживания потока. Меняется
один параметр за раз. Инструмент помогает управлению, но не снижает раннюю
маскировку иглы, не включает сквозной блик и не меняет принятый размер.

| Stage 3 | Later stages |
| --- | --- |
| 3 разнесённые иглы, локально неоднородный cover | 4–5/специальные количества под throughput и другие streams |
| Один optional challenge с 1 ценной иглой, более сложными углами/перекрытиями, manual feed | Metal decoys, загрязнения, разные needle variants; изменение размеров только после отдельного визуального решения |
| Очередь, выбор ручного/assisted режима, допустимый early finish | Quality/damage, разные свойства материалов, multi-line routing |
| Один rare medallion | Несколько rarities, особые collection contracts |
| Условия на job card известны до старта | Timed/zero-miss contracts, specialization/исследование |

Для challenge не увеличивать одновременно плотность, размер партии, скорость
и ограничения. Первый challenge: тот же размер/плотность стола, другая
достижимая cover-композиция и 1 дорогая игла. Никакого compulsory fail grind.
Первые обычные работы учат разным участкам, не скрытым подстройкам сложности.
Последние проценты full clear должны занимать несколько осмысленных движений,
а не минуты гонки за одним patch. Если хвост длинный — исправлять geometry/
контроль до увеличения reward.

## 11. Batch / contracts

Рекомендуется **Factory Jobs**, маленькая очередь работ без полноформатной
contract system. До знакомства с линией «Следующая работа» выдаёт ordinary.
После 5 settled работ и >=9 secured игл рядом может появиться добровольный challenge.
Дополнительно он требует один пропуск: 3 полных эквивалента обычной работы
(960 реально processed units). Накопить можно максимум один пропуск; он тратится
при выборе challenge manifest, не возвращается при отмене/early finish. Это
доступ к отдельной работе, не новая валюта и не таймер ожидания. Прогресс к следующему
пропуску виден только в job details. Без пропуска ordinary A/B остаются доступны.
Первый пропуск тоже зарабатывается работой, не выдаётся за открытие меню.
Ordinary card имеет выбор доступного cover-profile A/B; второе место занимает
optional challenge. Одновременно максимум **2 job cards**, без ежедневных deadlines, случайного
reroll shop, мирового календаря или трёх currencies за каждый контракт.

После покупки contact oscillator становится доступен ordinary cover-profile
«Перекрёстная куча»: 3 иглы, 320 patches, более разнесённые доступные позиции и
перекрытия, V=18 вместо 15, ServiceFee=30, PerfectBonus=15 (full=99 кр.).
Игрок выбирает этот profile либо базовый ordinary; density/needle size не
меняются одновременно. Так более сложная работа есть в минимальном loop без
сразу целой системы контрактов. Ценность 18 — гипотеза, а не скрытый level bonus.

Card содержит тип, N, ценность обычной иглы/полную возможную выплату, примерный
профиль сложности, режим подачи и факт доступности rare ticket **без раскрытия
результата hidden roll**. Никакого location hint. Precision challenge требует
**ручного извлечения иглы из INTAKE** в приёмный карман metal/packer, с >=70%
domain uncovered axis и попаданием в открытый участок. Использует ту же
подтверждаемую lift/drag операцию, что collectible, с другим silhouette query.
Highlight по-прежнему начинается >60% и не является разрешением извлечения.
Conveyor/magnet/packer остаются физически работоспособными; обычная auto-capture
этой иглы не выполняет заранее объявленное ручное условие challenge. Награда
65 и perfect требуют manual provenance; service pay остаётся за материал.
Для такой работы выбран Manual feed и закрыта покупная автоподача.
Если игрок подал цель в линию, UI показывает невыполненное условие, не сохраняет
дорогую награду как будто challenge выполнен. В baseline ordinary ничего этого нет.
Итог различает physically recovered и METHOD_MISMATCH: игла не исчезла,
но не выполнен метод этого заказа. UI никогда не обещает premium payout до qualification.

При ручном извлечении единственной иглы разрешён early finish даже при C<25%:
65 кр. — честная выплата за выполненную цель, но без обработки и perfect bonus.
Её нельзя повторять бесконечно подряд: следующий challenge требует ещё 960
ordinary work units. Проверять доход на полном цикле ordinary → challenge,
а не только на самом удачном одиночном поиске. Этот work gate, условия выбора
и расход пропуска входят в save, только если optional challenge реализуется.

Без ручного условия одиночная ценная игла при том же 320-patch bulk flow —
не честная сложность: premium challenge может стать лучшим фармом. Если общий
pickup/provenance стоит дорого или раздражает, отложить весь challenge, а не
ввести дорогую одиночную auto-recipe с фиктивной сложностью.
Ordinary всегда есть; challenge не открывает единственный доходный путь.

Выбор card создаёт один durable manifest. Повторное открытие меню/rotation не
меняет offers/roll; невыбранная card не генерирует предметы и бонусы. После
settlement выбирается следующая работа или Upgrade; покупки действуют со
следующего manifest, не меняют старые ценности посреди партии.
Recipe имеет стабильный ID и параметры: needle manifest, material work units,
cover profile, values/service/perfect bonus, allowed feed modes и unlock conditions.
Позже можно добавить новые contract conditions через эти данные; сейчас не нужен
универсальный quest framework или генератор сюжетных заданий.

## 12. UI / result screen

Основной экран содержит **wallet**, **Secured 1/3**, тонкий material progress
и компактный cash-out минимум при eligibility. Recovered/Missed/Packaged для
диагностики остаются в i drawer/подробностях, а не четырьмя бухгалтерскими HUD cards.
Единственный «следующий инструмент: 30 кр.» доступен в компактной upgrade вкладке.
Редкость — краткий toast после secure; второй currency counter раскрывается в
shop/collection, без постоянного баннера в search field.

Run/Pause, gate и текущий feed mode имеют явные названия/иконки, не только цвет.
Hold paddle — крупная удерживаемая область, без tap-spam. Portrait bottom console
и landscape side console остаются отдельными компактными layouts; новые элементы
не перекрывают фабрику и не уменьшают search field. Sensor fallback не ухудшает
права на находку. Results/shop panels ставят simulation на Pause.

Пример results обычной perfect работы:

```text
СОХРАНЕНО                 3 / 3
МАТЕРИАЛ                 100%

Металл                   +45 кр.
Обработка                +30 кр.
Полная очистка           +15 кр.
ИТОГО                    90 кр.

Редкая находка            Латунный медальон · +1 печать

[Следующая работа]        [Улучшить]
```

Для early результата: «1/3 · 25% · 16 кр. · Осталось 2 иглы», без красного
«проигрыш». Для bad batch: «Металл не сохранён · Обработка +30», с одной полезной
причиной/подсказкой, не экраном штрафов. Details раскрывает discarded/missed и
receipt, но по умолчанию скрыто. 0–1 rare row; никаких множителей ×1.1 ×1.2 ×1.3.

Result уже содержит durable receipt. Нажатие «Следующая», «Улучшить», закрытие
экрана/повторный запуск не платят снова. Кнопка Factory/Continue возвращает к
мастерской, если нужен просмотр purchased modules; отдельного travel screen нет.
Feedback: маленький металл-hit/перенос в secure pocket, ритмичный jaw cycle,
один сдержанный cash-in count-up после settlement. Никакого confetti на каждый
patch. Packer haptics группируются, чтобы 3 быстрые иглы не создавали вибрационный
шум; каждое OUTPUT остаётся отдельным economic event даже при объединённом эффекте.

## 13. Save schema proposal

Минимальный **versioned local profile**, с первого выпуска `schemaVersion=1`.
Нужны: wallet, purchased upgrade IDs, unlock IDs, progression/work counters,
rare currency/collection/pity, настройки, content/generator version и exactly-once
receipt. Ни cloud/account, ни encrypted premium wallet. Формат JSON — предложение,
не реализация и не выбор новой serialization dependency.

Пример согласованного checkpoint: две ordinary perfect работы дали 180 кр.,
куплена rake за 30; третья активна, 80/320 patches обработаны, одна игла OUTPUT.
Эта игла ещё в escrow, поэтому wallet остаётся 150.

```json
{
  "schemaVersion": 1,
  "contentVersion": 1,
  "revision": 12,
  "credits": 150,
  "restorerSeals": 0,
  "ownedUpgrades": ["feed_rake"],
  "unlocks": ["ordinary_v1"],
  "progress": {"settledJobs": 2, "securedNeedles": 6, "processedWorkUnits": 640},
  "rare": {"workCreditUnits": 0, "pityFailures": 0, "introGuaranteePending": true, "collectionIds": []},
  "settings": {"haptics": true, "feedMode": "manual", "theme": "workshop"},
  "nextBatchSequence": 4,
  "lastSettledBatchSequence": 2,
  "activeBatch": {
    "batchSequence": 3,
    "recipeId": "ordinary_v1",
    "generatorVersion": 1,
    "seed": "42",
    "phase": "active",
    "manifest": {"strawUnits": 320, "needleIds": [320, 321, 322], "needleValues": [15, 15, 15], "serviceFee": 30, "perfectBonus": 15, "rareTicket": "intro-guaranteed", "rareId": 323},
    "processedStrawIdRanges": [[0, 79]],
    "criticalRoutes": {"320": "output", "321": "intake", "322": "intake", "323": "intake"},
    "bufferIds": [],
    "packerId": null,
    "securedRareIds": [],
    "activeSeconds": 25,
    "machineEnabled": {"feedGate": true, "conveyor": true, "separator": true, "packer": true}
  }
}
```

Manifest также фиксирует конкретную collectible definition/value и snapshot
recipe параметров/owned capabilities; сокращённый пример показывает основные
поля. Signed Long seed можно сериализовать десятичной строкой, не терять точность
в JSON viewers. Work counts/currency — integers; unknown upgrade IDs не дают бонус.
Настройки FPS/debug не являются economic state.

**Не сохраняем poses/velocities всех straw bodies.** Terminal straw IDs можно
упаковать в bitmap/ranges; это учёт переработки, а не физический save. После
process death та же версия generator восстанавливает исходный manifest:
processed/discarded/secure IDs не появляются снова, оставшийся intake переукладывается
в канонические позиции. Pipeline straw, ещё не RESIDUE, возвращается в intake
без начисления C. Critical items сохраняют logical route/queue и минимум route
progress; они не должны дублироваться при восстановлении buffer/packer.
INTAKE critical pose тоже может восстановиться канонически.

Rotation/background в живом процессе по-прежнему сохраняют **точное текущее
состояние Stage 2**, без переукладки. Cold process recovery честно сообщает
«Незавершённое сырьё переуложено; находки и заработок сохранены». Знание seed/
координат после перезапуска не считается античитом; повторной выплаты нет.

Save checkpoints: выдача manifest/ticket, secure critical item, завершение
разумного work блока, покупка, settlement и уход в background. Для crash до
последнего checkpoint возможна потеря нескольких секунд неплатной работы,
но не durable купленных улучшений/уже сохранённых находок. Полный physics snapshot
и запись файла каждый frame не нужны.

Batch ID и awarded status входят в **одну транзакцию** с wallet, progression,
rare award и receipt. Покупка — debit + owned ID в одном snapshot. Repeat tap,
crash между results и Next, replay старого UI command не меняют баланс второй раз.
Одна активная партия и монотонный sequence позволяют compact receipt/high-water
mark вместо бесконечного списка каждого проданного patch.

IO на worker, immutable snapshot и подтверждение durable записи, не blocking GL.
Запись во временный файл в том же каталоге → безопасная замена + last-good backup;
ошибка записи сохраняет pending transaction и позволяет retry, без повторной
выдачи денег. Не обходить сломанный save бесплатным новым profile. Validation:
неотрицательные разумно ограниченные суммы, уникальные IDs, корректный queue/recipe.

Migration: чистые последовательные шаги v1→v2→v3 с fixture tests и backup исходника.
Schema новее клиента не перезаписывается старым билдом. Content migration отдельно:
для активного manifest сохранить generator version или завершить его безопасным
однократным partial receipt по durable secure IDs. Нельзя молча заменить seed/
rare roll при загрузке. JSON из examples не обязан существовать в accepted Stage 2.

## 14. Offline model

**Рекомендация: Stage 3 cash offline income = 0**, даже после покупки первого
full feeder. Сначала подтвердить, что active jobs, cash-out и upgrades интересны
и их rates устойчивы. ROADMAP «initial offline model» трактовать как определённую
политику/eligibility для следующего slice, а не обязанность сейчас выдавать деньги.
Это scope-решение требует owner approval; ROADMAP пока не переписан под него.
Background не запускает physics, авто-next или скрытое выращивание редкостей.

Минимальная поздняя гипотеза, если owner затем разрешит passive модель:

- Только после full feeder и 3 успешно проведённых auto jobs.
- Только «переработка складского сырья», без perfect/recovery/rare/challenge bonus.
- Один складской allowance после активной завершённой работы; между такими
  работами максимум 30 кр. Повторные Home/resume не пополняют allowance.
- Cap времени 10 min, rate = 10% измеренного service-only дохода обычной auto
  работы. Нельзя измерять его на challenge/jackpot или брать рекорд одного frame.

```text
OfflineCredits = min(remainingAllowance,
                     floor(0.10 * ordinaryServiceCreditsPerMinute * min(idleMinutes, 10)))
Allowance cap = 30 credits; refill only after a completed active ordinary job.
```

Пример: service 30 за auto job в 60 s → 3 кр./offline min → максимум 30 кр.
за уход; меньше одной средней активной работы, не хватает на новый основной
механизм. Уже выданное вычитается из allowance; unused progress учитывается
однократно, не округляется заново при каждом resume. Clock deltas clamp к
неотрицательному cap; назад часы не дают деньги. Локальная игра не обещает
защиту от ручной правки часов/save. Сервер ради этого не нужен.

Эта формула **не входит в рекомендуемый MUST Stage 3**. Если даже небольшой
offline доход побуждает закрывать игру вместо работы руками, оставить его
отключённым, а не повышать цены/добавлять upkeep, чтобы компенсировать пассивность.

## 15. Anti-frustration rules

| Ситуация | Предлагаемое правило / проверяемый oracle |
| --- | --- |
| Невозможный seed | Проверять каждую initial cover/границы/доступность finger; needle centre margin >= pusher radius + половина длины + запас, ориентир 1.42 m. До выдачи manifest bounded placement attempts и известная достижимая fallback-композиция, сохраняя ticket/IDs/ценности. Сотни seeds + native drain tests, не гарантия по одной картинке. |
| Несколько игл приходят одновременно | FIFO, один busy packer/capture owner, backpressure; ни потери, ни бесплатной parallel упаковки. Проверки off/on каждого узла и five-item burst. |
| Слишком длинная работа | Cash-out после минимальной работы, все keys снимают C gate; >120 active s emergency partial exit с secure item. Никакой бесконечной обязанности добить perfect. Repair intake layout/stuck geometry, не добавлять скрытый highlight hint. |
| Rare под недоступной соломой | Безопасная placement зона, доступный физический путь; при проходе по линии есть residue inspection pocket до finish. Нет automatic rare award, недостижимая placement — generator bug. |
| Bad luck | Guaranteed intro + work-based rolls + pity; pity не сбрасывается на невидимом/lost spawn. Нет gating основного прогресса печатями. |
| Случайный missed | Объявленный switch state, без surprise failure; сохранённые другие иглы/processing pay остаются. Optional recall upgrade возвращает один ID один раз, без duplicate payout. |
| Не хватает денег | Бесплатные обычные работы, service payout за full processing даже при bad recovery; отсутствуют обязательные entry/upkeep charges и задолженность. |
| Packer выключен при cash-out | Явно подтверждённый bounded drain belt/packer; учитывается текущий magnet state. Нет вечного waiting-results. |
| Force-stop / rotation / повторный Collect | Durable manifest/receipt, queue/secure IDs; exact live-state retention при rotation. Restart не создаёт новый rare ticket, результаты не начисляются дважды. |
| Short-run spam | Work eligibility, квадратичная service component и full bonus; rares/learning gates по работе, не числу Next. Проверить credits/min реальными traces: формула сама по себе не доказывает отсутствие exploit. |

Если rapid first-needle exits выигрывают все обычные full runs, сначала менять
placement distribution/work gate и долю service/perfect payment, затем prices.
Не решать это раздражающими loading задержками, forced ads, cooldown, налогом
за выход или потерей всех прошлых денег. Иногда выгодный early выход — цель
дизайна; почти всегда лучший early выход — ошибка баланса.

## 16. Numerical examples / balance targets

Все строки используют формулу раздела 5. Округление вниз после суммы.

| Работа / результат | Secured | C | Металл | Обработка | Bonus | Итого |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Плохая ordinary, все иглы missed | 0/3 | 100% | 0 | 30 | 0 | **30 кр.** |
| Early после первой иглы | 1/3 | 25% | 15 | 1.875 | 0 | **16 кр.** |
| Нормальная partial | 2/3 | 80% | 30 | 19.2 | 0 | **49 кр.** |
| Нормальная, все keys, немного сырья оставлено | 3/3 | 90% | 45 | 24.3 | 0 | **69 кр.** |
| Early после всех keys | 3/3 | 60% | 45 | 10.8 | 0 | **55 кр.** |
| Полная обработка, 1 missed | 2/3 | 100% | 30 | 30 | 0 | **60 кр.** |
| Perfect clear | 3/3 | 100% | 45 | 30 | 15 | **90 кр.** |
| Полный precision challenge с manual provenance | 1/1 | 100% | 65 | 25 | 20 | **110 кр.** |

Поздний ordinary cover-profile: 3×18 + 30 + 15 = **99 кр.** за perfect.
Challenge, взятый обычным auto-capture без ручного условия: 0 contract-secured,
при C=100% только 25 кр. service pay, не 110. Это заранее объявленная другая
работа; secured premium preview не появляется при неправильном provenance.

Rare медальон добавляет **1 печать**, а не ещё 110 кр. при любом credit результате.
Расчёт таблицы, суммы M-prices и illustrative purchase trace проверен локально
как арифметика документа; это не implemented economy или proof of fun/balance.

Проверяемый пример решения: early 1/3, C=.25 за 25 active s — 38.4 кр./min;
perfect за 90 s — 60 кр./min. Early all keys, C=.60 за 45 s — 73.3 кр./min;
полная за 75 s — 72 кр./min. У игрока может быть причина закончить после keys,
но потеря full bonus/rare inspection не делает это автоматическим выбором.
**Это вымышленные timings.** Early 16 кр. за 10 s дал бы уже 96 кр./min — сценарий
для проверки при изменении feed rate или обходе work gate, а не измерение текущей
игры. При нынешних 320 patches, feed interval 0.115 s и belt transit около 5 s
обработка 80 patches сама требует порядка 14 s даже без времени поиска. Это
аналитический ориентир, не Samsung benchmark; баланс оценивается по реальным traces.
Precision early 65 кр. после одной ручной находки может выглядеть очень выгодно
в отдельном run; обязательные 960 ordinary units между такими jobs включаются
в сравнение. Время поиска не добавляет выплату и не отнимает её.

| Цель первых тестов | Ориентир, не deadline |
| --- | --- |
| Первый secure/find beat | 15–30 active s; не моментальный reveal одним broad swipe |
| Первая работа | 60–90 s; cash-out может закончить её раньше |
| Обычная manual работа | 60–100 s; P90 желательно <=120 s |
| Интервал между находками | Примерно 10–30 s, без одинаковых обязательных повторов |
| Assisted обычная работа | 45–75 s при сохранении удобного control/recognition |
| Optional challenge | 90–140 s, минимум 1 понятный результат; без времени как штрафа |
| Первый upgrade | 1–2 работы / примерно 1–3 min |
| Первый contact assist | Примерно 4 работы / 4–6 min |
| «Уже настоящая фабрика» | Rake + assist + FIFO cassette, примерно 5–9 min |
| Partial scan-pass automation | Около 14 средних работ / 12–20 min |
| Full automatic feeder | Около 21 средней работы / **20–30 min**, зависит от результатов и выбора покупок |

Wallet trace считает 70 кр. на работу; фактический average ещё неизвестен.
Optional покупки немного откладывают auto, но не нужны для его unlock. Если игрок
час не видит качественного изменения, проблема pacing, не повод вводить +10% tiers.
Цель rate проверки: full и разумный all-keys early должны быть сопоставимы;
спам first-only не должен устойчиво обгонять вдумчивые runs >20% на representative
seed set. Не скрывать unfair seeds усреднением — смотреть P50/P90 и worst cases.

## 17. Stage 3 scope / future verification

### MUST HAVE FOR STAGE 3 — рекомендуемая граница

- Два ordinary cover-profiles с 3 иглами: первый с начала, второй после contact
  assist; один генератор/материал. Immutable manifests/IDs, FIFO и настоящий
  single-cycle downstream, без потери/дублирования предметов.
- Early finish/ясный drain/receipt, основной wallet, три строки reward и results.
- Бесплатные factory jobs, минимальный upgrade panel и семь M-механизмов раздела 9.
  Видимое manual → contact assist → hold assist → scan-pass → full feeder.
- Один доступный физический медальон, work/pity, одна rare currency и одна
  косметическая покупка. Нет предметной энциклопедии из десятков вариантов.
- Versioned local profile + compact active batch recovery, atomic purchases/
  settlement, настройки. Без save физических соломинок.
- Accepted needle/art/layout/fullscreen/lifecycle non-regression, compact HUD.
- Offline **политика без cash accrual**, локальные balance measurements,
  tests/build/lint/GL smoke и физический owner review перед acceptance.

### SHOULD HAVE IF CHEAP

- Один meaningful precision challenge / вторая job card после baseline learning.
- Три S-upgrades: inspection clutch, recall diverter, receiving cradle; если
  требуют большой новой ветки simulation/art, перенести их целиком.
- Лёгкий discovery sound/малые particles; readable recipe info/collection drawer.
- Одно 4-needle variation recipe после queue validation, не новый environment.

### LATER STAGES

5-needle rich contracts, metal decoys/damage/quality, загрязнения и новые materials,
полная collection rarity ladder, редкие функциональные sidegrades, specialization,
research/prestige, несколько линий, placement/optimization framework, realtime days,
streak/timed multipliers, paid acquisition/maintenance, полноценный offline production,
cloud/accounts и production audio/art. Real-money mechanics не предлагаются.

Первый implementation slice **после отдельного approval**: только manifest +
3 needles + single-owner queue/conservation. Затем settlement + минимальный save/
cash-out; затем earn→buy rake loop; затем остальные M-assists и редкая находка.
Каждый slice проверять, не внедрять весь документ одним широким refactor.
Full Stage 3 acceptance требует игрового loop, а не только готового calculator.

Future QA oracle: для каждого утверждения заранее определить observable state:
unique item IDs/dispositions, secured value, C work counts, receipt/wallet delta,
queue occupancy, purchased capability и физическое поведение. JVM: payout
examples/monotonicity, double settlement/purchase, burst/off-on routing, seeded cover,
pity/work ticket, challenge quota/qualification, migration/corrupt/future saves
и process-recovery identity.
GL: representative batches и обе ориентации с одинаковым state/scale. Samsung:
>=10 обычных работ, несколько early exits, каждый M-upgrade до/после, rare pickup/
miss, rotation/Home/process recovery; optional challenge отдельно. FPS/jank/heat
измерять на той же density; scripted desktop time не выдавать за phone benchmark.
Локальная телеметрия без нового SDK: first-secure, C at cash-out, active seconds,
reward/min, mode, queue waits, time-to-purchase и две owner оценки — «поиск» и
«физическая возня». Число новых tests определяется реализацией, не выдумывается.

### Проверки этого design pass

Проверены UTF-8/whitespace, последовательность 18 разделов, 11 локальных ссылок,
валидность JSON-примера save, 9 payout examples, сумма семи mechanical prices
(1415 кр.) и расчёт pity interval. `git diff --check` проходит. Diff ограничен
документацией: этот design, статус design-only в AGENTS/ROADMAP/Toolkit.
Accepted tag/HEAD не изменены; builds/JVM/Samsung Stage 3 не запускались,
поскольку runtime не создавался. Числовые timings требуют будущего playtest,
а сохранение Stage 2 поведения не выдаётся за новый физический тест.

### Что сохранено/изменено относительно прошлого proposal

| Сохранено | Уточнено или изменено |
| --- | --- |
| 3 ordinary + optional valuable challenge | 3 обязательная база; difficulty не обязана линейно менять N. Challenge scope ограничен. |
| Manifest/unique IDs, same-state lifecycle, exactly-once settlement | Явные single-owner FIFO/busy ограничения обнаружены в реальном Stage 2 code. |
| Partial finish и нелинейная оплата обработки | Work gate 25% вместо 20%; без q_i, скрытого Q и скидки на уже secure иглу. Три строки results. |
| Credits + medallions + seals, work-based rarity/pity | Один rare тип сейчас; pity на 8-й eligible попытке вместо 11-й; первая косметика за 1 печать. |
| Manual → assisted → automatic, без rare gating | Не откатываем фабрику к пустому столу; existing downstream owned. Семь behavior purchases вместо перепокупки conveyor/packer. |
| Честный поиск/металл >60%, плотная естественная солома | Не смешиваем presentation highlight с domain discovery/reward; baseline magnet продолжает работать. |
| Data-driven bounded progression | SupplyCost=0, нет recovery-loan jobs; профиль/active-ID save спроектирован отдельно; offline income отложен. |

GAMEPLAY_ECONOMY_PROPOSAL.md сохранён как исторический input без перезаписи.
Этот документ пока не превращает его гипотезы в утверждённые runtime требования.

## 18. Open questions for owner

1. **Начало campaign:** оставить basic Stage 2 line owned (рекомендовано) или
   сделать отдельный pre-factory tutorial? Второй вариант расширит Stage 3;
   нельзя молча продавать/отнимать уже работающие conveyor/magnet/packer.
2. **Распознавание иглы:** ordinary factory допускает bulk feed с magnet;
   нужен ли обязательный manual discovery в основной игре, или только в
   precision challenges? Рекомендовано не менять ordinary capture rule.
3. **Cash-out:** принять work gate 25% плюс первая secure находка/все keys, либо
   разрешить мгновенный выход после первой? Рекомендован gate, без конфискации
   escrow, с проверкой коротких runs на реальных timings.
4. **Награда:** понятны ли credits + nonlinear service + perfect (90 кр. full),
   или service должен быть линейным? Нет entry cost/таймерного штрафа.
5. **Pacing:** подходит full feeder за 20–30 min, либо цель ближе к 10–15 min?
   До теста не утверждать final prices; first tool должен прийти за 1–2 работы.
6. **Rare art/currency:** медальоны + печати и одна латунная nameplate (рекомендовано)
   или буквальные gold/platinum coins? Базовая автоматизация не зависит от этого.
7. **Stage 3 scope:** согласовать семь M-механизмов/один collectible или сократить
   первый approved slice до 3 needles + earn/cash-out/save + rake? Это план slices,
   не разрешение реализовать остальные системы автоматически.
8. **Offline:** согласовать отсутствие денежного offline в Stage 3 и перенести
   accrual в отдельный будущий approval; ROADMAP wording потом уточняется явно.
9. **Process recovery:** допустима переукладка непереработанного сырья при сохранении
   ID/secure находок/денег, без восстановления каждой физической позы?

**STOP:** review документа, затем отдельное owner approval на конкретный
implementation scope. Сейчас никакого Stage 3 runtime, commit или push.

# Перенос пучков и рост фабрики — proposal v3

2026-10-06. **DESIGN / OWNER REVIEW. Не разрешение на implementation.**
Stage 3 остаётся CURRENT, не accepted/completed. APK/products-v2 не изменён.

Последующее разрешение owner: 2026-10-06 разрешён только small clump/grab pass.
Он реализуется в текущем candidate; economy tables и остальной scope ниже
остаются proposal. См. STAGE_3_REPORT.md для текущих проверок.
Toolkit: GAMEPLAY_CORE + PHYSICS_TOUCH + ECONOMY_PROGRESSION + VISUAL_ART +
UI_UX + QA_VERIFY. Роли: gameplay/physics, economy, technical art, UI/UX, QA.
Без новых plugins/services/dependencies. Без commit/push.

## 1. Owner-вектор и текущая реализация

Цель: переносить маленькие пучки руками, затем покупать capacity инструмента,
лучшие приёмники, звенья транспорта и processing machines. Фабрика физически
растёт. Полная прогрессия — ориентир **10+ часов интересной активной игры**;
endgame ещё не определён. Не превращать это в десять часов одинаковых переносов.

По чтению FactoryConfig, FactoryIntakePhysics, FactorySession, FactoryScreen,
Workshop и FactoryDiorama:

- Сейчас 120 authoritative straw patches, каждый содержит 32 нарисованные
  соломинки: 3,840 strands и три отдельные иглы. Это не независимая физика
  каждого стебля. Patch имеет body с тремя curved proxy sections.
- Generator распределяет материал по полю, создаёт pockets и patches у краёв.
  Перекрывающиеся крупные patches могут читаться круглой массой. Это не один
  authoritative объект в углу.
- Палец — dynamic pusher, **не захват**. Базовый круг радиуса .62 world unit;
  rake — поперечная головка. Следуя пальцу, он отталкивает материал контактом.
- Straw bodies не сталкиваются нативно друг с другом; rake добавляет ограниченное
  soft crowding только локально затронутых patches. Полного переплетения нет.
- Tray bounds-clamp удерживает геометрию в поле. Он предотвращает потерю, но
  заменяет wall physics и требует edge-stuck проверки.
- Feed boundary — правая граница search table, а не дальний sale/output tray.
  Текущий контакт + пересечение края + metering gate разрешают handoff. Затем
  receiver/press завершают приём: палец не переносит груз через всю фабрику.
- Release снимает pusher/feed permission; damping=18 гасит движение. Нет idle
  suction. Уже принятая масса может закончить короткий processing cycle.
- Текущий trial: loose 6/full lot, bundled 16, raw needle 3; rake 28, guides 16,
  Baler 42. Эти runtime цены не изменены данным proposal.

Сохраняем accepted needle size/highlight, multi-needle, saved wallet/ownership,
conserved mass, one settlement, lifecycle, fullscreen и screen-space scale.
Работающий Baler/Stage 2 reference не удаляем. Pacing с нуля проверяется отдельным
QA profile; заработанный owner wallet не обнуляется.

## 2. Маленькие crossing clumps

Принимаем направление: **10 templates**, каждый с 3–5 длинными тонкими стеблями,
пересекающимися нерегулярно ближе к центральной части. Не круговой веер, кисть,
параллельная щётка или канат. Это единый лёгкий физический proxy рыхлого пучка,
не обещание индивидуального распутывания каждой соломинки.

Seed выбирает template, rotation, mirror X/Y, небольшую вариацию длины/тона/изгиба.
Mirror преобразует geometry, bend и collision proxy согласованно. Stable ID и
масса неизменны; отдельная игла не приклеивается к пучку и не зеркалится с ним.

Первый trial: **24 переносимых пучка**, примерно 96 strands и 3 отдельные иглы.
10 — число вариантов, не число объектов на весь batch. Сравнить 18/24/32 на
Samsung; config должен позволять менять их без переписывания системы.
Плотность — локальные перекрытия в куче, а не 3,840 стеблей равномерным ковром.
Не утверждаем, что 24 уже достаточно: слишком открытые/очевидные seeds отвергаем.

Почему не 120 одиночных переносов: даже 2 s на перенос дают 240 s без поиска.
24 дают 48 s + поиск/results; первый batch остаётся примерно 45–90 s.
Поздние jobs увеличивают объём вместе с инструментом, не до первого upgrade.
Mass lot нормализована отдельно от числа спрайтов: новая библиотека не создаёт
денег из воздуха и не уменьшает цену одинакового объёма.

Spawn margin учитывает настоящие rotated bounds. Центральные пересечения
улучшают силуэт, но не заменяют устранение edge traps. Нет fake cover без
authoritative операции снятия. Иглы прячутся под реально двигающимися стеблями.

## 3. Захват вместо bulldozer

Touch на видимом стебле выбирает верхний ближайший clump. Damped spring тянет
его за локальную точку контакта: поворот, скольжение, ограниченные collisions,
без телепорта к пальцу. Release отпускает; инерция быстро затухает.
Reference preset сохраняет старый push/rake для сравнения.

Маленький visual маркер и удобное попадание — разные параметры. Trial:
маркер 6–8 dp, acquisition tolerance 14–18 dp вокруг **видимой геометрии**,
но capacity строго 1. Не выбирать скрытый центр большого collision rectangle.
HUD кнопки сохраняют нормальные touch targets. Маленькая точка не должна
превращать игру в экзамен по pixel-perfect попаданию пальцем.

Следующие levels расширяют collection footprint и count/mass cap. Новые clumps
попадают в held set только от реального локального контакта, не через весь экран.
Rotation/Home отменяют gesture и оставляют cargo poses/batch/wallet; повторный
touch забирает тот же груз, не создаёт копию. Иглы независимы: перенесение
visual patch не даёт concealed metal автоматически. Expose -> deliberate recovery.
<=60% highlight OFF, принятые длина/толщина и поздний металлический отклик сохранены.

### Доступность и честная сложность

- Rounded walls, shallow guides, bounded spring force/torque; proxy не шире
  рисунка настолько, что визуально свободный проход оказывается закрыт.
- Любой legal pose обратим: отпустить, перехватить видимую часть, вытащить назад.
  Умеренная уступчивость proxy допустима, автоматическая доставка — нет.
- Только mouth crossing held cargo при свободной capacity принимает массу.
  При full queue груз остаётся снаружи в доступной зоне. Gate не закрывается
  сквозь held object; bounds-clamp не загоняет стебель внутрь collider.
- Сложность: выбрать/повернуть пучок, расчистить проход, совместить размер
  переносимого груза с mouth и сохранить металл. Не безвыходные snagging traps.
- Большой tool удобен, но широкий груз требует подходящего receiver. Размеры
  и queue видны; никаких скрытых штрафов за upgrade.
- Numerical overlap repair локально возвращает доступность, никогда не
  переносит массу к продаже и не даёт reward. Нет глобального ghost mode.

Будущий oracle: fast/slow diagonal drag, long hold, edge release/regrab,
full receiver, rotation/Home while held. После release 10 s новой accepted
массы нет; committed processing может завершаться. Минимум 50 seeds с доступным
извлечением всех clumps. Эти проверки ещё не проведены для нового grab.

## 4. Пять уровней инструмента и приёмника

Рекомендация: пять **total levels**, бесплатный I + четыре paid. Если owner
имел в виду пять платных, добавить VI после review. Trial incremental price:
round(firstPrice × 1.9^(paidStep-1)); это цена перехода, не total владения.

| Grip | Max clumps | Цена | Видимое изменение |
|---|---:|---:|---|
| I | 1 | Free | Одна точка ручного захвата |
| II | 2 | 20 | Парный инструмент, две рабочие точки |
| III | 3 | 38 | Короткая поперечная гребёнка |
| IV | 4 | 72 | Более широкая головка, шарнир |
| V | 5 | 137 | Широкий ручной инструмент с читаемыми точками |

Это capacity, не feeder. Radius/mass limits уточняются по геометрии clumps.
Нельзя считать рост radius линейным ростом площади/throughput.

| Receiver | Вход / queue | Цена | Изменение |
|---|---|---:|---|
| I | Узкий, 1 clump-equivalent | Free | Простой лоток |
| II | Удобнее вход, 2 | 30 | Щёки/скосы |
| III | Шире, 3 | 57 | Малый накопитель и заслонка |
| IV | 4, быстрее cycle | 108 | Камера и видимый привод |
| V | 5, finite queue | 205 | Funnel и level indicator |

Размеры/скорость — отдельные tunables, не готовая реализация. Level I не должен
быть специально мучительно медленным. Receiver снимает ожидание/точное наведение,
не повышает цену loose straw. Baler добавляет grade; не путать с receiver level.

Единая система machine I–V: цифра/табличка + 1–5 chevrons и restrained accent.
Пятый может иметь маленькую точку как дополнительную деталь. Без зависимости
от цвета или заучивания рангов. Форма head/mouth/frame/chamber действительно
меняется; одной перекраски/галочки недостаточно.

Rake/guides/Baler ownership не конфискуется и не конвертируется молча в новый
level. Следующий implementation требует явной save migration mapping.

## 5. Транспорт и processing ladder

Предлагаемый порядок меняет Baler-first v2:

1. Один clump руками в простой receiver.
2. Grip II **или** receiver II — выбор, не обязательный налог из обоих.
3. Первый короткий conveyor module сокращает перенос.
4. Достроенный route и Baler: loose 6 -> bundled 16/full lot.
5. Separator: помощь recovery/sorting, trial needle 3 ->4.
6. Buffer: снимает уже испытанный backpressure.
7. Needle packer: 4 ->7 total, не сумма 3+4+7.
8. Later wrapper/dryer: конкретное назначение и новая grade/material.
9. Assisted, затем explicit automatic feeder; rake/grip не подменяют его.

Max grip/receiver не prerequisites всех machines. Comfort/capacity и product
processing конкурируют за бюджет. Ручная работа остаётся полезной после belts.
Transport не повышает sale price, сокращает путь и handling.

**Первый купленный участок сразу полезен:** переносит до своего доступного
конца, дальше груз перемещает игрок. Конец недостроенной линии удерживает cargo /
имеет temporary accessible tray. Не телепортирует к продаже, не уничтожает.
Не заставлять купить восемь бесполезных tiles прежде, чем belt заработает.
Узлы соединяются портами по реальной траектории; очереди finite, масса conserved.

### Stage 3 и Stage 4

Нынешняя разметка — presentation, не authoritative build grid. Search world
10×8 units; owner-визуальное «10×7 клеток» нельзя принять за готовую сетку.
Будущий размер, например 10×7, обсуждается отдельно и не меняет accepted module
scale. Conveyor tile = логическое звено; machines могут занимать 2×2/2×3 cells.

Stage 3: покупка **preset** сегментов/машин, поочерёдное раскрытие маршрута.
Stage 4: выбор клетки, placement/rotation, straight/corner tiles, connections,
machine footprints, route validation и layout save. Build Mode здесь не начинаем.

Receiver на другой стороне рабочего стола даёт понятный путь, но не движение
через HUD/неинтерактивный фон. В landscape тот же груз не должен требовать вдвое
более длинного пути только из-за orientation. Module/cargo scale стабилен.

## 6. Экономика: быстрая первая покупка, длинное развитие

Owner пример 20 ->30 за то же время трактуем как **income/time**, не повышение
цены сырья от владения пятном. Быстрее перенёс — продал больше. Цена зависит от
реальной grade/mass. Untouched material не оплачивается, settlement один раз.

Следующие цены — proposal, не runtime patch: Grip II 20, first useful transport
25, Baler 60 вместо текущих 42, если human pace оправдает повышение.
Raw ordinary 80% mass +2/3 needles =10/13, среднее 11.5.
Bundled =18/21, среднее 19.5. Без rare jackpot/bonus/idle income.

| Период | Допущение работы | Доход | Покупка | Wallet |
|---|---|---:|---:|---:|
| 0–2:20 | 2 raw batches ×70 s | 23 | Grip II 20 | 3 |
| 2:20–4:10 | 2 ×55 s | 23 | Transport 25 | 1 |
| 4:10–8:40 | 6 ×45 s | 69 | Baler 60 | 10 |
| После 8:40 | 2 bundled ×45 s | 39 | Grip III 38 или receiver II 30 | 11 или 19 |

Времена включают короткие results/transitions. Receiver-first может отложить
Baler. Цель first processing 7–10 min — hypothesis. Conveyor не ускоряет поиск
магическим multiplier; сокращение времени надо измерить после touch prototype.

Paid grip total **267**, receiver total **400**, вместе **667**. При 26 credits/min
это около 26 min накопления, не 10 h. Даже ×2 price steps сами по себе не дадут
10 h. Подняв firstPrice для этого, сделали бы мучительную первую покупку.
**Этот вариант отвергаем.** Начало должно показывать рост в первые минуты.

Контентный budget для всей 10-hour progression, пока не утверждённый:

| Active time | Причина играть | Trial mean gross/min | Gross интервала |
|---|---|---:|---:|
| 0–15 min | Ручной инструмент, transport/processing | 12 | 180 |
| 15–60 min | Связная линия, выбор capacity/grade | 24 | 1,080 |
| 1–3 h | Throughput, buffer/packer, partial help | 40 | 4,800 |
| 3–6 h | Разные продукты/работы, specialization, automation | 55 | 9,900 |
| 6–10 h | Развитая фабрика и ещё не утверждённые mastery цели | 65 | 15,600 |

Итого **31,560 gross**, не wallet и не готовая цена максимума. Machine/content
investment budget должен соответствовать реальному throughput. Tool/receiver
I–V — малая часть sinks. Если automation увеличит доход сильнее, пересчитать.
Не вводить repair taxes, таймеры или 500 identical batches ради длительности.

30–40 min до automatic feeder из v2 — исторический ориентир, не цель нового
10-hour model. Предварительно full feed automation — 3–5 h, partial help раньше.
Это отдельное решение до реализации, не новый runtime unlock. Auto feed не
выбирает Next batch/не находит скрытые needles/не даёт offline автоматически.

Нужен следующий balance oracle: tool-first / receiver-first / transport-first,
слабый/средний/сильный поиск, ранний cash-out, finite queue и human durations.
Если costly level почти бесполезен, изменить назначение/цену, не удлинять grind.

## 7. Золотая игла

Later jackpot, не опора базового income. Предлагаем **позолоченную стальную**
иглу, чтобы сохранить будущую separator fantasy; чистое золото требует отдельного
правила сортировки. Warm metallic tint после раскрытия, без раннего яркого marker.

Trial: 0.5–1% batches заменяют одну из трёх needles редкой; deterministic manifest,
не тайная четвёртая игла. 5× sale price соответствующей готовой grade:
raw 3 ->15, sorted 4 ->20, packaged 7 ->35; grades не складываются.
При 1% raw expected extra всего .12/batch. Mandatory purchases не зависят от luck.
Recovery обязательно; early finish/miss могут оставить ценность в материале.

Те же visibility/occlusion rules; special payoff только после recovery.
Restart не reroll manifest, ID conserved. Возможная bounded bad-luck protection
проектируется позже. Частоту проверять с реальным batch time. Сейчас не внедрять.

## 8. Распределение по milestones

| Этап | Включить | Не включать автоматически |
|---|---|---|
| Следующий small Stage 3 pass | 10 templates, 3–5 stems, one-clump spring grab, reversible edges, 24-clump trial | Все levels сразу, grid, новая long economy |
| После touch review | Grip/receiver I–V data/save; сначала 1–2 paid levels, measured prices | Grind одного действия на 10 h |
| Дальнейший Stage 3 | Preset transport/processing purchases, grades, ближайшие цели | Free placement/offline |
| Stage 4 | Tile build grid, connections, optimization/bottlenecks | Полный будущий контент одним проходом |
| Stage 5 | Материалы/влажность, dryer/wrapper с назначением | Пустые quality percentages |
| Stage 6 | Contracts/specialization, gold rare layer, endgame | Prestige по умолчанию/monetization |

Gold можно перенести раньше отдельным pass после стабилизации базы. Ближайшая
цель «новый продукт/машина» нужна уже в Stage 3, не ждать complex contracts Stage 6.
Читаемое изменение формы/level нужно при покупке, full art/audio polish позже.

## 9. Решения и следующий gate

Принимаем: crossing clumps/mirrors, direct manual grab, paid capacity, better
receivers, purchased transport, factory growth, two product branches, rare ×5.
Уточняем: 10 templates ≠10 cargo, пять total levels, tiny marker ≠tiny mandatory
hit target, доход/time ≠grade price. Все новые costs/durations — trials.
Отвергаем: edge traps, idle pull, чрезмерный первый batch, 10 h одинаковой работы,
бесполезные belt tiles, recolor-only levels, оплата grade без реальной обработки.

Рекомендуемый следующий implementation — **только материал и one-clump grab**.
Оставить wallet/Baler/products-v2 arithmetic, сравнивать touch без одновременной
смены экономики. Потом измерить time-to-sale, strokes/lot, поиск, throughput
после grip upgrade, receiver usability, portrait/landscape и FPS.

Owner до реализации: подтвердить clump вместо single stem, five total levels,
24-clump trial и bounded grab pass. Transport-before-Baler и feeder pacing —
следующие design решения. Endgame нужно отдельно спроектировать; он не блокирует
маленький tactile experiment.

В этом pass нет tests/build/device smoke: gameplay code/APK не менялись.
Арифметика проверена: 267,400,8.67 min,10 wallet,.12 rare extra,31,560 gross.
Новые touch/visual/pacing hypotheses ещё не имеют device acceptance evidence.

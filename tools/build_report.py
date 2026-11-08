"""Create editable Russian report and PDF from actual project evidence.
Dependencies: reportlab, pillow; use installed system fonts, no runtime web requests.
"""
import sys,re,json,html,os
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'.tools/python'))
from reportlab.platypus import BaseDocTemplate,PageTemplate,Frame,Paragraph,Spacer,Table,TableStyle,Image,PageBreak,NextPageTemplate,Preformatted,KeepTogether
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4,landscape
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

fontdir=Path(os.environ.get('WINDIR','C:/Windows'))/'Fonts'
pdfmetrics.registerFont(TTFont('Body',str(fontdir/'arial.ttf')))
pdfmetrics.registerFont(TTFont('Bold',str(fontdir/'arialbd.ttf')))
pdfmetrics.registerFont(TTFont('Mono',str(fontdir/'consola.ttf')))
pdfmetrics.registerFontFamily('Body',normal='Body',bold='Bold',italic='Body',boldItalic='Bold')
ink=colors.HexColor('#1c3546');teal=colors.HexColor('#24695f');muted=colors.HexColor('#65737a')
styles={
'body':ParagraphStyle('body',fontName='Body',fontSize=10.3,leading=15,textColor=ink,spaceAfter=10),
'h1':ParagraphStyle('h1',fontName='Bold',fontSize=25,leading=29,textColor=ink,spaceAfter=18),
'h2':ParagraphStyle('h2',fontName='Bold',fontSize=15,leading=19,textColor=teal,spaceBefore=10,spaceAfter=9),
'h3':ParagraphStyle('h3',fontName='Bold',fontSize=11.5,leading=16,textColor=ink,spaceAfter=8),
'cell':ParagraphStyle('cell',fontName='Body',fontSize=8.5,leading=12,textColor=ink),
'head':ParagraphStyle('head',fontName='Bold',fontSize=8.5,leading=12,textColor=colors.white),
'code':ParagraphStyle('code',fontName='Mono',fontSize=8.5,leading=12,textColor=ink,leftIndent=8,spaceAfter=12),
'caption':ParagraphStyle('caption',fontName='Body',fontSize=9,leading=13,textColor=muted,spaceAfter=10),
}

def generate_source():
    chapters=[json.loads(p.read_text(encoding='utf-8')) for p in sorted((ROOT/'src/main/resources/content').glob('chapter-*.json'))]
    audit=json.loads((ROOT/'docs/verification/audit-results.json').read_text(encoding='utf-8'))
    total=sum(int(s['tests']) for s in audit['junit'])
    text='''# Путешествие по истории Казахстана
Software Design Patterns · учебный проект

Бахыт Карима · Адай Аида · Казбекова Айша

Технический отчёт · 8 ноября 2026

## Introduction
Цель проекта — дать учащемуся короткий интерактивный маршрут по истории Казахстана и показать, как шесть паттернов Java образуют единую архитектуру. Пользователь создаёт профиль, выбирает режим, исследует сцену, разговаривает с жителями, выполняет задания и сохраняет итог.

Аудитория — студенты и школьники, которым полезно связать дату, место и содержание события. Образовательная игра охватывает десять глав от образования Казахского ханства до государственной независимости. Полноценную академическую историю короткий маршрут не заменяет.

Объём версии: 10 сцен, 20 вымышленных NPC, 20 вопросов и 10 заданий карты; два аватара и два режима; сохранение, продолжение, отмена, история и TXT-экспорт. Современный контур карты служит ориентиром, а не исторической границей.

Код, контент и оформление текущего технического прототипа созданы с AI-помощью Codex. Самостоятельная реализация двух паттернов каждым участником и индивидуальная защита пока не подтверждены. План ответственности отделён от выполненного личного вклада.

<!-- PAGE -->
# Main Body
## Пользовательские сценарии и сущности
1. Создать профиль и выбрать героя или героиню; просмотреть карту.
2. Изменить имя и аватар существующего профиля и сохранить.
3. Выбрать доступную главу и один из двух режимов.
4. Исследовать сцену, выслушать обоих NPC, прочитать справку.
5. В обучении получить подсказку после ошибки и повторить ответ.
6. В проверке дать один ответ на каждое задание и получить оценку.
7. Завершить успешно, раскрыть название и открыть следующую главу.
8. Перезапустить Java и продолжить с сохранёнными NPC и ответами.
9. Открыть историю, результат и экспорт выбранной попытки.
10. Отменить ACTIVE-попытку с подтверждением, сохранив прогресс.
11. Освоить десять глав и экспортировать общий итог.

| Сущность | Назначение и связи |
| --- | --- |
| Player | ID, имя, Avatar; один игрок имеет несколько Attempt |
| Chapter | Порядок, скрытый заголовок, период, география, сцена, NPC, задачи и источники |
| Attempt | playerId/chapterId, Mode, State, NPC, ответы, UUID запросов, время и passed |
| Task / TaskResult | QuizTask и MapTask; QuizResult и MapTaskResult для итога |
| PlayerProgress | Множество уникальных успешно освоенных глав и обработанных событий |

ACTIVE переходит только в COMPLETED или CANCELLED. passed хранится отдельно для COMPLETED. Отмена не удаляет прошлый прогресс. Повторное прохождение увеличивает число попыток, сохраняя число уникальных глав.

<!-- PAGE -->
# Main Body
## Содержание десяти глав
'''
    text+='\n| № | Событие | Период | Место сцены |\n| --- | --- | --- | --- |\n'
    for c in chapters:text+=f"| {c['order']} | {c['title']} | {c['period']} | {c['location']} |\n"
    text+='''
Контент загружается из resources/content через ContentCatalog. Даты, география и проверяемые факты подтверждены источниками S01–S10, S08b и S10b. Каждый NPC и каждое задание содержит sourceId; ссылки открываются в текущей главе.

До успешного завершения обзорные DTO, карта и история используют «Уровень N». В открытой сцене период и факты доступны как часть обучения. Правильные ответы не включены в DTO заданий. Неуспешное повторное прохождение не скрывает уже раскрытое название.

Сцена Кенесары относится к 1838 году; 1847 год сообщает позднейший рассказчик. Есен-Буга не появляется в сцене 1465–1466. Оренбург находится вне современного Казахстана и вынесен на вставку. Голод и война рассматриваются спокойно, через изучение сведений; численность жертв и спорные оценки не превращены в игровой балл.

<!-- PAGE -->
# Main Body
## Архитектура, SOLID и хранение
web принимает DTO и проверяет ввод; service выполняет use cases; model содержит сущности; content/task создают главы и задания; strategy/event/result выполняют специализированное поведение; storage фиксирует JSON. Браузер управляет отображением и движением, сервер — ответами, допуском, прогрессом и итогом.

SRP: отдельные ProfileService, AttemptService, ResultService и JsonGameRepository. OCP: новый подписчик или посетитель расширяет обработку. LSP: продукты Task и стратегии соблюдают интерфейсные контракты. ISP: небольшие GameRepository, ResultVisitor и ChapterCompletedObserver. DIP: GameFacade зависит от интерфейса хранилища. Ограничение: фасад пока сам собирает небольшие сервисы и подписчиков.

JSON находится вне JAR, в конфигурируемой папке data. Снимок версии 1 содержит профили, попытки, NPC, ответы, прогресс, статистику и дедупликацию. Первый запуск создаёт demo; последующие загружают сохранение. Позиция героя после продолжения — безопасная стартовая точка.

Транзакция: создать копию состояния, применить действие и подписчиков, записать временный файл, force, ATOMIC_MOVE, затем опубликовать в памяти. Краткие AccessDenied на Windows повторяются ограниченно; постоянная ошибка возвращает 503 и оставляет старый снимок. Повреждённый файл не перезаписывается: пользователь получает инструкцию восстановить или переименовать его и перезапустить сервер.

Модель доверия локальная: сервер слушает 127.0.0.1, аутентификации нет. Выбор профиля не является защитой от владельца компьютера. Принадлежность попытки, порядок заданий, посещение NPC и доступ к главе проверяются сервером; координаты Canvas не удостоверяются API. Для одной папки данных поддерживается один Java-процесс.

<!-- PAGE -->
# Main Body
## Обязательная таблица паттернов
Назначение студентов ниже — план, а не доказательство самостоятельной реализации. Все шесть паттернов созданы в Java и вызываются рабочими API-сценариями.

| Pattern | Problem it solves | Main classes/interfaces | How to demonstrate it | Responsible student |
| --- | --- | --- | --- | --- |
| Builder | Согласованное создание главы | ChapterBuilder, ChapterDefinition, Chapter | Запуск: загрузка 10 глав; неверная конфигурация отклоняется | Бахыт Карима, план |
| Factory Method | Разные продукты заданий | TaskCreator, QuizTaskCreator, MapTaskCreator, Task | Два вопроса и карта одной главы | Бахыт Карима, план |
| Facade | Координация игрового действия | GameFacade, ProfileService, AttemptService, ResultService | Начать, поговорить, ответить, завершить | Адай Аида, план |
| Strategy | Взаимозаменяемые правила ответов | AnswerCheckingStrategy, LearningStrategy, AssessmentStrategy | Одна ошибка в двух режимах | Адай Аида, план |
| Observer | Реакция на завершение | ChapterCompletedPublisher, ChapterCompletedObserver, ProgressObserver, CompletionStatisticsObserver | Успех открывает главу; неуспех учитывается | Казбекова Айша, план |
| Visitor | Статистика и экспорт разных результатов | TaskResult, QuizResult, MapTaskResult, ResultVisitor, StatisticsVisitor, TextReportVisitor | Экран результата и TXT | Казбекова Айша, план |

Два порождающих, один структурный, три поведенческих паттерна. Framework-механизмы не засчитываются как собственные реализации. Ограничения: Visitor требует обновить все visit при новом типе результата; синхронные подписчики работают только внутри снимка; Facade нужно защищать от чрезмерного роста.

<!-- PAGE -->
# Main Body
## Java: Builder, Factory Method и Strategy
ContentCatalog читает определения, реестр готовит Task, Builder создаёт неизменяемый Chapter. Нельзя обойти проверку ссылок на NPC, источники и допустимые варианты.

```java
result.add(new ChapterBuilder().definition(d)
    .tasks(d.tasks().stream().map(registry::prepare).toList())
    .build());
```

TaskCreator.prepare содержит общий процесс и вызывает переопределяемый createTask. Это отличает Factory Method от одного класса с названием Factory.

```java
public final Task prepare(TaskDefinition d) {
    // Validation of fields, options and coordinates
    return createTask(d);
}
protected abstract Task createTask(TaskDefinition d);
```

Выбранный режим сохранён в Attempt. Стратегия определяет повтор, продвижение и раскрытие обратной связи; AnswerEvaluator повторно использует сравнение с серверным ответом.

```java
var strategy = attempts.strategy(a);
var answer = strategy.check(task, optionId,
                            a.answers.get(taskId));
a.answers.put(taskId, answer);
var feedback = strategy.feedback(task, answer);
```

LearningStrategy позволяет продолжить только после правильного ответа. AssessmentStrategy продвигает после любого единственного ответа, возвращая лишь подтверждение приёма. Полная оценка появляется после finish. Сервер сам проверяет текущий taskId и optionId.

<!-- PAGE -->
# Main Body
## Java: Facade, Observer и Visitor
GameFacade координирует use case внутри repository.transaction. Завершение публикует событие только при реальном ACTIVE → COMPLETED; повтор finish возвращает прежний результат.

```java
a.complete(a.answers.values().stream()
    .allMatch(Attempt.Answer::correct));
publisher.publish(new ChapterCompleted(
    a.id, a.playerId, a.chapterId, a.passed), s);
```

ProgressObserver раскрывает главу только при passed=true, CompletionStatisticsObserver считает любой результат. Обработанный attemptId предотвращает повторное начисление. Подписчики меняют тот же кандидат, который сохранится вместе с попыткой.

```java
if (!stats.processedEvents.add(event.attemptId())) return;
stats.completed++;
if (event.passed()) stats.passed++; else stats.failed++;
```

QuizResult и MapTaskResult реализуют TaskResult.accept. Тип this выбирает перегрузку visit, а реализация посетителя определяет действие — двойная диспетчеризация.

```java
public void accept(ResultVisitor visitor) {
    visitor.visit(this);
}
var visitor = new StatisticsVisitor();
details.forEach(r -> r.accept(visitor));
```

StatisticsVisitor считает задания, правильные окончательные ответы, число ответов и ошибок. TextReportVisitor пишет вопрос, выбранный/правильный вариант, объяснение и sourceId; для карты также координаты. Оба посетителя реально вызываются ResultService для результата и TXT.

<!-- PAGE -->
# Main Body
## Тестирование и фактические результаты
'''
    text+=f'Контрольный Maven verify: {total} тестов JUnit, 0 failures, 0 errors, 0 skipped. Отдельный process_smoke.py: 60 HTTP/процессных проверок, включая настоящий перезапуск Java. Полная таблица входов, шагов, ожиданий и результатов находится в docs/TEST_SCENARIOS.md.\n\n'
    text+='''| Проверка | Наблюдаемый результат | Статус |
| --- | --- | --- |
| Все 10 глав, оба режима | 9 открытий и финал 10/10 | Passed Java/API |
| Обучение: ошибка и повтор | 3 задания, 4 ответа, 1 ошибка | Passed |
| Проверка: один неверный | COMPLETED, passed=false, история сохранена | Passed |
| Повтор UUID / finish / события | Без повторного ответа или начисления | Passed |
| Перезапуск отдельного процесса | Те же профиль, аватар, NPC, следующий вопрос | Passed |
| Ошибка записи / corrupt JSON | Откат и 503; исходный файл сохранён | Passed |
| Пустое имя, чужая попытка, закрытая глава | 400/404/409 без изменения состояния | Passed |
| Оба Visitor и продукты фабрики | Quiz/Map, счётчики и UTF-8 TXT | Passed |
| Достижимость всех NPC | BFS достигает 20 NPC с коллизиями | Passed, геометрия |
| Ручное управление и экран скачивания | Браузерный инструмент заблокирован | Not run |

Проверка геометрии не заменяет ручное прохождение сцены. Повторный process smoke после диагностики краткой блокировки OneDrive подтвердил все 60 проверок; обработка постоянного отказа записи проверена отдельным тестом.

Запуск: ./mvnw.cmd verify; python tools/process_smoke.py --java <JDK17>/bin/java.exe. JavaScript: node --check src/main/resources/static/app.js. Тесты хранилища используют изолированные временные папки.

<!-- PAGE -->
# Main Body
## Источники, инструменты и вклад
Исторические факты: Qazaqstan tarihy по главам 1–5 и 8; публикация К. С. Алдажуманова о голоде; GOV.KZ по Алаш, космическому полёту и независимости; первичный текст закона от 16 декабря 1991 в ИПС Әділет. Отдельная публикация «Вклад Казахстана в Победу» подтверждает работу тыла. Полные URL, даты доступа и ограничения приведены в docs/HISTORICAL_SOURCES.md.

Карта: Natural Earth v5.1.2, public domain; локально извлечена геометрия Казахстана. Canvas/SVG-персонажи, сцены и интерфейсные тексты созданы заново. Системные шрифты отдельно не распространяются. Статьи, фотографии, логотипы и оформление исторических порталов не копировались.

Инструменты: Microsoft OpenJDK 17.0.18, Maven Wrapper / Maven 3.9.12, Spring Boot 3.5.7, JUnit Jupiter 5.12.2, HTML/CSS/JavaScript, Python 3.14, PlantUML 1.2025.10, ReportLab и Apache PDFBox 3.0.5. Целевой Java release — 17; игра после сборки не требует сети.

Фактическая реализация новых файлов выполнена Codex с AI-помощью. План команды: Бахыт Карима — Builder/Factory Method, Адай Аида — Facade/Strategy, Казбекова Айша — Observer/Visitor. Личный код, самостоятельные прогоны и защита участников пока не подтверждены. Реальная Git-история отражает этапы этой сессии и не имитирует три недели студенческой разработки.

## Скриншоты работающей версии — Not run
Настоящие скриншоты не получены: browser host недоступен, Computer Use остановил обращение к Chrome, поскольку не смог уверенно определить URL окна. Изображения UML далее являются диаграммами, а не скриншотами. Перед сдачей команда должна пройти UI всех десяти глав и добавить реальные изображения карты, сцены, задания и результата. Это остающийся критерий отчёта.

<!-- PAGE -->
<!-- LANDSCAPE -->
# Main Body · UML 1
## Сущности и слои приложения
![Связи сущностей и прикладных слоёв](../uml/architecture.png)
Общая схема соответствует финальным Java-классам. Более подробные участники паттернов показаны на следующих схемах. Редактируемый исходник: docs/uml/architecture.puml; векторная версия: architecture.svg.

<!-- PAGE -->
<!-- LANDSCAPE -->
# Main Body · UML 2
## Builder и Factory Method
![Участники порождающих паттернов](../uml/creational.png)
ContentCatalog использует ChapterBuilder и TaskCreatorRegistry. Общая подготовка в абстрактном TaskCreator вызывает фабричный метод конкретного создателя. Исходник: docs/uml/creational.puml.

<!-- PAGE -->
<!-- LANDSCAPE -->
# Main Body · UML 3
## Strategy и Observer
![Стратегии и подписчики завершения](../uml/behavioral.png)
AttemptService выбирает стратегию по Mode, а GameFacade публикует завершение двум подписчикам. Обе реакции записываются с Attempt в одном снимке GameState. Исходник: docs/uml/behavioral.puml.

<!-- PAGE -->
<!-- LANDSCAPE -->
# Main Body · UML 4
## Visitor и результаты
![Двойная диспетчеризация результатов](../uml/visitor.png)
accept выбирает перегрузку visit для QuizResult или MapTaskResult. StatisticsVisitor используется экраном результата, TextReportVisitor — экспортом. Исходник: docs/uml/visitor.puml.

<!-- PAGE -->
<!-- LANDSCAPE -->
# Main Body · UML 5
## Ответ и экспорт результата
![Фрагмент последовательности ответа и экспорта](../uml/answer-sequence.png)
Полная последовательность: docs/uml/game-sequence.puml и game-sequence.svg. Для читаемости в отчёте приведены два фрагмента. Все проверки ответа выполняются сервером; до finish режим проверки не раскрывает правильность.

<!-- PAGE -->
<!-- LANDSCAPE -->
# Main Body · UML 6
## Завершение и согласованное сохранение
![Фрагмент транзакции завершения](../uml/completion-sequence.png)
Событие содержит ID попытки, профиля, главы и passed. Подписчики дедуплицируют attemptId. Ответ клиенту отправляется после фиксации снимка; постоянная ошибка записи оставляет прежнее состояние.

<!-- PAGE -->
# Conclusion
Технический прототип содержит десять исторических глав, Java-сервер, браузерный интерфейс, два режима, шесть интегрированных паттернов, JSON-сохранение, историю и экспорт. JUnit и проверки отдельных процессов подтверждают правила игры, открытие уровней, итог 10/10, восстановление и обработку ошибок.

Подготовлены редактируемые и отрисованные UML, README, матрица, материалы защиты, исполняемый JAR и упаковщик исходников. Состояние контрольной сборки из чистой распаковки отражено в docs/verification/RELEASE_CHECK.md. Оценку по рубрике выставляет преподаватель.

Ограничения: ручной UI-прогон, мобильный вид и реальные скриншоты не проверены из-за блокировки браузерного инструмента; push в GitHub ожидает явного разрешения после автоматического отказа публикации. Личная самостоятельная разработка, еженедельные демонстрации и индивидуальная защита команды остаются обязательными действиями. Техническая AI-реализация не подтверждает выполнение личных критериев.

# Future Work
Развитие контента: дополнительные источники, педагогическая экспертиза формулировок, новые главы и доступная навигация. Развитие архитектуры: дополнительные стратегии проверки, новые типы заданий, более гибкая сборка зависимостей фасада и хранилище для нескольких пользователей.

Адаптивные задания и исторический AI-помощник возможны только как последующее развитие: проверенные источники, явное отделение реконструкций, проверяемые ссылки и контроль ответов преподавателем. В текущей игре AI-персонажей, генерации ответов и адаптивного обучения нет.

К сдаче команда должна самостоятельно реализовать и объяснить свои паттерны, выполнить упражнения live change, добавить личные коммиты и результаты защиты, пройти интерфейс, вставить реальные скриншоты и согласовать использование AI с преподавателем.
'''
    sources=[]
    for c in chapters:
        for s in c['sources']:
            if s['id'] not in {v['id'] for v in sources}:sources.append(s)
    bibliography=''
    for start in (0,6):
        bibliography+='\n<!-- PAGE -->\n# Main Body\n## Проверенные источники'+(' · продолжение' if start else '')+'\nДата доступа ко всем публикациям: 8 ноября 2026. Факты пересказаны своими словами; изображения и полные статьи не копировались.\n\n'
        for s in sources[start:start+6]:
            bibliography+=f"### {s['id']} · {s['title']}\n{s['organization']}\n\n[{s['url']}]({s['url']})\n\n"
        if start:
            bibliography+='Карта: [Natural Earth — Terms of Use](https://www.naturalearthdata.com/about/terms-of-use/) (public domain). Технологии: [Spring Boot 3.5, Java 17+](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [Maven Wrapper](https://maven.apache.org/wrapper/), [JUnit 5.12.2](https://docs.junit.org/5.12.2/user-guide/). Ограничения источников и поддерживаемые факты описаны отдельно в HISTORICAL_SOURCES.md.\n'
    marker='<!-- PAGE -->\n<!-- LANDSCAPE -->\n# Main Body · UML 1'
    text=text.replace(marker,bibliography+'\n'+marker)
    return text.replace('\u2011','-').replace('\u2013','-').replace('\u2014','-')

def markup(s):
    s=html.escape(s)
    s=re.sub(r'\*\*(.+?)\*\*',r'<b>\1</b>',s)
    return re.sub(r'\[([^\]]+)\]\((https?://[^\s)]+)\)',r'<link href="\2" color="#24695f">\1</link>',s)

def make_table(rows,width):
    n=len(rows[0]);widths=[width/n]*n
    if n==5:widths=[width*v for v in [.12,.19,.28,.24,.17]]
    if n==4:widths=[width*v for v in [.07,.37,.19,.37]]
    if n==3:widths=[width*v for v in [.32,.48,.20]]
    if n==2:widths=[width*.25,width*.75]
    data=[[Paragraph(markup(c),styles['head' if i==0 else 'cell']) for c in row] for i,row in enumerate(rows)]
    table=Table(data,colWidths=widths,repeatRows=1,hAlign='LEFT')
    table.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),teal),('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,colors.HexColor('#eef2ed')]),('GRID',(0,0),(-1,-1),.5,colors.HexColor('#d9d9d9')),('VALIGN',(0,0),(-1,-1),'MIDDLE'),('LEFTPADDING',(0,0),(-1,-1),8),('RIGHTPADDING',(0,0),(-1,-1),8),('TOPPADDING',(0,0),(-1,-1),8),('BOTTOMPADDING',(0,0),(-1,-1),8)]))
    return table

def page_flow(text,wide):
    width=(landscape(A4)[0] if wide else A4[0])-88
    lines=text.strip().splitlines();flow=[];i=0
    while i<len(lines):
        line=lines[i].strip()
        if not line or line.startswith('<!--'):i+=1;continue
        if line.startswith('```'):
            code=[];i+=1
            while i<len(lines) and not lines[i].startswith('```'):code.append(lines[i]);i+=1
            flow.append(Preformatted('\n'.join(code),styles['code']));i+=1;continue
        if line.startswith('|'):
            rows=[]
            while i<len(lines) and lines[i].strip().startswith('|'):
                cells=[c.strip() for c in lines[i].strip().strip('|').split('|')]
                if not all(re.fullmatch(r'[-: ]+',c) for c in cells):rows.append(cells)
                i+=1
            flow.append(make_table(rows,width));flow.append(Spacer(1,12));continue
        image=re.match(r'!\[(.*?)\]\((.*?)\)',line)
        if image:
            img=Image(str((ROOT/'docs/report'/image[2]).resolve()))
            ratio=min(width/img.imageWidth,350/img.imageHeight)
            img.drawWidth=img.imageWidth*ratio;img.drawHeight=img.imageHeight*ratio;img.hAlign='CENTER';flow+=[img,Spacer(1,10)];i+=1;continue
        style='body'
        if line.startswith('### '):style='h3';line=line[4:]
        elif line.startswith('## '):style='h2';line=line[3:]
        elif line.startswith('# '):style='h1';line=line[2:]
        flow.append(Paragraph(markup(line),styles[style]));i+=1
    return flow

out=ROOT/'output/pdf';out.mkdir(parents=True,exist_ok=True)
source=ROOT/'docs/report/REPORT.md';source.parent.mkdir(parents=True,exist_ok=True)
if '--from-source' not in sys.argv:source.write_text(generate_source(),encoding='utf-8')
text=source.read_text(encoding='utf-8')
def decorate(canvas,doc):
    w,h=canvas._pagesize
    canvas.saveState();canvas.setFillColor(muted);canvas.setFont('Body',8)
    canvas.drawString(44,h-25,'HISTORY JOURNEY  /  SOFTWARE DESIGN PATTERNS')
    canvas.drawString(44,23,'Бахыт Карима · Адай Аида · Казбекова Айша')
    canvas.drawRightString(w-44,23,str(doc.page));canvas.restoreState()

pdf=out/'history-journey-report.pdf'
doc=BaseDocTemplate(str(pdf),title='Путешествие по истории Казахстана — Software Design Patterns',author='Бахыт Карима; Адай Аида; Казбекова Айша — AI-assisted implementation disclosed',subject='Технический отчёт: архитектура, шесть паттернов и фактические проверки',leftMargin=44,rightMargin=44,topMargin=49,bottomMargin=42)
templates=[]
for name,size in [('portrait',A4),('landscape',landscape(A4))]:
    templates.append(PageTemplate(id=name,pagesize=size,frames=[Frame(44,42,size[0]-88,size[1]-91,leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)],onPage=decorate))
doc.addPageTemplates(templates)
story=[]
for index,part in enumerate(text.split('<!-- PAGE -->')):
    wide='<!-- LANDSCAPE -->' in part
    if index:story.extend([NextPageTemplate('landscape' if wide else 'portrait'),PageBreak()])
    story.extend(page_flow(part,wide))
doc.build(story)
print('Created',pdf)

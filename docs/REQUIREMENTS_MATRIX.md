# Матрица требований

Основание: FINAL PROJECT (1).docx, разделы 1–11, и требования пользователя к игре из десяти глав. Матрица связывает критерии с реализацией и проверками; оценку выставляет преподаватель. Подтверждённые результаты предыдущего прогона сохранены, итог повторной проверки после исправлений указывается в `docs/verification/RELEASE_CHECK.md`.

| ID / раздел исходного задания | Реализация и файлы | Проверка | Статус | Оставшееся действие |
| --- | --- | --- | --- | --- |
| O1, §1: полный путь | ProfileService → GameFacade → ResultService, app.js | JUnit, process_smoke.py | Passed для Java/API; UI Not run | Ручной путь в браузере |
| O2, §3: ≥3 сущности | Player, Chapter, Attempt, PlayerProgress, Task | GameLogicTest | Passed | Объяснить связи на защите |
| O3, §3: ≥5 сценариев, CRUD/отмена | 11 сценариев README; create/edit/select/start/talk/answer/cancel/history/export | HTTP, процессные проверки | Passed на уровне API; UI Not run | Проверить кнопки вручную |
| O4, §3: два поведения | LearningStrategy / AssessmentStrategy | allTenChaptersAndNineUnlocks, learningHintRetry..., assessmentHides... | Passed | Продемонстрировать различие в UI |
| O5, §3: событие | ChapterCompletedPublisher + 2 observers | replayFinishAndObserverDeduplication | Passed | Показать код подписчиков |
| O6, §3: сохранение | JsonGameRepository; JSON v1, temp/force/atomic move | saveReload..., writeFailure..., process_smoke.py | Passed | Проверить на целевой файловой системе команды |
| O7, §3: итог/экспорт | StatisticsVisitor, TextReportVisitor, ResultService | bothVisitorsDoubleDispatch + HTTP TXT | Passed | Нажатие скачивания в браузере Not run |
| O8, §3: ввод, ошибки, sample data | ProfileService, ApiExceptionHandler; demo при первом старте | JUnit 400/404/409/503, повреждённый файл | Passed | — |
| O9, §4: 6 паттернов из 3 категорий | Builder, Factory Method, Facade, Strategy, Observer, Visitor | рабочие операции + тесты, UML | Реализованы и проверены Java/API | Демонстрация и объяснение преподавателю |
| O10, §4 и §11: 2 самостоятельных паттерна каждому | CONTRIBUTIONS.md | Сведения о фактическом авторстве и распределении паттернов | Внешнее подтверждение | Самостоятельность участников не подтверждена; технический код реализован с раскрытой AI-помощью |
| O11, §5: GUI связан с Java | HTML/CSS/JS, Canvas/SVG, GameController | статические ресурсы HTTP200; JS syntax | Реализован; ручной UI Not run | Chrome заблокирован инструментом Computer Use |
| O12, §6: слои/SOLID | model/content/task/service/web/storage/event/result | обзор кода и архитектуры | Реализовано | Код-ревью команды |
| O13, §6: ≥8 сценариев, ≥3 автотестов, ≥3 границы | GameLogicTest, HttpApiTest, TEST_SCENARIOS | mvnw verify; process_smoke.py | Passed, фактические счётчики в verification | — |
| O14, §7: сроки и демонстрации | Исходное задание; фактическая Git-история | Срок сдачи — конец учебной недели 9, защита — неделя 10 | Внешнее подтверждение | Календарь курса и проведённые демонстрации подтверждает команда; трёхнедельная история не имитируется |
| O15, §8: README и ZIP, запуск | README, scripts/package_release.py, dist | Чистая распаковка, сборка, отдельный запуск JAR | Доказательства в RELEASE_CHECK.md | Повторить на компьютере команды |
| O16, §8: GitHub/GitLab с историей | origin finalprojectspd; AI-коммиты | git log и проверка origin/main | Первые четыре коммита опубликованы, HEAD 56409e5 | Итоговые изменения текущего цикла фиксируются отдельно; личное авторство студентов не заявляется |
| O17, §8: PDF и исходник | docs/report/REPORT.md, docs/report/history-journey-report.pdf | Сборка, извлечение текста, визуальная проверка страниц | Результат в PDF_QA.md | Реальные UI-скриншоты отсутствуют |
| O18, §8: class/sequence UML | docs/uml/*.puml, *.svg, *.png | PlantUML, сверка классов | Подготовлены | Ревью преподавателем |
| O19, §9: защита и live change | Фактические сведения в CONTRIBUTIONS.md | Личная защита ещё не проводилась | Внешнее подтверждение | Результаты защиты подтверждает преподаватель |
| O20, §10: рубрика | 20+30+10+5+5 командных, 30 индивидуальных | Матрица и фактические проверки | Самооценка баллов не выставляется | Оценка преподавателя |
| O21, §11: раскрытие AI и материалов | CONTRIBUTIONS, ASSET_PROVENANCE, THIRD_PARTY_NOTICES, отчёт | Проверка атрибуции | Задокументировано | Согласовать допустимость с преподавателем |
| U1: ровно 10 полных глав | resources/content/chapter-01..10.json, общий загрузчик | все 10 в двух режимах; 62 process checks | Passed Java/API | UI каждой сцены Not run |
| U2: минимум 2 NPC и 2 Quiz+1 Map на главу | 20 NPC, 30 tasks | Builder/Factory tests; BFS достижимость | Passed для контента и геометрии | Ручная проверка движения и диалогов |
| U3: серверная тайна | MapEntry/AttemptView, контент вне static | HttpApiTest, маскировка и отсутствие content в HTTP | Passed | — |
| U4: состояния и повторные запросы | Attempt, Receipt, GameFacade | запреты переходов, UUID-body conflict, duplicate finish | Passed | — |
| U5: карта и внешняя точка Оренбурга | Natural Earth SVG, project(), inset | локальная геометрия/код | Реализовано, UI Not run | Визуально проверить подписи/перекрытия |
| U6: реальное происхождение, без прежних ресурсов | ASSET_PROVENANCE, audit_release.py | аудит состава и строк | Результат audit-results.json | Права по договору проверяет команда отдельно |
| U7: история, общий финал 10/10 | summary / exportSummary, app.js | JUnit, HTTP | Passed Java/API | UI финала Not run |

Рубрика: функциональность 20, паттерны 30, архитектура 10, интерфейс 5, документация/тесты/Git 5, индивидуальная защита 30. Технический прототип с раскрытой AI-помощью сам по себе не подтверждает самостоятельность или право на указанные баллы.

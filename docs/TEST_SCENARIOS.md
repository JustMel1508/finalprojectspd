# Фактические сценарии тестирования

Команды: `./mvnw.cmd verify`, `python tools/process_smoke.py --java <JDK17>/bin/java.exe`, `node --check src/main/resources/static/app.js`, `node tools/ui_logic_test.cjs`. Итог текущего цикла: 48 Java-тестов, 62 HTTP/процессные проверки и 20 unit-проверок интерфейсной логики — Passed. Отчёты JUnit находятся в `target/surefire-reports`, сводка — в `docs/verification/RELEASE_CHECK.md`. Ручные браузерные проверки выделены отдельно и остаются Not run.

| ID | Предусловия / ввод | Шаги | Ожидание | Фактический результат | Способ / статус |
| --- | --- | --- | --- | --- | --- |
| T01 | Новый профиль HEROINE | create → start c1 → talk×2 → answer×3 → finish → export | Сохранённый успех и TXT | API: успех 3/3, UTF-8 TXT; UI не проверен | process_smoke; Passed API / Not run UI |
| T02 | Профиль и ACTIVE c1 | изменить имя/аватар, остановить процесс, запустить | Изменения и попытка восстановлены | NPC 2, acceptedTasks 1, имя/аватар совпали | process_smoke; Passed |
| T03 | Один неверный ответ | LEARNING и ASSESSMENT | Разная возможность повторов и раскрытие | Learning оставляет текущее; assessment двигается без оценки | GameLogicTest; Passed |
| T04 | LEARNING c1 | ошибка, повтор UUID, верный новый UUID, закончить | 3 задания, 4 ответа, 1 ошибка | Точные значения подтверждены; подсказка видна | learningHintRetry...; Passed |
| T05 | ASSESSMENT c1 | 1 неверный и 2 верных | COMPLETED, passed=false, сохраняется | failed=1, раскрытия новой главы нет | assessmentHides...; Passed |
| T06 | Успех c1, затем неуспех | пройти повторно неверно | Заголовок c1 и доступ c2 сохраняются | Проверено | replayFinish... + process; Passed |
| T07 | Новый demo, два режима отдельно | 10×(start,talk,answer,finish) | 9 открытий, 10 освоенных | Оба режима проходят все главы, финал без c11 | parameterized JUnit; Passed |
| T08 | Пройдено 10 глав | summary и общий TXT | 10/10, повторы отдельно | 11 завершений, 10 освоенных, 1 неуспех | process_smoke; Passed |
| T09 | Освоена c1, ACTIVE c2 | cancel дважды | Одна отмена, c1 остаётся | Проверено; COMPLETED→CANCELLED запрещён | cancellation...; Passed |
| T10 | Сохранены NPC/ответ | terminate Java, новый Popen | Тот же ID, NPC, следующий этап | Отдельный реальный процесс восстановил данные | process_smoke; Passed |
| T11 | Завершённая попытка | export и result/statistics | TXT соответствует состоянию | Совпали tasks/correct/tries/errors и объяснения | Visitor + process; Passed API / Not run download UI |
| T12 | Пустое/пробельное/>40 имя, неверный avatar | POST/PUT profile | 400, данные прежние | HTTP400/ошибка GameException | JUnit+HTTP; Passed |
| T13 | Закрытая c10, unknown ID/option, чужой профиль | Прямые API-вызовы | 409/404/400, без изменения | Проверены допуск, порядок, NPC и принадлежность | JUnit+process; Passed |
| T14 | Завершённая попытка | finish повторно, событие повторно | Один completed и одно освоение | Дедупликация attemptId работает | replayFinish...; Passed |
| T15 | Сбой диска / повреждённый JSON | искусственная IOException; запуск с {broken | 503, старый снимок, файл сохранён | Память/диск остались ACTIVE; corrupt не перезаписан | JUnit+process; Passed |
| T16 | Второй новый профиль | summary/map | Независимый 0/10 | Проверено | JUnit+process; Passed |
| T17a | Все 10 сцен, obstacles и NPC | BFS из безопасной точки с радиусом коллизий | Достижимы 20 NPC | Все обязательные персонажи достигнуты по геометрии | everyNpcReachable...; Passed |
| T17b | Браузер, WASD/стрелки/диалог/blur | пройти каждую сцену, проверить паузу/фокус/мобильный вид | Рабочее управление и кнопки | Повторная попытка в текущем цикле: browsers=[], browser access host unavailable; открыть локальную вкладку невозможно | Not run |
| T18 | Новый профиль, обзор/активная проверка | map/history/ответы до finish | Нет будущих title/answer keys, correctness скрыта | HTTP DTO проверены; content не отдаётся как static | HttpApiTest + process; Passed |
| T19 | Неполный Builder, неизвестный вариант фабрики | build / prepare | Отклонение несогласованных данных | Проверены 10 Builder, Quiz/Map products, неверные координаты | builderAndFactory...; Passed |
| T20 | QuizResult + MapTaskResult | accept обоих Visitor | Двойная диспетчеризация, статистика и TXT | Проверены разные visit и координаты карты | bothVisitorsDoubleDispatch; Passed |
| T21 | 8 одновременных create | threads, reload snapshot | Нет потерянных записей | Всего 9 профилей с demo | concurrentRequestsSerialized; Passed |
| T22 | Модели DOM/Canvas, без браузера | 60/144 Hz, коллизии, фокус/blur, карта, изоляция pending, UUID после сбоя, двойное нажатие | Постоянная скорость, корректные стадии и ввод | Все 20 проверок Passed; просмотрены данные 10 сцен и 30 задач | node tools/ui_logic_test.cjs; Passed unit |
| T23 | Синтаксически корректный, но несогласованный JSON | Подмена chapterId/NPC/task/option, passed, счётчиков, прогресса и UUID; пропуск поля или повтор JSON-ключа | Обработанная ошибка 503, исходный файл нетронут | SavedStateValidationTest: 30 проверок Passed | Maven verify; Passed |

Статус Passed означает реальную проверку указанного уровня. Автотест геометрии не является ручным UI-прохождением. Реальные скриншоты приложения отсутствуют; перед сдачей команда должна пройти T01/T11/T17b в браузере и добавить изображения карты, сцены и результата в отчёт.

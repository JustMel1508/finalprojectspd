# HTTP API v1

Все операции локальны (127.0.0.1), выбор профиля без аутентификации. JSON UTF-8. Ошибка: `{code,message,action}`. 400 — неверный ввод; 404 — неизвестный объект; 409 — недопустимый переход; 503 — хранилище недоступно. Ответы не содержат серверных определений глав.

| Метод и путь | Назначение / тело |
| --- | --- |
| GET /api/health | Состояние хранилища и сообщение восстановления |
| GET /api/players | Список профилей |
| POST /api/players | `{name,avatar}`; avatar HERO или HEROINE |
| PUT /api/players/{playerId} | Изменение `{name,avatar}` |
| GET /api/players/{playerId}/map | displayTitle, доступность и раскрытые даты |
| GET /api/players/{playerId}/summary | Уникальные освоенные главы, статистика |
| GET /api/players/{playerId}/history | История без скрытых заголовков |
| GET /api/players/{playerId}/active | Текущая попытка или null |
| POST /api/players/{playerId}/attempts | `{chapterId,mode}`; LEARNING/ASSESSMENT |
| GET /api/players/{playerId}/attempts/{id} | Продолжение, разрешённый этап и сцена |
| POST /api/players/{playerId}/attempts/{id}/talk | `{npcId}` |
| POST /api/players/{playerId}/attempts/{id}/answers | `{requestId,taskId,optionId}` |
| POST /api/players/{playerId}/attempts/{id}/finish | Завершение; повтор идемпотентен |
| POST /api/players/{playerId}/attempts/{id}/cancel | Отмена; повтор идемпотентен |
| GET /api/players/{playerId}/attempts/{id}/export | TXT завершённой попытки |
| GET /api/players/{playerId}/export | Общий TXT |

На профиль разрешена одна ACTIVE-попытка. Ответы принимаются только после разговоров со всеми NPC, по порядку заданий. Для нового ответа нужен новый UUID requestId; для повтора после сетевой ошибки тот же UUID и те же поля. При конфликте тела с requestId — 409. В ASSESSMENT правильность и объяснения выдаются только после finish. ACTIVE может перейти только в COMPLETED или CANCELLED. После успешной атомарной записи обновлённый DTO возвращается клиенту.

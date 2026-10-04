README.md

```markdown
# OpenRouter Agent

Нативное Android-приложение — клиент OpenRouter с агентным режимом (Agentic Mode), Skills, Projects, MCP и импортом навыков из GitHub.

Приложение работает как локальный агентный оркестратор: собирает системный промпт, подключает активные Skills и контекст Project, объединяет локальные и MCP-инструменты в единый реестр, вызывает модель через OpenRouter, выполняет tool calls и возвращает результаты обратно в модель до финального ответа.

---

## Возможности

### Чаты
- Список диалогов, создание, переименование, удаление.
- Потоковая генерация ответа (streaming).
- Markdown и code blocks, копирование сообщений.
- Повтор генерации, редактирование пользовательского сообщения, остановка streaming.
- Отображение tool calls, промежуточных результатов agent loop и ошибок.
- Привязка Skills и Project к чату.
- Выбор модели, индикатор Agentic Mode, активные Skills.

### Agentic Mode
Agent loop выполняет 12 шагов:
1. Получить пользовательскую задачу.
2. Собрать system prompt.
3. Добавить активный Skill.
4. Добавить контекст проекта.
5. Объединить локальные и MCP-инструменты.
6. Отправить запрос в OpenRouter.
7. Проверить наличие `tool_calls`.
8. Запросить подтверждение, если инструмент опасный.
9. Выполнить инструмент.
10. Добавить результат в историю.
11. Повторить запрос.
12. Завершить цикл после обычного ответа или достижения лимита итераций.

### Projects
- Отдельные рабочие пространства.
- Привязка чатов, файлов и навыков.
- Изоляция контекста между проектами.

### Skills
- Форматы: `SKILL.md`, JSON, YAML.
- Поля: `name`, `description`, `system_prompt`, `input_schema`, `tools_allowed`, `output_format`.
- Встроенные и пользовательские навыки.
- Создание из шаблона внутри приложения.
- Импорт по ссылке на GitHub с валидацией схемы и preview перед активацией.
- Привязка к проекту или отдельному чату.

### MCP
- Подключение серверов: URL, transport, auth.
- Список exposed tools.
- Единый Tool Registry: Local + MCP + Model tools.
- Timeout и недоступность сервера не блокируют приложение.
- Отключённый MCP tool не отправляется модели.

### Settings
1. OpenRouter API Key.
2. Выбранная модель.
3. `openrouter/free` (free router).
4. Temperature.
5. Max tokens.
6. Максимальное число agent iterations.
7. Parallel tool calls.
8. Подтверждение опасных действий.
9. Разрешить MCP.
10. Разрешить сетевые инструменты.
11. Автоматическая отправка результатов tools.
12. Очистка локальной истории.
13. Экспорт данных.
14. Удаление всех данных.

---

## Технологический стек

- **Kotlin**
- **Jetpack Compose + Material 3**
- **MVVM + Repository + Clean Architecture**
- **Hilt** — dependency injection
- **Retrofit + OkHttp** — OpenRouter, GitHub, MCP HTTP
- **Kotlin Coroutines + Flow**
- **Room** — чаты, сообщения, проекты, Skills, MCP-конфигурации
- **DataStore** — настройки
- **Android Keystore** — шифрование API-ключей и токенов
- **Kotlin Serialization** — JSON-модели и OpenRouter API
- **YAML-парсер** — импорт Skills
- **WorkManager** — фоновые операции: импорт репозитория, индексация файлов, синхронизация MCP
- **MCP Kotlin SDK**

---

## Архитектура

```

app            → UI, навигация, DI-корень
feature/*      → экраны и ViewModel (auth, chats, projects, skills, mcp, settings)
domain         → модели, интерфейсы репозиториев, use cases
data/*         → OpenRouter, GitHub, MCP, Skills, Tools, реализации репозиториев
agent          → AgentEngine, AgentLoop, ApprovalGate, ToolCallDispatcher
core/*         → common, ui, database, datastore, network, security
worker         → WorkManager-воркеры

```

Правило зависимостей: `app → feature → domain ← data → core`. Модуль `agent` зависит от `domain` и `data`, модуль `worker` — от `data` и `agent`.

---

## Структура репозитория

```

OpenRouterAgent/
├── app/
├── core/
│   ├── common/
│   ├── ui/
│   ├── database/
│   ├── datastore/
│   ├── network/
│   └── security/
├── feature/
│   ├── auth/
│   ├── chats/
│   ├── projects/
│   ├── skills/
│   ├── mcp/
│   └── settings/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── data/
│   ├── openrouter/
│   ├── github/
│   ├── mcp/
│   ├── skills/
│   ├── tools/
│   └── repository/
├── agent/
└── worker/

```

---

## Начало работы

### Требования
- Android Studio последней стабильной версии.
- JDK 17+.
- Android SDK: minSdk 26, targetSdk 34+.
- Аккаунт OpenRouter и API Key (https://openrouter.ai/keys).

### Сборка
```bash
git clone <repository-url>
cd OpenRouterAgent
./gradlew assembleDebug
```

Запуск

1. Установить приложение на устройство или эмулятор.
2. На экране входа ввести OpenRouter API Key.
3. Проверить ключ запросом к API.
4. Сохранить ключ в зашифрованном хранилище.
5. Выбрать модель или openrouter/free в Settings.
6. Начать новый чат или создать Project.

---

Безопасность

· API Key и MCP-токены шифруются через Android Keystore (AES/GCM).
· Ключевой материал Keystore недоступен для обычного извлечения приложением.
· Authorization редактируется в OkHttp-логах.
· Экспорт данных не включает секреты без явного подтверждения.
· Удаление всех данных очищает Room, DataStore, файлы Projects и ключи Keystore.
· Импорт Skills из GitHub не выполняет код.
· Исполняемые файлы запрещены по умолчанию.
· Неизвестные инструменты отклоняются без выполнения.

---

Импорт Skills из GitHub

Поддерживаемые ссылки:

```
https://github.com/user/repository
https://github.com/user/repository/tree/main/skills/example
https://raw.githubusercontent.com/user/repository/main/SKILL.md
```

Алгоритм:

1. Разобрать URL.
2. Получить README, SKILL.md, JSON или YAML.
3. Определить корневой каталог Skill.
4. Проверить обязательные поля.
5. Проверить размер файлов.
6. Запретить исполняемые файлы.
7. Показать preview.
8. Подтвердить импорт.
9. Сохранить Skill локально.
10. Предложить привязать к проекту или чату.

---

Предустановленные источники Skills

· https://github.com/DietrichGebert/ponytail
· https://github.com/anthropics/skills
· https://github.com/tashfeenahmed/freellmapi
· https://github.com/obra/superpowers
· https://github.com/Shubhamsaboo/awesome-llm-apps

Перед включением проверьте лицензии репозиториев.

---

OpenRouter

Используется OpenAI-compatible API:

· /api/v1/chat/completions — чат и streaming.
· /api/v1/models — список моделей и возможностей.
· Tool calling — цикл: модель предлагает tool, приложение выполняет, результат возвращается модели.
· Structured outputs — через response_format.
· openrouter/free — отдельный free router для автоматического выбора бесплатных моделей с нужными возможностями.

---

Тесты

API Key

· Ключ сохраняется зашифрованным.
· Ключ не появляется в логах.

Chat

· Streaming корректно собирается.
· Отмена запроса останавливает поток.
· История сохраняется после перезапуска.

Agent

· Неизвестный tool отклоняется.
· Превышение maxIterations завершает цикл.
· Tool result возвращается модели.
· Dangerous tool требует approval.

Skills

· Некорректный YAML отклоняется.
· Отсутствующие name/description обнаруживаются.
· Запрещённые tools блокируются.
· Импорт из GitHub не выполняет код.

MCP

· Timeout корректно обрабатывается.
· Недоступный сервер не блокирует приложение.
· Инструменты сервера появляются в registry.
· Отключённый MCP tool не отправляется модели.

---

Roadmap

☐ Core: network, database, datastore, security.
☐ Auth + Settings.
☐ Chats со streaming.
☐ Agentic Mode + локальные tools + approval.
☐ Skills: парсеры и валидация.
☐ Импорт Skills из GitHub через WorkManager.
☐ Projects: чаты, файлы, навыки.
☐ MCP: клиенты и единый ToolRegistry.
☐ Экспорт, очистка, удаление данных.

---

Лицензия

Укажите лицензию проекта. Предустановленные внешние Skills и Plugins распространяются под лицензиями соответствующих репозиториев.

---

Контрибьютинг

1. Форкнуть репозиторий.
2. Создать ветку feature/<name> или fix/<name>.
3. Писать тесты на новые сценарии.
4. Открыть pull request с описанием изменений.

---

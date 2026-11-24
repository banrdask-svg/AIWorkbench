# AIWorkbench

AIWorkbench - Paper-плагин для Minecraft 1.21.11 и Java 21. Он добавляет ИИ-верстак, через который игрок пишет простую идею предмета, получает предпросмотр, подтверждает создание, а плагин регистрирует предмет, рецепт и ассеты ресурспака.

## Режимы создания

По умолчанию API отключён:

```yaml
ai:
  enabled: true
  base-url: "https://api.deepseek.com/chat/completions"
  api-key: ""
  model: "deepseek-flash"
  reasoning-effort: "high"
```

Если `api-key` пустой или `ai.enabled: false`, предметы создаются локально, без HTTP-запросов. Если указать DeepSeek API key в серверном `plugins/AIWorkbench/config.yml`, будет использоваться DeepSeek Chat Completions API.

Не храни реальный API-ключ в исходниках и не публикуй `config.yml` с ключом.

## Текстуры через Gemini

DeepSeek отвечает за механику и JSON предмета. Gemini можно включить отдельно для PNG-текстур ресурспака:

```yaml
gemini:
  enabled: true
  api-key: "твой-gemini-api-key"
  base-url: "https://generativelanguage.googleapis.com/v1beta/interactions"
  model: "gemini-3.1-flash-image"
```

Если Gemini выключен или вернул ошибку, плагин создаст локальную безопасную PNG-текстуру, чтобы предмет всё равно зарегистрировался.

## Крафт из своих предметов

Новые рецепты могут использовать уже созданные тобой AIWorkbench-предметы как ингредиенты. В JSON это хранится как `aiworkbench:item_id`, а в рецепте Paper проверяется точный `ItemStack` с PDC, поэтому обычный предмет с похожим названием не подойдёт.

## Команды

- `/aiworkbench` или `/aiwb` - открыть GUI.
- `/aiworkbench give [player]` - выдать ИИ-верстак.
- `/aiworkbench create` - создать предмет через чат.
- `/aiworkbench library` - открыть библиотеку.
- `/aiworkbench reload` - перезагрузить конфиг.
- `/aiworkbench debug` - показать режим отладки.
- `/aiworkbench resourcepack rebuild` - пересобрать ресурспак.

## Сборка

```bash
./gradlew build
```

Готовый JAR: `build/libs/AIWorkbench-0.6.1.jar`.

## Установка и первый запуск

Соберите проект командой gradlew.bat build на Java 21. Поместите JAR из build/libs в plugins сервера Paper 1.21.11 и перезапустите сервер. Выполните /aiwb give, откройте верстак или используйте /aiwb create. Сначала проверьте локальный режим без API-ключей. Параметры сервера находятся в plugins/AIWorkbench/config.yml. Реальные API-ключи задавайте только на сервере.

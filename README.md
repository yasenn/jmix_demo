# Демо jmix

- Простая система заявок на обслуживание с BPMN-согласованием.
- Стратегия "согласен, если не отвечаю": если согласующий не отвечает в течение 1 минуты, заявка автоматически согласуется.
- Мок дашборда, получаемого из 1С через OData (имитация платежных поручений).
- Просмотр статуса на диаграмме процесса с подсветкой текущих и завершенных узлов.

# Pre-requisites

Достаточно одного из вариантов:

- Docker Engine и Docker Compose (режим docker).
- Nix с nix-shell (режим nix).
- JDK 17 и Gradle 8+ в PATH (режим system).
- curl и доступ в интернет (режим nix-portable, скачивает локально без sudo).

Makefile сам выбирает первый доступный режим. Принудительный выбор: `make docker-run`, `make nix-run`, `make system-run`, `make nix-portable-run`.

Проверка окружения и выбранного режима: `make doctor`.

# Запуск

- `make run`
- http://localhost:8080
- `make clean`

# Что под капотом

## Структура

```
Makefile
docker/
  Dockerfile
  docker-compose.yml
nix/
  shell.nix
poc-service-request/
  build.gradle
  src/main/java/com/example/poc/
  src/main/resources/
```

## Стек приложения

- Spring Boot 3.2 на Java 17 + Gradle + Maven Central
- [ Flowable 7.0.1 ](https://github.com/flowable/flowable-engine)
- [ H2 ](https://github.com/h2database/h2database)
- [ Thymeleaf  ](https://github.com/thymeleaf/thymeleaf) в стиле [ Jmix Lumo ](https://docs.jmix.io/jmix/flow-ui/themes/lumo.html)

## Режимы запуска

Makefile определяет доступный инструмент и запускает приложение в одном из режимов:

- docker: `docker compose -f docker/docker-compose.yml up --build`. Многоступенчатая сборка (gradle:8-jdk17 - eclipse-temurin:17-jre), runtime-образ около 200 MB, порт 8080 пробрасывается.
- nix: `nix-shell nix/shell.nix --run 'cd poc-service-request && gradle bootRun'`. JDK 17 и Gradle 8 берутся из Nix store, воспроизводимо на любой машине.
- system: `cd poc-service-request && gradle bootRun` с системными gradle и java.
- nix-portable: скачивает `nix/nix-portable` (~10 MB) и поднимает Nix в userspace без sudo, дальше как режим nix. Кэш Nix store живет в `~/.nix-portable`.

Для всех режимов после старта приложения фоновый цикл ждет открытия порта и выводит в консоль
```
>>> Открыть: http://localhost:8080
```

## BPMN

- [ service-request.bpmn20.xml ](poc-service-request/src/main/resources/processes/service-request.bpmn20.xml)
- Автосогласование реализовано `boundaryEvent ` с `cancelActivity="true"`: по истечении таймера задача отменяется и процесс уходит в `approvedEnd`.
- Секция `bpmndi:BPMNDiagram` обязательна: в ней `BPMNShape` с `dc:Bounds` для каждого узла и `BPMNEdge` с `di:waypoint` для каждого потока.

## Диаграмма

- DiagramService рендерит PNG через `ProcessDiagramGenerator.generateDiagram`.
- Для активного процесса подсвечиваются текущие узлы через `runtimeService.getActiveActivityIds`.
- Для завершенного процесса список берется из HistoryService (завершенные активности).
- Используется сигнатура из 6 аргументов: `(BpmnModel, png, active, emptyList, 1.0, true)`. Короткие сигнатуры в Flowable 7 удалены.
- Статичный preview схемы на главной странице отдается методом `getPreview` по ключу `serviceRequestApproval`.

## Мок 1С

- `Mock1CService `возвращает список платежных поручений в памяти.
- `OData`-вызовы не выполняются, данные имитированы.
- Эндпоинт `/mock-1c` рендерит метрики (всего, оплачено, ожидает, документов) и таблицу платежей.

## Очистка

- `make clean`: освобождает порт 8080, удаляет контейнеры и локальные образы Docker, `nix/nix-portable` и `~/.nix-portable`, а также `build` и `.gradle` проекта.
- `make clean-deep`: дополнительно чистит `~/.gradle/caches` и висячие Docker-образы.
- Отдельные шаги: `make clean-port`, `make clean-docker`, `make clean-nix`, `make clean-gradle`.

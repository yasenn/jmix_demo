SHELL := $(shell command -v bash 2>/dev/null || echo /bin/sh)
.DEFAULT_GOAL := help

PROJECT      := poc-service-request
PORT         := 8080
URL          := http://localhost:$(PORT)

NIX_DIR      := nix
NIX_SHELL    := $(NIX_DIR)/shell.nix
NIXPORT_BIN  := $(NIX_DIR)/nix-portable
NIXPORT_URL  := https://github.com/DavHau/nix-portable/releases/latest/download/nix-portable-$(shell uname -m)

DOCKER_DIR   := docker
COMPOSE_FILE := $(DOCKER_DIR)/docker-compose.yml

COMPOSE       := $(shell if docker compose version >/dev/null 2>&1; then echo 'docker compose'; elif command -v docker-compose >/dev/null 2>&1; then echo 'docker-compose'; fi)
DOCKER_BIN    := $(shell command -v docker 2>/dev/null)
DOCKER_OK     := $(shell command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1 && [ -n "$(COMPOSE)" ] && echo yes)
NIX_SHELL_BIN := $(shell command -v nix-shell 2>/dev/null)
SYS_GRADLE    := $(shell command -v gradle 2>/dev/null)
SYS_JAVA      := $(shell command -v java 2>/dev/null)

ifeq ($(DOCKER_OK),yes)
  MODE := docker
else ifneq ($(NIX_SHELL_BIN),)
  MODE := nix
else ifneq ($(SYS_GRADLE),)
  MODE := system
else
  MODE := nix-portable
endif

START_docker       := $(COMPOSE) -f $(COMPOSE_FILE) up --build
START_nix          := nix-shell $(NIX_SHELL) --run 'cd $(PROJECT) && gradle bootRun --console=plain'
START_system       := cd $(PROJECT) && gradle bootRun --console=plain
START_nix-portable := $(NIXPORT_BIN) nix-shell $(NIX_SHELL) --run 'cd $(PROJECT) && gradle bootRun --console=plain'

.PHONY: help run doctor docker-run nix-run nix-portable-run system-run \
        docker-down docker-logs \
        clean clean-deep clean-port clean-docker clean-nix clean-gradle \
        nix-clean

help: ## Список целей
	@echo "Использование: make <цель>"
	@echo ""
	@echo "Авто-выбор режима:"
	@awk 'BEGIN {FS=":.*## "} /^(run|doctor):.*## / { printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo ""
	@echo "Форсированный режим:"
	@awk 'BEGIN {FS=":.*## "} /^(docker-run|nix-run|nix-portable-run|system-run):.*## / { printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo ""
	@echo "Очистка:"
	@awk 'BEGIN {FS=":.*## "} /^(clean|clean-deep|clean-port|clean-docker|clean-nix|clean-gradle|docker-down|docker-logs):.*## / { printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2 }' $(MAKEFILE_LIST)

run: ## Авто: docker → nix → system gradle → nix-portable
	@$(MAKE) --no-print-directory _preflight MODE=$(MODE)
	@echo "==> Режим: $(MODE)"
	@echo "==> Первый запуск: 1-5 минут (скачивание зависимостей)"
	@if [ "$(MODE)" = "nix-portable" ] && [ ! -x "$(NIXPORT_BIN)" ]; then \
	    $(MAKE) --no-print-directory $(NIXPORT_BIN); fi
	@( \
	  ( for i in $$(seq 1 300); do \
	      if curl -sf -o /dev/null --max-time 1 $(URL)/ 2>/dev/null; then \
	        printf '\n\033[1;32m>>> Открыть: $(URL)\033[0m\n\n'; exit 0; \
	      fi; \
	      sleep 1; \
	    done ) & \
	  $(START_$(MODE)) \
	)

docker-run: ## Запуск в Docker (требует docker + compose)
	@$(MAKE) --no-print-directory _preflight MODE=docker
	@$(MAKE) --no-print-directory run MODE=docker

nix-run: ## Запуск через системный nix-shell
	@$(MAKE) --no-print-directory _preflight MODE=nix
	@$(MAKE) --no-print-directory run MODE=nix

nix-portable-run: ## Запуск через локальный nix-portable (без sudo)
	@$(MAKE) --no-print-directory _preflight MODE=nix-portable
	@$(MAKE) --no-print-directory run MODE=nix-portable

system-run: ## Запуск через системный gradle + java 17
	@$(MAKE) --no-print-directory _preflight MODE=system
	@$(MAKE) --no-print-directory run MODE=system

_preflight:
	@case "$(MODE)" in \
	  docker) \
	    [ -n "$(DOCKER_BIN)" ] || { echo "ERROR: docker не найден"; exit 1; }; \
	    [ -n "$(COMPOSE)" ]    || { echo "ERROR: docker compose не найден"; exit 1; }; \
	    docker info >/dev/null 2>&1 || { echo "ERROR: docker daemon не запущен"; exit 1; } ;; \
	  nix) \
	    [ -n "$(NIX_SHELL_BIN)" ] || { echo "ERROR: nix-shell не найден"; exit 1; }; \
	    [ -f "$(NIX_SHELL)" ]     || { echo "ERROR: $(NIX_SHELL) отсутствует"; exit 1; } ;; \
	  system) \
	    [ -n "$(SYS_GRADLE)" ] || { echo "ERROR: gradle не найден"; exit 1; }; \
	    [ -n "$(SYS_JAVA)" ]   || { echo "ERROR: java не найден"; exit 1; } ;; \
	  nix-portable) \
	    command -v curl >/dev/null 2>&1 || { echo "ERROR: curl не найден"; exit 1; }; \
	    [ -f "$(NIX_SHELL)" ] || { echo "ERROR: $(NIX_SHELL) отсутствует"; exit 1; } ;; \
	  *) echo "ERROR: неизвестный режим '$(MODE)'"; exit 1 ;; \
	esac

$(NIXPORT_BIN):
	@mkdir -p $(NIX_DIR)
	@echo "==> Скачиваем nix-portable (~10 MB) ..."
	@curl -fsSL -o $(NIXPORT_BIN).tmp $(NIXPORT_URL)
	@mv $(NIXPORT_BIN).tmp $(NIXPORT_BIN)
	@chmod +x $(NIXPORT_BIN)

clean: ## Полная очистка: порт, docker, nix-portable, gradle
	@echo "==> Полная очистка артефактов проекта"
	@-$(MAKE) --no-print-directory clean-port    || true
	@-$(MAKE) --no-print-directory clean-docker  || true
	@-$(MAKE) --no-print-directory clean-nix     || true
	@-$(MAKE) --no-print-directory clean-gradle  || true
	@echo "==> Готово"

clean-deep: ## Дополнительно: ~/.gradle/caches и все docker-образы
	@-$(MAKE) --no-print-directory clean
	@echo "==> Чистим кэши Gradle (замедлит следующий build)"
	@rm -rf "$${HOME}/.gradle/caches" "$${HOME}/.gradle/daemon" 2>/dev/null || true
	@if [ -n "$(DOCKER_BIN)" ]; then docker image prune -f >/dev/null 2>&1 || true; fi
	@echo "==> Готово (глубоко)"

clean-port: ## Освободить порт $(PORT)
	@if command -v lsof >/dev/null 2>&1; then \
	    PIDS=$$(lsof -ti tcp:$(PORT) 2>/dev/null || true); \
	    [ -n "$$PIDS" ] && echo "  kill $$PIDS" && kill -9 $$PIDS 2>/dev/null || true; \
	elif command -v ss >/dev/null 2>&1; then \
	    PIDS=$$(ss -ltnp 2>/dev/null | awk -v p=":$(PORT)" '$$4 ~ p {print $$6}' | grep -oP 'pid=\K[0-9]+' || true); \
	    [ -n "$$PIDS" ] && echo "  kill $$PIDS" && kill -9 $$PIDS 2>/dev/null || true; \
	fi
	@echo "==> Порт $(PORT) свободен"

clean-docker: ## Остановить и удалить контейнеры, тома, локальные образы
	@if [ -n "$(COMPOSE)" ] && [ -f "$(COMPOSE_FILE)" ]; then \
	    echo "  $(COMPOSE) down"; \
	    $(COMPOSE) -f $(COMPOSE_FILE) down -v --rmi local --remove-orphans 2>/dev/null || true; \
	else \
	    echo "  docker/compose отсутствует или нет $(COMPOSE_FILE) — пропуск"; \
	fi
	@echo "==> Docker очищен"

clean-nix: ## Удалить локальный nix-portable и кэш в $HOME
	@echo "  rm -f $(NIXPORT_BIN)"
	@rm -f $(NIXPORT_BIN)
	@if [ -d "$${HOME}/.nix-portable" ]; then \
	    echo "  rm -rf $${HOME}/.nix-portable"; \
	    rm -rf "$${HOME}/.nix-portable"; \
	fi
	@echo "==> nix-portable удалён"

clean-gradle: ## Удалить артефакты Gradle в проекте
	@echo "  rm -rf $(PROJECT)/build $(PROJECT)/.gradle"
	@rm -rf $(PROJECT)/build $(PROJECT)/.gradle
	@echo "==> Артефакты Gradle удалены"

nix-clean: ## (алиас) = clean-nix
	@$(MAKE) --no-print-directory clean-nix

doctor: ## Показать окружение и выбранный режим
	@printf "  %-14s %s\n" "distro:"     "$$( . /etc/os-release 2>/dev/null && echo $$PRETTY_NAME || uname -s )"
	@printf "  %-14s %s\n" "docker:"     "$$(docker --version 2>/dev/null || echo нет)"
	@printf "  %-14s %s\n" "compose:"    "$(or $(COMPOSE),нет)"
	@printf "  %-14s %s\n" "nix-shell:"  "$(or $(NIX_SHELL_BIN),нет)"
	@printf "  %-14s %s\n" "gradle:"     "$(or $(SYS_GRADLE),нет)"
	@printf "  %-14s %s\n" "java:"       "$(or $(SYS_JAVA),нет)"
	@printf "  %-14s %s\n" "авто-режим:" "$(MODE)"
	@printf "  %-14s %s\n" "URL:"        "$(URL)"

docker-down: ## Остановить и удалить docker-контейнер (без образов)
	@$(COMPOSE) -f $(COMPOSE_FILE) down -v 2>/dev/null || true

docker-logs: ## Логи docker-контейнера
	@$(COMPOSE) -f $(COMPOSE_FILE) logs -f

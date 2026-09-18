.PHONY: init init-env init-infra init-backend init-frontend front back diagrams-check diagrams-export

COMPOSE ?= docker compose
MVN ?= mvn
NPM ?= npm
SPRING_PROFILES_ACTIVE ?= local

LOCAL_SERVICES := postgres minio rabbitmq clamav

ifeq ($(OS),Windows_NT)
WINDOWS_MAKE := 1
endif
ifneq ($(COMSPEC),)
WINDOWS_MAKE := 1
endif

ifeq ($(WINDOWS_MAKE),1)
SHELL := cmd.exe
.SHELLFLAGS := /c
SPRING_PROFILE_ENV = set SPRING_PROFILES_ACTIVE=$(SPRING_PROFILES_ACTIVE)&&
else
SPRING_PROFILE_ENV = SPRING_PROFILES_ACTIVE=$(SPRING_PROFILES_ACTIVE)
endif

init: init-env init-frontend init-infra init-backend

ifeq ($(WINDOWS_MAKE),1)
init-env:
	@if not exist .env copy .env.example .env
else
init-env:
	@test -f .env || cp .env.example .env
endif

init-infra:
	$(COMPOSE) up -d --wait $(LOCAL_SERVICES)

init-backend:
	$(MVN) -f backend/pom.xml -DskipTests compile

init-frontend:
	cd frontend && $(NPM) install

front: init-env init-frontend
	cd frontend && $(NPM) run dev

back: init-env init-infra init-backend
	$(SPRING_PROFILE_ENV) $(MVN) -f backend/pom.xml spring-boot:run

diagrams-check:
	sh scripts/check-diagrams.sh

diagrams-export:
	sh scripts/export-diagrams.sh
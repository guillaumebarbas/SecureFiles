.PHONY: init init-env init-infra init-backend init-frontend front back diagrams-check diagrams-export

COMPOSE ?= docker compose
MVN ?= mvn
NPM ?= npm
SPRING_PROFILES_ACTIVE ?= local

LOCAL_SERVICES := postgres minio rabbitmq clamav

init: init-env init-infra init-backend init-frontend

init-env:
	@test -f .env || cp .env.example .env

init-infra:
	$(COMPOSE) up -d --wait $(LOCAL_SERVICES)

init-backend:
	$(MVN) -f backend/pom.xml -DskipTests compile

init-frontend:
	$(NPM) --prefix frontend install

front: init-env init-frontend
	$(NPM) --prefix frontend run dev

back: init-env init-infra init-backend
	SPRING_PROFILES_ACTIVE=$(SPRING_PROFILES_ACTIVE) $(MVN) -f backend/pom.xml spring-boot:run

diagrams-check:
	sh scripts/check-diagrams.sh

diagrams-export:
	sh scripts/export-diagrams.sh
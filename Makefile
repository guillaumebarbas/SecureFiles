.PHONY: front back diagrams-check diagrams-export

front:
	npm --prefix frontend run dev

back:
	SPRING_PROFILES_ACTIVE=local mvn -f backend/pom.xml spring-boot:run

diagrams-check:
	sh scripts/check-diagrams.sh

diagrams-export:
	sh scripts/export-diagrams.sh
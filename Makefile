.PHONY: front back

front:
	npm --prefix frontend run dev

back:
	SPRING_PROFILES_ACTIVE=local mvn -f backend/pom.xml spring-boot:run
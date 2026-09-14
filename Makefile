.PHONY: front back

front:
	npm --prefix frontend run dev

back:
	mvn -f backend/pom.xml spring-boot:run
---
name: securefiles-testing
description: Use when adding tests, diagnosing a failing test, changing a domain invariant, or deciding test doubles and integration coverage in SecureFiles.
---

# Strategie de test SecureFiles

## A charger

- [Strategie de test](../../../rules/strategy_test.md)
- [Regles Java](../../../rules/java.md) pour les conventions du code et des tests backend.
- [Clean Code](../../../rules/clean_code.md)
- [AGENTS.md](../../../AGENTS.md)
- [TDD obligatoire](../tdd/SKILL.md)

## TDD obligatoire

Pour toute implementation frontend ou backend, charger [TDD](../tdd/SKILL.md) et appliquer red-green-refactor sur une seam publique avant d'elargir la modification.

## Decision par couche

1. **Domaine** : viser 100 % des lignes et branches utiles. Entrer par le handler de cas d'utilisation, dependre uniquement des ports, ne pas utiliser de framework de mocking et fournir des fakes en memoire implementant `TestState<T, ID>`. Structurer les scenarios en Given-When-Then.
2. **Application** : mocker ou faker les ports de repository, stockage et antivirus selon le contrat de la couche. Verifier les interactions, l'ordre des operations, la reservation conditionnelle et les changements de statut avec un test d'API pilote par contrat.
3. **Infrastructure** : utiliser Testcontainers pour les adaptateurs pilotes et valider les migrations contre une base propre. Un mock seul ne valide ni le protocole, ni le mapping, ni les limites de flux.
4. **REST** : verifier DTO, validation, codes HTTP, headers et ouverture du contenu uniquement pour `CLEAN`.
5. **Frontend** : verifier upload, rafraichissement, etats de scan, messages d'erreur et absence de download pour les statuts bloques.

## Regles d'ecriture

- Nommer les tests Java `methodUnderTest_shouldExpectedBehavior_whenCondition`.
- Utiliser Arrange, Act, Assert et une assertion de comportement par intention.
- Garder les tests deterministes, independants et rapides au niveau unitaire.
- Tester les erreurs et limites, pas seulement le chemin heureux.
- Lancer `mvn test` depuis `backend/` et `npm run build` depuis `frontend/` lorsque les couches concernees changent.

## Signal d'architecture

Si un test de domaine necessite Spring, JPA, HTTP, ClamAV ou un mock d'adaptateur, arreter et reexaminer la responsabilite testee avant d'ajouter la dependance.
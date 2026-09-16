---
name: securefiles-testing
description: Use when adding tests, diagnosing a failing test, changing a domain invariant, or deciding integration coverage in SecureFiles.
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

1. **Domaine** : viser 100 % des lignes et branches utiles. Appeler l'API publique d'un type de production reel. `@Mock` est autorise uniquement pour les ports ou collaborateurs de production dont depend ce systeme teste. Les fakes, stubs, spies, `TestState` et classes de support de test sont interdits. Structurer les scenarios en Given-When-Then.
2. **Application** : ne pas tester la couche d'orchestration. Tester uniquement les mappers purs sous `application/mapper`, avec des objets reels et sans double.
3. **Infrastructure** : ne pas tester la couche technique, ses adaptateurs, ses protocoles ou sa persistance. Les mappers de conversion purs restent les seuls elements testables s'ils existent.
4. **Application/controller** : ne pas tester la couche HTTP. Verifier les conversions
	par les mappers purs uniquement ; les invariants restent couverts par le domaine.
5. **Frontend** : verifier upload, rafraichissement, etats de scan, messages d'erreur et absence de download pour les statuts bloques.

## Regles d'ecriture

- Nommer les tests Java `methodUnderTest_shouldExpectedBehavior_whenCondition`.
- Utiliser Arrange, Act, Assert et une assertion de comportement par intention.
- Garder les tests deterministes, independants et rapides au niveau unitaire.
- Tester les erreurs et limites, pas seulement le chemin heureux.
- Lancer `mvn test` depuis `backend/` lorsque le domaine ou un mapper change. Pour une
	modification d'infrastructure sans mapper, verifier la compilation sans ajouter de test
	de couche.

## Signal d'architecture

Si un test de domaine necessite un port sortant, utiliser `@Mock` uniquement sur ce port de production et appeler le use case reel. Ne pas creer de fake, stub, spy ou classe auxiliaire.
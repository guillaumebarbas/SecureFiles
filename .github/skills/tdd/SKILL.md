---
name: tdd
description: Use for every new or modified frontend or backend implementation in SecureFiles. Follow the red-green-refactor loop, test public seams, and avoid implementation-coupled tests.
---

# Test-Driven Development SecureFiles

Ce skill est adapte de [mattpocock/skills](https://github.com/mattpocock/skills), notamment `skills/engineering/tdd/SKILL.md`, `tests.md` et `mocking.md` ; source sous licence MIT.

## Regle obligatoire

Charger ce skill avant toute implementation ou modification de code, que le code concerne le backend Java/Spring ou le frontend React/TypeScript. Une implementation ne commence pas avant d'avoir identifie le test qui doit echouer.

## Seam et comportement

- Identifier la seam publique a tester avant d'ecrire le test : API HTTP, cas d'utilisation, port, composant, hook ou fonction publique.
- Tester le comportement observable depuis cette interface, jamais une methode privee ou un detail interne.
- Confirmer que la seam correspond au contrat de la couche avant de multiplier les cas.
- Pour une evolution d'interface ou de decoupage, charger aussi le skill de design d'architecture avant de figer la seam.

## Boucle red-green-refactor

1. **Red** : ecrire un test lisible qui exprime un comportement et verifier qu'il echoue pour la bonne raison.
2. **Green** : produire uniquement l'implementation minimale qui fait passer ce test.
3. **Refactor** : ameliorer la structure apres le vert, sans changer le comportement ; relancer les tests.
4. Reprendre la boucle sur une seule tranche verticale a la fois.

## Application SecureFiles

- Domaine backend : viser 100 % des lignes et branches utiles avec des tests purs et des interfaces publiques.
- Application backend : tester les cas d'utilisation avec des doubles des ports et verifier les interactions de securite.
- Infrastructure backend : utiliser des tests d'integration ou de contrat pour PostgreSQL, le stockage et ClamAV.
- Frontend : tester les comportements visibles des composants, hooks et appels API ; ne pas tester la structure interne du composant.
- Respecter les invariants de `rules/strategy_test.md` et les noms de tests du projet.

## Anti-patterns

- Ne pas ecrire toute la suite de tests avant l'implementation : avancer par tranches verticales.
- Ne pas mocker les classes internes que le test controle ; mocker uniquement les limites externes ou les ports necessaires a l'isolation.
- Ne pas tester des methodes privees, des appels internes, un ordre d'appels non contractuel ou des compteurs d'invocation sans raison comportementale.
- Ne pas recalculer la valeur attendue avec le meme algorithme que le code ; utiliser une valeur litterale ou une source de verite independante.
- La refactorisation generale appartient a une etape separee apres le vert, pas au milieu d'une boucle rouge-verte.

## Validation

- Backend : lancer le test cible puis `mvn test` depuis `backend/` lorsque la tranche est stable.
- Frontend : lancer le test cible puis `npm run build` depuis `frontend/` lorsque la tranche est stable.
- Si aucun harnais de test n'existe encore, creer d'abord le plus petit harnais necessaire et le tester dans une tranche dediee.
---
name: securefiles-code-review
description: Use when reviewing, refactoring, debugging, or assessing a SecureFiles change for defects, regressions, architecture violations, or security risks.
---

# Revue de code SecureFiles

## A charger

- [AGENTS.md](../../../AGENTS.md)
- [README.md](../../../README.md)
- [Clean Code](../../../rules/clean_code.md)
- [Strategie de test](../../../rules/strategy_test.md)
- [Securite du flux](../securefiles-security/SKILL.md) si le changement touche upload, scan, stockage ou download.
- [Frontend UX/UI](../securefiles-frontend/SKILL.md) si le changement touche la console.

## Procedure

1. Identifier le port, le cas d'utilisation, l'adaptateur ou le composant qui decide directement le comportement.
2. Lire le diff et le test voisin le plus discriminant avant de conclure.
3. Verifier les invariants de securite : statut initial `PENDING_SCAN`, acces contenu reserve a `CLEAN`, echec antivirus ferme, flux non materialises et absence de donnees sensibles dans les logs.
4. Verifier les limites d'architecture : domaine pur, orchestration dans `application`, dependances externes dans `infrastructure`, DTO uniquement dans `interfaces/rest`.
5. Chercher les regressions de concurrence, de validation, de gestion d'erreur et de compatibilite HTTP.
6. Verifier qu'un test reproduit chaque comportement a risque et que la validation ciblee a ete executee.

## Compte rendu

- Presenter les problemes d'abord, tries par severite.
- Pour chaque probleme, donner le fichier concerne, le comportement observe, l'impact et le correctif attendu.
- Separarer les questions ouvertes et les risques residuels du resume des changements.
- Ne pas signaler un choix de style comme un bug s'il ne menace ni le contrat, ni la lisibilite, ni la securite.
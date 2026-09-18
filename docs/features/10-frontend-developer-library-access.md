Feature name: frontend-developer-library-access

## Summary

La navigation de la console affiche l'onglet Bibliothèque uniquement aux profils dont les rôles contiennent `developpeur`. Les visiteurs anonymes et les profils sans ce rôle ne voient pas cet onglet, tandis qu'un profil cumulant `developpeur` et `utilisateur` le voit.

## Status

Stable

## Public API / Contracts

- Aucun endpoint ni contrat backend n'est modifié.
- La visibilité du lien `/components` dépend du profil courant et de `UserProfile.roles`.
- La route `/components` reste une route frontend existante ; cette feature conditionne l'affichage de l'onglet, pas l'accès direct par URL.

## Quick usage

- Se connecter avec un profil `developpeur` pour afficher l'onglet Bibliothèque.
- Un profil `utilisateur` seul ne l'affiche pas.
- Un profil avec `developpeur` et `utilisateur` l'affiche.

## Design decisions

- Le filtrage est réalisé dans `App`, qui possède l'état du profil courant et construit la navigation.
- `SideNavBar` reste un composant de présentation générique et ne porte pas la règle de rôle.
- La règle utilise la présence du rôle `developpeur` avec `roles.includes('developpeur')`, afin de respecter les rôles cumulatifs.

## Tests & validation

- [App.test.tsx](../../frontend/src/tests/App.test.tsx) couvre les profils anonyme, utilisateur seul et développeur cumulatif, ainsi que l'ouverture de la Bibliothèque par un développeur.
- `npm test` : 19 fichiers et 90 tests réussis.
- `npm run build` : build Vite réussi.

## Related files

- [App.tsx](../../frontend/src/App.tsx)
- [App.test.tsx](../../frontend/src/tests/App.test.tsx)
- [filesApi.ts](../../frontend/src/api/filesApi.ts)

## Changelog

- 2026-09-18 - Restriction de visibilité de l'onglet Bibliothèque au rôle développeur.

## Notes

- Commit de la feature : `211fd00` (`feat(frontend): restrict library tab to developers`).

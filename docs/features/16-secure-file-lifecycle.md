Feature name: secure-file-lifecycle

## Summary

Cette feature separe le quota visible des reservations d'upload afin que seuls
les fichiers `CLEAN` soient comptes dans `usedBytes`. Elle expose le quota
authentifie dans le profil avec une barre de progression partagee et maintient
les statuts d'upload du Dashboard a jour sans navigation.

## Status

Stable

## Public API / Contracts

- `GET /api/v1/users/me/storage` renvoie `usedBytes` et `quotaBytes` pour
  l'utilisateur authentifie.
- `usedBytes` compte uniquement les fichiers `CLEAN`; `PENDING_SCAN` et
  `SCANNING` utilisent une reservation interne, tandis que `INFECTED` et
  `SCAN_FAILED` liberent cette reservation.
- Le Dashboard suit les fichiers `UPLOADING`, `PENDING_SCAN` et `SCANNING` et
  conserve les statuts terminaux `CLEAN`, `INFECTED`, `SCAN_FAILED` et `REJECTED`.

## Quick usage

- Executer `mvn --batch-mode test` depuis `backend/`.
- Executer `npm test -- --run` puis `npm run build` depuis `frontend/`.
- Ouvrir le profil pour consulter la section `Stockage disponible`.

## Design decisions

- `reserved_bytes` protege les uploads concurrents sans reduire le quota
  visible avant le resultat de l'analyse.
- `BarreProgression` reste un composant partage : son gradient, sa valeur
  maximale et son formatage sont configurables, avec une animation compatible
  avec `prefers-reduced-motion`.
- Le Dashboard possede le suivi des lignes non terminales deja presentes dans
  la liste; le formulaire d'upload reste le proprietaire du suivi immediat de
  l'upload qu'il vient d'accepter.
- Un seul polling de metadonnees est actif par fichier et il s'arrete au statut
  terminal ou au demontage du composant.

## Tests & validation

- Backend : `mvn --batch-mode test` passe avec 108 tests, 0 echec et 16 ignores.
- Frontend : `npm test -- --run` passe avec 24 fichiers et 118 tests.
- Frontend : `npm run build` termine avec succes.
- `git diff --check` termine avec succes.
- `make back` n'a pas pu lancer une seconde instance car le port `8080` etait
  deja occupe; le processus present repondait toutefois HTTP 200 avec un statut
  `/actuator/health` `UP`.

## Related files

- [README.md](../../README.md)
- [008-separate-clean-file-quota.yaml](../../backend/src/main/resources/db/changelog/changes/008-separate-clean-file-quota.yaml)
- [StorageQuotaController.java](../../backend/src/main/java/com/securefiles/application/controller/StorageQuotaController.java)
- [FileQuotaJpaRepository.java](../../backend/src/main/java/com/securefiles/infrastructure/repository/jpa/FileQuotaJpaRepository.java)
- [ProfilePage.tsx](../../frontend/src/pages/Profile/ProfilePage.tsx)
- [BarreProgression.tsx](../../frontend/src/shared/feedback/BarreProgression/BarreProgression.tsx)
- [DashboardPage.tsx](../../frontend/src/pages/Dashboard/DashboardPage.tsx)
- [FileUpload.tsx](../../frontend/src/shared/forms/FileUpload/FileUpload.tsx)

## Changelog

- 2026-09-19 - Added clean-only quota accounting, storage progress UI and
  continuous upload-status synchronization.

## Notes

- Les fichiers non `CLEAN` restent indisponibles au telechargement selon le
  contrat antivirus existant.

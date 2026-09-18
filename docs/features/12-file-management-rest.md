Feature name: file-management-rest

## Summary

Le backend expose des metadonnees de fichiers paginees, les capacites `canDownload` et `canDelete`, ainsi que la suppression autorisee par proprietaire ou administrateur. La console frontend reutilise le menu d'actions partage et le presente aussi dans la Bibliotheque des composants.

## Status

Stable

## Public API / Contracts

- `GET /api/v1/files` expose les capacites calculees pour le demandeur courant.
- `GET /api/v1/files/{id}` expose les metadonnees et les erreurs de scan connues.
- `DELETE /api/v1/files/{id}` renvoie `204 No Content` lorsque la suppression est autorisee.
- `GET /api/v1/files/{id}/content` reste reserve au proprietaire authentifie d'un fichier `CLEAN`.

## Quick usage

```bash
curl -b cookies.txt http://localhost:8080/api/v1/files
curl -b cookies.txt -X DELETE http://localhost:8080/api/v1/files/<id>
```

Dans la console, `FileActionsMenu` n'affiche le download que pour un fichier `CLEAN` avec `canDownload === true`, et la suppression seulement lorsque `canDelete === true` et que l'utilisateur est authentifie.

## Design decisions

- Les capacites sont calculees par le backend et servent uniquement a construire l'interface; les use cases gardent l'autorite sur les controles d'acces.
- L'orchestration API de suppression reste dans `DashboardPage`; `FileActionsMenu` ne connait que les actions et le callback public de suppression.
- Le composant partage est range sous `shared/files`, tandis que la Bibliotheque fournit uniquement des donnees de demonstration.

## Tests & validation

- `cd backend && mvn test` : 85 tests executes, 0 echec, 0 erreur, 10 ignores.
- `cd frontend && npm test -- --run` : 22 fichiers et 105 tests passes.
- `cd frontend && npm run build` : compilation TypeScript et build Vite reussis.

## Related files

- [ListFilesController.java](../../backend/src/main/java/com/securefiles/application/controller/ListFilesController.java)
- [DeleteFileController.java](../../backend/src/main/java/com/securefiles/application/controller/DeleteFileController.java)
- [ListFilesUseCase.java](../../backend/src/main/java/com/securefiles/domain/file/usecases/ListFilesUseCase.java)
- [DeleteFileUseCase.java](../../backend/src/main/java/com/securefiles/domain/file/usecases/DeleteFileUseCase.java)
- [FileActionsMenu.tsx](../../frontend/src/shared/files/FileActionsMenu.tsx)
- [DashboardPage.tsx](../../frontend/src/pages/Dashboard/DashboardPage.tsx)
- [SharedComponentsShowcasePage.tsx](../../frontend/src/pages/SharedComponentsShowcase/SharedComponentsShowcasePage.tsx)

## Changelog

- 2026-09-18 - Added paginated file metadata capabilities, authorized deletion, and shared frontend file actions.

## Notes

- Commits: `7a9798e`, `14e2bfb`, `559f5eb`, `8090bdd`.
- The local prompt journal and `excalidraw.log` remain outside the delivery commits.
Feature name: file-list-server-pagination

## Summary

Replaces frontend-only pagination in the recent-files register with backend pagination. The API now returns only the requested page and its navigation metadata, so the console does not load or retain the complete file list when the volume grows.

## Status

Stable

## Public API / Contracts

- [GET /api/v1/files](../../backend/src/main/java/com/securefiles/application/controller/ListFilesController.java) accepts `page` starting at `1` and `size` between `1` and `50`, defaulting to `1` and `10`.
- The response envelope contains `content`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`, and `hasPrevious`.
- Results use the stable order `createdAt DESC, id DESC`; an out-of-range page returns an empty `200` envelope.
- Invalid pagination parameters return `400 Bad Request` with the stable `INVALID_PAGINATION` code.

## Quick usage

Request a page with:

```http
GET /api/v1/files?page=2&size=25
```

The Dashboard sends the active page and page size to the API and replaces the visible rows with the returned page.

## Design decisions

- Spring Data `Page` and `Pageable` remain in infrastructure; the domain exposes the repository-neutral `StoredFilePage` type.
- `GenericTable` keeps local pagination for reusable local datasets and exposes a controlled server-pagination mode for backend-backed tables.
- The database adds an index on `created_at, id` to support the recent-files ordering.

## Tests & validation

- Backend use-case and mapper tests cover page metadata, invalid parameters, author/failure mapping, and response-envelope mapping.
- Frontend API, table, Dashboard, and App tests cover query parameters, page navigation, page replacement, selected page size, and existing local-table pagination.
- `mvn test` passed: 77 tests passed, 10 opt-in integration tests skipped.
- `npm run test` passed: 19 test files and 86 tests passed.
- `npm run build` passed.
- `git diff --check` passed.

## Related files

- [ListFilesController](../../backend/src/main/java/com/securefiles/application/controller/ListFilesController.java)
- [ListFilesUseCase](../../backend/src/main/java/com/securefiles/domain/file/usecases/ListFilesUseCase.java)
- [JPA repository adapter](../../backend/src/main/java/com/securefiles/infrastructure/repository/jpa/JpaStoredFileRepositoryAdapter.java)
- [List-files index migration](../../backend/src/main/resources/db/changelog/changes/003-create-stored-file-list-index.yaml)
- [Files API](../../frontend/src/api/filesApi.ts)
- [Dashboard](../../frontend/src/pages/Dashboard/DashboardPage.tsx)
- [GenericTable](../../frontend/src/shared/data/GenericTable.tsx)
- [HTTP contract](../../README.md)

## Changelog

- 2026-09-17 - Added backend pagination for recent files and connected the Dashboard to controlled server navigation.

## Notes

- Branch: `feat/file-list-server-pagination`.
- Commits: `d2c6772 feat(backend): add paginated file listing`; `e004f0c feat(frontend): use server pagination for recent files`.

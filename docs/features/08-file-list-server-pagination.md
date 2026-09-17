Feature name: file-list-server-pagination-and-querying

## Summary

Replaces frontend-only pagination in the recent-files register with backend pagination. The API now returns only the requested, globally sorted and filtered page with navigation metadata, so the console does not load or retain the complete file list when the volume grows.

## Status

Stable

## Public API / Contracts

- [GET /api/v1/files](../../backend/src/main/java/com/securefiles/application/controller/ListFilesController.java) accepts `page` starting at `1` and `size` between `1` and `50`, defaulting to `1` and `10`.
- `sort` is allowlisted to `name`, `author`, `size`, and `createdAt`; `direction` accepts `asc` or `desc`.
- Repeated `status` parameters have OR semantics. An absent status filter includes all seven canonical statuses.
- The response envelope contains `content`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`, and `hasPrevious`.
- Results use the requested order with the stable `id DESC` tie-breaker; filtering and ordering happen before pagination and counting.
- An out-of-range page returns an empty `200` envelope.
- Invalid pagination parameters return `400 Bad Request` with the stable `INVALID_PAGINATION` code. Invalid sort, direction, or status values return `400 Bad Request` with `INVALID_LIST_QUERY`.

## Quick usage

Request a page with:

```http
GET /api/v1/files?page=2&size=25&sort=author&direction=asc&status=CLEAN&status=SCAN_FAILED
```

The Dashboard sends the active page, page size, sort and selected statuses to the API and replaces the visible rows with the returned page. Changing a criterion resets the page to `1` while page navigation preserves the active criteria.

## Design decisions

- Spring Data `Page` and `Pageable` remain in infrastructure; the domain exposes the repository-neutral `FileListQuery` and `StoredFilePage` types.
- `GenericTable` keeps local pagination for reusable local datasets and exposes a controlled server-pagination mode for backend-backed tables.
- The database query uses allowlisted sort expressions, joins `app_user` for author ordering, and always adds `id DESC` for deterministic pagination.

## Tests & validation

- Backend use-case and mapper tests cover page metadata, invalid parameters, author/failure mapping, and response-envelope mapping. The use case also verifies propagation of the typed sort/filter query.
- Frontend API, table, Dashboard, and App tests cover repeated status query parameters, server sorting, status filtering, page navigation, page replacement, selected page size, and existing local-table pagination.
- `mvn test` passed: 78 tests passed, 10 opt-in integration tests skipped.
- The full frontend validation passed: 89 tests passed with no failures.
- `npm run build` passed.
- `git diff --check` passed.

## Related files

- [ListFilesController](../../backend/src/main/java/com/securefiles/application/controller/ListFilesController.java)
- [FileListQuery](../../backend/src/main/java/com/securefiles/domain/file/model/list/FileListQuery.java)
- [StoredFile JPA repository](../../backend/src/main/java/com/securefiles/infrastructure/repository/jpa/StoredFileJpaRepository.java)
- [ListFilesUseCase](../../backend/src/main/java/com/securefiles/domain/file/usecases/ListFilesUseCase.java)
- [JPA repository adapter](../../backend/src/main/java/com/securefiles/infrastructure/repository/jpa/JpaStoredFileRepositoryAdapter.java)
- [List-files index migration](../../backend/src/main/resources/db/changelog/changes/003-create-stored-file-list-index.yaml)
- [Files API](../../frontend/src/api/filesApi.ts)
- [Dashboard](../../frontend/src/pages/Dashboard/DashboardPage.tsx)
- [GenericTable](../../frontend/src/shared/data/GenericTable.tsx)
- [HTTP contract](../../README.md)

## Changelog

- 2026-09-18 - Added backend pagination, server-side sorting and status filtering for recent files, then connected the Dashboard to controlled server navigation.

## Notes

- Branch: `feature/server-file-list-pagination`.
- Commits: `4a0f70a feat(backend): add paginated file listing`; `aa6a40f feat(frontend): use server pagination for recent files`.

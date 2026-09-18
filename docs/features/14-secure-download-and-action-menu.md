Feature name: secure-download-and-action-menu

## Summary

This delivery allows any authenticated user to download a `CLEAN` file while keeping incomplete, unscanned, infected, and failed files unavailable. It also keeps file action menus visible above table edges and scroll containers so the available actions remain usable on small viewports.

## Status

Stable

## Public API / Contracts

- `GET /api/v1/files/{id}/content` streams content for any authenticated requester when the file status is `CLEAN`; anonymous requests remain blocked and non-clean files remain unavailable. See [README.md](../../README.md).
- `GET /api/v1/files` exposes `canDownload` for authenticated requesters on `CLEAN` files and keeps `canDelete` owner/admin-scoped.
- Local startup remains available through `make init`, as documented by [feature 13](13-project-initialization-workflow.md).

## Quick usage

- Run `make init`, then start the API and console with `make back` and `make front`.
- Sign in, open the file library, and download a `CLEAN` file owned by another authenticated user.
- Open a row action menu near the bottom or side of the viewport; the menu is rendered above the page surface and remains within the viewport.

## Design decisions

- The owner authorization check was removed only from the download use case and download capability calculation. Authentication, `CLEAN` status, storage metadata verification, streaming, and owner/admin deletion rules remain enforced.
- Action menus render in `document.body` with fixed, viewport-aware coordinates, flipping above the trigger and scrolling internally when space is limited.

## Tests & validation

- `DownloadFileUseCaseTest` and `ListFilesUseCaseTest`: 15 tests passed.
- Frontend action-menu/dashboard suites: 4 suites and 28 tests passed; `npm run build` succeeded.
- Full backend `mvn test`: 86 tests passed, 0 failures, 0 errors, and 10 skipped.

## Related files

- [DownloadFileUseCase.java](../../backend/src/main/java/com/securefiles/domain/file/usecases/DownloadFileUseCase.java)
- [ListFilesUseCase.java](../../backend/src/main/java/com/securefiles/domain/file/usecases/ListFilesUseCase.java)
- [DownloadFileUseCaseTest.java](../../backend/src/test/java/com/securefiles/domain/file/usecases/DownloadFileUseCaseTest.java)
- [ListFilesUseCaseTest.java](../../backend/src/test/java/com/securefiles/domain/file/usecases/ListFilesUseCaseTest.java)
- [menuActions.tsx](../../frontend/src/shared/actions/menuActions.tsx)
- [menuActions.test.tsx](../../frontend/src/tests/shared/actions/menuActions.test.tsx)

## Changelog

- 2026-09-18 — allowed authenticated cross-owner downloads for `CLEAN` files and corrected viewport handling for file action menus.

## Notes

- Commit `d48ac28` is already documented by feature `13`; this summary focuses on the download-access and action-menu changes delivered afterward.
- Commit `051a1e6` adds Windows-compatible local startup commands and is included in the branch history.
- Commit `61a2570` adds the TypeSafe skill registry, its license and guidance, the Claude skill link, and the Excalidraw diagnostic log to the branch.
- The branch now contains the prompt journal changes from `resume.md`; they are documented as branch context and are not part of the secure file behavior.

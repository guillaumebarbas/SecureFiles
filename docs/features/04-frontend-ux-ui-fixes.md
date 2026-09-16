Feature name: frontend-ux-ui-fixes

## Summary

Improves the SecureFiles console shell and upload feedback so the current route, backend availability, and file-processing state are easier to understand. The primary navigation no longer exposes the unused `Fichiers` tab, while its route remains available for future work.

## Status

Implementing

## Public API / Contracts

- [App](../../frontend/src/App.tsx) exposes `Dashboard` and `Bibliotheque` in the primary navigation, keeps the `/files` route, and supplies route metadata to the shared header.
- [Header](../../frontend/src/shared/layout/Header/Header.tsx) accepts explicit `title`, `eyebrow`, and `description` values; the title precedes the blue route context and the description is aligned in the secondary group.
- [filesApi](../../frontend/src/api/filesApi.ts) consumes the existing `GET /actuator/health` contract and maps Actuator status `UP` to `online`; every other response or request error is represented as `offline`.
- [FileUpload](../../frontend/src/shared/forms/FileUpload/FileUpload.tsx) reads `GET /api/v1/files/config` before submission, uploads through `POST /api/v1/files`, and polls `GET /api/v1/files/{id}` independently of feedback visibility until a terminal scan status.
- [Vite configuration](../../frontend/vite.config.ts) proxies both `/api` and `/actuator` to the local backend during development. No new backend health endpoint is introduced.

## Quick usage

Run the backend, then execute `npm run dev` from `frontend/` or `make front` from the repository root. Open the dashboard to see the backend status, select a file after the upload limit is loaded, and follow the transfer and antivirus status in the dashboard register.

## Design decisions

- The shared header uses explicit route metadata instead of duplicating introductory copy inside each page; the title and blue context stay grouped on the left while the description and optional actions occupy the right group.
- Actuator health is reused as the health seam, keeping frontend availability feedback separate from file API requests and avoiding a duplicate backend endpoint.
- Accepted upload feedback is a presentation state: it starts its dismissal timer after acceptance, fades out after 10 seconds, and is removed after a 320 ms exit transition without stopping metadata polling. Error feedback remains visible and retryable.
- The `Fichiers` navigation item is hidden without deleting the `/files` route or `FilesPage`, so the unfinished view can be restored without recreating its structure.

## Tests & validation

- [App tests](../../frontend/src/tests/App.test.tsx) cover route metadata, online/offline backend status, and the absence of the `Fichiers` navigation link.
- [API tests](../../frontend/src/tests/api/filesApi.test.ts) cover the Actuator health mapping and existing file API behavior.
- [Header tests](../../frontend/src/tests/shared/layout/Header.test.tsx), [BackendStatus tests](../../frontend/src/tests/shared/layout/BackendStatus.test.tsx), and [SideNavBar tests](../../frontend/src/tests/shared/layout/SideNavBar.test.tsx) cover the shared shell contracts.
- [FileUpload tests](../../frontend/src/tests/shared/forms/FileUpload/FileUpload.test.tsx) cover upload configuration, feedback dismissal, polling, progress, and error behavior.
- [Dashboard tests](../../frontend/src/tests/pages/Dashboard/DashboardPage.test.tsx) cover the dashboard register and upload-driven refresh behavior.
- Frontend validation observed on 2026-09-16: `npm test -- --run` passed with 17 test files and 55 tests; `npm run build` completed successfully.
- `git diff --check -- frontend` passed. Browser visual validation for this latest change is not determined from the repository state.

## Related files

- [styles.css](../../frontend/src/styles.css)
- [DashboardPage](../../frontend/src/pages/Dashboard/DashboardPage.tsx)
- [SharedComponentsShowcasePage](../../frontend/src/pages/SharedComponentsShowcase/SharedComponentsShowcasePage.tsx)
- [FileUpload styles](../../frontend/src/shared/forms/FileUpload/style.ts)
- [Header styles](../../frontend/src/shared/layout/Header/style.ts)
- [resume](../../resume.md)

## Changelog

- 2026-09-16 - Refined the console header, backend status indicator, upload feedback lifecycle, and primary navigation.

## Notes

- The feature is documented from unstaged changes on `feat/frontend-ux-ui-fixes`; no commit exists beyond the configured upstream at the time of analysis.
- `backend/src/test/resources/integration/MicrosoftTeams.pkg` is an unrelated untracked file and is not part of this feature.

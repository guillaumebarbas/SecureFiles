Feature name: frontend-ux-ui-fixes

## Summary

Improves the SecureFiles console shell and upload feedback so the current route, backend availability, and file-processing state are easier to understand. The primary navigation exposes the `Profil` page and shared component library, while the header provides the reusable login control.

## Status

Implementing

## Public API / Contracts

- [App](../../frontend/src/App.tsx) exposes `Dashboard`, `Profil`, and `Bibliotheque` in the primary navigation, keeps the `/files` route, and supplies route metadata to the shared header.
- [Header](../../frontend/src/shared/layout/Header/Header.tsx) accepts explicit `title` and `eyebrow` values; the title precedes the blue route context and the right-side group hosts the login control.
- [Login](../../frontend/src/shared/layout/Login/Login.tsx) renders an accentuated disconnected `Se connecter` action that opens local `Connexion` and `Inscription` forms, or a connected user account with an absolute `Se déconnecter` menu revealed by hover, focus, or click.
- [ProfilePage](../../frontend/src/pages/Profile/ProfilePage.tsx) displays the local profile name and the current `utilisateur` role. These are presentation values until authentication is implemented.
- [filesApi](../../frontend/src/api/filesApi.ts) consumes the existing `GET /actuator/health` contract and maps Actuator status `UP` to `online`; every other response or request error is represented as `offline`.
- [FileUpload](../../frontend/src/shared/forms/FileUpload/FileUpload.tsx) reads `GET /api/v1/files/config` before submission, uploads through `POST /api/v1/files`, and polls `GET /api/v1/files/{id}` independently of feedback visibility until a terminal scan status.
- [Vite configuration](../../frontend/vite.config.ts) proxies both `/api` and `/actuator` to the local backend during development. No new backend health endpoint is introduced.

## Quick usage

Run the backend, then execute `npm run dev` from `frontend/` or `make front` from the repository root. Open the dashboard to see the backend status, select a file after the upload limit is loaded, and follow the transfer and antivirus status in the dashboard register.

## Design decisions

- The shared header uses explicit route metadata instead of duplicating introductory copy inside each page; the title and blue context stay grouped on the left while the login control occupies the right group.
- Login state remains local and callback-driven. The header exposes the form without authentication side effects, while the component showcase supplies a local mock user after form submission to demonstrate both states.
- The account menu and authentication panel use focus, pointer and Escape handling in addition to hover; both panels touch the control so pointer travel cannot close the interaction in an empty gap.
- The header establishes a higher stacking context than the page content so its absolute Login panels remain visible above the main view.
- Actuator health is reused as the health seam, keeping frontend availability feedback separate from file API requests and avoiding a duplicate backend endpoint.
- Accepted upload feedback is a presentation state: it starts its dismissal timer after acceptance, fades out after 10 seconds, and is removed after a 320 ms exit transition without stopping metadata polling. Error feedback remains visible and retryable.
- The `Fichiers` navigation item is hidden without deleting the `/files` route or `FilesPage`, so the unfinished view can be restored without recreating its structure.

## Tests & validation

- [App tests](../../frontend/src/tests/App.test.tsx) cover route metadata, online/offline backend status, and the absence of the `Fichiers` navigation link.
- [Login tests](../../frontend/src/tests/shared/layout/Login.test.tsx) cover the local connection and registration forms, callback submission, connected-state hover/focus menu exposure, Escape dismissal, and logout callbacks.
- [Profile tests](../../frontend/src/tests/pages/Profile/ProfilePage.test.tsx) cover the profile name, current role, and removal of the available-roles section.
- [Shared components showcase tests](../../frontend/src/tests/pages/SharedComponentsShowcase/SharedComponentsShowcasePage.test.tsx) cover the local connection mock.
- [API tests](../../frontend/src/tests/api/filesApi.test.ts) cover the Actuator health mapping and existing file API behavior.
- [Header tests](../../frontend/src/tests/shared/layout/Header.test.tsx), [BackendStatus tests](../../frontend/src/tests/shared/layout/BackendStatus.test.tsx), and [SideNavBar tests](../../frontend/src/tests/shared/layout/SideNavBar.test.tsx) cover the shared shell contracts.
- [FileUpload tests](../../frontend/src/tests/shared/forms/FileUpload/FileUpload.test.tsx) cover upload configuration, feedback dismissal, polling, progress, and error behavior.
- [Dashboard tests](../../frontend/src/tests/pages/Dashboard/DashboardPage.test.tsx) cover the dashboard register and upload-driven refresh behavior.
- Frontend validation observed on 2026-09-17: targeted Login, Profile, App, Header, and showcase tests passed; full-suite and build validation are part of the current delivery.
- `git diff --check -- frontend` passed. Browser visual validation for this latest change is not determined from the repository state.

## Related files

- [styles.css](../../frontend/src/styles.css)
- [DashboardPage](../../frontend/src/pages/Dashboard/DashboardPage.tsx)
- [SharedComponentsShowcasePage](../../frontend/src/pages/SharedComponentsShowcase/SharedComponentsShowcasePage.tsx)
- [FileUpload styles](../../frontend/src/shared/forms/FileUpload/style.ts)
- [Header styles](../../frontend/src/shared/layout/Header/style.ts)
- [Login styles](../../frontend/src/shared/layout/Login/style.ts)
- [Profile styles](../../frontend/src/pages/Profile/style.ts)
- [resume](../../resume.md)

## Changelog

- 2026-09-16 - Refined the console header, backend status indicator, upload feedback lifecycle, and primary navigation.
- 2026-09-17 - Added the Profile route, reusable Login control, connected-state showcase, and header account action.
- 2026-09-17 - Added local Login and Inscription forms, accentuated connection actions, continuous hover panels, and centered profile roles.
- 2026-09-17 - Kept Login panels above the page content and moved the current profile role into the profile information section.

## Notes

- The feature is documented from unstaged changes on `feat/frontend-ux-ui-fixes`; no commit exists beyond the configured upstream at the time of analysis.
- `backend/src/test/resources/integration/MicrosoftTeams.pkg` is an unrelated untracked file and is not part of this feature.

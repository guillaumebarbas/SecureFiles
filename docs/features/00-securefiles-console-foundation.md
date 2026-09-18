Feature name: securefiles-console-foundation

## Summary

This feature establishes the SecureFiles console foundation with a Spring Boot file flow, a React/Vite console shell, and a tested set of shared UI primitives. The dashboard can upload files, hydrate its recent-files register from the owner-scoped API, follow scan status, and paginate the shared table locally.

## Status

Implementing

## Public API / Contracts

- Frontend navigation currently exposes `/` and `/files` from [App](../../frontend/src/App.tsx).
- The dashboard recent-files register is hydrated by `GET /api/v1/files`; the separate [FilesPage](../../frontend/src/pages/Files/FilesPage.tsx) remains a placeholder screen.
- Reusable UI contracts are available through [Button](../../frontend/src/shared/actions/Button.tsx), [GenericTable](../../frontend/src/shared/data/GenericTable.tsx), [SearchBar](../../frontend/src/shared/forms/SearchBar.tsx), [Tooltip](../../frontend/src/shared/feedback/Tooltip.tsx), [Tag](../../frontend/src/shared/feedback/Tag.tsx), and [SideNavBar](../../frontend/src/shared/layout/SideNavBar/SideNavBar.tsx).
- The backend HTTP contract and owner-scoped file listing are documented in [README.md](../../README.md) and implemented through the file controllers and domain ports.

## Quick usage

- Run `make front` from the repository root to start the Vite console defined in [Makefile](../../Makefile).
- Run `make back` from the repository root to start the Spring Boot application defined in [Makefile](../../Makefile).
- Open the console and use the sidebar from [SideNavBar](../../frontend/src/shared/layout/SideNavBar/SideNavBar.tsx) to switch between the shared component library and the files registry placeholder.

## Design decisions

- Navigation stays dependency-light: [App](../../frontend/src/App.tsx) uses `window.history` and `popstate` instead of adding a router for the initial two-screen shell.
- Shared components encode accessibility at the contract level: [Button](../../frontend/src/shared/actions/Button.tsx) adds tooltips for icon-only actions, [GenericTable](../../frontend/src/shared/data/GenericTable.tsx) exposes sortable headers with `aria-sort` and optional local pagination, and [BackendStatus](../../frontend/src/shared/layout/BackendStatus/BackendStatus.tsx) keeps backend state explicit.
- Backend bootstrap stays intentionally thin: [pom.xml](../../backend/pom.xml) only wires Spring Boot web, validation, JPA, Actuator, and PostgreSQL so later slices can add domain and use-case code without reworking the project base.

## Tests & validation

- Frontend tests live under [frontend/src/tests](../../frontend/src/tests) and cover app navigation, showcase behavior, button variants and tooltips, sortable tables, and layout/status components.
- Test infrastructure is configured with Vitest, jsdom, and Testing Library in [vitest.config.ts](../../frontend/vitest.config.ts) and [setup.ts](../../frontend/src/tests/setup.ts).
- Observed validation commands for this delivery are `mvn -f backend/pom.xml test`, `npm --prefix frontend run test`, and `npm --prefix frontend run build`.
- Backend tests cover the owner-scoped list use case and metadata mapper; frontend tests cover API listing, dashboard hydration/refresh/error states, upload polling, and table pagination.

## Related files

- [frontend/src/App.tsx](../../frontend/src/App.tsx)
- [frontend/src/pages/Files/FilesPage.tsx](../../frontend/src/pages/Files/FilesPage.tsx)
- [frontend/src/pages/SharedComponentsShowcase/SharedComponentsShowcasePage.tsx](../../frontend/src/pages/SharedComponentsShowcase/SharedComponentsShowcasePage.tsx)
- [frontend/src/shared/actions/Button.tsx](../../frontend/src/shared/actions/Button.tsx)
- [frontend/src/shared/data/GenericTable.tsx](../../frontend/src/shared/data/GenericTable.tsx)
- [backend/src/main/java/com/securefiles/SecureFilesApplication.java](../../backend/src/main/java/com/securefiles/SecureFilesApplication.java)
- [backend/src/main/resources/application.yml](../../backend/src/main/resources/application.yml)
- [README.md](../../README.md)

## Changelog

- 2026-09-15 — Added the backend bootstrap, owner-scoped file listing, frontend console shell, persistent recent-files register, shared UI primitives, local table pagination, frontend test harness, and supporting delivery/tooling documentation.

## Notes

- Observed commit series layers workflow guidance, backend bootstrap, file flow, frontend console foundation, and frontend test coverage.
- File content remains in private MinIO storage while the list reads owner-filtered metadata from PostgreSQL; the frontend pagination does not expose storage details or alter download authorization.
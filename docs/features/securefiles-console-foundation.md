Feature name: securefiles-console-foundation

## Summary

This feature establishes the first SecureFiles delivery foundation with a Spring Boot backend bootstrap, a React/Vite console shell, and a tested set of shared UI primitives. It provides a navigable frontend reference surface and project tooling for later file-flow implementation without yet wiring the documented file HTTP endpoints.

## Status

Implementing

## Public API / Contracts

- Frontend navigation currently exposes `/` and `/files` from [App](../../frontend/src/App.tsx).
- The files registry view is present as a placeholder screen in [FilesPage](../../frontend/src/pages/Files/FilesPage.tsx).
- Reusable UI contracts are available through [Button](../../frontend/src/shared/actions/Button.tsx), [GenericTable](../../frontend/src/shared/data/GenericTable.tsx), [SearchBar](../../frontend/src/shared/forms/SearchBar.tsx), [Tooltip](../../frontend/src/shared/feedback/Tooltip.tsx), [Tag](../../frontend/src/shared/feedback/Tag.tsx), and [SideNavBar](../../frontend/src/shared/layout/SideNavBar/SideNavBar.tsx).
- The target backend HTTP contract is documented in [README.md](../../README.md), while the observed backend implementation is currently limited to [SecureFilesApplication](../../backend/src/main/java/com/securefiles/SecureFilesApplication.java) and Actuator exposure configured in [application.yml](../../backend/src/main/resources/application.yml).

## Quick usage

- Run `make front` from the repository root to start the Vite console defined in [Makefile](../../Makefile).
- Run `make back` from the repository root to start the Spring Boot application defined in [Makefile](../../Makefile).
- Open the console and use the sidebar from [SideNavBar](../../frontend/src/shared/layout/SideNavBar/SideNavBar.tsx) to switch between the shared component library and the files registry placeholder.

## Design decisions

- Navigation stays dependency-light: [App](../../frontend/src/App.tsx) uses `window.history` and `popstate` instead of adding a router for the initial two-screen shell.
- Shared components encode accessibility at the contract level: [Button](../../frontend/src/shared/actions/Button.tsx) adds tooltips for icon-only actions, [GenericTable](../../frontend/src/shared/data/GenericTable.tsx) exposes sortable headers with `aria-sort`, and [BackendStatus](../../frontend/src/shared/layout/BackendStatus/BackendStatus.tsx) keeps backend state explicit.
- Backend bootstrap stays intentionally thin: [pom.xml](../../backend/pom.xml) only wires Spring Boot web, validation, JPA, Actuator, and PostgreSQL so later slices can add domain and use-case code without reworking the project base.

## Tests & validation

- Frontend tests live under [frontend/src/tests](../../frontend/src/tests) and cover app navigation, showcase behavior, button variants and tooltips, sortable tables, and layout/status components.
- Test infrastructure is configured with Vitest, jsdom, and Testing Library in [vitest.config.ts](../../frontend/vitest.config.ts) and [setup.ts](../../frontend/src/tests/setup.ts).
- Observed validation commands for this delivery are `mvn -f backend/pom.xml test`, `npm --prefix frontend ci`, `npm --prefix frontend run test`, and `npm --prefix frontend run build`.
- No dedicated backend test file was observed in [backend/src/test/java](../../backend/src/test/java); the current backend validation confirms the bootstrap project builds and tests cleanly.

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

- 2026-09-15 — Added the backend bootstrap, frontend console shell, shared UI primitives, frontend test harness, and supporting delivery/tooling documentation.

## Notes

- Observed commit series layers workflow guidance, backend bootstrap, frontend console foundation, and frontend test coverage.
- The documented SecureFiles file HTTP contract remains a target contract in [README.md](../../README.md); no upload, listing, metadata, or download use case is implemented yet in backend application code.
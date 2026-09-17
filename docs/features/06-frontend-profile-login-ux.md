Feature name: frontend-profile-login-ux

## Summary

Adds a local Profile view and a reusable Login control to the SecureFiles console. The showcase demonstrates connection, registration, and logout states without adding backend authentication or persistent credentials.

## Status

Stable

## Public API / Contracts

- [App](../../frontend/src/App.tsx) exposes the `/profile` route and the `Profil` navigation entry, and provides route metadata to the shared header.
- [Login](../../frontend/src/shared/layout/Login/Login.tsx) accepts optional `onLogin`, `onRegister`, `onLogout`, and `user` callbacks/values for local state integration.
- [ProfilePage](../../frontend/src/pages/Profile/ProfilePage.tsx) displays `Utilisateur local` with the current `utilisateur` role.
- The login and registration forms validate a non-empty normalized pseudo in the browser and do not call an authentication endpoint.

## Quick usage

- Run `npm run dev` from `frontend/`.
- Open the dashboard to use `Se connecter`, or open the `Profil` route to inspect the local profile.
- In the shared components showcase, submit the local forms to display the connected account and the logout action.

## Design decisions

- Login state is callback-driven and local so the UI can demonstrate both disconnected and connected states without introducing authentication infrastructure.
- The connected account menu and disconnected form panel remain directly attached to the header control and are layered above page content.
- Profile information is intentionally limited to the local display name and current role; the removed available-roles list is not represented as an authorization contract.

## Tests & validation

- [Login tests](../../frontend/src/tests/shared/layout/Login.test.tsx) cover connection, registration, normalized pseudo validation, connected-state menu behavior, Escape dismissal, and logout.
- [Profile tests](../../frontend/src/tests/pages/Profile/ProfilePage.test.tsx) cover the local name, current role, and absence of the available-roles section.
- [App tests](../../frontend/src/tests/App.test.tsx), [Header tests](../../frontend/src/tests/shared/layout/Header.test.tsx), and [showcase tests](../../frontend/src/tests/pages/SharedComponentsShowcase/SharedComponentsShowcasePage.test.tsx) cover route metadata, header integration, and the local showcase flow.
- `npm test -- --run` passed: 19 files and 66 tests.
- `npm run build` passed with Vite.
- `git diff --check` passed before the commits.

## Related files

- [Login styles](../../frontend/src/shared/layout/Login/style.ts)
- [Header](../../frontend/src/shared/layout/Header/Header.tsx)
- [Global styles](../../frontend/src/styles.css)
- [Shared components showcase](../../frontend/src/pages/SharedComponentsShowcase/SharedComponentsShowcasePage.tsx)

## Changelog

- 2026-09-17 - Added the Profile route, local Login and Inscription forms, connected account menu, showcase callbacks, and frontend coverage.

## Notes

- The feature is frontend-only. No backend endpoint, token, credential storage, or authentication persistence was added.
- Unrelated local changes were preserved separately in a stash and are not part of this feature branch delivery.
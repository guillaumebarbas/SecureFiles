Feature name: secure-file-access-authentication

## Summary

Adds persistent user accounts, JWT-backed sessions, and the console flows needed to
authenticate before uploading. The file register exposes non-content metadata publicly
with an author, while detailed metadata, upload, and download operations remain protected.

## Status

Stable

## Public API / Contracts

- [POST /api/v1/auth/register](../../backend/src/main/java/com/securefiles/application/controller/UserAuthenticationController.java) creates a public account with allowed cumulative roles and a password between 8 and 255 characters.
- [POST /api/v1/auth/login](../../backend/src/main/java/com/securefiles/application/controller/UserAuthenticationController.java) creates a revocable, 30-day session and sends its JWT only in the HttpOnly `SECUREFILES_AUTH` cookie.
- [GET /api/v1/users/me](../../backend/src/main/java/com/securefiles/application/controller/CurrentUserController.java) returns the current profile, or `204 No Content` when no valid session exists.
- [GET /api/v1/files](../../backend/src/main/java/com/securefiles/application/controller/ListFilesController.java) returns public file metadata including `author`; [GET /api/v1/files/{id}](../../backend/src/main/java/com/securefiles/application/controller/GetFileMetadataController.java), content downloads, and uploads remain authenticated and owner-scoped.
- [FileUpload](../../frontend/src/shared/forms/FileUpload/FileUpload.tsx) requests authentication before opening the file picker for an anonymous visitor.

## Quick usage

1. Start the backend and frontend with `make back` and `make front`.
2. Register and log in from the header before selecting a file for upload.
3. Browse the recent-files register without a session; sign in to upload or access an owned file's details and content.

## Design decisions

- The JWT identifies a persisted session that is revoked at logout, so a token is not sufficient after its associated session is inactive.
- User-facing author names are resolved from file owners; unresolved legacy owners are displayed as `Auteur inconnu` instead of exposing internal identifiers.
- The anonymous file register contains no file bytes, storage keys, hashes, download links, or private detailed metadata.

## Tests & validation

- [User domain tests](../../backend/src/test/java/com/securefiles/domain/user) cover account creation, authentication, profile lookup, and logout use cases.
- [File listing tests](../../backend/src/test/java/com/securefiles/domain/file/usecases/ListFilesUseCaseTest.java) cover global ordered listing, resolved authors, and neutral legacy-author handling.
- [Frontend tests](../../frontend/src/tests) cover registration and login errors, anonymous session handling, public file-register display, and authentication before file selection.
- `mvn test` passed on 2026-09-17: 73 tests passed, 10 opt-in integration tests skipped.
- `npm test -- --run` passed on 2026-09-17: 19 test files and 84 tests passed.
- `npm run build` passed on 2026-09-17.

## Related files

- [Authentication configuration](../../backend/src/main/java/com/securefiles/config/AuthenticationProperties.java)
- [JWT filter](../../backend/src/main/java/com/securefiles/infrastructure/security/JwtAuthenticationFilter.java)
- [User domain](../../backend/src/main/java/com/securefiles/domain/user)
- [Application shell](../../frontend/src/App.tsx)
- [Login component](../../frontend/src/shared/layout/Login/Login.tsx)
- [HTTP client](../../frontend/src/api/filesApi.ts)

## Changelog

- 2026-09-17 - Added JWT session authentication, account management, public recent-file metadata with authors, and authentication-aware upload interactions.

## Notes

- [UserAuthenticationFlowIntegrationTest](../../backend/src/test/java/com/securefiles/integration/UserAuthenticationFlowIntegrationTest.java) is opt-in and requires a freshly initialized database. Its later local execution was blocked by a pre-existing `app_user` relation during Liquibase initialization.
- Branch commits: `26b4088 feat(backend): add authenticated file access` and `357d420 feat(frontend): add authenticated file console`.
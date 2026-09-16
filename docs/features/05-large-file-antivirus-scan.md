Feature name: large-file-antivirus-scan

## Summary

Aligns the production upload-size policy across Spring, the file domain, ClamAV, and the frontend while keeping antivirus failures fail closed. Adds an opt-in integration scenario that reproduces a ClamAV stream-size failure with generated small files and a test-only Compose override.

## Status

Stable

## Public API / Contracts

- `POST /api/v1/files` accepts a complete upload with `202 Accepted` and `PENDING_SCAN`.
- `GET /api/v1/files/config` exposes the runtime `maximumSizeBytes` used by the frontend.
- `GET /api/v1/files/{fileId}/content` remains available only for `CLEAN` files and returns `409 Conflict` for a terminal scan failure.
- `MAX_FILE_SIZE_BYTES` is the shared production file-size setting; `MAX_REQUEST_SIZE_BYTES` covers the multipart envelope.
- `CLAMAV_HOST_PORT` defaults to host port `3311` so the backend does not accidentally use a separate local daemon on `3310`.

## Quick usage

Start the nominal dependencies:

```bash
docker compose up -d --wait postgres minio rabbitmq clamav
```

Run the test-only low ClamAV limit scenario:

```bash
docker compose \
  -f docker-compose.yml \
  -f docker-compose.integration-scan-limit.yml \
  up -d --wait postgres minio rabbitmq clamav

cd backend
SECUREFILES_SCAN_LIMIT_INTEGRATION=true \
mvn -Dtest=FileScanSizeLimitIntegrationTest test
```

Restore the nominal ClamAV service after the isolated run:

```bash
docker compose -f docker-compose.yml up -d --force-recreate --wait clamav
```

## Design decisions

- Production limits stay byte-based and share `MAX_FILE_SIZE_BYTES`; the test override changes only ClamAV limits.
- The test keeps the backend upload limit above the generated file sizes so the request reaches the antivirus scanner instead of being rejected at the HTTP boundary.
- A generated `512 KiB` file verifies `CLEAN`; a generated `2 MiB` file exceeds the test-only `1 MiB` `StreamMaxLength` and verifies `SCAN_FAILED` plus blocked download.
- The large external Teams fixture is not part of the committed test path; the boundary test remains small and deterministic.

## Tests & validation

- `backend`: `mvn test` completed with 56 tests, 0 failures, 0 errors, and 6 skipped.
- `FileScanSizeLimitIntegrationTest`: 2 tests completed with 0 failures and 0 errors using the Compose override.
- `FileScanFlowIntegrationTest#upload_shouldReachClean_whenScanInfrastructureIsAvailable`: 1 test completed with 0 failures and 0 errors using nominal ClamAV configuration.
- `frontend`: the existing suite completed with 17 files and 56 tests, and `npm run build` completed successfully.
- `git diff --check` completed successfully.

## Related files

- [README](../../README.md)
- [Nominal Compose services](../../docker-compose.yml)
- [Test-only ClamAV override](../../docker-compose.integration-scan-limit.yml)
- [Backend upload and ClamAV configuration](../../backend/src/main/resources/application.yml)
- [Small-file scan-limit integration test](../../backend/src/test/java/com/securefiles/integration/FileScanSizeLimitIntegrationTest.java)
- [Existing scan-flow integration test](../../backend/src/test/java/com/securefiles/integration/FileScanFlowIntegrationTest.java)
- [File upload console](../../frontend/src/shared/forms/FileUpload/FileUpload.tsx)
- [File upload frontend tests](../../frontend/src/tests/shared/forms/FileUpload/FileUpload.test.tsx)

## Changelog

- 2026-09-16 - Aligned production size settings, isolated the low-limit ClamAV integration test, and displayed readable upload sizes.

## Notes

The test-only override must not be used for normal local development. The external `MicrosoftTeams.pkg` fixture remains untracked and is intentionally excluded from delivery.

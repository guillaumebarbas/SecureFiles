Feature name: backend-secure-file-flow

## Summary

Implements the backend lifecycle for securely accepting, scanning, listing, and distributing files. Content is streamed through private object storage and can be downloaded only after a clean antivirus result.

## Status

Stable

## Public API / Contracts

- [POST /api/v1/files](../../backend/src/main/java/com/securefiles/application/controller/UploadFileController.java) accepts a multipart `file` and returns accepted metadata in `PENDING_SCAN`.
- [GET /api/v1/files/config](../../backend/src/main/java/com/securefiles/application/controller/UploadConfigurationController.java) exposes the runtime upload-size limit.
- [GET /api/v1/files](../../backend/src/main/java/com/securefiles/application/controller/ListFilesController.java) and [GET /api/v1/files/{fileId}](../../backend/src/main/java/com/securefiles/application/controller/GetFileMetadataController.java) expose owner-scoped metadata.
- [GET /api/v1/files/{fileId}/content](../../backend/src/main/java/com/securefiles/application/controller/DownloadFileController.java) streams a download only when the domain accepts the file as `CLEAN`.

## Quick usage

Start PostgreSQL, MinIO, RabbitMQ, and ClamAV with the local Compose configuration, then run `mvn spring-boot:run` from `backend/`. A multipart upload is accepted for asynchronous scanning; clients poll its metadata until it reaches `CLEAN`, `INFECTED`, or `SCAN_FAILED`.

## Design decisions

- File bytes remain in MinIO quarantine storage while PostgreSQL holds metadata, scan attempts, and Outbox events.
- Upload and scan operations stream content, measure size and SHA-256, and compare canonical storage metadata before accepting or completing a scan.
- The scan workflow uses a lease and bounded retries. Missing, inconsistent, unavailable, and unknown scan outcomes fail closed with stable failure codes.
- The MinIO adapter obtains the canonical version from a post-write metadata read, avoiding multipart response version mismatches.

## Tests & validation

- [Domain tests](../../backend/src/test/java/com/securefiles/domain/file) cover lifecycle transitions, upload integrity, scan retries, metadata, and download authorization.
- [Mapper tests](../../backend/src/test/java/com/securefiles/application/mapper) cover HTTP-to-domain conversions; [authentication integration coverage](../../backend/src/test/java/com/securefiles/integration/UserAuthenticationFlowIntegrationTest.java) covers authenticated access and session revocation.
- `mvn test` completed successfully on 2026-09-16: 54 tests passed with 4 integration tests skipped by default.
- [FileScanFlowIntegrationTest](../../backend/src/test/java/com/securefiles/integration/FileScanFlowIntegrationTest.java) completed successfully on 2026-09-16 with local Docker dependencies and external EICAR fixtures: 4 tests passed, covering clean, multipart, EICAR text, and EICAR ZIP outcomes.

## Related files

- [StoredFile](../../backend/src/main/java/com/securefiles/domain/file/model/StoredFile.java)
- [UploadFileUseCase](../../backend/src/main/java/com/securefiles/domain/file/usecases/UploadFileUseCase.java)
- [ScanFileUseCase](../../backend/src/main/java/com/securefiles/domain/file/usecases/ScanFileUseCase.java)
- [MinioFileContentStorage](../../backend/src/main/java/com/securefiles/infrastructure/minio/MinioFileContentStorage.java)
- [RabbitMqScanListener](../../backend/src/main/java/com/securefiles/infrastructure/rabbitmq/RabbitMqScanListener.java)

## Changelog

- 2026-09-16 - Added secure file intake, antivirus scanning, HTTP contracts, and integration coverage.
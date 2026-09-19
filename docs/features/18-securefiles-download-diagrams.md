Feature name: securefiles-download-diagrams

## Summary

The documentation now includes native detailed and simplified draw.io sequence diagrams for authenticated secure-file downloads. The upload sequence was aligned with the current quota reservation, Outbox acceptance, RabbitMQ relay, and antivirus scan flow.

## Status

Stable

## Public API / Contracts

- `GET /api/v1/files/{id}/content` is authenticated and streams only `CLEAN` files.
- `POST /api/v1/files` returns `202/PENDING_SCAN` before the asynchronous scan flow.
- The README diagram table links each detailed and simplified source plus its PNG preview.

## Quick usage

- Open the native sources under `docs/diagrams/sequence/feature/file-download/` or `docs/diagrams/sequence/feature/file-upload/` in draw.io.
- Regenerate previews with `DRAWIO_BIN=/path/to/drawio make diagrams-export`.

## Design decisions

- Keep detailed and simplified diagrams in the same feature scope, with a dedicated `renders/` directory for PNG previews.
- Show download authentication, `CLEAN` gating, storage size/version verification, and streamed MinIO bytes without representing file content in PostgreSQL.
- Show upload acceptance through `FileAcceptancePort / JpaFileAcceptanceAdapter`, quota reservation, transactional metadata plus Outbox persistence, then `OutboxRabbitMqRelay` publication.

## Tests & validation

- `make diagrams-check` passed for all canonical draw.io sources and simplified pairs.
- `make diagrams-export` passed with draw.io desktop `31.4.5`; all four upload/download PNG previews were non-empty.
- `git diff --check` passed.

## Related files

- [README.md](../../README.md)
- [Download sequence](../diagrams/sequence/feature/file-download/download-file-sequence.drawio)
- [Simplified download sequence](../diagrams/sequence/feature/file-download/download-file-sequence-simplified.drawio)
- [Detailed upload sequence](../diagrams/sequence/feature/file-upload/upload-file-sequence.drawio)
- [Simplified upload sequence](../diagrams/sequence/feature/file-upload/upload-file-sequence-simplified.drawio)

## Changelog

- 2026-09-19 - Added secure download sequence diagrams and aligned upload sequence documentation.

## Notes

- The untracked `.github/modernize/` assessment artifact was intentionally excluded from the feature commits.
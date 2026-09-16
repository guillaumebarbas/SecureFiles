Feature name: securefiles-delivery-foundation

## Summary

Establishes the delivery guidance for SecureFiles and updates the repository guide to match the implemented secure file flow. It keeps agent instructions, testing discipline, local runtime instructions, and prompt history explicit and reviewable.

## Status

Stable

## Public API / Contracts

- [README](../../README.md) documents the file HTTP contract, local dependencies, upload-size policy, and opt-in antivirus integration scenarios.
- [Makefile](../../Makefile) exposes `make front` and `make back` as local development entry points.
- [AGENTS.md](../../AGENTS.md) and [backend/AGENTS.md](../../backend/AGENTS.md) define the architecture, security invariants, TDD workflow, and validation rules for future changes.
- [SecureFilesAgent](../../.github/agents/SecureFilesAgent.agent.md) and [SecureFilesPlanner](../../.github/agents/SecureFilesPlanner.agent.md) separate implementation from read-only planning.

## Quick usage

Use `make front` to start the Vite console and `make back` to start the Spring Boot service. Read the README before running the full local stack because the secure scan flow requires PostgreSQL, MinIO, RabbitMQ, and ClamAV.

## Design decisions

- Agent and skill guidance requires a narrow, test-first implementation loop and a focused validation after each change.
- The runtime guide documents fail-closed scanning, streamed content, private storage, and local-only development identity without publishing sensitive values.
- Feature summaries and verbatim prompt records provide a concise delivery trail without replacing commits, tests, or pull-request review.

## Tests & validation

- The documentation and workflow changes passed `git diff --check` before their commits.
- Backend validation executed on 2026-09-16: `mvn test` completed with 54 tests and no failures; the opt-in Docker integration suite completed with 4 tests and no failures.
- Frontend validation executed on 2026-09-16: `npm test` completed with 17 files and 49 tests passing, and `npm run build` completed successfully.

## Related files

- [securefiles-code-review skill](../../.github/skills/securefiles-code-review/SKILL.md)
- [securefiles-security skill](../../.github/skills/securefiles-security/SKILL.md)
- [TDD skill](../../.github/skills/tdd/SKILL.md)
- [clean code rules](../../rules/clean_code.md)
- [test strategy rules](../../rules/strategy_test.md)

## Changelog

- 2026-09-16 - Added delivery workflow guidance and refreshed the runtime documentation for secure file processing.
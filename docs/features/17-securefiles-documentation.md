Feature name: securefiles-documentation

## Summary

This delivery reorganizes the native draw.io documentation workflow and simplifies the main README. Diagram sources now use category and scope directories with paired detailed and simplified views, while the README presents the console, public contracts, local startup, configuration, validation commands, and known limits in a shorter guide.

## Status

Stable

## Public API / Contracts

- Canonical diagram sources use `docs/diagrams/<category>/<scope-kind>/<scope-name>/`.
- Each canonical `.drawio` source has a `-simplified.drawio` companion and a scope-local `renders/` directory.
- `make diagrams-check` validates the draw.io XML sources, their hierarchy, and their simplified companions.
- The README documents the HTTP routes, secure file lifecycle, local service ports, startup commands, integration switches, and known limitations.

## Quick usage

```bash
make diagrams-check
DRAWIO_BIN=/path/to/drawio make diagrams-export
```

The console and local API are started with `make front` and `make back` after `make init`.

## Design decisions

- Native draw.io XML remains the editable source of truth; PNG files are the README previews.
- Diagram scopes distinguish features, transversal logic, and system views without sharing a `renders/` directory between scopes.
- The main README keeps public contracts and operator commands while moving agent and MCP implementation details back to their dedicated files.
- Existing diagram assets are relocated rather than duplicated, and the three console screenshots are stored under `docs/screenshots/`.

## Tests & validation

- `make diagrams-check` passed and validated 12 canonical draw.io files and their simplified companions.
- `git diff --cached --check` passed before the feature commit.
- No backend or frontend application tests were run because this delivery contains documentation, diagram assets, and validation workflow changes only.

## Related files

- [Main README](../../README.md)
- [Diagram skill](../../.github/skills/securefiles-diagrams/SKILL.md)
- [Diagram prompt](../../.github/prompts/create-securefiles-diagrams.prompt.md)
- [Diagram documentation](../diagrams/README.md)
- [Diagram validation script](../../scripts/check-diagrams.sh)

## Changelog

- 2026-09-19 - Reorganized native diagram documentation, added console screenshots, and simplified the project README.

## Notes

- Commit: `e217360` (`docs(diagrams): reorganize workflow and simplify project README`).
- `.github/modernize/` remains an unrelated untracked artifact and is excluded from this feature.

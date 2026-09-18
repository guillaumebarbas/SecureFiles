Feature name: native-diagram-skill-workflow

## Summary

Le workflow de diagrammes SecureFiles produit des sources XML draw.io natives, une vue detaillee et une vue `-simplified` par categorie, ainsi que des apercus PNG regenerables pour le README. Les anciennes sources Mermaid et Excalidraw restent archivees sous `_legacy/`.

## Status

Stable

## Public API / Contracts

- Les diagrammes sont ranges sous `docs/diagrams/<category>/`.
- Chaque sujet utilise les fichiers `<slug>.drawio` et `<slug>-simplified.drawio`.
- `make diagrams-check` valide les sources XML et les paires simplified.
- `make diagrams-export` genere les apercus PNG avec `DRAWIO_BIN`.

## Quick usage

```bash
make diagrams-check
DRAWIO_BIN=/path/to/drawio make diagrams-export
```

## Design decisions

- draw.io XML est la source editable canonique ; une URL de rendu ou une source Mermaid seule ne constitue pas un livrable.
- Les categories separent les questions de use case, sequence, architecture, component, data flow et deployment.
- Le PNG est l'apercu de reference pour la documentation ; le JPG reste optionnel.

## Tests & validation

- `make diagrams-check` a valide 4 fichiers draw.io canoniques, leurs vues simplified et les apercus PNG.
- `make diagrams-export` a regenere les 4 apercus PNG avec `/opt/homebrew/bin/drawio` sans erreur.
- `git diff --cached --check` est passe avant le commit.

## Related files

- [Diagram skill](../../.github/skills/securefiles-diagrams/SKILL.md)
- [Diagram prompt](../../.github/prompts/create-securefiles-diagrams.prompt.md)
- [Diagram documentation](../diagrams/README.md)
- [Diagram validation script](../../scripts/check-diagrams.sh)
- [Diagram export script](../../scripts/export-diagrams.sh)

## Changelog

- 2026-09-18 - Added the native draw.io diagram workflow with simplified views and PNG previews.

## Notes

- Commit: `1625c43` (`feat(workflow): add native diagram skill workflow`).
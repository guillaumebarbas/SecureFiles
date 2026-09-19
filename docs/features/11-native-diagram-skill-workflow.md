Feature name: native-diagram-skill-workflow

## Summary

Le workflow de diagrammes SecureFiles produit des sources XML draw.io natives, une vue detaillee et une vue `-simplified` par scope, ainsi que des apercus PNG regenerables pour le README. Les sources sont separees par categorie puis par scope `feature`, `logic` ou `system`. Les anciennes sources Mermaid et Excalidraw restent archivees sous `_legacy/`.

## Status

Stable

## Public API / Contracts

- Les diagrammes sont ranges sous `docs/diagrams/<category>/<scope-kind>/<scope-name>/`.
- Chaque scope utilise les fichiers `<slug>.drawio`, `<slug>-simplified.drawio` et son propre dossier `renders/`.
- `make diagrams-check` valide les sources XML, la hierarchie et les paires simplified.
- `make diagrams-export` genere les apercus PNG avec `DRAWIO_BIN`.

## Quick usage

```bash
make diagrams-check
DRAWIO_BIN=/path/to/drawio make diagrams-export
```

## Design decisions

- draw.io XML est la source editable canonique ; une URL de rendu ou une source Mermaid seule ne constitue pas un livrable.
- Les categories separent les questions de use case, sequence, architecture, component, data flow et deployment ; les scopes separent les features, la logique technique et les vues systeme.
- Le PNG est l'apercu de reference pour la documentation ; le JPG reste optionnel.

## Tests & validation

- `make diagrams-check` a valide 8 fichiers draw.io canoniques (4 vues detaillees et 4 vues simplified), ainsi que leurs apercus PNG.
- `make diagrams-export` a regenere les 8 apercus PNG avec `/opt/homebrew/bin/drawio` sans erreur.
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
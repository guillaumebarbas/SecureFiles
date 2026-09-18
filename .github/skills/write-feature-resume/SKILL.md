---
name: write-feature-resume
description: "Use when documenting a completed or in-progress feature from branch commits, staged changes, unstaged changes, and untracked local files."
---

# Write Feature Resume

Ce skill produit une documentation courte et factuelle d'une feature a partir de l'etat Git de la branche. Il ne doit pas deduire une fonctionnalite uniquement depuis un nom de branche et ne doit jamais remplacer une verification du diff par une supposition.

## Collecte obligatoire

Lire les sources dans cet ordre, en lecture seule :

1. `git status --short --branch` pour connaitre la branche, les changements staged/unstaged et les fichiers non suivis.
2. `git diff --cached` pour les changements staged.
3. `git diff` pour les changements unstaged.
4. `git ls-files --others --exclude-standard` puis lire les fichiers non suivis qui appartiennent a la feature.
5. `git log --oneline --decorate --no-merges -30` pour les commits recents de la branche.
6. Si un upstream ou une base de comparaison existe, comparer la branche a cette base avec `git merge-base` et `git diff`; sinon documenter uniquement les commits et fichiers effectivement observes.

Ne jamais utiliser de commande destructive (`reset`, `checkout`, `clean`) et ne jamais modifier le code pour produire le resume. Les fichiers non suivis sont une source de verite au meme titre que les commits : le scaffold initial peut ne contenir aucun commit.

## Analyse de perimetre

- Distinguer les changements de la feature des changements preexistants ou sans rapport ; ne pas les presenter comme une seule livraison.
- Lire les diffs et les fichiers modifies pour identifier les contrats publics, decisions d'architecture, migrations, dependances et comportements observes.
- Rechercher les tests et les commandes de validation dans les commits et les fichiers ; ne jamais affirmer qu'une commande a ete executee si aucune preuve n'est disponible.
- Marquer `Design`, `Implementing`, `Stable` ou `Deprecated` uniquement selon l'etat observable ; demander une precision si le statut ne peut pas etre deduit.
- Ne pas copier de secret, token, contenu de fichier ou donnee sensible dans la documentation. Rediger un resume de la configuration plutot que sa valeur.

## Sortie

- Ecrire par defaut dans `docs/features/<NN>-<short-name>.md`.
- Determiner `NN` avant la redaction en listant les resumes existants qui correspondent a `^[0-9]{2}-.+\.md$`, puis utiliser le nombre suivant le plus eleve. Le numero `00` est reserve a la fondation initiale lorsqu'elle doit etre regularisee ; les numeros publies restent immuables.
- Ne jamais creer de resume de feature non numerote. `docs/features/README.md` est le seul fichier autorise sans prefixe numerique car il sert d'index.
- Avant le commit, verifier que chaque fichier de feature respecte le format `^[0-9]{2}-.+\.md$`, qu'aucun numero n'est duplique et que l'index reference le nouveau nom.
- Si une autre branche a reserve le meme numero entre l'analyse et le commit, conserver les numeros deja publies et choisir le prochain numero libre avant de poursuivre.
- Creer le dossier de sortie uniquement si la documentation doit effectivement etre ecrite.
- Utiliser des liens relatifs vers les fichiers et les contrats du depot.
- Utiliser la date courante au format `YYYY-MM-DD` pour le changelog.
- Ne pas inventer de resultat, de test, d'endpoint, de decision ou de fichier. Employer `Non determine` lorsqu'une information ne peut pas etre etablie a partir du depot.

# Feature documentation template

```markdown
Feature name: <short-name>

## Summary

1–3 sentence description of the feature and why it exists.

## Status

Design / Implementing / Stable / Deprecated

## Public API / Contracts

- Endpoints, messages, DTOs — link to code where possible.

## Quick usage

- Minimal example(s) showing how to use the feature.

## Design decisions

- Decision 1: short explanation and rationale
- Decision 2: short explanation and rationale

## Tests & validation

- Where tests live and key scenarios they cover.

## Related files

- src/… (links to main files)

## Changelog

- YYYY-MM-DD — summary of change

## Notes

- Any other short pointers or references
```

Le document final doit rester concis : les details complets appartiennent aux commits, au code et aux tests ; le resume doit permettre de comprendre l'intention, le contrat et la validation de la feature.
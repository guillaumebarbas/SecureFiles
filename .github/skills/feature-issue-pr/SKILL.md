---
name: feature-issue-pr
description: "Use when delivering a branch by analyzing every commit and local change, creating logical Conventional Commits, creating a GitHub issue, and opening a pull request toward main with a feature resume."
---

# Feature Issue Pull Request

Ce skill orchestre la livraison d'une feature depuis une branche vers `main`. Il collecte tous les commits de la branche, examine les changements locaux, cree un ou plusieurs commits logiques au format Conventional Commits, produit le resume de feature, puis cree l'issue avant la pull request.

## Required subskill

`#file:write-feature-resume`

Charger et appliquer [write-feature-resume](../write-feature-resume/SKILL.md) avant de rediger l'issue ou la pull request. Lui transmettre le nom court, la branche source, `main` comme base, le merge-base observe, la liste complete des commits et le snapshot des changements locaux.

## Preconditions

- Lire `AGENTS.md` et le contrat pertinent de `README.md` avant toute action.
- Lire la branche courante avec `git branch --show-current` ; refuser de creer une pull request si la branche courante est `main` ou vide.
- Verifier que `origin` existe et que la branche distante `main` est accessible. Utiliser `git fetch origin main` pour actualiser la reference distante sans modifier les fichiers de travail.
- Determiner `BASE=$(git merge-base origin/main HEAD)` ; ne jamais limiter la collecte a un nombre arbitraire de commits.
- Verifier l'authentification GitHub via le serveur MCP GitHub configure ou `gh` si disponible. Ne jamais demander, afficher ou enregistrer un token.
- Ne jamais utiliser `git reset`, `git checkout`, `git clean` ou une autre commande destructive.

## Collecte complete de la branche

Executer en lecture seule, dans cet ordre :

1. `git status --short --branch` pour distinguer les changements staged, unstaged et non suivis.
2. `git log --reverse --format=fuller "$BASE"..HEAD` pour prendre en compte tous les commits de la branche, y compris les merges.
3. `git diff --stat "$BASE"..HEAD` puis `git diff "$BASE"..HEAD` pour comprendre le contenu effectivement versionne.
4. `git diff --cached` et `git diff` pour analyser les changements locaux non inclus dans les commits.
5. `git ls-files --others --exclude-standard` puis lire uniquement les fichiers non suivis pertinents.

Comparer le contenu de la branche avec `origin/main`, pas seulement avec le dernier commit. Ne pas presenter les changements locaux comme faisant partie de la pull request tant qu'ils ne sont pas commites.

## Local changes policy

- Par defaut, ne pas commiter automatiquement les changements locaux. Lorsque l'utilisateur demande explicitement le workflow complet `feature-issue-pr`, les changements de la feature peuvent etre regroupes et commites selon la section [Preparation des commits](#preparation-des-commits).
- Ne jamais inclure dans ces commits les changements sans rapport, les fichiers sensibles, les secrets, les tokens ou le contenu de fichiers utilisateur. Si les changements de la feature ne peuvent pas etre separes proprement, arreter le workflow et afficher les chemins concernes.
- Les changements locaux peuvent etre transmis a `#file:write-feature-resume` pour documenter l'etat en cours, mais ils ne doivent pas etre annonces comme livres dans la PR.
- Apres l'appel a `write-feature-resume`, le fichier de documentation genere est un changement attendu. Le commiter uniquement avec un message `docs: add feature resume for <short-name>` si le workflow de livraison a ete explicitement demande et si aucun autre changement local n'est present.
- Avant de creer l'issue, verifier que le diff de la branche est deterministe et qu'aucun secret, token, contenu de fichier ou donnee sensible n'apparait dans les textes produits.

## Preparation des commits

Cette etape s'applique uniquement lorsque le workflow complet `feature-issue-pr` a ete explicitement demande.

1. Classer les changements de la feature par unite logique : implementation, tests, documentation, configuration ou outillage.
2. Creer un ou plusieurs commits coherents. Ne pas produire un commit geant melangeant des changements independants ; ne pas separer artificiellement des fichiers qui doivent rester atomiques pour etre compris ou testes ensemble.
3. Utiliser le format Conventional Commits :

	```text
	<type>(<scope>): <description imperative courte>
	```

	Types usuels : `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`, `chore` et `perf`. La description commence en minuscule, reste concise, decrit l'intention du commit et ne se termine pas par un point.
4. Utiliser un scope precis lorsqu'il apporte de l'information, par exemple `frontend`, `backend`, `security`, `tests` ou `workflow`.
5. Exemples acceptes :

	- `feat(frontend): add file scan status view`
	- `test(frontend): cover icon-only tooltip actions`
	- `fix(backend): reject downloads before antivirus scan`
	- `docs(workflow): add feature resume for secure-download`

Avant chaque commit :

- selectionner explicitement les fichiers de la tranche concernee ; ne pas utiliser `git add .` ou `git add -A` sans verification du perimetre ;
- inspecter `git diff --cached` et executer `git diff --cached --check` ;
- verifier qu'aucun secret, token, contenu de fichier ou changement sans rapport n'est staged ;
- executer les validations pertinentes avant le commit et ne jamais declarer un test reussi sans sortie observable.

Apres chaque commit :

- verifier le message avec `git log -1 --format=%s` et l'etat avec `git status --short --branch` ;
- conserver l'ordre logique des commits et les inclure tous dans le resume, l'issue et la pull request ;
- ne jamais utiliser `--amend`, rebase destructif ou force push sauf demande explicite de l'utilisateur.

Si un hook de commit echoue, corriger la cause dans le perimetre de la feature et recommencer la validation ; ne pas contourner le hook avec `--no-verify`.

## Feature resume

Appeler explicitement :

```text
#file:write-feature-resume
```

Le resume doit prendre en compte la plage complete `BASE..HEAD`, les fichiers modifies et les validations observees. Ecrire par defaut dans `docs/features/<short-name>.md`, puis verifier les liens et le contenu genere. Ne jamais inventer un test, une API, une decision ou un resultat.

## GitHub issue

- Deriver un titre court et stable a partir de la feature documentee, par exemple `Feature: <short-name>`.
- Creer l'issue avant la pull request avec le serveur MCP GitHub configure ; utiliser `gh issue create` uniquement comme solution de repli si `gh` est disponible.
- Le corps de l'issue doit contenir : resume, motivation, perimetre, criteres d'acceptation, validations observees, liste de commits et lien vers le resume de feature.
- Ne pas fermer automatiquement une issue preexistante sans preuve qu'elle correspond a la meme feature.
- Conserver le numero et l'URL de l'issue pour les reutiliser dans la pull request.

## Pull request vers main

- Verifier que tous les commits a livrer sont sur la branche source et que le working tree est propre.
- Pousser la branche source avec `git push -u origin HEAD` si elle n'est pas encore disponible sur le remote.
- Creer la pull request avec `main` comme branche de base et la branche courante comme branche source, via le MCP GitHub ou `gh pr create` en repli.
- Utiliser un titre coherent avec l'issue et un corps contenant `Closes #<issue-number>`, le resume, les decisions, les validations et les fichiers principaux.
- Ne jamais creer une PR vers une autre base par defaut, ne jamais utiliser `main` comme branche source et ne jamais masquer des changements locaux non commites.
- Apres creation, verifier le numero, l'URL, la branche de base `main`, la branche source et l'etat final du working tree.

## Stop conditions

Arreter le workflow sans creer d'issue ni de pull request si :

- la branche courante est `main` ;
- `origin/main` ou le merge-base ne peut pas etre determine ;
- des changements de code restent non commites ;
- l'authentification GitHub ou l'acces au depot echoue ;
- les tests ou validations obligatoires echouent ;
- le perimetre de la feature ne peut pas etre distingue de changements sans rapport.
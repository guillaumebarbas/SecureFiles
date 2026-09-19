# Skills SecureFiles

Ces skills sont des guides locaux, adaptes au domaine de SecureFiles. Ils reprennent les idees utiles des categories `code-review`, `tdd` et `codebase-design` du depot [mattpocock/skills](https://github.com/mattpocock/skills), puis ajoutent les contraintes propres au scan antivirus, au streaming et a la console de fichiers.

## Selection

- `securefiles-code-review` : analyser un changement, une regression ou une demande de revue.
- `securefiles-testing` : choisir la strategie de test par couche et proteger les invariants.
- `tdd` : imposer la boucle red-green-refactor pour toute implementation frontend ou backend.
- `securefiles-security` : verifier upload, quarantaine, ClamAV, download, flux et logs.
- `securefiles-frontend` : modifier la console React en respectant l'UX/UI de reference et le contrat API.
- `securefiles-diagrams` : analyser le code SecureFiles et produire des paires natives `.drawio` detaillees/simplifiees d'architecture, de sequence, de cas d'utilisation ou de flux, avec apercus PNG optionnels.
- `write-feature-resume` : documenter une feature a partir des commits, changements locaux et fichiers non suivis de la branche.
- `feature-issue-pr` : analyser tous les commits d'une branche, appeler `write-feature-resume`, puis creer une issue et une pull request vers `main`.

Le prompt `/create-securefiles-diagrams` declenche ce workflow avec un sujet, un type de diagramme et une categorie/slug optionnels. Chaque demande doit produire un fichier `.drawio` detaille et son compagnon `-simplified.drawio`; Mermaid et Excalidraw restent des formats de brouillon ou d'archive.

Les diagrammes sont ranges sous `docs/diagrams/<category>/<scope-kind>/<scope-name>/` (`use_case`, `sequence`, `architecture`, `component`, `data_flow` ou `deployment`, puis `feature`, `logic` ou `system`). Chaque scope possede un dossier `renders/` pour ses apercus. `make diagrams-check` valide les sources natives, leur hierarchie et leurs paires. `make diagrams-export` produit les apercus PNG avec un binaire draw.io configure par `DRAWIO_BIN`; `DIAGRAM_EXPORT_JPG=1` ajoute les JPG.

Chaque skill renvoie vers les regles partagees dans `rules/`. Charger uniquement les skills utiles a la tache, et charger `securefiles-security` pour tout changement qui touche le contenu ou le statut d'un fichier.

Les resumes de features sont stockes dans `docs/features/<NN>-<short-name>.md` avec une numerotation append-only a deux chiffres. `docs/features/README.md` est le seul fichier sans numero ; un nouveau resume prend le numero suivant le plus eleve sans renumeroter les livraisons existantes.

Les issues et pull requests SecureFiles sont toujours publiees en anglais. Les identifiants techniques, statuts, commandes, noms de branches, messages de commit et libelles produit cites restent inchanges.
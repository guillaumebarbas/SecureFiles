# Diagrammes SecureFiles

Les diagrammes editables sont des fichiers XML natifs draw.io. Une demande de diagramme produit toujours une paire dans la meme categorie :

```text
docs/diagrams/<category>/<slug>.drawio
docs/diagrams/<category>/<slug>-simplified.drawio
docs/diagrams/<category>/renders/<slug>.png
docs/diagrams/<category>/renders/<slug>-simplified.png
```

Categories :

- `use_case` : acteurs, systeme et objectifs metier ;
- `sequence` : ordre temporel des appels et retours ;
- `architecture` : couches, frontieres et dependances ;
- `component` : relations entre composants ;
- `data_flow` : circulation des metadonnees et des octets ;
- `deployment` : services, reseau et dependances d'execution.

Mermaid et Excalidraw peuvent etre conserves dans `_legacy/` comme sources historiques ou brouillons. Ils ne remplacent jamais les fichiers `.drawio` canoniques.

## Validation

```bash
make diagrams-check
```

Cette commande valide chaque fichier `.drawio` canonique comme XML et verifie la presence de sa vue `-simplified`.

## Export

L'export PNG utilise le binaire draw.io desktop. Le binaire peut etre installe localement ou passe explicitement :

```bash
DRAWIO_BIN=/Applications/draw.io.app/Contents/MacOS/draw.io make diagrams-export
```

Le PNG est le format de reference pour le README. Le JPG est optionnel :

```bash
DIAGRAM_EXPORT_JPG=1 DRAWIO_BIN=/Applications/draw.io.app/Contents/MacOS/draw.io make diagrams-export
```

Si aucun binaire draw.io n'est disponible, `make diagrams-export` echoue volontairement et ne cree aucun apercu incomplet.

---
name: create-securefiles-diagrams
description: "Analyze SecureFiles and create paired native draw.io diagrams, including a simplified view and an optional PNG preview."
argument-hint: "Describe what to diagram, choose a type, and optionally provide a category and slug"
agent: "SecureFilesAgent"
tools: [read, search, edit, execute, drawio/*]
---

Charge [le skill SecureFiles Diagrams](../skills/securefiles-diagrams/SKILL.md) et applique-le integralement.

Sujet a diagrammer : ${input:diagram_request:Decris le flux, le use case ou le sous-systeme SecureFiles a representer}
Type de diagramme : ${input:diagram_type:architecture, sequence, use-case, component, data-flow ou deployment}
Categorie et slug optionnels : ${input:diagram_output:Par defaut, utiliser docs/diagrams/<category>/<slug>.drawio}

Instructions :

1. Lis `AGENTS.md`, `README.md`, les instructions applicables, les documents de feature concernes et les tests voisins.
2. Recherche le code qui controle directement le comportement demande. Ne deduis aucun noeud, appel, statut ou acteur sans source observable.
3. Applique le skill [SecureFiles Diagrams](../skills/securefiles-diagrams/SKILL.md), notamment les invariants antivirus, de streaming, de stockage et d'authentification.
4. Produis toujours deux sources XML natives dans la meme categorie: `docs/diagrams/<category>/<slug>.drawio` et `docs/diagrams/<category>/<slug>-simplified.drawio`. Si un fichier `.drawio` ne peut pas etre cree ou valide, interromps le workflow avec le statut `ABORTED`; un `.mmd`, une URL ou une capture ne le remplace pas.
5. Pour les formes UML ou cloud, utilise `#tool:drawio/search_shapes`, puis `#tool:drawio/open_drawio_xml`. Utilise `#tool:drawio/open_drawio_mermaid` uniquement comme aide de brouillon, jamais comme livrable final.
6. Conserve les sources natives dans les dossiers `use_case`, `sequence`, `architecture`, `component`, `data_flow` ou `deployment`. Les anciennes sources Mermaid/Excalidraw vont dans `_legacy/` lorsqu'elles sont conservees.
7. Verifie les textes, les connecteurs, l'ordre des appels et la coherence avec le code. Execute `make diagrams-check` puis `git diff --check` si une source est creee ou modifiee.
8. Execute `make diagrams-export` si un binaire draw.io est disponible via `DRAWIO_BIN`; le PNG est l'apercu README canonique et le JPG est optionnel avec `DIAGRAM_EXPORT_JPG=1`. Si aucun exporteur n'est disponible, signale clairement le blocage sans ajouter de lien d'image casse.
9. Reponds avec exactement les sections `ANALYSE`, `DIAGRAMMES`, `VALIDATION` et `RISQUES RESTANTS`, puis ajoute la demande et le resultat dans `resume.md` selon les conventions du depot.
10. Si le code est ambigu, conserve ce qui est verifiable et signale le blocage sans inventer de rendu ou de comportement.

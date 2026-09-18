---
name: securefiles-diagrams
description: "Use when creating or updating SecureFiles native draw.io architecture, component, data-flow, sequence, use-case, or deployment diagrams, including simplified views and PNG previews."
argument-hint: "Describe the SecureFiles flow, diagram type, and optional output path"
---

# SecureFiles Diagrams

## Mission

Ce skill transforme une demande de documentation technique en paire de diagrammes natifs, verifiables et editables. Il s'appuie sur le code et les contrats observes de SecureFiles, utilise le serveur MCP draw.io configure dans `.vscode/mcp.json`, et conserve les sources `.drawio` ainsi que leurs apercus PNG dans le depot.

## Utiliser ce skill lorsque

- il faut expliquer l'architecture backend/frontend et les adaptateurs sortants ;
- il faut tracer un use case d'upload, de scan, d'authentification, de retry ou de download ;
- il faut produire un diagramme de sequence, de cas d'utilisation, de composants, de flux de donnees ou de deploiement ;
- un diagramme existant doit etre mis a jour apres une evolution du code.

## Preconditions

1. Lire `AGENTS.md`, le `README.md` pertinent, les instructions applicables et les documents de feature concernes.
2. Verifier que le serveur MCP local `drawio` est disponible. Utiliser le transport `stdio` `npx -y @drawio/mcp` configure dans `.vscode/mcp.json`.
3. Ne pas utiliser l'endpoint heberge `https://mcp.draw.io/mcp` pour des informations internes a SecureFiles.
4. Toute demande de diagramme doit produire deux fichiers XML natifs : une vue detaillee et une vue `<slug>-simplified.drawio`. Un fichier Mermaid ou Excalidraw seul ne constitue jamais un resultat valide.
5. Si les fichiers `.drawio` ne peuvent pas etre crees et valides comme XML, interrompre le workflow et signaler `ABORTED`. Ne pas remplacer le livrable par une URL, un fichier `.mmd` ou une capture navigateur.
6. Ne pas installer de dependance, modifier la configuration MCP ou creer un nouveau serveur sans demande explicite.

## Source de verite

- Rechercher le port entrant, le use case ou l'adaptateur qui decide directement le comportement. Ne pas deduire un flux depuis un simple nom de classe ou un commentaire.
- Pour un use case, suivre le chemin public depuis le controller ou le port entrant jusqu'aux ports sortants et aux transitions du domaine.
- Lire les tests voisins et les contrats REST lorsque le diagramme decrit un comportement observable.
- Ajouter dans la reponse la liste courte des fichiers et symboles qui justifient le diagramme. Ne pas copier de contenu de fichier dans le diagramme.
- Marquer explicitement `Non determine` toute partie que le code ne permet pas de confirmer.

## Taxonomie et nommage

Les diagrammes sont ranges par question principale sous `docs/diagrams/<category>/` :

- `use_case/` : acteurs, systeme et objectifs metier ;
- `sequence/` : ordre temporel des appels et retours ;
- `architecture/` : couches, composants et frontieres ;
- `component/` : dependances entre composants ;
- `data_flow/` : circulation des metadonnees et des octets ;
- `deployment/` : services, reseau et dependances d'execution.

Le nom canonique est `docs/diagrams/<category>/<slug>.drawio`, accompagne de
`docs/diagrams/<category>/<slug>-simplified.drawio`. Les fichiers rendus sont places dans
`docs/diagrams/<category>/renders/` avec les memes slugs et l'extension `.png`.

## Choix du format et de l'outil

| Besoin | Format recommande | Outil draw.io |
| --- | --- | --- |
| Architecture, composants ou flux de donnees | XML draw.io natif | `open_drawio_xml` |
| Diagramme de sequence | XML draw.io natif | `open_drawio_xml` |
| Diagramme de cas d'utilisation UML | XML draw.io avec acteurs, systeme et ellipses | `search_shapes`, puis `open_drawio_xml` |
| Architecture cloud ou infrastructure | XML draw.io avec bibliotheques AWS/Azure/GCP/Kubernetes | `search_shapes`, puis `open_drawio_xml` |
| Diagramme existant a inspecter ou modifier | Fichier `.drawio` local | `list_pages`, `get_page`, `set_page` lorsque disponibles |

Utiliser `search_shapes` uniquement lorsqu'une forme metier, cloud ou UML apporte une information utile. Les rectangles, ellipses et connecteurs simples sont preferables aux icones decoratives.

Mermaid et Excalidraw peuvent servir de brouillon de travail ou rester dans `docs/diagrams/_legacy/`, mais ne sont pas des sources de livraison.

## Procedure

### 1. Cadrer la demande

Identifier le sujet, les acteurs, la frontiere du systeme, le type de diagramme, le niveau de detail et le chemin de sortie. Si plusieurs flux sont demandes, les separer en diagrammes courts plutot que de construire une planche illisible.

### 2. Collecter les preuves

- Rechercher les endpoints, ports, use cases, statuts, messages et adaptateurs directement concernes.
- Pour un diagramme de sequence, conserver l'ordre chronologique des appels, les retours et les branches d'erreur.
- Pour une architecture, distinguer `domain/`, `application/` et `infrastructure/` et ne pas dessiner une dependance interdite comme si elle etait valide.
- Pour un cas d'utilisation, distinguer les acteurs humains, les systemes externes et les limites de SecureFiles.

### 3. Construire les deux vues

- Donner un titre, une legende si les couleurs ont une signification, et des identifiants stables aux noeuds importants.
- Utiliser des libelles courts et exacts ; conserver les statuts API en majuscules (`UPLOADING`, `PENDING_SCAN`, `SCANNING`, `CLEAN`, `INFECTED`, `SCAN_FAILED`, `REJECTED`).
- Montrer les chemins nominaux et les erreurs qui changent la disponibilite du fichier.
- Pour une sequence, afficher les requetes, les reponses et les transitions dans l'ordre. Pour une architecture, orienter les flux de gauche a droite et regrouper les couches sans impliquer une dependance technique interdite.
- Ne pas inventer de base de donnees, de file, d'acteur, d'endpoint, de statut ou de retry absent des sources.
- Creer d'abord la vue detaillee, puis une vue `-simplified` qui reduit les participants et les libelles sans supprimer la question principale, les statuts critiques, les frontieres de securite ou la separation des octets et des metadonnees.
- Garder les deux vues dans la meme categorie et utiliser les memes identifiants de provenance lorsque les noeuds representent le meme comportement.

### 3 bis. Verifier la logique avant le rendu

Avant de placer les formes, ecrire le petit graphe du diagramme : point de depart, resultat attendu, noeuds traverses, transitions d'etat, erreurs et retours. Chaque fleche doit ensuite avoir une source, une cible, une direction et une semantique explicite (`appel`, `retour`, `evenement`, `flux d'octets`, `transition d'etat` ou `retry`). Une fleche qui ne peut pas etre justifiee par le code est retiree ou marquee `Non determine`.

- Un diagramme repond a une question principale. Ne pas melanger une sequence temporelle, une architecture de composants et une machine d'etats sur le meme canevas sans sections clairement independantes.
- Lorsque l'upload synchrone et le scan asynchrone sont tous les deux demandes, preferer trois vues courtes : acceptance de l'upload, traitement du scan/retry et architecture des dependances. Un lien entre deux vues doit etre une note, jamais une longue fleche qui traverse une autre section.
- Pour une sequence, utiliser une lecture verticale (temps de haut en bas) et des participants stables. Pour une architecture ou un flux de donnees, utiliser une lecture horizontale de gauche a droite. Ne pas changer de sens de lecture au milieu d'une vue.
- Separer visuellement `application`, `domain` et `infrastructure`. Les ports et adaptateurs doivent etre visibles lorsque leur omission ferait croire que le domaine depend directement de JPA, PostgreSQL, RabbitMQ, MinIO ou ClamAV.
- Representer la transaction d'acceptation comme un bloc coherent : le port d'acceptation et `JpaFileAcceptanceAdapter` enregistrent les metadonnees et `FILE_SCAN_REQUESTED` dans PostgreSQL avant la reponse `202 Accepted`. Le use case d'upload ne publie pas directement dans RabbitMQ.
- Representer l'Outbox comme un flux distinct : `OutboxRabbitMqRelay` lit les evenements non publies, publie le message dans l'echange, puis marque l'evenement publie apres confirmation du broker. Ne pas dessiner une confirmation avant la publication.
- Representer le scan dans son ordre reel : `RabbitMqScanListener` appelle `ScanFileUseCase`, qui reserve le fichier, relit le flux prive MinIO, envoie les octets a ClamAV, puis persiste le verdict. L'ACK ne vient qu'apres le traitement persistant.
- Pour le retry RabbitMQ, distinguer la queue de scan, la queue de retry avec TTL et le retour vers la queue de scan. Appeler une queue `dead-letter` uniquement si une liaison effective du code la recoit; ne pas confondre automatiquement dead-letter et retry.
- La base PostgreSQL ne porte jamais de fleche d'octets. Les fleches vers elle doivent etre etiquetees `metadata`, `status`, `ScanAttempt` ou `Outbox`; les fleches d'octets restent entre le use case, MinIO et ClamAV.
- Pour le scan, ne dessiner `CLEAN` qu'apres une reponse antivirus explicitement saine. Une detection, une erreur de protocole, une incoherence de stockage ou une indisponibilite doit conduire a un statut bloquant ou a un retry confirme par le code.

### 3 ter. Regles de composition et de lisibilite

- Limiter une bande de lecture a environ six ou huit noeuds principaux. Si le canevas ne reste lisible qu'a moins de 75 pour cent de zoom, decouper le diagramme.
- Garder les connexions entre noeuds adjacents. Les connecteurs diagonaux qui remontent dans une autre section, passent sous plusieurs composants ou croisent une autre fleche sont interdits; utiliser des segments orthogonaux ou deplacer le noeud cible.
- Ne jamais faire passer une fleche dans un conteneur, derriere une forme ou sur une etiquette. Les retours et les erreurs doivent rester proches du chemin qu'ils corrigent.
- Placer la reponse `202 Accepted` a cote du controleur ou de l'acteur appelant, et non a l'autre extremite d'une fleche qui traverse les composants internes.
- Reutiliser une couleur de couche de maniere constante, mais ne jamais faire reposer une distinction critique sur la couleur seule. La legende doit aussi expliquer les types de traits et les libelles.
- Garder les titres et les statuts tres visibles. Les noms de classes, queues et messages doivent rester exacts; les explications longues vont dans une note ou dans la documentation voisine, pas dans chaque rectangle.
- Verifier le rendu a la taille de lecture finale, idealement a 100 pour cent ou dans une capture qui conserve une taille de texte lisible. Un diagramme techniquement exact mais lisible seulement a 51 pour cent est considere comme non valide.
- Donner un identifiant stable aux noeuds et aux fleches importants afin de pouvoir verifier les liens, comparer deux versions et corriger un rendu sans reconstruire le graphe au hasard.

### 4. Conserver les sources natives et les apercus

Lorsque l'utilisateur demande un fichier, produire `docs/diagrams/<category>/<slug>.drawio` et sa variante `-simplified.drawio`. Ne pas presenter une URL de rendu comme unique source de verite. Ajouter une courte note de provenance si le diagramme depend d'un contrat ou d'une feature precise.

Le PNG est l'apercu canonique pour le README. Utiliser un exporteur draw.io natif configurable par `DRAWIO_BIN`, ou un autre moteur local explicitement verifie. Le JPG est facultatif et ne doit pas remplacer le PNG pour les textes fins. Si aucun exporteur n'est disponible, ne pas ajouter de lien d'image casse : conserver les `.drawio` et signaler le blocage d'export.

### 5. Verifier le rendu et le contrat

- Ouvrir la source avec l'outil draw.io adapte et fournir le resultat au demandeur.
- Verifier que les textes ne sont pas tronques, que les connecteurs restent lisibles et qu'aucun noeud ne recouvre un autre lorsque le rendu est accessible.
- Comparer les statuts, endpoints et transitions du diagramme avec les fichiers lus avant de le declarer exact.
- Executer `git diff --check` apres une creation ou une modification de source documentaire lorsque l'outil d'execution est disponible.
- Valider les deux fichiers `.drawio` comme XML avant tout rendu ou export.
- Verifier que le PNG est non vide, lisible a la taille cible et reference uniquement une source `.drawio` existante.
- Si le rendu MCP echoue, conserver les sources natives, decrire l'erreur et ne pas affirmer qu'un apercu visuel a ete genere.

## Invariants SecureFiles a representer

Lorsqu'ils sont dans le perimetre du diagramme :

- `UPLOADING` n'est ni scannable, ni telechargeable, ni un upload accepte ;
- un upload complet passe par `PENDING_SCAN` avant toute demande de scan ;
- seul `CLEAN` autorise le flux de telechargement ;
- une erreur ou une reponse inconnue de ClamAV bloque le fichier ;
- les octets restent dans le stockage, jamais dans PostgreSQL, et les flux ne sont pas materialises integralement en memoire ;
- les sessions, cookies, secrets, tokens et contenus de fichiers ne doivent pas apparaitre dans le diagramme ;
- les frontieres `domain`, `application` et `infrastructure` restent visibles lorsque l'architecture est le sujet.

## Format de sortie

La reponse finale doit contenir, dans cet ordre :

1. `ANALYSE` : perimetre et sources observees ;
2. `DIAGRAMMES` : types produits, outils MCP utilises et chemins des deux sources `.drawio` et des apercus ;
3. `VALIDATION` : controles de coherence et rendu obtenu ;
4. `RISQUES RESTANTS` : zones ambiguës, rendu ou export non verifie, informations non determinees et eventuel statut `ABORTED`.

Avant de terminer, ajouter a la fin de `resume.md` un bloc contenant la demande utilisateur verbatim et un resultat factuel. Ne jamais y inscrire un secret, un token, le contenu d'un fichier ou une URL contenant des donnees sensibles.

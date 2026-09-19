# SecureFiles backend - regles d'architecture

## Perimetre

Ce dossier contient l'API Java 21 / Spring Boot et les traitements de scan de fichiers.
Le backend doit conserver une architecture hexagonale : le domaine exprime les regles
metier, les ports expriment les dependances, et les adaptateurs portent les details
techniques.

Le code source est sous `src/main/java/com/securefiles`. Les tests sont sous
`src/test/java` et suivent les memes packages que le code teste.

## Documents enfants

- `src/main/java/com/securefiles/domain/AGENTS.md` : modele, ports et use cases metier.
- `src/main/java/com/securefiles/application/AGENTS.md` : orchestration transverse sans
  duplication de logique metier, DTOs, mappers et adaptateurs HTTP entrants.
- `src/main/java/com/securefiles/infrastructure/AGENTS.md` : JPA, Liquibase, stockage,
  ClamAV, RabbitMQ et Outbox.
- `src/main/java/com/securefiles/config/AGENTS.md` : configuration Spring, profils et
  wiring technique.

Un document enfant precise ce document dans son sous-arbre. Il ne peut pas autoriser
une dependance ou un comportement qui viole les invariants ci-dessous.

## Invariants de securite

- Un transfert incomplet peut etre represente par `UPLOADING`, mais il n'est jamais
  scannable, telechargeable ni retourne comme upload accepte.
- Un upload complet et valide passe a `PENDING_SCAN` avant toute demande de scan et le
  `POST /api/v1/files` retourne `202 Accepted` avec ce statut.
- Seul `CLEAN` autorise l'ouverture du flux de telechargement pour un utilisateur
  authentifie. Le download n'est pas limite au proprietaire ; la suppression reste
  reservee au proprietaire ou a un administrateur.
- Toute panne, reponse inconnue, limite depassee ou timeout antivirus est fail closed.
  Aucun doute ne peut produire `CLEAN`.
- Les octets restent hors PostgreSQL. La base conserve les metadonnees, les statuts,
  les tentatives et les evenements Outbox.
- Les contenus sont transferes en flux. Un fichier complet ne doit pas etre materialise
  en memoire pour l'upload, le scan ou le download.
- Les noms, tailles et types MIME sont valides a la frontiere HTTP. Un nom fourni par
  le client ne devient jamais une cle de stockage ou un chemin local.
- Les logs ne contiennent ni secret, ni token, ni contenu, ni information sensible
  inutile au diagnostic.

## Architecture cible

```text
application/controller
        |
application/dto + application/mapper
  |
        v
ports entrants -> domain/file/usecases -> ports sortants
                                      |             |
                                      v             v
                             application      infrastructure
                                            PostgreSQL / S3 / ClamAV
                                            RabbitMQ / Outbox / Liquibase
```

### Domaine fichier

Le contexte fichier est regroupe sous `domain/file` :

```text
domain/file/
├── model/       agregats, value objects, statuts et transitions
├── port/
│   ├── in/      contrats des cas d'utilisation exposes au systeme
│   └── out/     contrats vers stockage, persistance, scanner et publication
└── usecases/    implementations pures des cas d'utilisation metier
```

`domain/file/usecases` est l'unique proprietaire de la logique metier d'un use case.
Un cas comme `UploadFileUseCase` implemente un port entrant, depend du modele et des
ports sortants, et reste independant de Spring, HTTP, JPA, AMQP et ClamAV.

`application` ne contient pas une seconde implementation de `UploadFile`, `ScanFile` ou
`DownloadFile`. Cette couche peut composer des use cases, adapter HTTP dans
`application/controller` ou appliquer une orchestration transverse, mais elle ne deplace
pas les invariants metier hors du domaine.

Les DTOs de cette couche sont places sous `application/dto` et leurs conversions sous
`application/mapper`. Un mapper applicatif ne lit pas un flux, ne touche pas aux ports
sortants et ne connait aucun detail HTTP, JPA, S3, AMQP ou ClamAV.

### Lisibilite et structure du code

- Les types de production backend sont top-level et places dans leur propre fichier ;
  les classes imbriquees ne sont pas utilisees pour cacher une implementation.
- Une implementation de use case expose un workflow lisible et delegue chaque etape a
  une methode courte nommee par intention metier.
- Le package `usecases` contient uniquement les services qui implementent les ports
  entrants. Les objets de domaine, erreurs et traitements lies a une fonctionnalite sont
  places dans un sous-package `model` adapte.
- Les tests appellent une classe et une methode de production. Les fakes, stubs, spies et
  classes de support dediees aux tests sont interdits.
- `@Mock` est autorise uniquement pour une classe, interface ou port de production dont
  depend le systeme teste. Le systeme teste reste une instance reelle de production.

### Adaptateurs

- `infrastructure/` implemente les ports sortants et les protocoles techniques.
- `application/controller/` adapte HTTP vers les DTOs applicatifs, utilise les mappers
  applicatifs puis appelle les ports entrants sans decider si un fichier est telechargeable.
- `config/` construit les beans et lit les proprietes sans porter de regle metier.

Aucune entite JPA ne traverse vers une reponse HTTP. Les DTOs applicatifs sont distincts
du modele de domaine et des entites de persistance.

## Cycle de vie du fichier

La machine d'etats canonique est :

```text
UPLOADING -> PENDING_SCAN -> SCANNING -> CLEAN
                                      -> INFECTED
                                      -> SCAN_FAILED
                                      -> REJECTED
```

Regles principales :

- `UPLOADING` est reserve au transfert non termine et doit etre nettoye ou rejete en
  cas d'expiration.
- `DELETING` est un etat interne de maintenance : il n'est pas listable ni telechargeable
  et un reaper reprend la suppression apres une interruption.
- `PENDING_SCAN` signifie que les octets sont ecrits, que la taille et le SHA-256 sont
  connus et que la version/ETag du stockage est conservee.
- Un worker reserve atomiquement `PENDING_SCAN -> SCANNING`.
- Un scanner explicite propre est le seul resultat qui permet `SCANNING -> CLEAN`.
- Une menace produit `INFECTED` et ne peut jamais etre promue en `CLEAN`.
- Une erreur temporaire produit un echec observable et peut etre remise en file avec
  backoff. Apres le nombre maximal de tentatives, `SCAN_FAILED` est terminal.
- Une violation connue de politique, comme une taille interdite, produit `REJECTED`.
- Un objet reference apres `PENDING_SCAN` est immuable. Un remplacement est une
  nouvelle version logique qui doit etre analysee.

La condition metier de telechargement est :

```text
downloadable(file, user) = file.status == CLEAN && authorization(user, file)
```

Elle est verifiee dans le use case de download, jamais uniquement dans le controleur ou
la console frontend.

## Flux de traitement

### Upload

1. La frontiere HTTP authentifie le demandeur et valide les limites.
2. Le use case recoit une commande metier et un flux, jamais un `MultipartFile`.
3. Le contenu est ecrit sous une cle opaque de quarantaine.
4. La taille et le SHA-256 sont calcules pendant le flux.
5. La version/ETag et les metadonnees sont verifies.
6. Le fichier complet passe a `PENDING_SCAN` et un evenement `FILE_SCAN_REQUESTED`
   est enregistre dans la meme transaction que la metadonnee.
7. La reponse retourne `202 Accepted` sans octets ni cle de stockage.

### Scan

1. Un relay Outbox publie un message minimal dans RabbitMQ.
2. Le worker revendique le fichier avec une transition conditionnelle.
3. Il verifie la taille, le hash et la version attendus.
4. Il ouvre le flux depuis le stockage prive et le transmet a ClamAV avec `INSTREAM`.
5. Il enregistre une tentative et le resultat normalise.
6. Il persiste la transition finale avant l'ack du message.
7. Une lease expiree peut etre recuperee sans permettre deux scans concurrents.

### Download

1. Le use case verifie l'existence et l'autorisation du demandeur.
2. Il refuse tous les statuts autres que `CLEAN`.
3. Il ouvre le flux depuis le stockage prive.
4. L'adaptateur REST applique un `Content-Disposition` avec un nom assaini.
5. La cle de stockage et les details internes ne sont jamais exposes.

## Choix techniques valides pour le MVP

- Stockage des octets : bucket S3-compatible prive ; MinIO est utilise en local.
- Analyse : ClamAV via `clamd` et `INSTREAM`.
- Asynchronisme : RabbitMQ avec livraison at-least-once et worker idempotent.
- Coherence DB/broker : Outbox dans PostgreSQL, puis relay vers RabbitMQ.
- Migrations : Liquibase avec des changelogs versionnes et retrocompatibles.
- Identite : port `CurrentUser` dans le domaine ou l'application ; l'adaptateur
  d'authentification de production est une evolution distincte.
- Distribution : streaming backend direct pour le MVP ; URL presignee uniquement apres
  validation et seulement comme evolution documentee.
- API : prefixe `/api/v1/files`, `409 Conflict` pour un fichier non disponible,
  `404 Not Found` pour une ressource inexistante ou non accessible, et `413 Payload Too
  Large` pour une taille depassee.

Ces choix concernent la cible de reconstruction. Le scaffold actuel peut encore contenir
moins de dependances et aucun flux metier implemente.

## Tests et qualite

- Les types de domaine instanciables directement sont testes par leur API publique. Un
  use case peut isoler ses ports sortants de production avec `@Mock`.
- Les tests n'utilisent ni fake, ni stub, ni spy, ni classe de support dediee aux tests.
- La couche `application` n'a pas de tests de couche : seuls les mappers purs sous
  `application/mapper` sont testes.
- La couche `infrastructure` n'a pas de tests de couche : aucun adaptateur, protocole,
  repository, stockage ou client externe n'est teste directement.
- Les controles HTTP ne recoivent pas de tests de couche ; leur contrat reste documente,
  compile et relu sans reimplementation des regles du domaine.
- Les transitions, invariants de download, resultats antivirus, retry et cas limites
  restent proteges par les tests du domaine lorsqu'ils sont implementes.

## Validation locale

Depuis `backend/` :

```bash
mvn test
mvn spring-boot:run
```

Apres toute demande ou modification backend, revenir a la racine du depot et lancer
`env -u DATABASE_URL -u DATABASE_USERNAME -u DATABASE_PASSWORD make back`. Attendre la fin
de l'initialisation ou l'echec de Spring Boot, puis verifier les logs de demarrage et
`GET /actuator/health` avec un statut `UP` lorsque le serveur est disponible.

Les dependances locales peuvent etre demarrees avec :

```bash
docker compose up -d postgres minio rabbitmq clamav
```

Toute evolution de schema, de contrat HTTP ou de dependance doit etre documentee dans
`README.md` et verifiee par la validation adaptee ; cette validation ne cree pas de test
de couche `application` ou `infrastructure` et teste uniquement les mappers purs concernes.

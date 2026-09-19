# SecureFiles

Micro-service de depot et de distribution de fichiers securises, realise pour un exercice Java / Spring Boot / React / PostgreSQL.

## Etat actuel

La reconstruction couvre maintenant le flux complet d'upload, de scan, de distribution et de
maintenance du backend :

- le domaine contient le use case d'upload, ses ports et ses invariants de streaming ;
- le domaine contient aussi la creation d'utilisateur, l'authentification par session et
   la revocation des sessions ;
- `application/dto` et `application/mapper` contiennent les conversions entre le domaine
   et la couche applicative ;
- `application/controller` contient le point d'entree `POST /api/v1/files` ;
- les adaptateurs PostgreSQL/JPA, stockage objet MinIO, ClamAV et RabbitMQ/Outbox sont
   presents et relies par la configuration Spring.
- les leases, l'Outbox, les quotas, le rate limiting et les reapers recuperent les traitements
   interrompus sans contourner le fail closed.

La suite `mvn test` est executable sans dependance externe. Le demarrage Spring et le flux
complet necessitent PostgreSQL, MinIO, RabbitMQ et ClamAV.

## Analyse du sujet

Le risque principal n'est pas l'upload : c'est de laisser un chemin de telechargement contourner l'analyse antivirus. Le service applique donc une machine d'etats fermee :

```text
UPLOADING -> PENDING_SCAN -> SCANNING -> CLEAN
                         -> INFECTED
                         -> SCAN_FAILED
UPLOADING -> REJECTED
```

`DELETING` est un statut interne de maintenance : une suppression le marque avant de retirer
l'objet puis les metadonnees. Il n'est pas listable par l'API publique et un reaper reprend les
suppressions interrompues.

Un fichier nouvellement recu est depose dans un espace de quarantaine, puis analyse par ClamAV. Tant que le resultat n'est pas `CLEAN`, l'API de contenu renvoie `409 Conflict`. Les erreurs de connexion ou de protocole antivirus sont traitees comme un refus, jamais comme un fichier sain.

Les octets sont conserves sur un volume de fichiers et non dans PostgreSQL. PostgreSQL contient l'identifiant, le nom original nettoye, le type MIME declare, la taille, le hash SHA-256, le statut et les dates. Cette separation evite de faire grossir les lignes SQL et permet de remplacer le volume local par S3/MinIO sans changer le domaine.

## Structure

```text
SecureFiles/
├── backend/
│   ├── pom.xml              # dependances et build Maven
│   └── src/                 # domaine, application et infrastructure
├── frontend/                # console React + Vite
│   ├── package.json         # dependances et scripts npm
│   └── src/                 # code applicatif et tests dans src/tests/
├── docs/diagrams/           # sources draw.io natives et apercus PNG
├── Makefile                 # alias de lancement du frontend et du backend
├── rules/                   # regles partagees de code, tests et UX/UI
├── resume.md                # journal resume des prompts traites
├── .github/skills/          # skills SecureFiles charges par l'agent VS Code
├── .github/prompts/         # prompts de workflow declenches a la demande
├── .github/instructions/    # instructions ciblees par type de fichier
├── .github/agents/          # agent VS Code SecureFilesAgent
├── docker-compose.yml       # PostgreSQL, MinIO, RabbitMQ et ClamAV locaux
└── README.md
```

## Diagrammes

Les diagrammes editables sont des sources XML draw.io rangees par categorie. Chaque vue detaillee possede une vue `-simplified` correspondante ; Mermaid et Excalidraw sont conserves uniquement comme brouillons ou archives.

| Sujet | Apercu simplified | Source editable |
| --- | --- | --- |
| Upload secure file | [![Upload file sequence simplified](docs/diagrams/sequence/renders/upload-file-sequence-simplified.png)](docs/diagrams/sequence/upload-file-sequence-simplified.drawio) | [draw.io](docs/diagrams/sequence/upload-file-sequence-simplified.drawio) |
| Upload use case | [![Upload use case simplified](docs/diagrams/use_case/renders/upload-file-simplified.png)](docs/diagrams/use_case/upload-file-simplified.drawio) | [draw.io](docs/diagrams/use_case/upload-file-simplified.drawio) |

Pour regenerer les apercus, installer ou fournir le binaire draw.io desktop puis lancer `DRAWIO_BIN=/path/to/drawio make diagrams-export`. Ajouter `DIAGRAM_EXPORT_JPG=1` pour produire aussi les JPG.

## Contrat HTTP initial

- `POST /api/v1/files` : recoit un champ multipart `file`, renvoie `202 Accepted` et les metadonnees en `PENDING_SCAN`. Une taille superieure a la politique serveur renvoie `413 Payload Too Large` avec le code stable `MAX_SIZE_EXCEEDED`. La console demande une connexion avant d'ouvrir le selecteur de fichier et avant tout envoi.
- `GET /api/v1/files/config` : expose la politique publique d'upload sous la forme `{ "maximumSizeBytes": <entier> }`. La reponse ne contient ni variable d'environnement ni configuration sensible.
- `GET /api/v1/files?page=1&size=10&sort=createdAt&direction=desc` : liste publiquement
   une page de metadonnees de fichiers. `page` commence a `1`, `size` vaut `10` par defaut
   et est limite a `50`. L'offset calcule `(page - 1) * size` ne peut pas depasser `10 000`.
   Les valeurs de `sort` sont limitees a `name`, `author`, `size` et
   `createdAt`. `direction` accepte `asc` ou `desc`. Chaque requete ajoute `id DESC` comme
   departage deterministe, quel que soit le tri choisi.
   Le filtre `status` peut etre repete, par exemple
   `status=CLEAN&status=SCAN_FAILED` ; les valeurs sont combinees avec OR. Sans filtre,
   les sept statuts canoniques sont inclus. Le tri et le filtrage sont executes en base
   avant le comptage et la pagination, afin que `totalElements`, `totalPages`, `hasNext`
   et `hasPrevious` correspondent aux criteres demandes. Le tri par auteur repose sur la
   jointure infrastructurelle avec `app_user` et conserve `Auteur inconnu` pour les
   proprietaires non resolus.
   La reponse est une enveloppe `{ "content": [], "page": 1, "size": 10,
   "totalElements": 0, "totalPages": 0, "hasNext": false, "hasPrevious": false }`.
   Seuls les fichiers de la page demandee sont charges et enrichis. La reponse ne contient
   ni contenu, ni hash, ni cle MinIO. Chaque element expose l'auteur resolu dans `author` et
   le type MIME declare nullable dans `clientContentType`. Les champs `failureCode` et
   `failureCause` sont toujours `null` dans cette liste publique, y compris lorsqu'un scan
   echoue. Une
   page hors limite renvoie une enveloppe vide avec le statut `200`. Une pagination invalide
   renvoie `INVALID_PAGINATION`; un tri, une direction ou un statut invalide renvoie
   `INVALID_LIST_QUERY`, dans les deux cas avec le statut `400`. Chaque element expose aussi
   `canDownload` et `canDelete`. Ces capacites sont calculees par le backend pour le demandeur
   courant : `canDownload` vaut `true` pour tout demandeur authentifie d'un fichier `CLEAN`, et
   `canDelete` vaut `true` pour son proprietaire ou un administrateur, sauf lorsque le
   fichier est `UPLOADING` ou `SCANNING`. Ces indicateurs servent a construire l'interface ; les
   controles d'acces restent appliques par les use cases des endpoints.
   Cette exposition publique est une decision explicite du MVP : seuls le nom nettoye, la
   taille, le type MIME declare, l'auteur resolu, le statut et les capacites sont exposes.
   Les octets, le hash SHA-256, la cle de stockage et les diagnostics internes ne sont pas
   publics. Un deploiement traitant des noms ou auteurs confidentiels doit proteger cette
   liste par authentification avant mise en production.
- `DELETE /api/v1/files/{id}` : supprime le contenu prive, les metadonnees et les evenements Outbox
   associes pour le proprietaire authentifie ou un administrateur (`ROLE_ADMIN`). La reponse est
   `204 No Content`. Un fichier absent ou inaccessible renvoie `404 Not Found` avec `FILE_NOT_FOUND`.
   Une suppression pendant `UPLOADING` ou `SCANNING` renvoie `409 Conflict` avec
   `FILE_NOT_AVAILABLE`. Une erreur de stockage ou de persistance renvoie `500 Internal Server Error`
   avec `FILE_DELETE_FAILED`. Le backend reste la source d'autorite : les capacites de la liste ne
   remplacent pas l'autorisation de cette route.
   La suppression revendique d'abord l'etat interne `DELETING`, qui est exclu des listes et
   des telechargements. Le stockage puis les metadonnees sont supprimes de maniere repriseable
   par un reaper ; il n'existe toujours pas de transaction distribuee MinIO/PostgreSQL, donc
   une panne peut laisser un objet orphelin et doit etre traitee par reconciliation.
- `GET /api/v1/files/{id}` : lit les metadonnees et le statut courant du fichier pour
   son proprietaire, y compris le type MIME declare dans `clientContentType` et un `failureCode`
   nullable lorsqu'une erreur est connue. Si les tentatives de scan sont epuisees,
   `failureCause` expose en plus la derniere cause precise. Un fichier
   absent ou inaccessible renvoie `404`; cette reponse n'expose ni les octets ni la cle de
   stockage.
- `GET /api/v1/files/{id}/content` : streame le contenu pour tout utilisateur authentifie lorsque
   le statut est `CLEAN`. Un fichier absent ou inaccessible renvoie `404`, et un
   fichier dont le statut n'est pas `CLEAN` renvoie `409 Conflict`. Le flux est emis directement par
   le backend, sans charger le fichier complet en memoire dans la console.
- `POST /api/v1/auth/register` : cree un compte public avec un ou plusieurs roles autorises
   (`developpeur` et `utilisateur`) et renvoie le profil sans mot de passe ni token. Le mot
   de passe doit contenir entre 8 et 255 caracteres ; une valeur hors limites renvoie
   `400 Bad Request` avec le code `INVALID_PASSWORD`.
- `POST /api/v1/auth/login` : ouvre une session de 30 jours, persiste son identifiant et
   renvoie le profil. Le JWT est uniquement transmis dans le cookie HttpOnly
   `SECUREFILES_AUTH` par defaut. La route est limitee par IP source a cinq requetes par
   minute par defaut. Un depassement renvoie `429 Too Many Requests`, le code stable
   `LOGIN_RATE_LIMIT_EXCEEDED` et `Retry-After`; une indisponibilite du compteur renvoie
   `503` avec `LOGIN_RATE_LIMIT_UNAVAILABLE`.
- `GET /api/v1/users/me` : renvoie le profil de l'utilisateur authentifie. Sans session
   valide, il renvoie `204 No Content` plutot que `401 Unauthorized`. Le JWT est verifie
   cryptographiquement et la session doit encore etre active en base lorsqu'il est present.
- `GET /api/v1/users/me/storage` : renvoie `{ "usedBytes": <entier>, "quotaBytes": <entier> }`
   pour l'utilisateur authentifie. L'identite est derivee de la session, jamais d'un identifiant
   fourni par le client. Lorsqu'aucune ligne de quota n'existe encore, `usedBytes` vaut `0` et
   `quotaBytes` reprend la valeur configuree par proprietaire (`securefiles.quota.per-owner`,
   `STORAGE_QUOTA_PER_OWNER`). Les deux valeurs sont des octets ; le profil peut les afficher
   avec les libelles de stockage de la console sans modifier le quota persiste. `usedBytes`
   compte uniquement les fichiers `CLEAN` ; les fichiers `PENDING_SCAN` et `SCANNING` utilisent
   une reservation interne pour proteger le quota concurrent sans reduire cette valeur visible,
   et les fichiers `INFECTED` ou `SCAN_FAILED` ne sont pas comptes. Sans session valide, la route
   renvoie `204 No Content`.
- `POST /api/v1/auth/logout` : revoque la session courante et efface le cookie HttpOnly.
- `GET /api/v1/auth/csrf` : initialise le cookie CSRF lisible par le frontend en production.
- `GET /actuator/health` : healthcheck technique.

Le controller transmet l'upload au port entrant du domaine. Le stockage, la persistance,
ClamAV et la publication RabbitMQ sont des adaptateurs separes ; une Outbox PostgreSQL est
persistee avant toute publication RabbitMQ et n'est marquee publiee qu'apres confirmation du broker.

Les nouveaux mots de passe sont pre-hashes en SHA-256 UTF-8 avant BCrypt dans un format
versionne. Cela evite la limite historique de 72 octets de BCrypt tout en conservant la
verification des anciens hashes BCrypt bruts. Le contrat HTTP reste de 8 a 255 caracteres.

Les compteurs de tentative sont independants : `publish_attempts` mesure les tentatives de
publication Outbox, `scan_attempt_count` mesure les reservations metier de scan et les
redeliveries RabbitMQ sont bornees par `RABBITMQ_MAXIMUM_TRANSPORT_RETRIES` (3 par defaut).
Apres epuisement, le message est publie dans la DLQ configuree ; la queue de retry ne peut
donc plus boucler indefiniment. Lorsque la limite de redrive DLQ est atteinte, un fichier
encore `PENDING_SCAN` passe a `SCAN_FAILED` avec `SCAN_ATTEMPTS_EXHAUSTED` et la cause
precise `RABBITMQ_TRANSPORT_EXHAUSTED`; le message n'est acquitte qu'apres cette persistance.
L'Outbox attache `securefiles-file-id` au message afin de conserver cette correlation meme si
le JSON est indecodable. Un message sans correlation exploitable est publie dans la queue
poison durable `RABBITMQ_POISON_QUEUE` avant son acquittement, pour analyse hors du flux de scan.

## Demarrage local

Prerequis : Java 21+, Maven 3.9+, Node.js 22+, Docker Compose.

Sur Apple Silicon, le service ClamAV est execute en `linux/amd64` via l'emulation Docker
Desktop, car l'image officielle utilisee ne publie pas de variante `linux/arm64`.

Le backend possede son entree REST et le wiring des ports sortants. La cible `make init`
prepare le demarrage local necessaire au flux complet : elle cree `.env` depuis
`.env.example` s'il n'existe pas, demarre les dependances, compile le backend et installe
les dependances frontend.

1. Initialiser l'environnement local en premier :

   ```bash
   make init-env
   ```

2. Initialiser le demarrage local :

   ```bash
   make init
   ```

   Sous Windows, GNU Make peut etre installe avec Chocolatey depuis une console
   PowerShell administrateur : `choco install make -y`. Maven est aussi requis pour
   compiler et lancer le backend : `choco install maven -y`. Les cibles `init-env` et `back`
   utilisent automatiquement la syntaxe Windows lorsqu'elles sont executees avec GNU Make.
   Apres l'installation de Maven, recharger le profil Chocolatey puis l'environnement :

   ```powershell
   Import-Module $env:ChocolateyInstall\helpers\chocolateyProfile.psm1
   refreshenv
   ```

   Fermer puis rouvrir VS Code produit le meme effet. `mvn` doit ensuite etre disponible
   dans le `PATH` de `make`.
   La cible `init` depend elle-meme de `init-env` ; l'etape est donc rejouee sans effet
   si `.env` existe deja.

   PostgreSQL reste sur le port `5432` dans le conteneur et est expose sur le port
   hote `5433` par defaut afin d'eviter les collisions avec une instance PostgreSQL
   deja installee sur macOS. Le port hote peut etre change avec `POSTGRES_HOST_PORT`,
   en alignant alors `DATABASE_URL`.

3. Lancer l'API dans un terminal :

   ```bash
   make back
   ```

   Le profil `local` conserve uniquement les reglages de developpement de l'authentification
   (cookie non securise et cle ephemere) et reste utilise par defaut. Pour lancer un autre
   profil, utiliser `SPRING_PROFILES_ACTIVE=prod make back`. Toute route protegee exige une
   session JWT active ; il faut donc creer un compte et se connecter avant d'utiliser les
   fichiers.

4. Dans un autre terminal, lancer la console :

   ```bash
   make front
   ```

La console est disponible sur `http://localhost:5173` et l'API sur `http://localhost:8080`.

### Authentification locale et production

Les sessions durent exactement 30 jours (`JWT_TOKEN_LIFETIME=PT720H`). La cle de signature
`JWT_SECRET` doit contenir au moins 32 octets et peut etre fournie en Base64 ou en texte
UTF-8. En production, cette variable est obligatoire. Le profil `local` autorise une cle
ephemere si `JWT_ALLOW_EPHEMERAL_KEY=true` via sa configuration locale ; cette cle est
regenereree a chaque demarrage et invalide les sessions precedentes.

Le cookie d'authentification est HttpOnly, limite au chemin `/` et utilise `SameSite=Lax`.
`JWT_COOKIE_NAME` permet de changer son nom et `JWT_COOKIE_SECURE=true` doit etre conserve
en HTTPS. Les roles publics sont configures par `securefiles.auth.registration-roles` et
doivent rester limites a `developpeur` et `utilisateur`; le role `admin` n'est pas
selectionnable par l'inscription publique.

En production, les requetes mutantes sont protegees par CSRF. Le frontend appelle d'abord
`GET /api/v1/auth/csrf`, puis envoie le cookie CSRF lisible par le navigateur avec la requete.
Le profil local desactive cette protection uniquement pour faciliter le developpement local.

### Test d'integration du scan

Le test `FileScanFlowIntegrationTest` demarre une API Spring sur un port aleatoire,
envoie un fichier sans menace connue, puis attend un statut antivirus terminal. Il est
desactive par defaut pour que `mvn test` reste autonome ; les dependances Docker doivent
etre demarrees avant son execution :

```bash
cd backend
SECUREFILES_INTEGRATION=true mvn -Dtest=FileScanFlowIntegrationTest test
```

Cette classe verifie egalement qu'un upload accepte en `202/PENDING_SCAN` ne reduit pas
`usedBytes` avant le scan. Apres un resultat `CLEAN`, la taille est transferee de la
reservation interne vers `usedBytes`; un resultat `INFECTED` ou `SCAN_FAILED` libere la
reservation sans consommer le quota visible.

Le test `UserAuthenticationFlowIntegrationTest` verifie l'inscription, la session, le
refus d'un suffixe different au-dela de 72 octets et la revocation :

```bash
cd backend
SECUREFILES_AUTH_INTEGRATION=true \
mvn -Dtest=UserAuthenticationFlowIntegrationTest test
```

Si une variable `DATABASE_URL` existe deja dans le shell, elle peut cibler une autre base.
Pour utiliser explicitement la base PostgreSQL du Compose, lancer le test avec :

```bash
cd backend
DATABASE_URL=jdbc:postgresql://localhost:5433/securefiles \
DATABASE_USERNAME=securefiles \
DATABASE_PASSWORD=securefiles-local-only \
JWT_SECRET= \
SECUREFILES_INTEGRATION=true \
mvn -Dtest=FileScanFlowIntegrationTest test
```

En cas de timeout, le test affiche le statut du fichier, le lease, les tentatives de scan
et l'etat de publication Outbox, sans afficher les octets du fichier.

Le test execute aussi un flux de `19 553 061` octets, genere par blocs dans un fichier
temporaire puis envoye comme multipart. Il couvre la branche multipart MinIO sans
materialiser le contenu complet en memoire.

Deux scenarios opt-in peuvent aussi verifier que ClamAV bloque un fichier EICAR texte
et une archive EICAR. Les fichiers restent hors du depot et leurs chemins sont fournis
uniquement a l'execution :

```bash
cd backend
SECUREFILES_INTEGRATION=true \
SECUREFILES_EICAR_TEXT_PATH=/chemin/vers/eicar.com.txt \
SECUREFILES_EICAR_ZIP_PATH=/chemin/vers/eicar_com2.zip \
mvn -Dtest=FileScanFlowIntegrationTest test
```

Chaque scenario verifie `202/PENDING_SCAN`, puis exige le statut terminal `INFECTED`.
Sans variable correspondante, le scenario est ignore; un chemin configure mais illisible
fait echouer le test.

### Test d'integration d'une limite ClamAV basse

Le test `FileScanSizeLimitIntegrationTest` utilise uniquement de petits fichiers generes
pendant le test. Il verifie qu'un fichier de `512 KiB` atteint `CLEAN`, puis qu'un fichier
de `2 MiB` depassant la limite ClamAV de test reste bloque en `SCAN_FAILED` et ne peut pas
etre telecharge.

La limite basse est fournie uniquement par l'override Compose de test
`docker-compose.integration-scan-limit.yml`. La configuration de production et
`MAX_FILE_SIZE_BYTES` ne sont pas modifies par ce scenario : la limite d'upload reste
superieure a la taille du fichier afin que le scan soit effectivement atteint.

```bash
docker compose \
   -f docker-compose.yml \
   -f docker-compose.integration-scan-limit.yml \
   up -d --wait postgres minio rabbitmq clamav

cd backend
SECUREFILES_SCAN_LIMIT_INTEGRATION=true \
mvn -Dtest=FileScanSizeLimitIntegrationTest test
```

L'override configure `StreamMaxLength` a `1 MiB` et conserve `MaxFileSize` ainsi que
`MaxScanSize` a `4 MiB` afin d'isoler le depassement du flux ClamAV. Il ne doit pas etre
utilise pour le lancement local normal.

### Politique de taille d'upload

`MAX_FILE_SIZE_BYTES` definit, en octets, la taille maximale autorisee pour un fichier.
Cette source de verite unique aligne la limite multipart Spring,
`securefiles.upload.maximum-size` du domaine et les limites ClamAV `StreamMaxLength`,
`MaxFileSize` et `MaxScanSize`. La valeur par defaut est `1073741824` (1 Gio). La console
lit cette politique par `GET /api/v1/files/config` et ne duplique pas de limite locale.

Apres un changement de `MAX_FILE_SIZE_BYTES`, recreer le conteneur ClamAV pour que
`clamd` recharge ses limites :

```bash
docker compose up -d --force-recreate --wait clamav
```

ClamAV est expose sur le port hote `3311` par defaut, configurable par
`CLAMAV_HOST_PORT`. Le backend local utilise la meme valeur par `CLAMAV_PORT`; ce port
separe evite de joindre par erreur un daemon ClamAV installe sur macOS qui ecouterait sur
le port standard `3310`.

`MAX_REQUEST_SIZE_BYTES` couvre la requete multipart complete et doit donc rester
strictement superieure a `MAX_FILE_SIZE_BYTES`. Sa valeur par defaut est `1153433600`
(1 100 Mio), ce qui laisse de la place a l'enveloppe multipart pour un fichier de 1 Gio.
Si `MAX_FILE_SIZE_BYTES` est modifie, `MAX_REQUEST_SIZE_BYTES` doit etre ajuste en
consequence.

La console lit `GET /api/v1/files/config` avant l'envoi et affiche la taille maximale
autorisee dans la zone de depot. Elle desactive l'action d'upload si la politique ne peut
pas etre lue ou si le fichier la depasse, et propose une relance de lecture. Cette
verification ameliore l'experience utilisateur; un client HTTP direct reste controle par
Spring et par la verification streamee du domaine.

### Delai global ClamAV

Chaque analyse ClamAV est bornee par `CLAMAV_SCAN_TIMEOUT`, avec une valeur par defaut de
`PT4M`, inferieure a la lease de scan par defaut `PT5M`. Ce budget couvre la connexion, la
lecture du stockage, l'ecriture `INSTREAM` et la lecture de la reponse. A expiration, le flux
et le socket sont fermes, le fichier reste indisponible et la tentative conserve la cause
stable `CLAMAV_SCAN_TIMEOUT`; aucun timeout ne peut produire `CLEAN`.

### Initialisation et alias de lancement

Depuis la racine du depot :

```bash
make init
make front
make back
```

`make init` demarre PostgreSQL, MinIO, RabbitMQ et ClamAV avec Docker Compose, installe les dependances npm et compile le backend sans lancer de serveur.
`make front` installe ou verifie les dependances frontend puis lance Vite depuis `frontend/`.
`make back` demarre les dependances Docker, compile le backend puis lance Spring Boot depuis `backend/`.

Le profil Spring `local` reste necessaire par defaut pour le lancement local : il desactive
le flag `Secure` du cookie pour `http://localhost` et autorise la cle JWT ephemere lorsque
`JWT_SECRET` n'est pas fourni. Pour utiliser un autre profil, fournir explicitement
`SPRING_PROFILES_ACTIVE` :

```bash
SPRING_PROFILES_ACTIVE=prod make back
```

Ce profil peut etre retire uniquement si la configuration d'execution fournit deja un
`JWT_SECRET` valide et les reglages de cookie/HTTPS adaptes ; ce n'est pas le cas du
demarrage local par defaut.

## Test rapide de l'API

```bash
curl -c cookies.txt \
   -H 'Content-Type: application/json' \
   -d '{"name":"Alice Martin","password":"mot-de-passe","roles":["developpeur","utilisateur"]}' \
   http://localhost:8080/api/v1/auth/register

curl -c cookies.txt -b cookies.txt \
   -H 'Content-Type: application/json' \
   -d '{"name":"Alice Martin","password":"mot-de-passe"}' \
   http://localhost:8080/api/v1/auth/login

curl -b cookies.txt http://localhost:8080/api/v1/users/me
curl -b cookies.txt http://localhost:8080/api/v1/users/me/storage
curl -b cookies.txt -F "file=@./document.pdf" http://localhost:8080/api/v1/files
curl -b cookies.txt http://localhost:8080/api/v1/files
curl -b cookies.txt -OJ http://localhost:8080/api/v1/files/<id>/content
curl -b cookies.txt -X DELETE http://localhost:8080/api/v1/files/<id>
curl -b cookies.txt -X POST http://localhost:8080/api/v1/auth/logout
```

En production, appeler d'abord `GET /api/v1/auth/csrf` et transmettre le cookie CSRF dans
les requetes mutantes (`POST` et `DELETE`). Le telechargement ne devient possible qu'apres le passage
a `CLEAN`.
Pour tester un fichier detecte, utiliser le fichier EICAR de test dans un environnement
isole, jamais un malware reel.

## Decisions de securite et de capacite

- **Fail closed** : `PENDING_SCAN`, `SCANNING`, `INFECTED` et `SCAN_FAILED` sont tous non telechargeables.
- **Flux** : l'upload, l'analyse ClamAV `INSTREAM` et le download utilisent des flux ; la limite multipart borne les abus HTTP. Le budget global `CLAMAV_SCAN_TIMEOUT` arrete une analyse qui ne progresse plus.
- **Integrite** : un SHA-256 est calcule au depot et conserve en base pour verifier un objet avant scan. La taille et la version canonique MinIO sont relues apres l'ecriture; une divergence bloque le fichier avant `PENDING_SCAN`.
- **Codes de defaillance** : les codes stables sont centralises dans le domaine et reproduits dans la console pour afficher un diagnostic securise sans exposer une exception brute.
- **Noms** : le chemin fourni par le client est reduit a un nom de fichier, sans traversal.
- **Metadonnees publiques** : la liste recente est publique par choix MVP, avec une surface
  limitee aux metadonnees declarees ci-dessus ; elle ne doit pas etre utilisee pour des noms
  ou auteurs confidentiels sans changement de politique.
- **Observabilite** : Actuator expose le healthcheck ; les reapers, le rate limiting et les
   erreurs de scan journalisent des identifiants techniques sans contenu sensible. Les metriques
   de latence, taille, statut de scan, profondeur des queues et taux d'erreur restent a ajouter
   avant production.
- **Acces** : les sessions JWT sont liees a une session persistante et revoquees au logout ;
   la liste et le download restent volontairement accessibles selon les decisions MVP existantes.
   Les quotas sont actuellement calcules par proprietaire ; une autorisation par tenant et un
   fournisseur d'identite externe restent a evaluer avant exposition publique.
- **Capacite** : une reservation interne est ajoutee atomiquement avant `PENDING_SCAN` pour
   proteger l'admission concurrente, mais `usedBytes` ne compte que les fichiers `CLEAN`.
   Un fichier `INFECTED` ou `SCAN_FAILED` libere sa reservation sans consommer le quota visible.
   Le rate limiting des uploads et des connexions est partage par PostgreSQL et renvoie `429`
   lorsque le bucket correspondant est depasse ; les buckets de connexion sont namespaces par
   IP et ne se melangent pas avec ceux d'upload. Les compteurs expires sont purges par un reaper.
- **Suppression** : `DELETE` revendique `DELETING` avant de retirer l'objet et les metadonnees.
   Les reapers reprennent les suppressions et uploads abandonnes, mais la reconciliation des objets
   orphelins reste necessaire avant une garantie de disponibilite de production.

## Suite recommandee

1. Remplacer MinIO local par un stockage objet prive de production avec URLs signees emises uniquement apres validation.
2. Ajouter une reconciliation et un nettoyage des metadonnees ou objets orphelins apres les
   suppressions partielles.
3. Ajouter des metriques, traces, alertes et runbooks pour les queues, leases, scans, quotas et DLQ.
4. Ajouter des tests de charge, de panne, de redemarrage et de restauration sur les dependances externes.
5. Ajouter retention, chiffrement au repos, rotation des secrets et audit des acces.

## Agent importe

`SecureFilesAgent` est l'adaptation de l'agent TrainMyMat : le role d'ingenieur senior et la discipline hexagonale sont conserves, tandis que le contexte Yu-Gi-Oh!/Expo est remplace par React web, Spring Boot, PostgreSQL, stockage de fichiers et antivirus.

## Contexte des agents

Les agents chargent les regles partagees depuis `rules/` selon la tache :

- `rules/clean_code.md` pour les principes de lisibilite et de conception ;
- `rules/java.md` pour les conventions Java du backend ;
- `rules/strategy_test.md` pour la couverture du domaine, les doubles de ports et le nommage des tests ;
- `rules/ux_ui.md` pour la console, les etats de scan, le responsive et l'accessibilite.

Les skills specialises sont documentes dans `.github/skills/README.md`. L'agent les charge au besoin pour les revues, tests, changements de securite, evolutions frontend, documentation des features et livraison issue/PR. Le prompt [.github/prompts/plan-approve-implement.prompt.md](.github/prompts/plan-approve-implement.prompt.md) impose une validation du plan avant implementation et peut deleguer l'exploration au planner [SecureFilesPlanner](.github/agents/SecureFilesPlanner.agent.md) en lecture seule. Les conventions propres au frontend sont appliquees depuis `.github/instructions/frontend.instructions.md`.

## MCP GitHub et icones Lucide

La configuration partagee des serveurs MCP se trouve dans [.vscode/mcp.json](.vscode/mcp.json) :

- `github` utilise le serveur HTTP officiel et ne contient aucun token. Lors de la premiere utilisation dans VS Code, une authentification OAuth GitHub peut etre demandee ;
- `lucid-icon` lance le serveur tiers `lucide-mcp` avec `npx`, pour rechercher des icones Lucide et recuperer leurs metadonnees ou leur SVG.

Le serveur Lucide n'est pas officiel ; son paquet est installe a la demande par `npx -y lucide-mcp`. Les interfaces frontend doivent utiliser le MCP Lucide pour choisir une icone, puis le composant correspondant de `lucide-react` dans le code.

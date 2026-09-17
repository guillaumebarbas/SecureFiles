# SecureFiles

Micro-service de depot et de distribution de fichiers securises, realise pour un exercice Java / Spring Boot / React / PostgreSQL.

## Etat actuel

La reconstruction couvre maintenant le premier flux complet d'upload et de scan du backend :

- le domaine contient le use case d'upload, ses ports et ses invariants de streaming ;
- le domaine contient aussi la creation d'utilisateur, l'authentification par session et
   la revocation des sessions ;
- `application/dto` et `application/mapper` contiennent les conversions entre le domaine
   et la couche applicative ;
- `application/controller` contient le point d'entree `POST /api/v1/files` ;
- les adaptateurs PostgreSQL/JPA, stockage objet MinIO, ClamAV et RabbitMQ/Outbox sont
   presents et relies par la configuration Spring.

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

Un fichier nouvellement recu est depose dans un espace de quarantaine, puis analyse par ClamAV. Tant que le resultat n'est pas `CLEAN`, l'API de contenu renvoie `409 Conflict`. Les erreurs de connexion ou de protocole antivirus sont traitees comme un refus, jamais comme un fichier sain.

Les octets sont conserves sur un volume de fichiers et non dans PostgreSQL. PostgreSQL contient l'identifiant, le nom original nettoye, le type MIME declare, la taille, le hash SHA-256, le statut et les dates. Cette separation evite de faire grossir les lignes SQL et permet de remplacer le volume local par S3/MinIO sans changer le domaine.

## Structure

```text
SecureFiles/
├── backend/
│   ├── pom.xml              # dependances et build Maven
│   └── src/                 # domaine, application et infrastructure
├── frontend/                # console React + Vite a reconstruire
│   ├── package.json         # dependances et scripts npm
│   └── src/                 # code applicatif et tests dans src/tests/
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

## Contrat HTTP initial

- `POST /api/v1/files` : recoit un champ multipart `file`, renvoie `202 Accepted` et les metadonnees en `PENDING_SCAN`. Une taille superieure a la politique serveur renvoie `413 Payload Too Large` avec le code stable `MAX_SIZE_EXCEEDED`. La console demande une connexion avant d'ouvrir le selecteur de fichier et avant tout envoi.
- `GET /api/v1/files/config` : expose la politique publique d'upload sous la forme `{ "maximumSizeBytes": <entier> }`. La reponse ne contient ni variable d'environnement ni configuration sensible.
- `GET /api/v1/files` : liste publiquement les metadonnees de tous les fichiers. La reponse
   est un tableau trie par date de creation decroissante puis par
   identifiant ; elle ne contient ni contenu, ni hash, ni cle MinIO. Chaque element expose
   l'auteur resolu dans `author` et peut exposer un `failureCode` nullable et stable
   lorsqu'un traitement a echoue. La consultation de la liste n'accorde pas le droit de
   telecharger un fichier ou de lire ses metadonnees detaillees : ces acces restent
   controles par le proprietaire.
- `GET /api/v1/files/{id}` : lit les metadonnees et le statut courant du fichier pour
   son proprietaire, avec un `failureCode` nullable lorsqu'une erreur est connue. Un fichier
   absent ou inaccessible renvoie `404`; cette reponse n'expose ni les octets ni la cle de
   stockage.
- `GET /api/v1/files/{id}/content` : streame le contenu uniquement si le statut est `CLEAN`.
- `POST /api/v1/auth/register` : cree un compte public avec un ou plusieurs roles autorises
   (`developpeur` et `utilisateur`) et renvoie le profil sans mot de passe ni token. Le mot
   de passe doit contenir entre 8 et 255 caracteres ; une valeur hors limites renvoie
   `400 Bad Request` avec le code `INVALID_PASSWORD`.
- `POST /api/v1/auth/login` : ouvre une session de 30 jours, persiste son identifiant et
   renvoie le profil. Le JWT est uniquement transmis dans le cookie HttpOnly
   `SECUREFILES_AUTH` par defaut.
- `GET /api/v1/users/me` : renvoie le profil de l'utilisateur authentifie. Sans session
   valide, il renvoie `204 No Content` plutot que `401 Unauthorized`. Le JWT est verifie
   cryptographiquement et la session doit encore etre active en base lorsqu'il est present.
- `POST /api/v1/auth/logout` : revoque la session courante et efface le cookie HttpOnly.
- `GET /api/v1/auth/csrf` : initialise le cookie CSRF lisible par le frontend en production.
- `GET /actuator/health` : healthcheck technique.

Le controller transmet l'upload au port entrant du domaine. Le stockage, la persistance,
ClamAV et la publication RabbitMQ sont des adaptateurs separes ; une Outbox PostgreSQL est
persistee avant toute publication RabbitMQ.

## Demarrage local

Prerequis : Java 21+, Maven 3.9+, Node.js 22+, Docker Compose.

Sur Apple Silicon, le service ClamAV est execute en `linux/amd64` via l'emulation Docker
Desktop, car l'image officielle utilisee ne publie pas de variante `linux/arm64`.

Le backend possede son entree REST et le wiring des ports sortants. Les commandes
ci-dessous demarrent l'environnement local necessaire au flux complet.

1. Copier `.env.example` vers `.env` et adapter les secrets locaux.
2. Demarrer les dependances :

   ```bash
   docker compose up -d --wait postgres minio rabbitmq clamav
   ```

   PostgreSQL reste sur le port `5432` dans le conteneur et est expose sur le port
   hote `5433` par defaut afin d'eviter les collisions avec une instance PostgreSQL
   deja installee sur macOS. Le port hote peut etre change avec `POSTGRES_HOST_PORT`,
   en alignant alors `DATABASE_URL`.

3. Lancer l'API :

   ```bash
   SPRING_PROFILES_ACTIVE=local mvn -f backend/pom.xml spring-boot:run
   ```

   Le profil `local` conserve uniquement les reglages de developpement de l'authentification
   (cookie non securise et cle ephemere). Toute route protegee exige une session JWT active ;
   il faut donc creer un compte et se connecter avant d'utiliser les fichiers.

4. Dans un autre terminal, lancer la console :

   ```bash
   cd frontend
   npm install
   npm run dev
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

### Alias de lancement

Depuis la racine du depot :

```bash
make front
make back
```

`make front` lance Vite depuis `frontend/` et `make back` lance Spring Boot depuis `backend/`.

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
curl -b cookies.txt -F "file=@./document.pdf" http://localhost:8080/api/v1/files
curl -b cookies.txt http://localhost:8080/api/v1/files
curl -b cookies.txt -OJ http://localhost:8080/api/v1/files/<id>/content
curl -b cookies.txt -X POST http://localhost:8080/api/v1/auth/logout
```

En production, appeler d'abord `GET /api/v1/auth/csrf` et transmettre le cookie CSRF dans
les requetes `POST`. Le telechargement ne devient possible qu'apres le passage a `CLEAN`.
Pour tester un fichier detecte, utiliser le fichier EICAR de test dans un environnement
isole, jamais un malware reel.

## Decisions de securite et de capacite

- **Fail closed** : `PENDING_SCAN`, `SCANNING`, `INFECTED` et `SCAN_FAILED` sont tous non telechargeables.
- **Flux** : l'upload, l'analyse ClamAV `INSTREAM` et le download utilisent des flux ; la limite multipart borne les abus HTTP.
- **Integrite** : un SHA-256 est calcule au depot et conserve en base pour verifier un objet avant scan. La taille et la version canonique MinIO sont relues apres l'ecriture; une divergence bloque le fichier avant `PENDING_SCAN`.
- **Codes de defaillance** : les codes stables sont centralises dans le domaine et reproduits dans la console pour afficher un diagnostic securise sans exposer une exception brute.
- **Noms** : le chemin fourni par le client est reduit a un nom de fichier, sans traversal.
- **Observabilite** : Actuator expose le healthcheck ; les metriques de latence, taille, statut de scan et taux d'erreur sont a ajouter avant production.
- **Acces** : les sessions JWT sont liees a une session persistante et revoquees au logout ;
   une autorisation par tenant, des quotas et un fournisseur d'identite externe restent a
   evaluer avant exposition publique.

## Suite recommandee

1. Remplacer MinIO local par un stockage objet prive de production avec URLs signees emises uniquement apres validation.
2. Ajouter authentification, quotas par tenant, rate limiting et antivirus redondant.
3. Durcir la politique de retry et de dead-letter avec une reconciliation et un nettoyage des objets orphelins.
4. Ajouter quotas par tenant, rate limiting, observabilite et contrats d'exploitation des
   adaptateurs ; aucune regle metier ne doit migrer dans ces composants.
5. Ajouter retention, suppression, chiffrement au repos et audit des acces.

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

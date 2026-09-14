# SecureFiles

Micro-service de depot et de distribution de fichiers securises, realise pour un exercice Java / Spring Boot / React / PostgreSQL.

## Etat du scaffold

Le backend et le frontend ont ete volontairement remis a zero pour permettre une reconstruction manuelle :

- `backend/src/` est vide ;
- `frontend/src/` est vide ;
- les manifestes, regles, skills, workflows et fichiers d'infrastructure sont conserves comme points de depart ;
- aucune implementation metier ou entree d'application n'est actuellement presente.

Les commandes `mvn test`, `mvn spring-boot:run`, `npm run build` et `npm run dev` redeviendront executables apres l'ajout des premiers fichiers sources.

## Analyse du sujet

Le risque principal n'est pas l'upload : c'est de laisser un chemin de telechargement contourner l'analyse antivirus. Le service applique donc une machine d'etats fermee :

```text
PENDING_SCAN -> SCANNING -> CLEAN
                         -> INFECTED
                         -> SCAN_FAILED
```

Un fichier nouvellement recu est depose dans un espace de quarantaine, puis analyse par ClamAV. Tant que le resultat n'est pas `CLEAN`, l'API de contenu renvoie `409 Conflict`. Les erreurs de connexion ou de protocole antivirus sont traitees comme un refus, jamais comme un fichier sain.

Les octets sont conserves sur un volume de fichiers et non dans PostgreSQL. PostgreSQL contient l'identifiant, le nom original nettoye, le type MIME declare, la taille, le hash SHA-256, le statut et les dates. Cette separation evite de faire grossir les lignes SQL et permet de remplacer le volume local par S3/MinIO sans changer le domaine.

## Structure

```text
SecureFiles/
├── backend/
│   ├── pom.xml              # dependances et build Maven
│   └── src/                 # a reconstruire
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
├── docker-compose.yml       # PostgreSQL et ClamAV locaux
└── README.md
```

## Contrat HTTP initial

- `POST /api/v1/files` : recoit un champ multipart `file`, renvoie `202 Accepted` et les metadonnees en `PENDING_SCAN`.
- `GET /api/v1/files` : liste les fichiers et leurs statuts.
- `GET /api/v1/files/{id}` : lit les metadonnees d'un fichier.
- `GET /api/v1/files/{id}/content` : streame le contenu uniquement si le statut est `CLEAN`.
- `GET /actuator/health` : healthcheck technique.

Le scan est declenche par un ordonnanceur Spring qui reserve les fichiers en base avant de les transmettre a ClamAV. La reservation conditionnelle limite les doubles scans lors d'une execution multi-instance ; une file de messages (SQS/RabbitMQ/Kafka) pourra remplacer l'ordonnanceur pour une charge plus importante.

## Demarrage local

Prerequis : Java 21+, Maven 3.9+, Node.js 22+, Docker Compose.

Le scaffold actuel ne demarre pas encore d'API ou de console : il ne contient volontairement plus de code dans `backend/src` et `frontend/src`. Les commandes ci-dessous decrivent l'environnement cible une fois les points d'entree recréés.

1. Copier `.env.example` vers `.env` et adapter les secrets locaux.
2. Demarrer les dependances :

   ```bash
   docker compose up -d postgres clamav
   ```

3. Lancer l'API :

   ```bash
   cd backend
   mvn spring-boot:run
   ```

4. Dans un autre terminal, lancer la console :

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

La console est disponible sur `http://localhost:5173` et l'API sur `http://localhost:8080`.

### Alias de lancement

Depuis la racine du depot :

```bash
make front
make back
```

`make front` lance Vite depuis `frontend/` et `make back` lance Spring Boot depuis `backend/`.

## Test rapide de l'API

```bash
curl -F "file=@./document.pdf" http://localhost:8080/api/v1/files
curl http://localhost:8080/api/v1/files
curl -OJ http://localhost:8080/api/v1/files/<id>/content
```

Le dernier appel ne devient possible qu'apres le passage a `CLEAN`. Pour tester un fichier detecte, utiliser le fichier EICAR de test dans un environnement isole, jamais un malware reel.

## Decisions de securite et de capacite

- **Fail closed** : `PENDING_SCAN`, `SCANNING`, `INFECTED` et `SCAN_FAILED` sont tous non telechargeables.
- **Flux** : l'upload, l'analyse ClamAV `INSTREAM` et le download utilisent des flux ; la limite multipart borne les abus HTTP.
- **Integrite** : un SHA-256 est calcule au depot et expose dans la base pour preparer une verification d'integrite.
- **Noms** : le chemin fourni par le client est reduit a un nom de fichier, sans traversal.
- **Observabilite** : Actuator expose le healthcheck ; les metriques de latence, taille, statut de scan et taux d'erreur sont a ajouter avant production.
- **Acces** : cette premiere tranche ne branche pas encore l'authentification utilisateur. Un fournisseur OIDC/JWT et une autorisation par tenant sont obligatoires avant exposition publique.

## Suite recommandee

1. Remplacer le volume local par un stockage objet prive avec URLs signees emises uniquement apres validation.
2. Ajouter authentification, quotas par tenant, rate limiting et antivirus redondant.
3. Externaliser les scans vers une queue durable et ajouter une politique de retry avec dead-letter queue.
4. Ajouter tests d'integration PostgreSQL/ClamAV avec Testcontainers et tests de charge sur gros fichiers.
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

# SecureFiles - instructions de travail

## Perimetre

SecureFiles est un micro-service de depot et de distribution de fichiers qui combine :

- `backend/` : API Java 21 / Spring Boot / PostgreSQL ;
- `frontend/` : console React / TypeScript / Vite ;
- ClamAV : analyse antivirus des octets avant toute distribution.

## Invariants de securite

- Un transfert incomplet peut etre represente par `UPLOADING`, mais il n'est jamais
	scannable, telechargeable ni retourne comme upload accepte.
- Un upload complet et valide passe a `PENDING_SCAN` avant toute demande de scan.
- Seul le statut `CLEAN` peut produire une reponse de telechargement.
- Toute erreur de scan est bloquante : le fichier reste indisponible.
- Les octets ne sont pas stockes dans PostgreSQL ; la base conserve les metadonnees et le volume conserve le contenu.
- Les contenus sont transferes en flux et ne doivent pas etre charges integralement en memoire.
- Les noms de fichiers, tailles et types MIME sont valides a la frontiere HTTP.
- Aucun secret, token ou contenu de fichier ne doit apparaitre dans les logs.

## Architecture backend

Respecter la separation suivante :

- `domain/` : modele, ports et implementations pures des cas d'utilisation sans
	dependance Spring ;
- `application/` : orchestration transverse sans reimplementation de la logique metier ;
- `application/dto/` et `application/mapper/` : objets et conversions entre la couche
	application et les autres couches ;
- `application/controller/` : adaptateurs entrants HTTP et traduction des erreurs HTTP ;
- `infrastructure/` : PostgreSQL, stockage local/S3 et adaptateur ClamAV ;
- `domain/` ne depend jamais de Spring, HTTP, JPA, S3, AMQP ou ClamAV.

Ne jamais renvoyer une entite JPA depuis un controleur. Toute evolution de schema doit etre documentee dans `README.md` et rester compatible avec les donnees existantes.

## Lisibilite du code backend

- Les classes de production backend sont des types top-level, places dans leur propre fichier ; les classes imbriquees ne sont pas utilisees pour cacher une implementation.
- Les use cases sont decomposes en methodes courtes nommees par intention metier afin que la methode publique expose clairement l'ordre du workflow.
- Les tests du domaine appellent une classe et une methode de production. Les fakes, stubs,
	spies, implementations anonymes de ports et classes de support dediees aux tests sont interdits.
- `@Mock` est autorise uniquement pour une classe, interface ou port de production dont depend le systeme teste. Le systeme teste est toujours une instance reelle de production et sa methode publique est appelee par le test.
- Un use case dependant de ports sortants peut utiliser `@Mock` pour ces ports de production ;
	les adaptateurs reels restent hors de la strategie de tests de couche.
- La couche `application` n'est pas testee comme couche d'orchestration ; seuls ses mappers
	purs, places sous `application/mapper`, sont testes.
- La couche `infrastructure` n'est pas testee comme couche technique ; aucun test d'adaptateur,
	de protocole ou de persistance n'est ajoute dans cette couche.

## Validation

Depuis `backend/` :

```bash
mvn test
mvn spring-boot:run
```

Apres toute demande ou modification du backend, lancer depuis la racine du depot
`env -u DATABASE_URL -u DATABASE_USERNAME -u DATABASE_PASSWORD make back`. Conserver les
logs jusqu'a l'initialisation complete ou l'echec du processus, verifier le message de
demarrage Spring et, lorsque le serveur est disponible, `GET /actuator/health` doit retourner
`UP`.

Depuis `frontend/` :

```bash
npm install
npm run build
npm run dev
```

Les dependances locales peuvent etre demarrees avec `docker compose up -d postgres clamav`.

## Livraison GitHub

- Les titres, rubriques et textes narratifs des issues et pull requests sont toujours rediges en anglais.
- Conserver tels quels les identifiants de code, statuts API, commandes, noms de branches, messages de commit et libelles produit cites.
- Relire le titre et le corps complets avant toute creation ou mise a jour via GitHub MCP afin d'eviter un contenu mixte francais/anglais.

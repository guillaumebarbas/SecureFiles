# SecureFiles - instructions de travail

## Perimetre

SecureFiles est un micro-service de depot et de distribution de fichiers qui combine :

- `backend/` : API Java 21 / Spring Boot / PostgreSQL ;
- `frontend/` : console React / TypeScript / Vite ;
- ClamAV : analyse antivirus des octets avant toute distribution.

## Invariants de securite

- Un upload est toujours cree avec le statut `PENDING_SCAN`.
- Seul le statut `CLEAN` peut produire une reponse de telechargement.
- Toute erreur de scan est bloquante : le fichier reste indisponible.
- Les octets ne sont pas stockes dans PostgreSQL ; la base conserve les metadonnees et le volume conserve le contenu.
- Les contenus sont transferes en flux et ne doivent pas etre charges integralement en memoire.
- Les noms de fichiers, tailles et types MIME sont valides a la frontiere HTTP.
- Aucun secret, token ou contenu de fichier ne doit apparaitre dans les logs.

## Architecture backend

Respecter la separation suivante :

- `domain/` : modele et ports sans dependance Spring ;
- `application/` : cas d'utilisation et orchestration ;
- `infrastructure/` : PostgreSQL, stockage local/S3 et adaptateur ClamAV ;
- `interfaces/rest/` : DTOs et controleurs HTTP.

Ne jamais renvoyer une entite JPA depuis un controleur. Toute evolution de schema doit etre documentee dans `README.md` et rester compatible avec les donnees existantes.

## Validation

Depuis `backend/` :

```bash
mvn test
mvn spring-boot:run
```

Depuis `frontend/` :

```bash
npm install
npm run build
npm run dev
```

Les dependances locales peuvent etre demarrees avec `docker compose up -d postgres clamav`.

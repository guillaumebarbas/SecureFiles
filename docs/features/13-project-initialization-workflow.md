Feature name: project-initialization-workflow

## Summary

Le Makefile centralise le demarrage local de SecureFiles et prepare les dependances necessaires avant le lancement du frontend ou du backend. Les cibles separees permettent aussi d'initialiser tout le projet avec une seule commande.

## Status

Stable

## Public API / Contracts

- `make init` cree `.env` depuis `.env.example` s'il n'existe pas, demarre PostgreSQL, MinIO, RabbitMQ et ClamAV, compile le backend et installe les dependances frontend.
- `make front` prepare `.env`, installe les dependances frontend puis lance Vite.
- `make back` prepare `.env`, demarre les services Docker, compile le backend puis lance Spring Boot.
- `SPRING_PROFILES_ACTIVE` reste configurable et vaut `local` par defaut.

## Quick usage

```bash
make init
make front
make back
```

Pour utiliser un autre profil Spring :

```bash
SPRING_PROFILES_ACTIVE=prod make back
```

## Design decisions

- Les cibles `init-env`, `init-infra`, `init-backend` et `init-frontend` isolent les preparations et evitent de dupliquer les commandes dans `front` et `back`.
- `init-env` ne remplace jamais un `.env` existant ; les reglages locaux deja adaptes sont conserves.
- Le profil `local` reste le defaut car il autorise la cle JWT ephemere et desactive `Secure` pour le cookie de developpement sur `http://localhost`.
- Les variables `COMPOSE`, `MVN`, `NPM` et `SPRING_PROFILES_ACTIVE` sont surchargeables par l'environnement ou la ligne de commande.

## Tests & validation

- `make init` a demarre les services Docker en bonne sante, compile Maven et termine `npm install` avec succes.
- `make -n init`, `make -n front`, `make -n back` et `make -n SPRING_PROFILES_ACTIVE=prod back` ont valide les sequences sans lancer de serveur longue duree.
- `git diff --cached --check` est passe avant le commit.

## Related files

- [Makefile](../../Makefile)
- [README.md](../../README.md)
- [application-local.yml](../../backend/src/main/resources/application-local.yml)
- [JwtSecurityConfiguration.java](../../backend/src/main/java/com/securefiles/config/JwtSecurityConfiguration.java)

## Changelog

- 2026-09-18 - Added centralized local initialization and launch targets.
- 2026-09-18 - Aligned `make init` with the README local startup preparation.

## Notes

- Commit: `34cc780` (`feat(workflow): initialize local project before launch`).
- The local prompt journal and `excalidraw.log` are not part of the delivery.
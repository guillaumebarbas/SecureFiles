Feature name: scan-retry-reliability

## Summary

Fiabilise le traitement antivirus asynchrone afin qu'une reservation concurrente, une lease expiree, une publication RabbitMQ non confirmee ou un scan qui ne progresse plus ne laisse pas un fichier bloque silencieusement en `PENDING_SCAN`. Le resultat terminal conserve le code d'epuisement et les diagnostics detailles restent reserves a la fiche proprietaire.

## Status

Stable

## Public API / Contracts

- Les messages de scan non reservables restent reessayables et les leases `SCANNING` expirees peuvent etre recuperees.
- La publication Outbox n'est marquee comme publiee qu'apres confirmation du broker RabbitMQ.
- L'Outbox publie `securefiles-file-id`; a la limite DLQ, cette correlation permet de conclure un `PENDING_SCAN` en `SCAN_FAILED` meme lorsque le JSON est indecodable. Un message non correlable est durablement publie dans la queue poison avant son acquittement.
- Un scan est borne par `CLAMAV_SCAN_TIMEOUT` sur la connexion, le transfert et la reponse; son expiration reste fail closed avec `CLAMAV_SCAN_TIMEOUT`.
- `GET /api/v1/files/{id}` peut exposer `failureCode` et `failureCause` a son proprietaire. La liste publique force ces deux diagnostics a `null`.
- `SCAN_ATTEMPTS_EXHAUSTED` est le code terminal stable lorsque la limite de tentatives est atteinte.

## Quick usage

- Configurer la duree de lease, le nombre maximal de tentatives et le delai de retry avec les proprietes `SCAN_LEASE_DURATION`, `SCAN_MAX_ATTEMPTS` et `SCAN_RETRY_DELAY`.
- Configurer le delai de confirmation RabbitMQ avec `RABBITMQ_CONFIRM_TIMEOUT_MILLIS`.
- Configurer le budget complet d'analyse avec `CLAMAV_SCAN_TIMEOUT` et conserver une valeur inferieure a `SCAN_LEASE_DURATION`.
- Executer `mvn test` depuis `backend/`, puis `npm test -- --run` et `npm run build` depuis `frontend/`.

## Design decisions

- Le statut courant est retourne lorsqu'une reservation conditionnelle echoue, afin que le worker puisse distinguer un traitement deja en cours d'un resultat absent.
- Les messages `PENDING_SCAN` et `SCANNING` ne sont pas acquittes prematurement ; le retry RabbitMQ est aligne sur le delai metier.
- La correlation de fichier est portee par l'en-tete RabbitMQ afin qu'un corps invalide ne masque pas un echec terminal. L'absence de correlation est traitee comme un poison durable et ne boucle pas dans la DLQ.
- La cause technique precise est separee du code fonctionnel terminal pour fournir un diagnostic stable sans exposer d'exception brute.
- Le frontend n'expose aucun diagnostic de scan dans la liste publique et ne propose pas de telechargement pour un statut different de `CLEAN`.

## Tests & validation

- Backend : `mvn test` reussit avec 79 tests et 10 tests ignores ; le test cible `ScanFileUseCaseTest` reussit avec 8 tests.
- Contrat metadata backend : 18 tests cibles reussissent pour le mapper, la metadata detaillee et la liste.
- Frontend : 89 tests reussissent ; les tests Dashboard cibles reussissent avec 16 tests ; `npm run build` reussit.
- Le test d'integration du scan a ete tente precedemment mais bloque par une base PostgreSQL existante contenant deja la relation `app_user` ; ce resultat n'est pas presente comme un succes.

## Related files

- [ScanFileUseCase.java](../../backend/src/main/java/com/securefiles/domain/file/usecases/ScanFileUseCase.java)
- [RabbitMqScanListener.java](../../backend/src/main/java/com/securefiles/infrastructure/rabbitmq/RabbitMqScanListener.java)
- [OutboxRabbitMqRelay.java](../../backend/src/main/java/com/securefiles/infrastructure/rabbitmq/OutboxRabbitMqRelay.java)
- [FileMetadataResponseDto.java](../../backend/src/main/java/com/securefiles/application/dto/FileMetadataResponseDto.java)
- [filesApi.ts](../../frontend/src/api/filesApi.ts)
- [DashboardPage.tsx](../../frontend/src/pages/Dashboard/DashboardPage.tsx)
- [README.md](../../README.md)

## Changelog

- 2026-09-18 — fiabilisation des retries de scan, confirmation Outbox/RabbitMQ et exposition des causes d'echec.
- 2026-09-19 — ajout du budget global ClamAV, de la correlation DLQ par en-tete et de la queue poison durable.

## Notes

- Merge-base observe : `c3a51add813f00449925a960ad0fc437b853bf9d` (`origin/main`).
- La livraison comprend trois commits Conventional Commits sur `fix/scan-retry-reliability`.
- En multi-instance, les JVM doivent partager la meme base, le meme stockage, la meme queue et des parametres de scan compatibles.

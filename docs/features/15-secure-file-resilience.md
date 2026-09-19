Feature name: secure-file-resilience

## Summary

Renforce le flux de fichiers pour qu'un traitement interrompu, une publication
RabbitMQ non confirmee, un scan ClamAV bloque ou une suppression partielle ne
laisse pas un fichier dans un etat ambigu. Le contrat de liste publique est
egalement reduit aux metadonnees necessaires, tandis que les diagnostics precis
restent reserves a la fiche autorisee.

## Status

Stable

## Public API / Contracts

- `GET /api/v1/files` borne l'offset, expose `clientContentType` et masque
  `failureCode` et `failureCause` dans la liste publique.
- `GET /api/v1/files/{id}` conserve les diagnostics precis pour le proprietaire
  autorise.
- `POST /api/v1/files` reserve le quota avant `PENDING_SCAN` et reste soumis au
  rate limiting d'upload.
- `POST /api/v1/auth/login` applique un rate limiting par adresse IP et renvoie
  `429` ou `503` avec un code stable selon le cas.
- `DELETING`, les reapers de maintenance, les leases de scan et le redrive DLQ
  sont des mecanismes internes; aucun statut non terminal ne rend un fichier
  telechargeable.

## Quick usage

- Configurer le budget complet d'analyse avec `CLAMAV_SCAN_TIMEOUT` et le garder
  inferieur a `SCAN_LEASE_DURATION`.
- Configurer `RABBITMQ_MAXIMUM_TRANSPORT_RETRIES`,
  `RABBITMQ_MAXIMUM_DEAD_LETTER_REDRIVES`, les quotas et les fenetres de rate
  limiting dans l'environnement d'execution.
- Executer `mvn test` depuis `backend/`, puis `npm test -- --run` et
  `npm run build` depuis `frontend/`.

## Design decisions

- Un timeout ClamAV, une panne de broker ou une reponse inconnue reste fail
  closed et ne peut jamais produire `CLEAN`.
- Les leases, la correlation `securefiles-file-id` et la queue poison evitent
  les acquittements ambigus et bornent les redeliveries.
- La suppression revendique `DELETING` avant l'effacement; les reapers
  reprennent les uploads abandonnes, les suppressions et les scans expires.
- Les quotas sont reserves atomiquement par proprietaire et les buckets de rate
  limiting sont persistants dans PostgreSQL.

## Tests & validation

- `backend/` : `mvn test` passe avec 104 tests; 15 scenarios d'integration sont
  ignores par defaut.
- `frontend/` : `npm test -- --run` passe avec 106 tests et `npm run build`
  termine avec succes.
- `git diff --check` termine avec succes.

## Related files

- [README.md](../../README.md)
- [ClamAvAntivirusScanner.java](../../backend/src/main/java/com/securefiles/infrastructure/clamav/ClamAvAntivirusScanner.java)
- [OutboxRabbitMqRelay.java](../../backend/src/main/java/com/securefiles/infrastructure/rabbitmq/OutboxRabbitMqRelay.java)
- [RabbitMqDeadLetterListener.java](../../backend/src/main/java/com/securefiles/infrastructure/rabbitmq/RabbitMqDeadLetterListener.java)
- [StoredFileJpaRepository.java](../../backend/src/main/java/com/securefiles/infrastructure/repository/jpa/StoredFileJpaRepository.java)
- [FailScanUseCase.java](../../backend/src/main/java/com/securefiles/domain/file/usecases/FailScanUseCase.java)
- [DashboardPage.tsx](../../frontend/src/pages/Dashboard/DashboardPage.tsx)

## Changelog

- 2026-09-19 - Added bounded scan, broker, deletion, quota and rate-limit
  recovery with a reduced public metadata surface.

## Notes

- Les scenarios d'integration dependent des services locaux PostgreSQL, MinIO,
  RabbitMQ et ClamAV et restent separes de la suite autonome.
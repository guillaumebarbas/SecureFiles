# Strategie de test

## Objectif

Les tests doivent proteger les invariants de SecureFiles, en particulier l'impossibilite de telecharger un fichier avant un statut `CLEAN`, le refus ferme en cas de panne antivirus et le transfert en flux.

## Backend testing guidelines

### Domain Module Testing Guidelines

- Tests call only public APIs of production classes and must not copy, reimplement or bypass business rules.
- Fakes, stubs, spies, anonymous port implementations, `TestState` and classes created solely as test support are forbidden.
- `@Mock` is allowed only for a production class, interface or port that is a dependency of the production system under test. The system under test must be a real production instance and the test must invoke its public method.
- A production type with no external dependency is tested directly with production and JDK types only.
- A use case that depends on outgoing ports may mock those production ports with `@Mock`. Integration tests with real adapters validate the protocol and persistence concerns separately.
- Tests follow Given-When-Then structure and assert observable behavior.

### Test structure (Given-When-Then)

```java
@Test
void completeUpload_shouldReturnPendingScan_whenMetadataIsComplete() {
	// Given
	var uploadingFile = StoredFile.startUpload(/* production arguments */);

	// When
	var result = uploadingFile.completeUpload(/* production arguments */);

	// Then
	assertThat(result.status()).isEqualTo(PENDING_SCAN);
}
```

### Application and infrastructure layer guidelines

- Application : ne pas tester les handlers, services, coordinations ou frameworks de la
  couche. Tester uniquement les mappers purs sous `application/mapper`.
- Infrastructure : ne pas tester les repositories, adaptateurs, protocoles, migrations,
  stockages, brokers ou clients externes de la couche.
- Les conversions pures exceptionnellement presentes dans une couche peuvent etre
  testees par leur mapper public, sans fake, stub, spy ou classe de support.

### SecureFiles coverage by layer

- Domaine : viser 100 % de couverture des lignes et des branches utiles ; tester chaque transition d'etat, chaque invariant, chaque cas limite et chaque refus de telechargement.
- Application : ne pas ajouter de tests de couche ; seuls les mappers purs sont testes.
- Infrastructure : ne pas ajouter de tests de couche, meme pour les adaptateurs techniques.
- Application/controller : ne pas ajouter de test de couche, de test HTTP ou de test
	Spring. Seuls les mappers purs de `application/mapper` sont testes. Frontend : tester
	upload, rafraichissement, etats visibles et absence de download pour tout statut
	different de `CLEAN`.
- Si un test de domaine exige un port sortant, utiliser `@Mock` uniquement sur ce port de production et appeler le use case reel. Ne jamais creer de fake, stub, spy ou classe auxiliaire.

## Nommage et structure

- Nommer les tests Java selon `methodUnderTest_shouldExpectedBehavior_whenCondition`.
- Exemples : `canDownload_shouldReturnFalse_whenFileIsPendingScan`, `scan_shouldMarkFileClean_whenAntivirusReturnsOk`.
- Un test doit suivre Arrange, Act, Assert et verifier un comportement observable.
- Preferer plusieurs tests courts a un test parametrable illisible. Les donnees de test doivent rendre le scenario evident.
- Les tests sont deterministes, independants, repetables et executables sans ordre impose.
- Ne pas verifier des details d'implementation qui ne font pas partie du contrat.

## Definition de fini

- Un changement du domaine maintient la cible de couverture a 100 %.
- Un changement d'orchestration dans `application` est valide par compilation et par les
	tests des mappers concernes ; aucun test de couche n'est ajoute.
- Un changement d'adaptateur dans `infrastructure` est valide par compilation et revue
	de contrat ; aucun test de couche n'est ajoute.
- La commande de validation la plus ciblee est executee avant toute elargissement du perimetre.
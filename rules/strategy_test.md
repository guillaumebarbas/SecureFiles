# Strategie de test

## Objectif

Les tests doivent proteger les invariants de SecureFiles, en particulier l'impossibilite de telecharger un fichier avant un statut `CLEAN`, le refus ferme en cas de panne antivirus et le transfert en flux.

## Backend testing guidelines

### Domain Module Testing Guidelines

- Entry point: tests must drive the system through a use-case handler, the primary port implementation.
- Dependency Inversion: tests must depend only on abstractions and ports, never on concrete technical implementations.
- No mocking frameworks in domain tests: provide lightweight, in-process fakes for secondary ports.
- Fakes must implement a `TestState<T, ID>` interface so tests can seed initial state.
- Tests follow Given-When-Then structure and assert observable behavior.

### TestState contract

```java
public interface TestState<T, ID> {
	void add(T item);
	Optional<T> find(ID id);
	List<T> findAll();
}
```

### Fake repository example

```java
public class FakeOrderRepository implements OrderRepository, TestState<Order, String> {
	private final List<Order> store = new ArrayList<>();

	@Override
	public void add(Order item) {
		store.removeIf(o -> o.id().equals(item.id()));
		store.add(item);
	}

	@Override
	public Optional<Order> find(String id) {
		return store.stream().filter(o -> o.id().equals(id)).findFirst();
	}

	@Override
	public List<Order> findAll() { return List.copyOf(store); }
}
```

### Test structure (Given-When-Then)

```java
@Test
void givenValidBasket_whenCreateOrder_thenOrderCreatedAndPaymentRequested() {
	// Given
	fixture.state1().add(/* pre-existing entity */);

	// When
	var result = fixture.useCase().create(/* command */);

	// Then
	assertThat(result).isSuccessful();
	assertThat(fixture.state1().findAll()).hasSize(1);
}
```

### API Testing Guidelines (Application Module)

- Contract-driven: assert response shape and status against the documented API contract.
- Use mocked use-cases and deterministic fakes or WireMock for fast, deterministic tests of external interactions.
- Use RestAssured, MockMvc or another lightweight HTTP client as the test driver.

### Infrastructure Testing Guidelines

- Use Testcontainers for all driven adapters when the real dependency is available as a container.
- Validate Flyway or Liquibase migrations against a clean database started by Testcontainers.
- Verify protocol boundaries, stream limits, connection failures, unexpected antivirus responses and persistence mappings.

```java
@Testcontainers
class UserRepositoryIT {
	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
		.withDatabaseName("buvette_test")
		.withUsername("test")
		.withPassword("test");

	@Test
	void savesAndLoadsUser() {
		// your test
	}
}
```

### SecureFiles coverage by layer

- Domaine : viser 100 % de couverture des lignes et des branches utiles ; tester chaque transition d'etat, chaque invariant, chaque cas limite et chaque refus de telechargement.
- Application : verifier les interactions importantes, notamment la reservation unique, l'ordre stockage puis scan, les changements de statut et l'ouverture du flux uniquement apres `CLEAN`.
- Infrastructure : tester PostgreSQL, le stockage local et ClamAV contre leurs vraies dependances ; ne pas se limiter a mocker la bibliotheque testee.
- REST et frontend : tester les DTO, validations HTTP, codes de statut, etats visibles, upload, rafraichissement et absence de download pour tout statut different de `CLEAN`.
- Si un test de domaine exige Spring, JPA, HTTP, ClamAV ou un mock d'adaptateur, arreter et reexaminer la responsabilite testee.

## Nommage et structure

- Nommer les tests Java selon `methodUnderTest_shouldExpectedBehavior_whenCondition`.
- Exemples : `canDownload_shouldReturnFalse_whenFileIsPendingScan`, `scan_shouldMarkFileClean_whenAntivirusReturnsOk`.
- Un test doit suivre Arrange, Act, Assert et verifier un comportement observable.
- Preferer plusieurs tests courts a un test parametrable illisible. Les donnees de test doivent rendre le scenario evident.
- Les tests sont deterministes, independants, repetables et executables sans ordre impose.
- Ne pas verifier des details d'implementation qui ne font pas partie du contrat.

## Definition de fini

- Un changement du domaine maintient la cible de couverture a 100 %.
- Un changement d'orchestration ajoute les doubles de ports necessaires et verifie les interactions de securite.
- Un changement d'adaptateur ajoute ou met a jour un test de contrat ou d'integration.
- La commande de validation la plus ciblee est executee avant toute elargissement du perimetre.
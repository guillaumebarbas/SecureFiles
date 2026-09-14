# Resume des prompts

Chaque demande traitee doit etre ajoutee a la fin de ce fichier avec le format suivant. Ne pas supprimer les blocs precedents. Ne jamais y inscrire de secret, token, contenu de fichier ou donnee sensible.

- - - - -
Prompt :
Peux me faire un alias pour lancer le front, et un autre alias pour lancer le back.

Ajoute ceci dans les regles du front :

Aussi tout les fichiers tests dans le front doivent etre dans src/tests avec une aborsrecence symetrique au src.

Et aussi Ajouter le fait que chaque composant, page doit avoir son fichier style.ts.
Resultat : Alias projet `make front` et `make back` ajoutes, tests frontend deplaces sous `src/tests` avec une arborescence miroir, et regles `style.ts` et d'organisation des tests ajoutees aux instructions frontend.
- - - - -
Prompt :
J'aimerai ajouté dans les regles de l'agent d'écrire dans un fichier resume.md le résumé des prompts dans ce format :


- - - - -
Prompt :
Model :
Nombre de crédit utilisé :
Resultat :
- - - - -
Resultat : Regle ajoutee aux deux agents et fichier resume.md cree avec le format demande. Le modele n'etait pas expose par la session.
- - - - -

- - - - -
Prompt :
Je vois que dans le resume tu as "Model github copilot" et crédit non disponible pourquoi ?

Trouve un moyen de corrigé ca
Resultat : Verification du store local et du journal de session : aucun modele ni usage n'est expose. Regles corrigees pour ne plus deduire ces valeurs.
- - - - -

- - - - -
Prompt :
J'aimerai que dans Prompt, il y est le prompt réélement utilisé non pas un résumé.

Ensuite dans ux_uid.md j'aimerai juste les regles de design , animation, palette de couleur, style general, border radius etc
Resultat : Le champ Prompt est maintenant verbatim et ux_ui.md contient uniquement les regles de design visuel.
- - - - -

- - - - -
Prompt :
Peux tu recuperé le skill tdd du lien de toute à l'heure, et de dire à l'agent de toujours l'utilisé dans l'implémentation de code que ce soit pour le front et pour le back.

Dans ux_ui.md soit plus précis sur la palette de couleur, les degradés quand mettre des dégradés. Les types d'animation (hover, au clic).
Rajouter aussi une regle que chaque bouton/elements avec une action doit avoir un tooltip au hover.
Resultat : Skill TDD adapte ajoute et rendu obligatoire pour le frontend et le backend ; regles de palette, degrades, animations et tooltips precisees dans ux_ui.md.
- - - - -

- - - - -
Prompt :
J'ai toujours : Model : Non disponible
Nombre de crédit utilisé : Non disponible

Dans resume.md aurais tu une solution ?

J'aimerai également supprimer le dossier .claude et déplacer
"## Regles frontend

- Utiliser `src/api/filesApi.ts` pour les appels HTTP et Axios, jamais `fetch` directement.
- Garder les etats de scan visibles et explicites.
- Ne proposer une action de download que pour un fichier `CLEAN`.
- Utiliser `StyleSheet` uniquement si le code devient React Native ; pour ce projet web, garder les styles dans `src/styles.css`."

Car cette partie est pas spécifique à l'agent, il faudrait le mettre dans instruction. donc créer le dossier instructions et le fichier adéquat
Resultat : Modele epingle dans l'agent, consommation premium clarifiee, regles frontend deplacees dans .github/instructions et copie .claude supprimee.
- - - - -

- - - - -
Prompt :
Dans ce cas là supprime model et nombre de crédit du resume.md

Ensuite ajoute un mcp lucid-icon. Et ajoute la regle qui faut pour des icones toujours utilisé ceci.
Resultat : Les champs de modele et de credit ont ete retires des metadonnees du journal, le MCP `lucid-icon` lance `lucide-mcp`, et les regles frontend imposent son utilisation pour choisir les icones.
- - - - -

- - - - -
Prompt :
Peux tu etre plus precis sur la typographie, les titres, text basique, grad etc...

J'aimerai ajouter ca pour les regles test du back : # Testing Guidelines

## Domain Module Testing Guidelines

Entry point: tests must drive the system through a use-case handler.

### High-level Principles

- Entry point: tests must drive the system through a use-case handler (the "primary" port implementation).
- Dependency Inversion: tests must depend only on abstraction (interfaces/ports), never on concrete technical implementations.
- No mocking frameworks: provide lightweight, in-process fakes for secondary ports.
- Fakes must implement a `TestState<T>` interface so tests can seed initial state.
- Tests follow Given-When-Then (Behavior Driven) structure.

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

## API Testing Guidelines (Application Module)

- Contract-driven: assert response shape and status against the documented API contract.
- Use mocked use-cases and deterministic fakes (WireMock) for fast, deterministic tests.
- Use RestAssured or lightweight HTTP clients as the test driver.

## Infrastructure Testing Guidelines

- Use Testcontainers for all driven adapters.
- Validate Flyway/Liquibase migrations against a clean DB started by Testcontainers.

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

Et j'aimerai un skill write-feature-resume. Qui lit les commit et changement local de la branch est écrit la feature efféctué : # Feature documentation template

Feature name: <short-name>

## Summary

1–3 sentence description of the feature and why it exists.

## Status

Design / Implementing / Stable / Deprecated

## Public API / Contracts

- Endpoints, messages, DTOs — link to code where possible.

## Quick usage

- Minimal example(s) showing how to use the feature.

## Design decisions

- Decision 1: short explanation and rationale
- Decision 2: short explanation and rationale

## Tests & validation

- Where tests live and key scenarios they cover.

## Related files

- src/… (links to main files)

## Changelog

- YYYY-MM-DD — summary of change

## Notes

- Any other short pointers or references

Ca ppour le java : # Java Coding Guidelines

This document defines the Java coding conventions and best practices for the project. Follow these rules to keep the codebase consistent, readable, and maintainable.

## Formatting

- **Indentation:** 4 spaces per indent level. Do not use tabs.
- **Line length:** Prefer max 120 characters.
- **Braces:** Always use K&R style (opening brace on same line): `if (cond) {`.

Recommended tooling: use `google-java-format` or `spotless` configured in Gradle.

## Naming Conventions

- **Packages:** All lower-case, reverse-domain style: `com.exalt.it.belairdomain`.
- **Classes / Interfaces / Enums:** PascalCase: `OrderService`, `MenuItem`.
- **Methods:** camelCase, verb-based: `calculateTotal()`.
- **Variables / Parameters / Fields:** camelCase: `orderItems`, `totalAmount`.
- **Constants:** UPPER_SNAKE_CASE and `static final`: `DEFAULT_TAX_RATE`.

## Java Records

Use `record` types for concise, immutable data carriers (DTOs, value objects):

```java
public record Money(BigDecimal amount, Currency currency) {
	public Money {
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(currency, "currency must not be null");
	}
}
```

## Constructor Injection

```java
@Service
public class OrderService {
	private final OrderRepository repository;

	public OrderService(OrderRepository repository) {
		this.repository = Objects.requireNonNull(repository);
	}
}
```

## Error Handling

- Use checked exceptions for recoverable conditions.
- Use runtime exceptions for programming errors and unrecoverable states.
- Prefer specific exception types to provide context.

## Null Handling

- Avoid returning `null` from public methods. Prefer `Optional<T>` for optional results.
- Validate public method arguments using `Objects.requireNonNull()`.

## Logging

- Use `org.slf4j.Logger`: `private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class);`
- Never log sensitive information.

## Build & Dependencies

- Use Maven for builds.

## References & Further Reading

- Google Java Style Guide
- Effective Java, Joshua Bloch

J'aimerai aussi avec un context marker tout le temps utilisé par l'agent :
## Core Guidelines
You MUST strictly adhere to the following guidelines:

### CRITICAL : Context Markers
- **ALWAYS** start replies with STARTER_CHARACTER + space (default: 🍀).
- **ALWAYS** Stack emojis, don't replace.
- **ALWAYS** start replies with 🔎 as STARTER_CHARACTER when you are conducting analysis or research, or designing architecture or high-level structures.
- **ALWAYS** start replies with 💻 as STARTER_CHARACTER when you are implementing code.
- **ALWAYS** start replies with 🕵️ as STARTER_CHARACTER when you are reviewing code.
- **ALWAYS** start replies with 📚 as STARTER_CHARACTER when you are documenting code or practices.
- **ALWAYS** start replies with 🏗️ as STARTER_CHARACTER when you are working on improving the AGENTS.md instructions or other agent-related documentation.
- **ALWAYS** start replies with 🔴 as STARTER_CHARACTER when entering a red phase of TDD (writing failing tests).
- **ALWAYS** start replies with 🟢 as STARTER_CHARACTER when entering a green phase of TDD (writing code to make tests pass).
- **ALWAYS** start replies with ⚪ as STARTER_CHARACTER when entering a refactoring phase of TDD (improving code without changing behavior).
Resultat : Regles UX/UI precisees avec echelle typographique, hierarchie de titres, texte courant et gradients ; regles de test backend ajoutees avec `TestState`, fakes, Given-When-Then et Testcontainers ; regles Java, skill `write-feature-resume` et marqueurs de contexte ajoutes.
- - - - -

- - - - -
Prompt :
J'aimerai créer un skills qui prend en compte tout les commit de la branche, qui créer une issue + pull request vers main et qui appel #file:write-feature-resume
Resultat : Skill `feature-issue-pr` ajoute pour analyser tous les commits et changements locaux, appeler `#file:write-feature-resume`, puis creer une issue et une pull request vers `main` avec des conditions d'arret securisees.
- - - - -

- - - - -
Prompt :
J'aimerai un skill ou  un prompt qui dit (améliore comment c'est écrit) : "Analyse ma demande, mon besoin , analyse le code pour trouver le bon endroit , ensuite fait un plan d'implementation, fait moi valider ta reflexetion et le plan d'implémentation , relis le plan d'implementation et voit si tu peux l'améliorer, optimiser. une fois mon accord implemente le plan.

Tu pourrais faire limite appel à un agent spécialisé qui va faire ces taches avec un sub agent qu'est ce que t'en pense ?
Resultat : Prompt `plan-approve-implement` ajoute avec validation obligatoire du plan, auto-relecture avant approbation et implementation TDD apres accord ; sous-agent `SecureFilesPlanner` limite a la lecture et a la recherche pour explorer le code et critiquer le plan.
- - - - -

- - - - -
Prompt :
Tache strictement en lecture seule, sans modification de fichier ni commande shell. Analyse le dépôt SecureFiles pour la demande suivante : construire une première page de démonstration qui affiche successivement des composants réutilisables (barre de recherche avec test visuel de la valeur soumise, bouton primary/secondary/gradient avec icône/texte optionnels et hover translateY, tag avec texte obligatoire et hover, ligne, colonne, tableau générique triable ASC/DESC avec en-tête gris et bordure légère, icône Lucide configurable avec hover bounce léger), dans `frontend/src/shared`, avec tests frontend TDD et une arborescence de parcours utilisateur. La limite de 500 lignes par composant doit être respectée. Vérifie package.json, l’absence ou présence de src/tests, les règles frontend/UX/TDD, et propose une découpe de fichiers, un nom de page et les risques. Retourne exactement : CONTEXTE, CODE CONCERNE, RISQUES ET OPTIONS, PLAN PROPOSE, VALIDATION. Ne code rien, ne lance aucun test/build.
Resultat : Showcase frontend implementee avec SearchBar, Button, Tag, Row, Column, GenericTable, Icon et Tooltip, tests Vitest/Testing Library, page de reference responsive et point d'entree Vite.
- - - - -

- - - - -
Prompt :
/plan-approve-implement

Il faut améliorer les tags, il manque des couleurs de fond + l'animation de couleur au hover.

Maintenant on va vouloir ajouter des nouveaux composant dans la bibliotheque de composants.

Un SideNavBar lateral:
Avec un NavHeader contenant une icone (qui sera un shield pour representé l'application) une petite description.

En dessous de cette sections. Une list de NavItem item qui amene sur une route avec un affiche un composant. Ces navitem doivent avoir une animation avec un tres leger zoom hover et une couleur de fond blanc qui devient tres legerement grisatre.

Et tout en bas la version de l'application + Un état du back end avec bouton indicateur.

Je ferais aussi un composant "Header", qui sera un composant à la racine de App.tsx.

Header contiendra Le titre de l'application et des elements en props mit en colonne.

Ensuite un composant section.
Avec une ligne Avec icone titre, et tout à droite une description. Et son contenue.
Resultat : Tags semantiques avec fonds pleins et transition hover, shell applicatif avec SideNavBar responsive, navigation History native vers la vue Fichiers, Header, Section, et BackendStatus reutilisables avec tests miroirs.
- - - - -

- - - - -
Prompt :
Corriger les tooltips, rendre le Header blanc et horizontal, et imposer l’utilisation de Row/Column pour l’alignement.
Resultat : Les tooltips sont maintenant limites aux controles icon-only ; les boutons, liens, statuts et tris qui affichent deja un texte n’en rendent plus. Le Header utilise une composition Row horizontale avec un fond blanc et un retour a la ligne responsive. NavHeader, NavItem, BackendStatus, SideNavBar et Section utilisent Row/Column pour leurs alignements. Les instructions frontend, le skill frontend et les regles UX/UI ont ete alignees avec ces conventions. Les diagnostics frontend sont propres ; les runners de tests et de build sont restes muets dans cet environnement.
- - - - -

- - - - -
Prompt :
dans le skill feature issu pr, ajout le fait de faire un/des commits de maniere logique avec le conventional commit
Resultat : Le skill `feature-issue-pr` autorise, lorsque le workflow complet est explicitement demande, la creation d’un ou plusieurs commits logiques au format Conventional Commits. Il impose le decoupage par unite coherente, la verification du staging et des secrets, les validations avant commit, le controle des messages et l’interdiction des operations destructives ou du contournement des hooks.
- - - - -

- - - - -
Prompt :
#file:write-feature-resume
Resultat : Documentation de feature ajoutee dans docs/features/securefiles-console-foundation.md a partir des changements staged de la branche, puis entree de journal ajoutee dans resume.md.
- - - - -

- - - - -
Prompt :
#file:feature-issue-pr
Resultat : Serie de commits Conventional Commits creee, branche `feat/Initialisation_composant_atomic` poussee sur `origin`, resume de feature rafraichi et committe ; creation d'issue et de pull request bloquee faute de session GitHub authentifiee dans les outils disponibles.
- - - - -
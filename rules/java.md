# Java Coding Guidelines

Ce document definit les conventions Java et les pratiques de conception applicables au backend SecureFiles. Les regles privilegient un code lisible, testable et compatible avec l'architecture hexagonale du projet.

## Formatting

- **Indentation:** 4 espaces par niveau. Ne pas utiliser de tabulations.
- **Line length:** preferer une longueur maximale de 120 caracteres.
- **Braces:** utiliser le style K&R avec l'accolade ouvrante sur la meme ligne : `if (condition) {`.
- **Formatting tool:** utiliser `google-java-format` ou configurer Spotless pour Maven lorsque le code Java sera reconstruit.

## Naming Conventions

- **Packages:** minuscules, style reverse-domain : `com.securefiles.domain.file.model`.
- **Classes / Interfaces / Enums:** PascalCase : `FileDownload`, `StoredFileRepository`.
- **Methods:** camelCase et oriente verbe : `calculateChecksum()`.
- **Variables / Parameters / Fields:** camelCase : `storedFile`, `scanResult`.
- **Constants:** UPPER_SNAKE_CASE avec `static final` : `DEFAULT_MAX_FILE_SIZE`.

## Java Records

Utiliser les `record` pour les DTO, value objects et donnees immuables concises :

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
    }
}
```

Ne pas utiliser un `record` lorsqu'un objet doit avoir une identite mutable, un cycle de vie JPA ou une logique d'encapsulation qui depasse un simple transport de donnees.

## Constructor Injection

Utiliser l'injection par constructeur pour les services Spring et les adaptateurs. Le domaine reste depourvu de dependance Spring :

```java
@Service
public class FileQueryService {
    private final StoredFileRepository repository;

    public FileQueryService(StoredFileRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }
}
```

## Error Handling

- Utiliser des exceptions verifiees pour les conditions recuperables lorsque le contrat l'exige.
- Utiliser des exceptions runtime pour les erreurs de programmation et les etats impossibles.
- Preferer des types d'exception specifiques qui donnent le contexte utile sans exposer de donnee sensible.
- Ne pas utiliser une exception generique pour controler le flux metier si un type d'erreur dedie est possible.

## Null Handling

- Eviter de retourner `null` depuis les methodes publiques ; preferer `Optional<T>` pour un resultat facultatif.
- Valider les arguments publics avec `Objects.requireNonNull()` ou une validation adaptee a la frontiere HTTP.
- Ne pas utiliser `Optional` comme champ JPA, parametre de methode ou conteneur de collection.

## Logging

Utiliser SLF4J :

```java
private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class);
```

Ne jamais journaliser de secret, token, contenu de fichier, nom de fichier sensible ou donnee permettant de reconstituer une information confidentielle.

## Build & Dependencies

- Utiliser Maven pour les builds et les dependances.
- Preferer les dependances deja presentes dans `backend/pom.xml` ; toute nouvelle dependance doit avoir une justification et un test cible.
- Maintenir la separation `domain`, `application`, `infrastructure` et `interfaces/rest` ; ne pas introduire Spring, JPA ou HTTP dans le domaine pur.

## References & Further Reading

- Google Java Style Guide
- Effective Java, Joshua Bloch
---
name: java-to-kotlin
description: Trinnvis Java-til-Kotlin-migrering med rammeverk-bevisste transformasjoner for Spring, Ktor og Nav-mønstre
license: MIT
compatibility: Java project migrating to Kotlin
metadata:
  domain: backend
  tags: kotlin java migration refactoring ktor spring
---

# Java to Kotlin Migration

Systematisk konvertering av Java-kodebaser til idiomatisk Kotlin, med rammeverk-bevisste transformasjoner for Nav-tjenester. Dekker hele reisen fra tro oversettelse via nullability-revisjon, collection-migrering og idiomatiske transformasjoner — pluss rammeverksspesifikke mønstre for Spring→Ktor, JPA→Kotliquery, JUnit→Kotest, og eliminering av Lombok.

## Når skal denne brukes

- Migrere eksisterende Java-tjenester til Kotlin
- Konvertere Java-filer i blandede Java/Kotlin-kodebaser
- Onboarde team fra Java til Kotlin-mønstre
- Planlegge en trinnvis migreringsstrategi for en Nav-tjeneste

## 4-stegs konverteringsmetodikk

### Steg 1: Tro oversettelse

Direkte Java → Kotlin-konvertering som bevarer eksakt oppførsel. Bruk IntelliJs innebygde konverterer som utgangspunkt, fiks deretter kompileringsfeil. Behold alle eksisterende tester grønne. Ingen idiomatiske endringer ennå — korrekthet først.

```java
// Java — before
public class UserService {
    private final UserRepository repository;
    private final Logger log = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User findById(Long id) {
        User user = repository.findById(id);
        if (user == null) {
            throw new NotFoundException("User not found: " + id);
        }
        log.info("Found user: {}", user.getName());
        return user;
    }

    public List<User> findActive() {
        return repository.findAll().stream()
            .filter(u -> u.getStatus().equals("active"))
            .collect(Collectors.toList());
    }
}
```

```kotlin
// Kotlin — Step 1: faithful translation (no idiomatic changes)
class UserService(private val repository: UserRepository) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

    fun findById(id: Long): User {
        val user = repository.findById(id)
        if (user == null) {
            throw NotFoundException("User not found: $id")
        }
        log.info("Found user: {}", user.name)
        return user
    }

    fun findActive(): List<User> {
        return repository.findAll().stream()
            .filter { u -> u.status == "active" }
            .collect(Collectors.toList())
    }
}
```

### Steg 2: Nullability-revisjon

Gå gjennom hver `!` (platform type-assertion) og gjør nullability eksplisitt. Map Java `@Nullable` / `@NotNull` til Kotlin `?` / non-null-typer. Avgjør per tilfelle: `requireNotNull()` vs safe calls vs standardverdier. Fokuser på API-grensene — intern kode får strenge non-null-typer.

```kotlin
// Before — platform types and implicit nullability
fun findById(id: Long): User {
    val user = repository.findById(id)  // returns User! (platform type)
    if (user == null) {
        throw NotFoundException("User not found: $id")
    }
    return user
}

// After — explicit nullability
fun findById(id: Long): User {
    return repository.findById(id)  // returns User? (explicit nullable)
        ?: throw NotFoundException("User not found: $id")
}
```

### Steg 3: Collection- og typemigrering

| Java | Kotlin |
|------|--------|
| `List<T>` (mutable) | `List<T>` (immutable) eller `MutableList<T>` |
| `Optional<T>` | `T?` (nullable) |
| `Stream<T>`-pipeline | Kotlin stdlib collection-operasjoner |
| `Map<K,V>` | `Map<K,V>` / `MutableMap<K,V>` |
| `enum` med metoder | Kotlin `enum` eller `sealed class` |

```kotlin
// Before — Java streams and Optional
fun findActive(): List<User> {
    return repository.findAll().stream()
        .filter { u -> u.status == "active" }
        .collect(Collectors.toList())
}

fun getDisplayName(id: Long): String {
    val user: Optional<User> = repository.findOptional(id)
    return user.map { it.name }.orElse("Unknown")
}

// After — Kotlin collection operations
fun findActive(): List<User> =
    repository.findAll().filter { it.status == "active" }

fun getDisplayName(id: Long): String =
    repository.findById(id)?.name ?: "Unknown"
```

### Steg 4: Idiomatiske transformasjoner

Bruk Kotlin-idiomer: data classes, extension functions, sealed classes, `when`-uttrykk og scope functions der de forbedrer lesbarheten.

```kotlin
// Before — Java-style POJO translated directly
class UserDto(
    private var id: Long,
    private var name: String,
    private var email: String,
    private var status: String
) {
    fun getId() = id
    fun getName() = name
    fun getEmail() = email
    fun getStatus() = status
    // equals, hashCode, toString, copy...
}

// After — Kotlin data class
data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val status: String,
)

// Before — utility class with static methods
class StringUtils {
    companion object {
        fun maskFnr(fnr: String): String =
            if (fnr.length == 11) fnr.take(6) + "*****" else fnr
    }
}
val masked = StringUtils.maskFnr(ident)

// After — extension function
fun String.maskFnr(): String =
    if (length == 11) take(6) + "*****" else this

val masked = ident.maskFnr()

// Before — complex conditional chain
fun categorize(age: Int, status: String): String {
    if (status == "disabled") return "inactive"
    if (age < 18) return "minor"
    if (age < 67) return "working-age"
    return "senior"
}

// After — when expression
fun categorize(age: Int, status: String): String = when {
    status == "disabled" -> "inactive"
    age < 18 -> "minor"
    age < 67 -> "working-age"
    else -> "senior"
}

// Before — builder pattern
val config = Config.builder()
    .setHost("localhost")
    .setPort(8080)
    .setDebug(true)
    .build()

// After — named arguments with defaults
val config = Config(
    host = "localhost",
    port = 8080,
    debug = true,
)
```

## Rammeverk-bevisste konverteringer

### Spring Boot → Ktor

| Spring | Ktor |
|--------|------|
| `@RestController` | `routing { }` med `get/post/put/delete` |
| `@Service` | Vanlig klasse med constructor-injection |
| `@Repository` (JPA) | `using(sessionOf(dataSource))`-mønster |
| `@Autowired` | Constructor-parametre (ingen annotasjoner) |
| `@Configuration` | Sealed class-konfigurasjonsmønster |
| `application.yml` | `System.getenv()` eller `konfig`-biblioteket |

```java
// Spring Boot controller
@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request) {
        UserDto created = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

```kotlin
// Ktor route handler
fun Route.userRoutes(userService: UserService) {
    route("/api/users") {
        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID")
            val user = userService.findById(id)
            call.respond(HttpStatusCode.OK, user)
        }

        post {
            val request = call.receive<CreateUserRequest>()
            val created = userService.create(request)
            call.respond(HttpStatusCode.Created, created)
        }
    }
}
```

### Hibernate/JPA → Kotliquery

```java
// JPA entity + repository
@Entity
@Table(name = "vedtak")
public class VedtakEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String ident;
    private String status;
    private LocalDate fom;
}

public interface VedtakRepository extends CrudRepository<VedtakEntity, Long> {
    List<VedtakEntity> findByIdent(String ident);
}
```

```kotlin
// Kotliquery — data class + repository
data class Vedtak(
    val id: Long,
    val ident: String,
    val status: String,
    val fom: LocalDate,
)

class VedtakRepository(private val dataSource: DataSource) {
    fun findByIdent(ident: String): List<Vedtak> =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf("SELECT * FROM vedtak WHERE ident = ?", ident)
                    .map { row ->
                        Vedtak(
                            id = row.long("id"),
                            ident = row.string("ident"),
                            status = row.string("status"),
                            fom = row.localDate("fom"),
                        )
                    }.asList
            )
        }

    fun save(vedtak: Vedtak): Long =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO vedtak (ident, status, fom) VALUES (?, ?, ?)",
                    vedtak.ident, vedtak.status, vedtak.fom
                ).asUpdateAndReturnGeneratedKey
            ) ?: throw IllegalStateException("Failed to insert vedtak")
        }
}
```

### JUnit → Kotest

```java
// JUnit 5 test
public class UserServiceTest {
    @Mock private UserRepository repository;
    @InjectMocks private UserService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldFindUserById() {
        User user = new User(1L, "Kari");
        when(repository.findById(1L)).thenReturn(user);

        User result = service.findById(1L);

        assertEquals("Kari", result.getName());
        verify(repository).findById(1L);
    }

    @Test
    void shouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> service.findById(99L));
    }
}
```

```kotlin
// Kotest matchers + MockK
class UserServiceTest {
    private val repository = mockk<UserRepository>()
    private val service = UserService(repository)

    @Test
    fun `should find user by id`() {
        val user = User(1L, "Kari")
        every { repository.findById(1L) } returns user

        val result = service.findById(1L)

        result.name shouldBe "Kari"
        verify { repository.findById(1L) }
    }

    @Test
    fun `should throw when not found`() {
        every { repository.findById(99L) } returns null

        shouldThrow<NotFoundException> {
            service.findById(99L)
        }
    }
}
```

### Lombok → Kotlin nativt

| Lombok | Kotlin |
|--------|--------|
| `@Data` | `data class` |
| `@Builder` | Named arguments + standardverdier |
| `@Getter` / `@Setter` | `val` / `var`-properties |
| `@Slf4j` | `KotlinLogging.logger {}` |
| `@AllArgsConstructor` | Primary constructor (Kotlin-standard) |
| `@NoArgsConstructor` | Ikke nødvendig, eller legg til standardverdier |
| `@RequiredArgsConstructor` | Primary constructor med `val`-parametre |

```java
// Lombok
@Data
@Builder
@Slf4j
public class Søknad {
    private final String ident;
    private final LocalDate innsendtDato;
    @Builder.Default
    private String status = "mottatt";

    public void behandle() {
        log.info("Behandler søknad for {}", ident);
        this.status = "behandlet";
    }
}
```

```kotlin
// Kotlin native
private val logger = KotlinLogging.logger {}

data class Søknad(
    val ident: String,
    val innsendtDato: LocalDate,
    var status: String = "mottatt",
) {
    fun behandle() {
        logger.info { "Behandler søknad for $ident" }
        status = "behandlet"
    }
}
```

### Jackson → kotlinx.serialization (valgfritt)

| Jackson | kotlinx.serialization |
|---------|----------------------|
| `@JsonProperty("name")` | `@SerialName("name")` |
| `ObjectMapper()` | `Json { ignoreUnknownKeys = true }` |
| `@JsonIgnore` | `@Transient` |

> **Merk:** Mange Nav-tjenester beholder Jackson med `jackson-module-kotlin` — migrer kun til kotlinx.serialization hvis teamet eksplisitt ønsker det.

## Bevare git-historikk

Todelt rename-strategi for å bevare `git log --follow`:

```bash
# Fase 1: rename fil (ren rename, ingen innholdsendring)
git mv src/main/java/no/nav/MyService.java src/main/kotlin/no/nav/MyService.kt
git commit -m "rename: MyService.java → MyService.kt"

# Fase 2: konverter innhold (i egen commit)
# ... apply Kotlin conversion ...
git commit -m "refactor: convert MyService to idiomatic Kotlin"
```

Dette sikrer at `git log --follow src/main/kotlin/no/nav/MyService.kt` viser hele historikken, inkludert Java-æraen.

## Arbeidsflyt for batch-konvertering

Konverter nedenfra og opp: avhengigheter før de som avhenger av dem. Hold blandede Java/Kotlin-bygg fungerende hele veien. Kjør hele testsuiten etter hver filkonvertering.

**Foreslått rekkefølge:**

1. **Modeller/DTO-er** — data classes, enkle gevinster
2. **Utilities** — extension functions, liten scope
3. **Repositories** — Kotliquery-migrering
4. **Services** — forretningslogikk, kan ha komplisert nullability
5. **Controllers/Routes** — rammeverksmigrering (Spring→Ktor)
6. **Konfigurasjon** — sealed class-mønstre
7. **Tester** — Kotest-migrering (gjøres sist, holder valideringen fungerende)

Innenfor hvert lag: konverter blad-pakker først (ingen interne avhengigheter), jobb deretter innover.

## Vanlige fallgruver

| Fallgruve | Løsning |
|---------|----------|
| Kotlin-nøkkelord som identifikatorer (`when`, `is`, `in`, `object`) | Backtick-escape `` `when` `` eller gi nytt navn |
| SAM-konvertering — Java funksjonelle interfaces konverteres automatisk, Kotlin-interfaces gjør det ikke | Bruk `fun interface` for Kotlin SAM-typer |
| Platform types (`T!`) fra Java uten null-annotasjoner | Avgjør null-strategi eksplisitt — la aldri `!` stå igjen |
| Java static → Kotlin | `companion object` eller top-level-funksjoner |
| `@JvmStatic` / `@JvmField` | Nødvendig hvis Java-kode fortsatt kaller Kotlin `companion object`-medlemmer |
| Checked exceptions | Kotlin har ikke dette — legg til `@Throws` hvis kalt fra Java |
| Property access-syntaks | Java `getX()` blir `x` hos Kotlin-kallere |

## Relatert

| Ressurs | Bruk til |
|----------|---------|
| `kotlin-ktor`-instruksjon | Målmønstre for Ktor-utvikling |
| `kotlin-spring`-instruksjon | Spring Boot Kotlin-mønstre (hvis man blir på Spring) |
| `kotlin-app-config`-skill | Sealed class-konfigurasjonsmønster |
| `spring-boot-scaffold`-skill | Scaffolding av nye Spring Boot-tjenester |
| `flyway-migration`-skill | Databasemigrasjonsmønstre |

## Grenser

### ✅ Alltid

- Bevar git-historikk (todelt rename)
- Kjør tester etter hver filkonvertering
- Konverter nedenfra og opp (avhengigheter før de som avhenger av dem)
- Fiks nullability eksplisitt — la aldri platform types stå igjen
- Hold blandede Java/Kotlin-bygg kompilerende hele veien

### ⚠️ Spør først

- Rammeverksmigrering (Spring → Ktor)
- Endring av testrammeverk (JUnit → Kotest)
- Endringer i byggesystem (Maven → Gradle)
- Bytte serialiseringsbibliotek (Jackson → kotlinx)

### 🚫 Aldri

- Konverter flere filer uten å teste mellom hver
- Undertrykk Kotlin-advarsler med `@Suppress`
- Bruk `!!` uten å verifisere at verdien ikke kan være null
- Endre oppførsel under konvertering — korrekthet først
- Slett Java-filer før Kotlin-erstatningene kompilerer og består tester

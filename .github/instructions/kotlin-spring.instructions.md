---
name: Kotlin Spring Boot Patterns
description: "Spring Boot-mønstre for Nav-backends: controller, service, repository, validering og feilhåndtering."
applyTo: "**/*.kt"
---

Spring Boot-mønstre for `bidrag-backend`: controller, service, repository, validering og feilhåndtering.

> `bidrag-backend` er et Kotlin + Spring Boot + PostgreSQL-monorepo bygget med Maven. Denne instruksjonsfilen forutsetter Spring Boot.

# Spring Boot-rammeverksmønstre

## Controller-laget

```kotlin
@RestController
@RequestMapping("/api")
class ResourceController(
    private val service: ResourceService
) {
    @GetMapping("/resources/{id}")
    fun getResource(@PathVariable id: UUID): ResponseEntity<ResourceDTO> {
        val resource = service.findById(id)
        return ResponseEntity.ok(resource)
    }

    @PostMapping("/resources")
    fun createResource(@RequestBody @Valid request: CreateResourceRequest): ResponseEntity<ResourceDTO> {
        val created = service.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
}
```

## Service-laget

```kotlin
@Service
class ResourceService(
    private val repository: ResourceRepository
) {
    @Transactional
    fun create(request: CreateResourceRequest): ResourceDTO {
        val entity = request.toEntity()
        return repository.save(entity).toDTO()
    }
}
```

## Databasetilgang

Sjekk eksisterende repository-implementasjoner i kodebasen — mønstrene varierer mellom apper:

```kotlin
// Alternativ A: CrudRepository / JpaRepository-grensesnitt
@Repository
interface ResourceRepository : CrudRepository<ResourceEntity, UUID> {
    fun findByIdent(ident: String): List<ResourceEntity>

    @Query("SELECT * FROM resource WHERE status = :status", nativeQuery = true)
    fun findByStatus(status: String): List<ResourceEntity>
}

// Alternativ B: NamedParameterJdbcTemplate (rå SQL)
@Repository
class JdbcResourceRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    fun findById(id: UUID): ResourceEntity? {
        val sql = "SELECT * FROM resource WHERE id = :id"
        return namedParameterJdbcTemplate.query(sql, mapOf("id" to id)) { rs, _ ->
            ResourceEntity(id = rs.getObject("id", UUID::class.java))
        }.firstOrNull()
    }
}
```

## Auth (token-validation-spring)

```kotlin
@ProtectedWithClaims(issuer = "azuread")
@RestController
class ProtectedController {
    @GetMapping("/api/protected")
    fun protectedEndpoint(): ResponseEntity<Any> {
        // Token-validering håndteres automatisk av filteret
        return ResponseEntity.ok(mapOf("status" to "ok"))
    }
}
```

## Konfigurasjon

Bruk `application.yml` / `application-{profile}.yml` for Spring-konfigurasjon:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_DATABASE}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  flyway:
    enabled: true
```

## Strukturert logging

```kotlin
// Sjekk eksisterende logg-setninger i repoet for å følge det etablerte mønsteret
// KotlinLogging (io.github.oshai:kotlin-logging) er standarden i dette repoet
private val logger = KotlinLogging.logger {}

logger.info { "Processing event: eventId=$eventId" }

// PII (fnr, navn, adresse) skal aldri gå til standard-loggeren — bruk secureLogger i stedet
secureLogger.info { "Detaljer for sak $sakId: $sensitivePayload" }
```

## Feilhåndtering (ProblemDetail)

```kotlin
@RestControllerAdvice
class ErrorHandler {
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Ressurs ikke funnet")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validering feilet").apply {
            setProperty("feil", ex.bindingResult.fieldErrors.map {
                mapOf("felt" to it.field, "melding" to it.defaultMessage)
            })
        }
}
```

## Configuration Properties

```kotlin
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val externalApiUrl: String,
    val maxRetries: Int = 3,
    val featureFlags: FeatureFlags = FeatureFlags(),
) {
    data class FeatureFlags(
        val nyFunksjon: Boolean = false,
    )
}

// Aktiver i Application.kt
@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class Application
```

## Testing

Se `testing-kotlin.instructions.md` for teststrategi,
Kotest/MockK-mønstre og eksempler på `@WebMvcTest`/`@DataJpaTest`/`@SpringBootTest`.

Repo-spesifikt å huske på:

- `com.ninja-squad:springmockk` (`@MockkBean`) er allerede en avhengighet — bruk den
  for å mocke Spring-beans, ikke Mockitos `@MockBean`.
- Enkelte apper i dette repoet (f.eks. de som kobler mot Bisys/DB2) låser
  `testcontainers` til 1.x-linjen lokalt i sin `pom.xml` — sjekk appens egen
  `pom.xml` før du antar hvilken Testcontainers-versjon som er tilgjengelig.

## Grenser

### ✅ Alltid
- Bruk constructor injection (ikke field injection)
- Annoter transaksjonsgrenser eksplisitt
- Følg eksisterende repository-mønster i kodebasen — ikke bland stiler
- Bevar eksisterende kodestruktur ved målrettede fikser — ikke gi nytt navn til, restrukturer eller refaktorer fungerende kode utover det oppgaven krever
- Rut PII gjennom `secureLogger`, aldri standard-loggeren

### ⚠️ Spør først
- Innføre nye Spring-moduler eller starters
- Endre transaksjons-isolasjonsnivåer

### 🚫 Aldri
- Bruk field injection (`@Autowired` på felter)
- Bland Spring Data JPA og JDBC i samme repository-lag
- Legg forretningslogikk i controllere

## Relatert

| Type | Navn | Når brukes |
|------|------|-------------|
| Skill | [$java-to-kotlin](../skills/java-to-kotlin/) | Migrere gjenværende Java-moduler til Kotlin |
| Skill | [$spring-boot-scaffold](../skills/spring-boot-scaffold/) | Scaffolde en ny Spring Boot Kotlin-app i `apps/` |
| Skill | [$flyway-migration](../skills/flyway-migration/) | Databasemigrasjonsmønstre |
| Skill | [$code-review](../skills/code-review/) | Repo-spesifikk gjennomgang (pom.xml, sikkerhet, workflows) |
| Instructions | [testing-kotlin.instructions.md](testing-kotlin.instructions.md) | Kotest/MockK-testmønstre, slice- og integrasjonstester |
| Agent | @auth-agent | Oppsett av autentisering (TokenX, Azure AD) |

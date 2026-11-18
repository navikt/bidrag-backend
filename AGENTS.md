# AGENTS.md — bidrag-backend

Team Bidrag's (Nav) backend monorepo for child-support cases. About 35 Spring Boot apps in `apps/`,
shared libraries in `libs/bidrag-felles` and `libs/bidrag-beregn-felles`, and Nais manifests in `.nais/<app>/`.
Apps may have their own `AGENTS.md` (for example `apps/bidrag-belopshistorikk/AGENTS.md`) that supplements this one.

## Tech stack

- Kotlin (some Java), Java 25, Spring Boot 4, Maven
- JPA/Hibernate, Flyway, PostgreSQL, Kafka, Caffeine, `RestTemplate`
- Auth: Azure AD/TokenX through token-support (`@EnableJwtTokenValidation`, `@Protected`, `@ProtectedWithClaims`) and Texas
- Code style: ktlint `intellij_idea` (see `.editorconfig`)
- Domain terms are Norwegian: `stønad`, `vedtak`, `engangsbeløp`, `periode`

## Build and test

Run from the repo root with `-pl`, or from the app directory.

```bash
mvn test -pl apps/<app> -am      # Test one app (and the modules it depends on)
mvn verify -pl apps/<app> -am    # Build, ktlint and tests
```

## Structure

```text
apps/<app>/                # Spring Boot apps
libs/bidrag-felles/        # Shared modules (bidrag-domene, bidrag-transport, feilhåndtering, etc.)
libs/bidrag-beregn-felles/ # Shared calculation logic
.nais/<app>/               # Nais manifests and environment config (q1, q2, prod)
.github/workflows/         # One workflow per app: detect_changes → bygg_og_test → deploy_q1/q2/prod
```

## Conventions

- Keep changes small and focused. Don't rename, restructure or refactor working code outside the task.
- Follow existing patterns in the app you are changing.
- Use guard clauses and `require`/`check`. Prefer `val`, data classes and null-safety over `!!`.
- No magic numbers or strings. Use constants or enums.
- Reuse helpers from `bidrag-commons`/`bidrag-domene` instead of copying them.
- No silent `catch (e: Exception) {}`. Use the shared exceptions in `bidrag-backend-feilhandtering`. Kafka listeners must not swallow errors.
- No commented-out code and no TODO without a ticket.
- Dependency versions are properties in the app's own `pom.xml`, not the root POM.
- Flyway: never modify migrations that have already been applied, and follow the app's version format.

## Security

- No secrets, tokens or passwords in code, `application*.yaml`, tests or `.nais/*.yaml`. Secrets come from Nais/Azure.
- Personal data (fnr, aktørId, names, addresses) goes only to `secureLogger` (team-logs), never to regular logs, exceptions or error responses.
- Never log tokens, headers or full request/response bodies.
- Every endpoint needs `@Protected` or `@ProtectedWithClaims`. `@Unprotected` requires justification. Access to person and case data goes through tilgangskontroll.
- Validate input (`@Valid`, Jakarta constraints) on DTOs and Kafka payloads. Validation messages must not echo input (`${validatedValue}`).
- SQL must use bind parameters, never string concatenation.
- CodeQL fixes: a boolean `require()` or a denylist is not a sanitizer. Derive a new value, for example `Regex(...).matchEntire(value)?.value`.
- GitHub Actions must be pinned to a SHA. Don't use `pull_request_target`.

```kotlin
// ❌ Personal data in the regular log, and SQL built from input
log.info { "Henter stønad for $fnr" }
jdbcTemplate.query("SELECT * FROM stonad WHERE skyldner = '$fnr'", mapper)

// ✅
secureLogger.info { "Henter stønad for $fnr" }
jdbcTemplate.query("SELECT * FROM stonad WHERE skyldner = ?", mapper, fnr)
```

## Performance

- Avoid N+1: don't call a repository or `RestTemplate` inside `map`/`forEach`. Use fetch joins, `@EntityGraph` or batch lookups. No new `FetchType.EAGER` on collections.
- No remote calls or Kafka sends inside long `@Transactional` methods.
- `@Transactional`, `@Cacheable` and `@Retryable` don't apply on self-invocation.
- Large tables need paging or limits.
- Check the key and TTL on `@Cacheable`, and never share user-specific data between users.
- Close streams and responses. Avoid `runBlocking` on request threads.

## Testing

- New logic and bug fixes need tests. Cover null, empty lists, overlapping or open periods (`til = null`) and boundary dates.
- Use Norwegian backtick test names, for example ``fun `skal returnere tom liste når stønad mangler`()``, and Given/When/Then.
- Use Kotest matchers (`shouldBe`), not `assertTrue(x != null)`.
- Mock external services (WireMock/MockK), not domain logic.
- Tests must be independent and deterministic. Don't use `LocalDate.now()` without a fixed clock or input.
- Tools: JUnit 5, Kotest, MockK/mockito-kotlin, WireMock, H2/Testcontainers.

## Boundaries

### ✅ Always

- Run tests after changes
- Validate all external input

### ⚠️ Ask first

- Changing authentication mechanisms
- Adding new dependencies
- Modifying the database schema

### 🚫 Never

- Commit secrets or credentials
- Skip input validation at external boundaries

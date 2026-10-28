# Copilot code review instructions for bidrag-backend

## Project Context

- **What**: Team Bidrag's (Nav) backend monorepo for child-support cases. It has about 35 Spring Boot apps in `apps/`, shared libraries in `libs/bidrag-felles` and `libs/bidrag-beregn-felles`, and Nais manifests in `.nais/<app>/`.
- **Tech stack**: Kotlin (with some Java), Java 25, Spring Boot 4, Maven, JPA/Hibernate, Flyway, PostgreSQL, Kafka, Caffeine cache, and `RestTemplate` clients.
- **Auth**: Azure AD/TokenX through token-support (`@EnableJwtTokenValidation`, `@Protected`, `@ProtectedWithClaims`) and Texas.
- **Testing**: JUnit 5, Kotest matchers (`shouldBe`), MockK/mockito-kotlin, WireMock, H2/Testcontainers. Test names use backticks and Norwegian, for example ``fun `skal returnere tom liste når stønad mangler`()``.
- **Code style**: ktlint `intellij_idea` (see `.editorconfig`). Domain terms are Norwegian (`stønad`, `vedtak`, `engangsbeløp`, `periode`).

## Review Language

Always write review comments in **English**.

## Review Priorities

### 🔴 CRITICAL (Block merge)
- **Security**: exposed secrets, missing auth on endpoints, injection, or access-control bypass for person or case data.
- **PII leaks**: FNR, aktørId, names, or addresses in regular logs, exceptions, or error responses. These may only go to `secureLogger`.
- **Correctness**: wrong period, amount, or date logic, `BigDecimal` rounding and comparison errors, race conditions, or non-idempotent Kafka consumers.
- **Data loss**: destructive Flyway migrations, edits to already-applied migrations, or bulk updates/deletes without a guard.
- **Breaking changes**: REST or Kafka contract changes (DTOs in `bidrag-transport`/`bidrag-domene`) that consumers aren't ready for.

### 🟡 IMPORTANT (Requires discussion)
- **Performance**: N+1 queries, unbounded result sets, or remote calls inside DB transactions.
- **Test coverage**: missing tests for new logic or critical paths, removed assertions, or `@Disabled` without a reason.
- **Architecture**: business logic in controllers or repositories, or deviations from the app's established patterns.
- **Infrastructure**: `.nais` `accessPolicy`/secrets/auth changes, new workflow `permissions`, unpinned actions, or new dependencies.

### 🟢 SUGGESTION (Non-blocking)
- Readability, naming, simplification, magic numbers, documentation, or minor convention deviations.

## General Review Principles

1. **Be specific**: reference exact files and lines, and give concrete examples.
2. **Provide context**: explain *why* something matters and what the impact is.
3. **Suggest solutions**: show corrected Kotlin code, not just the problem.
4. **Be constructive and pragmatic**: not every suggestion must be fixed now.
5. **Recognize good practices**: acknowledge well-written code.
6. **Group related comments**: don't repeat the same point on every line.
7. **Ask** when the intent is unclear, especially in period logic and payment calculations.
8. **Respect scope**: flag unrelated renames, refactors, or reformatting bundled into the PR.

## Security Review
- No hardcoded secrets, tokens, or passwords in code, `application*.yaml`, tests, or `.nais/*.yaml`. Secrets come from Nais/Azure.
- Never log tokens, headers, or full request/response bodies. PII goes only to `secureLogger` (team-logs).
- Every new or changed controller must have `@Protected` or `@ProtectedWithClaims`. Challenge any `@Unprotected`. Person or case access must go through tilgangskontroll.
- Validate input on DTOs (`@Valid`, Jakarta constraints) and Kafka payloads. Validation messages must not echo input (`${validatedValue}`).
- SQL must use bind parameters, never string concatenation in `@Query(nativeQuery = true)` or `JdbcTemplate`.
- For CodeQL fixes, a boolean `require()` or a denylist is not a sanitizer. Derive a new value, for example `Regex(...).matchEntire(value)?.value`.
- GitHub Actions must be pinned to a SHA. Flag `pull_request_target`.

```kotlin
// ❌ BAD: PII in the regular log, and SQL built from input
log.info { "Henter stønad for $fnr" }
jdbcTemplate.query("SELECT * FROM stonad WHERE skyldner = '$fnr'", mapper)

// ✅ GOOD
secureLogger.info { "Henter stønad for $fnr" }
jdbcTemplate.query("SELECT * FROM stonad WHERE skyldner = ?", mapper, fnr)
```

## Performance Considerations
- **N+1**: lazy relations accessed in loops, repository or `RestTemplate` calls inside `map`/`forEach`, or new `FetchType.EAGER` on collections. Suggest fetch joins, `@EntityGraph`, or batch lookups.
- **Transactions**: no remote calls or Kafka sends inside long `@Transactional` methods. `@Transactional`, `@Cacheable`, and `@Retryable` don't apply on self-invocation.
- **Pagination**: large tables need paging or limits.
- **Caching**: check the `@Cacheable` key and TTL, and never share user-specific data across users.
- **Resources**: close streams and responses, and avoid `runBlocking` on request threads.

```kotlin
// ❌ BAD: one query per vedtak
vedtakListe.map { vedtak -> stønadRepository.findByVedtakId(vedtak.id) }

// ✅ GOOD: one query
val stønaderPerVedtak = stønadRepository.findAllByVedtakIdIn(vedtakListe.map { it.id }).groupBy { it.vedtakId }
```

## Code Quality Standards
- Use descriptive names that are consistent with the existing Norwegian domain terms.
- Keep functions small and focused (ideally under 20–30 lines), with nesting no deeper than 3–4 levels. Prefer guard clauses and `require`/`check`.
- No duplication. Reuse helpers from `bidrag-commons`/`bidrag-domene` instead of copying them.
- No magic numbers or strings. Use constants or enums.
- Prefer null-safety over `!!`, and immutable `val` and data classes.
- Error handling: no silent `catch (e: Exception) {}`. Use meaningful messages (without PII) and the shared exceptions in `bidrag-backend-feilhandtering`. Kafka listeners must not swallow errors.
- No commented-out code, and no TODO without a ticket.
- Dependency versions are properties in the app's own `pom.xml`, not the root POM.
- Flyway: never modify applied scripts, and follow the app's existing version format.

## Testing Standards
- New logic and bug fixes need tests. Cover edge cases: null values, empty lists, overlapping or open periods (`til = null`), and boundary dates.
- Use descriptive backtick names, a Given/When/Then structure, and specific assertions (`shouldBe`, not `assertTrue(x != null)`).
- Mock external services (WireMock/MockK), not domain logic. Tests must be independent and deterministic, with no reliance on `LocalDate.now()` without a fixed clock or input.

## Comment Format

```markdown
**[🔴 CRITICAL | 🟡 IMPORTANT | 🟢 SUGGESTION] Category: Brief title**

What the issue is (file and line).

**Why this matters:** impact on users, data, or security.

**Suggested fix:**
<Kotlin code>
```

## Readability

Always suggest changes that improve readability. For example, the suggestion below makes the code more readable, and it also makes the validation reusable and testable:

```kotlin
// Instead of:
if (periode.fom != null && periode.til != null && periode.fom.isBefore(periode.til) && beløp > BigDecimal.ZERO) {
    lagrePeriode(periode, beløp)
} else {
    throw IllegalArgumentException("Ugyldig periode")
}

// Consider:
fun Periode.erGyldig(): Boolean = fom != null && til != null && fom.isBefore(til)

require(periode.erGyldig() && beløp > BigDecimal.ZERO) { "Ugyldig periode" }
lagrePeriode(periode, beløp)
```

Always prioritize security vulnerabilities, PII leaks, and data-correctness issues, which can affect people's child-support payments, and performance issues that could impact users.

# Copilot code review instructions for bidrag-backend

## Project Context

Shared project context, conventions, security, performance and testing rules live in [`AGENTS.md`](../AGENTS.md) at the repo root. Apply them when reviewing. This file only adds review-specific guidance.

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

## Rules to enforce

Enforce the Security, Performance, Code Quality and Testing sections of `AGENTS.md`. Also check:

- Verify authentication on every new or changed endpoint, and flag `@Unprotected` without justification.
- Flag unpinned GitHub Actions and `pull_request_target`.
- A CodeQL fix that only adds a boolean `require()` or a denylist is not a sanitizer. Expect a derived value, for example `Regex(...).matchEntire(value)?.value`.
- Flag N+1 queries, remote calls inside `@Transactional`, and self-invocation of `@Transactional`/`@Cacheable`/`@Retryable`.

## Comment Format

```markdown
**🔴 CRITICAL | 🟡 IMPORTANT | 🟢 SUGGESTION - Category: Brief title**

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

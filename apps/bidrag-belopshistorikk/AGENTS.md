# AGENTS.md — bidrag-belopshistorikk

Spring Boot-tjeneste som behandler beløpshistorikk (stønad og engangsbeløp) i Bidrag.
Konsumerer vedtakshendelser fra Kafka og oppdaterer perioder i PostgreSQL.
Ved nye vedtak erstattes eksisterende perioder — overlappende perioder ugyldiggjøres og restperioder gjenskapes.

## Build & Test Commands

```bash
mvn test              # Run tests
mvn verify            # Build + lint (ktlint) + test
mvn spring-boot:run -Dspring-boot.run.profiles=local  # Run locally
```

## Project Structure

```text
src/main/kotlin/no/nav/bidrag/belopshistorikk/
├── controller/          # REST-endepunkter
├── service/             # Forretningslogikk (BeløpshistorikkService, BehandleHendelseService)
├── hendelse/            # Kafka-consumer (VedtakHendelseListener)
├── persistence/
│   ├── entity/          # JPA-entiteter (Stønad, Periode, Engangsbeløp)
│   └── repository/      # Spring Data repositories
├── bo/                  # Business objects
├── konfig/              # Kafka, cache, etc.
├── aop/                 # Correlation ID aspect
└── exception/           # Feilhåndtering
src/main/resources/
├── db/migration/        # Flyway SQL-migrasjoner
└── application.yaml
.nais/                   # Nais-manifest og miljøconfig (q1, q2, prod)
```

## Code Style

### Minimal Editing

When fixing a bug or implementing a feature, change only what is necessary.
Do not rename variables, restructure working code, or refactor beyond the task at hand.
Keep diffs small and focused so they are easy to review.

## Git Workflow

Trunk-based: push til main trigger deploy til q1/q2 via GitHub Actions. Produksjon deployes med release-workflow. Rollback-workflow finnes for prod.

## Boundaries

### ✅ Always

- Run tests after changes
- Follow existing code patterns in the project
- Preserve existing code structure — do not reorganize or refactor beyond the task
- Validate all external input

### ⚠️ Ask First

- Changing authentication mechanisms
- Adding new dependencies
- Modifying database schema

### 🚫 Never

- Commit secrets or credentials
- Skip input validation on external boundaries

---
name: Database Migration Standards
description: "Standarder for databasemigrasjoner med Flyway i bidrag-backend: navnekonvensjon, sikre endringer og idempotente skript."
applyTo: "**/db/migration/**/*.sql"
---

# Standarder for databasemigrasjoner (Flyway)

Standarder for databasemigrasjoner med Flyway i `bidrag-backend`.

## Navnekonvensjon for migrasjonsfiler

Konvensjonen i dette repoet: `V{major}_{minor}_{patch}__{description-with-dashes}.sql` (understrek mellom versjonssegmentene, bindestrek i beskrivelsen — sjekk appens eksisterende `db/migration`-mappe før du legger til en ny migrasjon, siden det eksakte mønsteret kan variere litt per app).

### Eksempler (fra dette repoet)

```
V1_0_9__alter-table-ainntektspost-rename-columns.sql
V1_0_20__alter-table-kontantstotte-add-index.sql
V1_0_22__create-table_forelder.sql
V1_0_29__drop-table-husstandsmedlem.sql
```

### Regler

- Versjonsnumre må være sekvensielle innenfor appens egen migrasjonshistorikk
- **Endre ALDRI eksisterende migrasjoner** — opprett alltid en ny
- Følg samme beskrivelsesstil som allerede brukes i målappens `db/migration`-mappe (noen bruker bindestrek, andre understrek i beskrivelsesdelen — vær konsistent med det som allerede finnes, ikke bland innenfor samme app)

## Struktur på migrasjonsfil

```sql
-- V1_0_1__create-table-eksempel.sql

CREATE TABLE eksempel (
    id BIGSERIAL PRIMARY KEY,
    ident VARCHAR(11) NOT NULL,
    periode_id UUID NOT NULL,
    fom DATE NOT NULL,
    tom DATE NOT NULL,
    opprettet_tidspunkt TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ident ON eksempel(ident);
CREATE INDEX idx_periode ON eksempel(periode_id);
CREATE INDEX idx_fom_tom ON eksempel(fom, tom);
```

## Beste praksis

### Primærnøkler

```sql
-- Bruk BIGSERIAL for auto-inkrementerende primærnøkler
id BIGSERIAL PRIMARY KEY,

-- Bruk UUID for distribuerte systemer
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
```

### Tidsstempler

```sql
-- Inkluder alltid tidsstempler — følg navngivningen som allerede brukes i målappen
opprettet_tidspunkt TIMESTAMP NOT NULL DEFAULT NOW(),
endret_tidspunkt TIMESTAMP NOT NULL DEFAULT NOW()
```

### Indekser

```sql
-- Indekser fremmednøkler
CREATE INDEX idx_sak_id ON periode(sak_id);

-- Indekser ofte spurte kolonner
CREATE INDEX idx_opprettet_tidspunkt ON periode(opprettet_tidspunkt);

-- Sammensatte indekser for spørringer på flere kolonner
CREATE INDEX idx_sak_status ON periode(sak_id, status);

-- Partielle indekser for filtrerte spørringer
CREATE INDEX idx_aktive_perioder ON periode(sak_id)
WHERE status = 'aktiv';
```

### Constraints

```sql
-- Fremmednøkler med ON DELETE CASCADE
sak_id BIGINT NOT NULL REFERENCES sak(id) ON DELETE CASCADE,

-- Check-constraints
CONSTRAINT check_positive_amount CHECK (belop > 0),
CONSTRAINT check_valid_status CHECK (status IN ('pending', 'aktiv', 'avsluttet')),

-- Unique-constraints
CONSTRAINT unique_sak_periode UNIQUE (sak_id, periode_id)
```

### Datatyper

```sql
-- Foretrekk spesifikke typer
VARCHAR(n)      -- For strenger med kjent maks-lengde (f.eks. VARCHAR(11) for fnr)
TEXT            -- For strenger med ukjent lengde
BIGINT          -- For store tall
NUMERIC(10,2)   -- For desimaltall (penger)
TIMESTAMP       -- For dato/tid
DATE            -- Kun for datoer
BOOLEAN         -- For sant/usant
UUID            -- For unike identifikatorer
JSONB           -- For strukturert JSON-data
```

## Migrasjonsmønstre

### Legge til en kolonne

```sql
-- V1_1_0__add-status-column.sql

ALTER TABLE periode
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'pending';

CREATE INDEX idx_status ON periode(status);
```

### Migrering av store tabeller

```sql
-- Legg til kolonne med default (øyeblikkelig i PostgreSQL 11+)
ALTER TABLE stor_tabell ADD COLUMN ny_kolonne BOOLEAN DEFAULT false;

-- Bruk CREATE INDEX CONCURRENTLY kun i sin egen dedikerte migrasjon
-- uten andre statements i filen, og med Flyway sin transaksjonshåndtering
-- konfigurert deretter (samtidig indeksbygging kan ikke kjøre inne i en transaksjon).
```

## Optimalisering av PostgreSQL-spørringer

### EXPLAIN ANALYZE

Analyser alltid nye eller endrede spørringer:

```sql
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT * FROM vedtak
WHERE sak_id = 12345 AND status = 'aktiv'
ORDER BY opprettet_tidspunkt DESC LIMIT 10;
```

Faresignaler: `Seq Scan` på store tabeller, `Sort external merge`, stort avvik mellom estimerte/faktiske rader.

## Spring Boot-integrasjon

Flyway kjører automatisk ved oppstart via Spring Boot sin autokonfigurasjon — ingen manuell `Flyway.configure()`-kall trengs i applikasjonskoden:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

## Testing av migrasjoner

```kotlin
@Testcontainers
class MigrationTest {
    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15")
    }

    @Test
    fun `migrations should run successfully`() {
        val dataSource = HikariDataSource().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
        }

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .load()

        val result = flyway.migrate()
        result.migrationsExecuted shouldBeGreaterThan 0
    }
}
```

## Grenser

### ✅ Alltid

- Følg versjon + beskrivelse-navnekonvensjonen som allerede brukes i målappens `db/migration`-mappe
- Legg til indekser for fremmednøkler
- Inkluder tidsstempel-kolonner
- Bruk passende datatyper
- Test migrasjoner i dev-miljø først

### ⚠️ Spør først

- Skjemaendringer som påvirker flere tabeller
- Sletting av kolonner eller tabeller
- Endring av primærnøkler
- Store datamigrasjoner

### 🚫 Aldri

- Endre eksisterende migrasjonsfiler
- Hoppe over versjonsnumre
- Blande navnekonvensjoner innenfor samme app
- Deploye utestede migrasjoner til produksjon
- Committe migrasjonsfiler uten å teste

## Relatert

| Ressurs | Bruk til |
|----------|---------|
| `$flyway-migration`-skill | Flyway-migrasjonsmønstre og beste praksis |
| `$postgresql-review`-skill | Spørringsoptimalisering og indekseringsstrategi |
| `@nais-agent` | GCP Cloud SQL-konfigurasjon i Nais-manifester |

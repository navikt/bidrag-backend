# bidrag-admin-fss

FSS-tjeneste for produksjonsoppfølging av Bisys — automatiske datakvalitetskontroller og batch-styring mot DB2.

## Hva gjør tjenesten?

Tjenesten kjører planlagte sjekker hvert 5. minutt **kun i prod-fss** og varsler ved avvik:

| Oppfølging | Beskrivelse | Utfall |
|---|---|---|
| **Duplikate roller** | Finner saker i Bisys med flere roller av samme type | Jira-sak i FAGSYSTEM med ferdig SQL-patch |
| **Ugyldige objektnummer** | Finner saker med gjenbrukte objektnummer for barn | Jira-sak i FAGSYSTEM med ferdig SQL-patch |
| **Feilede vedtaksoverføringer** | Oppdager vedtaksoverføringer med status FEILET | Slack-varsel med saksnumre og feilbeskrivelse |
| **Porten-saker** | Nye Porten-saker uten Slack-label | Slack-varsel med lenke til Jira-saken |

I tillegg eksponerer tjenesten et API for å starte, stoppe og overvåke batchjobber i Bisys.

## Kom i gang

**Forutsetninger:** Java 21, Maven, tilgang til FSS-nettverk (DB2)

```bash
# Bygg
mvn -f bidrag-admin-fss clean install

# Kjør tester
mvn -f bidrag-admin-fss test
```

Tjenesten kobler seg til IBM DB2 og kan ikke kjøres fullstendig lokalt uten VPN og DB2-tilgang. Bruk profilen `lokal-nais` med nødvendige miljøvariabler:

```bash
SPRING_PROFILES_ACTIVE=lokal-nais \
  BISYS_DB_USERNAME=... \
  BISYS_DB_PASSWORD=... \
  mvn spring-boot:run -f bidrag-admin-fss
```

## Tech stack

- **Språk:** Kotlin
- **Rammeverk:** Spring Boot
- **Database:** IBM DB2 (Bisys)
- **Plattform:** Nais (FSS)
- **Auth:** Azure AD (`token-validation-spring`)

## API

Swagger UI er tilgjengelig på rot-URL (`/`) i dev og prod.

| Method | Path | Beskrivelse |
|--------|------|-------------|
| `GET` | `/api/bisys/batch` | Henter alle batchnavn fra Bisys |
| `GET` | `/api/bisys/batch/launch/{jobName}` | Starter en batchjobb |
| `GET` | `/api/bisys/batch/running/{jobName}` | Henter kjørende jobber for et jobbnavn |
| `GET` | `/api/bisys/batch/stop/{executionId}` | Stopper en kjørende jobb |
| `GET` | `/api/bisys/batch/parameters/{executionId}` | Henter parametere for en kjøring |
| `GET` | `/api/bisys/batch/summary/{executionId}` | Henter sammendrag for en kjøring |
| `GET` | `/actuator/health` | Health check |
| `GET` | `/actuator/prometheus` | Prometheus-metrikker |

## Konfigurasjon

| Variabel | Beskrivelse | Påkrevd |
|----------|-------------|---------|
| `DB_HOST` | DB2-host | Ja |
| `DB_PORT` | DB2-port | Ja |
| `DB_NAME` | DB2-databasenavn | Ja |
| `BISYS_DB_SCHEMA` | DB2-schema for Bisys | Ja |
| `BISYS_URL` | Host for Bisys-tjenester | Ja |
| `SLACK_CHANNEL_ID` | Slack-kanal for varsler | Ja |
| `JIRA_URL` | Base-URL til Jira REST API | Ja |

Hemmeligheter hentes fra Nais secrets:

| Secret | Innhold |
|--------|---------|
| `bidrag-bisys-db` | DB2-brukernavn og passord |
| `mr-b-slack-oauth-token` | Slack OAuth-token |
| `bidrag-admin-prodoppfolging-jira-creds` | Jira API-token |

## Deploy

- **Dev:** Automatisk ved push til alle brancher
- **Prod:** Automatisk ved push til `main` (kun ved endringer i relevante filer)
- **Manifester:** `.nais/bidrag-admin-fss/`

| Miljø | URL |
|-------|-----|
| Dev | https://bidrag-admin-fss.intern.dev.nav.no |
| Prod | https://bidrag-admin-fss.intern.nav.no |

## Observability

- **Logger:** [Kibana / team-logs](https://logs.adeo.no) — søk på `nais_app_name: bidrag-admin-fss`
- **Metrikker:** `/actuator/prometheus`

## Team

- **Team:** Bidrag
- **Slack:** [#team-bidrag](https://nav-it.slack.com/archives/CGVH3MV47)
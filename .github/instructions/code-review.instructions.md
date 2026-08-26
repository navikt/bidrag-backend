---
applyTo: "**"
---

# Instruksjoner for kodegjennomgang

Generelle retningslinjer for GitHub Copilot sin kodegjennomgang. Flagg for menneskelig reviewer — ikke blokker.

## Norsk tekst

Bruk norsk bokmål for `.md`-filer og annen brukervendt tekst.

## Generelle sjekker

- **Omfang**: Rimelig omfang, ingen urelaterte endringer bundlet inn. Flagg spesielt
  omdøpte variabler/parametere, restrukturert fungerende kode eller refaktorering
  utenfor PR-ens uttalte scope — diffen bør stå i forhold til det oppgitte målet.
- **Tester**: Ny kode bør ha unit-/integrasjonstester; eksisterende tester skal fortsatt passere

## Sikkerhetskritiske endringer (flagg alltid)

### Hemmeligheter og credentials

- Hardkodede tokens, passord, API-nøkler eller credentials
- Environment-variabler som inneholder `SECRET`, `TOKEN`, `CREDENTIAL`, `PASSWORD`
- Vault- eller Azure Key Vault-referanser lagt til/endret
- `.env`-filer committet (skal være i `.gitignore`)

### Sensitive data i output

- FNR (fødselsnummer), aktørId eller annen PII i logg-setninger (bruk `secureLogger`/team-logs, aldri standard-loggeren)
- Token-verdier, request-bodies eller headers logget
- Valideringsannotasjoner som eksponerer brukerinput (`${validatedValue}` i `@Pattern`/`@Size`)
- Exception-meldinger med sensitivt innhold

### Autentisering og autorisasjon

- Token-håndtering (OIDC, SAML, JWT, Azure AD, TokenX, ID-porten)
- Tilgangskontroll-annotasjoner (`@ProtectedWithClaims`, `@BeskyttetRessurs`)
- ABAC-/policy-evalueringslogikk
- Nye eller endrede API-endepunkter uten auth-middleware
- Endringer i CORS-konfigurasjon (spesielt `allowedOrigins: ["*"]`)

### CodeQL / fiksing av sikkerhetsvarsler

- En fiks for et CodeQL-varsel som kun legger til en boolsk `require()`/`check()`-sjekk med `.matches()` eller en denylist (`!contains("..")`) uten å avlede en **ny** verdi fra valideringen — CodeQL sin taint-tracker regner ikke dette som en sanitizer. Se `$security-review` og `@codeql-fix` for det korrekte `Regex(...).matchEntire(value)?.value`-mønsteret.
- En fiks som strammer inn valideringen akkurat nok til å dysse ned det spesifikke varselet, uten å adressere det underliggende problemet med input man ikke stoler på, mer generelt

## Infrastrukturendringer (flagg for gjennomgang)

### NAIS-konfigurasjon

Endringer i `nais*.yaml` / `.nais/*.yaml`:

- `accessPolicy` — inbound-/outbound-regler endret
- `env` — nye secrets, scopes eller credentials
- `azure.application` / `tokenx` / `idporten`-konfigurasjon
- Ressursgrenser (CPU, minne) betydelig redusert
- Endringer i antall replikaer
- Nye `envFrom`-secret-referanser

### GitHub Actions

Endringer i `.github/workflows/`:

- Deploy-mål eller -miljøer endret
- Teststeg fjernet eller svekket
- Ikke-pinnede action-versjoner (bruk SHA, ikke `@main` eller `@v3`)
- Secrets brukt i workflow-steg
- `pull_request_target`-trigger (sikkerhetsrisiko)
- Nye tillatelser gitt til workflow
- Ny app-workflow som ikke følger det etablerte `detect_changes` → `bygg_og_test` → `deploy_q1`/`deploy_q2`/`deploy_prod`-mønsteret (se `$code-review`-skillen for hele begrunnelsen)

## Kodekvalitet (flagg hvis bekymringsverdig)

### Testdekning

- Testfiler fjernet eller test-assertions slettet
- `@Disabled`, `skipTests`, `skip()` lagt til uten forklaring
- Endringer som reduserer dekning på kritiske stier
- Deploy-guards eller godkjenningssteg svekket

### Integrasjonspunkter

- Nye eksterne tjeneste-klienter (REST, gRPC, SOAP)
- Endringer i Kafka-topic, serialisering eller feilhåndtering
- Databasemigrasjonsfiler (skjemaendringer, risiko for datatap)
- Endringer i retry-logikk eller circuit breaker-konfigurasjon

### Feilhåndtering

- Catch-all exception-handlere som svelger feil stille
- Manglende feilpropagering i async-/bakgrunnsoppgaver
- Panic/fatal i bibliotekskode (bør returnere feil i stedet)

## Relatert

| Type | Navn | Når brukes |
|------|------|-------------|
| Skill | `$code-review` | Full repo-spesifikk gjennomgang (pom.xml, sikkerhet, workflows, tester) |
| Skill | `$security-review` | Sikkerhetssjekk før commit/push |
| Agent | `@codeql-fix` | Triagere og fikse CodeQL-/code-scanning-varsler korrekt |

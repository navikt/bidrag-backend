---
name: security-owasp
description: OWASP Top 10:2025 kodenivå-mønstre for Kotlin, Go, Java og Node.js — tilgangskontroll, forsyningskjede, injeksjon og feilhåndtering
license: MIT
metadata:
  domain: auth
  tags: security owasp kotlin go java nodejs nais supply-chain
---

# OWASP Top 10:2025 — kodenivå-sikkerhet

Taktiske sikkerhetsmønstre for Kotlin, Go, Java og Node.js på Nais, tilpasset **OWASP Top 10:2025**.

Utfyller `@security-champion-agent` (arkitekturnivå trusselmodellering) og `security-review`-skillen (skanneverktøy).

> Fullstendige kodeeksempler for hver kategori: se `examples.md` i denne skill-mappen.

## A01: Broken Access Control (inkl. SSRF)

```kotlin
// ❌ IDOR — stoler på brukeroppgitt ID uten eierskapssjekk
get("/api/vedtak/{id}") {
    val vedtak = vedtakRepository.findById(call.parameters["id"]!!.toLong())
    call.respond(vedtak)
}

// ✅ Verifiser eierskap før ressursen returneres
get("/api/vedtak/{id}") {
    val bruker = call.hentBruker()
    val vedtak = vedtakRepository.findById(call.parameters["id"]!!.toLong())
        ?: return@get call.respond(HttpStatusCode.NotFound)
    if (vedtak.brukerId != bruker.id) return@get call.respond(HttpStatusCode.Forbidden)
    call.respond(vedtak.toDTO())
}
```

```go
// ✅ SSRF-forebygging — valider utgående URL mot en allowlist
func fetchExternal(targetURL string) error {
    parsed, err := url.Parse(targetURL)
    if err != nil { return err }
    if !isAllowedHost(parsed.Host) { return fmt.Errorf("host not allowed: %s", parsed.Host) }
    // fortsett med kallet
}
```

- Deny by default — krev eksplisitte tilganger, ikke eksplisitte avslag
- Ressursnivå-sjekker — ikke bare «er innlogget», men «eier denne ressursen»
- M2M-tokens — valider `azp`-claim mot `AZURE_APP_PRE_AUTHORIZED_APPS`
- SSRF — valider utgående URL-er; bruk Nais `accessPolicy.outbound` som defense-in-depth

## A02: Security Misconfiguration

```kotlin
// ❌ Åpen CORS
install(CORS) { anyHost() }

// ✅ Begrens til kjente origins
install(CORS) { allowHost("my-app.intern.nav.no", schemes = listOf("https")) }
```

```go
// ❌ Debug-endepunkt eksponert på offentlig ingress
mux.HandleFunc("/debug/pprof/", pprof.Index)

// ✅ Debug-endepunkt på separat, intern-only port (Nais håndterer dette)
internalMux := http.NewServeMux()
internalMux.HandleFunc("/debug/pprof/", pprof.Index)
go http.ListenAndServe(":9090", internalMux) // ikke eksponert via ingress
```

- CORS begrenset til kjente origins — aldri `*` eller `anyHost()`
- Debug-/admin-endepunkter skal ikke ligge på offentlig ingress
- Feilresponser saneres — ingen stack traces, SQL-feil eller filstier til klienten
- Default-deny Nais `accessPolicy` — kun eksplisitt inbound/outbound

## A03: Software Supply Chain Failures (NY i 2025)

```go
// go.sum gir integritetsverifisering — committ den alltid
// Bruk govulncheck for kjente sårbarheter
// $ govulncheck ./...

// ✅ Pin avhengigheter til eksakte versjoner i go.mod
require (
    golang.org/x/crypto v0.31.0
    github.com/jackc/pgx/v5 v5.7.2
)
```

```kotlin
// build.gradle.kts — bruk dependency locking og BOM
dependencyLocking { lockAllConfigurations() }
dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.1") }
}
// Kjør: ./gradlew dependencies --write-locks
```

```yaml
# ✅ GitHub Actions — pin til full SHA, aldri @main eller flytende tags
- uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683 # v4.2.2
# ❌ Sårbar for supply chain-angrep
- uses: actions/checkout@main
```

- Pin alle avhengigheter til eksakte versjoner; bruk lockfiles
- Skann avhengigheter: `govulncheck ./...`, `trivy repo .`, `./gradlew dependencyCheckAnalyze`
- GitHub Actions pinnet til full commit-SHA (ikke tags)
- Generer SBOM for produksjonsartefakter der det er mulig
- Foretrekk godt vedlikeholdte, first-party pakker

## A04: Cryptographic Failures

```go
// ❌ Deaktivering av TLS-verifisering
client := &http.Client{
    Transport: &http.Transport{
        TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
    },
}

// ✅ Standard TLS-konfig (Go krever TLS 1.2+ som standard)
client := &http.Client{}
```

```kotlin
// ❌ Svak hashing for passord
val hash = MessageDigest.getInstance("MD5").digest(password.toByteArray())

// ✅ bcrypt for passordhashing
val hashed = BCrypt.hashpw(password, BCrypt.gensalt(12))
```

- Passord: bcrypt (cost ≥ 12) eller argon2id — aldri MD5/SHA-1/SHA-256
- Hemmeligheter: alltid fra Nais-miljøvariabler eller Secret-ressurser — aldri hardkodet
- TLS 1.2+: sett aldri `InsecureSkipVerify: true`
- Kryptering: AES-256-GCM; roter nøkler jevnlig

## A05: Injection

```kotlin
// ❌ SQL-injeksjon via strengmal
session.run(queryOf("SELECT * FROM vedtak WHERE status = '$status'").map { ... }.asList)

// ✅ Parameterisert spørring (Kotliquery)
session.run(queryOf("SELECT * FROM vedtak WHERE status = ?", status).map { ... }.asList)
```

```go
// ❌ Kommandoinjeksjon via shell
exec.Command("sh", "-c", fmt.Sprintf("process %s", userInput)).Run()

// ✅ Send argumenter direkte (ingen shell-tolkning)
exec.Command("process", userInput).Run()
```

- Alle SQL-spørringer parameterisert (`?` / `$1`) — aldri strengkonkatenering
- Ingen shell-kjøring med brukerkontrollert input
- Valider/saner all ekstern input ved tjenestegrensen

## A09: Security Logging and Alerting Failures

```kotlin
// ✅ Strukturert logging med korrelasjons-ID, ingen PII
log.info("Vedtak opprettet", kv("vedtakId", vedtak.id), kv("sakId", sak.id))

// ❌ PII i logg — brudd på GDPR
log.info("Vedtak for bruker ${bruker.fnr}")
```

- Ingen PII i logg (fnr, navn, adresse, tokens)
- Sporingslogg for sensitive operasjoner (vedtak, utbetaling, tilgang)
- Korrelasjons-ID-er forplantes på tvers av tjenester (OpenTelemetry trace context)
- Varsling på avvikende mønstre (auth-feil, rate spikes)

## A10: Mishandling of Exceptional Conditions (NY i 2025)

```go
// ❌ Panic lekker til kalleren, krasjer tjenesten
func processRequest(data []byte) Result {
    var req Request
    json.Unmarshal(data, &req) // ignorerer feil, req kan bli zero-value
    return handle(req)
}

// ✅ Håndter feil eksplisitt, feil trygt
func processRequest(data []byte) (Result, error) {
    var req Request
    if err := json.Unmarshal(data, &req); err != nil {
        return Result{}, fmt.Errorf("invalid request payload: %w", err)
    }
    return handle(req)
}
```

```kotlin
// ❌ Svelger exceptions stille
fun process(data: String): Result {
    try { return parse(data) }
    catch (e: Exception) { return Result.empty() } // stille feil, ingen logging
}

// ✅ Logg, wrap og eksponer feil på riktig måte
fun process(data: String): Result {
    return try { parse(data) }
    catch (e: Exception) {
        log.error("Parsing failed", kv("error", e.message))
        throw ServiceException("Could not process input", e)
    }
}
```

- Håndter alltid feil — ignorer aldri returnerte feil i Go
- Recover fra panics ved HTTP-handler-grensen (middleware)
- Feil trygt: nekt tilgang som standard når tilstanden er usikker
- Saner feilmeldinger: interne detaljer blir i loggen, ikke i responsen
- Sentralisert feilhåndtering via middleware/exception mappers

## Hurtigsjekkliste

- [ ] **A01** — Ressursnivå-tilgangssjekker på alle endepunkter (ikke bare auth)
- [ ] **A01** — M2M-tokens validerer `azp` mot pre-autoriserte apper
- [ ] **A01** — Utgående URL-er valideres; Nais egress-policy konfigurert
- [ ] **A02** — CORS begrenset til kjente origins
- [ ] **A02** — Debug-endepunkter ikke på offentlig ingress
- [ ] **A02** — Feilresponser saneres (ingen stack traces til klient)
- [ ] **A03** — Avhengigheter pinnet til eksakte versjoner med lockfiles
- [ ] **A03** — `govulncheck` / `trivy repo .` går uten HIGH/CRITICAL
- [ ] **A03** — GitHub Actions pinnet til full SHA
- [ ] **A04** — bcrypt/argon2id for passord, aldri MD5/SHA-1
- [ ] **A04** — TLS 1.2+ håndheves, ingen `InsecureSkipVerify`
- [ ] **A04** — Hemmeligheter fra miljø/Nais, aldri hardkodet
- [ ] **A05** — Alle SQL-spørringer parameterisert (`?` / `$1`)
- [ ] **A05** — Ingen shell-kjøring med brukerinput
- [ ] **A07** — JWT validerer `exp`, `iss`, `aud` og algoritme
- [ ] **A08** — Deserialisering kun til konkrete typer
- [ ] **A09** — Ingen PII i logg (fnr, navn, adresse)
- [ ] **A09** — Sporingslogg for sensitive operasjoner
- [ ] **A10** — Alle feil håndtert (ingen ignorerte returverdier i Go)
- [ ] **A10** — Panic recovery i HTTP-handlere
- [ ] **A10** — Feilmeldinger saneres før klientrespons

## Relatert

| Ressurs | Bruk til |
|----------|---------|
| `security-review`-skill | Pre-commit-skanning (trivy, zizmor, govulncheck) |
| `@security-champion-agent` | Trusselmodellering, compliance, Navs sikkerhetsarkitektur |
| `@auth-agent` | JWT-validering, TokenX, ID-porten-implementasjon |
| `threat-model`-skill | STRIDE-A-analyse for nye tjenester |
| [OWASP Top 10:2025](https://owasp.org/Top10/2025/) | Offisielle kategoribeskrivelser |
| [OWASP Go SCP](https://owasp.org/www-project-go-secure-coding-practices/) | Go-spesifikk veiledning for sikker koding |
| [OWASP CI/CD Top 10](https://owasp.org/www-project-top-10-ci-cd-security-risks/) | Sikkerhetsrisikoer i pipeline |
| [sikkerhet.nav.no](https://sikkerhet.nav.no) | Navs Golden Path |

## Grenser

### ✅ Alltid

- Parameteriserte spørringer for all SQL
- Ressursnivå-tilgangssjekker på alle datareturnerende endepunkter
- Strukturert logging uten PII
- SHA-pinnede GitHub Actions
- Eksplisitt feilhåndtering (ingen ignorerte feil)
- Avhengigheter skannet før release

### ⚠️ Spør først

- Egne kryptografiske implementasjoner
- Deaktivering av sikkerhetsfunksjoner for testing
- Endring av autentiserings- eller autorisasjonslogikk
- Nye utgående eksterne hosts

### 🚫 Aldri

- Strengkonkatenerte SQL-spørringer
- `InsecureSkipVerify: true` i produksjon
- PII i loggutsagn (fnr, navn, adresse)
- Wildcard CORS (`*` / `anyHost()`)
- Hardkodede hemmeligheter eller krypteringsnøkler
- Flytende tags (`@main`, `@v3`) for GitHub Actions
- Stille svelging av feil uten logging

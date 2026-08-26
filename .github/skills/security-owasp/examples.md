# OWASP Top 10:2025 — eksempler

Referanse-snippets for Kotlin, Go, Java (Spring Boot) og Node.js / Next.js.

Hver underseksjon viser et kort ❌ anti-mønster og et tilhørende ✅ korrekt mønster.

## A01: Broken Access Control (inkl. SSRF)

### Kotlin

```kotlin
// ❌ IDOR — stoler på id fra kalleren
val vedtak = vedtakRepository.findById(call.parameters["id"]!!.toLong())
call.respond(vedtak)

// ✅ Verifiser ressurseierskap før data returneres
val bruker = call.hentBruker()
val vedtak = vedtakRepository.findById(call.parameters["id"]!!.toLong()) ?: return@get call.respond(HttpStatusCode.NotFound)
if (vedtak.brukerId != bruker.id) return@get call.respond(HttpStatusCode.Forbidden)
call.respond(vedtak.toDTO())
```

### Go

```go
// ❌ SSRF — henter brukeroppgitt URL direkte
resp, _ := http.Get(r.URL.Query().Get("url"))

// ✅ Allowlist host og krev HTTPS
u, err := url.Parse(r.URL.Query().Get("url"))
if err != nil || u.Scheme != "https" || !allowedHosts[u.Hostname()] {
http.Error(w, "host not allowed", http.StatusForbidden)
return
}
resp, _ := http.Get(u.String())
```

### Java (Spring Boot)

```java
// ❌ Autentisert bruker kan lese enhver vedtak via id
@GetMapping("/api/vedtak/{id}")
VedtakDto hent(@PathVariable Long id) { return service.hent(id); }

// ✅ Håndhev tilgang ved metodegrensen
@GetMapping("/api/vedtak/{id}")
@PreAuthorize("@vedtakAuth.canRead(#id, authentication)")
VedtakDto hent(@PathVariable Long id) { return service.hent(id); }
```

### Node.js / Next.js

```ts
// ❌ Route-handler stoler på sakId fra query string
export async function GET(req: NextRequest) {
  return Response.json(await hentVedtak(req.nextUrl.searchParams.get("sakId")!))
}

// ✅ Begrens oppslag til autentisert bruker
export async function GET() {
  const session = await requireSession()
  return Response.json(await hentVedtakForBruker(session.brukerId))
}
```

### Mønstre

- Verifiser eierskap og omfang for hver ressurs, ikke bare ved innlogging.
- Deny by default når autorisasjonsdata mangler eller er tvetydige.
- For SSRF: allowlist utgående hosts, krev HTTPS, blokker loopback- og metadata-adresser.
- For M2M-tokens: valider `azp` mot pre-autoriserte apper.

## A02: Security Misconfiguration

### Kotlin

```kotlin
// ❌ Åpen CORS i produksjon
install(CORS) { anyHost() }

// ✅ Begrens origins eksplisitt
install(CORS) {
    allowHost("my-copilot.intern.nav.no", schemes = listOf("https"))
}
```

### Go

```go
// ❌ Debug-endepunkt på offentlig listener
mux.HandleFunc("/debug/pprof/", pprof.Index)

// ✅ Debug-endepunkt på intern-only listener
internalMux := http.NewServeMux()
internalMux.HandleFunc("/debug/pprof/", pprof.Index)
go http.ListenAndServe("127.0.0.1:9090", internalMux)
```

### Java (Spring Boot)

```java
// ❌ Wildcard CORS på controller
@CrossOrigin(origins = "*")
@RestController class VedtakController {}

// ✅ Restriktiv CORS via Spring Security
cfg.setAllowedOrigins(List.of("https://my-copilot.intern.nav.no"));
cfg.setAllowedMethods(List.of("GET", "POST"));
source.registerCorsConfiguration("/**", cfg);
```

### Node.js / Next.js

```ts
// ❌ For bred Server Actions-konfig
serverActions: { allowedOrigins: ["*"], bodySizeLimit: "20mb" }

// ✅ Same-origin som standard, legg kun til betrodde proxyer ved behov
serverActions: { allowedOrigins: ["my-proxy.intern.nav.no"], bodySizeLimit: "1mb" }
```

### Mønstre

- Begrens CORS til kjente origins, metoder og headere.
- Hold debug- og admin-endepunkter unna offentlig ingress.
- Deaktiver funksjoner som kun er ment for utvikling, i produksjon.
- Returner generiske klientfeil; hold stack traces og SQL-feil kun i logg.

## A03: Software Supply Chain Failures

### Kotlin

```kotlin
// ❌ Flytende avhengighetsversjoner
implementation("org.postgresql:postgresql:+")

// ✅ Pin og lås avhengigheter
implementation("org.postgresql:postgresql:42.7.5")
dependencyLocking { lockAllConfigurations() }
```

### Go

```go
// ❌ Manglende verifiseringssteg i CI
// go test ./...

// ✅ Verifiser integritet og kjente sårbarheter
// go mod verify
// govulncheck ./...
```

### Java (Spring Boot)

```xml
<!-- ❌ Flytende ranges i pom.xml -->
<version>[5.8,)</version>

<!-- ✅ Pin eksakt versjon og hold lockfile/BOM oppdatert -->
<version>5.8.16</version>
```

### Node.js / Next.js

```json
// ❌ Flytende avhengighetsrange
"next": "^16.0.0"

// ✅ Pin versjon og committ lockfile
"next": "16.0.0"
```

### Mønstre

- Pin avhengigheter og committ lockfiles (`go.sum`, `gradle.lockfile`, `package-lock.json` eller `pnpm-lock.yaml`).
- Skann avhengigheter jevnlig med `govulncheck`, `npm audit`, Trivy eller tilsvarende CI-sjekker.
- Pin GitHub Actions til full commit-SHA, ikke tags eller branches.
- Foretrekk vedlikeholdte, first-party pakker fremfor forlatte wrappere.

## A04: Cryptographic Failures

### Kotlin

```kotlin
// ❌ Svak passordhashing
val hash = MessageDigest.getInstance("MD5").digest(password.toByteArray())

// ✅ bcrypt for passord
val hashed = BCrypt.hashpw(password, BCrypt.gensalt(12))
```

### Go

```go
// ❌ TLS-verifisering deaktivert
client := &http.Client{Transport: &http.Transport{TLSClientConfig: &tls.Config{InsecureSkipVerify: true}}}

// ✅ Bruk standard TLS-verifisering
client := &http.Client{}
```

### Java (Spring Boot)

```java
// ❌ Hardkodet hemmelighet og reversibel passordlagring
String signingKey = "secret";
String stored = password;

// ✅ Hemmelighet fra env og adaptiv passordhashing
String signingKey = env.getRequiredProperty("JWT_SIGNING_KEY");
String stored = passwordEncoder.encode(password);
```

### Node.js / Next.js

```ts
// ❌ Svak hash og hardkodet hemmelighet
const hash = createHash("sha1").update(password).digest("hex")
const jwtSecret = "secret"

// ✅ Bruk scrypt/bcrypt og env-styrt hemmelighet
const hash = await scryptHash(password)
const jwtSecret = process.env.JWT_SECRET!
```

### Mønstre

- Bruk bcrypt eller argon2id for passord, aldri MD5 eller ren SHA-hash.
- Hold hemmeligheter i Nais-miljøvariabler eller secret-ressurser, aldri i kildekode.
- Krev moderne TLS og sett aldri `InsecureSkipVerify: true`.
- Foretrekk autentisert kryptering som AES-256-GCM når du krypterer applikasjonsdata.

## A05: Injection

### Kotlin

```kotlin
// ❌ SQL-injeksjon via string interpolation
queryOf("SELECT * FROM vedtak WHERE status = '$status'")

// ✅ Parameterisert spørring
queryOf("SELECT * FROM vedtak WHERE status = ?", status)
```

### Go

```go
// ❌ Shell-injeksjon via sh -c
exec.Command("sh", "-c", fmt.Sprintf("journalctl -u %s", service)).Run()

// ✅ Send argumenter direkte
exec.Command("journalctl", "-u", service).Run()
```

### Java (Spring Boot)

```java
// ❌ SQL-injeksjon i JdbcTemplate
jdbcTemplate.query("SELECT * FROM vedtak WHERE fnr = '" + fnr + "'", rowMapper);

// ✅ Preparerte parametre i JdbcTemplate
jdbcTemplate.query("SELECT * FROM vedtak WHERE fnr = ?", rowMapper, fnr);
```

### Node.js / Next.js

```ts
// ❌ Rå SQL og usanert bruker-HTML
await prisma.$queryRawUnsafe(`SELECT * FROM vedtak WHERE fnr = '${fnr}'`)
return <div dangerouslySetInnerHTML={{ __html: kommentar }} />

// ✅ Parameteriser SQL og saner rendret innhold
await prisma.$queryRaw`SELECT * FROM vedtak WHERE fnr = ${fnr}`
return <div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(kommentar) }} />
```

### Mønstre

- Parameteriser alle SQL-spørringer; bygg aldri SQL med strengkonkatenering.
- Unngå shell-kjøring for brukerkontrollerte data.
- Valider input ved grensen før den når template-, shell- eller databasekode.
- Behandle brukerinput som data, aldri som kode eller template-kilde.

## A06: Insecure Design

### Kotlin

```kotlin
// ❌ Forretningsregel mangler — negativt beløp aksepteres
fun opprettVedtak(belop: BigDecimal) = vedtakService.opprett(belop)

// ✅ Håndhev domeneregler før tilstandsendringer
fun opprettVedtak(belop: BigDecimal): Vedtak {
    require(belop > BigDecimal.ZERO) { "Beløp må være positivt" }
    return vedtakService.opprett(belop)
}
```

### Go

```go
// ❌ Ingen rate limiting på login
http.HandleFunc("/api/login", handleLogin)

// ✅ Rate-begrens sensitive endepunkter
http.Handle("/api/login", rateLimitMiddleware(limiter, http.HandlerFunc(handleLogin)))
```

### Java (Spring Boot)

```java
// ❌ Ingen validering av request body
public ResponseEntity<?> opprett(@RequestBody VedtakRequest req) { return ok(service.opprett(req)); }

// ✅ Valider form tidlig og håndhev forretningsregler i service-laget
public ResponseEntity<?> opprett(@RequestBody @Valid VedtakRequest req) { return ok(service.opprett(req)); }
```

### Node.js / Next.js

```ts
// ❌ Server Action stoler på rå form-data
export async function opprettVedtak(_: unknown, formData: FormData) { return save(formData.get("belop")) }

// ✅ Valider input før mutasjon
export async function opprettVedtak(_: unknown, formData: FormData) {
  const data = schema.parse({ belop: Number(formData.get("belop")) })
  return save(data.belop)
}
```

### Mønstre

- Valider input-form ved grensen, håndhev deretter forretningsregler i domenelaget.
- Legg til rate limiting på login, passordbytte, OTP og kostbare mutasjoner.
- Bruk idempotens for operasjoner som ellers kan dobbeltsendes.
- Design for fail-closed oppførsel når tilstanden er usikker.

## A07: Authentication Failures

### Kotlin

```kotlin
// ❌ Godtar enhver signert JWT
val claims = parser.parseClaimsJws(token).body

// ✅ Valider issuer, audience og utløpstid
val claims = parser.requireIssuer(issuer).requireAudience(audience).build().parseClaimsJws(token).body
require(claims.expiration.after(Date()))
```

### Go

```go
// ❌ Token parses uten claim-validering
jwt.Parse(tokenString, keyFunc)

// ✅ Håndhev issuer, audience, utløpstid og algoritme
jwt.ParseWithClaims(tokenString, &Claims{}, keyFunc,
jwt.WithIssuer(expectedIssuer), jwt.WithAudience(expectedAudience), jwt.WithValidMethods([]string{"RS256"}))
```

### Java (Spring Boot)

```java
// ❌ Egen auth-sjekk stoler på usignert header
String user = request.getHeader("X-User");

// ✅ La Spring Security validere JWT og håndheve auth sentralt
http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
http.authorizeHttpRequests(auth -> auth.requestMatchers("/api/**").authenticated());
```

### Node.js / Next.js

```ts
// ❌ middleware sjekker kun om cookien finnes
if (!req.cookies.get("session")) return NextResponse.redirect(new URL("/login", req.url))

// ✅ middleware vokter ruter, action re-sjekker session og Origin
if (!req.cookies.get("session")) return NextResponse.redirect(new URL("/login", req.url))
const session = await requireSession()
const h = await headers()
if (h.get("origin") !== `https://${h.get("host")}`) throw new Error("CSRF blocked")
```

### Mønstre

- Foretrekk plattform- eller rammeverkskomponenter for auth fremfor egen token-parsing.
- Valider `iss`, `aud`, `exp` og aksepterte signeringsalgoritmer.
- Bruk sikre, HTTP-only, `SameSite`-cookies for nettleser-sesjoner.
- Re-sjekk autentisering og autorisasjon inne i Server Actions og route-handlere.

## A08: Software or Data Integrity Failures

### Kotlin

```kotlin
// ❌ Usikker polymorf deserialisering
objectMapper.enableDefaultTyping()

// ✅ Dekod til eksplisitte DTO-er
val req = objectMapper.readValue(payload, VedtakRequest::class.java)
```

### Go

```go
// ❌ Dekod ubetrodd input til interface{}
var payload interface{}
json.NewDecoder(r.Body).Decode(&payload)

// ✅ Dekod til konkret request-type
var req VedtakRequest
json.NewDecoder(r.Body).Decode(&req)
```

### Java (Spring Boot)

```java
// ❌ Stol på webhook-payload uten signatursjekk
service.importVedtak(body);

// ✅ Verifiser signatur før data behandles
if (!signatureVerifier.isValid(signature, body)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
service.importVedtak(body);
```

### Node.js / Next.js

```ts
// ❌ Behandle callback-body uten integritetssjekk
await behandleVedtak(await req.text())

// ✅ Verifiser HMAC før payload godtas
const body = await req.text()
if (!isValidSignature(req.headers.get("x-signature"), body)) return new Response("unauthorized", { status: 401 })
await behandleVedtak(body)
```

### Mønstre

- Dekod ubetrodd input til eksplisitte DTO-er, ikke generiske objektgrafer.
- Verifiser signaturer på webhooks, callbacks og importerte artefakter før bruk.
- Pin CI-avhengigheter og actions slik at pipelinen er reproduserbar.
- Returner kun minimal, betrodd data fra server-kode til klienter.

## A09: Security Logging and Alerting Failures

### Kotlin

```kotlin
// ❌ PII i logg
log.info("Opprettet vedtak for fnr=${bruker.fnr}")

// ✅ Strukturert logging uten PII
log.info("Vedtak opprettet", kv("vedtakId", vedtak.id), kv("sakId", vedtak.sakId), kv("callId", callId))
```

### Go

```go
// ❌ Logger fnr direkte
slog.Info("vedtak created", "fnr", bruker.Fnr)

// ✅ Bruk ugjennomsiktige ID-er og request-korrelasjon
slog.Info("vedtak created", "vedtak_id", vedtak.ID, "sak_id", vedtak.SakID, "request_id", requestID)
```

### Java (Spring Boot)

```java
// ❌ Lekker fnr i logg
log.info("Opprettet vedtak for fnr={}", fnr);

// ✅ Logg ugjennomsiktige identifikatorer og trace-kontekst
log.info("Vedtak opprettet vedtakId={} sakId={} traceId={}", vedtakId, sakId, MDC.get("traceId"));
```

### Node.js / Next.js

```ts
// ❌ Logger request body med fnr
logger.info({ body }, "oppretter vedtak")

// ✅ Strukturert audit-logg uten PII
logger.info({ vedtakId, sakId, requestId }, "vedtak opprettet")
```

### Mønstre

- Logg aldri fnr, tokens, rå request bodies eller hemmeligheter.
- Bruk strukturert logg med korrelasjons-ID-er og ugjennomsiktige ressurs-ID-er.
- Opprett audit-hendelser for sensitive handlinger som vedtaksendringer og tilgangstildelinger.
- Varsle på mistenkelige mønstre som gjentatte auth-feil eller uvanlige trafikk-topper.

## A10: Mishandling of Exceptional Conditions

### Kotlin

```kotlin
// ❌ Svelger feil og fortsetter
val req = runCatching { call.receive<VedtakRequest>() }.getOrNull()

// ✅ Feil trygt og returner sanert feilmelding
val req = try { call.receive<VedtakRequest>() } catch (e: Exception) {
    log.warn("Invalid vedtak request", kv("callId", callId))
    return@post call.respond(HttpStatusCode.BadRequest, "Ugyldig forespørsel")
}
```

### Go

```go
// ❌ Ignorerer decode-feil
json.NewDecoder(r.Body).Decode(&req)

// ✅ Håndter feilen eksplisitt og stopp videre behandling
if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
http.Error(w, "invalid request", http.StatusBadRequest)
return
}
```

### Java (Spring Boot)

```java
// ❌ Lekker interne exception-detaljer til klient
return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));

// ✅ Sentraliser sanering i @RestControllerAdvice
@ExceptionHandler(Exception.class)
ResponseEntity<Map<String, String>> handle(Exception ex) { return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error")); }
```

### Node.js / Next.js

```ts
// ❌ Returnerer stack trace til klient
return Response.json({ error: err.stack }, { status: 500 })

// ✅ Logg detaljer server-side, returner generisk respons
logger.error({ err, requestId }, "route failed")
return Response.json({ error: "Internal server error" }, { status: 500 })
```

### Mønstre

- Håndter parse-, IO- og databasefeil eksplisitt.
- Feil closed når systemet ikke kan avgjøre et trygt utfall.
- Hold detaljert exception-data i logg, ikke i responser.
- Sentraliser feil-mapping i middleware, exception mappers eller route-hjelpere.

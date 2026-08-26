---
name: security-review
description: Bruk før commit, push eller pull request for å sjekke at koden er trygg å merge
license: MIT
metadata:
  domain: auth
  tags: security pre-commit vulnerability-scanning code-review
---

# Security Review Skill

Denne skillen gir pre-commit- og pre-PR-sikkerhetssjekker for Nav-applikasjoner. Dekker secret scanning, sårbarhetsskanning og Nav-spesifikke krav.

For arkitekturspørsmål, trusselmodellering eller compliance-avgjørelser, bruk `@security-champion` i stedet.

## Automatiserte skanninger

Kjør med `run_in_terminal`:

```bash
# Skann repo for kjente sårbarheter og hemmeligheter
trivy repo .

# Skann Docker-image for HIGH/CRITICAL CVE-er
trivy image <image-name> --severity HIGH,CRITICAL

# Skann GitHub Actions-workflows for usikre mønstre
zizmor .github/workflows/

# Rask søk etter hemmeligheter i git-historikk
git log -p --all -S 'password' -- '*.kt' '*.ts' | head -100
git log -p --all -S 'secret' -- '*.kt' '*.ts' | head -100
```

## Parameterisert SQL (aldri konkatener)

```kotlin
// ✅ Riktig – parameterisert spørring
fun findBruker(fnr: String): Bruker? =
    jdbcTemplate.queryForObject(
        "SELECT * FROM bruker WHERE fnr = ?",
        brukerRowMapper,
        fnr
    )

// ❌ Feil – risiko for SQL-injeksjon
fun findBrukerUnsafe(fnr: String): Bruker? =
    jdbcTemplate.queryForObject(
        "SELECT * FROM bruker WHERE fnr = '$fnr'",
        brukerRowMapper
    )
```

## Ingen PII i logg

```kotlin
// ✅ Riktig – logg korrelasjons-ID, ikke PII
log.info("Behandler sak for bruker", kv("sakId", sak.id), kv("tema", sak.tema))

// ❌ Feil – logg aldri FNR, navn eller annen PII
log.info("Behandler sak for bruker ${bruker.fnr}")  // brudd på GDPR
log.info("Navn: ${bruker.navn}")                      // brudd på GDPR
```

## Hemmeligheter fra miljø, aldri hardkodet

```kotlin
// ✅ Riktig – les fra miljøvariabel (Nais injiserer via Secret)
val dbPassword = System.getenv("DB_PASSWORD")
    ?: throw IllegalStateException("DB_PASSWORD mangler")

// ❌ Feil – hardkodet hemmelighet
val dbPassword = "supersecret123"
```

## Nettverkspolicy (Nais)

Eksponer kun det som må eksponeres:

```yaml
spec:
  accessPolicy:
    inbound:
      rules:
        - application: frontend-app      # kun eksplisitt navngitte kallere
    outbound:
      rules:
        - application: pdl-api
          namespace: pdl
          cluster: prod-gcp
      external:
        - host: api.external-service.no  # kun hvis strengt nødvendig
```

## OWASP Top 10-sjekker

For fullstendige kodemønstre per kategori (A01 Broken Access Control, A03 Software
Supply Chain, A04 Cryptographic Failures, A05 Injection, A09 Logging osv.) på tvers
av Kotlin/Go/Java/Node.js, se `$security-owasp`. Denne skillen dekker kun
skanneverktøyene og de repo-generelle sjekkene over.

## Filopplasting-sikkerhet

```kotlin
// ✅ Riktig — valider filtype, størrelse og magic bytes
fun validateUpload(file: MultipartFile) {
    require(file.size <= 10 * 1024 * 1024) { "File too large (max 10 MB)" }
    require(file.contentType in ALLOWED_TYPES) { "Invalid file type" }

    val bytes = file.bytes.take(8).toByteArray()
    require(verifyMagicBytes(bytes, file.contentType!!)) { "File content does not match type" }
}

private val ALLOWED_TYPES = setOf("application/pdf", "image/png", "image/jpeg")
```

## Avhengighetshåndtering

```kotlin
// build.gradle.kts — pin versjoner, bruk BOM
dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.1")
    }
}
```

```bash
# Kotlin – sjekk for utdaterte/sårbare avhengigheter
./gradlew dependencyUpdates
./gradlew dependencyCheckAnalyze   # OWASP-sjekk
trivy repo .

# Node/TypeScript
npm audit
npm audit fix
```

## Sikkerhetssjekkliste

- [ ] Ingen hemmeligheter, tokens eller API-nøkler hardkodet i kildekode
- [ ] Ingen PII (FNR, navn, adresse) i loggutsagn
- [ ] Alle SQL-spørringer bruker parameteriserte statements
- [ ] Nais `accessPolicy` begrenser inbound/outbound til kun det som trengs
- [ ] CORS er begrenset til kjente domener
- [ ] Input er validert og sanert
- [ ] Tilgangskontroll sjekker eierskap (ikke bare auth)
- [ ] Token-validering på alle beskyttede endepunkter (se `@security-champion`)
- [ ] M2M-tokens validerer `azp` mot `AZURE_APP_PRE_AUTHORIZED_APPS`
- [ ] Auth-kode samsvarer med `.nais/` accessPolicy inbound-regler (ingen død kode eller manglende regler)
- [ ] Filopplasting validerer type, størrelse og innhold
- [ ] `trivy repo .` passerer uten HIGH/CRITICAL-funn
- [ ] `zizmor` passerer på alle GitHub Actions-workflows
- [ ] Git-historikk er ren for committede hemmeligheter (`git log`-skann over)
- [ ] HTTPS håndheves – ingen rene HTTP-kall til eksterne tjenester
- [ ] Avhengigheter er oppdaterte og sårbarhetsskannet (`dependencyUpdates` / `npm audit`)
- [ ] Ingen `dangerouslySetInnerHTML` uten sanering

## Relatert

| Ressurs | Bruk til |
|----------|---------|
| `$security-owasp` | OWASP Top 10:2025 kodemønstre per kategori (Kotlin/Go/Java/Node.js) |
| `@security-champion` | Trusselmodellering, compliance-spørsmål, Navs sikkerhetsarkitektur |
| `@auth-agent` | JWT-validering, TokenX, ID-porten, Maskinporten |
| `@nais-agent` | Nais-manifest, accessPolicy, oppsett av hemmeligheter |
| [sikkerhet.nav.no](https://sikkerhet.nav.no) | Navs Golden Path, autoritativ sikkerhetsveiledning |

---
name: codeql-fix
description: Trierer og fikser CodeQL/code-scanning-varsler i bidrag-backend korrekt, med spesielt fokus på at fiksen faktisk bryter CodeQL sin taint-sporing
tools:
  - execute
  - read
  - edit
  - search
  - todo
  - io.github.navikt/github-mcp/get_file_contents
  - io.github.navikt/github-mcp/search_code
  - io.github.navikt/github-mcp/list_code_scanning_alerts
  - io.github.navikt/github-mcp/get_code_scanning_alert
  - io.github.navikt/github-mcp/pull_request_read
  - io.github.navikt/github-mcp/search_pull_requests
  - io.github.navikt/github-mcp/list_commits
---

# CodeQL Fix Agent

Fikser CodeQL/code-scanning-varsler i `bidrag-backend`. Målet er ikke bare å få varselet til å forsvinne fra alert-listen, men å faktisk **bryte dataflyten CodeQL sporer** — ellers dukker varselet opp igjen, eller enda verre: koden ser "fikset" ut for en menneskelig reviewer mens den fortsatt er sårbar.

Denne agenten finnes fordi det samme feilmønsteret har oppstått **flere ganger** i dette repoet.

## Kjerneproblem: boolsk validering ≠ sanitizer for CodeQL

CodeQL sin taint-tracker følger **verdier**, ikke kontrollflyt. Et `require()`/`check()`/`if`-uttrykk som validerer en streng med `.matches()` og deretter kaster en exception hvis den er ugyldig, endrer **ikke hvilket objekt** som brukes videre. Den opprinnelige, tainted strengen flyter fortsatt rett inn i sink-et (URL-bygging, filsti, SQL, prosesskall).

```kotlin
// ❌ CodeQL ser fortsatt jobName som tainted her — require() er ikke en sanitizer
require(jobName.matches(Regex("[\\w\\-]+"))) { "Ugyldig jobName: $jobName" }
val url = restTemplate.getForObject("$baseUrl/job/$jobName") // fortsatt flagget

// ✅ safeJobName er et NYTT objekt, avledet fra matchresultatet — taint brytes
val safeJobName = Regex("[\\w\\-]+").matchEntire(jobName)?.value
    ?: throw IllegalArgumentException("Ugyldig jobName: $jobName")
val url = restTemplate.getForObject("$baseUrl/job/$safeJobName") // ikke lenger flagget
```

**Den eneste sjekken som betyr noe:** se på variabelen som faktisk brukes i sink-et (URL, filsti, spørring). Er det den opprinnelige parameteren, eller en ny verdi avledet fra valideringen? Hvis det er den opprinnelige — fiksen er ufullstendig, uansett hvor riktig valideringslogikken ser ut.

## Andre gyldige sanitizer-mønstre CodeQL anerkjenner

Alle disse produserer en **ny** verdi i stedet for å bare validere den gamle:

```kotlin
// Regex-ekstraksjon (path-segmenter, filnavn, jobbnavn)
val safe = Regex("^[\\w\\-]+$").matchEntire(input)?.value
    ?: throw IllegalArgumentException("Ugyldig verdi: $input")

// Numerisk rundtur (IDer som skal være tall)
val safeId = input.toLongOrNull()?.toString()
    ?: throw IllegalArgumentException("Ugyldig id: $input")

// UUID-parsing
val safeId = UUID.fromString(input).toString()

// Allowlist-oppslag (returner den kjente, trygge verdien — ikke input)
val safeLand = kodeverkService.hentLandkoder()[input]?.also { }
    ?: throw IllegalArgumentException("Ugyldig land: $input")
// merk: her må selve landkoden som brukes videre komme fra oppslaget/en konstant,
// ikke fra input-strengen direkte, selv om oppslaget bekrefter at input er gyldig
```

## Allowlist fremfor denylist

Denylist (`!value.contains("..")`, `!value.contains("/")`) er svakere **og** ofte heller ikke anerkjent som sanitizer av CodeQL:

```kotlin
// ❌ Denylist — bypassbar (URL-encoding, Unicode-varianter, symlinker) og fortsatt tainted
require(!foldername.contains("..") && !foldername.contains("/"))
val path = getPath("$foldername/$template.json") // foldername/template fortsatt originale, tainted verdier

// ✅ Allowlist + matchEntire — både sikrere og taint-brytende
val safeFoldername = Regex("^[\\w\\-]+$").matchEntire(foldername)?.value
    ?: throw IllegalArgumentException("Invalid foldername: $foldername")
val safeTemplate = Regex("^[\\w\\-]+$").matchEntire(template)?.value
    ?: throw IllegalArgumentException("Invalid template: $template")
val path = getPath("$safeFoldername/$safeTemplate.json")
```

## Arbeidsflyt

1. **Hent varselet** — bruk `get_code_scanning_alert`/`list_code_scanning_alerts` for å finne nøyaktig fil, linje og CodeQL-regel (f.eks. `java/ssrf`, `java/path-injection`, `java/xxe`, `java/sql-injection`).
2. **Spor kilden til sink** — les koden fra der brukerinput kommer inn (controller/consumer-parameter) til der den brukes i sink-et. Identifiser nøyaktig hvilken variabel som treffer sink-et.
3. **Velg riktig sanitizer-mønster** — se tabellene over. Foretrekk allowlist/regex-ekstraksjon eller typet parsing (Long/UUID) fremfor boolsk validering.
4. **Skriv fiksen slik at sink-et bruker DEN NYE, avledede verdien** — ikke bare valider og gjenbruk originalparameteren.
5. **Grep etter søsken-instanser** av samme mønster i andre metoder/klasser som ikke nødvendigvis er flagget ennå.
6. **Legg til/oppdater en test** som verifiserer at ugyldige verdier (`../`, `/`, null-bytes, altfor lange strenger) kaster exception, og at gyldige verdier fortsatt fungerer.
7. **Skriv PR-beskrivelse i Before/After-format** (se historikk-eksemplene over) — dette har vist seg nyttig for reviewere i dette repoet og bør videreføres.
8. **Ikke anta at varselet er løst kun fordi build/tester er grønne** — CodeQL kjører kun i den planlagte `codeql.yml`-workflowen (cron, ikke per PR i dag) eller ved `workflow_dispatch`. Foreslå en manuell `workflow_dispatch`-kjøring eller nevn eksplisitt i PR-beskrivelsen at varselet bør reverifiseres etter merge.

## Relatert

| Type | Navn | Bruk til |
|------|------|----------|
| Skill | `$security-review` | Bredere sikkerhetssjekk før commit/push (secrets, deps, workflows) |
| Skill | `$security-owasp` | OWASP Top 10-referanse for Kotlin/Java-mønstre |
| Skill | `$threat-model` | Arkitektur-/trusselmodellering på et høyere nivå |

## Grenser

### ✅ Alltid

- Verifiser at sink-et faktisk bruker en *avledet* verdi fra valideringen, ikke den opprinnelige parameteren
- Foretrekk allowlist (`^[\w\-]+$` e.l.) fremfor denylist (`!contains("..")`)
- Grep etter lignende sink-mønstre i samme fil/klasse før du markerer et alert som fullstendig løst
- Skriv/oppdater en test som dekker både avvist og godkjent input
- Skriv PR-beskrivelse med konkret Before/After-kodeeksempel for det aktuelle alertet

### ⚠️ Spør først

- Endre offentlig funksjonssignatur (parametertype) for å tvinge frem en trygg type (f.eks. bytte `String` til en egen `SafePathSegment`-verdiklasse)
- Fjerne eksisterende, fungerende validering fremfor å legge til/erstatte med en sterkere variant

### 🚫 Aldri

- Marker et alert som løst kun basert på at en `require()`/`check()`-boolsk sjekk ble lagt til uten at sink-verdien er byttet ut
- Bruk denylist-validering (`!contains(...)`) som eneste forsvar mot path traversal/SSRF
- Slett eller svekk eksisterende sikkerhetstester for å få en fiks til å bygge
- Anta at et alert er verifisert løst uten at CodeQL faktisk har kjørt på nytt mot endringen

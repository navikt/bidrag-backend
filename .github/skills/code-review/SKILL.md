---
name: code-review
description: Kodegjennomgang for bidrag-backend – pom.xml-disiplin, sikkerhet iht. sikkerhet.nav.no, workflow-konsistens og testdekning
license: MIT
compatibility: navikt/bidrag-backend (Kotlin, Spring Boot, PostgreSQL, Maven-monorepo)
metadata:
  domain: backend
  tags: code-review pom-xml maven security nav github-actions kotlin spring-boot postgres
---

# Kodegjennomgang for bidrag-backend

Denne skillen brukes til å gjennomgå kode i `bidrag-backend`-monorepoet før commit, push
eller pull request. Den er skrevet spesifikt for dette repoet og bygger videre på
mønstrene som allerede er etablert.

Bruk denne skillen når du:

- skal godkjenne eller kommentere på en pull request i `bidrag-backend`,
- legger til eller endrer en avhengighet i en `pom.xml`,
- oppretter eller endrer en GitHub Actions workflow under `.github/workflows/`,
- er usikker på om ny kode følger sikkerhetskravene fra Nav.

For dypere arkitekturbeslutninger, bruk `@nav-pilot`/`$nav-plan` i stedet. For generell
OWASP-sjekk av kode, se `$security-owasp`. Denne skillen er det repo-spesifikke laget
oppå de generelle skillene.

## 1. Standard kodestack

Standard stack i `bidrag-backend` er **Kotlin + Spring Boot + PostgreSQL**, bygget med
Maven i et flernivås monorepo (root-`pom.xml` som parent for alle apper under `apps/`).

- Nye apper/moduler skal følge denne stacken med mindre det finnes en god, dokumentert
  grunn til noe annet.
- Avvik (annet språk, annet rammeverk, annen database) skal:
  - begrunnes eksplisitt i PR-beskrivelsen, og
  - helst diskuteres med teamet/arkitektur før det implementeres, ikke oppdages i review.
- Java tillates der det er en **eksisterende** avhengighet som er skrevet i Java
  (f.eks. joint Java/Kotlin-kompilering i `bidrag-dokument-arkiv`), men nye filer skal
  som hovedregel skrives i Kotlin.

## 2. pom.xml-disiplin

Dette er trolig det viktigste og mest kontinuerlige sjekkpunktet i denne skillen, siden
det er lett å innføre unødvendig duplisering i et Maven-monorepo med mange moduler.

### Hovedregel

**Versjoner skal eies av den enkelte app-/lib-pom, ikke av root-`pom.xml`.** Root-pomen
skal kun beholde de få property-ene den trenger for sin egen build (`java.version`,
`kotlin.version`, `ktlint.version`, `versions-maven-plugin.version`). All annen
avhengighetsversjonering (Nav-moduler, tredjeparts-biblioteker, test-rammeverk osv.)
skal defineres direkte i hver app-/lib-pom som faktisk bruker avhengigheten – selv om
flere apper bruker samme versjon (dupliser da property-en i hver av dem).

Dette er en bevisst reversering av den tidligere «arv fra root»-regelen. Bakgrunnen er
at root-`pom.xml` er en aggregator med `<modules>` for samtlige ~43 undermoduler: en
versjonsendring i en property i root sin `<properties>`-seksjon trigget tidligere
rebuild av *alle* apper i CI, selv om kanskje bare halvparten faktisk brukte den
endrede avhengigheten. Ved å flytte versjonene ut til hver enkelt app-/lib-pom påvirker
en versjonsendring kun de appene som faktisk endres (og som CI sin `detect_changes`
dermed korrekt oppdager som endret).

- Ikke legg til en ny avhengighetsversjon i root-pom sin `<properties>` – den hører
  hjemme i app-/lib-pomen som bruker den.
- Alle versjonerte properties i en app-/lib-pom skal ligge samlet **øverst** i
  `<properties>`-blokken, ikke spredt utover fila – dette gjør det raskt å se i en
  review nøyaktig hvilke versjoner appen selv eier ansvaret for.
- Hvis flere apper trenger samme versjon av en avhengighet, dupliser property-en i hver
  av dem med samme verdi – ikke løft den opp i en delt/mellomliggende pom for å "spare"
  linjer. Unntaket er properties/plugins root-pomen selv trenger for sin egen build.
- Hver property bør faktisk brukes et sted (sjekk med `grep` i `src/` og pom.xml selv).
  Fjern properties som ikke lenger refereres til noe sted (både i root og i app-/
  lib-pomer).

### Unntak: overstyring av transitive avhengigheter

Noen ganger er det nødvendig å tvinge en spesifikk versjon av en **transitiv**
avhengighet via `<dependencyManagement>` – typisk for å lukke et sikkerhetshull i en
avhengighet du ikke kontrollerer direkte, eller for å unngå en kjent
inkompatibilitet mellom to biblioteker (f.eks. at et testbibliotek som `springmockk`
transitivt drar inn en eldre versjon av `mockk-dsl-jvm` enn den appen ellers bruker
direkte – se `mockk.version`-overstyringen i f.eks. `bidrag-behandling/pom.xml` og
`bidrag-aktoerregister/pom.xml` for et konkret eksempel). Dette er et dokumentert,
akseptert unntaksmønster – se Nav sin egen veiledning på
[sikkerhet.nav.no/docs/sikker-utvikling/tredjepartskode](https://sikkerhet.nav.no/docs/sikker-utvikling/tredjepartskode),
som viser nøyaktig dette mønsteret for Maven:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.example</groupId>
      <artifactId>sarbar-transitiv-avhengighet</artifactId>
      <version>2.5.1</version> <!-- Tvinger patched versjon, se CVE-XXXX-YYYY -->
    </dependency>
  </dependencies>
</dependencyManagement>
```

Siden versjonering nå eies av den enkelte app-/lib-pom (se hovedregelen over), skal en
slik `<dependencyManagement>`-overstyring **også** ligge i app-/lib-pomen som faktisk
trenger den – ikke løftes til root – selv om det betyr at flere apper ender opp med en
identisk overstyringsblokk. Legg den rett under `<properties>` og bruk appens egen
lokale versjons-property i `${...}`-referansen.

Krav når dette unntaket brukes:

- **Begrunn overstyringen med en kommentar** i pom.xml (CVE-nummer, kompatibilitetsgrunn,
  eller lenke til issue) – ikke la en fremtidig leser gjette hvorfor den er der.
- **Verifiser at overstyringen faktisk gjør noe.** Kjør
  `mvn dependency:tree -Dincludes=<groupId>:<artifactId>` og bekreft at versjonen som
  faktisk brukes endres av overstyringen din. En overstyring som ikke endrer noe reelt
  (fordi transitivt treff allerede gir samme versjon) er bare støy og skal fjernes. Merk
  at dette må sjekkes **per app** – samme avhengighet kan trenge overstyring i én app
  (fordi den mangler en konkurrerende direkte avhengighet på samme artefakt) og være
  unødvendig i en annen (fordi appen allerede har en direkte avhengighet som vinner via
  Mavens nearest-wins-mediering). Ikke kopier en overstyring blindt til alle apper som
  bruker den samme avhengigheten – test og legg den kun til der den faktisk endrer noe.
- **Kjør alltid full test-suite (`mvn clean test`, gjerne `mvn verify`) etter enhver
  avhengighetsendring** – både etter at en overstyring legges til, og etter at en fjernes.
  `dependency:tree`-analyse alene er **ikke tilstrekkelig**: en importert BOM (f.eks.
  `graphql-dgs-platform-dependencies`) kan ha sin egen `dependencyManagement`-oppføring
  for en transitiv avhengighet som vinner over det den transitive grafen «naturlig» ville
  gitt, med den konsekvens at fjerning av en tilsynelatende overflødig eksplisitt versjon
  faktisk endrer hvilken versjon som brukes i praksis. Dette skjedde konkret under
  opprydding i `bidrag-dokument-arkiv/pom.xml`: fjerning av en eksplisitt
  `json-path`-versjon så ut til å være trygt basert på `dependency:tree`, men brakk
  10+ tester i kjøretid fordi BOM-en sin egen (eldre) pin vant i stedet. Rett feil ble
  oppdaget kun fordi full testkjøring ble kjørt etterpå – ikke stol på statisk
  tre-analyse alene.

### Sjekkliste for pom.xml i review

- [ ] Ingen ny avhengighetsversjon er lagt til i root-pom sin `<properties>` – den skal
      ligge i app-/lib-pomen som bruker den, selv om flere apper trenger samme verdi.
- [ ] Alle versjonerte properties i app-/lib-pomen ligger samlet øverst i `<properties>`.
- [ ] Alle properties er faktisk i bruk (ingen død kode/versjon), både lokalt og i root.
- [ ] Eventuelle `dependencyManagement`-overstyringer ligger i app-/lib-pomen som trenger
      dem (ikke i root), har en forklarende kommentar, og er verifisert med
      `dependency:tree` for **den spesifikke appen** – ikke antatt nødvendig andre steder.
- [ ] `mvn clean test` (eller `verify`) er kjørt og er grønn etter endringen – ikke bare
      `dependency:tree`.

## 3. Sikkerhet (sikkerhet.nav.no)

Følgende sjekkpunkter er hentet direkte fra
[sikkerhet.nav.no/docs/sikker-utvikling](https://sikkerhet.nav.no/docs/sikker-utvikling/)
og tilpasset til hva som er relevant for et Kotlin/Spring Boot/Postgres-backend som
`bidrag-backend`.

### Hemmeligheter

- Ingen hemmeligheter (passord, API-nøkler, private nøkler, tokens) skal være
  hardkodet i kildekode, testdata eller konfigurasjonsfiler som sjekkes inn.
- Hemmeligheter skal komme fra miljøvariabler/Nais-secrets (injisert av plattformen),
  aldri fra filer i repoet.
- Sjekk at nye konfigurasjonsfiler ikke ved et uhell committer lokale `.env`-filer
  eller IDE-credential-filer.

### Input- og outputvalidering

- All ekstern input skal valideres – også data fra tjenester du stoler på
  (andre Nav-team, eksterne integrasjoner).
- Bruk **parameteriserte spørringer** mot PostgreSQL (JPA/Hibernate-parametre,
  `NamedParameterJdbcTemplate` e.l.) – aldri strengkonkatenering av brukerinput inn i SQL.
- Enkod output riktig i konteksten den brukes i (HTML, JSON, logg).

### Logging – ingen PII i åpen logg

- Fødselsnummer, navn, adresse, aktørId og annen personinformasjon skal **aldri**
  logges i vanlig applikasjonslogg. Bruk secure/team-logs der sensitiv informasjon
  faktisk må logges.
- Pass spesielt på at FNR eller andre identifikatorer ikke havner i URL-er
  (path/query-parametre) eller headere som logges av mellomliggende komponenter
  (load balancer, gateway, APM-verktøy).
- Bruk saks-/vedtaks-ID i logglinjer i stedet for personopplysninger når du skal
  spore opp en konkret sak.

### Tilgangsstyring

- Bruk OAuth2/OIDC med smalt scopede tokens.
- Der brukerkontekst finnes, bruk Token Exchange (TokenX) i stedet for en generisk
  systembruker – dette bevarer sporbarheten til den faktiske brukeren gjennom
  hele kallkjeden.
- `accessPolicy` i nais.yaml skal være eksplisitt (inbound/outbound), ikke åpne bredere
  enn nødvendig.

### Maskin-til-maskin (M2M)

- Bruk OAuth2 Client Credentials Flow for system-til-system-kall uten brukerkontekst.
- Ikke bruk langlivede, statiske passord for servicebrukere der en
  plattform-utstedt, kortlivet client credentials-flyt er tilgjengelig.

### Tredjepartskode og supply chain

- Vurder nye avhengigheter før de legges til: aktivt vedlikeholdt, kjente sårbarheter,
  faktisk behov (unngå avhengigheter som kun brukes for en liten hjelpefunksjon).
- Dependabot skal være aktivt og oppdatere avhengigheter jevnlig (med cooldown for
  helt ferske releaser, jf. sikkerhet.nav.no sin anbefaling om minimum release-alder).
- Se pom.xml-seksjonen over for hvordan transitive avhengigheter skal håndteres.

## 4. GitHub Actions – workflow-konsistens

`bidrag-backend` har allerede et svært konsistent oppsett på tvers av alle apper:
én workflow-fil per app (f.eks. `bidrag-aktoerregister.yaml`) som delegerer til delte,
gjenbrukbare workflows: `bygg_og_deploy.yaml`, `bygg_og_deploy_prod.yaml`, i tillegg til de
repo-globale `codeql.yml`, `dependabot_bygg_og_test.yaml`, `tag_utgivelse.yaml`.

**Ikke innfør nye, avvikende mønstre uten god grunn.** En ny app-workflow skal normalt
kun bestå av jobbene:

- `detect_changes` (bruker `.github/actions/utled-app-endringer` til å avgjøre om appen
  faktisk har en reell diff mot `main` siden merge-base, uavhengig av eventuelle
  merge-commits inn i branchen — se begrunnelse under)
- `bygg_og_test`
- `deploy_prod` (delegerer til `bygg_og_deploy_prod.yaml`)
- `deploy_q1` / `deploy_q2` (delegerer til `bygg_og_deploy.yaml`)

Alle jobber utover `detect_changes` skal ha `needs: detect_changes` og
`if: needs.detect_changes.outputs.changed == 'true' && (...)` (AND'et med den eksisterende
`if`-betingelsen), slik at bygg/deploy skippes når appen ikke faktisk er endret.

**Historikk:** Et tidligere forsøk på et beslektet problem var
`sjekk_sync_med_main.yaml`, som *krevde* at branchen hadde main som ancestor (blokkerte
deploy til Q1/Q2 med feilmelding hvis ikke). Denne ble fjernet igjen (commit
"Fjerner sync med main") — trolig fordi den løste feil problem: den tvang fram sync med
main i stedet for å håndtere at en *reell* diff-sjekk mot main var det som egentlig
manglet. `detect_changes`/`utled-app-endringer`-mønsteret erstatter denne løsningen med en
presis merge-base-diff i stedet for et hardt sync-krav.

Avvik (f.eks. at en app kjører på FSS i stedet for GCP, eller har ressurser i begge
miljøer) skal begrunnes eksplisitt og ikke bare
gjøres stille i workflow-filen. Dobbeltsjekk `nais_cluster`-verdien mot en tilsvarende,
allerede migrert app i samme kategori (f.eks. andre apper under
`bidrag-dokumenthåndtering`) før du antar hvilket miljø som er riktig.

Sikkerhetskrav til selve workflow-filene (jf.
[sikkerhet.nav.no/docs/sikker-utvikling/github](https://sikkerhet.nav.no/docs/sikker-utvikling/github)):

- Tredjeparts Actions skal pinnes til **commit-SHA**, ikke floating tags
  (`actions/checkout@<sha> # v7`, ikke `actions/checkout@v7`) – dette er allerede
  konsekvent gjort i repoet og skal videreføres.
- `permissions:` skal settes eksplisitt og minimalt per job – ikke bruk bred,
  implisitt `write`-tilgang.
- Unngå `pull_request_target` med kjøring av PR-ens egen kode. Bruk `pull_request`,
  eller splitt ut privilegerte steg i en egen jobb som trigges av `workflow_run` og
  kun leser artefakter – aldri kjører kode fra PR-en direkte.
- Bruk mellomliggende `env:`-variabler for alt som stammer fra
  `${{ github.event.* }}` og brukes i et `run:`-steg, i stedet for å interpolere
  direkte i shell-scriptet (unngår script-injeksjon).
- Maven-cache skal bruke smal, per-app cache-nøkkel
  (`hashFiles('pom.xml', 'apps/<app>/pom.xml')`), ikke `setup-java` sin innebygde
  `cache: maven`, som ville blitt invalidert av enhver pom.xml-endring hvor som helst
  i monorepoet.
- Kjør `zizmor .github/workflows/` (se `$security-review`) for å fange opp
  usikre mønstre før merge.

## 5. Tester

- Ny og/eller endret kode skal ha tilhørende tester (enhets- og/eller
  integrasjonstester, avhengig av hva som er endret).
- `mvn verify` (som inkluderer ktlint) skal være grønn før PR merges.
- For endringer i eksisterende kode uten testdekning fra før: legg til minst en
  characterization-test som fanger dagens oppførsel før du endrer den.
- Ved avhengighetsendringer i pom.xml: full `mvn clean test` skal kjøres og være
  grønn – se pom.xml-seksjonen over for hvorfor dette ikke er valgfritt.

## 6. Sjekkliste – kort oppsummering

- [ ] Kodestack er Kotlin/Spring Boot/PostgreSQL, eller avvik er eksplisitt begrunnet.
- [ ] Ingen ny avhengighetsversjon lagt til i root-pom – hører hjemme i app-/lib-pomen.
- [ ] Versjonerte properties i app-/lib-pomen ligger samlet øverst og er alle i faktisk bruk.
- [ ] Overstyringer av transitive avhengigheter har forklarende kommentar og er
      verifisert med `dependency:tree` **og** full testkjøring.
- [ ] Ingen hemmeligheter i kode/config.
- [ ] Ingen PII i åpen applikasjonslogg.
- [ ] Parameteriserte SQL-spørringer, ingen strengkonkatenering av brukerinput.
- [ ] Tilgangsstyring bruker riktig token-flyt (TokenX ved brukerkontekst,
      Client Credentials for ren M2M).
- [ ] GitHub Actions-workflow følger standardmønsteret (`detect_changes` →
      `bygg_og_test` → `deploy_prod`/`deploy_q1`/`deploy_q2`), actions pinnet
      til SHA, minimum permissions.
- [ ] Ny/endret kode har tilhørende tester, `mvn verify` er grønn.

## Relaterte ressurser

| Ressurs | Bruk til |
|---|---|
| `$security-review` | Generell pre-commit sikkerhetssjekk (trivy, zizmor, secret-scan) |
| `$security-owasp` | OWASP Top 10:2025-referanse for Kotlin/Java-kode |
| `$postgresql-review` | Dypere gjennomgang av SQL-spørringer og indeksering |
| `@security-champion` | Trusselmodellering og mer omfattende sikkerhetsvurdering |
| `@nav-pilot` / `$nav-plan` | Arkitekturbeslutninger som går utover enkeltstående PR-review |
| [sikkerhet.nav.no/docs](https://sikkerhet.nav.no/docs/) | Nav sin fullstendige sikkerhetsveiledning |
# Migrering av bidrag-tilgangskontroll til monorepoet

Dokumenterer endringene som ble gjort for å migrere `bidrag-tilgangskontroll` inn i
`bidrag-backend`-monorepoet, i tråd med de andre appene. Ikke commit endringene. 

## 1. pom.xml

- Byttet parent fra frittstående `org.springframework.boot:spring-boot-starter-parent`
  til `no.nav.bidrag:bidrag-backend` (`pom.xml`). Noen av appene slik som bidrag-admin og bidrag-dokumenthåndtering har et 
  ekstra nivå av pom.xml filer og skal legges der.
- Fjernet lokale properties/versjoner som nå arves fra root-pom sine felles properties:
  - `token-support.version` → `nav-token-support.version`
  - `unleash.version` → `unleash-client-java.version`
  - `springframework-cloud.version` → `spring-cloud-contract-wiremock.version`
  - `wiremock.version` → `wiremock-spring-boot.version`
  - `mockk.version` brukt feilaktig for `springmockk` → rettet til `springmockk.version`
  - `springdoc-openapi-ui.version`, `kotlin-logging-jvm.version`, `logback-encoder.version`,
    `kotest.version`, `bidrag-felles.version`, `kotlin.version`, `java.version` fjernet lokalt
    (arves fra root)
- Om det er særegne avhengigheter som ikke ser ut til å brukes av andre apper kan de 
  ligge i innerste pom.xml men da skal verjsoneringen av alt som overstyres/er særegent ligge på toppen under properties.
- Fjernet `tomcat-embed-core`- og `snakeyaml`-overstyring i `dependencyManagement`
  (allerede samme versjon i root sin Spring Boot 4.1.0 BOM).
- Fjernet lokal `<repositories>`, `<profiles>` (OSX-profil) og `<build>`
  (kotlin-maven-plugin, spring-boot-maven-plugin, ktlint) — alt arves nå fra root-pom.
- Fjernet eksplisitt `<version>` (arver `1.0.0-SNAPSHOT` fra parent).
- Lagt til `apps/bidrag-tilgangskontroll` i `<modules>` i root `pom.xml`.
- Migrer io.github.microutils:kotlin-logging-jvm over til io.github.oshai om det ikke er gjort allerede

Sjekk at resterende avhengigheter faktisk er i bruk, enten direkte eller via en transitiv avhengighet. 
Alle avhengigheter som skal være i pom.xml med egen versjoner skal ALLTID ha versjoneringen liggende øverst i filen under properties.

Verifisert med `mvn -pl apps/bidrag-tilgangskontroll -am test` — alle 27 tester grønt.

## 2. Nais-filer

Flyttet fra `apps/bidrag-tilgangskontroll/.nais/` til `.nais/bidrag-tilgangskontroll`
på root, i tråd med de andre appene:

| Gammel fil  | Ny fil      | Merknad |
|-------------|-------------|---------|
| `nais.yaml` | `nais.yaml` | Uendret innhold |
| `feature.yaml` | `q1.yaml` | Uendret innhold (app-navn/ingress rørt ikke, kun filnavn) |
| `main.yaml` | `q2.yaml` | Uendret innhold (app-navn/ingress rørt ikke, kun filnavn) |
| `prod.yaml` | `prod.yaml` | Uendret innhold |
| `unleash.yaml` | `unleash.yaml` | Uendret innhold |

App-navn og ingress i disse filene er **ikke** endret, siden de er bundet til
eksisterende, kjørende NAIS-ressurser (Application/ApiToken). Kun filsti/filnavn er
endret for å matche konvensjonen brukt i `.nais/bidrag-aktoerregister` m.fl.
Behold hvilken nais_cluster som er satt i hver fil, siden det er riktig for appen.

## 3. GitHub Actions workflow

Erstattet det gamle, frittstående workflow-oppsettet
(`apps/bidrag-tilgangskontroll/.github/workflows/{deploy_q1,deploy_q2,deploy_prod,rollback_prod}.yaml`,
som kalte gjenbrukbare workflows i `navikt/bidrag-workflow`) med én fil:
`.github/workflows/bidrag-tilgangskontroll.yaml`.

Ny workflow er skrevet i samme format som `bidrag-belopshistorikk.yaml`/
`bidrag-aktoerregister.yaml` (dette er nå det etablerte mønsteret for **alle** apper i
monorepoet — de gamle beskrivelsene med `bygg_og_test`/`sjekk_sync_med_main`/separate
branch-triggere for q1/q2 og `bygg_og_deploy_prod.yaml` finnes ikke lenger noe sted):

- `on`: `workflow_dispatch` (velg `miljo`: `prod`/`q1`/`q2`) og `push` på **alle**
  branches (`'**'`, unntatt `dependabot/**`) — ikke lenger egne `q1/**`/`q2/**`-
  branch-triggere, og ikke lenger noen `pull_request`-trigger. Push filtreres på
  `paths` (appens `apps/**` og `.nais/<app>/**.yaml`) og gir alltid bygg+test-
  feedback; deploy til q1/q2 for en feature-/PR-branch skjer utelukkende via
  manuell `workflow_dispatch` (tidligere PR-labels `q1`/`q2` fjernet — ga
  redundante rebuilds av samme commit, se historikk under).
- `detect_changes` — kjører `./.github/actions/utled-app-endringer`, som regner ut den
  faktiske diffen mot `main` (merge-base) fremfor GitHubs innebygde push-diff. Dette
  hindrer at en `main`-merge inn i en branch trigger bygg/deploy for apper som ikke
  faktisk er endret på branchen.
- `bygg_test_og_deploy` — `needs: detect_changes`, samme
  `if: needs.detect_changes.outputs.changed == 'true' && github.actor != 'dependabot[bot]'`
  som tidligere satt på en egen `finn_miljo`-jobb. Denne jobben (og den
  underliggende `./.github/actions/finn-deploy-miljo`-actionen) er fjernet —
  logikken var kun tre trivielle boolske uttrykk uten behov for en egen jobb
  eller `gh`-oppslag, og beregnes nå direkte i `with:`-blokken:
  `deploy_q1`/`deploy_q2`: `github.event_name == 'workflow_dispatch' && inputs.miljo == 'q1'`/`'q2'`.
  `deploy_prod`: samme dispatch-sjekk for `'prod'`, **eller** push til `main`.
  Ett enkelt kall til den gjenbrukbare `.github/workflows/bygg_og_deploy.yaml`
  (ingen egen `bygg_og_deploy_prod.yaml` lenger — samme fil håndterer
  q1/q2/prod). Øvrige sentrale inputs: `nais_hovedfil_navn`,
  `nais_variabler_filnavn_q1`/`_q2`/`_prod`, `maven_options` (typisk
  `-B -fae -pl apps/<app> -am`), `maven_cache_paths` (root-`pom.xml` + appens
  egen `pom.xml`, for en presis Maven-cache-nøkkel i stedet for å hashe alle
  pom.xml i monorepoet), `ktlint_paths`, `docker_context` og
  `image_suffix`/`tag`.

Selve `bygg_og_deploy.yaml` bygger/tester/lager Docker-image i én jobb (kjører alltid,
også på PR-er, for rask feedback), signerer/attesterer image (SBOM via `salsa`-jobben),
og har deretter tre **egne** `deploy_q1`/`deploy_q2`/`deploy_prod`-jobber som hver kun
kjører når kalleren faktisk ba om det miljøet (`if: inputs.deploy_q1` osv.) — ekte
`needs`-gating, ikke branch-navn-baserte `if`-betingelser som konkurrerte om å starte
samtidig slik det var i det gamle oppsettet. Ved vellykket prod-deploy trigges i tillegg
`tag_utgivelse.yaml` for å tagge en utgivelse.

`docker_context` peker på `./apps/bidrag-tilgangskontroll`, og `dockerfile_with_path`
er **ikke** satt — bruker dermed default (root sin `Dockerfile`), se punkt 4.

Gammel `apps/bidrag-tilgangskontroll/.github/` (inkl. egen `dependabot.yml`) er slettet.
Root sin `.github/dependabot.yml` dekker allerede `/apps/*`.

**⚠️ Åpent spørsmål — unleash-deploy:** Det gamle oppsettet hadde egne
`deploy_unleash_prod`/`_q1`/`_q2`-jobber som deployet `unleash.yaml`
(ApiToken-ressursen) separat, etter mønster fra `bidrag-automatisk-jobb.yaml`. Ingen av
de nye app-workflowene (inkl. `bidrag-automatisk-jobb.yaml`, som fortsatt har en
`.nais/bidrag-automatisk-jobb/unleash.yaml`-fil liggende) deployer lenger `unleash.yaml`
i det hele tatt — det finnes ingen `unleash`-referanser igjen i noen workflow, og ingen
commit i historikken forklarer hvorfor jobben ble fjernet (trolig falt den bort under
den historieløse migreringen av eksisterende apper). `bidrag-tilgangskontroll` har også
en `.nais/bidrag-tilgangskontroll/unleash.yaml`. **Avklar med teamet før migrering** om
unleash-ApiToken for denne appen (a) skal deployes manuelt/allerede er deployet og ikke
endres, eller (b) bør legges til på nytt som en input/jobb i `bygg_og_deploy.yaml` —
ikke anta at det ene eller andre er riktig uten bekreftelse.

## 4. Dockerfile

`apps/bidrag-tilgangskontroll/Dockerfile` var innholdsmessig identisk med root sin
`Dockerfile` (kun forskjell: `ubuntu:24.04` vs. `ubuntu:26.04` og store/små bokstaver på
`AS`). Slettet lokal Dockerfile — appen bygges nå med root sin `Dockerfile` og
`docker_context: ./apps/bidrag-tilgangskontroll` i workflowen, slik som de fleste andre
appene (aktoerregister, belopshistorikk, samhandler, reskontro, regnskap, statistikk,
person-hendelse, grunnlag).
Behold dockerfile om den innholder spesifikk entry-point. Slik som bidrag-sak.

## 5. .gitignore

`apps/bidrag-tilgangskontroll/.gitignore` slettet. Ingen andre apper har en egen
`.gitignore` — root sin `.gitignore` gjelder for alle moduler i monorepoet.
Kan også fjerne editorconfig, codeowners og license om det finnes også.

## 6. README.md

- Fjernet de to gamle badge-lenkene til `continuous integration` og
  `release bidrag-template-spring` (pekte til separate repo sine GitHub Actions, som
  ikke lenger finnes).
- Oppdatert kommandoen under "Test nais.yaml implementation" til å peke på ny
  `.nais/bidrag-tilgangskontroll`-plassering og kjøres fra rotmappen til monorepoet.

## 7. Blandet Java/Kotlin i src/main (spesialtilfelle)

Enkelte apper (f.eks. `bidrag-dokument-arkiv`) har ekte `.java`-filer i tillegg til
Kotlin-filer under det som etter migrering blir `src/main/kotlin`/`src/test/kotlin`
(Maven/Kotlin-plugin tillater `.java`-filer under `src/main/kotlin`). Root sin
nedarvede `kotlin-maven-plugin` binder `compile`/`test-compile`-eksekveringene til
Maven-fasen `compile` — samme fase som javac sin `default-compile`. Hvis Java-filer
refererer til symboler definert i Kotlin (eller omvendt), kan javac kjøre før Kotlin
er kompilert, og bygget feiler med «cannot find symbol».

**Løsning**: legg til en lokal `<build><plugins>`-overstyring i appens egen `pom.xml`
som redeklarerer `kotlin-maven-plugin` sine eksekveringer med samme `<id>`
(`compile`, `test-compile`) men ny `<phase>` (`process-sources` og
`process-test-sources`, dvs. før javac). Maven slår sammen plugin-eksekveringer fra
parent/child basert på `<id>`, så all annen konfigurasjon (compiler-plugins,
jvmTarget, allopen/noarg-args) arves fortsatt fra root — kun fasen overstyres.

Sjekk også om noen av Java-filene kaller en logger migrert til `io.github.oshai`
(f.eks. en delt `SECURE_LOGGER`). Oshai sin `KLogger` er Java-kallbar via SAM-konvertering,
men syntaksen må skrives om til lambda-form fra Java:
`SECURE_LOGGER.info(() -> "melding")` og `SECURE_LOGGER.warn(e, () -> "melding")`
(Throwable først, så lambda, for overload med exception).

Merk også at ikke alle loggere nødvendigvis skal migreres til oshai — sjekk om
`LOGGER`-feltet faktisk er en oshai `KLogger` eller en vanlig SLF4J `Logger`
(`LoggerFactory.getLogger(...)`) før du skriver om kallene til lambda-syntaks. Kun
oshai sine `KLogger`-kall skal ha lambda-form (`LOGGER.info { "..." }`); vanlige
SLF4J-kall skal stå uendret.

## 8. Fødselsnummer og annen PII hardkodet i testkode

Ved migrering av en ny app inn i monorepoet skal testkoden også ryddes for
hardkodede fødselsnummer, kontonummer og lignende identifikatorer. Bruk følgende
regex for å søke gjennom repoet (fanger opp gyldige fnr/d-nummer/b-nummer-format):

```
.*(?<!\d)(0[1-9]|[12]\d|3[01]|4[1-9]|5\d|6\d|7[01])(0[1-9]|1[0-2]|2[1-9]|3[0-2])\d{2}\d{3}\d{2}(?=[ "]|$)
```

**OBS**: `git ls-files` hopper stille over filer med ikke-ASCII-tegn i filstien
(f.eks. `bidrag-dokumenthåndtering`) med mindre den kjøres med
`git -c core.quotepath=false ls-files`. Bruk alltid dette flagget ved scanning av
repoet, ellers vil hele appfamilier med æøå i stien bli oversett.

### Fremgangsmåte

1. Søk kun i `src/test/**`. **Produksjonskode skal ikke endres** — flagg eventuelle
   treff der (f.eks. en fallback-verdi i en consumer) til bruker for eksplisitt
   avklaring i stedet for å endre den.
2. For hvert treff, avgjør om det faktisk er et fødselsnummer/identifikator for en
   person (feltnavn som `ident`, `gjelder`, `personId`, `fnr`, `barnIBehandling` osv.),
   eller om det er en tilfeldig regex-match på et annet forretnings-ID-felt
   (`saksnummer`, `journalpostId`, `dokumentreferanse` o.l.). Sjekk feltnavn og type
   der verdien brukes — ikke anta ut fra regex-treff alene.
3. Ekte fødselsnummer-literals erstattes med `genererFødselsnummer()` fra
   `no.nav.bidrag.generer.testdata.person` (krever `bidrag-backend-commons-test` som
   testavhengighet — sjekk at appens `pom.xml` allerede har denne).
   - `const val` må endres til `val` siden funksjonskall ikke er en
     kompileringstidskonstant.
   - Hvis samme verdi brukes både til å bygge testdata **og** i en separat
     assertion (f.eks. JSON-body og en hardkodet assert-streng), må det innføres en
     delt `val` (lokal, klasse- eller companion-nivå) som brukes begge steder —
     regenerer aldri verdien uavhengig flere steder, da brytes identitets-matchen.
   - For JSON-testfixtures: bytt ut literalen med en unik placeholder-token
     (f.eks. `SKYLDNER`, `KRAVHAVER1`) i `.json`-filen, og kjed
     `.replace("\"TOKEN\"", "\"${genererFødselsnummer()}\"")`-kall i testens
     fil-lesehjelpefunksjon — se `VedtakshendelseListenerIT.leggInnGenererteIdenter()`
     og `VedtakControllerTest.lesFilOgByggRequest()` for etablert mønster.
4. Kontonummer-literals erstattes med
   `genererKontonummer().norskKontonummer(true).opprett().norskKontonummer!!` fra
   `no.nav.bidrag.generer.testdata.konto`.
   - Skal testen dekke et *ugyldig* kontrollsiffer, skal det **ikke** hardkodes en
     rå 11-sifret streng. Generer et gyldig kontonummer og ødelegg kontrollsifferet
     (siste siffer) med `.replaceRange(...)`, se
     `KontonummerUtilsTest.medUgyldigKontrollsiffer()`.
5. Bevisst BNR/NPID-teststruktur (måned+20/+30, brukes for å teste at koden skiller
   BNR/NPID fra ordinære fnr, jf. `GrunnlagUtil.erBnrEllerNpid()`) skal **ikke**
   hardkodes. Generer et ekte fødselsnummer og bytt ut 3. siffer (første siffer i
   månedsdelen) med `2`/`3` slik at månedsverdien havner i 21-32 — se
   `TestUtil.genererBnrEllerNpid()` i `bidrag-grunnlag`. Velg `2` eller `3` dynamisk
   ut fra det opprinnelige andre månedssifferet, slik at resultatet alltid havner i
   gyldig intervall (måned `10` gir f.eks. `20` med `2`, som er ugyldig — bruk `3`
   for `30` i stedet).
6. Forretnings-ID-er som *ikke* er fødselsnummer, men som tilfeldigvis er 11 siffer
   og dermed trigger regex-treffet, skal reformateres til noe som ikke ligner et
   fnr, slik at fremtidige scan ikke gir falske positiver:
   - `saksnummer` → 9 siffer.
   - `dokumentreferanse`/`dokumentreferanseOriginal`/`dokumentRef` → format
     `BIFxxxxxxx` (BIF + 7 siffer), som matcher det ekte produksjonsformatet
     (`Dokument.dokumentreferanse get() = "BIF$dokumentId"`).
   - `journalpostId`/`nyJournalpostId` → 10 siffer.
7. Eksisterende, eksplisitt navngitte dummy-identer (f.eks. `DUMMY_NUMMER` for
   «Elin» i `bidrag-regnskap`) skal **ikke** røres selv om de matcher regexen — de
   er bevisst valgt og dokumentert dummydata.
8. Verifiser med `mvn test` per berørt modul etter hver endring, ikke bare til
   slutt — enklere å spore hvilken endring som eventuelt introduserer en regresjon.

## Ikke endret

- produksjonskode annet enn endringer ved ktlint --format som også skal kjøres

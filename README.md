# bidrag-backend

Monorepo for team Bidrag sine backend-tjenester. Bygget med Spring Boot og Kotlin
på Java 21, kjører på Nais.

## Lokal Texas-sidecar

Generer en lokal RSA-nøkkel før du starter Texas med Compose:

```sh
node util/bidrag-dev/generate-texas-jwk.mjs
docker compose up -d texas
```

Skriptet skriver `AZURE_APP_JWK` til `.env.texas`, som er ignorert av Git. Fila
trengs for å kjøre lokal Texas-sidecar. Finnes fila allerede, varsler skriptet
uten å overskrive. Slett fila og kjør skriptet på nytt hvis du
trenger en ny nøkkel.

## Bygg og deploy

Ved pull requests, i merge-køen og ved push til `main` starter
[`bygg-apper.yaml`](.github/workflows/bygg-apper.yaml) bygg for appene som er
berørt av endringene. Bibliotekene de trenger, klargjøres i én felles jobb.
Deretter bygges og testes appene parallelt. Deploy følger miljøreglene i
workflowen til hver app.

Hvilke filer som utløser bygg av en app, står i `env.APP_PATHS` i appens
workflow, med ett filmønster per linje. De samme filtrene brukes for push, PR
og merge-kø. Endrer du et felles bibliotek, kan flere apper bli bygget, men de
deler det samme bibliotekbygget. Endringer bare i denne README-en eller under
`util/` starter ikke workflowen.

### Merge-kø

Statussjekken på en PR sier bare at greina var grønn mot main slik main så ut
sist sjekken kjørte. Flytter main seg etterpå, kan en grønn hake være foreldet,
og feilen oppdages først etter merge, med rød main og en app som ikke ble
deployet.

Merge-køen løser dette: GitHub bygger kandidaten merget med main-tuppen, og
merger bare hvis bygget er grønt. Alternativet, «Require branches to be up to
date before merging», ville tvunget alle åpne PR-er til å rebase og kjøre hele
pipelinen på nytt for hver eneste merge. I et monorepo med 34 apper, der de
fleste endringer treffer ulike apper, koster det langt mer byggetid enn det
gir.

Køen bygger og tester, men deployer aldri. Alle `deploy_*`-flaggene krever
enten `workflow_dispatch` eller push til `refs/heads/main`, og en
merge_group-kjøring er ingen av delene. Deploy skjer først når køen har merget
og push-kjøringen på `main` er grønn.

Køen må slås på i repo-innstillingene (Settings → Rules → main → «Require merge
queue»). Required status check skal være `Alle bygg fullført`, samlejobben i
`bygg-apper.yaml`. Alle andre sjekker som gjøres required, må også kjøre på
`merge_group`, ellers står køen og venter på en sjekk som aldri kommer.

### Deploy manuelt

Åpne appens workflow i GitHub Actions, velg «Run workflow» og angi branch og
miljø. Du kan velge blant q1, q2 og prod, avhengig av hvilke miljøer appen
støtter. Ved manuell kjøring bygges appen uten å sjekke hvilke filer som er
endret. Bibliotekene hentes fra cache eller bygges ved behov.

Du kan hoppe over tester ved deploy til q1/q2, men ikke til prod.
«Deploy alle til q1/q2» starter én manuell kjøring per app. Disse bruker
bibliotekcachen, men har ikke en felles bibliotekjobb.

### Docker-image

Imaget lastes bare opp til registeret når kjøringen faktisk skal deploye,
altså ved `workflow_dispatch` til q1, q2 eller prod, eller ved push til `main`.
PR-kjøringer og merge-køen bygger imaget, men pusher det ikke. Dockerfilen og
byggkonteksten blir altså fortsatt verifisert, uten at hver eneste PR-push
legger igjen et image per app som ingen skal bruke.

Attestering og signering (`salsa`-jobben) og eksport av Docker-lagcachen følger
den samme regelen. Legger du til et nytt deploy-miljø i `bygg_og_deploy.yaml`,
må `push_image`, `cache_to` og `salsa`-jobben oppdateres med det nye flagget.
En test i `test_finn_berorte_apper.py` feiler hvis du glemmer det.

### Slik deles bibliotekene

[`klargjor-biblioteker`](.github/actions/klargjor-biblioteker/action.yaml)
henter eller bygger bibliotekene. De er samlet i tre grupper:

| Gruppe | Innhold |
| --- | --- |
| `felles` | Bibliotekene under `libs/bidrag-felles` |
| `beregn` | Beregningsbibliotekene under `libs/bidrag-beregn-felles`. Trenger også `felles`. |
| `oppgave` | `bidrag-oppgave-client` og `bidrag-oppgave-dto` |

Appene bruker `felles` som standard. Trenger en app andre grupper, angis de i
`bibliotekgrupper` i kallet til `bygg_og_deploy.yaml`. Bruk tom verdi (`''`)
for apper uten interne biblioteker.

Hver gruppe har sin egen cache. Maven bygger bare gruppene som ikke finnes i
cachen, og sørger for riktig byggerekkefølge. Hvis alle gruppene finnes der,
installeres bare parent-POM-ene. En endring i ett bibliotek kan dermed føre
til at hele gruppen bygges på nytt, men bare én gang per workflow-kjøring.

Ved cache-miss bygges uendret `felles` uten å kompilere eller kjøre tester.
Testene kjøres bare når diffen inneholder endringer under `libs/bidrag-felles/`.
For PR-er brukes diffen fra merge-base, og ved push til `main` brukes filene
som ble endret i pushen. Manuelle kjøringer sammenligner branchen med main;
på main brukes siste commit. Appens egne tester og testene i `beregn` og
`oppgave` påvirkes ikke.

En cache med testede fellesbiblioteker brukes hvis den finnes. Ellers kan
uendret felles bruke en egen cache fra bygg uten tester. Denne cachen brukes
aldri i stedet for å teste en faktisk felles-endring. Treffer kjøringen en
cache med allerede testet innhold, kjøres ikke de samme testene på nytt.

Cachen fornyes når kildekode, ressurser, POM-filer, Maven-/Java-versjon eller
byggoppsett endres. Beregningsbibliotekene bygges også på nytt når
fellesbibliotekene endres. Bygg med og uten tester har separate cacher.
`target` og genererte POM-filer påvirker ikke cache-nøkkelen.

Bibliotekjobben lagrer resultatet som et GitHub Actions-artefakt med JAR-er,
POM-er og Maven-metadata. Hver appjobb fjerner gamle interne artefakter fra
sin Maven-cache og laster ned de ferdige bibliotekene fra samme kjøring.
Deretter bygger den bare appen, uten `-am`. Maven-innstillinger og
innloggingsopplysninger følger ikke med artefaktet.

### Når et bygg feiler

Se jobbsammendraget i GitHub Actions for hvilke apper og bibliotekgrupper som
ble valgt, og hvilke Maven-moduler som ble bygget. Feiler bibliotekjobben,
starter ikke appjobbene. Feiler en app, kan bibliotekene som allerede er
lagret i cache, fortsatt brukes.

Kjører du en appjobb på nytt, bruker den bibliotekartefaktet fra den
vellykkede bibliotekjobben. Artefaktet beholdes i 14 dager. Er det slettet
eller utløpt, må du kjøre hele workflowen på nytt. Appbygget stopper hvis det
ikke får lastet ned artefaktet, fremfor å bruke gamle SNAPSHOT-er.

### Legge til apper og biblioteker

For en ny Maven-app kan du ta utgangspunkt i en eksisterende app-workflow.
Legg inn `APP_PATHS`, behold `workflow_call` og `workflow_dispatch`, og legg
til en jobb som kaller appens workflow i `bygg-apper.yaml`. Ikke legg egne
push- eller PR-triggere i appens workflow; da starter byggene utenom den
felles bibliotekjobben.

Nye biblioteker legges i modullisten for riktig gruppe i
`klargjor-biblioteker`. Pass også på at stiene for cache og opplasting av
artefakter dekker bibliotekets Maven-koordinater. Maven håndterer
avhengighetene og byggerekkefølgen.

`bidrag-felles.yaml` publiserer fortsatt kalenderversjoner separat.

Hvis du må gå tilbake til det gamle byggoppsettet, må du tilbakeføre
`bygg-apper.yaml`, app-workflowene og endringene i `bygg_og_deploy.yaml`
sammen. Ellers kan automatiske bygg utebli eller starte dobbelt. La pågående
deploy-jobber fullføre før du bytter oppsett.

## Automatisk PR-beskrivelse

Når du oppretter en pull request, legger workflowen
[`pr_beskrivelse.yaml`](.github/workflows/pr_beskrivelse.yaml) inn en oversikt
over endringene i PR-beskrivelsen: et kort sammendrag skrevet av Copilot, og en
tabell over hvilke maven-moduler som er berørt med antall filer og linjer.

Innholdet legges i en markert blokk:

```markdown
<!-- pr-beskrivelse:start -->
...generert innhold...
<!-- pr-beskrivelse:slutt -->
```

**Teksten du selv skriver utenfor blokken blir aldri rørt.** Alt inne i blokken
blir derimot overskrevet ved neste kjøring, så ikke rediger den.

### Når kjører den

| Hendelse | Modultabell | AI-sammendrag |
| --- | --- | --- |
| PR opprettet eller gjenåpnet | ✅ | ✅ |
| Draft merket som klar for review | ✅ | ✅ |
| Ny push til PR-en | ✅ | ♻️ beholdes fra forrige kjøring |
| Etiketten `oppdater-beskrivelse` settes på | ✅ | ✅ |

Modultabellen er ren git-utregning og oppdateres derfor på hver push.
AI-sammendraget koster å kjøre, så det
genereres bare når PR åpnes. Trenger du et nytt
sammendrag etter PR er opprettet, sett på etiketten `oppdater-beskrivelse` (fjern den
etterpå for å kunne bruke den igjen).

Draft-PR-er får tabellen, men ikke AI-sammendrag — det kommer først når PR-en
merkes klar for review. PR-er fra forks hoppes helt over, siden de får et
read-only token og uansett ikke kan oppdatere beskrivelsen. Feiler
Copilot-kallet, blokkerer det ikke PR-en: tabellen legges inn, forrige
sammendrag beholdes, og årsaken logges som en advarsel i jobben.
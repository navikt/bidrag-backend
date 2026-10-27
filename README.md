# bidrag-backend

Monorepo for team Bidrag sine backend-tjenester. Bygget med Spring Boot og Kotlin
på Java 25, kjører på Nais.

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
For hver app kjører image-bygg og tester parallelt. Deploy venter på begge
jobbene og følger miljøreglene i appens workflow.

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
endret. Maven bygger bibliotekene appen trenger fra samme checkout med `-am`.

Du kan hoppe over tester ved deploy til q1/q2, men ikke til prod.
«Deploy alle til q1/q2» starter én manuell kjøring per app. Hver app bygger
sine egne bibliotekavhengigheter.

### Docker-image

Imaget lastes bare opp til registeret når kjøringen faktisk skal deploye,
altså ved `workflow_dispatch` til q1, q2 eller prod, eller ved push til `main`.
PR-kjøringer bygger imaget etter at appens tester har bestått, men pusher det
ikke. Merge-køen hopper over Docker-bygget. Dockerfilen og byggkonteksten
blir dermed verifisert på PR-er uten at hver push legger igjen et image.

Når imaget pushes, genererer og attesterer `nais/docker-build-push` SBOM før
jobben fullfører. Deploy venter dermed på både appens tester og attesteringen.
Legger du til et nytt deploy-miljø i `bygg_og_deploy.yaml`, må `push_image`
omfatte det nye flagget.

### Bygg og test av biblioteker

Hver app bygger bibliotekene den trenger med `mvn -pl <app> -am` fra rot-POM-en.
Maven velger avhengighetene og bygger dem i riktig rekkefølge. Appjobben
installerer først appen og avhengighetene uten tester, kjører `verify` for
bare appen og bygger deretter imaget. Dermed kjøres ikke tester i uendrede
biblioteker for hvert appbygg. Med `skip_tester` bygger den appen og
avhengighetene uten å kjøre tester eller ktlint.

Når en fil under `libs/` endres, finner CI den tilhørende modulen i rot-POM-en.
En egen testjobb bygger modulens avhengigheter uten tester og kjører `verify`,
inkludert ktlint, for de endrede bibliotekene. Endringer i `pom.xml`,
`.mvn/`, `.editorconfig` eller den felles CI-bygglogikken utløser tester i
alle bibliotekene. Jobben kjører også når ingen app er valgt, parallelt med
appbyggene. Lib-testene inngår i samlet status og blokkerer merge hvis de
feiler, men appbyggene venter ikke på dem. For PR-er brukes diffen fra
merge-base, og ved push til `main` brukes filene som ble endret i pushen.
Ved manuell deploy bygges bibliotekene med `-am`, men bare appens tester kjøres
dersom tester ikke er hoppet over.

Appjobbene cacher eksterne Maven-avhengigheter, men ikke interne
`no/nav/bidrag`-artefakter. Bibliotekene bygges fra samme checkout som appen,
også når en enkelt appjobb kjøres på nytt.

Når en PR endrer den felles bygg-workflowen eller skriptet for appvalg, bygger
CI alle appene. Ved endring i én app-workflow bygges denne appen. Slike rene
CI-endringer starter ikke automatisk prod-deploy ved merge til `main`.

### Når et bygg feiler

Se jobbsammendraget i GitHub Actions for hvilke apper og biblioteker som ble
valgt. Feiler bibliotektestene, blir den samlede PR-statusen rød, mens
appbyggene fortsetter. Ved push til `main` venter ikke automatisk deploy på
lib-testene; appens egne tester må fortsatt bestå. Ved manuell deploy kjøres
ikke egne lib-tester. Feiler et appbygg, kan du kjøre jobben på nytt; Maven
bygger da avhengighetene på nytt fra samme commit.

### Legge til apper og biblioteker

For en ny Maven-app kan du ta utgangspunkt i en eksisterende app-workflow.
Legg inn `APP_PATHS`, behold `workflow_call` og `workflow_dispatch`, og legg
til en jobb som kaller appens workflow i `bygg-apper.yaml`. Ikke legg egne
push- eller PR-triggere i appens workflow; da starter byggene utenom den
felles endringsdeteksjonen.

Legg nye biblioteker i rot-POM-en og deklarer dem som Maven-avhengigheter i
appene som bruker dem. Oppdater `APP_PATHS` i disse appenes workflows slik at
en endring i biblioteket også velger de berørte appene. Ingen egne
bibliotekgrupper eller artefaktstier må vedlikeholdes.

`bidrag-felles.yaml` publiserer fortsatt kalenderversjoner separat.

Hvis du må gå tilbake til det gamle byggoppsettet, må du tilbakeføre
`bygg-apper.yaml`, app-workflowene, `bygg_og_deploy.yaml` og
`finn_berorte_apper.py` sammen med bibliotek-actionen. La pågående
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
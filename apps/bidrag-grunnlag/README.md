# Bidrag Grunnlag

Tjeneste for innhenting av grunnlag i bidragssaker. Tjenesten er sentrert rundt begrepet `grunnlagspakke`, som fungerer som en beholder for alle grunnlag tilknyttet en bestemt bidragssak. Konsumenter av tjenesten kan opprette grunnlagspakker og bestemme hvilke grunnlag og perioder som skal hentes for de ulike partene. Tjenesten vil hente alle ønskede grunnlag og knytte de opp mot opprettet grunnlagspakke. Grunnlagspakken kan deretter hentes ut med alle tilhørende grunnlag. Frem til det er fattet et vedtak i en bidragssak tilknyttet en grunnlagspakke, kan alle grunnlagene oppdateres og/eller endres.

For de grunnlagene som er relatert til inntekt og som potensielt inneholder flere underposter (A-inntekt og Skattegrunnlag), gjøres det en sammenligning mot eksisterende forekomster når det blir kjørt en oppdatering av grunnlagspakke. Hvis de(n) nye forekomsten(e) som hentes er identisk(e) med de(n) som er hentet fra før, oppdateres kun timestamp på eksisterende forekomst(er). For alle andre grunnlagstyper blir det ikke gjort noen sammenligning. Her insertes det ny(e) forekomst(er) og eksisterende forekomst(er) settes til aktiv=false og gyldigTil=current timestamp.

Støtter foreløpig følgende grunnlag:
* A-inntekt
* Skattegrunnlag
* Utvidet barnetrygd og småbarnstillegg
* Barnetillegg fra Pensjon
* Kontantstøtte
* Barnetilsyn
* Sivilstand
* Husstandsmedlemmer og egne barn

I tillegg er det laget et endepunkt for å hente grunnlag direkte, uten å bruke grunnlagspakke og lagring i bidrag-grunnlag.
Følgende grunnlag kan hentes på denne måten:
* A-inntekt
* Skattegrunnlag
* Utvidet barnetrygd og småbarnstillegg
* Barnetillegg fra Pensjon
* Kontantstøtte
* Barnetilsyn
* Sivilstand
* Husstandsmedlemmer og egne barn
* Arbeidsforhold
* Tilleggsstønad

Miljøer:
* DEV-GCP-FEATURE ([https://bidrag-grunnlag-feature.intern.dev.nav.no/](https://bidrag-grunnlag-feature.dev.intern.nav.no/))
* DEV-GCP ([https://bidrag-grunnlag.intern.dev.nav.no/](https://bidrag-grunnlag.dev.intern.nav.no/))
* PROD-GCP ([https://bidrag-grunnlag.intern.nav.no/](https://bidrag-grunnlag.intern.nav.no/))

## Planlagt innhenting av valutakursgrunnlag

Jobben kjører 1. januar og 1. juli kl. 05.00 i Oslo-tid. Etter lagring sendes en Slack-melding med dato, miljø, antall opprettede grunnlag og valutakodene som ikke ble innhentet. Hvis alle kurser ble hentet, står det «Ingen». Manglende enkeltkurser hindrer ikke at kjøringen fullføres.

En daglig kontroll kl. 06.00 varsler på Slack hvis databasen mangler valutakursgrunnlag for siste halvårskjøring.

ECB-kurser hentes samlet per observasjonsmåned med én felles NOK-serie. Norges Bank brukes bare for valutaer som mangler en gyldig ECB-kurs. Hvis hele ECB-kallet feiler eller NOK-serien er ugyldig, brukes Norges Bank for alle forespurte valutaer i måneden. Historisk innhenting bruker samme batching, med ett ECB-kall per halvår før eventuelle retries.

Ved innhentings- eller lagringsfeil sendes et feilvarsel, og feilen kastes videre. Feilvarselet inneholder feiltype, ikke exception-meldingen. Detaljer finnes i applikasjonsloggene. Feil ved Slack-sending logges av den felles Slack-tjenesten.

Nais-konfigurasjonen bruker secret `bidrag-bot-slack-oauth-token` og miljøvariabelen `SLACK_CHANNEL_ID`. Dev- og prod-kanalene er de samme som for `bidrag-regnskap`.

## Utstede gyldig token i dev-gcp
For å kunne teste applikasjonen i `dev-gcp` trenger man et gyldig AzureAD JWT-token. 
JWT-tokenet kan hentes ut manuelt eller ved hjelp at skriptet her: [hentJwtToken](https://github.com/navikt/bidrag-dev/blob/main/scripts/hentJwtToken.sh).

For å utstede et slikt token trenger man miljøvariablene `AZURE_APP_CLIENT_ID` og `AZURE_APP_CLIENT_SECRET`. Disse ligger tilgjengelig i de kjørende pod'ene til applikasjonen.

Miljøvariabler kan hentes ut fra en kjørende pod slik:
```bash
# Feature-branch:
export $(kubectl --namespace bidrag --cluster dev-gcp exec --tty deployment/bidrag-grunnlag-feature -- printenv | grep -e AZURE_APP_CLIENT_ID -e AZURE_APP_CLIENT_SECRET | xargs -L 1)
```

```bash
# Main-branch:
export $(kubectl --namespace bidrag --cluster dev-gcp exec --tty deployment/bidrag-grunnlag -- printenv | grep -e AZURE_APP_CLIENT_ID -e AZURE_APP_CLIENT_SECRET | xargs -L 1)
```

Deretter kan vi hente ned et gyldig Azure AD JWT-token med følgende kall:
```bash
# Feature-branch:
curl -X POST -H "Content-Type: application/x-www-form-urlencoded" -d 'client_id='"$AZURE_APP_CLIENT_ID"'&scope=api://dev-gcp.bidrag.bidrag-grunnlag-feature/.default&client_secret='"$AZURE_APP_CLIENT_SECRET"'&grant_type=client_credentials' 'https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/oauth2/v2.0/token'
```

```bash
# Main-branch:
curl -X POST -H "Content-Type: application/x-www-form-urlencoded" -d 'client_id='"$AZURE_APP_CLIENT_ID"'&scope=api://dev-gcp.bidrag.bidrag-grunnlag/.default&client_secret='"$AZURE_APP_CLIENT_SECRET"'&grant_type=client_credentials' 'https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/oauth2/v2.0/token'
```

## Kjøre applikasjon lokalt
En fullstendig fungerende applikasjon kan for øyeblikket ikke kjøres opp lokalt på egen maskin da vi ikke har mulighet til å kommunisere med eksterne tjenester. Applikasjonen kan allikevel kjøres opp for å teste endepunkter fra Swagger ([http://localhost:8080/bidrag-grunnlag](http://localhost:8080/bidrag-grunnlag)) og annen logikk i applikasjonen som er uavhengig av kontakt med eksterne tjenester. Operasjoner som går rett mot databasen, som opprettelse og henting av grunnlagspakker, vil også fungere ved hjelp av in-memory databasen H2.

For å starte applikasjonen kjører man `main`-metoden i fila `BidragGrunnlagLocal.kt` med profilen `local`.

Også når man kjører applikasjonen lokalt vil man trenge et gyldig JWT-token for å kunne kalle på endepunktene. For å utstede et slikt token kan man benytte det åpne endepunktet `GET /local/cookie/` med `issuerId=aad` og `audience=aud-localhost`. Her benyttes en "fake" token-issuer som er satt med wiremock ved hjelp av annotasjonen: `@EnableMockOAuth2Server` fra NAV-biblioteket `token-support`.

Kan vurdere å sette opp wiremocks for de eksterne tjenestene for å kunne kjøre opp en mer fullstedig applikasjon i fremtiden.

## Teste PostgreSQL-migrasjoner

`ValutakursgrunnlagMigrationTest` starter PostgreSQL 15 med Testcontainers og kjører alle Flyway-migrasjonene fra `db/migration`. Testen kontrollerer oppdateringstriggeren, halvårsgrenser, unik valuta per halvår, tillatte statuser og kilder samt kurskolonnens presisjon. Testbrukeren heter `cloudsqliamuser` fordi de eksisterende migrasjonene gir denne rollen tilgang.

Docker må være tilgjengelig. Kjør fra repoets rot:

```bash
mvn -pl apps/bidrag-grunnlag -am -Dtest=ValutakursgrunnlagMigrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## Testing i Swagger
Applikasjonen testes enklest i Swagger (for generering av gyldig token, se over):
```
https://bidrag-grunnlag.intern.dev.nav.no/swagger-ui/index.html
```

### Kjøre lokalt mot nais med lokal database
##### Start opp database
Start opp lokal postgres database med følgende kommando på rotmappen. 
```bash
docker-compose up -d
```
Dette vil starte en tom postgres database. 
Ved oppstart av appen vil flyway skriptene initialiseree alle tabeller som er nødvendig for lokal kjøring.

Databasen er persistent. Det vil si at all data vil bli lagret lokalt og være tilgjengelig selv ved restart av PC eller docker.

##### Initialiser miljøvariabler
Kjør ```initEnv.sh``` skriptet for å sette opp miljøvariabler for lokal kjøring.
<br/>
Dette vil hente Azure hemmeligheter og diverse miljøvariabler fra POD kjørende i dev

Man må først være logget inn i nais. Logg inn med
```bash
nais auth login
```

Hvis du ikke får `permission denied` når du prøver å kjøre skriptet så må du gi deg selv tilgang til å kjøre shell skript med følgende kommand:
```bash
Kjør chmod +x ./initEnv.sh
```

Du kan da starte opp applikasjonen ved å kjøre [BidragGrunnlagLokalNais.kt](src/test/kotlin/no/nav/bidrag/grunnlag/BidragGrunnlagLokalNais.kt)

Gå til [http://localhost:8086/swagger-ui/index.html](http://localhost:8086/swagger-ui/index.html) for å åpne swagger-ui
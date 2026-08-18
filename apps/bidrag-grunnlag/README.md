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

## Utstede gyldig token i dev-gcp
For å kunne teste applikasjonen i `dev-gcp` trenger man et gyldig AzureAD JWT-token. 
JWT-tokenet kan hentes ut manuelt eller ved hjelp at skriptet her: [hentJwtToken](https://github.com/navikt/bidrag-dev/blob/main/scripts/hentJwtToken.sh).

For å utstede et slikt token trenger man miljøvariablene `AZURE_APP_CLIENT_ID` og `AZURE_APP_CLIENT_SECRET`. Disse ligger tilgjengelig i de kjørende pod'ene til applikasjonen.

Koble seg til en kjørende pod (feature-branch):
```
kubectl -n bidrag exec -i -t bidrag-grunnlag-feature-<sha> -c bidrag-grunnlag-feature -- /bin/bash
```

Koble seg til en kjørende pod (main-branch):
```
kubectl -n bidrag exec -i -t bidrag-grunnlag-<sha> -c bidrag-grunnlag -- /bin/bash
```

Når man er inne i pod'en kan man hente ut miljøvariablene på følgende måte:
```
echo "$( cat /var/run/secrets/nais.io/azure/AZURE_APP_CLIENT_ID )"
echo "$( cat /var/run/secrets/nais.io/azure/AZURE_APP_CLIENT_SECRET )"
```

Deretter kan vi hente ned et gyldig Azure AD JWT-token med følgende kall (feature-branch): 
```
curl -X POST -H "Content-Type: application/x-www-form-urlencoded" -d 'client_id=<AZURE_APP_CLIENT_ID>&scope=api://dev-gcp.bidrag.bidrag-grunnlag-feature/.default&client_secret=<AZURE_APP_CLIENT_SECRET>&grant_type=client_credentials' 'https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/oauth2/v2.0/token'
```

Deretter kan vi hente ned et gyldig Azure AD JWT-token med følgende kall (main-branch):
```
curl -X POST -H "Content-Type: application/x-www-form-urlencoded" -d 'client_id=<AZURE_APP_CLIENT_ID>&scope=api://dev-gcp.bidrag.bidrag-grunnlag/.default&client_secret=<AZURE_APP_CLIENT_SECRET>&grant_type=client_credentials' 'https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/oauth2/v2.0/token'
```

## Kjøre applikasjon lokalt
En fullstendig fungerende applikasjon kan for øyeblikket ikke kjøres opp lokalt på egen maskin da vi ikke har mulighet til å kommunisere med eksterne tjenester. Applikasjonen kan allikevel kjøres opp for å teste endepunkter fra Swagger ([http://localhost:8080/bidrag-grunnlag](http://localhost:8080/bidrag-grunnlag)) og annen logikk i applikasjonen som er uavhengig av kontakt med eksterne tjenester. Operasjoner som går rett mot databasen, som opprettelse og henting av grunnlagspakker, vil også fungere ved hjelp av in-memory databasen H2.

For å starte applikasjonen kjører man `main`-metoden i fila `BidragGrunnlagLocal.kt` med profilen `local`.

Også når man kjører applikasjonen lokalt vil man trenge et gyldig JWT-token for å kunne kalle på endepunktene. For å utstede et slikt token kan man benytte det åpne endepunktet `GET /local/cookie/` med `issuerId=aad` og `audience=aud-localhost`. Her benyttes en "fake" token-issuer som er satt med wiremock ved hjelp av annotasjonen: `@EnableMockOAuth2Server` fra NAV-biblioteket `token-support`.

Kan vurdere å sette opp wiremocks for de eksterne tjenestene for å kunne kjøre opp en mer fullstedig applikasjon i fremtiden.

## Testing i Swagger
Applikasjonen testes enklest i Swagger (for generering av gyldig token, se over):
```
https://bidrag-grunnlag.intern.dev.nav.no/swagger-ui/index.html
```

### Kjøre lokalt mot nais med lokal database
##### Start opp database
Start opp lokal postgres database med følgende kommando på rotmappen. 
```
docker-compose up -d
```
Dette vil starte en tom postgres database. 
Ved oppstart av appen vil flyway skriptene initialiseree alle tabeller som er nødvendig for lokal kjøring.

Databasen er persistent. Det vil si at all data vil bli lagret lokalt og være tilgjengelig selv ved restart av PC eller docker.

##### Initialiser miljøvariabler
Kjør ```initEnv.sh``` skriptet for å sette opp miljøvariabler for lokal kjøring.
<br/>
Dette vil hente Azure hemmeligheter og diverse miljøvariabler fra POD kjørende i dev

Hvis du ikke får `permission denied` når du prøver å kjøre skriptet så må du gi deg selv tilgang til å kjøre shell skript med følgende kommand:
```bash
Kjør chmod +x ./initEnv.sh
```

Du kan da starte opp applikasjonen ved å kjøre [BidragGrunnlagLokalNais.kt](src/test/kotlin/no/nav/bidrag/grunnlag/BidragGrunnlagLokalNais.kt)

Gå til http://localhost:8086 for å åpne swagger-ui

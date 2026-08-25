# Bidrag-reisekostnad-api

Backend for å [fordele reisekostnader ved samvær med barn](https://www.nav.no/fordele-reisekostnader).

### Oppsett med lokal database-instans og WireMock API-simulering
Ved lokal kjøring brukes Spring-boot-instansen 
[BidragReisekostnadApiLokalTestapplikasjon](src/test/java/no/nav/bidrag/reisekostnad/BidragReisekostnadApiLokalTestapplikasjon.java).
For lokal kjøring må Spring-profil settes til enten <b>lokal-h2</b> eller 
<b>lokal-postgres</b> avhengig av hvilken database det ønskes å 
teste med. Til <b>lokal-postgres</b>-profilen kreves det en lokal Postgres-instans.

#### Lokal H2 instans

Eksempel på kommando for å starte applikasjonen lokalt med H2-database:
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test-compile exec:java \
-Dexec.mainClass="no.nav.bidrag.reisekostnad.BidragReisekostnadApiLokalTestapplikasjon" \
-Dexec.classpathScope=test \
-Dspring.profiles.active=lokal-h2
```

H2-databasen er satt opp in-memory og kan nås på 
http://localhost:8080/h2-console/login.jsp med "jdbc:h2:mem:default" JDBC 
URL og blankt passord. Det er ingen data i H2-databasen ved oppstart, men 
legges inn ved eksempelvis bruk av endepunktene via Swagger UI som forklart 
nedenfor.

Innstillinger og logg-nivået er satt i [application-lokal-h2.yml](src/main/resources/application-lokal-h2.yml).

#### Lokal Postgres instans

Installasjon av Postgres via Homebrew på MacOS:
```bash
brew install postgresql
```
Oppstart av Postgres-tjenesten:
```bash
brew services start postgresql
```

#### Oppsett av test-token

[BidragReisekostnadApiLokalTestapplikasjon](src/test/java/no/nav/bidrag/reisekostnad/BidragReisekostnadApiLokalTestapplikasjon.java)
er satt opp til å bruke et test-token generert av [token-support](https://github.com/navikt/token-support).

##### Teste brukerinformasjon endepunktet i lokal Swagger

1. Generer et test-token for _tokenx_ issuer og _aud-localhost_ audience for å 
sette tokenet i en cookie som Swagger kan bruke for autentisering:

    > Generer token for bruker. Erstatt {ident} med ønsket ident.
    >
    > http://localhost:8080/local/cookie?issuerId=tokenx&audience=aud-localhost&subject={ident}


2. Autentiser som valgte bruker, se etter Authorize-knappen 
øverst til høyre i Swagger UI og lim inn det genererte tokenet:
    > http://localhost:8080/swagger-ui/index.html

3. Test ut endepunktet for _brukerinformasjon_ i Swagger.

##### Teste forespoersel/ny endepunktet i lokal Swagger

Følg steg 1 og 2 i forrige seksjon for å generere token og autentisere i 
Swagger UI. Se etter "fellesBarn" og "ident" til innlogget testperson 
i [test/resources/mappings/bidrag-person-relasjon-*****.json](src/test/resources/mappings/).
Test ut endepunktet for _forespørsel/ny_ i Swagger.

## Oppsett av lokalt utviklingsmiljø mot sky

Kjør følgende kommandoer fra terminalvinduet i root mappen til
`bidrag-reisekostnad-api` prosjektet:

```bash
# Logg inn i GCP
gcloud auth login --update-adc
```

```bash
# Still inn kubectl cluster til dev-gcp
kubectl config use-context dev-gcp
```

```bash
# Sett namespace til bidrag
kubectl config set-context --current --namespace=bidrag
```

```bash
# Eksporter variabler til src/main/resources/application-lokal-sky-secrets.properties slik at appen kan autentisere i dev-gcp.
# Filen application-lokal-sky-secrets.properties skal aldri committes til Git og skal slettes etter bruk.
# Filen application-lokal-sky-secrets.properties er lagt til i .gitignore for å unngå at den committes ved en feil.
 kubectl exec -n bidrag deployment/bidrag-reisekostnad-api -- printenv | grep -E 'AZURE_APP_CLIENT_ID|AZURE_APP_CLIENT_SECRET|TOKEN_X|BIDRAG_PERSON_URL|BIDRAG_DOKUMENT_URL|SCOPE|AZURE_OPENID_CONFIG_TOKEN_ENDPOINT|AZURE_APP_TENANT_ID|AZURE_APP_WELL_KNOWN_URL' > src/main/resources/application-lokal-sky-secrets.properties
 ```

Kjør [BidragReisekostnadApiLokalSky](src/test/java/no/nav/bidrag/reisekostnad/BidragReisekostnadApiLokalSky.java).
Dette vil starte opp applikasjonen lokalt med `H2` database.

Api kall kan testes ved å først hente `reisekostnad_api_token` token fra
[https://bidrag-reisekostnad.intern.dev.nav.no/api/dev/session]
(https://bidrag-reisekostnad.intern.dev.nav.no/api/dev/session).
Testbruker hentes fra [Dolly](https://dolly.ekstern.dev.nav.no/).

Deretter kan tokenet brukes til å logge inn på swagger-ui
http://localhost:8080/swagger-ui/index.html og teste ut ulike api kall.
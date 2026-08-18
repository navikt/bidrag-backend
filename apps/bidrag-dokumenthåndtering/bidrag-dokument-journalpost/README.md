# bidrag-dokument-journalpost

Tjeneste for å hente metadata fra midlertidig brevlager (bidrag bisys)

### kjøring lokalt
Det som forventes av konfigurasjon er at følgende miljøvariabler blir satt:
* `BIDRAG_DB_HOST`
* `BIDRAG_DB_PORT`
* `BIDRAG_DB_NAME`
* `BIDRAG_DB_SCHEMA`
* `BIDRAG_DB_USERNAME`
* `BIDRAG_DB_PASSWORD`

Se **Sikkerhet** for kjøring med sikkerhet lokalt.

### beskrivelse

Dette er en mikrotjeneste som blir brukt av en annen mikrotjeneste, `bidrag-dokument`.
Formålet med tjensten er å lese metadata om dokumenter/notater fra midlertidig
brevlager (`T_JP` og `T_JSAK`).

### bygg og kjør applikasjon

Dette er en spring-boot applikasjon og kan kjøres som ren java applikasjon, ved å
bruke `maven` eller ved å bygge et docker-image og kjøre dette 

##### java og maven
* krever installasjon av java og maven:

  * `mvn clean install`<br>
    deretter
  * `cd bidrag-dokument-journalpost`
  * `mvn spring-boot:run`<br>
     eller
  * `cd bidrag-dokument-journalpost/target`<br>
  * `java -jar bidrag-dokument-journalpost-<versjon>.jar`

##### docker og maven
* krever installasjon av java, maven og docker
* docker image er det som blir kjørt som nais applikasjon

  * `mvn clean install`<br>
  deretter<br>
  * `docker build -t bidrag-dokument-journalpost .`
  * `docker run -p 8080:8080 bidrag-dokument-journalpost`

Etter applikasjon er startet kan den nåes med browser på
`http://localhost:8080/bidrag-dokument-journalpost/swagger-ui.html`

#### Profiler og kjøring lokalt

Se **Sikkerhet** for kjøring med sikkerhet lokalt.

##### Profil: live
`live`-profilen er default profil for BidragJournalpost-appen, og er profilen som skal
være aktiv i preprod og prod. Når denne profilen er aktiv, hentes alle ressurser som
kreves fra `vault.adeo.no`

##### Profil: test
`test`-profilen brukes primært ved kjøring av enhetstester, samt for lokal kjøring
i forbindelse med utvikling. Denne profilen benytter bla en `H2` (in-memory database),
test OIDC-tokengenerator, samt mockede ABAC og PIP-endepunkter.

Formålet med BidragJournalpostLocal er å tillate bruk av test-tokengenerator for lokal 
kjøring.

##### Profil: local
`local`-profilen benyttes for manuell testing lokalt uten eksterne avhengigheter.
Denne profilen benytter bla en `H2` (in-memory database), test OIDC-tokengenerator, 
samt mockede ABAC og PIP-endepunkter.
 
##### Profil: integration-db2
`integration-db2`-profilen kan brukes som JVM-argument til BidragJournalpostLocal ved
lokal kjøring om man ønsker å teste integrasjon mot en DB2-instans. Denne profilen
krever at DB2-relaterte hemmeligheter er satt som miljøvariabler.

##### Profil: integration-abac
`integration-abac`-profilen brukes ved lokal kjøring for å teste integrasjon mot ABAC.
Profilen aktiveres ved å starte BidragDokumentJournalpostIntegrationAbac. Merk her kan 
ikke BidragJournalpostLocal benyttes pga av at ABAC krever at OIDC-token med aktuell
bruker sendes over i PDP-requesten. `integration-abac` bruker lokal h2-database.

Profien krever at SRVBISYS_PASSWORD er satt i application.yaml (under
integration-abac profilen) før kjøring (hentes fra Fasit).

##### in memory DB og logging for test og integration-abac profilene
local og integration-abac-profilene bruker en in-memory database. Databasen kan oppdateres
gjennom h2-database konsoll som er tilgjengelig på addresse: 
`localhost:8080/bidrag-dokument-journalpost/h2-console/`.

Alternativt kan `test > resources > testdata > testdata.sql` oppdateres med ønsket 
testdata. Innholdet i denne fila leses inn i h2 databasen for local og integration-abac-
profilene.  

Profilen `test` vil i tillegg ha debug logging for sql generert av hibernate.

### Sikkerhet
Tjenestens endepunkter er sikret med navikt
[token-validation-spring](https://github.com/navikt/token-support/tree/master/token-validation-spring)
fra [token-support](https://github.com/navikt/token-support). Det betyr at gyldig
OIDC-id-token må være inkludert som Bearer-token i Authorization header for alle
spørringer mot disse endepunktene. 

For kjøring lokalt benyttes
[token-validation-test-support](https://github.com/navikt/token-support/tree/master/token-validation-test-support)
som blant annet sørger for at det genereres id-tokens til test formål. For å redusere
risikoen for at testgeneratoren ved en feil gjøres aktiv i produksjon er
token-validation-test-support-modulen kun tilgjengelig i test-scope. I tillegg er bruken av
testgeneratoren kun knyttet til en egen spring-boot app-definisjon,
BidragDokumentJournalpostLocal (lokalisert under test) som benytter test-profil.

BidragDokumentJournalpostLocal brukes i stedet for BidragDokumentJournalpost ved
lokal kjøring.

Miljøvariabelen 'ACCEPTED_AUDIENCE' angir hvilke apper som godkjennes som token-uthentere. Denne variabelen må settes i Vault for kjøring på NAIS.
Audience bidrag-q-localhost er lagt til for å støtte localhost redirect i preprod. Denne benyttes ved front-end-utvikling for å kunne kjøre tester 
med preprod-tjenester uten å måtte legge inn host-mappinger. bidrag-q-localhost-agenten er satt opp vha https://github.com/navikt/amag. 
Denne er ikke, og skal heller ikke være tilgjengelig i prod.

#### Oppskrift for kjøring med test-token i Swagger (ved integrasjonstesting mot AM eller ABAC må token hentes fra bidrag-ui.<domene-navn>/session)
 - Start BidragDokumentJournalpostLocal som standard Java-applikasjon
 - Hent test-token [http://localhost:8080/bidrag-dokument-journalpost/local/jwt](http://localhost:8080/bidrag-dokument-journalpost/local/jwt)
 - Åpne Swagger (http://localhost:8080/bidrag-dokument-journalpost/swagger-ui.html)
 - Trykk Authorize, og oppdater value-feltet med: `Bearer <testtoken-streng>` fra steg 2.

### Kafka

`bidrag-dokument-journalpost` er koblet opp mot kafka kjørende i kafka-pool `nav-dev` og `nav-prod`.
Ved kjøring av applikasjon lokalt vil denne funksjonaliteten ikke lastes og alle kafka meldinger blir bare logget.

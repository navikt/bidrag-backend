# bidrag-organisasjon

Mikrotjeneste for å hente ut navn på saksbehandler vha LDAP

### Bygg og kjør applikasjon

Dette er en spring-boot applikasjon og kan kjøres som ren java applikasjon, ved å
bruke `maven` eller ved å bygge et docker-image og kjøre dette 

##### java og maven
* krever installasjon av java og maven:

  * `mvn clean install`<br>
    deretter
  * `cd bidrag-organisasjon`
  * `mvn spring-boot:run`<br>
     eller
  * `cd bidrag-organisasjon/target`<br>
  * `java -jar bidrag-organisasjon-<versjon>.jar`

##### docker og maven
* krever installasjon av java, maven og docker
* docker image er det som blir kjørt som nais applikasjon

  * `mvn clean install`<br>
  deretter<br>
  * `docker build -t bidrag-organisasjon .`
  * `docker run -p 8080:8080 bidrag-organisasjon`

Etter applikasjon er startet kan den nåes med browser på
`http://localhost:8080/bidrag-organisasjon/swagger-ui.html`

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
BidragDokumentLocal (lokalisert under test) som benytter test-profil.

BidragOrganisasjonLocal brukes i stedet for BidragOrganisasjon ved lokal kjøring.

Miljøvariabelen 'ACCEPTED_AUDIENCE' angir hvilke apper som godkjennes som token-uthentere. Denne variabelen må settes i Vault for kjøring på NAIS.
Audience bidrag-q-localhost er lagt til for å støtte localhost redirect i preprod. Denne benyttes ved front-end-utvikling for å kunne kjøre tester 
med preprod-tjenester uten å måtte legge inn host-mappinger. bidrag-q-localhost-agenten er satt opp vha https://github.com/navikt/amag. 
Denne er ikke, og skal heller ikke være tilgjengelig i prod.

#### Oppskrift for kjøring med test-token i Swagger lokalt (ved integrasjonstesting mot AM eller bidrag-dokument-journalpost i NAIS, må token hentes fra bidrag-ui.<domene-navn>/session)
 - Start BidragOrganisasjonLocal som standard Java-applikasjon
 - Hent test-token [http://localhost:8080/bidrag-organisasjon/local/jwt](http://localhost:8090/bidrag-organisasjon/local/jwt)
 - Åpne Swagger (http://localhost:8080/bidrag-organisasjon/swagger-ui.html)
 - Trykk Authorize, og oppdater value-feltet med: Bearer <testtoken-streng> fra steg 2.

### Tilgjengelige tjenester (endepunkter)
Se Swagger

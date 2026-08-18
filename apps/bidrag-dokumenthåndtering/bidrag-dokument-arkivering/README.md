# bidrag-dokument-arkivering

Mikrotjeneste som overfører journalposter fra Bisys til Joark. Dette innebærer flytting av journalpostdokumenter
fra midlertidig brevlager til Joarks dokumentarkiv. Tjenesten forventer id til en bidrag journalpost med status
reservert som eneste inngangsparameter. Basert på denne id-en sørger tjenesten for at aktuell journalpost 
arkiveres i Joark sammen med tilhørende dokument fra midlertidig brevlager. 

### Sentrale aktørerer
- `bidrag-dokument-journalpost` (REST): Hente reservert journalpost fra Bisys
- `Brevserver - dokumentbestilling` (WMQ): Bestille tilgang til dokument i midlertidig brevlager
- `Brevserver - dokumentformidling` (SOAP):  Hente elektronisk dokument fra midlertidig brevlager  
- `Security Token Service (STS)` (REST): Hente OIDC token for service bruker srvbdarkivering for tilgang til JournalpostApi 
- `Dokarkiv/ JournalpostApi` (REST): Arkivering av journalpost med tilhørende dokument i Joark

### Prosessflyt
`Bisys` kontrollerer prosessen for arkivering av journalposter i Joark. Når en journalpost er klar for arkiviering, gjør `Bisys` et kall mot
bidrag-dokument-arkivs endepunkt med id til aktuell journalpost. Journalposten må ha status reservert for at arkiveringsprosessen skal 
kunne gjennomføres. 
 
Umiddelbart etter kallet mot `bidrag-dokument-arkivering`, gjør `Bisys` et kall til avviksrutinen for ARKIVERE_JOURNALPOST i `bidrag-dokument-journalpost`
med parameter START for å oppdatere ARKIVERING_STARTET-feltet i T_JSAK-tabellen til Bisys med nytt tidsstempel for å sikre at journalposten ikke endres 
av andre prosesser.

`bidrag-dokument-arkivering` starter med et kall mot `bidrag-dokument-journalpost` for å hente journalposten `Bisys` har bedt om å få arkivert. Dersom 
journalposten ikke finnes eller har en annen status enn reservert, avbrytes prosessen med HTTP-kode 404 - ikke funnet. `Bisys` vil for slike tilfeller
oppdatere ARKIVERING_FEILET-feltet i T_JSAK-tabellen via avviksrutinen for ARKIVERE_JOURNALPOST i `bidrag-dokument-journalpost`. 

Dersom journalposten finnes og har status reservert, vil `bidrag-dokument-arkivering` kalle avviksrutinen for ARKIVERE_JOURNALPOST i `bidrag-dokument-journalpost` med parameter STARTET for å oppdatere ARKIVERING_STARTET-feltet i T_JSAK-tabellen til Bisys med nytt tidsstempel for å sikre at journalposten ikke endres
av andre prosesser. Deretter `bidrag-dokument-arkivering` vil bestille tilgang til journalpostens dokument i `Midlertidig brevlager`.
Dette gjøres via en egen bestillingskø (WMQ) som `Midlertidig brevlager` monitorerer. `bidrag-dokument-arkivering` henter deretter en elektronisk kopi av
dokumentet via `Midlertidig brevlagers` SOAP-grenesnitt for dokumentformidling.

Det siste steget i prosessen er selve arkiveringen i Joark. Dette utføres ved hjelp av et kall mot REST-tjenesten `Dokarkiv/ JournalpostApi`. Kallet inneholder 
journalpostens metadata samt det tilhørende dokumentet. Dersom arkiveringen fullføres uten feil vil `bidrag-dokument-arkivering` svare med HTTP-kode 200. 
Responsen inneholder også journalpostens id i Joark. 

Hvis Dokarkiv svarer med HTTP-kode 200 vil `bidrag-dokument-arkivering` kalle igjen avviksrutinen for ARKIVERE_JOURNALPOST i `bidrag-dokument-journalpost` med parameter FULLFORT og joark journalpostid. `bidrag-dokument-journalpost` fyller da i JOARK_JP_ID feltet i T_JSAK tabellen samt oppdaterer ARKIVERING_FULLFORT tidstempel feltet.
Hvis Dokarkiv kallet feiler vil `bidrag-dokument-arkivering` kalle avviksrutinen for ARKIVERE_JOURNALPOST i `bidrag-dokument-journalpost` med parameter FEILET. `bidrag-dokument-journalpost` oppdaterer da ARKIVERING_FEILET feltet med nytt tidsstempel.
### bygg og kjør applikasjon

Dette er en spring-boot applikasjon. Den kan kjøres som ren java applikasjon, ved å
bruke `maven`, eller ved å bygge et docker-image og kjøre dette 

Se [Sikkerhet](#Sikkerhet) for kjøring med sikkerhet lokalt.

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

BidragDokumentArkiveringLocal brukes i stedet for BidragDokumentArkivering ved lokal kjøring.

AUD bidrag-q-localhost er lagt til for å støtte localhost redirect i preprod. Denne benyttes ved front-end-utvikling for å kunne kjøre tester med 
preprod-tjenester uten å måtte legge inn host-mappinger. bidrag-q-localhost-agenten er satt opp vha https://github.com/navikt/amag. Denne er ikke, 
og skal heller ikke være tilgjengelig i prod.

#### Oppskrift for kjøring med test-token i Swagger lokalt (ved integrasjonstesting mot AM eller bidrag-dokument-journalpost i NAIS, må token hentes fra bidrag-ui.<domene-navn>/session)
 - Start BidragDokumentArkivLocal som standard Java-applikasjon
 - Hent test-token [http://localhost:8080/local/jwt](http://localhost:8080/local/jwt)
 - Åpne Swagger (http://localhost:8080/swagger-ui.html)
 - Trykk Authorize, og oppdater value-feltet med: Bearer <testtoken-streng> fra steg 2.

 
#### Swagger Authorize 
Den grønne authorize-knappen øverst i Swagger-ui kan brukes til å autentisere requester om du har tilgang på et gyldig OIDC-token. For å benytte authorize må følgende legges i value-feltet:
   - "Bearer id-token" (hvor id-token erstattes med et gyldig id-token (jwt-streng))
 
For localhost kan et gyldig id-token hentes med følgende URL (gitt BidragDokumentArkivLocal er startet på port 8080):
   - [http://localhost:8080/local/jwt](http://localhost:8080/local/jwt)
   
For preprod kan følgende CURL-kommando benyttes (krever tilgang til isso-agent-passord i Fasit for aktuelt miljø): 
 
```
  curl -X POST \
	   -u "{isso-agent-brukernavn}:{isso-agent-passord}" \
	   -d "grant_type=client_credentials&scope=openid" \
	   {isso-issuer-url}/access_token
```
  
hvor `{isso-agent-brukernavn}` og `{isso-agent-passord}` hentes fra Fasit-ressurs OpenIdConnect bidrag-dokument-ui-oidc for aktuelt miljø (f.eks [https://fasit.adeo.no/resources/6419841](https://fasit.adeo.no/resources/6419841) for q2),
og `{isso-issuer-url}` hentes fra Fasit-ressurs BaseUrl isso-issuer (f.eks [https://fasit.adeo.no/resources/2291405](https://fasit.adeo.no/resources/2291405) for q2.

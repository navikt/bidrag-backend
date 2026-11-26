# bidrag-sak

Applikasjon som henter metadata på brukere som tilhører en sak i BISYS.

### Logisk inndeling
Applikasjonen er delt inn i to logiske grupper med tilhørende endepunkter:
- <b>Bidrag sak</b> leverer metadata for bidragsaker. Generelle konsumenter av bidrag saksinformasjon. Sikret med OpenId Connect vba [token-support](navikt/token-support). 
- <b>Bidrag sak PIP</b> - Leverer metadata for bidragsaker. Fungerer som policy information point for Bidrag datakonsumenter i forbindelse med ABAC-oppslag. 

### Regler for saksroller

Reglene står også i Springdoc for `POST /sak`, `POST /sak/oppdater` og `POST /sak/oppdater/roller` (`controller/SakRegler.kt`).

#### Saksroller (opprettelse og endring)
- Alle roller med fødselsnummer i forespørselen (BP, BM, BA, RM og FR) må finnes i bidrag-person. En RM som er samhandler, må finnes som samhandler.
- Fødselsnummer kan ikke være tom streng.
- Reell mottaker kan ikke ha tom ident.
- Minst én av rollene BP, BM eller BA må være knyttet til en kjent person.
- En person kan bare ha én rolle i saken, med unntak av RM og FR.
- Samme barn kan ikke sendes inn to ganger i én forespørsel.
- Saken kan ha maks én BM og én BP.
- RM kan bare registreres på barn.
- Barn fra og med 18 år må ha RM. Er fødselsdatoen ukjent etter oppslag, kreves ikke RM. Feiler oppslaget, feiler forespørselen.
- Verge styres av `reellMottaker.verge`. Mangler `reellMottaker`, brukes `mottagerErVerge`, som er deprecated. Verge lagres både på barnets rolle og på RM-rollen, fordi Bisys leser den fra RM-rollen. Verge krever reell mottaker og må være en person.
- Landkoden må finnes i kodeverket.

#### Ved hver lagring
- bidrag-sak bestemmer eierfogd fra arbeidsfordelingen i Norg og returnerer den i svaret. Adressebeskyttelse (kode 6/7) hentes fra PDL og skjerming fra skjermingsregisteret, for alle nåværende og tidligere parter. Norg får den strengeste av dem sammen med geografisk tilknytning og velger enheten. Har saken arbeidsfordeling `INH`, brukes settekontoret til enheten, unntatt når en part har adressebeskyttelse eller skjerming. Ved rolleendring skjer dette bare når toggelen `arbeidsflyt.flytt_oppgaver_ved_enhetsbytte` er på, slik at oppgavene flyttes med.
- bidrag-sak setter `ansatt` på saken: `true` når en nåværende eller tidligere part er skjermet i skjermingsregisteret, ellers `false`. Bisys krever utvidede rettigheter for saker med flagget.
- `inhabilitet` settes bare i det gamle rollebildet i Bisys. Ny løsning endrer ikke flagget.

#### Ved opprettelse (`POST /sak`)
- Har samme BP og BM en sak med arbeidsfordeling EEN fra før, returneres den saken, og det opprettes ingen ny. Mangler saksbehandleren tilgang til den saken, svarer endepunktet 403 uten data.
- Forespørselen har ikke `eierfogd`, `ansatt` eller `inhabilitet`. Sendes feltene, ignoreres de.

#### Ved endring (`POST /sak/oppdater` og `POST /sak/oppdater/roller`)
- Saksbehandleren må ha tilgang til saken. Ellers svarer endepunktet 403.
- Kjente BP-, BM- og BA-roller kan ikke fjernes eller endres. RM kan endres.
- Bare roller i forespørselen valideres. Lagrede roller som ikke er med, sjekkes ikke på nytt, heller ikke om de finnes i bidrag-person eller om myndige barn mangler RM.
- FR kan ikke endres. FR-roller i forespørselen ignoreres.
- bidrag-sak bruker gjeldende ident fra bidrag-person for alle roller unntatt FR, både i forespørselen og for lagrede roller. Gjeldende ident er folkeregisteridenten, eller NPID hvis personen ikke har folkeregisterident. Identbytter på lagrede roller lagres i rollehistorikken som `ENDRE_FNR`. Historisk og gjeldende ident regnes som samme person.
- Forespørselen har ikke `ansatt` eller `inhabilitet`. Sendes feltene, ignoreres de.

#### Unntak
- Eldre `POST /bidrag-sak/sak/ny` kan opprette en sak uten roller.
  [«Kopier fra annet fagområde» i bidrag-frontend](https://github.com/navikt/bidrag-frontend/blob/main/apps/dokument/src/common/components/avvik/components/types/KopierFraAnnenFagomrade.tsx)
  bruker endepunktet når et dokument skal knyttes til en ny sak. Senere oppdatering krever en kjent rolle.

### Sikkerhet
<b>Bidrag sak</b> er sikret med navikt [token-validation-spring](https://github.com/navikt/token-support/tree/master/token-validation-spring)
fra [token-support](https://github.com/navikt/token-support). Det betyr at gyldig OIDC-id-token må være inkludert som Bearer-token i Authorization header for alle
spørringer mot disse endepunktene. Tokenets audience (AUD) må være bidrag-ui, bidrag-dokument-ui, eller bisys, dvs at tokenet må hentes fra NAV AM av en av disse applikasjonene. 

<b>Bidrag sak PIP</b> bruker `GET /v2/pip/sak/{saksnummer}` med JWT fra Entra ID og rollen `sak-pip`.

###### Eksterne konsumenter
Tjenester utenfor bidragsdomenet kan benytte seg av bidrag-sak-apiet til å hente informasjon om bidragsaker gitt følgende:
 - Kall inneholder et OIDC Bearer-token ("Bearer " + {OIDC-token}) i Authorization header 
 - Tokenet må være utstedt av enten Azure AD eller OpenAM.
 - Tokenets audience (aud) må være registrert i bidrag-sak (bestilles i [#team-bidrag](https://nav-it.slack.com/app_redirect?channel=team%20bidrag)). 
   For dev er også ida-q godkjent.
 - Tokenet må være utstedt til en bruker som er medlem av AD-gruppa 0000-GA-Bidrag-sak-lese (tokenets 'sub' holder brukerid). Medlemskap i denne gruppa gir [basis-tilgang](https://confluence.adeo.no/display/ABAC/Basis+tilgang+for+bidrag) for bidrag i ABAC
  

### Profiler
- <b>live</b> profilen brukes for kjøring på NAIS i både prod og dev (preprod). 
- <b>integration-test</b> profilen brukes for å kjøre integrasjonstester mot eksterne tjenester fra lokalt utviklingsmiljø
- <b>test</b> profilen brukes ved kjøring av enhetstester
- <b>h2</b> profilen brukes for å kjøre integrasjonstester med H2 in-memory database uten å kreve Docker eller testcontainers

### Lokal kjøring
Gjelder alle profiler utenom live ved kjøring fra lokalt utviklingsmiljø. 

URL for lokal Swagger: http://localhost:8090/bidrag-sak/swagger-ui.html
URL for H2-konsoll: http://localhost:8090/bidrag-sak/h2-console

#### hemmeligheter
Legges inn som miljøvariabler ved lokal kjøring. F.eks må følgende miljøvariabler må settes for å kjøre profilen integration-test:
* `BIDRAG_DB_PASSWORD` (integration-db2, [Fasit - BidragDataSource](https://fasit.adeo.no/resources/528952)
* `ABAC_PASSWORD` (integration-test, [Vault - srvbdsak-dev](https://vault.adeo.no/ui/vault/secrets/serviceuser/show/dev/srvbdsak))

### test-token
Profilene test og integration db2 benytter [token-validation-test-support](https://github.com/navikt/token-support/tree/master/token-validation-test-support)
som blant annet sørger for at det genereres id-tokens til test formål. For å redusere risikoen for at testgeneratoren ved en feil gjøres aktiv i produksjon er
token-validation-test-support-modulen kun tilgjengelig i test-scope. I tillegg er bruken av testgeneratoren kun knyttet til en egen spring-boot app-definisjon,
BidragDokumentLocal (lokalisert under test) som benytter test-profil.

AUD bidrag-q-localhost er lagt til for å støtte localhost redirect i preprod. Denne benyttes ved front-end-utvikling for å kunne kjøre tester med
preprod-tjenester uten å måtte legge inn host-mappinger. bidrag-q-localhost-agenten er satt opp vha https://github.com/navikt/amag. Denne er ikke, 
og skal heller ikke være tilgjengelig i prod.

BidragSakLocal brukes for å f.eks bruke Swagger lokalt med test eller integration-db2-profilene.

#### bygg og kjør applikasjon

Dette er en spring-boot applikasjon og kan kjøres som ren java applikasjon, ved å
bruke `maven` eller ved å bygge et docker-image og kjøre dette 

##### java og maven
* krever installasjon av java og maven:

  * `mvn clean install`<br>
    deretter
  * `cd bidrag-sak`
  * `mvn spring-boot:run`<br>
     eller
  * `cd bidrag-sak/target`<br>
  * `java -jar bidrag-sak-<versjon>.jar`

##### docker og maven
* krever installasjon av java, maven og docker
* docker image er det som blir kjørt som nais applikasjon

  * `mvn clean install`<br>
  deretter<br>
  * `docker build -t bidrag-sak .`
  * `docker run -p 8090:8090 bidrag-sak`

Etter applikasjon er startet kan den nåes med browser på
`http://localhost:8090/bidrag-sak/swagger-ui.html`

#### Oppskrift for kjøring lokalt med test-token i Swagger (ved integrasjonstesting mot AM eller ABAC må token hentes fra bidrag-ui.<domene-navn>/session)
 - Start BidragSakLocal som standard Java-applikasjon
 - Hent test-token [http://localhost:8080/bidrag-sak/local/jwt](http://localhost:8090/bidrag-sak/local/jwt)
 - Åpne Swagger (http://localhost:8080/bidrag-sak/swagger-ui.html)
 - Trykk Authorize, og oppdater value-feltet med: Bearer <testtoken-streng> fra steg 2.     

#### Testskript for integrasjontesting mot ABAC
Demonstrerer at saksbehandler med paragraf 19-tilgang kan lese metadata til paragraf 19 sak, mens saksbehandler uten denne tilgangen vil ikke kunne hente opplysninger om 
samme sak (se [ABAC klartekstpolicies for Bidrag](https://confluence.adeo.no/pages/viewpage.action?pageId=309322099)). 

- Klargjør to testbrukere fra IDA. Én med og en uten rolle 0000-GA-Bisys-FFU_Utvidet.
- Hent passord til ABAC-bruker, srv, fra [vault](https://vault.adeo.no/ui/vault/secrets/serviceuser/show/dev/srvbdsak) (serviceuser < dev < srvbdsak)
- Legg passordet over inn som miljøvariabel i din IDE.
- Identifiser én testperson knyttet til paragraf 19-sak
    - Sjekk insert i test > java > resource > testdata > testdataLocal.sql, eller
    - Logg på [H2-konsollet](http://localhost:8090/bidrag-sak/h2-console), og hent person fra T_ROLLE 
- Start BidragSakIntegrationTest
- Hent id-token for hver av testbrukerne identifisert i første steg
    - Logg på [bidrag-ui](https://bidrag-ui.nais.preprod.local) 
    - Naviger til /session etter innlogging
 - Åpne lokal [Swagger UI](http://localhost:8090/bidrag-sak/swagger-ui.html)
    - Fyll inn bearer-token-feltet under Authorization-knappen med "Bearer <oidc-token>" hvor oidc-token er token til testbrukeren du ønsker å teste tilgang for
    - Fyll inn FNR til testperson knyttet til $19 sak i FNR-feltet til bidrag-sak-controller /person/sak/{fodselsnummer} "Finn metadata om for en bidragssak"
    - Trykk execute
    - Gjenta for den andre $19 testpersonen, og verifiser motsatt resultat

#### Kjøre tester uten Docker
Det er mulig å kjøre integrasjonstester uten Docker/testcontainers ved å bruke H2 in-memory database:

```bash
mvn test -Dtest=BidragSakServiceInMemoryTest
```

Denne testklassen er en kopi av `BidragSakServiceIntegrationIT` som bruker `SpringInMemoryTestRunner` i stedet for `SpringTestRunner`. 
`SpringInMemoryTestRunner` konfigurerer Spring til å bruke H2-databasen med `application-h2.yaml` profilen. 
H2 kjører med DB2-kompatibilitetsmodus og bruker Flyway-migreringer fra `src/test/resources/db/migration` for å opprette riktig database-skjema, 
som gjør det mulig å kjøre testene uten å ha Docker installert.

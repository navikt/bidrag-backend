# Bidrag-Aktoerregister

* DEV-Intern: https://bidrag-aktoerregister.intern.dev.nav.no/ (må være koblet til naisdevice for å få tilgang).
* DEV-Ekstern: https://bidrag-aktoerregister.ekstern.dev.nav.no/.
* PROD: https://bidrag-aktoerregister.nav.no/. Applikasjonen kjører, men ingen har foreløpig tilgang til den da dette må konfigureres manuelt av `#tech-sikkerhet`.

Applikasjonen har ansvar for å holde oversikt over endringer i navn, adresse og kontonummer for aktører involvert i bidragssaker. Utover personer kan slike aktører være blant annet kommuner, institusjoner, spesifike avdelinger innenfor en oranisasjon, utlandske myndigheter eller sperrede bankkontoer.

For personer er det kun opplysninger om kontonummer som følges opp, men for de andre typene aktører følges også navn og adresse. Informasjonen om aktørene kan hentes ved kall med identtype og ident.

## Hendelse-API
I stedet for at konsumentene skal spørre om opplysningene for hver enkelt aktør jevnlig er det lagt opp et hendelses-API. En hendelse inneholder et sekvensnummer og en aktørId. Ved kall til hendelse-APIet sendes det med første sekvensnummer som ønskes og maksimalt antall hendelser.

Sekvensen i hendelses-strømmen vil alltid være stigende, men det kan forekomme hull i rekken. Spørres det etter et sekvensnummer som ikke eksisteres får man det neste i stedet, samt de etterfølgende hendelsene inntil det ikke er flere hendelser igjen eller maksimalt antall er nådd. Dersom det ikke finnes noen hendelser med etterspurt sekvensnummer eller høyere returneres en tom liste.

Konsumenten er selv ansvarlig for å huske hvilket sekvensnummer som skal hentes ut som det neste.

Hendelsene inneholder i seg selv ikke endringene, disse må hentes for aktørId'n dersom det er interessant for konsumenten.

## Endepunkter

### Hent aktør

Returnerer informasjon om aktør av type `identType` med id `ident`. `identType` kan være `PERSONNUMMER` eller `AKTOERNUMMER`. Ved oppslag på ident av type `PERSONNUMMER` returneres kun kontoinformasjon. Ved oppslag på ident av type `AKTOERNUMMER` returneres både kontoinformasjon og adresseinformasjon.

### Hent hendelser

Returnerer en liste av aktører som har blitt oppdatert siden sekvensnummer `fraSekvensnummer`. Antall aktøerer i den returnerte listen styres av parameteren `antall`. Den returnerte listen vil aldri inneholde flere innslag av samme aktør. Altså kan det hende at det siste sekvensnummeret som returneres er større enn `fraSekvensnummer` + `antall`. Den returnerte hendelseslisten vil være sortert etter sekvensnummer i stigende rekkefølge.

## Integrasjoner

### Bidrag-samhandler

Ved forespørsel etter aktør på ident med identtype `AKTOERNUMMER` vil applikasjonen hente aktørinformasjon fra bidrag-samhandler dersom vi ikke allerede har informasjonen i databasen. 
Den mottatte aktøren lagres så i egen database før den returneres. Aktørinformasjon fra bidrag-samhandler inneholder både konto- og adresse-informasjon.

I tillegg til at applikasjonen henter aktørinformasjon om forespurte aktører dersom de ikke allerede finnes i databasen, er det også satt opp en batch-jobb som sjekker om aktørene med identtype `AKTOERNUMMER` har blitt oppdatert i bidrag-samhandler siden sist de ble hentet. Aktørene som er endret vil oppdateres i applikasjonens database. Dette medfører også nye hendelser for de oppdaterte aktørene.

### Bidrag-person

Ved forespørsel etter aktør med identtype `PERSONNUMMER` vil applikasjonen hente aktørinformasjon fra bidrag-person dersom vi ikke allerede har informasjonen i databasen. 
Aktørinformasjon fra bidrag-person inneholder kontoinformasjon, adresseinformasjon, navn, dødsbo m.m.


## Database

Applikasjonen benytter `PostgreSQL` i GCP for lagring av aktører og hendelser. Provisjonering av databasen gjøres gjennom konfigurasjon i `nais.yaml`. Alle nødvendige tabeller settes opp automatisk ved hjelp av `Flyway` migrasjoner, som kjøres ved oppstart av app. I tillegg til aktør og hendelse tabeller opprettes det også tabeller for håndtering av batch-jobb mot TSS. Dette er tabeller for å sørge for at vi ikke trigger den samme jobben på flere pods og for å kunne holde oversikt over status på jobb-kjøringer. For batch-jobber brukes `Spring-batch`. `Shedlock` brukes for å begrense batch-jobb til å kjøre på 1 pod.

`Flyway` migrerings-script ligger under `/resources/db/migration/` og følger en bestemt navn-konvensjon. Dersom man skal endre på tabeller i en eksisterende database må man opprette nye scripts/filer for dette. Hvis man forsøker å endre i eksisterende filer vil man få feil ved oppstart.

## Maskinporten

Endepunktene i applikasjonen som utleverer aktør- og hendelseinformasjon krever maskinporten-tokens med scope `nav:bidrag:aktoerregister.read`. 
Foreløpig kan token med riktig scope genereres av NAV og Skatteetaten. Dette er også konfigurert i `nais.yaml`.

## Kjør applikasjon lokalt

For å bruke `docker-compose.yaml` til å kjøre opp PostgreSQL må man stå på rotnivå av prosjektet i terminalen og kjøre 

```docker-compose up -d```

Nå vil PostgreSQL starte. 

Nå kan man kjøre opp applikasjonen med spring profilen `local`.

## Kjøring av tester

Noen av testene benytter `testcontainers` som krever at Docker kjører på maskinen.

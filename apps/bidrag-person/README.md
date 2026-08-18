# bidrag-person
![](https://github.com/navikt/bidrag-person/workflows/continuous%20integration/badge.svg)
![](https://github.com/navikt/bidrag-person/workflows/release%20bidrag-person/badge.svg)

Mikroservice for personinformasjon

### Bygg og kjør applikasjon

Dette er en spring-boot applikasjon og kan kjøres som ren java applikasjon, ved å
bruke `maven` eller ved å bygge et docker-image og kjøre dette

##### java og maven
* krever installasjon av java og maven:

    * `mvn clean install`, deretter
        * `cd bidrag-person`
        * `mvn spring-boot:run`

  eller
    * `cd bidrag-person/target`
    * `java -jar bidrag-person-<versjon>.jar`

##### docker og maven
* krever installasjon av java, maven og docker
* docker image er det som blir kjørt som nais applikasjon

    * `mvn clean install`, deretter
        * `docker build -t bidrag-person .`
        * `docker run -p 8080:8080 bidrag-person`

Etter applikasjon er startet kan den nåes med browser på
`http://localhost:8080/bidrag-person/swagger-ui.html`

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

BidragPersonLocal brukes i stedet for BidragPerson ved lokal kjøring.

Miljøvariabelen 'ACCEPTED_AUDIENCE' angir hvilke apper som godkjennes som token-uthentere. Denne variabelen må settes i Vault for kjøring på NAIS.
Audience bidrag-q-localhost er lagt til for å støtte localhost redirect i preprod. Denne benyttes ved front-end-utvikling for å kunne kjøre tester
med preprod-tjenester uten å måtte legge inn host-mappinger. bidrag-q-localhost-agenten er satt opp vha https://github.com/navikt/amag.
Denne er ikke, og skal heller ikke være tilgjengelig i prod.

#### Oppskrift for kjøring lokalt med test-token i Swagger (ved integrasjonstesting mot AM eller ABAC må token hentes fra bidrag-ui.<domene-navn>/session)
- Start BidragPersonLocal som standard Java-applikasjon
- Hent test-token [http://localhost:8090/bidrag-person/local/jwt](http://localhost:8090/bidrag-person/local/jwt)
- Åpne Swagger (http://localhost:8090/bidrag-person/swagger-ui.html)
- Trykk Authorize, og oppdater value-feltet med: Bearer <testtoken-streng> fra steg 2.

### Tjenester
####/bidrag-person/informasjon/{ident}
Henter ut informasjon om en person (PDL graphql) 

####/bidrag-person/geografisktilknytning/{ident}
Henter ut geografisk tilknytning for en person (PDL graphql) . Kan kun aksesseres med token utstedt av STS for systembruker.

#### Kjøre lokalt mot sky
For å kunne kjøre lokalt mot sky må du gjøre følgende

Åpne terminal på root mappen til `bidrag-person`
Konfigurer kubectl til å gå mot kluster `dev-fss`
```bash
# Sett cluster til dev-fss
kubectx dev-fss
# Sett namespace til bidrag
kubens bidrag 

# -- Eller hvis du ikke har kubectx/kubens installert 
# (da må -n=bidrag legges til etter exec i neste kommando)
kubectl config use dev-fss
```
Deretter kjør følgende kommando for å importere secrets. Viktig at filen som opprettes ikke committes til git

```bash
kubectl exec --tty deployment/bidrag-person-feature printenv | grep -E 'AZURE_|TOKEN_X|_URL|SCOPE' > src/main/resources/application-lokal-nais-secrets.properties
```

Deretter kan tokenet brukes til å logge inn på swagger-ui http://localhost:8080/bidrag-person/swagger-ui/index.html og teste ut ulike api kall
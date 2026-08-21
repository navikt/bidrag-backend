# bidrag-dokument-produksjon

Mikrotjeneste for produksjon av dokumenter (PDF/HTML) fra dokumentmaler. Brukes av
bl.a. `bidrag-behandling` og `bidrag-bidragskalkulator` for å generere vedtaksbrev,
notater og privatavtaler.

`bidrag-dokument-produksjon` orkestrerer to frittstående, men tett koblede,
tjenester som ligger i samme mappe:

* [`bidrag-dokumentmal`](bidrag-dokumentmal) — Node/React-app (React Router) som
  rendrer dokumentmaler til HTML.
* [`bidrag-pdfgen`](bidrag-pdfgen) — Python/WeasyPrint-tjeneste som konverterer HTML
  til PDF (flatning/konvertering).

Disse to er **ikke** Maven-prosjekter og er derfor ikke registrert som moduler i
`pom.xml` — de bygges og deployes uavhengig via sine egne GitHub Actions-workflows
(se punktet under). De ligger likevel i samme mappe som `bidrag-dokument-produksjon`
fordi de tre er utviklet og endres sammen.

### hensikt

`bidrag-dokument-produksjon` tilbyr et REST-grensesnitt for å produsere PDF/HTML av
en gitt dokumentmal og et payload (vedtak, notat e.l.):

1. Henter ferdig utfylt HTML for malen fra `bidrag-dokumentmal`.
2. Konverterer HTML til PDF via `bidrag-pdfgen` (WeasyPrint), eventuelt flatning av
   eksisterende PDF-er.

### bygg og kjør applikasjon

Dette er en spring-boot applikasjon og kan kjøres som ren java applikasjon, ved å
bruke `maven` eller ved å bygge et docker-image og kjøre dette.

##### java og maven

* krever installasjon av java og maven:

  * `mvn clean install`, deretter
  * `cd apps/bidrag-dokumenthåndtering/bidrag-dokument-produksjon`
  * `mvn spring-boot:run`

  eller
  * `cd apps/bidrag-dokumenthåndtering/bidrag-dokument-produksjon/target`
  * `java -jar app.jar`

Merk: Kildekoden ligger under `bidrag-dokument-produksjon/src` (ett nivå dypere enn
vanlig), for å holde denne appen i samme mappe som `bidrag-dokumentmal` og
`bidrag-pdfgen`, slik de lå i det opprinnelige repoet.

##### docker og maven

* krever installasjon av java, maven og docker
* docker image er det som blir kjørt som nais applikasjon

  * `mvn clean install`, deretter
  * `docker build -t bidrag-dokument-produksjon .`
  * `docker run -p 8080:8080 bidrag-dokument-produksjon`

Etter applikasjon er startet kan den nås med browser på
`http://localhost:8080/swagger-ui/index.html`

### bygg og kjør bidrag-dokumentmal / bidrag-pdfgen

Se README i hhv. [`bidrag-dokumentmal`](bidrag-dokumentmal/README.md) og
[`bidrag-pdfgen`](bidrag-pdfgen) for hvordan disse kjøres lokalt. Kort
oppsummert:

* `bidrag-dokumentmal`: `npm install && npm run dev` (se `package.json` for øvrige
  scripts).
* `bidrag-pdfgen`: `docker compose up` (se `docker-compose.yaml`).

For lokal kjøring mot `bidrag-dokument-produksjon` pekes det på disse via
miljøvariablene `BIDRAG_DOKUMENTMAL_URL` og `BIDRAG_PDFGEN_URL`
(se `src/test/resources/application-local.yaml`).

### Nais og deploy

Hver av de tre appene har sin egen NAIS-applikasjon og deployes uavhengig:

* `.nais/bidrag-dokument-produksjon` + `.github/workflows/bidrag-dokument-produksjon.yaml`
* `.nais/bidrag-dokumentmal` + `.github/workflows/bidrag-dokumentmal.yaml`
* `.nais/bidrag-pdfgen` + `.github/workflows/bidrag-pdfgen.yaml`

### 👥 Eierskap

**Team Bidrag** er ansvarlig for vedlikehold av disse applikasjonene.

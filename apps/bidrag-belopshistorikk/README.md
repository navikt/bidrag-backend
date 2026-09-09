# Bidrag-beløpshistorikk

Repo for behandling av beløpshistorikk (stønad og engangsbeløp) i Bidrag.
Ved nye vedtak for en stønad vil alltid periodene i det nye vedtaket erstatte eksisterende perioder i stønaden.
Ved overlapp vil eksisterende perioder merkes som ugyldiggjorte og nye perioder med identiske verdier opprettes
for periodene som eventuelt ikke dekkes av det nye vedtaket. Tilsvarende gjelder for engangsbeløp.

## Håndtering av identer

Identer som er lagret på en stønad eller et engangsbeløp kan bli utdaterte, for eksempel når en person får nytt
fødselsnummer. Tjenesten håndterer dette slik:

- **Utlevering:** Skyldner, kravhaver og mottaker slås opp mot bidrag-person ved henting, og det er alltid den nyeste
  identen som returneres — uavhengig av hvilken ident som er lagret i databasen.
- **Oppslag:** Ved søk etter stønad eller engangsbeløp brukes alle historiske identer, slik at treff også gis på
  utdaterte identer.
- **Opprettelse:** Det forutsettes at identene i innkommende vedtak er de nyeste. De lagres uendret.
- **Oppdatering:** Identene i vedtaket som oppdaterer stønaden regnes som de gjeldende, og erstatter identene som er
  lagret på stønaden. Nye engangsbeløp opprettes tilsvarende med identene fra oppdateringen.

#### Kjøre lokalt mot Nais postgres database
For å kunne kjøre lokalt mot sky må du gjøre følgende

Åpne terminal på root mappen til `bidrag-belopshistorikk`

Sett opp nødvendige miljøvariabler med følgende kommander
```bash
# Sett opp miljøvariabler
./initEnv.sh
# Start opp lokal kafka med docker
docker-compose up -d
```
Start opp proxy mot Q2 databasen med følgende kommando

```bash
nais postgres proxy -p 5598 bidrag-belopshistorikk-q2 --reason "Koble til databasen for lokal kjøring" --team bidrag --environment dev-gcp

```
Deretter start opp BidragBeløpshistorikkLokalNais med følgende miljøvariaber

``DB_USERNAME=<din Nav epost>``

## Access token for swagger
Kopier ut token fra:
- q2 https://azure-token-generator.intern.dev.nav.no/api/obo?aud=dev-gcp.bidrag.bidrag-belopshistorikk-q2
- q1 https://azure-token-generator.intern.dev.nav.no/api/obo?aud=dev-gcp.bidrag.bidrag-belopshistorikk-q1

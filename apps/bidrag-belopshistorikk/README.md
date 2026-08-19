# Bidrag-beløpshistorikk

Repo for behandling av beløpshistorikk (stønad og engangsbeløp) i Bidrag.
Ved nye vedtak for en stønad vil alltid periodene i det nye vedtaket erstatte eksisterende perioder i stønaden.
Ved overlapp vil eksisterende perioder merkes som ugyldiggjorte og nye perioder med identiske verdier opprettes
for periodene som eventuelt ikke dekkes av det nye vedtaket. Tilsvarende gjelder for engangsbeløp.

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

```2
Deretter start opp BidragBeløpshistorikkLokalNais med følgende miljøvariaber

``DB_USERNAME=<din Nav epost>``

## Access token for swagger
Kopier ut token fra:
- q2 https://azure-token-generator.intern.dev.nav.no/api/obo?aud=dev-gcp.bidrag.bidrag-belopshistorikk-q2
- q1 https://azure-token-generator.intern.dev.nav.no/api/obo?aud=dev-gcp.bidrag.bidrag-belopshistorikk-q1

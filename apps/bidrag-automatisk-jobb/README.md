# Bidrag-automatisk-jobb
Applikasjon for å kjøre jobber, primært batcher, for å utføre endringer i bidragssaker.

## Beskrivelse

### Aldersjustering
Bidrag-automatisk-jobb lagrer alle barn som er part i en bidragssak, og aldersjusterer dem.
Aldersjustering skjer for alle barn det året de fyller 6, 11 og 15 år, basert på sjablongverdier.
Erstatter Bisys-batchen FB260. Se full dokumentasjon: [docs/aldersjustering-bidrag.md](docs/aldersjustering-bidrag.md).

### Indeksregulering bidrag
Regulerer løpende bidragssatser i takt med konsumprisindeksen. Erstatter Bisys-batchen FB020.
Kun bidrag som løper i norske kroner (NOK) støttes p.t. Se full dokumentasjon:
[docs/indeksregulering-bidrag.md](docs/indeksregulering-bidrag.md).

### Revurdering forskudd
Bidrag-automatisk-jobb henter ut alle løpende forskudd det ikke har blitt gjort manuelle endringer på i løpet av x 
siste måneder og utfører en beregning for å sjekke om forskudded skal settes ned. 

## Kjøre applikasjonen lokalt

Kjør initEnv.sh for å sette miljøvariabler. Dette vil sette alle påkrevde miljøvariabler.
Start opp applikasjonen ved å kjøre [BidragAldersjusteringLocal.kt](src/test/kotlin/no/nav/bidrag/automatiskjobb/BidragAutomatiskJobbLocal.kt).

## Batch-dokumentasjon

Detaljert dokumentasjon for de årlige batch-jobbene ligger i [docs/](docs):

- [docs/aldersjustering-bidrag.md](docs/aldersjustering-bidrag.md) – aldersjustering av bidrag (erstatter FB260)
- [docs/indeksregulering-bidrag.md](docs/indeksregulering-bidrag.md) – indeksregulering av bidrag (erstatter FB020)

# Bidrag-person-hendelse

## Kort beskrivelse
Mottaksapplikasjon for personhendelser. Lytter på ulike hendelser fra PDL (fødsler, dødsfall mm). 
Distribuerer disse videre til Bisys via MQ og andre bidragsapplikasjoner via Kafka.

## Nøkkelinformasjon
| Felt             | Verdi                                                                                                          |
|------------------|----------------------------------------------------------------------------------------------------------------|
| Navn             | Bidrag-person-hendelse                                                                                         |
| Lenke            | |
| Applikasjonstype | Backend-tjeneste, varslingstjeneste                                                                            |
| Driftsområde     | GCP                                                                                                            |
| GitHub           | [navikt/bidrag-person-hendelse](https://github.com/navikt/bidrag-person-hendelse)                              |
| Har database     | [Ja](https://console.cloud.google.com/sql/instances/bidrag-person-hendelse/overview?project=bidrag-prod-8f72)  |
| Kafka-topic      | [Ja](https://github.com/navikt/bidrag-person-hendelse/blob/main/.github/workflows/deply-kafka-topics-prod.yml) |

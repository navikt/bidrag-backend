# Bidrag-elin-stub

## Beskrivelse

Bidrag-elin-stub er en applikasjon for å tilby stubber mot endepunkter på skatteetatens applikasjon Elin. Bidrag-elin-stub lever kun på https://bidrag-elin-stub.intern.dev.nav.no og kan ikke prodsettes.


Applikasjon tilbyr stubber for følgende endepunkter:

| Endepunkt              | Beskrivelse                                                                                                                                                                                                                                           | Responser                                                       | Testdata                                   |
|------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------|--------------------------------------------|
| bidrag/v1/bidragskrav  | Endepunkt for motak av krav fra Bidrag-Regnskap som skal sendes til Skatteetaten.<br/> Stubben er konstruert slik at kall med delytelsesId som ikke finnes i testdata<br/>returnerer OK, mens delytelsesId'er som finnes returnerer en konteringfeil. | 200: Tom response body<br/>400: Liste over feilede konteringer  | delytelsesId: <br/>123456789<br/>123456780 |


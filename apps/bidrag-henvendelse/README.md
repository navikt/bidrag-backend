# bidrag-henvendelse

En tjeneste som utleverer henvendelsene (chat, meldingskjeder, samtalereferater) på en person til
brukeroversikten i bidrag-frontend. Oversetter lista fra **sf-henvendelse-api-proxy**
(namespace `teamnks`, proxy mot henvendelsesløsningen i Salesforce) til bidragsdomenet.

Erstatter `HenvendelselisteProvider` og `HenvendelseRsConsumer` i BiSys (Favro NAV-30542).
## Feltene

| Felt | Betydning |
|------|-----------|
| `kjedeId` | id-en til meldingskjeden. Frontend bygger Modia-lenka ut fra denne |
| `henvendelsestype` | `CHAT`, `MELDINGSKJEDE`, `SAMTALEREFERAT` eller `UKJENT` |
| `tema` | tema-kode fra felles kodeverk, f.eks. `BID`. Dekodes av frontend |
| `temagruppe` | temagruppe-kode, f.eks. `FMLI`. Dekodes av frontend |
| `sisteMeldingSendt` | `max(meldinger[].sendtDato)`. `null` når kjeden er tom eller ingen melding har dato |

Samme kolonner som BiSys viser i Brukeroversikt, minus `Enhet` (BiSys setter den alltid til
`null`) og uten grensa på fem rader - frontend avgjør hvor mange som vises.


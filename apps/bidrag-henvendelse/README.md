# bidrag-henvendelse

En tjeneste som utleverer henvendelsene (chat, meldingskjeder, samtalereferater) på en person til
brukeroversikten i bidrag-frontend. Oversetter lista fra **sf-henvendelse-api-proxy**
(namespace `teamnks`, proxy mot henvendelsesløsningen i Salesforce) til bidragsdomenet.

Erstatter `HenvendelselisteProvider` og `HenvendelseRsConsumer` i BiSys (Favro NAV-30542).

## Avhengigheter

| Tjeneste                  | Hva vi bruker den til                        |
|---------------------------|----------------------------------------------|
| `bidrag-person`           | veksle fødselsnummer til aktørid             |
| `sf-henvendelse-api-proxy`| hente henvendelseslista for en aktørid       |

Begge kalles on-behalf-of, med saksbehandlerens identitet. `sf-henvendelse-api-proxy` svarer
`403 Machine token authorization not sufficient` utenfor `/kodeverk/`.

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

## Bruk

| Metode | Path             | Beskrivelse                                |
|--------|------------------|--------------------------------------------|
| POST   | `/henvendelser`  | Henvendelsene til personen i request-body  |

Identen sendes i body (`PersonRequest`). `tema` og `temagruppe` dekodes av frontend, som har
egen kodeverk-klient. Personer uten aktørid gir tom liste.

```
POST /henvendelser
Authorization: Bearer <on-behalf-of-token>
Content-Type: application/json
```

```json
{ "ident": "17490123474" }
```

**Svar (200):**

```json
{
  "henvendelser": [
    {
      "kjedeId": "a0J3N000004dUBJUA2",
      "henvendelsestype": "MELDINGSKJEDE",
      "tema": "BID",
      "temagruppe": "FMLI",
      "sisteMeldingSendt": "2026-06-28T09:30:00Z"
    },
    {
      "kjedeId": "a0J3N000004dUBKUA2",
      "henvendelsestype": "SAMTALEREFERAT",
      "tema": "BID",
      "temagruppe": "FMLI",
      "sisteMeldingSendt": null
    }
  ]
}
```

Tom liste er `{ "henvendelser": [] }`, aldri `null`.

**Svar ved feil**, som `ProblemDetail` - frontend har typen i `packages/api/src/ProblemDetail.ts`:

```json
{
  "type": "about:blank",
  "title": "Feil ved kall mot tjeneste",
  "status": 502,
  "detail": "Kunne ikke hente henvendelser fordi en tjeneste vi er avhengig av svarte med feil."
}
```

| Status | Når |
|--------|-----|
| 400 | identen er ikke et gyldig fødselsnummer eller d-nummer |
| 401 | manglende eller ugyldig token |
| 502 | en tjeneste vi er avhengig av feilet eller svarte ikke |
| 500 | uventet feil hos oss |

Detaljene står i loggen: feilmeldingene inneholder URL-er med aktørid og verdier fra
request-body.

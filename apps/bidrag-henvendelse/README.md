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

## Bygge

```bash
mvn -pl apps/bidrag-henvendelse -am clean verify        # bygg + tester + ktlint
mvn -pl apps/bidrag-henvendelse test                    # bare tester
mvn antrun:run@ktlint-format -pl apps/bidrag-henvendelse # formater
```

`~/.m2/settings.xml` må ha en `github`-server med et personal access token med
`read:packages`; `token-support` hentes fra GitHub Packages.

## Teste

### Mot mock

`HenvendelseIntegrasjonTest` starter appen med Wiremocked sf-henvendelse-api-proxy og
bidrag-person, og mock-oauth2-server som Azure. Raskeste vei til å se APIet svare:

```bash
mvn -pl apps/bidrag-henvendelse test -Dtest=HenvendelseIntegrasjonTest
```

Andre svar fra kilden legges til som stub i `stubHenvendelser` og `stubPerson`.

### Mot dev

Krever JDK 21 og naisdevice (tjenestene ligger på `*.intern.dev.nav.no`).

Start `BidragHenvendelseLocal` i `src/test/kotlin`; main-metoden setter profilene `local`,
`lokal-nais`, `nais` og `lokal-nais-secrets`. Appen svarer på `http://localhost:8080`.

URL-er og scopes for dev ligger i `src/test/resources/application-local.yaml`.
Azure-credentials settes som miljøvariabler:

```
AZURE_APP_TENANT_ID
AZURE_APP_CLIENT_ID
AZURE_APP_CLIENT_SECRET
```

Hentes fra kjørende pod i dev:

```bash
kubectl -n bidrag exec -it deploy/bidrag-henvendelse -- printenv \
  AZURE_APP_TENANT_ID AZURE_APP_CLIENT_ID AZURE_APP_CLIENT_SECRET
```

Sett dem i run-konfigurasjonen i IntelliJ (Edit Configurations → Environment variables).

### Swagger

Swagger-UI ligger på root path, OpenAPI-spesifikasjonen på `/v3/api-docs`:

| Miljø | Swagger-UI | OpenAPI |
|-------|------------|---------|
| dev   | https://bidrag-henvendelse.intern.dev.nav.no/ | https://bidrag-henvendelse.intern.dev.nav.no/v3/api-docs |
| prod  | https://bidrag-henvendelse.intern.nav.no/     | https://bidrag-henvendelse.intern.nav.no/v3/api-docs |
| lokalt| http://localhost:8080/ | http://localhost:8080/v3/api-docs |

Trykk **Authorize** og lim inn et token for en fiktiv saksbehandler fra Ida. I dev får du et
fra `azure-token-generator`.

## Ikke implementert: auditlogging og tilgangskontroll

Oppslag auditlogges ikke, og ingen ledd i kjeden sjekker tilgang til personen.

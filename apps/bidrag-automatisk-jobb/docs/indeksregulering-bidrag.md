# Indeksregulering bidrag

| | |
|---|---|
| **Applikasjon** | `bidrag-automatisk-jobb` |
| **Pakke** | `no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag` |
| **Kjøremønster** | Årlig, startes manuelt via API-endepunkter (schedulert kjøring er teknisk støttet, men cron er p.t. deaktivert i alle miljø) |
| **Auth** | Azure AD (`@Protected`), kalt av andre Nav-tjenester/team |
| **Status** | Kun norske bidragssaker (NOK) støttes p.t. |

> Denne batchen er en erstatning av den gamle FB020 fra Bisys-batch.
> Funksjonelt skal jobben oppføre seg på samme måte som FB020, men er teknisk fullstendig
> omskrevet fra Bisys-batch til Spring Batch-jobber i `bidrag-automatisk-jobb`.

## 1. Funksjonell beskrivelse

Indeksregulering bidrag har til hensikt å regulere løpende bidragssatser i takt med
konsumprisindeksen, slik at riktig beløp betales fra bidragspliktig (BP) til bidragsmottaker (BM).
Reguleringen beregnes av `bidrag-indeksregulering` fra beregn felles basert på gjeldende
indekseringsregler for barnebidrag.

**⚠️ Viktig endring fra FB020:** Løsningen støtter i dag **kun bidrag som løper i norske kroner
(NOK)**. Danske, islandske og svenske bidrag (DKK/ISK/SEK) — som tidligere ble indeksregulert av
FB020 med egne parametere (`normalAmount`/`adjustAmount`/`adjustPercentage` per land) — håndteres
**ikke** av denne jobben.

Jobben er delt inn i fire uavhengige steg (batcher) som normalt kjøres i rekkefølge for et gitt år:

1. **Opprett** – identifiserer barn med løpende bidrag som skal vurderes for indeksregulering.
2. **Gjennomfør** – beregner ny sats og oppretter vedtaksforslag (kan simuleres).
3. **Fatt vedtak** – fatter de opprettede vedtaksforslagene (kan simuleres).
4. **Rapporter** – genererer rapportfiler tilsvarende FB020s utdatafiler.

Hvert steg kan kjøres flere ganger. Saker som allerede er behandlet for det
angitte året hoppes over.

## 2. Kjøremønster

Jobben kjøres normalt én gang i året, tilsvarende norsk-delen av FB020 (første halvdel av mai,
med virkning 1. juli). Siden kun NOK støttes, er det ikke lenger egne kjøringer for
Danmark/Island/Sverige slik det var i FB020.

Batchene startes manuelt (av team/saksbehandler eller annen tjeneste) mot REST-endepunktene under
`/indeksregulering/bidrag/batch/*` på `bidrag-automatisk-jobb`. Alle endepunkter krever gyldig
Azure AD-token (`@Protected`).

Hvert steg har også en tilhørende Spring `@Scheduled`-jobb med `ShedLock`
(`opprettIndeksreguleringBidrag`, `gjennomforIndeksreguleringBidrag`,
`fattVedtakIndeksreguleringBidrag`, `rapporterIndeksreguleringBidrag`), styrt av miljøvariablene
`INDEKSREGULERING_BIDRAG_OPPRETT_CRON`, `INDEKSREGULERING_BIDRAG_GJENNOMFOR_CRON`,
`INDEKSREGULERING_BIDRAG_FATT_VEDTAK_CRON` og `INDEKSREGULERING_BIDRAG_RAPPORTER_CRON`. Disse er
satt til `"-"` (deaktivert) i alle miljø (dev/prod) p.t., slik at jobben i praksis kun startes
manuelt via API, i tråd med FB020s kjøremønster.

## 3. API-endepunkter

Alle endepunkter eksponeres av `IndeksreguleringBidragBatchController` og er dokumentert i Swagger
under taggen «Indeksregulering bidrag batch».

| Endepunkt | Metode | Parametere | Beskrivelse |
|---|---|---|---|
| `/indeksregulering/bidrag/batch/opprett` | POST | `saksnummer` (valgfri liste), `år` (default: inneværende år) | Starter opprett-steget. Begrenses til gitte saksnumre hvis angitt, ellers alle saker med løpende bidrag. |
| `/indeksregulering/bidrag/batch/gjennomfor` | POST | `år`, `simuler` (default `true`) | Beregner ny sats og oppretter vedtaksforslag for saker med status `UBEHANDLET`. Med `simuler=true` gjøres ingen endringer i Bidrag-vedtak. |
| `/indeksregulering/bidrag/batch/fattvedtak` | POST | `år`, `simuler` (default `true`) | Fatter vedtaksforslagene fra gjennomfør-steget. Med `simuler=true` gjøres ingen faktisk fatting. |
| `/indeksregulering/bidrag/batch/rapporter` | POST | `år` | Genererer rapportfiler for gjennomførte og fattede indeksreguleringer for året. |
| `/indeksregulering/bidrag/slett` | DELETE | `år` (påkrevd) | Sletter alle indeksreguleringsrader for året. Brukes typisk for å nullstille en feilslått kjøring/testkjøring. |
| `/indeksregulering/bidrag/tilbakestill-simulering` | POST | `år` (påkrevd) | Tilbakestiller alle rader med status `SIMULERT` til `UBEHANDLET`, slik at gjennomfør-steget kan kjøres på nytt (uten simulering) uten å slette hele årgangen. |

Hvert steg kjøres som et separat Spring Batch-job med `chunk`-størrelse 100 og feiltolerant
kjøring (enkeltrader som feiler hopper batchen videre forbi, uten å stoppe hele jobben, opptil
`skipLimit = 100` feil per kjøring).

## 4. Datamodell og prosessflyt

Alle mellomresultater lagres i tabellen `indeksregulering` (én rad per barn/stønadstype/år), se
`Indeksregulering`-entiteten. Status-feltet driver flyten mellom de fire batchene:

```
UBEHANDLET ──(gjennomfør)──▶ BEHANDLET ──(fatt vedtak)──▶ FATTET
     │                              │
     └── FEILET                    └── FATTE_VEDTAK_FEILET
```

| Status | Betydning |
|---|---|
| `UBEHANDLET` | Opprettet av opprett-steget, ikke behandlet ennå |
| `BEHANDLET` | Vedtaksforslag opprettet i Bidrag-vedtak |
| `SIMULERT` | Gjennomfør kjørt med `simuler=true` – ingen vedtaksforslag opprettet i Bidrag-vedtak |
| `FEILET` | Gjennomføring feilet (f.eks. mangler skyldner) |
| `FATTET` | Vedtak fattet i Bidrag-vedtak |
| `FATTE_VEDTAK_FEILET` | Fatting av vedtak feilet |
| `TRUKKET`, `SLETTES`, `SLETTET` | Reservert for fremtidig/manuell håndtering |

`behandlingstype` beskriver *hvorfor* en rad endte som den gjorde:

| Behandlingstype | Betydning |
|---|---|
| `FATTET_FORSLAG` | Vedtaksforslag ble opprettet – normalt løp |
| `INGEN` | Ingen løpende stønad funnet – reguleres ikke |
| `MANUELL` | Stønaden løper i utenlandsk valuta – må reguleres manuelt (se avsnitt 5) |
| `FEILET` | Teknisk feil under gjennomføring (f.eks. mangler skyldner) |

### 4.1 Opprett indeksregulering

`OpprettIndeksreguleringBidragBatch` leser barn med løpende bidrag (`BIDRAG`, `BIDRAG18AAR`,
`OPPFOSTRINGSBIDRAG`) fra den lokale `barn`-tabellen (holdt oppdatert via vedtakshendelser fra
`bidrag-vedtak`). For hvert barn/stønadstype henter `OpprettIndeksreguleringBidragService` løpende
stønad fra `bidrag-belopshistorikk` og oppretter en `indeksregulering`-rad (status `UBEHANDLET`)
dersom:

- det finnes en løpende periode (ikke opphørt, beløp > 0), **og**
- perioden løper i NOK (eller uten valutakode, som tolkes som NOK for saker fra gammel løsning), **og**
- stønaden har et `nesteIndeksreguleringsår` satt, **og**
- `nesteIndeksreguleringsår <= år` (dvs. ikke frem i tid)

Saker som allerede har en rad for året hoppes over slik at batchen kan kjøres på nytt.

Dette tilsvarer FB020s regel om at bidrag ikke indeksreguleres dersom indeksår på beløpslinjen er
`NN` eller senere enn kjøreåret, samt at fremtidige/opphørte perioder ikke reguleres.

> **Barn som fyller 18 år:** I motsetning til FB020 er det her `bidragTil`/`bidrag18ÅrTil`-datoene
> på `barn`-tabellen (satt basert på fødselsdato og bidragstype) som avgjør om et bidrag anses som
> løpende. Et bidrag som opphører før kjøring vil derfor ikke plukkes opp av opprett-steget.

### 4.2 Gjennomfør indeksregulering

`GjennomførIndeksreguleringBidragBatch` behandler alle rader med status `UBEHANDLET` for året.
For hver rad henter `GjennomførIndeksreguleringBidragService` på nytt løpende stønad, og:

- Hopper over (behandlingstype `INGEN`) dersom stønaden ikke lenger er løpende.
- Hopper over (behandlingstype `MANUELL`) dersom stønaden løper i annen valuta enn NOK – disse må
  reguleres manuelt, tilsvarende «Indeksregulering: Bidrag ulik normalbidrag»-oppgavene i FB020,
  men uten at det per i dag opprettes noen tilsvarende oppgave automatisk (se videre arbeid i avsnitt 7).
- Feiler (status `FEILET`) dersom saken mangler skyldner (BP).
- Ellers: kaller `bidrag-indeksregulering` for å beregne ny sats, og oppretter et vedtaksforslag i
  `bidrag-vedtak` (type `INDEKSREGULERING`, kilde `AUTOMATISK`) via
  `opprettEllerOppdaterVedtaksforslag`. Raden oppdateres med `vedtak`-id, nytt `beløp` og status
  `BEHANDLET` (eller `SIMULERT` ved simulering).

Med `simuler=true` beregnes satsen, men det opprettes **ikke** noe vedtaksforslag
i `bidrag-vedtak` – nyttig for å kvalitetssikre en kjøring før den faktisk berører saker. Simulerte
rader kan tilbakestilles til `UBEHANDLET` via `/tilbakestill-simulering`-endepunktet uten å måtte
slette og opprette hele årgangen på nytt.

### 4.3 Fatt vedtak

`FattVedtakIndeksreguleringBidragBatch` fatter vedtaksforslagene fra forrige steg (status
`BEHANDLET`, behandlingstype `FATTET_FORSLAG`) ved å kalle `fatteVedtaksforslag` i `bidrag-vedtak`.
Ved suksess settes status til `FATTET` med `fattetTidspunkt`. Ved feil settes status til
`FATTE_VEDTAK_FEILET` og feilen kastes videre (stopper ikke resten av chunken pga.
feiltolerant kjøring). Også dette steget støtter `simuler=true` for å teste uten å fatte vedtak.

### 4.4 Rapporter

`RapporterIndeksreguleringBidragBatch` bygger rapportfiler for året basert på rader med status
`FATTET`, tilsvarende de fem utdatafilene i FB020:

| FB020-fil | Ny rapport | Status                                                                |
|---|---|-----------------------------------------------------------------------|
| `BIDRAG.NOR.IREG.BRS.OPPDAT` (Bidragsreskontro) | Bidragsreskontro-rapport | Deaktivert                                                            |
| `BIDRAG.NOR.IREG.FFU.BREV` (BP i utlandet, brev) | BP-utland-brev-rapport | Deaktivert                                                            |
| `BIDRAG.NOR.IREG.FFU.DISKR` (BP i utlandet, diskresjon) | BP-utland-diskresjon-rapport | Deaktivert                                                            |
| `BIDRAG.NOR.IREG.FFU.ADDR` (BP i utlandet, mangler adresse) | BP-utland-mangler-adresse-rapport | Deaktivert                                                            |
| `BIDRAG.NOR.IREG.ELIN.BELOP` (Elin) | Elin-rapport | **Aktiv** – lastes opp til GCP-bucket og videre til Elin via filsluse |

Klassifiseringen (Bidragsreskontro / BP-utland-brev / -diskresjon / -mangler adresse / Elin) gjøres
i `IndeksreguleringsfilService` basert på skyldners (BPs) landkode, diskresjonskode og
adresseinformasjon fra `bidrag-person`, på samme måte funksjonelt som FB020 gjorde det:

- Skyldner med landkode `NO` → Bidragsreskontro.
- Skyldner med annen landkode → BP-utland-brev, og av disse videre til BP-utland-diskresjon
  (diskresjonskode satt) og/eller BP-utland-mangler-adresse (ingen adresselinjer registrert).
- Alle rader inkluderes uansett i Elin-rapporten.

De fire første rapportene er p.t. **kodemessig implementert, men deaktivert** i
`RapporterIndeksreguleringBidragTasklet` fordi det ikke er avklart om det fortsatt er tjenstlig
behov for dem etter at kun norske saker støttes (se avsnitt 7 videre arbeid). Kun
Elin-rapporten er aktiv, siden Elin uansett trenger varsel om indeksregulering.

## 5. Feilhåndtering / manuell oppfølging etter kjøring

Tilsvarende FB020s «TO DO etter kjøring», bør følgende sjekkes etter en kjøring:

1. **Saker som ikke ble opprettet/regulert:** Spør på `indeksregulering`-tabellen for året og
   sammenlign mot forventet saksmengde. En sak/barn regulereres ikke dersom:
   - stønaden ikke er løpende (opphørt, ingen beløp, eller opphørsdato passert),
   - `nesteIndeksreguleringsår` er `null` eller senere enn kjøreåret,
   - stønaden løper i annen valuta enn NOK (`behandlingstype = MANUELL`),
   - barnet ikke lenger regnes som løpende i den lokale `barn`-tabellen.
2. **Saker med `behandlingstype = MANUELL` (utenlandsk valuta):** Disse må følges opp manuelt av
   NAV Utland, tilsvarende FB020s håndtering av DKK/ISK/SEK. Det opprettes **ikke** automatisk
   noen Bisys-oppgave for disse i dag (i motsetning til FB020s
   «Indeksregulering: Bidrag ulik normalbidrag»-oppgaver) – dette må eventuelt løses manuelt eller
   bygges som en fremtidig utvidelse.
3. **Rader med `status = FEILET` eller `FATTE_VEDTAK_FEILET`:** Analyseres og rettes manuelt,
   eventuelt ved å rette underliggende data og kjøre gjennomfør-/fattvedtak-steget på nytt for de
   aktuelle sakene.
4. **Rapportfiler:** Elin-filen (`elin/elin-<dato>.txt`) lastes automatisk opp til GCP-bucket og
   videre til Elins filsluse. De øvrige fire filene genereres ikke p.t. (se avsnitt 4.4) – dersom
   det viser seg å være behov for dem (f.eks. til Bidragsreskontro eller NAV Utland), må dette
   aktiveres igjen og eventuelt integreres mot riktig mottaker.

## 6. Støtte for danske, islandske og svenske bidrag

FB020 støttet indeksregulering av DKK-, ISK- og SEK-bidrag med egne parametersett
(`normalAmount`/`adjustAmount`/`adjustPercentage` per land) bestilt av NFP Utland/Bidrag utland.
**Denne funksjonaliteten er ikke migrert** til `bidrag-automatisk-jobb`. Bidrag som løper i
utenlandsk valuta blir liggende som `behandlingstype = MANUELL` i opprett-/gjennomfør-steget og må
håndteres manuelt inntil videre. Dersom det er behov for automatisk støtte for disse valutaene må
dette planlegges og bygges som en egen utvidelse.

## 7. Videre arbeid

- Automatisk opprettelse av oppgave for saker med `behandlingstype = MANUELL` (utenlandsk
  valuta) er ikke implementert – må vurderes om/hvordan dette skal løses (Bisys-oppgave, e-post,
  eller annet varsel til NAV Utland).
- Støtte for indeksregulering av DKK/ISK/SEK-bidrag er ikke migrert fra FB020.
- De fire deaktiverte rapportfilene (Bidragsreskontro, BP-utland-brev/-diskresjon/-mangler
  adresse) må avklares tjenstlig behov før de eventuelt aktiveres.

## Relatert kode

- Controller: `IndeksreguleringBidragBatchController`
- Batch-steg: `batch/indeksregulering/bidrag/{opprett,gjennomfor,fattvedtak,rapporter}`
- Domenetjenester: `service/batch/indeksregulering/*`
- Entitet/repository: `persistence/entity/Indeksregulering.kt`,
  `persistence/repository/IndeksreguleringRepository.kt`

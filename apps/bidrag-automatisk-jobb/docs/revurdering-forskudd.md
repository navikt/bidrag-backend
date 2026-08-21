# Revurdering forskudd

| | Beskrivelse                                                                                                                            |
|---|----------------------------------------------------------------------------------------------------------------------------------------|
| **Applikasjon** | `bidrag-automatisk-jobb`                                                                                                               |
| **Pakke** | `no.nav.bidrag.automatiskjobb.batch.revurderforskudd`                                                                                  |
| **Kjøremønster** | Årlig, startes manuelt via API-endepunkter (schedulert kjøring er teknisk støttet, men cron er p.t. deaktivert i alle miljø) |
| **Auth** | Azure AD (`@Protected`)                                                                            |
| **Erstatter** | Bisys-batch **FB110**                                                                                                                  |

> Denne batchen er en erstatning av den gamle FB110 fra Bisys-batch. Funksjonelt skal jobben
> oppføre seg på samme måte som FB110 (plukke ut og revurdere løpende forskudd basert på endring i
> BMs inntekt), men er teknisk fullstendig omskrevet fra Bisys-batch/BBM-jobber til Spring
> Batch-jobber i `bidrag-automatisk-jobb`. I motsetning til FB110 hentes inntektsgrunnlaget direkte
> fra a-inntekt/sigrun via `bidrag-grunnlag` og beregnes med `bidrag-beregn-forskudd` i samme
> prosess.

## 1. Funksjonell beskrivelse

Revurdering forskudd har til hensikt å fange opp saker der bidragsmottakers (BMs) inntekt har
endret seg såpass mye siden forrige manuelle forskuddsvedtak at forskuddssatsen bør settes ned.
Jobben plukker ut alle saker med løpende forskudd som ikke har hatt et manuelt vedtak nylig,
beregner ny sats basert på oppdatert inntekt, og fatter automatisk vedtak dersom beregningen tilsier
at forskuddet skal reduseres. Batchen kan **kun redusere** forskudd – den øker aldri satsen
automatisk.

Jobben er delt inn i fire uavhengige steg (batcher) som normalt kjøres i rekkefølge:

1. **Opprett** – identifiserer saker med løpende forskudd som er aktuelle for revurdering denne
   måneden.
2. **Evaluer** – henter oppdatert inntekt, beregner nytt forskudd og oppretter vedtaksforslag (eller
   avviser revurdering).
3. **Fatt vedtak** – fatter de opprettede vedtaksforslagene og bestiller forsendelse av
   vedtaksbrev/informasjonsbrev.
4. **Revurderingslenke** – oppretter en forhåndsutfylt revurderingsbehandling i
   `bidrag-behandling` for saker der det bør vurderes tilbakekreving av forskudd som allerede er
   utbetalt.
5. **Opprett/distribuer forsendelse** – genererer og sender ut brev (delt med aldersjustering, se
   avsnitt 4.6).

I tillegg finnes to frittstående, manuelt trigget operasjoner:

- **Vurder tilbakekreving basert på reskontro** – oppdaterer `vurdereTilbakekreving`-flagget for
  allerede fattede vedtaksforslag basert på ny reskontrodata.
- **Evaluer for én sak** – kjører evaluerings-steget for en enkelt sak og returnerer
  resultatet direkte (primært for feilsøking eller enkeltsaksbehandling).

## 2. Kjøremønster

Jobben kjøres normalt én gang i året, tidspunktet besluttes av fagansvarlig. Batchene kan enten
startes manuelt (av team/saksbehandler) mot REST-endepunktene under `/revurderforskudd/batch/*` på
`bidrag-automatisk-jobb`, eller trigges automatisk via `@Scheduled`-jobber med `ShedLock`. Alle
API-endepunkter krever gyldig Azure AD-token (`@Protected`).

Alle fire batch-stegene har egne schedulere som kan aktiveres via cron-miljøvariabler:

| Batch              | Scheduler                                | Miljøvariabel                             |
|--------------------|-------------------------------------------|--------------------------------------------|
| Opprett            | `OpprettRevurderForskuddScheduler`        | `REVURDER_FORSKUDD_OPPRETT_CRON`            |
| Evaluer            | `EvaluerRevurderForskuddScheduler`        | `REVURDER_FORSKUDD_EVALUER_CRON`            |
| Fatt vedtak        | `FatteVedtakRevurderForskuddScheduler`    | `REVURDER_FORSKUDD_FATTE_VEDTAK_CRON`       |
| Revurderingslenke  | `RevurderingslenkeRevurderForskuddScheduler` | `REVURDER_FORSKUDD_REVURDERINGSLENKE_CRON` |
| Opprett forsendelse    | `OpprettForsendelseBatchScheduler`     | `REVURDER_FORSKUDD_OPPRETT_FORSENDELSE_CRON` |
| Distribuer forsendelse | `DistribuerForsendelseBatchScheduler`  | `REVURDER_FORSKUDD_DISTRUBUER_FORSENDELSE_CRON` |

Disse miljøvariablene brukes også i den månedlige Slack-varslingen om kjøreplan
(`RevurderForskuddBatchVarslingConfiguration`), slik at en satt cron vises i varselet i tillegg til
å faktisk trigge kjøringen. Alle miljø har disse satt til `"-"` p.t. (deaktivert), slik at jobben i
praksis startes manuelt via API.

Opprett og distribuer forsendelse er delt med aldersjustering (se avsnitt 4.6 i
[docs/aldersjustering-bidrag.md](aldersjustering-bidrag.md))

«Vurder tilbakekreving basert på reskontro» og «evaluer for én sak» har ingen scheduler, og må
fortsatt trigges manuelt via API ved behov.

## 3. API-endepunkter

Alle endepunkter eksponeres av `RevurderForskuddBatchController`.

| Endepunkt | Metode | Parametere | Beskrivelse |
|---|---|---|---|
| `/revurderforskudd/batch/opprett` | POST | `månederTilbakeForManueltVedtak` (default `12`) | Oppretter en rad i `revurdering_forskudd` (status `UBEHANDLET`) for hver sak med løpende forskudd som ikke er unntatt (se avsnitt 4.1). |
| `/revurderforskudd/batch/opprett/slett` | DELETE | `forMåned` (påkrevd, `YYYY-MM`) | Sletter alle revurderinger opprettet for angitt måned. |
| `/revurderforskudd/batch/evaluer` | POST | `simuler` (default `true`), `beregnFraMåned` (valgfri, default én måned frem), `forMåned` (valgfri, default inneværende måned), `antallMånederForBeregning` (default `3`) | Beregner revurdering og oppretter vedtaksforslag (eller avvist-forslag) for rader med status `UBEHANDLET`. |
| `/revurderforskudd/batch/evaluer/{saksnummer}` | POST | `simuler`, `beregnFraMåned`, `forMåned`, `antallMånederForBeregning` | Evaluerer én enkelt sak synkront og returnerer oppdatert `RevurderingForskudd`. |
| `/revurderforskudd/batch/evaluer/resetSimulering` | POST | – | Setter status tilbake til `UBEHANDLET` for alle rader med status `SIMULERT`. |
| `/revurderforskudd/batch/evaluer/resetFeilede` | POST | – | Setter status tilbake til `UBEHANDLET` for alle rader med status `FEILET`. |
| `/revurderforskudd/batch/fattevedtak` | POST | `simuler` (default `true`) | Fatter vedtak for vedtaksforslag med status `BEHANDLET`, og bestiller forsendelse av vedtaksbrev. |
| `/revurderforskudd/batch/revurderingslenke` | POST | `søktFraDato` (påkrevd), `forMåned` (valgfri, default inneværende måned) | Oppretter revurderingsbehandling i `bidrag-behandling` for fattede rader som skal vurderes for tilbakekreving. |
| `/revurderforskudd/batch/reskontroVurderTilbakekreving` | POST | – | Starter asynkron oppdatering av `vurdereTilbakekreving`-flagget basert på reskontrodata, for alle rader med `behandlingstype = FATTET_FORSLAG`. |
| `/batch/forsendelse/opprett` | POST | `prosesserFeilet` (default `false`), `bestillingIder` (valgfri) | Genererer forsendelse (brev) for bestillinger uten opprettet forsendelse. Delt/generisk batch, se avsnitt 4.6. |
| `/batch/forsendelse/distribuer` | POST | `bestillingIder` (valgfri) | Arkiverer og distribuerer opprettede forsendelser. Delt/generisk batch, se avsnitt 4.6. |

## 4. Datamodell og prosessflyt

Alle mellomresultater lagres i tabellen `revurdering_forskudd` (én rad per sak/måned), se
`RevurderingForskudd`-entiteten. Status-feltet driver flyten mellom stegene:

```
UBEHANDLET ──(evaluer)──▶ BEHANDLET ──(fatt vedtak)──▶ FATTET ──(revurderingslenke)──▶ FATTET (oppgave satt)
     │                          │
     └── FEILET                 └── FATTE_VEDTAK_FEILET
```

| Status | Betydning                                                                                 |
|---|-------------------------------------------------------------------------------------------|
| `UBEHANDLET` | Opprettet av opprett-steget, ikke evaluert ennå                                           |
| `BEHANDLET` | Evaluert med suksess, vedtaksforslag opprettet i `bidrag-vedtak`                          |
| `SIMULERT` | Evaluert med `simuler=true` – vedtaksforslag ikke nødvendigvis opprettet i `bidrag-vedtak` |
| `FEILET` | Evalueringen feilet – begrunnelse lagres i `begrunnelse`                                  |
| `FATTE_VEDTAK_FEILET` | Et forsøk på å fatte vedtak feilet                                                        |
| `FATTET` | Vedtak fattet i `bidrag-vedtak`                                                           |

`behandlingstype` beskriver *hvorfor* en rad endte som den gjorde:

| Behandlingstype | Betydning |
|---|---|
| `FATTET_FORSLAG` | Revurdering ble gjennomført – forskuddet skal settes ned |
| `INGEN` | Ingen endring – forskuddet skal ikke settes ned, eller saken kvalifiserer ikke (se begrunnelser under) |
| `FEILET` | Teknisk feil under evaluering (manglende grunnlag, feil i beregning e.l.) |
| `MANUELL` | Ikke i bruk av revurdering forskudd |

`begrunnelse` inneholder maskinlesbare koder som forklarer utfallet, bl.a.:
`INGEN_MANUELLE_VEDTAK`, `FORSKUDD_IKKE_LØPENDE`, `FANT_INGEN_GRUNNLAG_FOR_BARN`,
`FANT_INGEN_GRUNNLAG_FOR_BIDRAGSMOTTAKER`, `FEIL_VED_HENTING_AV_INNTEKTSGRUNNLAG`,
`FEIL_VED_BEREGNING`, `UGYLDIG_INPUT_VED_BEREGNING`, `UKJENT_FEIL_VED_BEREGNING`,
`SKAL_IKKE_SETTES_NED`, `FEIL_VED_SJEKK_AV_RESKONTRO`, `FEIL_VED_OPPRETTING_AV_VEDTAKSFORSLAG`.

### 4.1 Opprett revurdering forskudd

`OpprettRevurderForskuddBatch` leser alle barn med løpende forskudd (`forskuddFra`/`forskuddTil`)
fra `barn`-tabellen, gruppert per saksnummer (én sak kan ha flere barn/forskudd). For hver sak
opprettes én rad i `revurdering_forskudd` med status `UBEHANDLET` for inneværende måned – med
mindre saken skal unntas:

- Saken har allerede en revurdering for inneværende måned.
- Saken har en åpen forskuddsbehandling i `bidrag-behandling` (unngår kollisjon med pågående
  saksbehandling).
- Siste manuelle vedtak i saken er nyere enn cutoff-tidspunktet (`månederTilbakeForManueltVedtak`,
  default 12 måneder).
- Alle barn i saken er over 18 år eller fyller 18 år inneværende/neste måned – disse barna
  filtreres bort før saken vurderes.

### 4.2 Evaluer revurdering forskudd

`EvaluerRevurderForskuddBatch` behandler rader med status `UBEHANDLET` via
`EvaluerRevurderForskuddService.evaluerRevurderForskudd`, som for hvert barn i saken:

1. Henter siste manuelle forskuddsvedtak. Finnes ingen: `behandlingstype = INGEN`,
   begrunnelse `INGEN_MANUELLE_VEDTAK`.
2. Sjekker at forskuddet fortsatt er løpende. Er det ikke det: `behandlingstype = INGEN`,
   begrunnelse `FORSKUDD_IKKE_LØPENDE`.
3. Henter grunnlag for barn og BM fra forrige vedtak. Mangler grunnlag: `behandlingstype = FEILET`.
4. Henter oppdatert inntekt (a-inntekt/sigrunn) via `bidrag-grunnlag`/`InntektApi`, og beregner
   nytt forskudd via `BeregnForskuddApi` med høyeste av årsinntekt og siste
   `antallMånederForBeregning` måneders inntekt ganget med 12.
5. Sammenligner nytt beregnet forskudd med løpende beløp: dersom forskuddet **ikke** skal settes
   ned, avsluttes evalueringen med `behandlingstype = INGEN`, begrunnelse `SKAL_IKKE_SETTES_NED`.
6. Sjekker reskontro for forskuddsutbetaling siste 3 måneder – finnes utbetaling, settes
   `vurdereTilbakekreving = true` (brukes av revurderingslenke-steget, se 4.4).
7. Oppretter vedtaksforslag i `bidrag-vedtak`, setter
   `behandlingstype = FATTET_FORSLAG`.

Med `simuler=true` opprettes ikke noe reelt vedtaksforslag, men resultatet lagres
med status `SIMULERT` – nyttig for å hente ut oversikt over antall saker og fordeling av
behandlingstype/begrunnelse før en reell kjøring.

Enkeltsaks-endepunktet (`/evaluer/{saksnummer}`) kjører nøyaktig samme logikk synkront for én sak,
uavhengig av status, og returnerer resultatet direkte i responsen.

### 4.3 Fatt vedtak

`FatteVedtakRevurderForskuddBatch` fatter vedtak for rader med status `BEHANDLET` og satt
`vedtak`-id, ved å kalle `fatteVedtaksforslag` i `bidrag-vedtak`. 
Status settes til `FATTET` ved suksess og `fattetTidspunkt`settes, eller `FATTE_VEDTAK_FEILET` ved feil.

Ved vellykket fatting bestilles automatisk forsendelse (vedtaksbrev, `Forsendelsestype.
REVURDERING_FORSKUDD`, brevkode `BI01A08` via
`ForsendelseBestillingService`.

### 4.4 Revurderingslenke (tilbakekreving)

`RevurderingslenkeRevurderForskuddBatch` behandler fattede rader (status `FATTET`) for angitt
måned der `vurdereTilbakekreving = true` og `oppgave` ikke allerede er satt. For hver
rad opprettes en forhåndsutfylt revurderingsbehandling (`vedtakstype = REVURDERING`,
`behandlingstema = FORSKUDD`) i `bidrag-behandling` via `BidragBehandlingConsumer`, med barna som
roller og `søktFomDato = søktFraDato`. Id-en til den opprettede behandlingen lagres i
`oppgave`-kolonnen.

Dette gjør at saksbehandler kan åpne behandlingen direkte i saksbehandlingsløsningen og vurdere om
allerede utbetalt forskudd skal kreves tilbake, i stedet for å måtte opprette saken manuelt.

`vurdereTilbakekreving`-flagget kan også oppdateres i etterkant (uavhengig av evaluer-steget) via
`/revurderforskudd/batch/reskontroVurderTilbakekreving`, som ser på reskontro for alle rader med
`behandlingstype = FATTET_FORSLAG` – nyttig dersom nye reskontrotransaksjoner kommer inn etter at
evaluer-steget er kjørt.

### 4.5 Reset-operasjoner

To hjelpeendepunkter finnes for å rekjøre evaluer-steget uten å måtte opprette sakene på nytt:

- `/evaluer/resetSimulering` – setter status fra `SIMULERT` til `UBEHANDLET`.
- `/evaluer/resetFeilede` – setter status fra `FEILET` til `UBEHANDLET` (etter at årsaken til
  feilene er rettet, f.eks. manglende grunnlag).

### 4.6 Opprett/distribuer forsendelse

Forsendelse-batchene (`/batch/forsendelse/opprett` og `/batch/forsendelse/distribuer`) er **ikke**
revurder forskudd-spesifikke, men generiske jobber som også brukes av andre batcher i `bidrag-automatisk-jobb` som også brukes av aldersjustering bidrag. Se avsnitt 4.6 i
[docs/aldersjustering-bidrag.md](aldersjustering-bidrag.md) for full beskrivelse av hvordan disse
fungerer.

## 5. Feilhåndtering / manuell oppfølging etter kjøring

Følgende sjekkes etter en kjøring:

1. **Rader med `status = FEILET`:** Analyseres (begrunnelse i `begrunnelse`) og rekjøres fra
   evaluer-steget etter `/evaluer/resetFeilede`.
2. **Rader med `status = FATTE_VEDTAK_FEILET`:** Undersøk feilmelding i loggene og vurder om
   evaluer-steget må kjøres på nytt for saken.
3. **Rader med `vurdereTilbakekreving = true`:** Følg opp at revurderingslenke er opprettet
   (`oppgave`-kolonnen satt), og at saksbehandler faktisk behandler tilbakekrevingsvurderingen i
   `bidrag-behandling`.
4. **Forsendelse/brev:** Gjør stikkprøver på brevinnhold før distribusjon bestilles, tilsvarende
   aldersjustering (se avsnitt 4.6 i aldersjustering-dokumentasjonen).
5. **Simulering før reell kjøring:** Kjør evaluer-steget med `simuler=true` først for å få oversikt
   over antall saker og fordeling av behandlingstype/begrunnelse, før reell kjøring
   (`simuler=false`).

## 6. Videre arbeid

- Schedulerne er p.t. deaktivert (cron `"-"`) i alle miljø – automatisk kjøring må aktiveres
  eksplisitt før den tas i bruk.

## Relatert kode

- Controller: `RevurderForskuddBatchController`
- Batch-steg: `batch/revurderforskudd/{opprett,evaluer,fattvedtak,revurderingslenke}`
- Schedulere: `OpprettRevurderForskuddScheduler`, `EvaluerRevurderForskuddScheduler`,
  `FatteVedtakRevurderForskuddScheduler`, `RevurderingslenkeRevurderForskuddScheduler`,
  `OpprettForsendelseBatchScheduler`, `DistribuerForsendelseBatchScheduler`
- Domenetjenester: `service/RevurderForskuddService.kt`,
  `service/batch/revurderforskudd/{OpprettRevurderForskuddService,EvaluerRevurderForskuddService,FattVedtakRevurderForskuddService,RevurderingslenkeRevurderingForskuddService}.kt`
- Entitet/repository: `persistence/entity/RevurderingForskudd.kt`,
  `persistence/repository/RevurderForskuddRepository.kt`
- Delt forsendelse-infrastruktur: `batch/utils/forsendelse/*`, `service/ForsendelseBestillingService.kt`

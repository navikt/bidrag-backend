# Aldersjustering bidrag

| | Beskrivelse                                                                                                                                                                       |
|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Applikasjon** | `bidrag-automatisk-jobb`                                                                                                                                                          |
| **Pakke** | `no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag`                                                                                                                       |
| **Kjøremønster** | Årlig, startes manuelt via API-endepunkter (schedulert kjøring er teknisk støttet, men cron er p.t. deaktivert i alle miljø) |
| **Auth** | Azure AD (`@Protected`)                                                                                                                        |
| **Erstatter** | Bisys-batch **FB260**                                                                                                                                                             |

> Denne batchen er en erstatning av den gamle FB260 fra Bisys-batch.
> Funksjonelt skal jobben oppføre seg på samme måte som FB260 (aldersjustering av bidrag ved 6, 11
> og 15 år, basert på sjablongverdier), men er teknisk fullstendig omskrevet fra Bisys-batch til
> Spring Batch-jobber i `bidrag-automatisk-jobb`.

## 1. Funksjonell beskrivelse

Aldersjustering bidrag har til hensikt å justere løpende barnebidrag når barnet fyller 6, 11 eller
15 år, siden underholdskostnaden endrer seg med barnets alder. Justeringen beregnes automatisk ut
fra gjeldende sjablongverdier og forrige vedtaks grunnlag (samværsklasse, inntekter mv.), og
resulterer enten i et nytt automatisk vedtak, ingen endring, eller en sak som må behandles manuelt.

`bidrag-automatisk-jobb` holder oversikt over alle barn som er part i en bidragssak i den lokale
`barn`-tabellen (holdt oppdatert via vedtakshendelser), og aldersjusterer dem det året de fyller 6,
11 eller 15 år.

Jobben er delt inn i flere uavhengige steg (batcher) som kjøres i rekkefølge for et gitt år:

1. **Opprett** – identifiserer barn med løpende bidrag som skal aldersjusteres for året.
2. **Beregn** – beregner ny sats og oppretter vedtaksforslag (eller avviser aldersjustering), kan simuleres.
3. **Fatt vedtak** – fatter de opprettede vedtaksforslagene og bestiller forsendelse av vedtaksbrev, kan simuleres.
4. **Lagre B4-informasjon** – henter og lagrer avregningsbeløp (B4/D4) fra reskontro for fattede aldersjusteringer.
5. **Opprett/slett oppgave** – oppretter Bisys-oppgave for saker som må behandles manuelt, og rydder disse igjen etter behandling.
6. **Opprett/distribuer forsendelse** – genererer og sender ut vedtaksbrev (se avsnitt 4.6).

Hvert steg kan i all hovedsak kjøres flere ganger hvor rader som allerede er
behandlet for kombinasjonen barn/aldersgruppe/status hoppes over eller kan eksplisitt inkluderes
på nytt via statusparametre.

## 2. Kjøremønster

Jobben kjøres normalt én gang i året med virkning fra 1. juli. Batchene kan enten startes manuelt
(av team/saksbehandler) mot REST-endepunktene under `/aldersjustering/batch/*` på
`bidrag-automatisk-jobb`, eller trigges automatisk via `@Scheduled`-jobber med `ShedLock`. Alle
API-endepunkter krever gyldig Azure AD-token (`@Protected`).

Fire av batchene har egne schedulere som kan aktiveres via cron-miljøvariabler:

| Batch                  | Scheduler                                      | Miljøvariabel                                        |
|------------------------|------------------------------------------------|------------------------------------------------------|
| Opprett                | `OpprettAldersjusteringerBidragScheduler`      | `ALDERSJUSTERING_BIDRAG_OPPRETT_CRON`                |
| Beregn                 | `BeregnAldersjusteringerBidragScheduler`       | `ALDERSJUSTERING_BIDRAG_BEREGN_CRON`                 |
| Fatt vedtak            | `FattVedtakOmAldersjusteringerBidragScheduler` | `ALDERSJUSTERING_BIDRAG_FATT_VEDTAK_CRON`            |
| Lagre B4-informasjon   | `LagreB4InformasjonBidragScheduler`            | `ALDERSJUSTERING_BIDRAG_LAGRE_B4_CRON`               |
| Opprett oppgave        | `OppgaveAldersjusteringBidragScheduler`        | `ALDERSJUSTERING_BIDRAG_OPPRETT_OPPGAVE_CRON`        |
| Opprett forsendelse    | `OpprettForsendelseBatchScheduler`             | `ALDERSJUSTERING_BIDRAG_OPPRETT_FORSENDELSE_CRON`    |
| Distribuer forsendelse | `DistribuerForsendelseBatchScheduler`          | `ALDERSJUSTERING_BIDRAG_DISTRIBUER_FORSENDELSE_CRON` |

Disse miljøvariablene brukes også i den månedlige Slack-varslingen om kjøreplan
(`AldersjusteringBidragBatchVarslingConfiguration`), slik at en satt cron vises i varselet i tillegg
til å faktisk trigge kjøringen. Alle miljø har disse satt til `"-"` p.t. (deaktivert), slik at
jobben i praksis startes manuelt via API, i tråd med FB260s kjøremønster. Når schedulert kjøring
aktiveres kjører beregn- og fatt vedtak-stegene med `simuler=false`.

Opprett og distribuer forsendelse er delt med revurdering av forskudd (se avsnitt 4.6), og har
derfor to uavhengige `@Scheduled`-metoder – én per cron-variabel – slik at de to batchtypene kan
aktiveres uavhengig av hverandre uten å blokkere hverandre.

Slett oppgave har ingen egen scheduler, siden steget avhenger av en spesifikk `batchId` fra en
tidligere opprett oppgave-kjøring.

## 3. API-endepunkter

Alle endepunkter eksponeres av `AldersjusteringBidragBatchController`.

| Endepunkt | Metode | Parametere | Beskrivelse |
|---|---|---|---|
| `/aldersjustering/batch/bidrag/opprett` | POST | `aar` (påkrevd), `aldersjusteringsdato` (default `01.07.<aar>`) | Klargjør grunnlaget: oppretter en rad i `aldersjustering` (status `UBEHANDLET`) for hvert barn som fyller 6, 11 eller 15 år og har løpende bidrag på `aldersjusteringsdato`. |
| `/aldersjustering/batch/bidrag/beregn` | POST | `statuser` (default `UBEHANDLET,FEILET,SIMULERT`), `simuler` (default `true`), `barn` (valgfri liste) | Beregner aldersjustering og oppretter vedtaksforslag (eller avvist-forslag) for radene som matcher `statuser`. |
| `/aldersjustering/batch/bidrag/fattVedtak` | POST | `barn` (valgfri), `simuler` (default `true`), `behandlingstyper` (default `MANUELL,FATTET_FORSLAG,INGEN`), `kunRedusertBidrag` (default `false`) | Fatter vedtak for vedtaksforslag med status `BEHANDLET`, og bestiller forsendelse av vedtaksbrev. |
| `/aldersjustering/batch/bidrag/lagreB4Informasjon` | POST | `fattetÅr` (påkrevd), `barn` (valgfri) | Henter B4/D4-avregningsbeløp fra reskontro for fattede aldersjusteringer og lagrer i `b4_beløp`. |
| `/aldersjustering/batch/bidrag/oppgave` | POST | `barn` (valgfri) | Oppretter Bisys-oppgave for saker med `behandlingstype = MANUELL` og `status = BEHANDLET` som ikke allerede har oppgave. |
| `/aldersjustering/batch/bidrag/oppgave/slett` | POST | `barn` (valgfri), `batchId` (påkrevd) | Sletter oppgaver opprettet for en gitt `batchId` (typisk etter at sakene er ferdigbehandlet). |
| `/aldersjustering/batch/slettvedtaksforslag` | POST | `inkluderBehandlet` (default `false`), `barn` (valgfri) | Sletter vedtaksforslag i `bidrag-vedtak` for rader med status `SLETTES` (og `BEHANDLET` hvis `inkluderBehandlet=true`) som ikke er fattet, og setter status til `SLETTET`. |
| `/aldersjustering/batch/slettvedtaksforslag/alle` | POST | – | Sletter **alle** eksisterende vedtaksforslag i `bidrag-vedtak` (brukes ved større opprydding/feilsituasjoner). |
| `/batch/forsendelse/opprett` | POST | `prosesserFeilet` (default `false`), `bestillingIder` (valgfri) | Genererer forsendelse (brev) for bestillinger uten opprettet forsendelse. Delt/generisk batch, se avsnitt 4.6. |
| `/batch/forsendelse/distribuer` | POST | `bestillingIder` (valgfri) | Arkiverer og distribuerer opprettede forsendelser. Delt/generisk batch, se avsnitt 4.6. |

## 4. Datamodell og prosessflyt

Alle mellomresultater lagres i tabellen `aldersjustering` (én rad per barn/aldersgruppe), se
`Aldersjustering`-entiteten. Status-feltet driver flyten mellom stegene:

```
UBEHANDLET ──(beregn)──▶ BEHANDLET ──(fatt vedtak)──▶ FATTET ──(lagre B4)──▶ FATTET (b4Beløp satt)
     │                          │                                    │
     └── FEILET                └── SLETTET (slettvedtaksforslag)     └── FATTE_VEDTAK_FEILET
```

| Status | Betydning |
|---|---|
| `UBEHANDLET` | Opprettet av opprett-steget, ikke beregnet ennå |
| `BEHANDLET` | Beregnet med suksess, vedtaksforslag opprettet i `bidrag-vedtak` |
| `SIMULERT` | Beregnet med `simuler=true` – vedtaksforslag ikke opprettet i `bidrag-vedtak` |
| `FEILET` | Beregningen feilet – begrunnelse lagres i `begrunnelse` |
| `FATTE_VEDTAK_FEILET` | Et tidligere forsøk på å fatte vedtak feilet (typisk fordi saksbehandler fattet vedtak manuelt i mellomtiden) |
| `FATTET` | Vedtak fattet i `bidrag-vedtak` |
| `SLETTES` / `SLETTET` | Markert for sletting / vedtaksforslaget er slettet igjen i `bidrag-vedtak` |

`behandlingstype` beskriver *hvorfor* en rad endte som den gjorde:

| Behandlingstype | Betydning |
|---|---|
| `FATTET_FORSLAG` | Aldersjustering ble gjennomført – vedtaksforslag kan åpnes via sakshistorikken |
| `INGEN` | Vedtaksforslag opprettet med beslutningstype `AVVIST` – ingen faktisk endring i bidraget |
| `MANUELL` | Aldersjusteringen krever manuell behandling (typisk avvik i grunnlag/beregning) |
| `FEILET` | Teknisk feil under beregning (f.eks. mangler skyldner) |

### 4.1 Opprett aldersjustering

`OpprettAldersjusteringerBidragBatch` leser barn fra `barn`-tabellen for valgt år med
følgende utvalg det året barnet fyller 6, 11 eller 15 år:

- `:år - EXTRACT(YEAR FROM fødselsdato) IN (6, 11, 15)`
- `bidrag_fra <= aldersjusteringsdato`
- `bidrag_til IS NULL OR bidrag_til > aldersjusteringsdato`

For hvert treff opprettes en rad i `aldersjustering` med status `UBEHANDLET` dersom den ikke
allerede finnes for samme barn og aldersgruppe. `batch_id` settes til
`aldersjustering_bidrag_<årstall>`.

> Hvis det er feil i uttrekket, kan rader med årets `batch_id` slettes og batchen kjøres på nytt.
> **Ikke slett rader fra eldre batchkjøringer.**

**Grunnlagsoverføring (manuelt steg i Bisys):** Før alle grunnlag fra vedtak fattet i BBM er
overført til `bidrag-vedtak`, må grunnlagsoverføring kjøres for alle saker som skal
aldersjusteres, slik at beregningen har tilstrekkelige grunnlagsdata. Uttrekk over saker gjøres
med en SQL-spørring mot `barn`-tabellen, og filen brukes som input til Bisys-batchen `GB513`
(`sudo start-batch -f GB513 saksnr="¤FILE:..." modus=OVERFOR grid=10 overskrivOverfortGrunnlag=true`).
Dette steget er ikke migrert bort fra Bisys og må fortsatt kjøres manuelt frem til 
grunnlagsoverføring er fullført i sin helhet.

### 4.2 Beregn aldersjustering

`BeregnAldersjusteringerBidragBatch` behandler rader som matcher `statuser` (default
`UBEHANDLET,FEILET,SIMULERT`) via `AldersjusteringService.utførAldersjustering`, som delegerer
selve beregningen til `AldersjusteringOrchestrator` i `bidrag-beregn-barnebidrag`:

- Ved vellykket beregning: oppretter vedtaksforslag i `bidrag-vedtak`, status settes til
  `BEHANDLET`, behandlingstype `FATTET_FORSLAG`.
- Ved `SkalIkkeAldersjusteresException`: oppretter et avvist vedtaksforslag (beslutningstype
  `AVVIST`), behandlingstype `INGEN`, med begrunnelse fra beregningen.
- Ved `AldersjusteresManueltException`: oppretter et avvist vedtaksforslag, behandlingstype
  `MANUELL`, med begrunnelse fra beregningen – disse må følges opp med oppgave (se avsnitt 4.5).
- Ved teknisk feil (f.eks. mangler skyldner) eller uventet exception: status `FEILET`,
  behandlingstype `FEILET`, feilmelding lagres i `begrunnelse`.

Med `simuler=true` opprettes ikke noe vedtaksforslag i `bidrag-vedtak`, men
beregningen kjøres og resultatet lagres med status `SIMULERT` – nyttig for å hente ut oversikt
over antall barn og fordeling av behandlingstype før en reell kjøring.

`BEHANDLET`-rader kjøres ikke på nytt med mindre `BEHANDLET` eksplisitt inkluderes i `statuser` –
da slettes eksisterende vedtaksforslag og nytt opprettes (påvirker ikke allerede fattede vedtak).

### 4.3 Fatt vedtak

`FattVedtakOmAldersjusteringerBidragBatch` fatter vedtak for rader med status `BEHANDLET` og
`behandlingstype IN (MANUELL, FATTET_FORSLAG, INGEN)` (kan overstyres via `behandlingstyper`) som
ikke allerede er fattet, ved å kalle `fatteVedtaksforslag` i `bidrag-vedtak`.

- `simuler=false`: vedtak fattes. Status settes til `FATTET` ved suksess, eller
  `FATTE_VEDTAK_FEILET` ved feil (typisk fordi saksbehandler har fattet vedtak manuelt i
  mellomtiden på et grunnlag som ikke lenger er gyldig – disse rekjøres fra beregn-steget ved å
  inkludere `FATTE_VEDTAK_FEILET` i `statuser`).
- `simuler=true`: vedtak fattes ikke, men det opprettes likevel en
  **forsendelsebestilling** (kun for `behandlingstype = FATTET_FORSLAG`) i
  `forsendelse_bestilling`-tabellen for både BP og BM (`Forsendelsestype.ALDERSJUSTERING_BIDRAG`).
  Dette gjør det mulig å teste/kontrollere brevinnhold før vedtak faktisk fattes.

`kunRedusertBidrag=true` begrenser fatting til saker der aldersjustert beløp er lavere enn løpende
beløp – nyttig for å prioritere fatting av saker som ellers ville gitt avregning (B4) i regnskapet.

> Vedtak for manuelle saker må fattes etter 1. juli: det opprettes en søknad med mottatt dato
> 1. juli, og Bisys tillater ikke mottatt dato frem i tid.

### 4.4 Lagre B4-informasjon

`LagreB4InformasjonBidragBatch` henter, for hver fattet aldersjustering i `fattetÅr`, sum avregning
(transaksjonskode `B4`/`D4`) fra reskontro via `ReskontroService`, basert på vedtakets
vedtakstidspunkt. B4-beløpet oppstår når BM skylder BP penger – typisk fordi aldersjustert bidrag
er lavere enn allerede utbetalt bidrag for gjeldende periode. Kun beløp større enn null lagres i
`b4_beløp`-kolonnen. Batchen er idempotent og bør kjøres etter fatt vedtak-steget; den kan kjøres
flere ganger.

### 4.5 Opprett/slett oppgave

Aldersjustering bidrag oppretter en **Bisys-oppgave** for saker som må behandles manuelt.

- `OppgaveAldersjusteringBidragBatch` oppretter oppgave (type GEN, tema BID/FAR) for rader med
  `behandlingstype = MANUELL`, `status = BEHANDLET` og `oppgave IS NULL`, tildelt saksbehandlende
  enhet. Oppgaven inneholder begrunnelsen for hvorfor saken må behandles manuelt.
  `OppgaveService.opprettOppgaveForManuellAldersjustering` sjekker først om det allerede finnes en
  tilsvarende oppgave i saken, for å unngå duplikater.
- `SlettOppgaveAldersjusteringBidragBatch` sletter oppgaver som er opprettet for en gitt
  `batchId`

### 4.6 Opprett/distribuer forsendelse (delt infrastruktur)

Del 4 (`/batch/forsendelse/opprett`) og Del 5 (`/batch/forsendelse/distribuer`) er **ikke
aldersjustering-spesifikke** batcher, men generiske jobber som også brukes av andre batcher i `bidrag-automatisk-jobb` (f.eks.
revurdering av forskudd). De opererer på `forsendelse_bestilling`-tabellen uavhengig av hvilken
batch som opprettet bestillingen:

- **Opprett forsendelse:** finner bestillinger uten opprettet forsendelse og oppretter forsendelse
  i `bidrag-dokument-forsendelse`. `bestillingIder` kan brukes til å kjøre enkeltbestillinger på
  nytt (sletter først evt. eksisterende forsendelse). `prosesserFeilet=true` inkluderer
  bestillinger som tidligere har feilet.
- **Distribuer forsendelse:** arkiverer forsendelsen i Joark og bestiller distribusjon for alle
  opprettede og aktive forsendelser som ikke allerede er distribuert, eller kun for
  `bestillingIder` hvis angitt.

> Varsle i Slack-kanalen `#team-dokumenthåndtering` før det kjøres batch med opprettelse og
> distribusjon av vedtaksbrev for aldersjustering av barnebidrag. Sjekk innhold på noen av brevene
> (stikkprøver) før distribusjon bestilles. Hvis distribusjon feiler fordi dokumenter ikke er
> ferdigstilt, vent til de er klare, eller opprett forsendelsen på nytt i opprett-steget med
> `bestillingIder` og kjør distribusjon på nytt.

## 5. Feilhåndtering / manuell oppfølging etter kjøring

Følgende sjekkes etter en kjøring:

1. **Rader med `status = FEILET`:** Analyseres og rettes manuelt (feilmelding i `begrunnelse`).
   Rekjøres fra beregn-steget ved å inkludere `FEILET` i `statuser` (standard).
2. **Rader med `behandlingstype = MANUELL`:** Disse må behandles manuelt av saksbehandler via den
   opprettede Bisys-oppgaven. Etter ferdigbehandling ryddes oppgaven med
   `/aldersjustering/batch/bidrag/oppgave/slett`.
3. **Rader med `status = FATTE_VEDTAK_FEILET`:** Oppstår typisk når saksbehandler har fattet vedtak
   manuelt i perioden mellom beregning og fatting. Rekjøres fra beregn-steget ved å inkludere
   `FATTE_VEDTAK_FEILET` i `statuser`.
4. **Manglende B4-informasjon:** Sjekk at `lagreB4Informasjon`-steget er kjørt for riktig
   `fattetÅr` etter fatt vedtak-steget, slik at avregningsbeløp er tilgjengelig for regnskap/analyse.
5. **Forsendelse/brev:** Gjør stikkprøver på brevinnhold før distribusjon bestilles. Varsle
   `#team-dokumenthåndtering` i Slack før kjøring av opprett/distribuer-forsendelse.

## 6. Videre arbeid

- Grunnlagsoverføring (Bisys-batchen `GB513`) er ikke migrert og må fortsatt kjøres manuelt
  mellom opprett- og beregn-steget. Denne er planlagt ferdigstilt iløpet av 2026.
- Schedulerne er p.t. deaktivert (cron `"-"`) i alle miljø – automatisk kjøring må aktiveres
  eksplisitt før den tas i bruk.

## Relatert kode

- Controller: `AldersjusteringBidragBatchController`
- Batch-steg: `batch/aldersjustering/bidrag/{opprett,beregn,fattvedtak,lagreb4,oppgave}`
- Schedulere: `OpprettAldersjusteringerBidragScheduler`, `BeregnAldersjusteringerBidragScheduler`,
  `FattVedtakOmAldersjusteringerBidragScheduler`, `LagreB4InformasjonBidragScheduler`,
  `OppgaveAldersjusteringBidragScheduler`, `OpprettForsendelseBatchScheduler`,
  `DistribuerForsendelseBatchScheduler`
- Domenetjenester: `service/AldersjusteringService.kt`, `service/OppgaveService.kt`
- Entitet/repository: `persistence/entity/Aldersjustering.kt`,
  `persistence/repository/AldersjusteringRepository.kt`
- Delt forsendelse-infrastruktur: `batch/utils/forsendelse/*`, `service/ForsendelseBestillingService.kt`



## Nyttige SQL-spørringer for oppfølging (uttrekk over saker, statistikk over behandlingstype, oversikt over lagrede B4-beløp)

Hent utrekk over saker med følgende kommando:

```sql
SELECT a.id, b.id, b.saksnummer, a.status, a.begrunnelse 
FROM aldersjustering a 
INNER JOIN barn b ON b.id = a.barn_id 
WHERE a.behandlingstype = 'FATTET_FORSLAG';
```

Eller for mer detaljert utrekk for testing

```sql
SELECT
a.status,
saksnummer,
a.behandlingstype,
b.fødselsdato as fødselsdato,
resultat_siste_vedtak,
fattet_tidspunkt,
CASE
    WHEN array_length(begrunnelse, 1) IS NULL THEN ''
    ELSE string_agg(
            CONCAT(
                    UPPER(LEFT(REPLACE(begrunnelse_item, '_', ' '), 1)),
                    LOWER(SUBSTRING(REPLACE(begrunnelse_item, '_', ' '), 2))
            ), ', '
         )
    END AS begrunnelser,

CASE
    WHEN behandlingstype = 'MANUELL' THEN 'Ja'
    ELSE 'Nei'
    END AS skal_behandles_manuelt,
CASE
    WHEN behandlingstype = 'FATTET_FORSLAG' THEN 'Ja'
    ELSE 'Nei'
END AS fattes_vedtak,
a.vedtak as vedtak_id,
a.vedtaksid_beregning
FROM aldersjustering a
         INNER JOIN public.barn b ON b.id = a.barn_id
         LEFT JOIN LATERAL unnest(
        CASE
            WHEN array_length(begrunnelse, 1) > 0 THEN begrunnelse
            ELSE ARRAY[NULL]::text[]
            END
                           ) AS t(begrunnelse_item) ON array_length(begrunnelse, 1) > 0
WHERE a.status = 'BEHANDLET' and a.batch_id = 'aldersjustering_bidrag_2026'
GROUP BY b.id, a.id, a.oppgave, vedtak, vedtaksid_beregning, b.fødselsdato, b.kravhaver, saksnummer, begrunnelse, resultat_siste_vedtak, behandlingstype,b.skyldner
ORDER BY begrunnelser;
```

For statistikk
```sql
SELECT
    behandlingstype,
    CASE behandlingstype
        WHEN 'FATTET_FORSLAG' THEN 'Vedtak fattes automatisk'
        WHEN 'MANUELL'        THEN 'Skal behandles manuelt'
        WHEN 'INGEN'          THEN 'Ingen aldersjustering'
        WHEN 'FEILET'         THEN 'Feilet'
        ELSE                       'Ukjent / ikke beregnet'
    END                          AS beskrivelse,
    COUNT(*)                     AS totalt
FROM aldersjustering a
         INNER JOIN barn b ON b.id = a.barn_id
WHERE a.batch_id = 'aldersjustering_bidrag_2026'
GROUP BY behandlingstype
ORDER BY totalt DESC;
```

Hent oversikt over lagrede B4-beløp:

```sql
SELECT b.saksnummer, b.kravhaver, b.skyldner, a.b4_beløp
FROM aldersjustering a
INNER JOIN barn b ON b.id = a.barn_id
WHERE a.b4_beløp IS NOT NULL
  AND a.batch_id = 'aldersjustering_bidrag_2026'
ORDER BY a.b4_beløp DESC;
```
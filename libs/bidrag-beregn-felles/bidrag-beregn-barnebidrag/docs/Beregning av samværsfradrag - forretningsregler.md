# Beregning av samværsfradrag - forretningsregler

## Formål

Denne dokumentasjonen beskriver forretningsreglene i `BeregnSamværsfradragService`.

Tjenesten beregner samværsfradrag per periode basert på samværsklasse, barnets alder og gjeldende sjabloner.

Dokumentasjonen dekker flyten gjennom:

- `BeregnSamværsfradragService`
- `SamværsfradragMapper`
- `SamværsfradragBeregning`

---

## Hva beregnes?

Samværsfradrag er et fradrag i bidragsgrunnlaget som avhenger av:

- omfanget av samvær (samværsklasse)
- barnets alder
- sjablonverdier for samværsfradrag

Resultatet brukes i senere delberegninger av bidrag.

---

## Overordnet ansvar i modulene

### `SamværsfradragMapper`

Mapperen klargjør grunnlag per periode for:

- søknadsbarn
- samværsklasse
- sjablon samværsfradrag

Mapperen kan håndtere ulike flyter for søknadsbarn, løpende bidrag og privat avtale.

### `BeregnSamværsfradragService`

Tjenesten styrer periodisering, beregning per periode, sluttperiodehåndtering og resultatmapping.

### `SamværsfradragBeregning`

Utfører selve beregningen av fradragsbeløp per periode.

---

## Oppdeling i perioder

Beregningen deles i bruddperioder ved endringer i:

- samværsklasse
- sjablonperioder for samværsfradrag
- alderstrinn for barnet
- eventuelt virkningstidspunkt

Alderstrinn legges inn som egne bruddpunkter basert på sjablonenes aldersgrenser.

---

## Regel for aldersvurdering

Barnets alder beregnes med standardisert regel der barnet behandles som født 1. juli i fødselsåret.

For hver periode velges nærmeste gyldige `alderTom` i sjablonene som dekker barnets alder.

Dette brukes til å hente riktig fradragssats.

---

## Regel for løpende bidrag og privat avtale

Ved beregning knyttet til løpende bidrag eller privat avtale filtreres bruddperioder slik at bare perioder som overlapper med en samværsklasseperiode tas med.

Bakgrunnen er at samværsklasse ikke alltid er obligatorisk i disse flytene.

---

## Beregning per delperiode

For hver periode brukes:

- valgt alderstrinn for søknadsbarn
- samværsklasse i perioden
- sjablonverdier for samværsfradrag i perioden

Hvis samværsklasse mangler der den er påkrevd, stoppes beregningen med feil.

---

## Åpen sluttperiode

Hvis siste periode treffer beregningsperiodens slutt og åpen sluttperiode er aktivert, settes siste periode til åpen (`til = null`).

---

## Hva resultatet inneholder

Delberegningen per periode inneholder:

- fradragsbeløp for samvær
- periode
- grunnlagsreferanser

Resultatet knyttes til bidragspliktig og søknadsbarn.

---

## Kort oppsummering

`BeregnSamværsfradragService` følger disse hovedreglene:

1. Henter samværsklasse og samværsfradragssjabloner.
2. Deler perioden ved endringer i samvær, sjablon og alderstrinn.
3. Beregner fradrag per delperiode basert på samværsklasse og barnets alder.
4. Håndterer særskilt filtrering for løpende bidrag og privat avtale.
5. Returnerer periodisert fradrag med sporbare referanser.

---

## Eksempel

### Forutsetninger

- Barnet har registrert samværsklasse i perioden
- Sjablon `SAMVÆRSFRADRAG` er tilgjengelig med rader for relevant periode
- Barnets alder vurderes etter standardregelen (1. juli i fødselsåret)

### Illustrative tall

Basert på eksempel fra sjablon `SAMVÆRSFRADRAG`:

- Periode 1: samværsklasse 2, alderstrinn 6-10 år, sjablonsats 1 200
- Periode 2: samværsklasse 3, alderstrinn 6-10 år, sjablonsats 1 800
- Periode 3: samværsklasse 3, alderstrinn 11-14 år (barnet fyller 11 år), sjablonsats 2 000

### Trinnvis beregning

1. Tjenesten identifiserer bruddperioder fra:
   - endringer i samværsklasse
   - barnetrygdens aldersbrudd (barnet passerer 6 år, 11 år, 15 år)
   - periodebegrensninger i sjablon `SAMVÆRSFRADRAG`
2. For hver delperiode beregnes barnets eksakte alderstrinn.
3. Riktig sjablonrad velges ut fra kombinasjonen av samværsklasse og alderstrinn.
4. Fradragsbeløpet hentes fra sjablonen og lagres for delperioden.
5. Ved endring i samværsklasse eller når barnet passerer alderstrinn, opprettes ny periode med ny beregning.

### Resultat

- Når samværsklasse øker fra 2 til 3: fradraget øker fra 1 200 til 1 800 i ny periode
- Når barnet fyller 11 år: fradraget endres fra 1 800 til 2 000 selv om samværsklasse er uendret
- Hvis begge endringer skjer samtidig, behandles det som ett bruddpunkt med ny beregning





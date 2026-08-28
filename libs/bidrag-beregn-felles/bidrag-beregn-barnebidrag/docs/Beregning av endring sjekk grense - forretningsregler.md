# Beregning av endring sjekk grense - forretningsregler

## Formål

Denne dokumentasjonen beskriver forretningsreglene i `BeregnEndringSjekkGrenseService`.

Tjenesten gjør en samlet vurdering av om beregnet endring i bidrag er over eller under minimumsgrense for endring.

Dokumentasjonen dekker flyten gjennom:

- `BeregnEndringSjekkGrenseService`
- `EndringSjekkGrenseMapper`
- `EndringSjekkGrenseBeregning`

---

## Hva beregnes?

Tjenesten tar inn periodiserte resultater fra delberegning av endring-sjekk-grense-periode og produserer ett samlet kontrollresultat:

- `endringErOverGrense = true` eller `false`

Resultatet brukes i videre beslutning om bidragsvedtak skal endres.

Unntak: Dersom unleash-bryter beregning.bidrag_beregning_fra-forste-periode-over-tolv-prosent er aktivert, returneres resultat per periode

---

## Overordnet ansvar i modulene

### `EndringSjekkGrenseMapper`

Mapperen henter og klargjør perioderesultater fra `DELBEREGNING_ENDRING_SJEKK_GRENSE_PERIODE`.

### `BeregnEndringSjekkGrenseService`

Tjenesten:

- samler perioderesultatene
- kjører samlet grensesjekk (ikke periodisert)
- bygger resultatgrunnlag og referanser

### `EndringSjekkGrenseBeregning`

Utfører den samlede logikken som avgjør om endring er over grense.

---

## Ikke periodisert sluttvurdering

I motsetning til periode-tjenesten, gjør denne tjenesten en samlet vurdering på tvers av perioderesultatene.

Det betyr at perioderesultatene først brukes som input, og deretter omsettes til én samlet konklusjon.

Unntak: Dersom unleash-bryter beregning.bidrag_beregning_fra-forste-periode-over-tolv-prosent er aktivert, returneres resultat per periode

---

## Resultatperiode

Resultatet mappes til én periode som starter på beregningsperiodens `fom`.

Sluttdato settes slik:

- `til = null` ved åpen sluttperiode
- ellers `til = mottattGrunnlag.periode.til`

---

## Sporbarhet

Resultatet inneholder referanser til grunnlagene som inngår i den samlede vurderingen.

Dette gjør det mulig å se hvilke perioderesultater som førte til konklusjonen.

---

## Hva resultatet inneholder

Delberegningen inneholder:

- periode
- om endringen er over grense
- grunnlagsreferanser

---

## Kort oppsummering

`BeregnEndringSjekkGrenseService` følger disse hovedreglene:

1. Leser periodiserte grensesjekkresultater.
2. Gjør én samlet vurdering av om endring er over grense. Hvis minst en periode er over grense (12 %), blir samlet resultat `true`.
3. Returnerer ett samlet kontrollgrunnlag for videre behandling.
4. Sikrer sporbarhet ved å ta med grunnlagsreferanser.

Unntak: Dersom unleash-bryter beregning.bidrag_beregning_fra-forste-periode-over-tolv-prosent er aktivert, returneres resultat per periode med følgende regler:
- Resultatperiodene følger periodene fra delberegning DELBEREGNING_ENDRING_SJEKK_GRENSE_PERIODE
- Hver periode vurderes individuelt: Hvis endring i perioden er over grense (12 %), settes endringErOverGrense = true for den perioden, ellers false
- Hvis en periode er over grense, settes alle påfølgende perioder til endringErOverGrense = true


---

## Eksempel

### Forutsetninger

- Perioderesultater fra `DELBEREGNING_ENDRING_SJEKK_GRENSE_PERIODE` er tilgjengelige
- Samlet vurdering skal gi ett beslutningsgrunnlag
- Minimum én periode over grense → samlet resultat blir true

### Illustrative tall

**Periode 1 (jan-mar):** 
- Løpende bidrag: 3 000
- Beregnet bidrag: 3 150
- Endring: 150 (5 %)
- endringErOverGrense: **false**

**Periode 2 (apr-jun):** 
- Løpende bidrag: 3 000
- Beregnet bidrag: 3 500
- Endring: 500 (16,7 %)
- endringErOverGrense: **true**

**Periode 3 (jul-sep):** 
- Løpende bidrag: 3 500
- Beregnet bidrag: 3 600
- Endring: 100 (2,9 %)
- endringErOverGrense: **false**

### Trinnvis beregning

1. Tjenesten henter perioderesultatene fra periodeberegningen.
2. Hver periode evalueres:
   - Periode 1: under grense (5 % < 12 %)
   - Periode 2: over grense (16,7 % > 12 %)
   - Periode 3: under grense (2,9 % < 12 %)
3. Den samlede logikken vurderer om grensen er brutt i en eller flere perioder.
4. Siden **minst én periode** er over grensen (periode 2), blir samlet resultat: `endringErOverGrense = true`
5. Tjenesten mapper resultatet til én samlet periode for vedtaksfatting.

Unntak: Dersom unleash-bryter beregning.bidrag_beregning_fra-forste-periode-over-tolv-prosent er aktivert, returneres resultat per periode:
- Periode 1: `endringErOverGrense = false`
- Periode 2: `endringErOverGrense = true`
- Periode 3: `endringErOverGrense = true` (siden periode 2 er over grense, settes alle påfølgende perioder til true)

### Resultat

Selv om periode 1 og 3 er under grense, bestemmer periode 2 at samlet resultat blir `endringErOverGrense = true`. Dette betyr at ved minst én periode med endring over minimumsgrense, skal vedtak fattes på grunnlag av den nye beregningen. Hvis alle perioder hadde vært under grense, ville samlet resultat blitt `false` og beløpet ville ikke blitt endret.






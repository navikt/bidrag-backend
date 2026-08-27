# Beregning av endring sjekk grense periode - forretningsregler

## Formål

Denne dokumentasjonen beskriver forretningsreglene i `BeregnEndringSjekkGrensePeriodeService`.

Tjenesten vurderer per periode om endringen mellom løpende bidrag og beregnet bidrag er over eller under fastsatt grense.

Dokumentasjonen dekker flyten gjennom:

- `BeregnEndringSjekkGrensePeriodeService`
- `EndringSjekkGrensePeriodeMapper`
- `EndringSjekkGrensePeriodeBeregning`

---

## Hva beregnes?

For hver delperiode beregnes et kontrollresultat som blant annet viser:

- løpende bidrag (fra beløpshistorikk og/eller privat avtale)
- beregnet nytt bidrag
- faktisk endringsfaktor
- om endringen er over grense

Dette resultatet brukes videre i samlet endringskontroll.

---

## Overordnet ansvar i modulene

### `EndringSjekkGrensePeriodeMapper`

Mapperen klargjør periodisert grunnlag fra:

- sluttberegnet bidrag
- beløpshistorikk
- privat avtale (indeksregulert)
- relevante sjablontall

### `BeregnEndringSjekkGrensePeriodeService`

Tjenesten styrer periodisering, beregning per periode, håndtering av opphør/åpen sluttperiode og resultatgrunnlag.

### `EndringSjekkGrensePeriodeBeregning`

Utfører selve grensesjekken for én periode.

---

## Viktig regel for type beløpshistorikk

Tjenesten velger beløpshistorikk-type ut fra stønadstype:

- `BIDRAG18AAR` -> `BELØPSHISTORIKK_BIDRAG_18_ÅR`
- øvrige tilfeller -> `BELØPSHISTORIKK_BIDRAG`

Dette sikrer at riktig historikk brukes for rett ytelsestype.

---

## Oppdeling i perioder

Beregningen periodiseres ved endringer i:

- sluttberegnet bidrag
- beløpshistorikk
- privat avtale (indeksregulert)
- sjablontall
- eventuelt virkningstidspunkt (V2)

Resultatet beregnes per bruddperiode for å unngå at ulike regelgrunnlag blandes.

---

## Beregningsgrunnlag per periode

For hver periode hentes:

- beregnet bidrag (obligatorisk)
- løpende bidrag fra historikk (kan mangle i en konkret periode)
- privat avtalebeløp (hvis tilgjengelig)
- sjablontall

Når løpende bidrag mangler i perioden, settes beløp til `null`, men historikkreferansen beholdes for sporbarhet.

---

## Regler for sluttperiode og opphør

Etter periodisert beregning håndteres siste periode slik:

- hvis åpen sluttperiode er aktiv og siste periode treffer beregningsperiodens slutt -> `til = null`
- hvis periode ikke er åpen og `opphørsdato` finnes -> `til` settes til opphørsdato

Dette gjør at resultatperiodene følger både normal åpen sluttlogikk og eksplisitt opphør.

---

## V1 og V2

Tjenesten har både V1 og V2-flyt.

Forskjellen er i hovedsak:

- V2 støtter virkningstidspunkt i periodiseringen
- V2 bruker V2-mapping av privat avtale (indeksregulert beløp)

Forretningsmålet er likevel det samme: periodisert sjekk av om endring er over/under grense.

---

## Hva resultatet inneholder per periode

Delberegningen per periode inkluderer:

- løpende bidragbeløp
- om løpende bidrag kommer fra privat avtale
- beregnet bidragbeløp
- faktisk endringsfaktor
- flagg for om endringen er over grense

Samtidig returneres referanser til grunnlagene som ble brukt.

---

## Kort oppsummering

`BeregnEndringSjekkGrensePeriodeService` følger disse hovedreglene:

1. Riktig beløpshistorikk velges ut fra stønadstype.
2. Grunnlaget periodiseres ved endringer i bidrag, historikk, privat avtale og sjabloner.
3. Endring mot løpende nivå beregnes per periode.
4. Siste periode justeres for åpen sluttperiode eller opphør.
5. Resultat med sporbare referanser sendes videre til samlet grensesjekk.

---

## Eksempel

### Forutsetninger

- Det finnes både løpende nivå og nytt beregnet nivå
- Endringen skal vurderes per bruddperiode
- Sjablontall for grenseverdi er tilgjengelig
- Minimumsgrense for endring er satt til 12 prosent

### Illustrative tall

**Periode A:**
- Løpende bidrag: 3 000
- Beregnet bidrag: 3 150
- Endring: 3 150 - 3 000 = 150
- Endringsprosent: 150 / 3 000 = 5 %
- Grenseverdi: 12 %
- Resultat: 5 % < 12 % → endringErOverGrense = **false**

**Periode B:**
- Løpende bidrag: 3 000
- Beregnet bidrag: 3 500
- Endring: 3 500 - 3 000 = 500
- Endringsprosent: 500 / 3 000 = 16,7 %
- Grenseverdi: 12 %
- Resultat: 16,7 % > 12 % → endringErOverGrense = **true**

### Trinnvis beregning

1. Tjenesten splitter perioden når grunnlag endres.
2. For hver delperiode hentes løpende bidrag, beregnet bidrag og relevante sjablontall for grenseverdi.
3. Faktisk endring beregnes per delperiode:
   - Absolutt endring: beregnet - løpende
   - Relativ endring (prosent): endring / løpende × 100
4. Endringen sammenlignes med grenseverdien (sjablontall) for samme delperiode.
5. Hver periode merkes med `endringErOverGrense = true/false`.

### Resultat

Periode A kan ende under grense (5 %), mens periode B kan ende over grense (16,7 %). Derfor må grensesjekken utføres periodisert før samlet vurdering gjøres i neste steg. Denne periodiseringen sikrer at små endringer som ikke oppfyller minimumskravet, ikke automatisk fører til nye vedtak.





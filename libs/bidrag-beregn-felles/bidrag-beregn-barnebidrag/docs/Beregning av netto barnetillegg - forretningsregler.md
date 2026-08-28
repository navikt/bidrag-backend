# Beregning av netto barnetillegg - forretningsregler

## Formål

Denne dokumentasjonen beskriver forretningsreglene i `BeregnNettoBarnetilleggService`.

Tjenesten beregner netto barnetillegg per periode for valgt rolle (typisk BP eller BM), basert på barnetillegg og tilhørende skattefaktor.

Dokumentasjonen dekker flyten gjennom:

- `BeregnNettoBarnetilleggService`
- `NettoBarnetilleggMapper`
- `NettoBarnetilleggBeregning`

---

## Hva beregnes?

Netto barnetillegg beregnes som en periodisert delberegning som viser:

- summert brutto barnetillegg
- summert netto barnetillegg
- hvilke barnetilleggstyper som inngår

Resultatet brukes videre i barnebidragsberegningen.

---

## Overordnet ansvar i modulene

### `NettoBarnetilleggMapper`

Mapperen finner barnetillegg for valgt rolle og klargjør periodisert grunnlag.

### `BeregnNettoBarnetilleggService`

Tjenesten:

- finner referanse til valgt rolle
- periodiserer barnetillegg
- beregner netto barnetillegg per bruddperiode
- returnerer resultat med grunnlagsreferanser

### `NettoBarnetilleggBeregning`

Utfører selve summeringen og netto-beregningen for én periode.

---

## Viktig regel om rolle

Tjenesten beregner netto barnetillegg for rollen som sendes inn:

- `PERSON_BIDRAGSPLIKTIG` eller
- `PERSON_BIDRAGSMOTTAKER`

Dette gjør samme tjeneste gjenbrukbar for begge sider av saken.

---

## Oppdeling i perioder

Beregningen splittes ved endringer i:

- perioder med barnetillegg
- eventuelt virkningstidspunkt

Dermed beregnes netto barnetillegg separat i hver delperiode der grunnlaget er konstant.

---

## Regel for tomt grunnlag i en periode

Hvis ingen barnetillegg gjelder i en delperiode, gjøres det ikke beregning for perioden.

Tjenesten returnerer altså kun perioder der barnetillegg faktisk finnes.

---

## Åpen sluttperiode

Hvis siste beregnede periode treffer slutten av beregningsperioden og åpen sluttperiode er aktivert, settes siste periode til åpen (`til = null`).

---

## Hva resultatet inneholder

Per periode returneres:

- summert brutto barnetillegg
- summert netto barnetillegg
- liste over barnetilleggstyper
- grunnlagsreferanser

Dette gjør det synlig både hvilke tillegg som er brukt og hva nettoeffekten er.

---

## Kort oppsummering

`BeregnNettoBarnetilleggService` følger disse hovedreglene:

1. Velger grunnlag for riktig rolle (BP eller BM).
2. Deler perioden i bruddperioder ved endringer i barnetillegg.
3. Beregner bare perioder med faktisk barnetillegg.
4. Returnerer brutto, netto og typer per periode.
5. Åpner siste periode ved behov og tar med sporbare referanser.

---

## Eksempel

### Forutsetninger

- Beregningen kjøres for én rolle (BP eller BM)
- To barnetilleggstyper gjelder i samme periode
- Sjablontall `BARNETILLEGG_SKATT` brukes for skattefaktor

### Illustrative tall

- Barnetillegg type 1 (lønn): brutto 1 500
- Barnetillegg type 2 (pensjon): brutto 1 000
- Summert brutto: 2 500
- Skattefaktor (fra `BARNETILLEGG_SKATT`): 22 prosent

### Trinnvis beregning

1. Tjenesten henter barnetillegg som gjelder i perioden for valgt rolle (BP eller BM).
2. Barnetilleggstyper og beløp summeres:
   - Type 1: 1 500
   - Type 2: 1 000
   - Samlet brutto: 2 500
3. Skattefaktor fra sjablontall `BARNETILLEGG_SKATT` hentes (22 % i eksempelet).
4. Netto beregnes ved å redusere brutto for skatt:
   - Skattekostnad: 2 500 × 22 % = 550
   - Netto barnetillegg: 2 500 - 550 = 1 950
5. Barnetilleggstyper beholdes i resultatet for sporbarhet og forklaring.
6. Hvis grunnlaget endres i ny periode, gjentas beregningen i ny bruddperiode.

### Resultat

Netto barnetillegg blir 1 950 (lavere enn brutto 2 500) fordi skattefaktoren fra sjablontall `BARNETILLEGG_SKATT` reduserer beløpet med 550. Resultatet viser både brutto, netto og hvilke tilleggstyper som inngår.





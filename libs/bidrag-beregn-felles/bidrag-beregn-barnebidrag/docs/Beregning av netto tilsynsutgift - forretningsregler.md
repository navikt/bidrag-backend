# Beregning av netto tilsynsutgift - forretningsregler

## Formål

Denne dokumentasjonen beskriver forretningsreglene i `BeregnNettoTilsynsutgiftService`.

Tjenesten beregner netto tilsynsutgift per periode, basert på faktiske tilsynsutgifter, tilleggsstønad, antall barn og gjeldende sjablonbegrensninger.

Dokumentasjonen dekker flyten gjennom:

- `BeregnNettoTilsynsutgiftService`
- `NettoTilsynsutgiftMapper`
- `NettoTilsynsutgiftBeregning`

---

## Hva beregnes?

Per periode beregnes blant annet:

- total/brutto tilsynsutgift
- justert brutto tilsynsutgift
- skattefradrag
- netto tilsynsutgift
- beløp per barn med tilsynsutgifter

Resultatet brukes videre i underholdskostnad og øvrige delberegninger.

---

## Overordnet ansvar i modulene

### `NettoTilsynsutgiftMapper`

Mapperen henter og strukturerer grunnlag for:

- faktisk utgift
- tilleggsstønad
- barn hos bidragsmottaker
- sjabloner for maks tilsynsbeløp, maks fradrag og sjablontall

### `BeregnNettoTilsynsutgiftService`

Tjenesten:

- henter sjabloner
- periodiserer grunnlaget
- teller relevante barn i perioden
- velger riktig sjablonnivå per periode
- beregner netto tilsynsutgift
- bygger resultat- og sporbarhetsgrunnlag

### `NettoTilsynsutgiftBeregning`

Utfører selve beløpsberegningen for én periode.

---

## Oppdeling i perioder

Beregningen periodiseres ved endringer i:

- faktisk utgift
- tilleggsstønad
- sjablon for maks tilsynsbeløp
- sjablon for maks fradragsbeløp
- relevante sjablontall
- barnas fødselsmåneder (for barn født etter beregningsperiodens start)
- eventuelt virkningstidspunkt

Fødselsmåned legges inn som bruddpunkt for å få korrekt telling av barn under 12 år.

---

## Viktige regler for hvem som inngår

### Beregning gjøres bare når det finnes utgifter

Hvis ingen barn har faktiske tilsynsutgifter i en delperiode, gjøres det ingen beregning for den perioden.

### Barn under 12 år

Tjenesten beregner antall barn under 12 år etter en standardisert aldersregel der alder vurderes med fødselsdato justert til 1. juli i fødselsåret.

Dette brukes ved valg av fradragsgrenser.

---

## Valg av sjablonverdier per periode

For hver delperiode velges sjablonverdier som gjelder i perioden.

Ved flere sjablonnivåer velges første nivå som dekker relevant antall barn:

- `MAKS_TILSYN_BELØP`: basert på antall barn med tilsynsutgifter – velges riktig satsnivå fra sjablonen
- `MAKS_FRADRAG_TILSYN_BELØP`: basert på beregnet antall barn hos bidragsmottaker – velges riktig satsnivå fra sjablonen

I tillegg brukes `SKATTESATS_TILSYN` og andre relevante sjablontall for skattefradrag.

Dette sikrer at riktig trinn brukes når beløp skal begrenses.

---

## Regler for sluttperiode og opphør

Siste periode håndteres slik:

- hvis siste periode går til beregningsperiodens slutt og åpen sluttperiode er aktiv -> `til = null`
- hvis siste periode er åpen og opphørsdato finnes -> `til` settes til opphørsdato

---

## Delresultater og sporbarhet

Tjenesten mapper også delberegninger for:

- faktisk tilsynsutgift
- tilleggsstønad

og tar med referanser tilbake til opprinnelige grunnlag.

Dette gjør det mulig å forklare hvordan netto tilsynsutgift ble etablert.

---

## Hva resultatet inneholder

Delberegningen per periode inneholder blant annet:

- total/brutto/justert brutto tilsynsutgift
- skattefradrag og tilhørende delkomponenter
- antall barn som inngår i beregningen
- netto tilsynsutgift
- tilsynsutgift per barn
- om resultatet er begrenset av maksimumsregler

---

## Kort oppsummering

`BeregnNettoTilsynsutgiftService` følger disse hovedreglene:

1. Bruker faktisk utgift og tilleggsstønad som hovedgrunnlag.
2. Periodiserer ved alle relevante endringer i utgift, barn og sjabloner.
3. Beregner kun perioder med faktiske tilsynsutgifter.
4. Velger maksgrenser ut fra antall barn i perioden.
5. Returnerer nettoresultat med detaljer og sporbare referanser.

---

## Eksempel

### Forutsetninger

- To barn har faktiske tilsynsutgifter i perioden
- Tilleggsstønad finnes
- Maksgrenser fra sjablon gjelder
- Barn under 12 år brukes for å velge fradragsgrenser

### Illustrative tall

- Faktisk utgift i perioden: 6 000
- Tilleggsstønad: 1 000
- Antall barn med tilsynsutgifter: 2
- Maks tilsynsbeløp per barn (fra `MAKS_TILSYN_BELØP`): 3 500 (2-barns nivå)
- Antall barn hos bidragsmottaker: 1
- Maks fradrag (fra `MAKS_FRADRAG_TILSYN_BELØP`): 7 000 (1-barn nivå)
- Skattefaktor (fra `SKATTESATS_TILSYN`): 20 prosent

### Trinnvis beregning

1. Tjenesten identifiserer 2 barn med faktiske utgifter i perioden.
2. Brutto tilsynsutgift: 6 000
3. Tilleggsstønad reduserer: 6 000 - 1 000 = 5 000
4. Justert brutto sjekkes mot maks for 2 barn:
   - Maks: 3 500 × 2 = 7 000
   - Justert brutto: 5 000 (under grensen)
5. Beregnet antall barn hos bidragsmottaker: 1 barn
6. Maks fradragsgrense for 1 barn: 7 000
7. Skattefradrag: 5 000 × 20 % = 1 000 (innenfor grensen)
8. Netto tilsynsutgift: 5 000 - 1 000 = 4 000
9. Tilsynsutgift per barn: 4 000 ÷ 2 = 2 000 per barn

### Resultat

Netto tilsynsutgift blir 4 000, som er 1 000 lavere enn brutto utgift (etter tilleggsstønad) fordi skattefradraget reduserer beløpet. Hvis antall barn eller utgiftsnivå endres senere, opprettes ny bruddperiode og beregningen oppdateres.






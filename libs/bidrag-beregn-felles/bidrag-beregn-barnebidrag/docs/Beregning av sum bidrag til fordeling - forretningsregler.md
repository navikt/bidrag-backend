# Beregning av sum bidrag til fordeling - forretningsregler

## Formål

Denne dokumentasjonen beskriver delberegningen `delberegningSumBidragTilFordeling` i `BeregnEndeligBidragServiceV2`.

---

## Hva beregnes?

Delberegningen summerer bidrag til fordeling på tvers av:

- søknadsbarn
- løpende bidrag
- privat avtale

Den beregner også sum av prioriterte bidrag som skal trekkes fra før fordeling av evne.

---

## Regelsett

Beregningen utføres i `EndeligBidragBeregningV2.beregnSumBidragTilFordeling`.

Formler:

- `sumBidragTilFordeling = sum(søknadsbarn) + sum(løpende) + sum(privat avtale)`
- `sumPrioriterteBidragTilFordeling =`
  - løpende bidrag der `!erNorskBidrag || erOppfostringsbidrag`
  - pluss privat avtale der `!erNorskBidrag`
- `erKompletteGrunnlagForAlleLøpendeBidrag = bidragTilFordelingLøpendeBidragBeregningGrunnlagListe.isEmpty()`

Alle summer avrundes til 2 desimaler.

---

## Periodisering

Bruddperioder lages ut fra alle inngående perioder i de tre kildene.

Steget kjøres med `åpenSluttperiode = true` i orkestreringen, siden grunnlaget kan gå på tvers av flere barn/saker.

---

## Resultat og sporbarhet

Delresultatet inneholder:

- `sumBidragTilFordeling`
- `sumPrioriterteBidragTilFordeling`
- `erKompletteGrunnlagForAlleLøpendeBidrag`
- referanser til alle bidrag som inngår i summeringen

---

## Eksempel

- Sum søknadsbarn: 8 000
- Sum løpende bidrag: 2 000
- Sum privat avtale: 1 000

Da blir:

- `sumBidragTilFordeling = 11 000,00`

Hvis prioriterte bidrag er:

- løpende utenlandsk: 700
- oppfostringsbidrag: 500
- privat avtale utenlandsk: 300

Da blir:

- `sumPrioriterteBidragTilFordeling = 1 500,00`


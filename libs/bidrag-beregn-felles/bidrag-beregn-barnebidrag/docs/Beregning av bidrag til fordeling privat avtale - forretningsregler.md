# Beregning av bidrag til fordeling privat avtale - forretningsregler

## Formål

Denne dokumentasjonen beskriver delberegningen `delberegningBidragTilFordelingPrivatAvtale` i `BeregnEndeligBidragServiceV2`.

---

## Hva beregnes?

Delberegningen beregner bidrag til fordeling for private avtaler, basert på indeksregulert avtale og samværsfradrag.

---

## Regelsett

Beregningen utføres i `EndeligBidragBeregningV2.beregnBidragTilFordelingPrivatAvtale`.

### Valuta

- henter valutakurs NOK -> saksvaluta
- henter valutakurs saksvaluta -> NOK
- hvis valuta er NOK, brukes kurs 1
- manglende nødvendig kurs gir feil

### Beregning

- `indeksregulertBeløpValuta` hentes fra delberegning indeksregulering privat avtale
- `samværsfradragValuta = samværsfradragNOK * valutakursFraNOK` (hvis samværsfradrag finnes)
- `bidragTilFordelingValuta = indeksregulertBeløpValuta + samværsfradragValuta`
- `bidragTilFordelingNOK = bidragTilFordelingValuta * valutakursTilNOK`
- `erNorskBidrag = (valutakode == NOK && sakskategori == NASJONAL)`

### Manglende indeksregulering

I serviceklassen kan beregningsgrunnlag være `null` hvis indeksregulering mangler i perioden. Da hoppes perioden over i denne delberegningen.

---

## Periodisering

Bruddperioder lages fra:

- privat avtale
- indeksregulering privat avtale
- samværsfradrag
- valutakursgrunnlag

Siste periode kan åpnes i generell funksjon, men i orkestreringen kjøres dette steget med `åpenSluttperiode = false`.

---

## Eksempel

Forutsetning:

- valutakode: NOK
- indeksregulert beløp: 2 800
- samværsfradrag: 400

Utregning:

- `bidragTilFordeling = 2 800 + 400 = 3 200`
- `bidragTilFordelingNOK = 3 200`

Resultat:

- `erNorskBidrag = true`


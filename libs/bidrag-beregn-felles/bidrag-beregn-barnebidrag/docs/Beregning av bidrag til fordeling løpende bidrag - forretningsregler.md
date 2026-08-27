# Beregning av bidrag til fordeling løpende bidrag - forretningsregler

## Formål

Denne dokumentasjonen beskriver delberegningen `delberegningBidragTilFordelingLøpendeBidrag` i `BeregnEndeligBidragServiceV2`.

---

## Hva beregnes?

Delberegningen beregner bidrag til fordeling for løpende bidragssaker, inkludert valutahåndtering.

---

## Regelsett

Beregningen utføres i `EndeligBidragBeregningV2.beregnBidragTilFordelingLøpendeBidrag`.

### Valuta

- henter valutakurs NOK -> saksvaluta
- henter valutakurs saksvaluta -> NOK
- hvis valuta er NOK, brukes kurs 1
- manglende nødvendig kurs gir feil

### Beregning

- `reduksjonUnderholdskostnad = max(beregnetBeløp - faktiskBeløp, 0)`
- `samværsfradragValuta = samværsfradragNOK * valutakursFraNOK` (hvis samværsfradrag finnes)
- `bidragTilFordelingValuta = løpendeBeløp + samværsfradragValuta + reduksjonUnderholdskostnad`
- `bidragTilFordelingNOK = bidragTilFordelingValuta * valutakursTilNOK`

Unntak: Dersom unleash-bryter beregning.bidrag_reduksjon_underholdskostnad er skrudd av, er `reduksjonUnderholdskostnad` ikke med i regnestykket.

### Klassifisering

- `erNorskBidrag = (valutakode == NOK && sakskategori == NASJONAL)`
- `erOppfostringsbidrag = (stønadstype == OPPFOSTRINGSBIDRAG)`

---

## Periodisering

Bruddperioder lages fra:

- løpende bidrag
- samværsfradrag
- valutakursgrunnlag

Siste periode kan åpnes i generell funksjon, men i orkestreringen kjøres dette steget med `åpenSluttperiode = false`.

---

## Eksempel

Forutsetning:

- valutakode: EUR
- løpende beløp: 300 EUR
- beregnet beløp: 350 EUR
- faktisk beløp: 320 EUR
- samværsfradrag: 500 NOK
- kurs NOK->EUR: 0,09
- kurs EUR->NOK: 11,11

Utregning:

- `reduksjon = max(350 - 320, 0) = 30 EUR`
- `samværsfradragValuta = 500 * 0,09 = 45 EUR`
- `bidragTilFordelingValuta = 300 + 45 + 30 = 375 EUR`
- `bidragTilFordelingNOK = 375 * 11,11 = 4 166,25 NOK`


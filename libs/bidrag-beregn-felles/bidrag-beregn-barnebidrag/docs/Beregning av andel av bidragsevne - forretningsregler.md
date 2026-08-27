# Beregning av andel av bidragsevne - forretningsregler

## Formål

Denne dokumentasjonen beskriver delberegningen `delberegningAndelAvBidragsevne` i `BeregnEndeligBidragServiceV2`.

---

## Hva beregnes?

Delberegningen fordeler tilgjengelig evne mellom bidrag som konkurrerer i samme periode.

---

## Regelsett

Beregningen utføres i `EndeligBidragBeregningV2.beregnAndelAvBidragsevne`.

### Trinn 1: Juster sum for prioriterte bidrag

- `sumBidragTilFordelingJustertForPrioriterteBidrag = sumBidragTilFordeling - sumPrioriterteBidragTilFordeling`

### Trinn 2: Finn andel av sum

- hvis justert sum er 0: faktor = 0
- ellers:
  - `andelAvSumBidragTilFordelingFaktor = bidragTilFordeling / sumBidragTilFordelingJustertForPrioriterteBidrag`

### Trinn 3: Juster evne for prioriterte bidrag

- `evneJustertForPrioriterteBidrag = max(evne25Prosent - sumPrioriterteBidragTilFordeling, 0)`

### Trinn 4: Beregn andel av evne

- `andelAvEvneBeløp = evneJustertForPrioriterteBidrag * andelAvSumBidragTilFordelingFaktor`
- `bidragEtterFordeling = min(bidragTilFordeling, andelAvEvneBeløp)`
- `bruttoBidragJustertForEvneOg25Prosent = min(bidragTilFordeling, evneJustertForPrioriterteBidrag)`
- `harBPFullEvne = andelAvEvneBeløp >= bidragTilFordeling`

Avrunding:

- faktor: 10 desimaler
- beløp: 2 desimaler

---

## Periodisering

Bruddperioder lages fra:

- delberegning bidrag til fordeling
- delberegning sum bidrag til fordeling
- delberegning evne 25 prosent av inntekt
- eventuelt virkningstidspunkt

Siste periode kan åpnes (`til = null`) når `åpenSluttperiode` gjelder.

---

## Eksempel

- `bidragTilFordeling = 3 000`
- `sumBidragTilFordeling = 10 000`
- `sumPrioriterteBidragTilFordeling = 1 000`
- `evne25Prosent = 6 000`

Utregning:

- `sumJustert = 10 000 - 1 000 = 9 000`
- `andelFaktor = 3 000 / 9 000 = 0,3333333333`
- `evneJustert = 6 000 - 1 000 = 5 000`
- `andelAvEvneBeløp = 5 000 * 0,3333333333 = 1 666,67`
- `bidragEtterFordeling = min(3 000, 1 666,67) = 1 666,67`


# Beregning av evne 25 prosent av inntekt - forretningsregler

## Formål

Denne dokumentasjonen beskriver delberegningen `delberegningEvne25ProsentAvInntekt` i `BeregnEndeligBidragServiceV2`.

---

## Hva beregnes?

Delberegningen finner BPs evne justert mot 25 prosent av inntekt.

---

## Regelsett

Beregningen utføres i `EndeligBidragBeregningV2.beregnEvne25ProsentAvInntekt`:

- `evneJustertFor25ProsentAvInntekt = min(bidragsevne, sumInntekt25Prosent)`
- `erEvneJustertNedTil25ProsentAvInntekt = (sumInntekt25Prosent < bidragsevne)`

Resultatbeløpet avrundes til 2 desimaler.

---

## Periodisering

Bruddperioder lages fra:

- delberegning bidragsevne
- eventuelt virkningstidspunkt

Siste periode kan åpnes (`til = null`) når `åpenSluttperiode` gjelder.

---

## Resultat og sporbarhet

Delresultatet inneholder:

- justert evnebeløp
- flagg for om evnen er nedjustert av 25-prosentregelen
- referanse til bidragsevnegrunnlaget

---

## Eksempel

- `bidragsevne = 4 600`
- `sumInntekt25Prosent = 4 200`

Utregning:

- `evneJustertFor25ProsentAvInntekt = min(4 600, 4 200) = 4 200`
- `erEvneJustertNedTil25ProsentAvInntekt = true`


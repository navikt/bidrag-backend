# Beregning av bidrag justert for BP barnetillegg - forretningsregler

## Formål

Denne dokumentasjonen beskriver delberegningen `delberegningBidragJustertForBPBarnetillegg` i `BeregnEndeligBidragServiceV2`.

---

## Hva beregnes?

Delberegningen justerer bidrag etter fordeling opp mot netto barnetillegg hos BP.

---

## Regelsett

Beregningen utføres i `EndeligBidragBeregningV2.beregnBidragJustertForBPBarnetillegg`.

- `nettoBarnetilleggBP = barnetilleggBP eller 0`
- `bidragEtterFordeling = fra delberegning andel av bidragsevne`

Deretter:

1. Hvis delt bosted: behold `bidragEtterFordeling`
2. Ellers, hvis `nettoBarnetilleggBP > bidragEtterFordeling`: bruk `nettoBarnetilleggBP`
3. Ellers: bruk `bidragEtterFordeling`

I praksis for ikke-delt bosted blir beløpet `max(bidragEtterFordeling, nettoBarnetilleggBP)`.

Flagg:

- `erBidragJustertTilNettoBarnetilleggBP = (resultat == nettoBarnetilleggBP)`

Resultat avrundes til 2 desimaler.

---

## Periodisering

Bruddperioder lages fra:

- andel av bidragsevne
- netto barnetillegg BP
- delt bosted (for valg av regel)
- eventuelt virkningstidspunkt

Siste periode kan åpnes (`til = null`) når `åpenSluttperiode` gjelder.

---

## Eksempel

Ikke delt bosted:

- `bidragEtterFordeling = 2 100`
- `nettoBarnetilleggBP = 2 400`

Utregning:

- `resultat = 2 400`
- `erBidragJustertTilNettoBarnetilleggBP = true`

Delt bosted med samme tall:

- `resultat = 2 100` (ingen oppjustering mot barnetillegg i dette steget)


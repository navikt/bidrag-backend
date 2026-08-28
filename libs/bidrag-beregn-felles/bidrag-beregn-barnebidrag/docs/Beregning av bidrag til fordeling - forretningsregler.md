# Beregning av bidrag til fordeling - forretningsregler

## Formål

Denne dokumentasjonen beskriver delberegningen `delberegningBidragTilFordeling` i `BeregnEndeligBidragServiceV2`.

---

## Hva beregnes?

Delberegningen fastsetter bidrag som skal inngå i fordeling mot BPs evne.

Samtidig beregnes flagg for avslag og om bidraget er justert for netto barnetillegg hos BM.

---

## Regelsett

Beregningen utføres i `EndeligBidragBeregningV2.beregnBidragTilFordeling`.

### Valg av grunnlag ved delt bosted

- `erDeltBosted = bidragspliktigesAndelDeltBosted finnes`
- Ved delt bosted:
  - `samværsfradrag = 0`
  - BP-andel-beløp hentes fra `BidragspliktigesAndelDeltBosted`
  - `nettoBarnetilleggBM = 0`
- Uten delt bosted:
  - ordinært samværsfradrag brukes
  - BP-andel-beløp hentes fra `BPs andel av underholdskostnad`
  - netto barnetillegg BM inngår

### Beregningsformler

- `uMinusNettoBarnetilleggBM = underholdskostnad - nettoBarnetilleggBM`
- `bpAndelAvUMinusSamværsfradrag = bpAndelBeløp - samværsfradrag`
- `bidragTilFordeling = min(uMinusNettoBarnetilleggBM, bpAndelAvUMinusSamværsfradrag) + samværsfradrag`
- `nettoBidragEtterBarnetilleggBM = max(bidragTilFordeling - samværsfradrag, 0)`
- `erBidragJustertForNettoBarnetilleggBM = (uMinusNettoBarnetilleggBM == bidragTilFordeling - samværsfradrag)`

### Avslag

`erAvslag = barnetErSelvforsørget || søknadsbarnetBorHosBp`

Serviceklassen filtrerer perioder med avslag ut av delberegningsobjektet i dette steget. Selve avslag håndteres videre i sluttberegningen.

---

## Periodisering

Bruddperioder lages fra grunnlag som påvirker:

- underholdskostnad
- BP andel underholdskostnad / delt bosted-andel
- samværsfradrag
- netto barnetillegg BM
- søknadsbarnet bor hos BP
- eventuelt virkningstidspunkt

Siste periode kan åpnes (`til = null`) når `åpenSluttperiode` gjelder.

---

## Eksempel

Forutsetning (ikke delt bosted):

- underholdskostnad: 6 000
- netto barnetillegg BM: 800
- BP-andel-beløp: 3 700
- samværsfradrag: 500

Utregning:

- `uMinusNettoBarnetilleggBM = 6 000 - 800 = 5 200`
- `bpAndelAvUMinusSamværsfradrag = 3 700 - 500 = 3 200`
- `bidragTilFordeling = min(5 200, 3 200) + 500 = 3 700`
- `nettoBidragEtterBarnetilleggBM = 3 700 - 500 = 3 200`

Resultat:

- `bidragTilFordeling = 3 700,00`
- `erBidragJustertForNettoBarnetilleggBM = false`


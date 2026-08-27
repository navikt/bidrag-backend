# Beregning av sluttberegning barnebidrag V2 - forretningsregler

## Formål

Denne dokumentasjonen beskriver delberegningen `sluttberegningBarnebidrag` i `BeregnEndeligBidragServiceV2`.

Dette er siste steg som gir beregnet beløp og resultatbeløp for vedtak.

---

## Hva beregnes?

Sluttberegningen fastsetter:

- `beregnetBeløp`
- `resultatBeløp` (avrundet til nærmeste tier)
- eventuelle avslag (beløp settes til `null`)

---

## Regelsett

Beregningen utføres i `EndeligBidragBeregningV2.beregnSluttberegningBarnebidrag`.

### Avslag 1: Barnet bor hos BP

Hvis `søknadsbarnetBorHosBp = true`:

- `ikkeOmsorgForBarnet = true`
- `beregnetBeløp = null`
- `resultatBeløp = null`

### Avslag 2: Barnet er selvforsørget

Hvis `barnetErSelvforsørget = true`:

- `barnetErSelvforsørget = true`
- `beregnetBeløp = null`
- `resultatBeløp = null`

### Ordinær beregning

- `erDeltBosted = bidragspliktigesAndelDeltBosted finnes`
- `samværsfradrag = 0` ved delt bosted, ellers ordinært samværsfradrag
- `beregnetBeløp = max(bidragJustertForNettoBarnetilleggBP - samværsfradrag, 0)`
- `resultatBeløp = beregnetBeløp avrundet til nærmeste tier`

---

## Periodisering og åpen sluttperiode

Bruddperioder lages fra grunnlag som inngår i sluttformelen og avslagstestene, samt eventuelt virkningstidspunkt.

Særregel i serviceklassen for siste periode:

- normal åpen sluttperiode brukes når konfigurert
- ved avslag (`resultatBeløp = null`) kan `justertÅpenSluttperiode` settes til `true`
- dette gjelder når perioden slutter før måneden barnet fyller 18 år
- regelen brukes ikke tilsvarende for `BIDRAG18AAR`

---

## Resultat og sporbarhet

Delresultatet inneholder:

- beregnet og avrundet resultat
- avslagflagg når relevant
- grunnlagsreferanser til delberegningene som faktisk avgjorde utfallet

---

## Eksempel

Ikke avslag, ikke delt bosted:

- `bidragJustertForNettoBarnetilleggBP = 3 150`
- `samværsfradrag = 450`

Utregning:

- `beregnetBeløp = max(3 150 - 450, 0) = 2 700,00`
- `resultatBeløp = 2 700` (allerede tier)

Eksempel avslag:

- `søknadsbarnetBorHosBp = true`

Resultat:

- `beregnetBeløp = null`
- `resultatBeløp = null`


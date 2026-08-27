# Beregning av bidragspliktiges andel delt bosted - forretningsregler

## Formål

Denne dokumentasjonen beskriver delberegningen `delberegningBidragspliktigesAndelDeltBosted` i `BeregnEndeligBidragServiceV2`.

---

## Hva beregnes?

Delberegningen fastsetter BPs andel av underholdskostnad ved delt bosted.

Resultatet settes bare når grunnlaget faktisk er delt bosted.

---

## Regelsett

Beregningen utføres i `EndeligBidragBeregningV2.beregnBidragspliktigesAndelDeltBosted`:

- Hvis `deltBosted = true`:
  - `bpAndelAvUVedDeltBostedFaktor = max(bpAndelFaktor - 0,5, 0)`
  - `bpAndelAvUVedDeltBostedBeløp = underholdskostnad * bpAndelAvUVedDeltBostedFaktor`
- Hvis ikke delt bosted:
  - både faktor og beløp blir `null`

Avrunding:

- faktor: 10 desimaler
- beløp: 2 desimaler

---

## Periodisering

Bruddperioder lages fra:

- underholdskostnad
- BP andel underholdskostnad
- samværsklasse
- eventuelt virkningstidspunkt

Ved åpen sluttperiode settes siste periode til `til = null` når den treffer beregningsperiodens slutt.

---

## Viktig filtrering

Serviceklassen mapper bare ut periodesvar der faktor er satt (`!= null`).

Det betyr i praksis at perioder uten delt bosted ikke gir delberegningsobjekt av denne typen.

---

## Resultat og sporbarhet

Delresultatet inneholder blant annet:

- beregnet faktor
- beregnet beløp
- grunnlagsreferanser til underholdskostnad, BP-andel og eventuelt delt bosted-grunnlag

---

## Eksempel

Forutsetning:

- `andelFaktor = 0,70`
- `underholdskostnad = 6 000`
- `deltBosted = true`

Utregning:

- `faktor = max(0,70 - 0,50, 0) = 0,20`
- `beløp = 6 000 * 0,20 = 1 200,00`

Resultat:

- `bpAndelAvUVedDeltBostedFaktor = 0,2000000000`
- `bpAndelAvUVedDeltBostedBeløp = 1 200,00`


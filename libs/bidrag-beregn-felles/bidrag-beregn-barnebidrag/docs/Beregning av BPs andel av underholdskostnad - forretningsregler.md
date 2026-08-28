# Beregning av BPs andel av underholdskostnad - forretningsregler

## Formål

Denne dokumentasjonen beskriver forretningsreglene for beregning av bidragspliktiges andel av underholdskostnaden i `BeregnBpAndelUnderholdskostnadService`.

Dokumentasjonen dekker flyten gjennom:

- `BeregnBpAndelUnderholdskostnadService`
- `BpAndelUnderholdskostnadMapper`
- `BpAndelUnderholdskostnadBeregning`

---

## Hva beregnes?

Beregningen fastsetter hvor stor andel av barnets underholdskostnad som skal bæres av bidragspliktig.

Resultatet bygger på inntektsforholdene mellom:

- bidragspliktig (BP)
- bidragsmottaker (BM)
- søknadsbarnet (SB)

og kobles mot beregnet underholdskostnad.

---

## Overordnet ansvar i modulene

### `BpAndelUnderholdskostnadMapper`

Mapperen klargjør grunnlag per periode, inkludert:

- underholdskostnad
- inntekt for BP, BM og SB
- relevante sjablontall

### `BeregnBpAndelUnderholdskostnadService`

Tjenesten styrer beregningsløpet:

- henter sjabloner
- periodiserer grunnlaget
- beregner andel per bruddperiode
- bygger resultat og referanser

### `BpAndelUnderholdskostnadBeregning`

Utfører selve beregningen av andelsfaktor og andelsbeløp for én periode.

---

## Steg 1: Grunnlag og sjabloner

Tjenesten henter sjablontall som er relevante for beregning av BPs andel av underholdskostnad.

I tillegg hentes sjablon for innslag av kapitalinntekt, som brukes i delberegning av inntektsgrunnlag.

Mapperen kobler deretter sammen:

- delberegnet underholdskostnad
- periodiserte inntekter for BP, BM og SB
- sjablontall

---

## Steg 2: Oppdeling i perioder

Beregningen deles i bruddperioder når det skjer endringer i:

- underholdskostnad
- inntekt for BP
- inntekt for BM
- inntekt for SB
- relevante sjablontall
- eventuelt virkningstidspunkt

Dette sikrer at andelen beregnes korrekt i hver delperiode.

---

## Steg 3: Beregning per delperiode

For hver bruddperiode bygges et grunnlag med:

- underholdskostnad for barnet
- inntekt for BP
- inntekt for BM
- inntekt for SB
- sjablontall (forkuddssats)

Hvis et obligatorisk grunnlag mangler i perioden, stoppes beregningen med feil.

Når grunnlaget er komplett, brukes disse reglene i beregningen:

1. **Relevant sjablonverdi hentes**

   - `FORSKUDDSSATS_BELØP`

2. **Test om barnet er selvforsørget**

   - Barnet anses selvforsørget hvis:

     `inntekt SB >= 100 * forskuddssats`

3. **Hvis barnet er selvforsørget**

   - `beregnetAndelFaktor = 0`
   - `endeligAndelFaktor = 0`
   - `andelBeløp = 0`
   - barnets inntekt reduseres ikke videre i denne beregningen

4. **Hvis barnet ikke er selvforsørget**

   - Barnets inntekt reduseres med 30 ganger forskuddssats:

     `barnEndeligInntekt = max(inntekt SB - 30 * forskuddssats, 0)`

5. **Beregn inntektssum for fordeling**

   - `sumInntekt = inntekt BP + inntekt BM + barnEndeligInntekt`

6. **Beregn beregnet andelsfaktor**

   - Hvis `sumInntekt` er 0 settes faktor til 0
   - Ellers:

     `beregnetAndelFaktor = inntekt BP / sumInntekt`

7. **Begrens endelig andelsfaktor til maks 5/6**

   - `endeligAndelFaktor = min(beregnetAndelFaktor, 0,833333333333)`

8. **Beregn andelsbeløp**

   - `andelBeløp = underholdskostnad * endeligAndelFaktor`

9. **Avrunding av resultatfelter**

   - andelsfaktorer avrundes til 10 desimaler
   - beløp avrundes til 2 desimaler
   - barnets endelige inntekt avrundes til 2 desimaler

---

## Steg 4: Åpen sluttperiode

Hvis siste periode treffer slutten av beregningsperioden og åpen sluttperiode er aktivert, gjøres siste periode åpen (`til = null`).

---

## Delresultater og sporbarhet

Tjenesten mapper ut delberegninger for summert inntekt per rolle:

- BP
- BM
- SB

I tillegg returneres referanser til mottatte grunnlag og sjabloner som er brukt i beregningen.

Dette gjør resultatet etterprøvbart.

---

## Hva resultatet inneholder

Delberegningen per periode inneholder blant annet:

- endelig andelsfaktor
- beregnet andelsfaktor
- andelsbeløp
- barnets endelige inntekt
- flagg for om barnet er selvforsørget

---

## Kort oppsummering

`BeregnBpAndelUnderholdskostnadService` følger disse hovedreglene:

1. Underholdskostnad og inntekter for BP/BM/SB samles i periodisert grunnlag.
2. Perioder splittes ved endringer i grunnlag eller satser.
3. BPs andel beregnes separat for hver delperiode.
4. Resultatet uttrykkes som faktor og beløp.
5. Resultat og underliggende referanser returneres for sporbarhet.

---

## Eksempel

### Forutsetninger

- Beregningen gjøres for ett søknadsbarn
- Underholdskostnad er allerede delberegnet
- Inntektsgrunnlag finnes for BP, BM og SB
- Sjablon `FORSKUDDSSATS_BELØP` er tilgjengelig

### Eksempel 1 - Normal beregning (ingen takbegrensning)

#### Illustrative tall

- Underholdskostnad: 6 000
- Inntekt BP: 500 000
- Inntekt BM: 300 000
- Inntekt SB: 20 000
- Forskuddssats: 1 670

#### Trinnvis beregning

1. Selvforsørget-test:

   `100 * 1 670 = 167 000`

   `20 000 < 167 000` -> barnet er **ikke** selvforsørget

2. Reduser barnets inntekt:

   `barnEndeligInntekt = max(20 000 - 30 * 1 670, 0)`

   `barnEndeligInntekt = max(20 000 - 50 100, 0) = 0`

3. Beregn andelsfaktor:

   `sumInntekt = 500 000 + 300 000 + 0 = 800 000`

   `beregnetAndelFaktor = 500 000 / 800 000 = 0,6250`

4. Endelig andelsfaktor med tak:

   `endeligAndelFaktor = min(0,6250, 0,833333333333) = 0,6250`

5. Andelsbeløp:

   `andelBeløp = 6 000 * 0,6250 = 3 750,00`

#### Resultat

- beregnetAndelFaktor: `0,6250000000`
- endeligAndelFaktor: `0,6250000000`
- andelBeløp: `3 750,00`
- barnetErSelvforsørget: `false`

### Eksempel 2 - Tak på 5/6 slår inn

#### Illustrative tall

- Underholdskostnad: 6 000
- Inntekt BP: 900 000
- Inntekt BM: 100 000
- Inntekt SB: 0
- Forskuddssats: 1 670

#### Trinnvis beregning

1. Selvforsørget-test:

   `0 < 167 000` -> barnet er ikke selvforsørget

2. Barnets inntekt etter reduksjon:

   `barnEndeligInntekt = max(0 - 50 100, 0) = 0`

3. Beregnet andelsfaktor:

   `sumInntekt = 900 000 + 100 000 + 0 = 1 000 000`

   `beregnetAndelFaktor = 900 000 / 1 000 000 = 0,9000`

4. Takbegrensning:

   `endeligAndelFaktor = min(0,9000, 0,833333333333) = 0,833333333333`

5. Andelsbeløp:

   `andelBeløp = 6 000 * 0,833333333333 = 5 000,00`

#### Resultat

- beregnetAndelFaktor: `0,9000000000`
- endeligAndelFaktor: `0,8333333333`
- andelBeløp: `5 000,00`
- barnetErSelvforsørget: `false`

### Eksempel 3 - Barnet er selvforsørget

#### Illustrative tall

- Underholdskostnad: 6 000
- Inntekt BP: 500 000
- Inntekt BM: 300 000
- Inntekt SB: 200 000
- Forskuddssats: 1 670

#### Trinnvis beregning

1. Selvforsørget-test:

   `100 * 1 670 = 167 000`

   `200 000 >= 167 000` -> barnet er selvforsørget

2. Fordi barnet er selvforsørget settes resultatet direkte:

   `beregnetAndelFaktor = 0`

   `endeligAndelFaktor = 0`

   `andelBeløp = 0`

#### Resultat

- beregnetAndelFaktor: `0,0000000000`
- endeligAndelFaktor: `0,0000000000`
- andelBeløp: `0,00`
- barnetErSelvforsørget: `true`






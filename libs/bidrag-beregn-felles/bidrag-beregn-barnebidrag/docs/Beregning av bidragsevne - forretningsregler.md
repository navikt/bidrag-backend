# Beregning av bidragsevne - forretningsregler

## Formål

Denne dokumentasjonen beskriver forretningsreglene for beregning av bidragsevne i `BeregnBidragsevneService`.

Dokumentasjonen dekker flyten gjennom:

- `BeregnBidragsevneService`
- `BidragsevneMapper`
- `BidragsevneBeregning`

Målet er å beskrive hva som styrer resultatet og hvordan bidragsevne beregnes per periode.

---

## Hva er bidragsevne?

Bidragsevne er et uttrykk for hvor mye den bidragspliktige har økonomisk evne til å betale i bidrag, etter at inntekt, skatt og sentrale levekostnader er tatt hensyn til.

Resultatet brukes senere i barnebidragsberegningen, blant annet i fordelingsstegene.

---

## Overordnet ansvar i modulene

### `BidragsevneMapper`

Mapperen klargjør og strukturerer grunnlaget for beregningen, blant annet:

- inntektsgrunnlag for bidragspliktig
- boforhold (antall barn i husstanden og om vedkommende bor med andre voksne)
- sjabloner for bidragsevne, sjablontall og trinnvis skatt

### `BeregnBidragsevneService`

Tjenesten styrer beregningsløpet:

- henter relevante sjabloner
- deler perioden i bruddperioder
- finner riktig grunnlag per delperiode
- kaller selve beregningslogikken
- bygger opp resultat- og referansegrunnlag

### `BidragsevneBeregning`

Utfører selve beløpsberegningen for én periode.

---

## Steg 1: Hvilke grunnlag som brukes

Beregningen bygger på fire hovedtyper av data:

1. Summert inntekt for bidragspliktig
2. Boforhold i perioden
3. Sjablonverdier for bidragsevne
4. Trinnvis skattesats

I tillegg hentes sjablonverdi for innslag av kapitalinntekt, som inngår i delberegning av summert inntekt.

---

## Steg 2: Oppdeling i perioder

Beregningen periodiseres, slik at bidragsevnen kan endres når grunnlag endrer seg.

Det opprettes bruddperioder ved endringer i:

- inntekt
- boforhold
- sjablontall
- bidragsevne-sjabloner
- trinnvis skattesats
- eventuelt virkningstidspunkt

Dette sikrer at resultatet alltid følger gjeldende grunnlag og satser i hver delperiode.

---

## Steg 3: Regler for valg av bostatus

For hver delperiode avgjøres bostatus som grunnlag for riktige sjablonverdier:

- `GS` hvis bidragspliktig bor med andre voksne
- `EN` hvis bidragspliktig ikke bor med andre voksne

Bostatus brukes til å velge riktig bidragsevne-sjablon for perioden.

---

## Steg 4: Beregning per delperiode

For hver bruddperiode bygges et beregningsgrunnlag med:

- inntekt for bidragspliktig
- antall barn i husstanden
- voksne i husstanden
- sjablontall:
  - `INNSLAG_KAPITALINNTEKT` – brukt i inntektsaggregering
  - `MINSTEFRADRAG` – standardfradrag på inntekten
- relevant bidragsevne-sjablon (`BIDRAGSEVNE_EN` eller `BIDRAGSEVNE_GS` basert på bostatus)
- relevant trinnvis skattesats (`TRINNVIS_SKATT_ORDINÆR_INNTEKT`)

Deretter beregnes bidragsevne for perioden.

---

## Steg 5: Åpen sluttperiode

Hvis siste beregnede periode slutter ved slutten av beregningsperioden og åpen sluttperiode er aktivert, settes siste periode til åpen (`til = null`).

Dette gjør at resultatet kan gjelde videre til nytt grunnlag eventuelt foreligger.

---

## Delresultater og sporbarhet

Tjenesten bygger også opp delgrunnlag som brukes for etterprøvbarhet, blant annet:

- delberegning sum inntekt
- delberegning boforhold
- delberegning barn i husstand
- delberegning voksne i husstand

Resultatet inkluderer referanser til mottatte grunnlag og sjabloner som faktisk er brukt.

---

## Hva resultatet inneholder

Delberegning av bidragsevne per periode inneholder blant annet:

- beregnet bidragsevnebeløp
- skattekomponenter (minstefradrag, skatt på alminnelig inntekt, trinnskatt, trygdeavgift)
- samlet skattebelastning og skattefaktor
- underhold barn i egen husstand
- 25 prosent av inntekt (til bruk videre i kjeden)

---

## Kort oppsummering

`BeregnBidragsevneService` følger i hovedsak disse forretningsreglene:

1. Grunnlag for inntekt, boforhold og sjabloner klargjøres.
2. Beregningsperioden deles opp når relevante forhold endrer seg.
3. Bostatus (EN/GS) avgjør hvilken bidragsevne-sjablon som brukes.
4. Bidragsevne beregnes separat for hver delperiode.
5. Resultatet åpnes i siste periode ved behov.
6. Resultat og underliggende referanser returneres slik at beregningen er sporbar.

---

## Eksempel

### Forutsetninger

- Beregningsperiode: januar-juni
- Inntekt er uendret gjennom hele perioden
- Boforhold endres fra april
- Sjablontall og skatteregler er stabile

### Illustrative tall

**Periode 1 (jan-mar):** Bostatus `EN` (ikke bor med andre voksne)
- Inntekt: 600 000
- Antall barn i husstand: 2
- Minstefradrag (`MINSTEFRADRAG`): 100 000
- Skattbar inntekt: 600 000 - 100 000 = 500 000
- Trinnvis skatt (`TRINNVIS_SKATT_ORDINÆR_INNTEKT`): 103 000
- Trygdeavgift: 8 000
- Samlet skattekostnad: 111 000
- Underhold barn (sjablon `BIDRAGSEVNE_EN`): 50 000
- Bidragsevne: 600 000 - 111 000 - 50 000 = 439 000

**Periode 2 (apr-jun):** Bostatus `GS` (bor med andre voksne)
- Inntekt: 600 000 (samme)
- Antall barn i husstand: 2
- Minstefradrag: 100 000
- Skattbar inntekt: 500 000
- Trinnvis skatt: 103 000
- Trygdeavgift: 8 000
- Samlet skattekostnad: 111 000 (samme som periode 1)
- Underhold barn (sjablon `BIDRAGSEVNE_GS`): 35 000 (annet nivå pga. bostatus)
- Bidragsevne: 600 000 - 111 000 - 35 000 = 454 000

### Trinnvis beregning

1. Tjenesten identifiserer bruddpunkt i april fordi boforhold endres (EN → GS).
2. Perioden deles i to delperioder med hver sitt grunnlag.
3. For delperiode 1 velges sjablon `BIDRAGSEVNE_EN`.
4. For delperiode 2 velges sjablon `BIDRAGSEVNE_GS`.
5. Minstefradrag og trinnvis skatt beregnes per delperiode med same satser (fra `MINSTEFRADRAG` og `TRINNVIS_SKATT_ORDINÆR_INNTEKT`).
6. Underhold barn reduseres fra 50 000 til 35 000 når bostatus endrer seg.
7. Bidragsevne øker fra 439 000 til 454 000 i periode 2.

### Resultat

Selv med lik inntekt og skattebelastning øker bidragsevnen i periode 2 fordi bostatus `GS` resulterer i lavere underholdsytelse. Dette viser at bostatus styrer hvilket sjablonnivå som brukes i beregningen.






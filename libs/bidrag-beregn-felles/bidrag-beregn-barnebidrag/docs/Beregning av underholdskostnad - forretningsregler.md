# Beregning av underholdskostnad - forretningsregler

## Formål

Denne dokumentasjonen beskriver forretningsreglene for beregning av underholdskostnad i modulen for barnebidrag.

Dokumentasjonen dekker hele flyten gjennom:

- `BeregnUnderholdskostnadService`
- `UnderholdskostnadMapper`
- `UnderholdskostnadBeregning`

Målet er å beskrive **hva som skjer i beregningen**, **hvilke regler som styrer resultatet**, og **hvordan grunnlaget velges per periode**, uten å gå for langt ned i tekniske detaljer.

---

## Hva er underholdskostnad?

Underholdskostnad er et uttrykk for hva det koster å forsørge et barn i en gitt periode.

I denne løsningen bygges underholdskostnaden opp av:

- forbruksutgifter
- boutgifter
- eventuelt barnetilsyn
- eventuelt netto tilsynsutgift
- fradrag for barnetrygd

Resultatet brukes videre i den totale beregningen av barnebidrag.

---

## Overordnet ansvar i de tre modulene

### `UnderholdskostnadMapper`

Denne delen gjør om innkommende grunnlag til et format som er egnet for beregning.

Mapperen har særlig ansvar for å:

- hente ut søknadsbarnet
- hente ut perioder med barnetilsyn med stønad
- hente ut perioder med netto tilsynsutgift
- hente inn relevante sjabloner for perioden
- samle dette i ett grunnlag som senere brukes av tjenesten

Mapperen fastsetter altså ikke resultatet, men sørger for at beregningen får riktig og strukturert input.

### `BeregnUnderholdskostnadService`

Denne delen styrer selve beregningsløpet.

Tjenesten har ansvar for å:

- hente og klargjøre sjabloner
- dele opp beregningsperioden i riktige delperioder
- avgjøre hvilken type barnetrygd som gjelder i hver periode
- finne hvilket grunnlag som gjelder for hver delperiode
- kalle selve beregningslogikken
- bygge opp resultatobjekter og referanser til brukte grunnlag

### `UnderholdskostnadBeregning`

Denne delen utfører selve beløpsberegningen for én periode.

Her kombineres de konkrete beløpene og sjablonene til ett resultat for underholdskostnad.

---

## Steg 1: Klargjøring av grunnlag

Før beregningen kan starte, må løsningen finne frem til hvilke opplysninger som er relevante.

### Søknadsbarn

Mapperen finner søknadsbarnet og bruker fødselsdatoen til barnet som utgangspunkt for videre aldersvurdering.

Fødselsdatoen er viktig fordi den påvirker:

- hvilken sjablon for forbruksutgifter som skal brukes
- når barnet regnes å fylle 6 år
- hvilken type barnetrygd som gjelder i perioden

### Barnetilsyn med stønad

Mapperen henter inn perioder med barnetilsyn med stønad dersom slike finnes.

Disse opplysningene brukes senere for å avgjøre:

- om barnetilsyn skal inngå i beregningen
- hvilken type tilsyn som gjelder
- hvilken sjablon for barnetilsyn som eventuelt er relevant

### Netto tilsynsutgift

Mapperen henter også inn delberegnet netto tilsynsutgift for det aktuelle barnet.

Kun netto tilsynsutgift som gjelder søknadsbarnet tas med videre.

### Sjabloner

Tjenesten henter inn sjabloner for:

- sjablontall
- barnetilsyn
- forbruksutgifter

For underholdskostnad hentes følgende konkrete **sjablontall**:

- boutgifter for bidragsbarn
- ordinær barnetrygd
- forhøyet barnetrygd

I tillegg hentes egne sjabloner for **barnetilsyn** og **forbruksutgifter**. Disse er ikke sjablontall, men egne sjablontyper som brukes sammen med sjablontallene i beregningen.

---

## Steg 2: Oppdeling i beregningsperioder

Underholdskostnad beregnes ikke nødvendigvis som ett beløp for hele perioden. Beregningen deles opp i flere delperioder når det skjer endringer som kan påvirke resultatet.

Det opprettes bruddperioder når det skjer endringer i:

- barnetilsyn med stønad
- netto tilsynsutgift
- sjablontall
- sjablon for barnetilsyn
- sjablon for forbruksutgifter
- virkningstidspunkt, dersom dette er oppgitt

I tillegg legges det inn egne bruddpunkter knyttet til barnetrygdreglene:

- barnets fødselsmåned
- tidspunktet barnet regnes å fylle 6 år
- juli 2021, da reglene for forhøyet barnetrygd ble innført

Formålet med denne oppdelingen er å sikre at hver delperiode beregnes med riktig regelsett og riktige satser.

---

## Steg 3: Hvordan grunnlaget velges for hver delperiode

Når en delperiode er identifisert, finner tjenesten hvilket grunnlag som gjelder akkurat i den perioden.

### Barnets alder

For hver delperiode beregnes barnets alder.

Det brukes en fast forretningsregel der barnet **regnes som født 1. juli i fødselsåret**. Dette gir en standardisert aldersvurdering som brukes ved valg av sjablon for forbruksutgifter.

Deretter velges den aldersgruppen som passer for barnets beregnede alder.

### Sjablon for forbruksutgifter

Når alder er bestemt, velges sjablonen for forbruksutgifter som dekker denne alderen.

Forbruksutgifter inngår alltid i beregningen.

### Barnetilsyn

Hvis det finnes barnetilsyn med stønad i perioden, brukes dette til å finne riktig tilsynskode.

Denne koden brukes deretter for å finne relevant sjablon for barnetilsyn.

Det betyr at barnetilsynsdelen av beregningen først blir aktuell når det finnes grunnlag som viser at barnetilsyn faktisk er relevant i den aktuelle perioden.

### Netto tilsynsutgift

Hvis det finnes netto tilsynsutgift for perioden, tas denne med i beregningsgrunnlaget for samme delperiode.

---

## Steg 4: Regler for barnetrygd

For hver delperiode fastsettes hvilken type barnetrygd som skal trekkes fra.

Det skilles mellom:

- `INGEN`
- `ORDINÆR`
- `FORHØYET`

### Når det ikke trekkes barnetrygd

Det trekkes ikke barnetrygd når:

- stønadstypen er `BIDRAG18AAR`
- delperioden starter i barnets fødselsmåned

### Når det trekkes ordinær barnetrygd

Det trekkes ordinær barnetrygd når:

- delperioden er før juli 2021
- eller barnet er kommet forbi perioden der forhøyet barnetrygd gjelder

### Når det trekkes forhøyet barnetrygd

Det trekkes forhøyet barnetrygd når:

- perioden er fra og med juli 2021
- og barnet fortsatt er før måneden det regnes å fylle 6 år

Kort oppsummert:

- før juli 2021 brukes ordinær barnetrygd
- fra juli 2021 og frem til 6-årsgrensen brukes forhøyet barnetrygd
- etter dette brukes ordinær barnetrygd igjen
- i enkelte særtilfeller brukes ingen barnetrygd

---

## Steg 5: Selve beregningen av underholdskostnad

Når riktig grunnlag for perioden er valgt, sendes det videre til `UnderholdskostnadBeregning`.

Her beregnes underholdskostnaden etter denne hovedregelen:

**forbruksutgifter + boutgifter + barnetilsyn + netto tilsynsutgift - barnetrygd**

Dette betyr:

1. forbruksutgifter legges til
2. boutgifter legges til
3. barnetilsyn legges til hvis det finnes
4. netto tilsynsutgift legges til hvis den finnes
5. barnetrygd trekkes fra

Hvis resultatet blir negativt, settes det til **0**.

Underholdskostnaden kan altså aldri bli mindre enn null.

---

## Eksempler på beregning av underholdskostnad

Eksemplene under bruker **illustrative månedsbeløp** for å vise hvordan reglene virker i praksis.
Tallene er ment som forklaring av beregningsreglene, ikke som fasit for faktiske satser.

### Eksempel 1: Ordinær barnetrygd

Et barn er i en periode der det skal trekkes **ordinær barnetrygd** (fra sjablontall `ORDINÆR_BARNETRYGD`).

- forbruksutgifter (fra aldersbasert sjablon): 4 000
- boutgifter (fra `BOUTGIFT_BIDRAGSBARN`): 1 200
- barnetilsyn (fra barnetilsynssjablon eller konkret beløp): 800
- netto tilsynsutgift (fra delberegning): 500
- ordinær barnetrygd (fra `ORDINÆR_BARNETRYGD`): 1 300

Utregning:

`4 000 + 1 200 + 800 + 500 - 1 300 = 5 200`

Underholdskostnaden blir da **5 200**.

Dette eksemplet viser hovedregelen når ordinær barnetrygd skal trekkes fra summen av kostnadselementene.

### Eksempel 2: Forhøyet barnetrygd

Et barn er i en periode der det skal trekkes **forhøyet barnetrygd** (fra sjablontall `FORHØYET_BARNETRYGD`), for eksempel i perioden etter juli 2021 og før barnet regnes å fylle 6 år.

- forbruksutgifter (fra aldersbasert sjablon): 4 000
- boutgifter (fra `BOUTGIFT_BIDRAGSBARN`): 1 200
- barnetilsyn (fra barnetilsynssjablon eller konkret beløp): 800
- netto tilsynsutgift (fra delberegning): 500
- forhøyet barnetrygd (fra `FORHØYET_BARNETRYGD`): 1 700

Utregning:

`4 000 + 1 200 + 800 + 500 - 1 700 = 4 800`

Underholdskostnaden blir da **4 800**.

Dette eksemplet viser at høyere barnetrygd (forhøyet) gir et større fradrag og dermed en lavere underholdskostnad enn i et tilsvarende tilfelle med ordinær barnetrygd. Endringen skyldes øking fra `ORDINÆR_BARNETRYGD` til `FORHØYET_BARNETRYGD` i juli 2021.

### Eksempel 3: Resultatet blir satt til 0

I noen tilfeller er barnetrygden og de øvrige fradragene så store i forhold til kostnadene at resultatet ellers ville blitt negativt.

- forbruksutgifter: 900
- boutgifter: 300
- barnetilsyn: 0
- netto tilsynsutgift: 0
- barnetrygd: 1 500

Utregning:

`900 + 300 + 0 + 0 - 1 500 = -300`

Fordi underholdskostnaden aldri kan være negativ, settes resultatet til **0**.

Dette eksemplet viser "gulvregelen" i beregningen: selv om utregningen gir et negativt tall, returneres 0.

---

## Særregel for barnetilsyn i beregningen

I selve beregningen gjelder et viktig prioriteringsprinsipp:

- hvis det finnes et konkret beløp for barnetilsyn, brukes dette
- hvis det ikke finnes et konkret beløp, brukes sjablon for barnetilsyn når slik sjablon er relevant

Dette innebærer at konkret grunnlag går foran sjablon.

---

## Hvilke beløp som inngår i resultatet

Resultatet for hver periode viser normalt disse delene:

- forbruksutgift
- boutgift
- barnetilsyn med stønad
- netto tilsynsutgift
- barnetrygd
- samlet underholdskostnad

På denne måten blir det synlig både **hvordan resultatet er satt sammen** og **hva som ble endelig underholdskostnad i perioden**.

---

## Sporbarhet og referanser

Løsningen tar også vare på referanser til grunnlagene som faktisk er brukt i beregningen.

Det betyr at resultatet kan spores tilbake til:

- mottatte grunnlagsdata
- relevante sjabloner
- delberegninger som inngår, for eksempel netto tilsynsutgift

Dette er viktig både for etterprøvbarhet og for å kunne forklare resultatet i ettertid.

---

## Åpen sluttperiode

Hvis siste beregnede delperiode slutter samtidig med slutten på beregningsperioden, og tjenesten er kalt med åpen sluttperiode, gjøres siste periode åpen.

Det betyr at siste resultat ikke får en fast sluttdato, men gjelder videre fremover inntil nye forhold fører til en ny beregning.

---

## Sammenslåing av like perioder

Etter at alle delperioder er beregnet, slås perioder sammen når de i praksis representerer samme resultat og samme grunnlagsbruk.

Målet er å unngå unødvendig oppdeling og gi et mer forståelig sluttresultat.

---

## Kort oppsummering av forretningsreglene

Beregning av underholdskostnad følger i hovedsak disse reglene:

1. Innkommende grunnlag og sjabloner klargjøres først.
2. Beregningsperioden deles opp når grunnlag eller regelsett endrer seg.
3. Barnets alder brukes for å velge riktig sjablon for forbruksutgifter.
4. Barnetilsyn inngår når det finnes relevant grunnlag, og konkret beløp går foran sjablon.
5. Netto tilsynsutgift inngår når den finnes for barnet i perioden.
6. Barnetrygd trekkes fra etter reglene for ingen, ordinær eller forhøyet barnetrygd.
7. Underholdskostnaden kan aldri bli negativ.
8. Siste periode kan være åpen dersom beregningen skal løpe videre.
9. Like perioder slås sammen for å gi et ryddigere resultat.

---

## Oppsummert rollefordeling

For å forstå helheten kan de tre modulene oppsummeres slik:

- `UnderholdskostnadMapper` finner og strukturerer riktig grunnlag
- `BeregnUnderholdskostnadService` styrer perioder, regelvalg og resultatoppbygging
- `UnderholdskostnadBeregning` utfører selve beløpsberegningen for hver periode

Sammen utgjør disse hele beregningsløpet for underholdskostnad.




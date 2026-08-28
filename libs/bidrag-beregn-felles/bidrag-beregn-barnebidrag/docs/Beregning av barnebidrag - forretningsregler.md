# Beregning av barnebidrag - forretningsregler

## Formål

Denne dokumentasjonen beskriver forretningsreglene for beregning av barnebidrag i `BeregnBarnebidragService`.

Dokumentasjonen dekker flyten gjennom:

- `BeregnBarnebidragService.beregnBarnebidragV2`

For orkestrering på tvers av flere barn og vedtakstyper, se også:

- `bidrag-beregn-barnebidrag/docs/Orkestrering av bidragsberegning - forretningsregler.md`

Målet er å beskrive **hva som skjer i beregningen**, **hvilke regler som styrer resultatet**, og **hvordan ulike særtilfeller håndteres**, uten å gå i detalj på hva som skjer internt i de enkelte delberegningstjenestene.

---

## Hva er barnebidrag?

Barnebidrag er det beløpet en bidragspliktig forelder skal betale til den andre forelderen for å dekke barnets løpende kostnader.

Beregningen tar utgangspunkt i:

- bidragspliktiges betalingsevne (bidragsevne)
- hva det faktisk koster å forsørge barnet (underholdskostnad)
- bidragspliktiges andel av denne kostnaden
- fradrag for samvær med barnet
- eventuelle barnetillegg fra pensjon eller liknende

Resultatet brukes som grunnlag for å fatte vedtak om bidragsbeløp per periode.

---

## Overordnet beregningsflyt

Beregningen gjennomføres i følgende rekkefølge:

1. Hvert søknadsbarn klargjøres og valideres
2. Standard delberegninger kjøres for hvert søknadsbarn
3. Løpende bidrag og private avtaler berikes med oppdaterte delberegninger
4. Endelig bidrag beregnes samlet for alle barn
5. Særtilfeller ved evnesprekk vurderes og kan stoppe beregningen
6. Beregnet bidrag sjekkes mot minimumsgrense for endring
7. Perioder justeres og filtreres for vedtaksfatting

---

## Steg 1: Klargjøring og validering per søknadsbarn

Før delberegningene starter, gjøres en rekke forberedelser og kontroller for hvert søknadsbarn.

### Avslag

Hvis virkningstidspunktet angir et avslag, returneres resultatet direkte uten at noen beregning gjennomføres. Resultatet inneholder én periode uten beløp, og beregningslogikken hoppes over.

### Validering av inputdata

Innkommende grunnlag valideres for å avdekke mangler eller inkonsistenser som ville gitt feil resultat.

### Aldersjustering ved fylte 18 år

Hvis barnet fyller 18 år i løpet av beregningsperioden, justeres perioden slik at beregningen avsluttes den måneden barnet fyller 18. Det beregnes ikke barnebidrag for perioder etter at barnet er blitt myndig.

### Opprinnelig behandling eller revurdering

Det sjekkes om barnet er en del av den opprinnelige behandlingen, eller om det er lagt til i en revurderingssøknad. Dette påvirker senere om det skal fattes vedtak for barnet.

### Virkning-fra-periode

Virkningstidspunktet kan ligge senere enn starten på beregningsperioden. Hvis det er tilfellet, legges dette inn som et eget bruddpunkt, slik at perioden før virkningstidspunktet ikke får et vedtaksresultat.

---

## Steg 2: Delberegninger for hvert søknadsbarn

Etter klargjøring kjøres seks standard delberegninger for hvert søknadsbarn. Disse produserer hver sitt periodiserte resultat som brukes videre i den endelige beregningen.

### Bidragsevne

Beregner bidragspliktiges betalingsevne basert på inntekt og boutgifter. Resultatet uttrykker hvor mye den bidragspliktige har kapasitet til å betale.

### Netto tilsynsutgift

Beregner netto kostnad for barnetilsyn som bidragsmottaker faktisk bærer, etter fradrag for offentlig støtte. Inngår i underholdskostnaden.

### Underholdskostnad

Beregner hva det koster å forsørge barnet, basert på forbruksutgifter, boutgifter, tilsynsutgifter og barnetrygd. Se egen dokumentasjon for detaljerte regler.

### BPs andel av underholdskostnad

Beregner hvor stor del av underholdskostnaden som skal dekkes av den bidragspliktige, basert på inntektsfordelingen mellom foreldrene.

### Samværsfradrag

Beregner fradraget som den bidragspliktige har krav på basert på avtalt samvær med barnet. Høyere samvær gir høyere fradrag.

### Netto barnetillegg

Beregner eventuelt barnetillegg fra pensjon eller liknende ytelser for den bidragspliktige og bidragsmottakeren, etter skatt. Barnetillegg kan påvirke det endelige bidragsbeløpet.

---

## Steg 3: Løpende bidrag og private avtaler

Hvis det finnes løpende bidragssaker eller private avtaler, berikes disse med oppdaterte delberegninger.

### Løpende bidrag

For hvert løpende bidrag beregnes samværsfradraget på nytt. Dette er nødvendig fordi samværsavtalen kan ha endret seg siden forrige vedtak.

### Privat avtale

For private avtaler gjøres to ting:

- Avtalebeløpet indeksreguleres til gjeldende prisnivå
- Samværsfradraget beregnes på nytt

---

## Steg 4: Endelig bidragsberegning

Når alle delberegninger er klare, gjennomføres den endelige beregningen for alle søknadsbarn samlet.

Den endelige beregningen tar hensyn til:

- bidragsevnen opp mot summen av alle bidragsforpliktelser (inkludert løpende bidrag og private avtaler)
- fordelingen av bidragsevnen mellom alle barna når evnen ikke strekker til
- justering for barnetillegg fra pensjon
- fastsettelse av endelig bidragsbeløp per barn per periode

Et sentralt hensyn i dette steget er at bidragsevnen er én felles ressurs som fordeles mellom alle barn den bidragspliktige har forpliktelser overfor. Bidragsberegningen for ett barn kan derfor ikke gjøres isolert fra de andre.

---

## Steg 5: Særtilfeller ved evnesprekk

Evnesprekk oppstår når den bidragspliktiges betalingsevne ikke er tilstrekkelig til å dekke alle bidragsforpliktelsene fullt ut.

Hvis det oppdages evnesprekk, gjøres to kontroller som kan stanse beregningen.

### Evnesprekk og ufullstendige grunnlag

Hvis det finnes løpende bidrag eller private avtaler med norske bidrag, og grunnlagene for disse ikke er fullstendige, er det ikke mulig å fordele evnen riktig mellom alle barn. Beregningen avbrytes med en feil som signaliserer at fullstendige grunnlag for alle saker må hentes inn før vedtak kan fattes.

Dette er en sikkerhetsregel som sikrer at ingen barn får feil beregning som følge av manglende informasjon.

### Evnesprekk og oppfostringsbidrag

Hvis det finnes et oppfostringsbidrag blant de løpende bidragene og det samtidig er evnesprekk, kan ikke systemet håndtere dette automatisk. Beregningen avbrytes med en feil som krever manuell behandling av saksbehandler.

Begge tilfellene returnerer beregningsresultater sammen med feilen, slik at den som kaller beregningen kan presentere kontekst rundt problemet.

---

## Steg 6: Minimumsgrense for endring (12%-regelen)

Etter at endelig bidrag er beregnet, sjekkes det om endringen fra gjeldende vedtak er stor nok til å begrunne et nytt vedtak.

Regelen innebærer at hvis det beregnede beløpet avviker fra gjeldende beløp med mindre enn en viss prosentsats, brukes gjeldende beløp istedenfor det beregnede. Formålet er å unngå hyppige, små justeringer.

Resultatet av denne sjekken kan føre til at beregnet beløp erstattes av historisk beløp i én eller flere perioder.

---

## Steg 7: Vedtakslogikk og periodefiltrering

Etter at alle beregninger er ferdige, filtreres og justeres periodene for hvert søknadsbarn.

### Hvem får vedtak

Det fattes vedtak for et søknadsbarn hvis:

- barnet er del av den opprinnelige behandlingen, **eller**
- barnet er lagt til i en revurderingssøknad, **og** det finnes perioder med evnesprekk som overlapper virkningstidspunktet

Hvis det ikke skal fattes vedtak for et barn, returneres en tom periodeliste. Alle grunnlag tas likevel med i responsen for sporbarhet.

### Virkning-fra-periode

Hvis virkningstidspunktet er senere enn starten på beregningsperioden, returneres tomme perioder for perioden før virkningstidspunktet. Vedtak fattes kun fra og med virkningstidspunktet.

### Opphørsdato

Hvis saken har en opphørsdato, justeres siste periode slik at den avsluttes på opphørsdatoen. Det beregnes ikke bidrag etter dette tidspunktet.

---

## Sporbarhet og referanser

Resultatet inneholder referanser til alle grunnlag som faktisk er brukt i beregningen.

Dette gjør det mulig å spore:

- hvilke inndata som lå til grunn
- hvilke delberegninger som ble gjennomført
- hvilke sjabloner og satser som gjaldt i den aktuelle perioden

Referansene er viktige for å kunne etterprøve og forklare resultatet i ettertid.

---

## Kort oppsummering av forretningsreglene

Beregning av barnebidrag følger i hovedsak disse reglene:

1. Avslag håndteres direkte uten beregning.
2. Barnet justeres ut av beregningen fra og med måneden det fyller 18 år.
3. Alle standard delberegninger gjennomføres for hvert søknadsbarn.
4. Løpende bidrag og private avtaler berikes med oppdaterte beregninger.
5. Endelig bidrag beregnes samlet for alle barn, med én felles bidragsevne som fordeles.
6. Evnesprekk kombinert med ufullstendige grunnlag eller oppfostringsbidrag stopper beregningen.
7. Beregnet beløp erstattes av historisk beløp hvis endringen er under minimumsgrensen.
8. Vedtak fattes kun for relevante barn og kun fra og med virkningstidspunktet.
9. Alle grunnlag bevares i resultatet for sporbarhet, selv når det ikke fattes vedtak.

---

## Komplette regneeksempler

Eksemplene under er illustrative, men følger beregningsrekkefolgen i `beregnBarnebidragV2` og viser hvordan delresultater henger sammen fra input til vedtaksgrunnlag.

---

### Case 1: Komplett kjede med søknadsbarn, løpende bidrag og privat avtale

Dette caset viser hele flyten med alle sentrale delberegninger.

#### Grunnlag (illustrativt)

- Ett søknadsbarn
- Løpende bidrag finnes
- Privat avtale finnes
- Ikke delt bosted
- Barnet er ikke selvforsørget
- Barnet bor ikke hos BP

### Steg A: Standard delberegninger for søknadsbarn

#### 1) Bidragsevne

- Beregnet bidragsevne: `4 800,00`
- 25 prosent av inntekt (`sumInntekt25Prosent`): `5 000,00`

#### 2) Netto tilsynsutgift

- Faktisk tilsynsutgift: `2 000,00`
- Fradrag/støtte/skattevirkning (samlet): `1 400,00`
- Netto tilsynsutgift: `600,00`

#### 3) Underholdskostnad

- Forbruksutgift: `4 000,00`
- Boutgift: `1 200,00`
- Barnetilsyn: `800,00`
- Netto tilsynsutgift: `600,00`
- Barnetrygd: `1 300,00`

Utregning:

`4 000 + 1 200 + 800 + 600 - 1 300 = 5 300,00`

Underholdskostnad: `5 300,00`

#### 4) BPs andel av underholdskostnad

- Endelig andelsfaktor: `0,6250000000`

Utregning:

`5 300,00 * 0,6250000000 = 3 312,50`

Andelsbeløp: `3 312,50`

#### 5) Samværsfradrag (søknadsbarn)

- Samværsfradrag: `500,00`

#### 6) Netto barnetillegg

- Netto barnetillegg BM: `700,00`
- Netto barnetillegg BP: `1 000,00`

---

### Steg B: Beriking av løpende bidrag og privat avtale

#### 7) Bidrag til fordeling løpende bidrag

- Løpende beløp: `3 000,00`
- Beregnet beløp: `3 500,00`
- Faktisk beløp: `3 200,00`
- Samværsfradrag: `400,00`

Utregning:

- `reduksjonUnderholdskostnad = max(3 500 - 3 200, 0) = 300,00`
- `bidragTilFordeling = 3 000 + 400 + 300 = 3 700,00`

Resultat: `3 700,00`

#### 8) Bidrag til fordeling privat avtale

- Indeksregulert avtale: `2 500,00`
- Samværsfradrag: `300,00`

Utregning:

`2 500 + 300 = 2 800,00`

Resultat: `2 800,00`

---

### Steg C: Endelig bidrag V2-kjeden

#### 9) Bidragspliktiges andel ved delt bosted

- Ikke delt bosted i dette caset
- Delberegningen gir derfor ikke styrende beløp i videre utregning

#### 10) Bidrag til fordeling (søknadsbarn)

- Underholdskostnad: `5 300,00`
- Netto barnetillegg BM: `700,00`
- BP-andel-beløp: `3 312,50`
- Samværsfradrag: `500,00`

Utregning:

- `uMinusNettoBarnetilleggBM = 5 300,00 - 700,00 = 4 600,00`
- `bpAndelAvUMinusSamværsfradrag = 3 312,50 - 500,00 = 2 812,50`
- `bidragTilFordeling = min(4 600,00, 2 812,50) + 500,00 = 3 312,50`
- `nettoBidragEtterBarnetilleggBM = max(3 312,50 - 500,00, 0) = 2 812,50`

Resultat søknadsbarn: `3 312,50`

#### 11) Sum bidrag til fordeling

- Søknadsbarn: `3 312,50`
- Løpende bidrag: `3 700,00`
- Privat avtale: `2 800,00`

Utregning:

`sumBidragTilFordeling = 3 312,50 + 3 700,00 + 2 800,00 = 9 812,50`

Antatt i caset:

- `sumPrioriterteBidragTilFordeling = 0,00`

#### 12) Evne 25 prosent av inntekt

Utregning:

`evneJustertFor25ProsentAvInntekt = min(4 800,00, 5 000,00) = 4 800,00`

#### 13) Andel av bidragsevne

- `bidragTilFordeling` (søknadsbarn): `3 312,50`
- `sumBidragTilFordeling`: `9 812,50`
- `sumPrioriterteBidragTilFordeling`: `0,00`
- `evneJustertFor25ProsentAvInntekt`: `4 800,00`

Utregning:

- `sumBidragTilFordelingJustertForPrioriterteBidrag = 9 812,50 - 0,00 = 9 812,50`
- `andelAvSumBidragTilFordelingFaktor = 3 312,50 / 9 812,50 = 0,3375796178`
- `evneJustertForPrioriterteBidrag = max(4 800,00 - 0,00, 0) = 4 800,00`
- `andelAvEvneBeløp = 4 800,00 * 0,3375796178 = 1 620,38`
- `bidragEtterFordeling = min(3 312,50, 1 620,38) = 1 620,38`

#### 14) Bidrag justert for BP barnetillegg

- `bidragEtterFordeling = 1 620,38`
- `nettoBarnetilleggBP = 1 000,00`
- Ikke delt bosted

Utregning:

`bidragJustertForNettoBarnetilleggBP = max(1 620,38, 1 000,00) = 1 620,38`

#### 15) Sluttberegning barnebidrag V2

- `bidragJustertForNettoBarnetilleggBP = 1 620,38`
- `samværsfradrag = 500,00`

Utregning:

- `beregnetBeløp = max(1 620,38 - 500,00, 0) = 1 120,38`
- `resultatBeløp = avrundet til nærmeste tier = 1 120`

Foreløpig nytt bidrag: `1 120`

---

### Steg D: Endring-sjekk (periode + samlet)

#### 16) Endring sjekk grense periode

- Løpende nivå i vedtak: `1 000`
- Nytt beregnet nivå: `1 120`

Utregning:

- Endring: `120`
- Endringsprosent: `120 / 1 000 = 12 %`

Resultat i perioden: over/lik grense -> nytt nivå kan legges til grunn.

#### 17) Endring sjekk grense (samlet)

Hvis minst én relevant periode er over grensen, blir samlet resultat `endringErOverGrense = true`.

I dette caset: `true`.

---

### Case 2: Barnet er selvforsørget (avslag)

Dette caset viser hvordan kjeden stopper i realiteten selv om delberegninger finnes.

- I delberegning `BPs andel av underholdskostnad` settes:
  - `barnetErSelvforsørget = true`
  - andelsbeløp/faktor til `0`
- I `bidrag til fordeling` settes `erAvslag = true`
- I `sluttberegning barnebidrag V2` gir dette:
  - `beregnetBeløp = null`
  - `resultatBeløp = null`

Konsekvens: vedtak får avslag i perioden.

---

### Case 3: Endring under minimumsgrense (12%-regelen)

Dette caset viser at ny beregning kan bli overstyrt av gjeldende vedtak.

- Beregnet `resultatBeløp`: `3 080`
- Gjeldende beløp: `3 000`

Utregning:

`(3 080 - 3 000) / 3 000 = 2,67 %`

Siden endringen er under 12 %, brukes gjeldende beløp videre i perioden.

---

### Praktisk tolkning av eksemplene

- Case 1 viser full kjede med fordeling av begrenset evne på tvers av flere forpliktelser.
- Case 2 viser avslagsspor når barnet er selvforsørget.
- Case 3 viser at vedtak kan bli stående selv om ny beregning finnes, fordi endringen er for liten.

# Orkestrering av bidragsberegning - forretningsregler

## Formål

Denne dokumentasjonen beskriver forretningsreglene for orkestrering i `BidragsberegningOrkestrator`.

Dokumentasjonen dekker flyten gjennom:

- `utførBidragsberegningV3`
- `orkestrerBeregning`
- `utførBeregningOgFeilhåndtering`

Målet er å beskrive **hvordan beregningen styres på tvers av barn og vedtakstyper**, **når beregningen kjøres i flere runder**, og **hvordan feil, avvisning og grunnlag håndteres**, uten å gå i detalj på intern logikk i hver delberegning.

---

## Hva er orkestrering i bidragsberegning?

Orkestrering betyr å styre rekkefølge, avgrensning og sammenstilling av beregninger for ett eller flere søknadsbarn.

Orkestratoren skal blant annet:

- velge flyt basert på `beregningstype` (`BIDRAG`, `OMGJØRING`, `OMGJØRING_ENDELIG`)
- håndtere direkte avslag
- avgjøre om beregning må kjøres i én eller to runder ved revurderingsbarn
- sørge for riktig responsformat per barn
- ivareta sporbarhet i grunnlagsreferanser, også når beregning kjøres i flere runder

---

## Overordnet orkestreringsflyt

Orkestreringen følger i hovedsak denne rekkefølgen:

1. Velg flyt ut fra `beregningstype`
2. Hvis beregningstype er `BIDRAG`, håndter eventuelt direkte avslag
3. Vurder om det finnes revurderingsbarn
4. Kjør beregning i runde 1 / 2A, og eventuelt 2B ved evnesprekk
5. Håndter avviste revurderingsbarn
6. Bygg vedtaksresultat per barn
7. Juster og slå sammen grunnlag for sporbarhet
8. Konverter exception til forretningsrespons eller teknisk feilrespons

---

## Steg 1: Valg av hovedflyt per beregningstype

`utførBidragsberegningV3` styrer tre hovedløp.

### BIDRAG

- Ved `erDirekteAvslag = true` bygges avslag per barn uten ordinær beregning.
- Ellers brukes `orkestrerBeregning`.
- Resultatet returneres som vedtakstype `ENDRING`.

### OMGJØRING

- Beregning kjøres via `orkestrerBeregning(..., beregnForOmgjøring = true)`.
- Resultatet returneres som vedtakstype `KLAGE` med `omgjøringsvedtak = true`.

### OMGJØRING_ENDELIG

- Først kjøres omgjøringsberegning (`orkestrerBeregning(..., true)`).
- Deretter kjøres endelig omgjøring per barn via `OmgjøringOrkestratorV2`.
- Grunnlag fra flere trinn slås sammen og justeres før respons returneres.

---

## Steg 2: Direkte avslag

Ved direkte avslag brukes `utforBeregningDirekteAvslag`.

For hvert søknadsbarn:

- det opprettes `BeregnGrunnlag`
- sluttperiode settes til åpen (`til = null`)
- `opprettAvslag` kalles
- responsen får én vedtaksliste med vedtakstype basert på hovedflyten

Hvis et barn feiler i avslagssporet, returneres tom periodeliste og feilen legges på det barnet.

---

## Steg 3: Rundehåndtering for revurderingsbarn (1, 2A, 2B)

`orkestrerBeregning` vurderer om alle søknadsbarn er del av opprinnelig behandling.

### Runde 1 (ingen revurderingsbarn)

- Hele requesten beregnes samlet i én runde.
- Resultat mappes direkte til orkestrert respons.

### Runde 2A (revurderingsbarn finnes)

- Revurderingsbarn filtreres midlertidig bort.
- Beregning kjøres for barn som er del av opprinnelig behandling.
- Revurderingsbarn kan markeres som avvist hvis de ikke inngår i resultatet.

### Runde 2B (evnesprekk med ufullstendige grunnlag)

Hvis runde 2A gir `IkkeFullBidragsevneOgUfullstendigeGrunnlagException`:

- beregning kjøres på nytt med nye grunnlag (inkludert revurderingsbarn)
- løpende bidrag som overlapper med nye grunnlag filtreres bort
- relevante delberegninger fra runde 2A gis unike referanser (`_2A`-postfix)
- sluttberegninger i ny runde kobles mot delberegning `sum bidrag til fordeling` fra runde 2A

Dette sikrer at sporbarheten i sluttresultatet peker til riktig beregningsgrunnlag når runder kombineres.

---

## Steg 4: Håndtering av avviste revurderingsbarn

Når beregningen ikke inneholder revurderingsbarn i resultatet:

- revurderingsbarn legges til med tomt beregningsresultat
- `avvistRevurderingsbarn = true`
- persongrunnlag for disse barna legges inn i grunnlagslisten

Dette gjør at konsument får eksplisitt resultat for alle barn i requesten, også de som ikke ble realitetsberegnet.

---

## Steg 5: Felles beregningskall og beriking av grunnlag

`utførBeregningOgFeilhåndtering` samler input på tvers av barn før kall mot `barnebidragApi.beregnV2`.

Før kall:

- løpende bidrag hentes ved behov (`skalHensyntaLøpendeBidrag`)
- total beregningsperiode beregnes fra alle søknadsbarn
- privat avtale-grunnlag skilles ut i egen liste
- valutakursgrunnlag bygges

Etter kall:

- resultater mappes per søknadsbarn
- særregel for direkte avslag/opphør i input håndteres ved tom periodeliste med åpen sluttperiode

---

## Steg 6: Feilhåndtering og forretningsmessige stoppregler

To forretningsfeil behandles eksplisitt og kastes videre i orkestrert format:

- evnesprekk kombinert med oppfostringsbidrag
- evnesprekk kombinert med ufullstendige grunnlag

Begge returnerer data som kan presenteres i klient med beregningskontekst.

Andre feil:

- i orkestreringsløp mappes til barnespesifikk feilrespons når mulig
- i felles beregningskall mappes til `BidragsberegningFeiletTekniskException`

---

## Steg 7: Omgjøring endelig og sammenslåing av grunnlag

I `OMGJØRING_ENDELIG`:

- hvert barn med gyldig mellomresultat sendes til `utførOmgjøringEndelig`
- delvedtak og omgjøringsvedtak mappes til `ResultatVedtakV2`
- grunnlag for ikke-delvedtak og ikke-omgjøringsvedtak samles
- grunnlag fra runde 2A med `_2A`-referanser kobles på sluttberegningene

Resultatet blir én samlet grunnlagsliste uten dupliserte referanser.

---

## Sporbarhet og referanser

Orkestratoren er ansvarlig for at grunnlagsreferanser fortsatt er etterprøvbare når:

- flere barn beregnes samlet
- beregning må kjøres i flere runder
- delberegninger fra tidligere runde må gjenbrukes i senere runde

Dette ivaretas gjennom:

- deduplisering på `referanse`
- postfix (`_2A`) for å skille runde 2A fra senere runde
- etterkobling av delberegning-referanser på sluttberegninger

---

## Kort oppsummering av forretningsreglene

Orkestrering av bidragsberegning følger i hovedsak disse reglene:

1. Beregningstype styrer hvilket vedtaksløp som brukes.
2. Direkte avslag håndteres separat per barn med åpen sluttperiode.
3. Revurderingsbarn kan utløse beregning i to runder (2A og 2B).
4. Evnesprekk med ufullstendige grunnlag eller oppfostringsbidrag stopper automatisk vedtaksløp.
5. Avviste revurderingsbarn returneres eksplisitt i resultatet.
6. Omgjøring endelig bygger vedtak per barn fra mellomresultater.
7. Grunnlag fra flere runder slås sammen med referansejustering for sporbarhet.
8. Responsen inneholder både resultat per barn og samlet relevant grunnlag.

---

## Komplette orkestreringseksempler

Eksemplene under er illustrative og følger flyten i `utførBidragsberegningV3`.

---

### Case 1: BIDRAG uten revurderingsbarn (runde 1)

#### Grunnlag (illustrativt)

- To søknadsbarn
- Begge `delAvOpprinneligBehandling = true`
- Ikke direkte avslag

#### Flyt

1. `beregningstype = BIDRAG` velges.
2. `orkestrerBeregning` kjører én samlet beregning.
3. `utførBeregningOgFeilhåndtering` returnerer resultat per barn.
4. Respons bygges med vedtakstype `ENDRING`.

#### Konsekvens

- Begge barn får ordinær periodeliste.
- Grunnlagsliste er deduplisert og klar for vedtak.

---

### Case 2: BIDRAG med revurderingsbarn og evnesprekk (runde 2A -> 2B)

#### Grunnlag (illustrativt)

- Ett barn del av opprinnelig behandling
- Ett revurderingsbarn (`delAvOpprinneligBehandling = false`)
- Ikke direkte avslag

#### Flyt

1. Runde 2A kjøres uten revurderingsbarn, men i stedet med løpende bidrag for disse barna.
2. Runde 2A stopper med `IkkeFullBidragsevneOgUfullstendigeGrunnlagException`.
3. Runde 2B kjøres med nye grunnlag for revurderingsbarna.
4. Delberegninger fra 2A filtreres, får `_2A`-postfix og kobles på sluttberegningene i 2B.

#### Konsekvens

- Endelig resultat inkluderer sporbarhet til både 2A og 2B.
- Revurderingsbarn inngår i samlet vurdering når fullstendige grunnlag finnes.

---

### Case 3: OMGJØRING_ENDELIG med delvedtak

#### Grunnlag (illustrativt)

- Ett barn har gyldig mellomresultat
- Ett barn feiler i mellomberegning

#### Flyt

1. Omgjøringsberegning kjøres for begge barn.
2. Barn med feil returneres med `beregningsfeil` og tom vedtaksliste.
3. Barn uten feil sendes til `utførOmgjøringEndelig`.
4. Delvedtak og omgjøringsvedtak mappes til vedtaksliste.

#### Konsekvens

- Responsen er barnespesifikk: ett barn med feil, ett barn med vedtak.
- Grunnlag for delvedtak holdes separat der det kreves.

---

### Praktisk tolkning av eksemplene

- Case 1 viser standard flerbarnsberegning uten ekstra runder.
- Case 2 viser hvorfor orkestratoren må kombinere grunnlag fra flere runder.
- Case 3 viser hvordan endelig omgjøring håndterer blandet utfall per barn i samme request.


# Beregning av endelig bidrag V2 - forretningsregler

## Formål

Denne dokumentasjonen beskriver forretningsreglene i `BeregnEndeligBidragServiceV2`.

Tjenesten orkestrerer flere delberegninger og bygger sluttresultat per søknadsbarn.

---

## Overordnet flyt

`delberegningEndeligBidrag()` kjører disse stegene i fast rekkefølge:

1. `delberegningBidragspliktigesAndelDeltBosted`
2. `delberegningBidragTilFordeling`
3. `delberegningBidragTilFordelingLøpendeBidrag`
4. `delberegningBidragTilFordelingPrivatAvtale`
5. `delberegningSumBidragTilFordeling`
6. `delberegningEvne25ProsentAvInntekt`
7. `delberegningAndelAvBidragsevne`
8. `delberegningBidragJustertForBPBarnetillegg`
9. `sluttberegningBarnebidrag`

Rekkefølgen er viktig fordi senere steg bruker delresultater fra tidligere steg.

---

## Datasett som behandles

Tjenesten håndterer tre typer grunnlag parallelt:

- søknadsbarn
- løpende bidrag
- privat avtale

Søknadsbarn går gjennom hele kjeden. Løpende bidrag og privat avtale brukes særlig i fordelingsstegene.

---

## Viktige regler i orkestreringen

- Løpende bidrag og privat avtale beregnes alltid med `åpenSluttperiode = false` for å unngå at disse overstyrer nytt bidrag i åpen periode.
- `sum bidrag til fordeling` beregnes med `åpenSluttperiode = true` fordi steget kan omfatte flere barn/saker.
- Hvert delsteg mapper ut:
  - delberegningsresultat
  - refererte grunnlag
  - personobjekter brukt i beregningen
- Like perioder med samme innhold slås sammen per grunnlagstype.

---

## Periodisering

Hvert delsteg lager egne bruddperioder basert på grunnlagene det steget bruker.

Konsekvensen er at samme kalenderrom kan deles ulikt i ulike delberegninger, men sluttberegningen periodiserer på nytt med samlet grunnlag.

---

## Særregler i sluttsteget

I `sluttberegningBarnebidrag()` finnes ekstra regler for siste periode:

- siste periode kan åpnes (`til = null`) når `åpenSluttperiode` gjelder
- for avslag (`resultatBeløp = null`) kan åpen sluttperiode overstyres til `true`
- 18-årslogikk tas med i vurderingen av om avslag skal åpnes
- regelen gjelder ikke tilsvarende for `BIDRAG18AAR`

---

## Deldokumenter

For detaljerte regler per delberegning, se:

- `bidrag-beregn-barnebidrag/docs/Beregning av bidragspliktiges andel delt bosted - forretningsregler.md`
- `bidrag-beregn-barnebidrag/docs/Beregning av bidrag til fordeling - forretningsregler.md`
- `bidrag-beregn-barnebidrag/docs/Beregning av bidrag til fordeling løpende bidrag - forretningsregler.md`
- `bidrag-beregn-barnebidrag/docs/Beregning av bidrag til fordeling privat avtale - forretningsregler.md`
- `bidrag-beregn-barnebidrag/docs/Beregning av sum bidrag til fordeling - forretningsregler.md`
- `bidrag-beregn-barnebidrag/docs/Beregning av evne 25 prosent av inntekt - forretningsregler.md`
- `bidrag-beregn-barnebidrag/docs/Beregning av andel av bidragsevne - forretningsregler.md`
- `bidrag-beregn-barnebidrag/docs/Beregning av bidrag justert for BP barnetillegg - forretningsregler.md`
- `bidrag-beregn-barnebidrag/docs/Beregning av sluttberegning barnebidrag V2 - forretningsregler.md`


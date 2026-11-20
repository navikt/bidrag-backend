# bidrag-dokumentmal

Lager HTML for notater og vedtaksforsider i bidragssaker. Malene er skrevet i React (React Router 7) og rendres
på serveren. Appen tar imot JSON og svarer med ferdig HTML. [`bidrag-dokument-produksjon`](..) gjør HTML-en om til PDF.

Dokumentet beskriver hvordan malene er satt opp, for både utviklere og KI-assistenter. Hvis du bare skal endre en
tekst, kan du gå rett til [Endre tekster i notatene](#endre-tekster-i-notatene).

## Slik henger det sammen

```text
bidrag-behandling                 bidrag-dokument-produksjon          bidrag-dokumentmal (denne appen)
NotatOpplysningerService   ──►    POST /api/v2/notat/pdf       ──►    POST /notat/<notat>  ──►  HTML
lager VedtakNotatDto              kaller <url>/notat/<type>           tekster + mal               │
                                  og lager PDF av HTML-en      ◄──────────────────────────────────┘
```

- Input er `VedtakNotatDto` fra `libs/bidrag-felles/bidrag-transport` (Kotlin). TypeScript-typene ligger i
  [`app/types/Api.ts`](app/types/Api.ts), generert med `npm run gen_api`.
- Backend lager input i `NotatOpplysningerService` i bidrag-behandling. Input er rådata: beløp som tall og datoer som
  ISO-datoer. Malen bestemmer hvordan de vises.
- Når DTO-en endres, deployer du dokumentmal før bidrag-behandling, slik at malen tåler både gammel og ny input.

## Adresser

Hvert notat (`forskudd`, `særbidrag` og `bidrag`) har tre adresser. Alle tar imot `POST` med `VedtakNotatDto` som JSON.

| Adresse                       | Innhold                                                               |
| ----------------------------- | --------------------------------------------------------------------- |
| `/notat/<notat>`              | Hele notatet: begrunnelsen og så vedleggene. Brukes av backend i dag. |
| `/notat/begrunnelse/<notat>`  | Bare delen saksbehandleren har skrevet og vurdert.                    |
| `/notat/opplysninger/<notat>` | Bare vedleggene med opplysninger og beregningsdetaljer.               |
| `/vedtak/forside`             | Forsiden til orkestrerte bidragsvedtak.                               |

Headeren `renderforpdf: false` gir HTML uten topp- og bunntekst. Uten headeren lages PDF-versjonen.

Notat-adressene er satt opp i [`app/routes.ts`](app/routes.ts). Resten av `app/routes` følger React Routers
filnavnkonvensjon (`vedtak.forside/route.tsx` blir `/vedtak/forside`).

## Mappestruktur

Notatene er delt i to deler, **begrunnelse** og **opplysninger**. Mappene for kode og tekster er delt på samme måte.

```text
app/
├── routes.ts                         adressene til notatene
├── routes/notat/
│   ├── forskudd.tsx                  /notat/forskudd: hele notatet (begrunnelse + opplysninger)
│   ├── særbidrag.tsx
│   ├── bidrag.tsx
│   ├── felles/
│   │   └── NotatSide.tsx             rammen rundt alle notatene, se under
│   ├── begrunnelse/
│   │   ├── felles/                   deler som er like i alle notatene (boforhold, inntekter, virkningstidspunkt)
│   │   ├── forskudd/route.tsx        /notat/begrunnelse/forskudd, viser rekkefølgen på delene
│   │   ├── særbidrag/                route.tsx og delene som bare finnes i særbidrag (utgifter, vedtak)
│   │   └── bidrag/                   route.tsx og delene som bare finnes i bidrag (underholdskostnad, samvær, gebyr …)
│   └── opplysninger/
│       ├── felles/                   vedlegg som er like i alle notatene (boforhold, inntekter)
│       ├── forskudd/route.tsx        /notat/opplysninger/forskudd, viser rekkefølgen på vedleggene
│       ├── særbidrag/                route.tsx og beregningsdetaljer for særbidrag
│       └── bidrag/                   route.tsx, vedleggene for bidrag og beregningsdetaljer/
├── tekster/                          alle faste tekster, i YAML. Se «Endre tekster i notatene»
│   ├── ord.yaml                      Ja, Nei, Årsak, Periode …
│   ├── felles.yaml                   kolonnenavn og begreper som brukes på tvers av malene
│   ├── notat/
│   │   ├── begrunnelse/              felles.yaml, forskudd.yaml, særbidrag.yaml, bidrag.yaml
│   │   └── opplysninger/             felles.yaml, forskudd.yaml, særbidrag.yaml, bidrag.yaml
│   └── vedtak/forside.yaml           forsiden til orkestrerte bidragsvedtak (/vedtak/forside)
├── components/                       felles byggeklosser (tabeller, NotatContext, topptekst) brukt av notater og vedtak
└── utils/visning.ts                  hvordan beløp, prosent, datoer og perioder vises
```

Vil du se hvordan et notat er bygd opp, starter du i `routes/notat/<notat>.tsx`. Derfra følger du
`begrunnelse/<notat>/route.tsx` og `opplysninger/<notat>/route.tsx`. Hver av dem lister delene i samme rekkefølge
som i dokumentet.

## Byggeklosser

**`NotatSide`** ([`routes/notat/felles/NotatSide.tsx`](app/routes/notat/felles/NotatSide.tsx)) brukes av alle
notat-routes. Den leser input, legger på topp- og bunntekst, søknadsdetaljer og tittel, og gjør dataene tilgjengelige i
`NotatContext`. Den eksporterer også `notatAction` og `notatMeta`. En route ser slik ut:

```tsx
export const action = notatAction;
export const meta = notatMeta(tekster.tittel);

export default function NotatBidrag() {
  return (
    <NotatSide
      id="bidrag_notat"
      tittel={tekster.tittel}
      vedlegg={vedleggBidrag}
      kanGjeldeFlereSaker
    >
      <BegrunnelseBidrag />
      <OpplysningerBidrag />
    </NotatSide>
  );
}
```

**`NotatContext`** ([`components/notat_felles/NotatContext.tsx`](app/components/notat_felles/NotatContext.tsx)) gir
alle deler tilgang til dataene uten props: `useNotatFelles()` gir `data`, rollene (`bidragsmottaker`, `søknadsbarn` …)
og flagg som `erAvslag` og `erInnkrevingsgrunnlag`. `useVedleggNummer("boforhold")` gir nummeret til et vedlegg.
Bruk context framfor props.

**Vedleggsnummer.** `OpplysningerBidrag`, `OpplysningerForskudd` og `OpplysningerSærbidrag` viser vedleggene
i samme rekkefølge som i nummerlisten i samme route-fil. Endrer du rekkefølgen i JSX, må du også endre listen.
`useVedleggNummer` henter nummeret fra listen. Begrunnelsen bruker den samme listen for henvisninger, også når
delene rendres hver for seg.

**Tabeller og felter.** Bruk [`FeltTabell`](app/components/FeltTabell.tsx) med `Feltrad` for faste ledetekster og
verdier. Bruk `Feltrader` når radene bygges fra data. De justerte radvariantene bevarer kolonnebreddene og
resultatkolonnen i tabellene som bruker den utformingen. Sett tittel, bredde og CSS-klasser rundt tabellen.

[`DokumentTabell`](app/components/DokumentTabell.tsx) lar deg skrive rader og celler direkte i JSX.
Bruk `Tabellrad` og `Tabellcelle`, og `Tabelldetaljer` når en rad har opplysninger under seg.
Gi både raden og detaljene samme radindeks, slik at stripingen stemmer.

**Deler under hverandre.** Bruk [`DokumentStabel`](app/components/DokumentStabel.tsx) med `gap={1}` til `gap={4}`
for mellomrom på 4 til 16 piksler. På skjerm bruker den flex. Ved utskrift bruker den blokkflyt, slik at WeasyPrint
slipper gjentatte flex-beregninger av lange tabeller. Barnas egne marger kommer i tillegg til mellomrommet.
Legg delene direkte i stabelen. Hvis en underkomponent returnerer flere deler i et fragment, legger du stabelen
inne i underkomponenten, rundt disse delene.

[`KolonneTabell`](app/components/KolonneTabell.tsx) og [`Feltliste`](app/components/Feltliste.tsx)
lager tabeller og lister ut fra et oppsett:

```tsx
const kolonner: Tabelloppsett = {
  kolonner: [
    { felt: "periode", tittel: ord.periode, format: "periode" },
    {
      felt: "beløp",
      tittel: t.tabell.inntekt,
      format: "beløp",
      bredde: "80px",
    },
  ],
};
<KolonneTabell oppsett={kolonner} rader={perioder} />;
```

`felt` er navnet i input-JSON, `tittel` kommer fra en tekstfil og `format` er et av formatene i
[`utils/visning.ts`](app/utils/visning.ts) (`beløp`, `beløpAvrundet`, `prosent`, `dato`, `periode`,
`periodeTilOgMedTilMåned`). Bruk `celler` når en verdi må beregnes.

**Tekster i koden.** Tekstfilene importeres direkte, for eksempel
`import tekster from "~/tekster/notat/begrunnelse/bidrag.yaml"`. Typene til YAML-filene lages av
[`scripts/tekster.ts`](scripts/tekster.ts), både i Vite-bygget og med `npm run tekster:typer`. De legges i
`.tekster-typer/`, som ikke sjekkes inn.

## Regler for endringer

- Alle faste tekster ligger i `app/tekster/*.yaml`. Tekstfilene har bare tekster, ikke bredder, formater eller logikk.
- Ord som brukes mange steder (Ja, Nei, Årsak …) ligger i `ord.yaml`. Tekster for én del ligger i filen for den delen.
- Formatering av tall og datoer gjøres i malen med `utils/visning.ts`, ikke i backend.
- Bruk `NotatContext` framfor props. Legg logikk som gjelder hele notatet i contexten.
- Ikke endre adressene `/notat/<notat>`. bidrag-dokument-produksjon bygger dem ut fra notattypen.
- Hold funksjonene i `utils/visning.ts` enkle og uten React. Målet er at malene senere kan skrives om til Typst, og
  YAML-filene kan da brukes som de er.

## Utvikling

```sh
npm ci
npm run dev           # utviklingsserver på http://localhost:5173
npm run typecheck     # typer for routes og tekster + tsc
npm run lint
npm run build && npm start
```

Send et eksempel fra [`../data/notat`](../data/notat) for å se resultatet:

```sh
curl -X POST http://localhost:5173/notat/forskudd -H "Content-Type: application/json" -H "renderforpdf: false" \
  --data-binary @../data/notat/forskudd_innvilget.json > notat.html
```

`bidrag.json`, `forskudd_avslag.json`, `forskudd_innvilget.json` og `saerbidrag.json` i `../data/notat` er kopier av
snapshotene fra `NotatOpplysningerServiceSnapshotTest` i bidrag-behandling
(`src/test/resources/notat-snapshot`). Kjør testen med `-DOPPDATER_SNAPSHOT=true` og kopier filene hit når input
endres. Når du refaktorerer, rendrer du eksemplene før og etter og sammenligner HTML-en. Resultatet
skal være likt, bortsett fra endringer du har gjort med vilje.

### Tester for PDF-oppsett

Kjør fra denne mappen:

```sh
npm test
npm run tailwind_build
docker build -f ../bidrag-pdfgen/DockerfileWeasyprint \
  -t bidrag-pdfgen:layout-test ../bidrag-pdfgen
docker run --rm --entrypoint python -v "$PWD:/dokumentmal:ro" \
  bidrag-pdfgen:layout-test /dokumentmal/tests/test_pdf_layout.py -v
```

React-testene lager HTML-eksempelet som PDF-testene bruker. PDF-testene kjører med WeasyPrint fra renderer-imaget
og malenes egne stilark. De sjekker mellomrom og marger mot flex-oppsettet, både på skjerm og ved utskrift,
og at nestede stabler beholder tekst og tabellkolonner over flere sider.

## Endre tekster i notatene

Alle faste tekster i notatene ligger i YAML-filer under [`app/tekster`](app/tekster). Du kan endre dem uten å røre koden.

Hvert notat har to deler, og tekstene er delt på samme måte:

- **Begrunnelse**: delen saksbehandleren har skrevet og vurdert, med tittelen på notatet først.
- **Opplysninger**: vedleggene med opplysninger og beregningsdetaljer.

| Fil                               | Innhold                                                                                                                         |
| --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| `ord.yaml`                        | Ord og korte setninger som brukes flere steder, for eksempel «Ja», «Nei», «Årsak» og «Periode». En endring her slår ut overalt. |
| `felles.yaml`                     | Kolonnenavn og begreper som brukes på tvers av malene, også i vedtaksforsiden.                                                  |
| `notat/begrunnelse/<notat>.yaml`  | Begrunnelsesdelen av hvert notat, med tittelen på notatet først.                                                                |
| `notat/opplysninger/<notat>.yaml` | Vedleggene i hvert notat, med tittelen på vedleggsdokumentet først.                                                             |
| `vedtak/forside.yaml`             | Forsiden til orkestrerte bidragsvedtak: informasjon om klagen, endringer etter vedtaket og spørsmål.                            |

`<notat>` er `forskudd`, `særbidrag` eller `bidrag`. `felles.yaml` i de to mappene har deler som er like i alle
notatene, for eksempel boforhold og inntekter. Delene står i samme rekkefølge i filene som i notatet.
Tekstene i samværsdelen av bidragsnotatet ligger for eksempel under `samvær:` i `notat/begrunnelse/bidrag.yaml`,
og samværsvedlegget under `samvær:` i `notat/opplysninger/bidrag.yaml`.

`{}` i en tekst er en plassholder som fylles ut med en verdi, for eksempel et vedleggsnummer. La den stå.

Tekstfilene inneholder bare tekster. Søk etter teksten du vil endre, og endre teksten til høyre for kolon:

```yaml
tabellInnvilget:
  inntekt: Inntekt
  forskudd: Forskudd
```

Navnet til venstre brukes av malen og må ikke endres. Innrykket viser hvilken del teksten hører til, så la det stå som
det er. Tekster som starter med `{` eller inneholder `: ` må stå i anførselstegn, for eksempel
`tittel: "Vedlegg nr. {}: Boforhold"`.

Hvis malen bruker en tekst som ikke finnes i filene, gir `npm run typecheck` feil. Typene lages fra YAML-filene av
[`scripts/tekster.ts`](scripts/tekster.ts).

Hvilke kolonner som vises, bredder og hvordan beløp og datoer formateres, bestemmes i malen (`.tsx`-filene).
Formatene ligger samlet i [`app/utils/visning.ts`](app/utils/visning.ts).

Rekkefølgen på vedleggene står i `routes/notat/opplysninger/<notat>/route.tsx` (`vedleggForskudd()` osv.).
Vedleggsnumrene, også i henvisningene fra begrunnelsen, følger av denne listen.

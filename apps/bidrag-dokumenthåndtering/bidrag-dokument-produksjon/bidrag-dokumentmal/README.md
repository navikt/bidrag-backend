# bidrag-dokumentmal

Lager HTML for notater og vedtaksforsider i bidragssaker. Malene er skrevet i React (React Router 7) og rendres
på serveren. Appen tar imot JSON og svarer med ferdig HTML. [`bidrag-dokument-produksjon`](..) gjør HTML-en om til PDF.

Dokumentet beskriver hvordan malene er satt opp, for både utviklere og KI-assistenter. Hvis du bare skal endre en
tekst, kan du gå rett til [Endre tekster i notatene](#endre-tekster-i-notatene).

## Slik henger det sammen

```text
bidrag-behandling                 bidrag-dokument-produksjon          bidrag-dokumentmal (denne appen)
NotatOpplysningerService   ──►    POST /api/v2/notat/pdf       ──►    POST /notat/<notat>  ──►  HTML
lager VedtakNotatDto              ?del=KOMPLETT|BEGRUNNELSE|          tekster + mal               │
                                  OPPLYSNINGER, kaller dokumentmal                                │
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

bidrag-dokument-produksjon velger adresse ut fra parameteren `del` (`KOMPLETT`, `BEGRUNNELSE` eller `OPPLYSNINGER`).
Uten `del` brukes hele notatet.

Alle tre adressene viser søknadsdetaljene og rollene øverst. Når vedtaket er fattet, står «Ferdigstilt» med
saksbehandler og dato til slutt i dokumentet. I hele notatet står det etter vedleggene. Lenkene fra begrunnelsen til
vedleggene («se vedlegg nr. …») vises bare i hele notatet, fordi vedleggene ikke står i samme dokument når delene
lages hver for seg.

Headeren `renderforpdf: false` gir HTML uten topp- og bunntekst. Uten headeren lages PDF-versjonen.

Notat-adressene er satt opp i [`app/routes.ts`](app/routes.ts). Resten av `app/routes` følger React Routers
filnavnkonvensjon (`vedtak.forside/route.tsx` blir `/vedtak/forside`).

### Slik bygges notatdelene

De tre adressene for et notat er bygd av de samme to delene:

| Adresse                       | Fil                                           | `del`          | Innhold                                      |
| ----------------------------- | --------------------------------------------- | -------------- | -------------------------------------------- |
| `/notat/<notat>`              | `routes/notat/<notat>.tsx`                    | `komplett`     | `Begrunnelse<Notat>` + `Opplysninger<Notat>` |
| `/notat/begrunnelse/<notat>`  | `routes/notat/begrunnelse/<notat>/route.tsx`  | `begrunnelse`  | `Begrunnelse<Notat>`                         |
| `/notat/opplysninger/<notat>` | `routes/notat/opplysninger/<notat>/route.tsx` | `opplysninger` | `Opplysninger<Notat>`                        |

- `routes/notat/begrunnelse/<notat>/route.tsx` eksporterer `Begrunnelse<Notat>`. Den viser delene saksbehandleren har
  skrevet og vurdert, for eksempel virkningstidspunkt, inntekter, boforhold og vedtak, i samme rekkefølge som i notatet.
- `routes/notat/opplysninger/<notat>/route.tsx` eksporterer `Opplysninger<Notat>` med vedleggene, og
  `vedlegg<Notat>()`. Funksjonen gir rekkefølgen på vedleggene, og dermed vedleggsnumrene.

Hver route legger innholdet inn i [`NotatSide`](app/routes/notat/felles/NotatSide.tsx):

```tsx
// /notat/bidrag
<NotatSide del="komplett" tittel={...} vedlegg={vedleggBidrag}>
  <BegrunnelseBidrag />
  <OpplysningerBidrag />
</NotatSide>

// /notat/begrunnelse/bidrag
<NotatSide del="begrunnelse" tittel={...} vedlegg={vedleggBidrag}>
  <BegrunnelseBidrag />
</NotatSide>
```

`NotatSide` legger på det som er felles for alle delene: topp- og bunntekst, søknadsdetaljer med roller, tittel og
«Ferdigstilt» til slutt. Den gir `del` videre i `NotatContext`, som skjuler vedleggslenkene når delen ikke er
`komplett`.

bidrag-behandling ber om en del med `POST /api/v2/notat/pdf?del=BEGRUNNELSE` (eller `OPPLYSNINGER`) til
bidrag-dokument-produksjon, som velger tilhørende adresse og lager PDF av HTML-en. Når toggelen `behandling.splitt_notat`
er på, journalfører bidrag-behandling begrunnelsen og opplysningene som to journalposter.

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
├── components/                       felles byggeklosser (tabeller, NotatContext, topptekst) brukt av notater og vedtak
└── utils/
    ├── tekst.ts                      oversetting med norsk standardtekst inline i malen
    └── visning.ts                    hvordan beløp, prosent, datoer og perioder vises
```

Vil du se hvordan et notat er bygd opp, starter du i `routes/notat/<notat>.tsx`. Derfra følger du
`begrunnelse/<notat>/route.tsx` og `opplysninger/<notat>/route.tsx`. Hver av dem lister delene i samme rekkefølge
som i dokumentet.

## Byggeklosser

**`NotatSide`** ([`routes/notat/felles/NotatSide.tsx`](app/routes/notat/felles/NotatSide.tsx)) brukes av alle
notat-routes. Den leser input, legger på topp- og bunntekst, søknadsdetaljer og tittel, og gjør dataene tilgjengelige i
`NotatContext`. Til slutt viser den «Ferdigstilt» når vedtaket er fattet. `del` (`komplett`, `begrunnelse` eller
`opplysninger`) forteller hvilken del routen viser. Den eksporterer også `notatAction` og `notatMeta`. En route ser slik ut:

```tsx
export const action = notatAction;
const tittel = tekst(
  "notat.begrunnelse.bidrag.tittel",
  "Bidrag, saksbehandlingsnotat",
);
export const meta = notatMeta(tittel);

export default function NotatBidrag() {
  return (
    <NotatSide
      id="bidrag_notat"
      del="komplett"
      tittel={tittel}
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
`visLenkerTilVedlegg` er sann bare i hele notatet. Bruk `LenkeTilVedlegg` (eller `Seksjon`) for lenker til vedlegg,
så forsvinner de av seg selv i de delte notatene.
Bruk context framfor props.

**Vedleggsnummer.** `OpplysningerBidrag`, `OpplysningerForskudd` og `OpplysningerSærbidrag` viser vedleggene
eksplisitt i JSX. Funksjonen `vedleggBidrag`, `vedleggForskudd` eller `vedleggSærbidrag` i samme route-fil gir den
ordnede nummerlisten til `NotatSide`. Hold rekkefølgen i JSX og nummerlisten i sync når et vedlegg legges til, fjernes
eller flyttes. `useVedleggNummer` henter nummeret fra listen. Begrunnelsen bruker samme liste for henvisninger, også
når delene rendres hver for seg.

**Tabeller og felter.** Bruk [`FeltTabell`](app/components/FeltTabell.tsx) med `Feltrad` for faste ledetekster og
verdier. Bruk `Feltrader` når radene bygges fra data. De justerte radvariantene bevarer kolonnebreddene og
resultatkolonnen i tabellene som bruker den utformingen. Sett tittel, bredde og CSS-klasser rundt tabellen.

[`DokumentTabell`](app/components/DokumentTabell.tsx) lar deg skrive rader og celler direkte i JSX.
Bruk `Tabellrad` og `Tabellcelle`, og `Tabelldetaljer` når en rad har opplysninger under seg.
Gi både raden og detaljene samme radindeks, slik at stripingen stemmer.

[`CalculationTable`](app/components/CalculationTable.tsx) brukes til beregningstabeller med beskrivelse, valgfri
beregning og resultat. Bruk `summary` for en avsluttende summeringsrad. Samme komponent brukes i notater og vedtak.

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
    {
      felt: "periode",
      tittel: tekst("ord.periode", "Periode"),
      format: "periode",
    },
    {
      felt: "beløp",
      tittel: tekst("ord.inntekt", "Inntekt"),
      format: "beløp",
      bredde: "80px",
    },
  ],
};
<KolonneTabell oppsett={kolonner} rader={perioder} />;
```

`felt` er navnet i input-JSON, `tittel` er teksten som vises og `format` er et av formatene i
[`utils/visning.ts`](app/utils/visning.ts) (`beløp`, `beløpAvrundet`, `prosent`, `dato`, `periode`,
`periodeTilOgMedTilMåned`). Bruk `celler` når en verdi må beregnes.

**Tekster i koden.** Faste tekster står inline i malen gjennom
`tekst("notat.begrunnelse.bidrag.samvær.tittel", "Samvær")`. Norsk bokmål er standardteksten. `i18next` bruker
nøkkelen når en katalog senere legges til.

## Regler for endringer

- Bruk `tekst(nøkkel, norskTekst)` for brukerrettet tekst. Behold nøkkelen stabil og ikke endre norsk tekst med mindre
  dokumentinnholdet skal endres.
- Formatering av tall og datoer gjøres i malen med `utils/visning.ts`, ikke i backend.
- Bruk `NotatContext` framfor props. Legg logikk som gjelder hele notatet i contexten.
- Ikke endre adressene `/notat/<notat>`. bidrag-dokument-produksjon bygger dem ut fra notattypen.
- Hold funksjonene i `utils/visning.ts` enkle og uten React, slik at malene senere kan skrives om til Typst.

## Utvikling

```sh
npm ci
npm run dev           # utviklingsserver på http://localhost:5173
npm run typecheck     # typer for routes + tsc
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

Faste tekster står der de brukes i React- og TypeScript-filene. Søk etter den norske teksten, og endre standardteksten
i andre argument til `tekst`:

```tsx
<h3>{tekst("notat.begrunnelse.bidrag.samvær.tittel", "Samvær")}</h3>
```

Første argument er en stabil nøkkel som en senere katalog kan overskrive. Nøkkelen følger området, dokumentdelen og
tekstens plassering. Ikke endre nøkkelen når du retter norsk ordlyd.

### Legg til et annet språk

Samle oversettelsene i én fil per språk, for eksempel
[`app/oversettelser/en.ts`](app/oversettelser/en.ts):

```ts
export default {
  notat: {
    begrunnelse: {
      bidrag: {
        samvær: {
          tittel: "Contact",
        },
      },
    },
  },
  ord: {
    periode: "Period",
    beløp: "Amount",
  },
};
```

Importer katalogen i [`app/utils/tekst.ts`](app/utils/tekst.ts), og send den til `opprettTekstoversetter`:

```ts
import en from "~/oversettelser/en";

export const tekstoversetter = opprettTekstoversetter({
  en: {
    translation: en,
  },
});
export const tekst = tekstoversetter.tekst;
```

Malene endres ikke. `tekst("notat.begrunnelse.bidrag.samvær.tittel", "Samvær")` viser `Contact` når engelsk er aktivt.
Hvis nøkkelen ikke finnes i katalogen, viser `i18next` den norske standardteksten.

Bruk `{{navn}}` for verdier som skal oversettes sammen med teksten:

```tsx
<h2>
  {tekst("notat.vedlegg.tittel", "Vedlegg nr. {{nummer}}", {
    nummer: vedleggNummer,
  })}
</h2>
```

Maloversettelser kan da holde samme rekkefølge og grammatikk uavhengig av norsk:

```ts
{
  notat: {
    vedlegg: {
      tittel: "Appendix no. {{nummer}}",
    },
  },
}
```

Dokumentmalene rendres på serveren. Velger du språk per forespørsel, skal du opprette en oversetter for den forespørselen
eller gi den riktige oversetteren videre gjennom context. Ikke bytt språk på en delt global `i18next`-instans mellom
parallelle forespørsler.

Hvilke kolonner som vises, bredder og hvordan beløp og datoer formateres, bestemmes i malen (`.tsx`-filene).
Formatene ligger samlet i [`app/utils/visning.ts`](app/utils/visning.ts).

Tekster med `{}` bruker fortsatt `.replace`, for eksempel et vedleggsnummer. Behold plassholderen når du endrer
tekst. Nye navngitte verdier kan bruke i18next-syntaksen `{{navn}}` og tredje argument til `tekst`.

Rekkefølgen på vedleggene står i `routes/notat/opplysninger/<notat>/route.tsx` (`vedleggForskudd()` osv.).
Vedleggsnumrene, også i henvisningene fra begrunnelsen, følger av denne listen.

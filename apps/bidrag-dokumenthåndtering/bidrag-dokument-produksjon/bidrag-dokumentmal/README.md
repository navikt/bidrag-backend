# Welcome to React Router v7 + Vite!

📖 See the [React Router docs](https://reactrouter.com/home) for details on supported features.

## Development

Run the Vite dev server:

```shellscript
npm run dev
```

## Deployment

First, build your app for production:

```sh
npm run build
```

Then run the app in production mode:

```sh
npm start
```

Now you'll need to pick a host to deploy it to.

### DIY

If you're familiar with deploying Node applications, the built-in React Router app server (`react-router-serve`) is production-ready.

Make sure to deploy the output of `npm run build`

- `build/server`
- `build/client`

## Endre tekster i notatene

Alle faste tekster i notatene ligger i JSON-filer under [`app/tekster`](app/tekster). Du kan endre dem uten å røre koden.

Hvert notat har to deler, og tekstene er delt på samme måte:

- **Saksbehandler**: delen saksbehandleren har skrevet og vurdert, med tittelen på notatet først.
- **Opplysninger**: vedleggene med opplysninger og beregningsdetaljer.

| Fil                                                                   | Innhold                                                                                                                         |
| --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| `ord.json`                                                            | Ord og korte setninger som brukes flere steder, for eksempel «Ja», «Nei», «Årsak» og «Periode». En endring her slår ut overalt. |
| `felles.json`                                                         | Kolonnenavn og begreper som brukes på tvers av malene, også i vedtaksforsiden.                                                  |
| `notat/felles/saksbehandler.json`, `notat/felles/opplysninger.json`   | Deler som er like i alle notatene, for eksempel boforhold og inntekter.                                                         |
| `notat/<notat>/saksbehandler.json`, `notat/<notat>/opplysninger.json` | Tekstene for hvert notat (`forskudd`, `særbidrag` og `bidrag`). Forskudd har bare felles opplysninger.                          |

Delene står i samme rekkefølge i filene som i notatet.
Tekstene i samværsdelen av bidragsnotatet ligger for eksempel under `"samvær"` i `notat/bidrag/saksbehandler.json`,
og samværsvedlegget under `"samvær"` i `notat/bidrag/opplysninger.json`.

`{}` i en tekst er en plassholder som fylles ut med en verdi, for eksempel et vedleggsnummer. La den stå.

Tekstfilene inneholder bare tekster. Søk etter teksten du vil endre, og endre verdien til høyre for kolon:

```json
"tabellInnvilget": {
  "inntekt": "Inntekt",
  "forskudd": "Forskudd"
}
```

Endre bare teksten i anførselstegn til høyre. Navnet til venstre brukes av malen og må ikke endres.

Hvilke kolonner som vises, bredder og hvordan beløp og datoer formateres, bestemmes i malen (`.tsx`-filene).
Formatene ligger samlet i [`app/utils/visning.ts`](app/utils/visning.ts).

Rekkefølgen på vedleggene står i `route.tsx` for hvert notat (`const vedlegg = [...]`). Vedleggsnumrene, også i
henvisningene fra resten av notatet, følger av denne listen.

For å se resultatet kjører du `npm run dev` og sender et eksempel fra `../data/notat` til malen, for eksempel:

```sh
curl -X POST http://localhost:5173/notat/forskudd -H "Content-Type: application/json" --data-binary @../data/notat/forskudd_innvilget.json > notat.html
```

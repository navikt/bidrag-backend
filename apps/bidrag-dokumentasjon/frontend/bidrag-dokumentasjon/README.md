# Bidrag-dokumentasjon

React-appen viser dokumentasjon og diagrammer for Bidrag. Den bruker Vite, TypeScript og Aksel.

## Lokal utvikling

Kjør fra frontendmappen `apps/bidrag-dokumentasjon/frontend/bidrag-dokumentasjon`:

```sh
pnpm install --frozen-lockfile
pnpm run dev
```

`dev` og `build` kopierer først dokumentene fra `apps/bidrag-dokumentasjon/dokumentasjon` til `public/dokumentasjon` og lager en innholdsindeks. De genererte filene skal ikke sjekkes inn.

## Sjekk og bygg

Kjør fra frontendmappen:

```sh
pnpm exec biome ci . --error-on-warnings
pnpm run build
pnpm run preview
```

Biome sjekker lint og formatering. Bygget kjører TypeScript-sjekken og lager `dist`. `.editorconfig` angir fire mellomrom og maks 120 tegn per linje.

## Deploy

`.github/workflows/bidrag-dokumentasjon.yaml` sjekker og bygger appen på pull requests. Ved relevante endringer på `main` publiserer workflowen `dist` til GitHub Pages. Appen trenger ikke et lokalt deploy-script.

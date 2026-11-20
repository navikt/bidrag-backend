# AGENTS.md — bidrag-dokumentmal

Les [README.md](README.md) før du endrer malene. Den beskriver adressene, mappestrukturen, byggeklossene og reglene.

Det viktigste:

- Notatene er delt i `begrunnelse` og `opplysninger`, både i `app/routes/notat` og `app/tekster/notat`.
- Faste tekster ligger bare i `app/tekster/**/*.yaml`. Formatering skjer i malen med `app/utils/visning.ts`.
- Bruk `NotatContext` (`useNotatFelles`, `useVedleggNummer`) framfor props.
- Ikke endre adressene `/notat/<notat>`. Backend kaller dem.
- Sjekk endringene med `npm run typecheck`, `npx eslint app` og ved å rendre eksemplene i `../data/notat` før og
  etter endringen. HTML-en skal være lik, bortsett fra endringer du har gjort med vilje. `tsc` har kjente feil fra før,
  så sammenlign antallet feil før og etter (`npx tsc --noEmit --ignoreDeprecations 6.0`).
- Kjør bare prettier på filene du har endret.

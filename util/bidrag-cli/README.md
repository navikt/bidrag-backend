# bidrag-cli

CLI tools for bidrag

For å komme i gang kjør følgende

```shell script
npm install

npm run build

Deretter legg til følgende i din .bashrc eller .zshrc

alias bidrag-cli='function _bcli(){node /path/to/bidrag-cli/dist/main.js "$@"};_bcli'

```

For opprette token kjør følgende kommando (du må være på riktig cluster)

```shell script
bidrag-cli token bidrag-person
```

Med scope

```shell script
bidrag-cli token bidrag-dokument-journalpost --scope dev-fss.oppgavehandtering.oppgave
```

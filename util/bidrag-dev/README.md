# bidrag-dev

Interne ressurser til hjelp med utvikling og deploy av applikasjoner under bidrag, 
samt noen guider for utvikling/forvaltning, linting og maven-settings.

## Fish

Last inn Fish-funksjonene i gjeldende skall fra repo-roten:

```fish
source util/bidrag-dev/bidrag-dev.fish
```

For å laste funksjonene automatisk i nye Fish-skall, lenk fila inn i Fish sin
`conf.d`-mappe:

```fish
mkdir -p ~/.config/fish/conf.d
ln -s /path/to/bidrag-backend/util/bidrag-dev/bidrag-dev.fish ~/.config/fish/conf.d/bidrag-dev.fish
```

Bytt `/path/to/bidrag-backend` med plasseringen av repoet ditt. Funksjonen
`bidrag-cli` finner CLI-fila relativt til `bidrag-dev.fish`.
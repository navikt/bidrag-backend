# bidrag-cli

CLI-verktøy for bidrag. Kan brukes til å hente Azure AD-token/credentials for
bidrag-applikasjoner, lytte på og sende meldinger til Kafka-topics, kjøre API-kall mot
test-/q-miljøer basert på config-filer, og formatere Java/Kotlin-kode.

## Kom i gang

```shell script
npm install
npm run build
```

Deretter legg til følgende i din `.bashrc` eller `.zshrc`:

```shell script
alias bidrag-cli='function _bcli(){node /path/to/bidrag-cli/dist/main.js "$@"};_bcli'
```

Alternativt kan `./install.sh` kjøres, som gjør `npm install`/`npm run build` og
legger til aliaset automatisk.

## Kommandoer

### token / creds

Krever at du er logget på riktig Kubernetes-cluster med `kubectl` (dev-gcp/prod-gcp)
— clusteret avgjør hvilken Azure AD-tenant (dev/prod) token hentes fra.

Hent Azure AD access token (client credentials) for en applikasjon. Tokenet skrives ut
og kopieres automatisk til utklippstavlen:

```shell script
bidrag-cli token bidrag-person
```

Med eksplisitt scope:

```shell script
bidrag-cli token bidrag-dokument-journalpost --scope dev-fss.oppgavehandtering.oppgave
```

Hent Azure client id/secret for en app (lest fra miljøvariabler i kjørende pod):

```shell script
bidrag-cli creds bidrag-person
```

Med `-a` skrives alle `AZURE_`-miljøvariabler fra podden ut:

```shell script
bidrag-cli creds bidrag-person -a
```

### kafka

Lytt, send og administrer offsets for Kafka-topics. Legg til `--local` for å koble
mot en lokalt kjørende Kafka i stedet for cluster.

```shell script
# Lytt på et topic (--filter for tekstfilter, --offsett for start-offset)
bidrag-cli kafka consume mitt-topic --filter "søkestreng"

# Send en melding fra en JSON-fil til et topic
bidrag-cli kafka produce mitt-topic --message ./melding.json

# List alle tilgjengelige topics
bidrag-cli kafka list

# Vis offsets for et topic (evt. for en gitt consumer group)
bidrag-cli kafka offsets mitt-topic -g min-consumer-group

# Sett offset for en consumer group
bidrag-cli kafka set-offset mitt-topic -g min-consumer-group -o 42
```

### request

Kjør et API-kall mot bidrag-applikasjoner basert på en JSON-config-fil (URL, metode,
body, headers m.m.). Velg miljø med `--q1`/`--q2`:

```shell script
bidrag-cli request ./mockdata/requests/hent-vedtak.json --q1
```

### config

Sett eller vis global konfigurasjon for hvor mock-data, miljøvariabler og dokumenter
hentes fra (lagres lokalt via `electron-store`):

```shell script
bidrag-cli config --data-path mockdata/data
bidrag-cli config list
bidrag-cli config path
```

### formater-bisys / formater-ktlint

Kjører kodeformattering på det tilstøtende Java/Kotlin-prosjektet via Maven:

```shell script
# Formaterer bisys-kode med com.spotify.fmt:fmt-maven-plugin:format
bidrag-cli formater-bisys

# Formaterer Kotlin-kode med ktlint (antrun:run@ktlint)
bidrag-cli formater-ktlint
```

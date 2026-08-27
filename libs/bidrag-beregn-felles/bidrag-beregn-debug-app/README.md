# Debug app for testing av beregning lokalt

## Beskrivelse
Dette er en debug-applikasjon for å teste beregning orkestrator lokalt med mulighet for kall til eksterne tjenester

### Bruk
For å starte appen i debug-modus og automatisk åpne nettleseren, bruk `start.sh`-skriptet:
`./start.sh`
Dette starter appen på port 9898 med Remote-debugging aktivert på port 5005, og åpner http://localhost:9898 etter noen sekunder.

For debugging, koble til Remote-debugging på port 5005 fra din IDE.

## Alternativt kjøre appen opp via Intellij
Sett i gang ved å kjøre

`./initEnv.sh` i mappen
Dette henter inn nødvendige miljøvariabler fra en kjørende bidrag-behandling-q1 pod i dev-gcp clusteret.

Start deretter opp appen ved å kjøre `BeregningDebugAppApplication` i din IDE.

## Endepunkter
- Hovedside: http://localhost:9898 (tilgang til debug-verktøy for beregninger)
- Andre endepunkter kan finnes i applikasjonens kode eller logs ved oppstart
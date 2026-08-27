[![Build status](https://github.com/navikt/bidrag-alerts/workflows/Deploy%20alerts%20to%20dev%20and%20prod/badge.svg)](https://github.com/navikt/bidrag-alerts/workflows/Deploy%20alerts%20to%20dev%20and%20prod/badge.svg)

# bidrag-alerts

Varslinger for team-bidrag apper i bidrag namespace

For mer informasjon om hvordan alarmene fungere se:
[https://github.com/nais/doc/tree/master/docs/observability/alerts](https://github.com/nais/doc/tree/master/docs/observability/alerts)

## Utvikling:
Du kan bruke `https://prometheus.nais.preprod.local/graph` som hjelp til å teste queries.

## Varsler slack
Varsler fra apper i prod vil vises på slack kanalen #team-bidrag-varsel (varselkanal angitt i [bidrag-naiskonsoll](https://teams.nav.cloud.nais.io/teams/bidrag))

Varsler fra apper i dev vil vises på slack kanalen #team-bidrag-varsel-dev (denne er lagt inn via egen alertmanagerkonfig i bidrdag-alerts-dev.yaml). 

## Statusplattform - status.nav.no
Varsler fra bidragsapper vises i Statusplattformen:
 - [prod](https://status.nav.no/sp/Dashboard/Internt) 
 - [dev](https://status.intern.dev.nav.no/sp/Dashboard/Privatperson)

Innslag i Statusplattform kan administreres her:
 - [adminkonsoll-prod](https://status.nav.no/sp/Admin?tab=Tjenester)
 - [adminkonsoll-dev](https://status.intern.dev.nav.no/sp/Admin?tab=Tjenester)


## Deploy:
Deployes automatisk til prod og dev via github actions

### For NAV ansatte
Vi er tilgjenngelig på slack kanalen #team-bidrag
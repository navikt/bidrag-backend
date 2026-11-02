# bidrag-alerts

Varslinger for team-bidrag apper i bidrag namespace

For mer informasjon om hvordan alarmene fungerer, se:
[https://github.com/nais/doc/tree/master/docs/observability/alerts](https://github.com/nais/doc/tree/master/docs/observability/alerts)

## Utvikling:
Du kan bruke `https://prometheus.nais.preprod.local/graph` som hjelp til å teste queries.

## Varsler slack
Varslene fra reglene i `bidrag-alerts-prod.yaml` sendes til Slack-kanalen #team-bidrag-monitorering via `AlertmanagerConfig`. Varsler uten denne rutingen sendes til #team-bidrag-varsel (standardkanalen i [Nais-konsollen](https://console.nav.cloud.nais.io/team/bidrag/settings)).

Varsler fra apper i dev vil vises på slack kanalen #team-bidrag-varsel-dev (denne er lagt inn via egen alertmanagerkonfig i bidrag-alerts-dev.yaml).

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
Vi er tilgjengelig på slack kanalen #team-bidrag
# Postgres Auditlog → Slack (Cloud Run)

Dette repoet inneholder en Cloud Run-tjeneste som sender Slack-varsler når noen kjører SQL-spørringer mot postgres database som har skrudd på auditlog.

Når en utvikler kjører en database-spørring sendes det en melding Slack med lenke til auditloggen.
Kanalen som sendes til konfigureres via miljøvariabelen `_SLACK_WEBHOOK_SECRET` som settes i cloud build triggeren.

# Postgres Auditlog → Slack (Cloud Run)

Dette repoet inneholder en Cloud Run-tjeneste som sender Slack-varsler når noen kjører SQL-spørringer mot postgres database som har skrudd på auditlog.

Når en utvikler kjører en database-spørring sendes det en melding til Slack med lenke til auditloggen.
Slack-webhook-URL konfigureres via Cloud Build substitution `_SLACK_WEBHOOK_SECRET`, som brukes til å sette runtime-variabelen `SLACK_WEBHOOK_URL`.

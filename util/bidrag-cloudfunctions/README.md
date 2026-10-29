# Postgres- og DB2-auditlogg → Slack (Cloud Run)

Dette repoet inneholder en Cloud Run-tjeneste som sender Slack-varsler når noen kjører SQL-spørringer mot bidragsdatabaser som har skrudd på auditlog.

For databasene `BI464P`, `SJ461P` og `BI460P` får varselet tittelen «DB2 auditlog». Logglenken bruker filtrene `jsonPayload.requestType="dbAuditEntry"` og `jsonPayload.dbName=~"^(BI464P|SJ461P|BI460P)$"`. Andre databaser beholder Postgres-varselet og Postgres-filtrene. Begge lenketypene filtrerer på `insertId` og et tidsvindu på ±5 minutter.

Når en utvikler kjører en database-spørring sendes det en melding til Slack med lenke til auditloggen.
Slack-webhook-URL konfigureres via Cloud Build substitution `_SLACK_WEBHOOK_SECRET`, som brukes til å sette runtime-variabelen `SLACK_WEBHOOK_URL`.

# Postgres- og DB2-auditlogg → Slack (Cloud Run)

Dette repoet inneholder en Cloud Run-tjeneste som sender Slack-varsler når noen kjører SQL-spørringer mot bidragsdatabaser som har skrudd på auditlog.

For databasene `BI464P`, `SJ461P` og `BI460P` får varselet tittelen «DB2 auditlog». Logglenken bruker filtrene `jsonPayload.requestType="dbAuditEntry"` og `jsonPayload.dbName=~"^(BI464P|SJ461P|BI460P)$"`. Andre databaser beholder Postgres-varselet og Postgres-filtrene. Begge lenketypene filtrerer på `insertId` og et tidsvindu på ±5 minutter.

Når en utvikler kjører en database-spørring sendes det en melding til Slack med lenke til auditloggen.
Slack-webhook-URL konfigureres via Cloud Build substitution `_SLACK_WEBHOOK_SECRET`, som brukes til å sette runtime-variabelen `SLACK_WEBHOOK_URL`.

## To typer innkommende meldinger

Tjenesten tar imot to typer meldinger på samme endepunkt:

| Kilde | Kjennetegn | Autentisering |
|---|---|---|
| Logging-sink → Pub/Sub push (`database-auditlog-slack`) | `message.data` (base64-kodet LogEntry) | OIDC-token fra tjenestekontoen `database-auditlog-slack@<prosjekt>.iam.gserviceaccount.com`. Audience er `PUBSUB_AUDIENCE`. |
| Loggbasert varsel i Cloud Monitoring (webhook) | `incident` | Delt hemmelighet `GCP_WEBHOOK_SECRET` (Basic/Bearer eller `X-GCP-Secret-Token`) |

Loggbaserte varsler kan sende varsel høyst hvert femte minutt. Derfor sender sinken én melding per loggpost. Varslene kan beholdes mens sinken verifiseres, men da blir det dobbelt opp med meldinger i Slack.

Kjør testene med `npm test` i `postgres-auditlog/`.

## Oppsett av sinken `database-auditlog-slack`

Sett opp alt i prosjektet der auditloggene havner. Hvis DB2-loggene ligger i et annet prosjekt, trenger det prosjektet en egen sink til samme topic.

### Via Cloud Console

**Topic**

1. Gå til **Pub/Sub → Topics → Create topic**.
2. Topic ID: `database-auditlog-slack`. Fjern haken for «Add a default subscription». Trykk **Create**.
3. Gjenta for dead letter-topicen `database-auditlog-slack-dlq`.

**Sink**

1. Gå til **Logging → Log Router → Create sink**.
2. Sink name: `database-auditlog-slack`. Trykk **Next**.
3. Sink service: **Cloud Pub/Sub topic**. Velg `projects/<prosjekt>/topics/database-auditlog-slack`. Trykk **Next**.
4. Lim inn filteret under «Build inclusion filter»:

   ```text
   (protoPayload.methodName="cloudsql.instances.query" AND protoPayload.request.@type="type.googleapis.com/google.cloud.sql.audit.v1.PgAuditEntry" AND protoPayload.request.user=~"nav.no")
   OR (jsonPayload.requestType="dbAuditEntry" AND jsonPayload.dbName=~"^(BI464P|SJ461P|BI460P)$")
   ```

5. Trykk **Preview logs** og sjekk at filteret treffer auditloggene. Trykk **Create sink**.
6. Konsollen gir vanligvis sinkens writer identity tilgang til topicen. Sjekk det under **Pub/Sub → Topics → database-auditlog-slack → Permissions**: writer identity (`service-<nr>@gcp-sa-logging.iam.gserviceaccount.com`) skal ha **Pub/Sub Publisher**. Hvis den mangler, legg den til.

**Tjenestekonto og push-abonnement**

1. Gå til **IAM → Service accounts → Create service account**. Navn: `database-auditlog-slack`. Konto trenger ingen prosjektroller.
2. Åpne kontoen og gå til **Permissions → Grant access**. Gi `service-<prosjektnummer>@gcp-sa-pubsub.iam.gserviceaccount.com` rollen **Service Account Token Creator**.
3. Gå til **Pub/Sub → Subscriptions → Create subscription**:
   - Subscription ID: `database-auditlog-slack`
   - Topic: `database-auditlog-slack`
   - Delivery type: **Push**, endpoint `https://postgres-auditlog-961465283338.europe-north1.run.app`
   - Huk av **Enable authentication**. Service account: `database-auditlog-slack`. Audience: samme URL som endpoint.
   - Acknowledgement deadline: 30 sekunder
   - Retry policy: **Retry after exponential backoff delay**, 10–600 sekunder
   - Huk av **Enable dead lettering**. Topic: `database-auditlog-slack-dlq`. Maks antall leveringsforsøk: 10. Trykk **Grant** på tilgangene konsollen foreslår.
4. Trykk **Create**.

### Via gcloud

Kjør kommandoene i prosjektet der auditloggene havner.

```bash
PROJECT_ID=<prosjekt>
PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')
NAME=database-auditlog-slack
RUN_URL=https://postgres-auditlog-961465283338.europe-north1.run.app

# 1. Topic og dead letter-topic
gcloud pubsub topics create $NAME --project=$PROJECT_ID
gcloud pubsub topics create $NAME-dlq --project=$PROJECT_ID

# 2. Sink. Filteret bør være det samme som i de loggbaserte varslene.
gcloud logging sinks create $NAME pubsub.googleapis.com/projects/$PROJECT_ID/topics/$NAME \
  --project=$PROJECT_ID \
  --log-filter='(protoPayload.methodName="cloudsql.instances.query" AND protoPayload.request.@type="type.googleapis.com/google.cloud.sql.audit.v1.PgAuditEntry" AND protoPayload.request.user=~"nav.no") OR (jsonPayload.requestType="dbAuditEntry" AND jsonPayload.dbName=~"^(BI464P|SJ461P|BI460P)$")'

WRITER=$(gcloud logging sinks describe $NAME --project=$PROJECT_ID --format='value(writerIdentity)')
gcloud pubsub topics add-iam-policy-binding $NAME --project=$PROJECT_ID \
  --member="$WRITER" --role=roles/pubsub.publisher

# 3. Tjenestekonto for push. OIDC-tokenet blir verifisert i koden.
gcloud iam service-accounts create $NAME --project=$PROJECT_ID
gcloud iam service-accounts add-iam-policy-binding $NAME@$PROJECT_ID.iam.gserviceaccount.com \
  --project=$PROJECT_ID \
  --member="serviceAccount:service-$PROJECT_NUMBER@gcp-sa-pubsub.iam.gserviceaccount.com" \
  --role=roles/iam.serviceAccountTokenCreator

# 4. Push-abonnement med retry og dead letter
gcloud pubsub subscriptions create $NAME --project=$PROJECT_ID \
  --topic=$NAME \
  --push-endpoint=$RUN_URL \
  --push-auth-service-account=$NAME@$PROJECT_ID.iam.gserviceaccount.com \
  --push-auth-token-audience=$RUN_URL \
  --ack-deadline=30 \
  --min-retry-delay=10s --max-retry-delay=600s \
  --dead-letter-topic=$NAME-dlq --max-delivery-attempts=10

gcloud pubsub topics add-iam-policy-binding $NAME-dlq --project=$PROJECT_ID \
  --member="serviceAccount:service-$PROJECT_NUMBER@gcp-sa-pubsub.iam.gserviceaccount.com" \
  --role=roles/pubsub.publisher
gcloud pubsub subscriptions add-iam-policy-binding $NAME --project=$PROJECT_ID \
  --member="serviceAccount:service-$PROJECT_NUMBER@gcp-sa-pubsub.iam.gserviceaccount.com" \
  --role=roles/pubsub.subscriber
```

Cloud Run-tjenesten må fortsatt være åpen (`--allow-unauthenticated`), siden webhooken fra Monitoring ikke kan sende OIDC. Tjenesten krever likevel enten gyldig OIDC-token eller den delte hemmeligheten.

Cloud Build setter `PUBSUB_AUDIENCE` og `PUBSUB_SERVICE_ACCOUNT` (se `cloudbuild.yaml`). Hvis tjenestekontoen ligger i et annet prosjekt enn Cloud Build, må `PUBSUB_SERVICE_ACCOUNT` endres.

### Verifisering

1. Kjør en spørring mot en database med auditlog, eller publiser en kopi av en ekte loggpost til topicen.
2. Sjekk at meldingen kommer i Slack, og at lenken åpner riktig loggpost.
3. Sjekk at `database-auditlog-slack-dlq` er tom.
4. Deaktiver de loggbaserte varslene når sinken fungerer.

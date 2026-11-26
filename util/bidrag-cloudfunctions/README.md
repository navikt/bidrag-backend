# Postgres- og DB2-auditlogg → Slack (Cloud Run)

Dette repoet inneholder en Cloud Run-tjeneste som sender Slack-varsler når noen kjører SQL-spørringer mot bidragsdatabaser som har skrudd på auditlog.

For databasene `BI464P`, `SJ461P` og `BI460P` får varselet tittelen «DB2 auditlog». Logglenken bruker filtrene `jsonPayload.requestType="dbAuditEntry"` og `jsonPayload.dbName=~"^(BI464P|SJ461P|BI460P)$"`. Andre databaser beholder Postgres-varselet og Postgres-filtrene. Begge lenketypene filtrerer på `insertId` og et tidsvindu på ±5 minutter.

Når en utvikler kjører en database-spørring sendes det en melding til Slack med lenke til auditloggen.
Slack-webhook-URL konfigureres via Cloud Build substitution `_SLACK_WEBHOOK_SECRET`, som brukes til å sette runtime-variabelen `SLACK_WEBHOOK_URL`.

Kjør testene med `npm test` i `database-auditlog/`.

## Oppsett i Google Cloud Console

Alt settes opp i prosjektet `bidrag-prod-8f72`. Begge auditloggene (Postgres og DB2) havner der, så én sink dekker begge.

Rekkefølgen er viktig: topic → sink → tjenestekonto → abonnement.

### 1. Opprett topicene

1. Åpne **Pub/Sub → Topics** og trykk **Create topic**.
2. Fyll inn:
   - **Topic ID:** `database-auditlog-slack`
   - **Add a default subscription:** fjern haken
   - **Use a schema**, **Set message retention duration** og **Use a customer-managed encryption key**: la stå av
3. Trykk **Create**.
4. Gjenta stegene for dead letter-topicen med **Topic ID** `database-auditlog-slack-dlq`. Her kan **Add a default subscription** stå på, slik at meldinger som feiler kan leses senere.

### 2. Opprett sinken

1. Åpne **Logging → Log Router** og trykk **Create sink**.
2. **Sink details**
   - **Sink name:** `database-auditlog-slack`
   - **Sink description:** `Postgres- og DB2-auditlogg til Slack`
   - Trykk **Next**.
3. **Sink destination**
   - **Select sink service:** **Cloud Pub/Sub topic**
   - **Select a Cloud Pub/Sub topic:** `projects/bidrag-prod-8f72/topics/database-auditlog-slack`
   - Trykk **Next**.
4. **Choose logs to include in sink:** lim inn filteret under **Build inclusion filter**:

   ```text
   (protoPayload.methodName="cloudsql.instances.query" AND protoPayload.request.@type="type.googleapis.com/google.cloud.sql.audit.v1.PgAuditEntry" AND protoPayload.request.user=~"nav.no")
   OR (jsonPayload.requestType="dbAuditEntry" AND jsonPayload.dbName=~"^(BI464P|SJ461P|BI460P)$")
   ```

   - Første del treffer Postgres-spørringer fra personer (`@nav.no`). Tjenestekontoer og applikasjonsbrukere blir ikke med.
   - Andre del treffer DB2-spørringer mot `BI464P`, `SJ461P` og `BI460P`.
5. Trykk **Preview logs**. Logs Explorer åpner seg med filteret. Sjekk at du ser auditlogger fra begge databasetypene, og gå tilbake.
6. **Choose logs to filter out of sink:** la stå tomt. Trykk **Create sink**.
7. Sjekk tilgangen til writer identity:
   - Åpne sinken under **Log Router** og kopier **Writer identity** (`serviceAccount:service-<nr>@gcp-sa-logging.iam.gserviceaccount.com`).
   - Åpne **Pub/Sub → Topics → database-auditlog-slack → Permissions** (panelet til høyre, trykk **Show info panel** hvis det er skjult).
   - Writer identity skal ha rollen **Pub/Sub Publisher**. Hvis den mangler: trykk **Add principal**, lim inn identiteten uten `serviceAccount:`, velg rollen **Pub/Sub Publisher** og trykk **Save**.

Postgres-loggene er Data Access-auditlogger. De krever at Cloud SQL-instansen har flaggene `cloudsql.enable_pgaudit=on` og `pgaudit.log`, og at Data Access-logging er slått på for Cloud SQL under **IAM → Audit Logs**.

### 3. Opprett tjenestekontoen

Pub/Sub bruker denne kontoen til å signere et OIDC-token på hver melding. Tjenesten godtar bare tokens fra denne kontoen.

1. Åpne **IAM & Admin → Service accounts** og trykk **Create service account**.
2. Fyll inn:
   - **Service account name:** `database-auditlog-slack`
   - **Service account ID:** `database-auditlog-slack` (fylles ut automatisk)
   - **Description:** `Pub/Sub push til database-auditlog`
   - Trykk **Create and continue**.
3. **Grant this service account access to project:** hopp over, kontoen trenger ingen prosjektroller. Trykk **Continue** og **Done**.
4. Åpne kontoen, gå til fanen **Principals with access** (eller **Permissions**) og trykk **Grant access**.
   - **New principals:** `service-961465283338@gcp-sa-pubsub.iam.gserviceaccount.com` (Pub/Subs egen tjenesteagent)
   - **Role:** **Service Account Token Creator**
   - Trykk **Save**.

### 4. Opprett push-abonnementet

1. Åpne **Pub/Sub → Subscriptions** og trykk **Create subscription**.
2. Fyll inn:
   - **Subscription ID:** `database-auditlog-slack`
   - **Select a Cloud Pub/Sub topic:** `projects/bidrag-prod-8f72/topics/database-auditlog-slack`
   - **Delivery type:** **Push**
   - **Endpoint URL:** `https://postgres-auditlog-961465283338.europe-north1.run.app`
   - **Enable authentication:** huk av
     - **Service account:** `database-auditlog-slack@bidrag-prod-8f72.iam.gserviceaccount.com`
     - **Audience:** `https://postgres-auditlog-961465283338.europe-north1.run.app`
   - **Enable payload unwrapping:** av
   - **Message retention duration:** 7 dager (standard)
   - **Expiration period:** **Never expire**
   - **Acknowledgement deadline:** 30 sekunder
   - **Subscription filter:** tomt
   - **Exactly once delivery** og **Message ordering:** av
   - **Dead lettering:** huk av **Enable dead lettering**
     - **Dead letter topic:** `projects/bidrag-prod-8f72/topics/database-auditlog-slack-dlq`
     - **Maximum delivery attempts:** 10
     - Konsollen viser at Pub/Sub-tjenesteagenten mangler tilganger. Trykk **Grant** på begge forslagene (publisher på dead letter-topicen og subscriber på abonnementet).
   - **Retry policy:** **Retry after exponential backoff delay**, **Minimum backoff** 10 sekunder, **Maximum backoff** 600 sekunder
3. Trykk **Create**.

### 5. Cloud Run

- Tjenesten `postgres-auditlog` må være åpen for kall (**Security → Authentication: Allow unauthenticated invocations**). Autentiseringen skjer i koden.
- Cloud Build setter miljøvariablene `PUBSUB_AUDIENCE` og `PUBSUB_SERVICE_ACCOUNT` (se `cloudbuild.yaml`). De må stemme med **Audience** og **Service account** i abonnementet.
- Opprett Firestore-databasen i Native mode hvis prosjektet ikke allerede har en:

  ```bash
  gcloud firestore databases create \
    --database=database-auditlog \
    --location=europe-north1 \
    --type=firestore-native
  ```

- Cloud Run-revisjonens runtime-tjenestekonto trenger rollen **Cloud Datastore User** (`roles/datastore.user`) for å reservere hendelser i collection `database-auditlog-deliveries`.
- Hvis Firestore midlertidig ikke kan reservere en hendelse, bruker tjenesten en begrenset idempotenscache i minnet. Denne fallbacken beskytter bare mot duplikater i samme Cloud Run-instans.
- Aktiver Firestore TTL for idempotensdokumentene:

  ```bash
  gcloud firestore fields ttls update expiresAt \
    --database=database-auditlog \
    --collection-group=database-auditlog-deliveries \
    --enable-ttl
  ```

### 6. Verifisering

1. Kjør en spørring mot en database med auditlog.
2. Sjekk at meldingen kommer i Slack, og at lenken åpner riktig loggpost.
3. Åpne **Pub/Sub → Subscriptions → database-auditlog-slack → Metrics**. **Unacked messages** skal være 0.
4. Sjekk at det ikke ligger meldinger på `database-auditlog-slack-dlq`.
5. Ved feil: se loggene under **Cloud Run → postgres-auditlog → Logs**. `401` betyr at service account eller audience ikke stemmer med miljøvariablene.

### 7. Test med en egen loggpost

Det finnes ikke et skjema for å skrive loggposter i Cloud Console. Bruk derfor **Cloud Shell** (ikonet `>_` øverst til høyre i konsollen). Testen går gjennom hele kjeden: sink → topic → abonnement → Cloud Run → Slack.

1. Åpne Cloud Shell og velg prosjektet:

   ```bash
   gcloud config set project bidrag-prod-8f72
   ```

2. Skriv en testpost. Bytt ut e-posten med din egen, så blir du pinget i Slack.

   `gcloud logging write` kan bare skrive `jsonPayload` eller tekst. DB2-poster ligger i `jsonPayload`, så de kan skrives med `gcloud logging write`. Postgres-poster ligger i `protoPayload`, så de må skrives med `curl` mot Logging-API-et (`entries:write`). API-et godtar `protoPayload` av typen `AuditLog`.

   **DB2:**

   ```bash
   gcloud logging write database-auditlog-test \
     '{"requestType":"dbAuditEntry","dbName":"BI464P","epost":"fornavn.etternavn@nav.no","tIdent":"TEST123","pgCommand":"SELECT","sqlStatement":"TEST - ignorer"}' \
     --payload-type=json --severity=INFO
   ```

   **Postgres:**

   ```bash
   curl -s -X POST "https://logging.googleapis.com/v2/entries:write" \
     -H "Authorization: Bearer $(gcloud auth print-access-token)" \
     -H "Content-Type: application/json" \
     -d '{
       "entries": [{
         "logName": "projects/bidrag-prod-8f72/logs/database-auditlog-test",
         "resource": {"type": "global", "labels": {"project_id": "bidrag-prod-8f72"}},
         "severity": "INFO",
         "protoPayload": {
           "@type": "type.googleapis.com/google.cloud.audit.AuditLog",
           "methodName": "cloudsql.instances.query",
           "serviceName": "cloudsql.googleapis.com",
           "resourceName": "instances/test-instans",
           "request": {
             "@type": "type.googleapis.com/google.cloud.sql.audit.v1.PgAuditEntry",
             "auditClass": "WRITE",
             "auditType": "SESSION",
             "command": "UPDATE",
             "database": "test-db",
             "object": "public.test_tabell",
             "objectType": "TABLE",
             "statement": "TEST - ignorer",
             "user": "fornavn.etternavn@nav.no"
           }
         }
       }]
     }'
   ```

   Et vellykket kall svarer med `{}`.

3. Åpne **Logging → Logs Explorer** og søk etter `logName="projects/bidrag-prod-8f72/logs/database-auditlog-test"`. Posten skal vises.
4. Sjekk at det kommer en «DB2 auditlog»- eller «Postgres auditlog»-melding i Slack innen noen sekunder, og at lenken åpner testposten.
5. Hvis meldingen ikke kommer, sjekk i denne rekkefølgen:
   - **Pub/Sub → Topics → database-auditlog-slack → Metrics:** har topicen fått en melding? Hvis ikke, stemmer ikke sinkfilteret, eller writer identity mangler **Pub/Sub Publisher**.
   - **Pub/Sub → Subscriptions → database-auditlog-slack → Metrics:** blir meldingen levert eller forsøkt på nytt?
   - **Cloud Run → postgres-auditlog → Logs:** feilmeldinger fra tjenesten.

**Test Postgres gjennom hele kjeden (Cloud SQL Studio)**

Du kan også lage en ekte Postgres-post i konsollen:

1. Åpne **SQL → velg instansen → Cloud SQL Studio**.
2. Logg inn med **IAM database authentication** og din egen `@nav.no`-bruker. Velg en testdatabase.
3. Kjør en ufarlig skrivespørring, for eksempel:

   ```sql
   CREATE TEMP TABLE auditlog_test (id int);
   INSERT INTO auditlog_test VALUES (1);
   ```

4. Sjekk at det kommer en «Postgres auditlog»-melding i Slack. Det forutsetter at pgaudit og Data Access-logging er slått på for instansen.

**Test bare abonnementet og Cloud Run (uten sinken)**

1. Åpne **Pub/Sub → Topics → database-auditlog-slack → Messages** og trykk **Publish message**.
2. Lim inn i **Message body**:

   ```json
   {"logName":"projects/bidrag-prod-8f72/logs/database-auditlog-test","insertId":"test-1","timestamp":"2026-11-17T08:00:00Z","jsonPayload":{"requestType":"dbAuditEntry","dbName":"BI464P","epost":"fornavn.etternavn@nav.no","pgCommand":"SELECT"}}
   ```

   For å teste Postgres-formatet, lim inn denne i stedet. Den gir en «Postgres auditlog»-melding:

   ```json
   {"logName":"projects/bidrag-prod-8f72/logs/cloudaudit.googleapis.com%2Fdata_access","insertId":"test-pg-1","timestamp":"2026-11-17T08:00:00Z","resource":{"type":"cloudsql_database","labels":{"project_id":"bidrag-prod-8f72","database_id":"bidrag-prod-8f72:test-instans"}},"protoPayload":{"@type":"type.googleapis.com/google.cloud.audit.AuditLog","methodName":"cloudsql.instances.query","resourceName":"instances/test-instans","serviceName":"cloudsql.googleapis.com","request":{"@type":"type.googleapis.com/google.cloud.sql.audit.v1.PgAuditEntry","auditClass":"WRITE","auditType":"SESSION","command":"UPDATE","database":"test-db","object":"public.test_tabell","objectType":"TABLE","statement":"UPDATE test_tabell SET verdi = 1 WHERE id = 1","user":"fornavn.etternavn@nav.no"}}}
   ```

3. Trykk **Publish** og sjekk Slack. Lenken i meldingen finner ingen auditlog, siden dette er en loggmelding som er opprettet manuelt uten en faktisk auditlog innslag.

// index.js
const functions = require('@google-cloud/functions-framework');

const EXPECTED_TOKEN    = process.env.GCP_WEBHOOK_SECRET;
const SLACK_WEBHOOK_URL = process.env.SLACK_WEBHOOK_URL;      // Incoming webhook

// Map NAV usernames → Slack user IDs
const USERNAME_TO_SLACK_ID = {
  "ugur.alpay.cenar": "U7577GCKW",
  "simen.naper.hoston": "U03RYJH2URZ",
  "lars.otto.haugen": "UC07E0JAD",
  "morten.hermansen": "UAQQKJL9L",
  "magnus.rinnan": "UC8R961EW",
  "tatjana.andersen": "UA4DANW3H",
};

functions.http('gcpAuditToSlack', async (req, res) => {

  const token = req.header('X-GCP-Secret-Token') || req.header('Authorization')?.replace("Basic ", "")?.replace("Bearer ", "");
  if (!token || token !== EXPECTED_TOKEN) {
    return res.status(403).send('Forbidden');
  }

  const payload = req.body;
  if (!payload?.incident) return res.status(400).send('No incident');

  const summary = payload.incident.summary || '';
  const labelsFromSummary = {};
  const labelsBlock = summary.match(/labels \{([^}]+)\}/);
  if (labelsBlock) {
    labelsBlock[1].split(',').forEach(pair => {
      const [key, ...valueParts] = pair.split('=');
      labelsFromSummary[key.trim()] = valueParts.join('=').trim();
    });
  }

  const projectId = payload.incident.resource?.labels?.project_id || 'bidrag-dev-45a9';
  const {
    command     = '(ukjent)',
    database    = '(ukjent)',
    user        = '(ukjent)',
    insertId    = '(mangler)',
    username    = user.split('@')[0] || '(ukjent)'   // fallback: use part before @nav.no
  } = labelsFromSummary;


  console.debug("DEBUG:", JSON.stringify(labelsFromSummary), JSON.stringify(payload))
  // ── 3. Resolve Slack mention
  let mention = '';
  const slackId = USERNAME_TO_SLACK_ID[username.toLocaleLowerCase()];
  if (username && slackId) {
    mention = `<@${slackId}>`;
  } else if (username) {
    mention = `\`${username}\``; // show name but no ping
  }

  const timestamp = payload.incident.started_at ? payload.incident.started_at * 1000 : new Date().toISOString();
  const ts = new Date(timestamp);
  const timeStart = new Date(ts.getTime() - 5 * 60 * 1000).toISOString();
  const timeEnd   = new Date(ts.getTime() + 5 * 60 * 1000).toISOString();

  const logQuery = encodeURIComponent(
      `protoPayload.methodName="cloudsql.instances.query"\n` +
      `protoPayload.request.@type="type.googleapis.com/google.cloud.sql.audit.v1.PgAuditEntry"\n` +
      `protoPayload.request.user=~"${user || ''}"\n` +
      `protoPayload.request.database="${database || ''}"\n` +
      `insertId="${insertId || ''}"`
  );

  const logUrl = `https://console.cloud.google.com/logs/query;` +
      `query=${logQuery}` +
      `;timeRange=${timeStart}--${timeEnd}` +
      `;cursorTimestamp=${timestamp}` +
      `?project=${projectId}`;

  const slackText = [
    `*Postgres auditlog*`,
    ``,
    `Kommando: \`${command || '(ukjent)'}\``,
    `Database: \`${database || '(ukjent)'}\``,
    `Utført av: \`${user || '(ukjent)'}\``,
    ``,
    `Unik referanse (insertId): \`${insertId || '(mangler)'}\``,
    ``,
    `<${logUrl}|Lenke til auditlog (med tidsvindu ±5 min)>`,
    ``,
    `${mention} husk å dokumentere endringen i <https://audit-approval.iap.nav.cloud.nais.io/?|GAAL>`,
  ].join('\n');

  try {
    const response = await fetch(SLACK_WEBHOOK_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text: slackText, mrkdwn: true })
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error('Slack responded with error', { status: response.status, body: errorText });
      return res.status(500).send('Slack error');
    }

    console.log('Slack message sent successfully', { insertId, user });
    return res.status(200).send('OK');
  } catch (err) {
    console.error('Failed to send to Slack', err);
    return res.status(500).send('Fetch failed');
  }
});
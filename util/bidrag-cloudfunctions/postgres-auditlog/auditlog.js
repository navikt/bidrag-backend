const DB2_DATABASE_PATTERN = /^(BI464P|SJ461P|BI460P)$/;
const DEFAULT_PROJECT_ID = 'bidrag-dev-45a9';
const LOG_WINDOW_MS = 5 * 60 * 1000;
const UNKNOWN = '(ukjent)';
const MISSING = '(mangler)';
const LOGS_EXPLORER_URL = 'https://console.cloud.google.com/logs/query';
const GAAL_URL = 'https://audit-approval.iap.nav.cloud.nais.io/?';

const PAYLOAD_TYPE = Object.freeze({ PUBSUB: 'pubsub', INCIDENT: 'incident' });

const USERNAME_TO_SLACK_ID = {
  "ugur.alpay.cenar": "U7577GCKW",
  "simen.naper.hoston": "U03RYJH2URZ",
  "lars.otto.haugen": "UC07E0JAD",
  "morten.hermansen": "UAQQKJL9L",
  "magnus.rinnan": "UC8R961EW",
  "tatjana.andersen": "UA4DANW3H",
  "cristiano.sadun": "U0BT124NGCU",
  "sigbjorn.myhre": "UBB6YAXJB"
};

function detectPayloadType(body) {
  if (body?.message?.data) return PAYLOAD_TYPE.PUBSUB;
  if (body?.incident) return PAYLOAD_TYPE.INCIDENT;
  return null;
}

function usernameFrom(user) {
  return (user || '').split('@')[0] || UNKNOWN;
}

function parseSummaryLabels(summary) {
  const labels = {};
  const block = (summary || '').match(/labels \{([^}]+)\}/);
  if (block) {
    block[1].split(',').forEach(pair => {
      const [key, ...valueParts] = pair.split('=');
      labels[key.trim()] = valueParts.join('=').trim();
    });
  }
  return labels;
}

function fromIncident(incident) {
  const labels = parseSummaryLabels(incident.summary);
  const user = labels.user || UNKNOWN;
  return {
    command: labels.command || UNKNOWN,
    database: labels.database || UNKNOWN,
    user,
    username: labels.username || usernameFrom(labels.user),
    insertId: labels.insertId || MISSING,
    timestamp: incident.started_at ? new Date(incident.started_at * 1000).toISOString() : new Date().toISOString(),
    projectId: incident.resource?.labels?.project_id || incident.scoping_project_id || DEFAULT_PROJECT_ID,
  };
}

function fromLogEntry(entry) {
  const pg = entry.protoPayload?.request;
  const db2 = entry.jsonPayload;
  const user = pg?.user || db2?.epost || db2?.tIdent || UNKNOWN;
  return {
    command: pg?.command || db2?.pgCommand || UNKNOWN,
    database: pg?.database || db2?.dbName || UNKNOWN,
    user,
    username: usernameFrom(user === UNKNOWN ? '' : user),
    insertId: entry.insertId || MISSING,
    timestamp: entry.timestamp || entry.receiveTimestamp || new Date().toISOString(),
    projectId: entry.resource?.labels?.project_id || entry.logName?.split('/')[1] || DEFAULT_PROJECT_ID,
  };
}

function decodePubSubMessage(message) {
  return JSON.parse(Buffer.from(message.data, 'base64').toString('utf8'));
}

function normalize(body) {
  const type = detectPayloadType(body);
  if (type === PAYLOAD_TYPE.PUBSUB) return fromLogEntry(decodePubSubMessage(body.message));
  if (type === PAYLOAD_TYPE.INCIDENT) return fromIncident(body.incident);
  return null;
}

function isDb2(database) {
  return DB2_DATABASE_PATTERN.test(database);
}

function buildLogUrl({ database, user, insertId, timestamp, projectId }) {
  const ts = new Date(timestamp).getTime();
  const timeStart = new Date(ts - LOG_WINDOW_MS).toISOString();
  const timeEnd = new Date(ts + LOG_WINDOW_MS).toISOString();
  const cursor = new Date(ts).toISOString();

  const databaseQuery = isDb2(database)
    ? `jsonPayload.requestType="dbAuditEntry"\n` +
      `jsonPayload.dbName="${database}"\n`
    : `protoPayload.methodName="cloudsql.instances.query"\n` +
      `protoPayload.request.@type="type.googleapis.com/google.cloud.sql.audit.v1.PgAuditEntry"\n` +
      `protoPayload.request.user="${user}"\n` +
      `protoPayload.request.database="${database}"\n`;
  const query = encodeURIComponent(databaseQuery + `insertId="${insertId}"`);

  return `${LOGS_EXPLORER_URL};query=${query};timeRange=${timeStart}--${timeEnd};cursorTimestamp=${cursor}?project=${projectId}`;
}

function slackMention(username) {
  if (!username || username === UNKNOWN) return '';
  const slackId = USERNAME_TO_SLACK_ID[username.toLowerCase()];
  return slackId ? `<@${slackId}>` : `\`${username}\``;
}

function buildSlackText(event) {
  return [
    `*${isDb2(event.database) ? 'DB2' : 'Postgres'} auditlog*`,
    ``,
    `Kommando: \`${event.command}\``,
    `Database: \`${event.database}\``,
    `Utført av: \`${event.user}\``,
    ``,
    `Unik referanse (insertId): \`${event.insertId}\``,
    ``,
    `<${buildLogUrl(event)}|Lenke til auditlog (med tidsvindu ±5 min)>`,
    ``,
    `${slackMention(event.username)} husk å dokumentere endringen i <${GAAL_URL}|GAAL>`,
  ].join('\n');
}

module.exports = {
  USERNAME_TO_SLACK_ID,
  PAYLOAD_TYPE,
  detectPayloadType,
  parseSummaryLabels,
  fromIncident,
  fromLogEntry,
  normalize,
  buildLogUrl,
  slackMention,
  buildSlackText,
};

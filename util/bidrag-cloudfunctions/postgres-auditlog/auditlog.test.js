const test = require('node:test');
const assert = require('node:assert/strict');
const a = require('./auditlog');

const toPubSub = entry => ({ message: { data: Buffer.from(JSON.stringify(entry)).toString('base64'), messageId: '1' }, subscription: 'projects/p/subscriptions/database-auditlog-slack' });

const pgEntry = {
  insertId: 'pg-123',
  timestamp: '2025-01-10T12:00:00.000Z',
  resource: { labels: { project_id: 'bidrag-prod-8f72' } },
  protoPayload: { request: { user: 'ugur.alpay.cenar@nav.no', database: 'bidrag-vedtak', command: 'UPDATE' } },
};
const db2Entry = {
  insertId: 'db2-456',
  timestamp: '2025-01-10T12:02:00.000Z',
  logName: 'projects/bidrag-db2-prod/logs/loggkamel-arkiv',
  jsonPayload: {
    auditType: 'SESSION', dbName: 'BI464P', epost: 'Magnus.Rinnan@nav.no', navIdent: null,
    pgAuditClass: 'WRITE', pgCommand: 'DELETE', requestType: 'dbAuditEntry',
    sqlStatement: "DELETE FROM BI464P.t_rolle WHERE saksnr='XXXX'", tIdent: 'TNV2990',
  },
};
const incident = { incident: { started_at: 1736510400, resource: { labels: { project_id: 'bidrag-prod-8f72' } }, summary: 'log match labels {command=SELECT, database=bidrag-sak, user=simen.naper.hoston@nav.no, insertId=inc-789}' } };

test('detectPayloadType skiller Pub/Sub, incident og ukjent', () => {
  assert.equal(a.detectPayloadType(toPubSub(pgEntry)), a.PAYLOAD_TYPE.PUBSUB);
  assert.equal(a.detectPayloadType(incident), a.PAYLOAD_TYPE.INCIDENT);
  assert.equal(a.detectPayloadType({}), null);
  assert.equal(a.detectPayloadType(undefined), null);
});

test('normalize Pub/Sub Postgres-loggpost', () => {
  assert.deepEqual(a.normalize(toPubSub(pgEntry)), {
    command: 'UPDATE', database: 'bidrag-vedtak', user: 'ugur.alpay.cenar@nav.no', username: 'ugur.alpay.cenar',
    insertId: 'pg-123', timestamp: '2025-01-10T12:00:00.000Z', projectId: 'bidrag-prod-8f72',
  });
});

test('normalize Pub/Sub DB2-loggpost henter prosjekt fra logName', () => {
  const e = a.normalize(toPubSub(db2Entry));
  assert.equal(e.database, 'BI464P');
  assert.equal(e.user, 'Magnus.Rinnan@nav.no');
  assert.equal(e.username, 'Magnus.Rinnan');
  assert.equal(e.command, 'DELETE');
  assert.equal(e.projectId, 'bidrag-db2-prod');
  assert.equal(a.slackMention(e.username), '<@UC8R961EW>');
});

test('normalize DB2-loggpost uten epost faller tilbake til tIdent', () => {
  const e = a.normalize(toPubSub({ ...db2Entry, jsonPayload: { ...db2Entry.jsonPayload, epost: null } }));
  assert.equal(e.user, 'TNV2990');
});

test('normalize incident konverterer started_at fra sekunder til ISO', () => {
  const e = a.normalize(incident);
  assert.equal(e.timestamp, '2025-01-10T12:00:00.000Z');
  assert.equal(e.insertId, 'inc-789');
  assert.equal(e.username, 'simen.naper.hoston');
});

test('normalize kaster ved ugyldig base64-JSON', () => {
  assert.throws(() => a.normalize({ message: { data: Buffer.from('ikke json').toString('base64') } }), SyntaxError);
});

test('fromLogEntry gir plassholdere når felter mangler', () => {
  const e = a.fromLogEntry({});
  assert.equal(e.user, '(ukjent)');
  assert.equal(e.username, '(ukjent)');
  assert.equal(e.insertId, '(mangler)');
  assert.equal(e.projectId, 'bidrag-dev-45a9');
});

test('buildLogUrl for DB2 bruker ISO cursorTimestamp, riktig prosjekt og dbName', () => {
  const url = a.buildLogUrl(a.normalize(toPubSub(db2Entry)));
  assert.match(url, /;timeRange=2025-01-10T11:57:00\.000Z--2025-01-10T12:07:00\.000Z;/);
  assert.match(url, /;cursorTimestamp=2025-01-10T12:02:00\.000Z\?project=bidrag-db2-prod$/);
  const query = decodeURIComponent(url.match(/;query=([^;]+);/)[1]);
  assert.match(query, /jsonPayload\.dbName="BI464P"/);
  assert.match(query, /insertId="db2-456"/);
});

test('buildLogUrl for Postgres filtrerer på user og database', () => {
  const query = decodeURIComponent(a.buildLogUrl(a.normalize(toPubSub(pgEntry))).match(/;query=([^;]+);/)[1]);
  assert.match(query, /protoPayload\.request\.user="ugur\.alpay\.cenar@nav\.no"/);
  assert.match(query, /protoPayload\.request\.database="bidrag-vedtak"/);
});

test('slackMention pinger kjent bruker, viser ukjent uten ping', () => {
  assert.equal(a.slackMention('Ugur.Alpay.Cenar'), '<@U7577GCKW>');
  assert.equal(a.slackMention('ola.nordmann'), '`ola.nordmann`');
  assert.equal(a.slackMention('(ukjent)'), '');
});

test('buildSlackText merker DB2 og Postgres', () => {
  assert.match(a.buildSlackText(a.normalize(toPubSub(db2Entry))), /^\*DB2 auditlog\*/);
  assert.match(a.buildSlackText(a.normalize(toPubSub(pgEntry))), /^\*Postgres auditlog\*/);
});

test('normalize ekte Postgres-loggpost fra Cloud SQL data_access', () => {
  const entry = {
    insertId: '2#887401732910#bidrag-regnskap-prod#audit#1793779875369000000#0000000000000141-0@a1',
    logName: 'projects/bidrag-prod-8f72/logs/cloudaudit.googleapis.com%2Fdata_access',
    protoPayload: {
      '@type': 'type.googleapis.com/google.cloud.audit.AuditLog',
      methodName: 'cloudsql.instances.query',
      request: {
        '@type': 'type.googleapis.com/google.cloud.sql.audit.v1.PgAuditEntry',
        auditClass: 'WRITE', command: 'INSERT', database: 'bidrag-regnskap-db',
        statement: 'INSERT INTO palop ...', user: 'simen.naper.hoston@nav.no',
      },
    },
    timestamp: '2026-11-04T08:11:15.369Z',
  };
  const e = a.normalize(toPubSub(entry));
  assert.equal(e.command, 'INSERT');
  assert.equal(e.database, 'bidrag-regnskap-db');
  assert.equal(e.projectId, 'bidrag-prod-8f72');
  assert.equal(a.slackMention(e.username), '<@U03RYJH2URZ>');

  const url = a.buildLogUrl(e);
  assert.match(url, /;cursorTimestamp=2026-11-04T08:11:15\.369Z\?project=bidrag-prod-8f72$/);
  const query = decodeURIComponent(url.match(/;query=([^;]+);/)[1]);
  assert.match(query, /insertId="2#887401732910#bidrag-regnskap-prod#audit#1793779875369000000#0000000000000141-0@a1"/);
  assert.match(a.buildSlackText(e), /^\*Postgres auditlog\*/);
});

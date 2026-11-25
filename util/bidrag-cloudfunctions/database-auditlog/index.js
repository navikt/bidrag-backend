const functions = require('@google-cloud/functions-framework');
const { Firestore } = require('@google-cloud/firestore');
const { OAuth2Client } = require('google-auth-library');
const { PAYLOAD_TYPE, detectPayloadType, normalize, idempotencyKey, buildSlackText } = require('./auditlog');
const { IdempotencyStore } = require('./idempotency');

const EXPECTED_TOKEN = process.env.GCP_WEBHOOK_SECRET;
const SLACK_WEBHOOK_URL = process.env.SLACK_WEBHOOK_URL;
const PUBSUB_AUDIENCE = process.env.PUBSUB_AUDIENCE;
const PUBSUB_SERVICE_ACCOUNT = process.env.PUBSUB_SERVICE_ACCOUNT;
const FIRESTORE_DATABASE_ID = process.env.FIRESTORE_DATABASE_ID || 'database-auditlog';

const oidcClient = new OAuth2Client();
const idempotencyStore = new IdempotencyStore({
  firestore: new Firestore({ databaseId: FIRESTORE_DATABASE_ID }),
  onFallback: error => console.warn('Firestore unavailable; using in-memory idempotency', {
    error: error.message,
  }),
});

function bearerToken(req) {
  return req.header('Authorization')?.replace(/^(Basic|Bearer) /, '');
}

function isValidSharedSecret(req) {
  const token = req.header('X-GCP-Secret-Token') || bearerToken(req);
  return Boolean(token) && token === EXPECTED_TOKEN;
}

async function isValidPubSubToken(req) {
  const token = bearerToken(req);
  if (!token || !PUBSUB_AUDIENCE || !PUBSUB_SERVICE_ACCOUNT) return false;
  try {
    const ticket = await oidcClient.verifyIdToken({ idToken: token, audience: PUBSUB_AUDIENCE });
    const claims = ticket.getPayload();
    return claims.email === PUBSUB_SERVICE_ACCOUNT && claims.email_verified === true;
  } catch (err) {
    console.warn('Invalid Pub/Sub OIDC token', err.message);
    return false;
  }
}

functions.http('gcpAuditToSlack', async (req, res) => {
  const type = detectPayloadType(req.body);
  if (!type) return res.status(400).send('Unknown payload');

  const authorized = type === PAYLOAD_TYPE.PUBSUB ? await isValidPubSubToken(req) : isValidSharedSecret(req);
  if (!authorized) return res.status(403).send('Forbidden');

  let event;
  try {
    event = normalize(req.body);
  } catch (err) {
    console.error('Could not parse payload', { type, error: err.message });
    return res.status(400).send('Invalid payload');
  }

  console.debug('Received auditlog event', { type, insertId: event.insertId });

  const key = idempotencyKey(req.body, event);
  if (!key) return res.status(400).send('Missing event id');

  try {
    const result = await idempotencyStore.runOnce(key, async () => {
      const response = await fetch(SLACK_WEBHOOK_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: buildSlackText(event), mrkdwn: true })
      });

      if (!response.ok) {
        const responseBody = await response.text();
        console.error('Slack responded with error', { status: response.status, body: responseBody });
        throw new Error(`Slack responded with status ${response.status}`);
      }
    });

    if (!result.executed) {
      console.log('Duplicate auditlog event ignored', { type, insertId: event.insertId, key });
      return res.status(200).send('Duplicate');
    }

    console.log('Slack message sent successfully', { type, insertId: event.insertId });
    return res.status(200).send('OK');
  } catch (err) {
    console.error('Failed to send to Slack', err);
    return res.status(500).send('Processing failed');
  }
});

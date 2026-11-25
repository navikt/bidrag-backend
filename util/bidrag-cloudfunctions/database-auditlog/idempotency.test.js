const test = require('node:test');
const assert = require('node:assert/strict');
const {
  IdempotencyCompletionError,
  IdempotencyInProgressError,
  IdempotencyStore,
  MemoryIdempotencyStore,
} = require('./idempotency');

class FakeFirestore {
  constructor() {
    this.documents = new Map();
    this.transaction = Promise.resolve();
  }

  collection(name) {
    return {
      doc: id => ({ path: `${name}/${id}` }),
    };
  }

  runTransaction(callback) {
    const operation = this.transaction.then(() => {
      const transaction = new FakeTransaction(this.documents);
      return callback(transaction).then(result => {
        transaction.commit();
        return result;
      });
    });
    this.transaction = operation.catch(() => {});
    return operation;
  }
}

class FakeTransaction {
  constructor(documents) {
    this.documents = documents;
    this.changes = [];
  }

  async get(reference) {
    const data = this.documents.get(reference.path);
    return {
      exists: data !== undefined,
      data: () => structuredClone(data),
    };
  }

  create(reference, data) {
    this.changes.push(['set', reference, structuredClone(data)]);
  }

  update(reference, data) {
    this.changes.push(['update', reference, structuredClone(data)]);
  }

  delete(reference) {
    this.changes.push(['delete', reference]);
  }

  commit() {
    for (const [operation, reference, data] of this.changes) {
      if (operation === 'delete') {
        this.documents.delete(reference.path);
      } else if (operation === 'update') {
        this.documents.set(reference.path, { ...this.documents.get(reference.path), ...data });
      } else {
        this.documents.set(reference.path, data);
      }
    }
  }
}

function createStore(firestore = new FakeFirestore(), options = {}) {
  return new IdempotencyStore({ firestore, ...options });
}

test('runOnce utfører samme nøkkel bare én gang', async () => {
  const firestore = new FakeFirestore();
  const stores = [createStore(firestore), createStore(firestore)];
  let executions = 0;

  const operation = async () => {
    executions += 1;
    return 'sent';
  };

  const [first, duplicate] = await Promise.allSettled([
    stores[0].runOnce('event-1', operation),
    stores[1].runOnce('event-1', operation),
  ]);

  assert.equal(executions, 1);
  assert.equal(first.status, 'fulfilled');
  assert.equal(duplicate.status, 'rejected');
  assert.ok(first.value?.executed);
  assert.ok(duplicate.reason instanceof IdempotencyInProgressError);
});

test('runOnce lar feilede nøkkelen forsøkes på nytt', async () => {
  const store = createStore();

  await assert.rejects(
    store.runOnce('event-1', async () => {
      throw new Error('Slack unavailable');
    }),
    /Slack unavailable/,
  );

  const retry = await store.runOnce('event-1', async () => 'sent');

  assert.deepEqual(retry, { executed: true, value: 'sent' });
});

test('runOnce gjenopptar utløpt lease etter en krasjet prosess', async () => {
  let now = 1_000;
  const store = createStore(new FakeFirestore(), {
    leaseMs: 100,
    now: () => now,
    createOwnerId: () => `owner-${now}`,
  });

  await store.claim('event-1', 'crashed-owner');
  now += 101;
  const result = await store.runOnce('event-1', async () => 'sent');

  assert.deepEqual(result, { executed: true, value: 'sent' });
});

test('runOnce holder ferdig hendelse idempotent etter lease', async () => {
  let now = 1_000;
  const firestore = new FakeFirestore();
  const store = createStore(firestore, { leaseMs: 100, now: () => now });

  await store.runOnce('event-1', async () => 'sent');
  now += 101;
  const duplicate = await store.runOnce('event-1', async () => 'sent-again');

  assert.deepEqual(duplicate, { executed: false });
});

test('runOnce lagrer Firestore TTL for ferdige hendelser', async () => {
  const now = 1_000;
  const firestore = new FakeFirestore();
  const store = createStore(firestore, { ttlMs: 500, now: () => now });

  await store.runOnce('event-1', async () => 'sent');

  const [document] = firestore.documents.values();
  assert.equal(document.expiresAt.getTime(), now + 500);
  assert.equal(document.status, 'completed');
});

test('runOnce bruker minne når Firestore ikke kan reservere hendelsen', async () => {
  const fallbackStore = new MemoryIdempotencyStore();
  const failures = [];
  const store = new IdempotencyStore({
    firestore: {
      collection: () => {
        throw new Error('Firestore unavailable');
      },
    },
    fallbackStore,
    onFallback: error => failures.push(error.message),
  });
  let executions = 0;
  const operation = async () => {
    executions += 1;
    return 'sent';
  };

  const [first, duplicate] = await Promise.all([
    store.runOnce('event-1', operation),
    store.runOnce('event-1', operation),
  ]);

  assert.equal(executions, 1);
  assert.deepEqual(first, { executed: true, value: 'sent' });
  assert.deepEqual(duplicate, { executed: false });
  assert.deepEqual(failures, ['Firestore unavailable']);
});

test('MemoryIdempotencyStore lar feilede hendelser forsøkes på nytt', async () => {
  const store = new MemoryIdempotencyStore();

  await assert.rejects(
    store.runOnce('event-1', async () => {
      throw new Error('Slack unavailable');
    }),
    /Slack unavailable/,
  );

  const retry = await store.runOnce('event-1', async () => 'sent');

  assert.deepEqual(retry, { executed: true, value: 'sent' });
});

test('runOnce bruker ikke minnefallback etter vellykket Slack-kall', async () => {
  const firestore = new FakeFirestore();
  let fallbackEvents = 0;
  const store = createStore(firestore, { onFallback: () => {
    fallbackEvents += 1;
  } });
  store.complete = async () => {
    throw new Error('Firestore completion failed');
  };
  let operationExecutions = 0;

  await assert.rejects(
    store.runOnce('event-1', async () => {
      operationExecutions += 1;
      return 'sent';
    }),
    IdempotencyCompletionError,
  );

  assert.equal(operationExecutions, 1);
  assert.equal(fallbackEvents, 0);

  await assert.rejects(
    store.runOnce('event-1', async () => {
      operationExecutions += 1;
      return 'sent-again';
    }),
    IdempotencyCompletionError,
  );
  assert.equal(operationExecutions, 1);
});

test('runOnce beholder deduplisering fra minne etter at Firestore kommer tilbake', async () => {
  let available = false;
  const firestore = new FakeFirestore();
  const store = createStore({
    collection: name => {
      if (!available) throw new Error('Firestore unavailable');
      return firestore.collection(name);
    },
    runTransaction: callback => firestore.runTransaction(callback),
  });
  let executions = 0;
  const operation = async () => {
    executions += 1;
    return 'sent';
  };

  await store.runOnce('event-1', operation);
  available = true;
  const duplicate = await store.runOnce('event-1', operation);

  assert.equal(executions, 1);
  assert.deepEqual(duplicate, { executed: false });
});

test('runOnce beholder deduplisering fra Firestore når Firestore blir utilgjengelig', async () => {
  let available = true;
  const firestore = new FakeFirestore();
  const store = createStore({
    collection: name => {
      if (!available) throw new Error('Firestore unavailable');
      return firestore.collection(name);
    },
    runTransaction: callback => firestore.runTransaction(callback),
  });
  let executions = 0;
  const operation = async () => {
    executions += 1;
    return 'sent';
  };

  await store.runOnce('event-1', operation);
  available = false;
  const duplicate = await store.runOnce('event-1', operation);

  assert.equal(executions, 1);
  assert.deepEqual(duplicate, { executed: false });
});

test('runOnce kaster retrybar feil for aktiv lease fra en annen instans', async () => {
  const firestore = new FakeFirestore();
  const owner = createStore(firestore, { createOwnerId: () => 'owner' });
  await owner.claim('event-1', 'another-owner');

  await assert.rejects(
    owner.runOnce('event-1', async () => 'sent'),
    IdempotencyInProgressError,
  );
});

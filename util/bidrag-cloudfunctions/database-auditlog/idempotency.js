const crypto = require('node:crypto');

const DEFAULT_TTL_MS = 7 * 24 * 60 * 60 * 1000;
const DEFAULT_LEASE_MS = 2 * 60 * 1000;
const DEFAULT_COLLECTION_NAME = 'database-auditlog-deliveries';
const DEFAULT_FALLBACK_TTL_MS = 24 * 60 * 60 * 1000;
const DEFAULT_FALLBACK_MAX_ENTRIES = 10_000;

function timestampMillis(value) {
  if (value instanceof Date) return value.getTime();
  if (typeof value?.toMillis === 'function') return value.toMillis();
  return new Date(value).getTime();
}

class IdempotencyStore {
  constructor({
    firestore,
    collectionName = DEFAULT_COLLECTION_NAME,
    ttlMs = DEFAULT_TTL_MS,
    leaseMs = DEFAULT_LEASE_MS,
    now = Date.now,
    createOwnerId = crypto.randomUUID,
    fallbackStore = new MemoryIdempotencyStore(),
    onFallback = () => {},
  }) {
    if (!firestore) throw new TypeError('firestore is required');
    this.firestore = firestore;
    this.collectionName = collectionName;
    this.ttlMs = ttlMs;
    this.leaseMs = leaseMs;
    this.now = now;
    this.createOwnerId = createOwnerId;
    this.fallbackStore = fallbackStore;
    this.onFallback = onFallback;
  }

  async runOnce(key, operation) {
    const ownerId = this.createOwnerId();
    let claim;
    try {
      claim = await this.claim(key, ownerId);
    } catch (error) {
      this.onFallback(error);
      return this.fallbackStore.runOnce(key, operation);
    }

    if (!claim.claimed) return { executed: false };

    let value;
    try {
      value = await operation();
    } catch (error) {
      try {
        await this.release(key, ownerId);
      } catch (releaseError) {
        throw new AggregateError([error, releaseError], 'Operation failed and Firestore release failed');
      }
      throw error;
    }

    await this.complete(key, ownerId);
    return { executed: true, value };
  }

  document(key) {
    const documentId = crypto.createHash('sha256').update(key).digest('hex');
    return this.firestore.collection(this.collectionName).doc(documentId);
  }

  async claim(key, ownerId) {
    const reference = this.document(key);
    const now = this.now();

    return this.firestore.runTransaction(async transaction => {
      const snapshot = await transaction.get(reference);
      const data = snapshot.exists ? snapshot.data() : null;

      if (data?.status === 'completed') return { claimed: false };
      if (data?.status === 'processing' && timestampMillis(data.leaseUntil) > now) {
        return { claimed: false };
      }

      const expiresAt = new Date(now + this.ttlMs);
      const leaseUntil = new Date(now + this.leaseMs);

      if (snapshot.exists) {
        transaction.update(reference, {
          status: 'processing',
          ownerId,
          leaseUntil,
          expiresAt,
        });
      } else {
        transaction.create(reference, {
          status: 'processing',
          ownerId,
          leaseUntil,
          expiresAt,
          createdAt: new Date(now),
        });
      }

      return { claimed: true };
    });
  }

  async complete(key, ownerId) {
    const reference = this.document(key);

    await this.firestore.runTransaction(async transaction => {
      const snapshot = await transaction.get(reference);
      if (!snapshot.exists) return;

      const data = snapshot.data();
      if (data.status !== 'processing' || data.ownerId !== ownerId) return;

      transaction.update(reference, {
        status: 'completed',
        leaseUntil: null,
        completedAt: new Date(this.now()),
      });
    });
  }

  async release(key, ownerId) {
    const reference = this.document(key);

    await this.firestore.runTransaction(async transaction => {
      const snapshot = await transaction.get(reference);
      if (!snapshot.exists) return;

      const data = snapshot.data();
      if (data.status === 'processing' && data.ownerId === ownerId) {
        transaction.delete(reference);
      }
    });
  }
}

class MemoryIdempotencyStore {
  constructor({
    ttlMs = DEFAULT_FALLBACK_TTL_MS,
    maxEntries = DEFAULT_FALLBACK_MAX_ENTRIES,
    now = Date.now,
  } = {}) {
    this.ttlMs = ttlMs;
    this.maxEntries = maxEntries;
    this.now = now;
    this.entries = new Map();
  }

  async runOnce(key, operation) {
    this.pruneExpired();

    const existing = this.entries.get(key);
    if (existing) {
      await existing.promise;
      return { executed: false, value: existing.value };
    }

    while (this.entries.size >= this.maxEntries) {
      this.entries.delete(this.entries.keys().next().value);
    }

    const entry = {
      expiresAt: this.now() + this.ttlMs,
      promise: null,
      value: undefined,
    };
    entry.promise = Promise.resolve()
      .then(operation)
      .then(value => {
        entry.value = value;
        return value;
      })
      .catch(error => {
        if (this.entries.get(key) === entry) this.entries.delete(key);
        throw error;
      });
    this.entries.set(key, entry);

    return { executed: true, value: await entry.promise };
  }

  pruneExpired() {
    const now = this.now();
    for (const [key, entry] of this.entries) {
      if (entry.expiresAt <= now) this.entries.delete(key);
    }
  }
}

module.exports = {
  DEFAULT_COLLECTION_NAME,
  DEFAULT_FALLBACK_MAX_ENTRIES,
  DEFAULT_FALLBACK_TTL_MS,
  DEFAULT_LEASE_MS,
  DEFAULT_TTL_MS,
  IdempotencyStore,
  MemoryIdempotencyStore,
};

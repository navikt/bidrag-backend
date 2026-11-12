package no.nav.bidrag.cache.util

import com.github.benmanes.caffeine.cache.Cache
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics

const val VALKEY_OPPSLAG_METRIKK = "bidrag.cache.valkey.gets"
const val FEIL_METRIKK = "bidrag.cache.error"

// Samme tagger som Spring Boot bruker for cachemetrikker, slik at de kan vises sammen.
private const val CACHE_MANAGER_TAG = "cache.manager"

/** Registrerer Micrometers standard cachemetrikker (`cache.gets`, `cache.size`, `cache.evictions` m.fl.). */
fun MeterRegistry.overvåkCaffeine(
    cache: Cache<*, *>,
    navn: String,
    cacheManager: String,
) {
    CaffeineCacheMetrics.monitor(this, cache, navn, Tags.of(CACHE_MANAGER_TAG, cacheManager))
}

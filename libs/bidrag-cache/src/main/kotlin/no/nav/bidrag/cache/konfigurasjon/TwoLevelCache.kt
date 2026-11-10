package no.nav.bidrag.cache.konfigurasjon

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.Cache
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.AbstractValueAdaptingCache
import java.time.Clock
import java.time.Instant
import java.util.concurrent.Callable

private val LOGGER = KotlinLogging.logger { }

internal class TwoLevelCache(
    private val localCache: CaffeineCache,
    private val sharedCache: Cache,
    private val deadline: () -> Instant,
    private val clock: Clock,
) : AbstractValueAdaptingCache(false) {
    override fun getName(): String = sharedCache.name

    override fun getNativeCache(): Any = localCache.nativeCache

    override fun lookup(key: Any): Any? {
        readLocal(key)?.let { return it.verdi.also { LOGGER.info { "Traff lokal cache (caffeine)" } } }
        val entry = readShared(key) ?: return null
        // Legger treff fra delt cache over til caffeine
        localCache.put(key, entry)
        return entry.verdi.also { LOGGER.info { "Traff delt cache (valkey)" } }
    }

    /** Caffeines loader samordner samtidige kall for samme nøkkel innenfor en pod. */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: Any, valueLoader: Callable<T>): T {
        val entry = checkNotNull(
            readLocal(key) ?: localCache.get(
                key,
            ) {
                readShared(key) ?: newEntry(valueLoader.call()).also { sharedCache.put(key, it) }
            },
        )
        return entry.verdi as T
    }

    override fun put(key: Any, value: Any?) {
        // Fjern gammel caffine verdi først, og fyll bare tilbake etter vellykket Valkey-skriving.
        localCache.evict(key)
        val entry = newEntry(value)
        sharedCache.put(key, entry)
        localCache.put(key, entry)
    }

    override fun putIfAbsent(key: Any, value: Any?): Cache.ValueWrapper? {
        localCache.evict(key)
        val entry = newEntry(value)
        val existing = sharedCache.putIfAbsent(key, entry)
        val stored = if (existing == null) {
            entry
        } else {
            val stored = existing.get()
            check(stored is CachetVerdi) { "Ugyldig cacheformat" }
            stored
        }
        if (!stored.gjennværendeTtl(clock).isPositive) return null
        localCache.put(key, stored)
        return if (existing == null) null else toValueWrapper(stored.verdi)
    }

    // Spring krever evict og clear
    override fun evict(key: Any) {
        localCache.evict(key)
        sharedCache.evict(key)
    }

    override fun clear() {
        localCache.clear()
        sharedCache.clear()
    }

    private fun readLocal(key: Any): CachetVerdi? {
        // Kontroller fristen uavhengig av når Caffeine fysisk fjerner oppføringen.
        val entry = localCache.get(key)?.get() as? CachetVerdi? ?: return null
        if (entry.gjennværendeTtl(clock).isPositive) return entry
        localCache.evict(key)
        return null
    }

    private fun readShared(key: Any): CachetVerdi? {
        val entry = sharedCache.get(key)?.get() ?: return null
        check(entry is CachetVerdi) { "Ugyldig cacheformat" }
        // Valkey kan fortsatt ha en fysisk oppføring med passert logisk frist.
        return entry.takeIf { it.gjennværendeTtl(clock).isPositive }
    }

    // Beregn fristen én gang per ny verdi, slik at begge får samme utløpstidspunkt.
    private fun newEntry(value: Any?): CachetVerdi = CachetVerdi(toStoreValue(value), deadline().toEpochMilli())
}

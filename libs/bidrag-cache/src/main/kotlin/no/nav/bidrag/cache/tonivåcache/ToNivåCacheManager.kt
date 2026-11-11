package no.nav.bidrag.cache.tonivåcache

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.MeterRegistry
import no.nav.bidrag.cache.util.LANG_CACHE_MANAGER
import no.nav.bidrag.cache.util.Levetid
import no.nav.bidrag.cache.util.overvåkCaffeine
import no.nav.bidrag.cache.util.tidTilStartenAvNesteArbeidsdag
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import java.util.concurrent.ConcurrentHashMap

class ToNivåCacheManager(
    private val deltCacheManager: CacheManager,
    private val maksStørrelse: Long,
    private val metrikker: MeterRegistry,
) : CacheManager {
    private val cacher = ConcurrentHashMap<String, Cache>()

    override fun getCache(name: String): Cache = cacher.computeIfAbsent(name, ::opprettCache)

    override fun getCacheNames(): Collection<String> = cacher.keys.toSet()

    /** Tømmer bare Caffeine i denne podden. Valkey tømmes av [no.nav.bidrag.cache.tømming.CacheTømmer]. */
    fun tømLokalt(navn: String?) {
        cacher.filterKeys { navn == null || it == navn }.values.forEach { (it as ToNivåCache).tømLokalt() }
    }

    private fun opprettCache(name: String): Cache {
        val caffeine =
            Caffeine
                .newBuilder()
                .expireAfter(Levetid(::tidTilStartenAvNesteArbeidsdag))
                .maximumSize(maksStørrelse)
                .recordStats()
                .build<Any, Any>()
        metrikker.overvåkCaffeine(caffeine, name, LANG_CACHE_MANAGER)
        val delt = requireNotNull(deltCacheManager.getCache(name)) { "Fant ikke delt cache $name" }
        return ToNivåCache(CaffeineCache(name, caffeine), delt, metrikker)
    }
}

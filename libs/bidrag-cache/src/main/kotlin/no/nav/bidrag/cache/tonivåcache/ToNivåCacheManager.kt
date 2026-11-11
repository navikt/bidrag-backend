package no.nav.bidrag.cache.tonivåcache

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.bidrag.cache.util.tidTilStartenAvNesteArbeidsdag
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import java.util.concurrent.ConcurrentHashMap
import no.nav.bidrag.cache.util.Levetid

class ToNivåCacheManager(
    private val deltCacheManager: CacheManager,
    private val maksStørrelse: Long,
) : CacheManager {
    private val cacher = ConcurrentHashMap<String, Cache>()

    override fun getCache(name: String): Cache = cacher.computeIfAbsent(name, ::opprettCache)

    override fun getCacheNames(): Collection<String> = cacher.keys.toSet()

    /** Tømmer bare Caffeine i denne podden. Valkey tømmes av [no.nav.bidrag.cache.tømming.CacheTømmer]. */
    fun tømLokalt(navn: String?) {
        cacher.filterKeys { navn == null || it == navn }.values.forEach { (it as ToNivåCache).tømLokalt() }
    }

    private fun opprettCache(name: String): Cache {
        val lokal =
            CaffeineCache(
                name,
                Caffeine
                    .newBuilder()
                    .expireAfter(Levetid(::tidTilStartenAvNesteArbeidsdag))
                    .maximumSize(maksStørrelse)
                    .build(),
            )
        val delt = requireNotNull(deltCacheManager.getCache(name)) { "Fant ikke delt cache $name" }
        return ToNivåCache(lokal, delt)
    }
}

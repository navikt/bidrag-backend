package no.nav.bidrag.cache.konfigurasjon

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.ConcurrentHashMap
import no.nav.bidrag.cache.util.InvaliderCacheFørStartenAvArbeidsdag
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache

class ToNivåCacheManager(
    private val deltCacheManager: CacheManager,
    private val maksStørrelse: Long,
) : CacheManager {
    private val cacher = ConcurrentHashMap<String, Cache>()

    override fun getCache(name: String): Cache = cacher.computeIfAbsent(name, ::opprettCache)

    override fun getCacheNames(): Collection<String> = cacher.keys.toSet()

    private fun opprettCache(name: String): Cache {
        val lokal =
            CaffeineCache(
                name,
                Caffeine
                    .newBuilder()
                    .expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
                    .maximumSize(maksStørrelse)
                    .build(),
                false,
            )
        val delt = requireNotNull(deltCacheManager.getCache(name)) { "Fant ikke delt cache $name" }
        return ToNivåCache(lokal, delt)
    }
}

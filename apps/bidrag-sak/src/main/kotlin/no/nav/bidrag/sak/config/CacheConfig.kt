package no.nav.bidrag.sak.config

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.bidrag.commons.cache.EnableUserCache
import no.nav.bidrag.commons.cache.InvaliderCacheFørStartenAvArbeidsdag
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

const val CACHE_SAMMENKNYTNINGER_HOVEDSØKNAD = "sammenknytninger_hovedsoknad"

@Configuration
@EnableCaching
@EnableUserCache
class CacheConfig {
    @Bean
    fun cacheManager(): CacheManager = object : ConcurrentMapCacheManager() {
        override fun createConcurrentMapCache(name: String): Cache {
            val concurrentMap =
                Caffeine
                    .newBuilder()
                    .apply {
                        if (name == CACHE_SAMMENKNYTNINGER_HOVEDSØKNAD) {
                            expireAfterWrite(10, TimeUnit.MINUTES)
                        } else {
                            expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
                        }
                    }.recordStats()
                    .build<Any, Any>()
                    .asMap()
            return ConcurrentMapCache(name, concurrentMap, true)
        }
    }
}

fun <T : Any> CacheManager.getNullable(
    cache: String,
    key: String,
    valueLoader: Callable<T>,
): T? = (getCacheOrThrow(cache)).get(key, valueLoader)

fun <T : Any> CacheManager.getValue(
    cache: String,
    key: String,
    valueLoader: Callable<T>,
): T = this.getNullable(cache, key, valueLoader) ?: error("Finner ikke cache for cache=$cache key=$key")

fun CacheManager.getCacheOrThrow(cache: String) = this.getCache(cache) ?: error("Finner ikke cache=$cache")

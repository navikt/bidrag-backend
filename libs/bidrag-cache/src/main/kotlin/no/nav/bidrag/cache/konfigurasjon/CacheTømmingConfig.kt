package no.nav.bidrag.cache.konfigurasjon

import no.nav.bidrag.cache.tonivåcache.ToNivåCacheManager
import no.nav.bidrag.cache.tømming.CacheController
import no.nav.bidrag.cache.tømming.CacheTømmer
import no.nav.bidrag.cache.tømming.CacheTømmingLytter
import no.nav.bidrag.cache.util.KORT_CACHE_MANAGER
import no.nav.bidrag.cache.util.LANG_CACHE_MANAGER
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

/** Lastes automatisk når appen har bidrag-cache som avhengighet. */
@AutoConfiguration(after = [CacheConfig::class])
class CacheTømmingConfig {
    @Bean
    fun cacheTømmer(
        @Qualifier(KORT_CACHE_MANAGER) kortCacheManager: CacheManager,
        @Qualifier(LANG_CACHE_MANAGER) langCacheManager: ToNivåCacheManager,
        valkeyCacheWriter: RedisCacheWriter,
        valkeyConnectionFactory: RedisConnectionFactory,
    ): CacheTømmer = CacheTømmer(kortCacheManager, langCacheManager, valkeyCacheWriter, StringRedisTemplate(valkeyConnectionFactory))

    @Bean
    fun cacheTømmingLytter(
        valkeyConnectionFactory: RedisConnectionFactory,
        cacheTømmer: CacheTømmer,
    ): CacheTømmingLytter = CacheTømmingLytter(valkeyConnectionFactory, cacheTømmer)

    @Bean
    @ConditionalOnMissingBean
    fun cacheController(cacheTømmer: CacheTømmer): CacheController = CacheController(cacheTømmer)
}

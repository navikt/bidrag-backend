package no.nav.bidrag.cache.konfigurasjon

import no.nav.bidrag.cache.tømming.CacheController
import no.nav.bidrag.cache.tømming.CacheTømmer
import no.nav.bidrag.cache.tømming.CacheTømmingLytter
import no.nav.bidrag.cache.util.KORT_CACHE_MANAGER
import no.nav.bidrag.cache.util.LANG_CACHE_MANAGER
import no.nav.bidrag.cache.util.VALKEY_INSTANS
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

/** Lastes automatisk når appen har bidrag-cache som avhengighet. */
@AutoConfiguration(after = [CacheConfig::class])
class CacheTømmingConfig {
    // Valkey-bønnene finnes bare når appen har Valkey. Uten Valkey tømmes bare Caffeine i podden.
    @Bean
    fun cacheTømmer(
        @Qualifier(KORT_CACHE_MANAGER) kortCacheManager: CacheManager,
        @Qualifier(LANG_CACHE_MANAGER) langCacheManager: CacheManager,
        valkeyCacheWriter: ObjectProvider<RedisCacheWriter>,
        @Qualifier(VALKEY_CONNECTION_FACTORY) valkeyConnectionFactory: ObjectProvider<RedisConnectionFactory>,
    ): CacheTømmer {
        val valkey =
            valkeyCacheWriter.ifAvailable?.let { writer ->
                CacheTømmer.Valkey(writer, StringRedisTemplate(valkeyConnectionFactory.getObject()))
            }
        return CacheTømmer(kortCacheManager, langCacheManager, valkey)
    }

    @Bean
    @ConditionalOnProperty(VALKEY_INSTANS)
    fun cacheTømmingLytter(
        @Qualifier(VALKEY_CONNECTION_FACTORY) valkeyConnectionFactory: RedisConnectionFactory,
        cacheTømmer: CacheTømmer,
    ): CacheTømmingLytter = CacheTømmingLytter(valkeyConnectionFactory, cacheTømmer)

    // Apper som komponentskanner no.nav.bidrag får kontrolleren derfra.
    @Bean
    @ConditionalOnMissingBean
    fun cacheController(cacheTømmer: CacheTømmer): CacheController = CacheController(cacheTømmer)

    private companion object {
        const val VALKEY_CONNECTION_FACTORY = "valkeyConnectionFactory"
    }
}

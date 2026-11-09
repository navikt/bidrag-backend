package no.nav.bidrag.cache.konfigurasjon

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.github.benmanes.caffeine.cache.Ticker
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class TwoLevelCacheManager(
    private val settings: CacheSettings,
    applicationName: String,
    private val registry: MeterRegistry,
    writer: RedisCacheWriter,
    private val ticker: Ticker = Ticker.systemTicker(),
    private val clock: Clock = Clock.systemDefaultZone(),
) : CacheManager {
    private companion object {
        const val MAX_STØRRELSE = 10_000L
        const val CACHE_VERSJON = "v1"
        val TILLATTE_PAKKER = listOf("no.nav.bidrag.", "java.util.", "java.lang.", "kotlin.collections.")

        // TTL på 0 betyr ingen utløp i Valkey, selv når den logiske fristen er passert.
        val MINIMUM_VALKEY_TTL: Duration = Duration.ofMillis(1)
    }

    private val opprettedeCaches = ConcurrentHashMap<String, Cache>()
    private val deltCacheManager: RedisCacheManager

    init {
        require(applicationName.matches(Regex("[A-Za-z0-9._-]+"))) { "Ugyldig applikasjonsnavn for cache" }

        val allowedTypes = BasicPolymorphicTypeValidator.builder()
        TILLATTE_PAKKER.forEach { allowedTypes.allowIfSubType(it) }

        val configuration = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .computePrefixWith { "$applicationName::$CACHE_VERSJON::$it::" }
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    CacheValueSerializer(
                        GenericJacksonJsonRedisSerializer.builder().enableDefaultTyping(allowedTypes.build()).build(),
                    ),
                ),
            )
            .entryTtl { _, entry ->
                check(entry is CachetVerdi) { "Ugyldig cacheformat" }
                maxOf(entry.gjennværendeTtl(clock), MINIMUM_VALKEY_TTL)
            }
        deltCacheManager = RedisCacheManager.builder(writer).cacheDefaults(configuration).build().apply { afterPropertiesSet() }
    }

    override fun getCache(name: String): Cache = opprettedeCaches.computeIfAbsent(name, ::createCache)

    override fun getCacheNames(): Collection<String> = opprettedeCaches.keys.toSet()

    private fun createCache(name: String): Cache {
        val expiry = requireNotNull(settings.cacher[name]) { "Cache mangler utløpsregel" }
        val caffeineCache = Caffeine.newBuilder()
            .maximumSize(MAX_STØRRELSE)
            .expireAfter(
                // Tilbakefylling fra Valkey bruker bare den gjenværende levetiden.
                Expiry.writing { _, entry ->
                    check(entry is CachetVerdi) { "Ugyldig cacheformat" }
                    maxOf(Duration.ZERO, entry.gjennværendeTtl(clock))
                },
            )
            .ticker(ticker)
            .recordStats()
            .build<Any, Any>()
        CaffeineCacheMetrics.monitor(registry, caffeineCache, "$name-caffeine")
        return TwoLevelCache(
            CaffeineCache(name, caffeineCache, false),
            checkNotNull(deltCacheManager.getCache(name)),
            { expiry.deadline(clock) },
            clock,
        )
    }
}

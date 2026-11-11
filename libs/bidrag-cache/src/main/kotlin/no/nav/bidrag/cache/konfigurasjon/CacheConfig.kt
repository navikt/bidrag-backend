package no.nav.bidrag.cache.konfigurasjon

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.bidrag.cache.nøkkelgenerering.CacheBrukerIdentifikator
import no.nav.bidrag.cache.nøkkelgenerering.CacheNøkkelGenerator
import no.nav.bidrag.cache.tonivåcache.ToNivåCacheManager
import no.nav.bidrag.cache.util.KORT_CACHE_MANAGER
import no.nav.bidrag.cache.util.LANG_CACHE_MANAGER
import no.nav.bidrag.cache.util.Levetid
import no.nav.bidrag.cache.util.NØKKELGENERATOR
import no.nav.bidrag.cache.util.NØKKELPREFIKS
import no.nav.bidrag.cache.util.finnLevetid
import no.nav.bidrag.cache.util.tidTilStartenAvNesteArbeidsdag
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.core.env.getRequiredProperty
import org.springframework.data.redis.cache.BatchStrategies
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.module.kotlin.KotlinModule
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {

    private companion object {
        // "[" er JVM-prefikset for arrayer, feks `[B` for ByteArray.
        val TILLATTE_PAKKER = listOf("no.nav.", "java.util.", "java.lang.", "java.time.", "java.math.", "kotlin.", "[")
        val KORT_CACHE_LEVETID: Duration = Duration.ofMinutes(10)
        const val MAKS_STØRRELSE = 10_000L
        const val SCAN_BATCHSTØRRELSE = 1_000
    }

    @Bean(NØKKELGENERATOR)
    fun bidragCacheKeyGenerator(brukerIdentifikator: ObjectProvider<CacheBrukerIdentifikator>): CacheNøkkelGenerator = CacheNøkkelGenerator(brukerIdentifikator)

    @Bean(KORT_CACHE_MANAGER)
    @Primary
    fun kortCacheManager(): CaffeineCacheManager = CaffeineCacheManager().apply {
        setCaffeine(
            Caffeine
                .newBuilder()
                .expireAfter(Levetid { KORT_CACHE_LEVETID })
                .maximumSize(MAKS_STØRRELSE),
        )
    }

    @Bean(LANG_CACHE_MANAGER)
    fun langCacheManager(valkeyCacheManager: RedisCacheManager): ToNivåCacheManager = ToNivåCacheManager(valkeyCacheManager, MAKS_STØRRELSE)

    @Bean
    fun valkeyConnectionFactory(
        @Value($$"${CACHE_VALKEY_INSTANCE}") instans: String,
        @Value($$"${bidrag.cache.valkey.tidsavbrudd:500ms}") tidsavbrudd: Duration,
        environment: Environment,
    ): LettuceConnectionFactory {
        // Nais eksponerer instansen med store bokstaver og bindestrek erstattet med understrek.
        val suffiks = instans.uppercase().replace('-', '_')
        val serverKonfigurasjon =
            RedisStandaloneConfiguration(
                environment.getRequiredProperty("VALKEY_HOST_$suffiks"),
                environment.getRequiredProperty<Int>("VALKEY_PORT_$suffiks"),
            ).apply {
                username = environment.getRequiredProperty("VALKEY_USERNAME_$suffiks")
                setPassword(environment.getRequiredProperty("VALKEY_PASSWORD_$suffiks"))
            }
        val klientKonfigurasjon =
            LettuceClientConfiguration
                .builder()
                .useSsl()
                .and()
                .commandTimeout(tidsavbrudd)
                .build()

        return LettuceConnectionFactory(serverKonfigurasjon, klientKonfigurasjon)
    }

    @Bean
    fun valkeyCacheWriter(valkeyConnectionFactory: RedisConnectionFactory): RedisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(valkeyConnectionFactory, BatchStrategies.scan(SCAN_BATCHSTØRRELSE))

    @Bean
    fun valkeyCacheManager(
        valkeyCacheWriter: RedisCacheWriter,
        redisCacheConfig: RedisCacheConfiguration,
    ): RedisCacheManager = RedisCacheManager
        .builder(valkeyCacheWriter)
        .cacheDefaults(redisCacheConfig)
        .build()

    @Bean
    fun redisCacheConfig(): RedisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
        .prefixCacheNameWith(NØKKELPREFIKS)
        .entryTtl { nøkkel, _ -> finnLevetid(nøkkel, ::tidTilStartenAvNesteArbeidsdag) }
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer()))

    private fun redisValueSerializer(): GenericJacksonJsonRedisSerializer {
        val tillatteTyper = BasicPolymorphicTypeValidator.builder()
        TILLATTE_PAKKER.forEach { tillatteTyper.allowIfSubType(it) }

        return GenericJacksonJsonRedisSerializer
            .builder()
            .enableDefaultTyping(tillatteTyper.build())
            // Skriver som Object slik at også final types som Long, LocalDate og enum får typeinformasjon.
            .writer { mapper, verdi -> mapper.writerFor(Any::class.java).writeValueAsBytes(verdi) }
            .customize { it.addModule(KotlinModule.Builder().build()) }
            .build()
    }
}

package no.nav.bidrag.cache.konfigurasjon

import io.lettuce.core.ClientOptions
import io.lettuce.core.SocketOptions
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.data.redis.cache.BatchStrategies
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import java.time.Duration
import java.util.Locale

@AutoConfiguration(
    after = [MetricsAutoConfiguration::class],
    before = [DataRedisAutoConfiguration::class, CacheAutoConfiguration::class],
)
@ConditionalOnBean(CacheSettings::class)
@EnableCaching
class BidragCacheConfiguration {
    private companion object {
        val VALKEY_TIMEOUT: Duration = Duration.ofMillis(500)
    }

    @Bean("redisConnectionFactory")
    fun valkeyConnection(settings: CacheSettings, environment: Environment): LettuceConnectionFactory {
        val instance = settings.valkeyInstans.uppercase(Locale.ROOT)
        val valkeyConfig = RedisStandaloneConfiguration(
            environment.getRequiredProperty("VALKEY_HOST_$instance"),
            environment.getRequiredProperty("VALKEY_PORT_$instance").toInt(),
        ).apply {
            username = environment.getRequiredProperty("VALKEY_USERNAME_$instance")
            password = RedisPassword.of(environment.getRequiredProperty("VALKEY_PASSWORD_$instance"))
        }

        val clientConfig = LettuceClientConfiguration.builder()
            .clientOptions(
                ClientOptions.builder()
                    .socketOptions(SocketOptions.builder().connectTimeout(VALKEY_TIMEOUT).build())
                    .build(),
            )
            .commandTimeout(VALKEY_TIMEOUT)
            .useSsl()
            .build()
        return LettuceConnectionFactory(valkeyConfig, clientConfig)
    }

    @Bean(BrukerCacheKeyGenerator.BEAN_NAME)
    fun brukerCacheKeyGenerator(settings: CacheSettings, environment: Environment): KeyGenerator = BrukerCacheKeyGenerator(
        environment.getRequiredProperty("VALKEY_PASSWORD_${settings.valkeyInstans.uppercase(Locale.ROOT)}"),
        settings.brukerId,
        settings.nøkkelprefix,
    )

    @Bean("cacheManager")
    fun cacheManager(settings: CacheSettings, environment: Environment, registry: MeterRegistry, connection: RedisConnectionFactory): CacheManager = TwoLevelCacheManager(
        settings,
        environment.getRequiredProperty("spring.application.name"),
        registry,
        // Vent på Valkey-skriving før Caffeine fylles, SCAN unngår blokkerende KEYS ved clear
        RedisCacheWriter.create(connection) { it.immediateWrites().batchStrategy(BatchStrategies.scan(1000)) },
    )

    @Bean("cacheErrorHandler")
    fun cacheErrorHandler(registry: MeterRegistry): CacheErrorHandler = CacheErrorHandler(registry)

    /** Sørger for at @Cacheable bruker bibliotekets nøkler. */
    @Bean("cachingConfigurer")
    fun cachingConfigurer(
        cacheManager: CacheManager,
        @Qualifier(BrukerCacheKeyGenerator.BEAN_NAME) keyGenerator: KeyGenerator,
        errorHandler: CacheErrorHandler,
    ): CachingConfigurer = object : CachingConfigurer {
        override fun cacheManager(): CacheManager = cacheManager

        override fun keyGenerator(): KeyGenerator = keyGenerator

        override fun errorHandler(): org.springframework.cache.interceptor.CacheErrorHandler = errorHandler
    }
}

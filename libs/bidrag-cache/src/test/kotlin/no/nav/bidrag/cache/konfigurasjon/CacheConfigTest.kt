package no.nav.bidrag.cache.konfigurasjon

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.bidrag.cache.annotasjoner.KortCache
import no.nav.bidrag.cache.annotasjoner.LangCache
import no.nav.bidrag.cache.tonivåcache.ToNivåCacheManager
import no.nav.bidrag.cache.tømming.CacheTømmer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory

class CacheConfigTest {
    private companion object {
        const val REDIS_HELSESJEKK_BEAN = "redisHealthContributor"
        const val EGEN_CACHE = "egen"
    }

    open class Tjeneste {
        open var kall = 0

        @KortCache("kort", perBruker = false)
        open fun kort(id: String): String = "kort$id".also { kall++ }

        @LangCache("lang", perBruker = false)
        open fun lang(id: String): String = "lang$id".also { kall++ }
    }

    @Configuration
    @EnableAutoConfiguration
    class App {
        @Bean
        fun tjeneste() = Tjeneste()
    }

    open class TjenesteMedEgenCache {
        @Cacheable(EGEN_CACHE)
        open fun hent(id: String): String = id
    }

    @Configuration
    @EnableAutoConfiguration
    class AppMedEgenCacheManager {
        @Bean
        fun tjeneste() = TjenesteMedEgenCache()

        @Bean
        fun egenCacheManager(): CacheManager = ConcurrentMapCacheManager()
    }

    @Configuration
    @EnableAutoConfiguration
    class AppUtenEgenCacheManager {
        @Bean
        fun tjeneste() = TjenesteMedEgenCache()
    }

    private val valkeyEgenskaper =
        arrayOf(
            "CACHE_VALKEY_INSTANCE=cache-test",
            "VALKEY_HOST_CACHE_TEST=127.0.0.1",
            "VALKEY_PORT_CACHE_TEST=1",
            "VALKEY_USERNAME_CACHE_TEST=bruker",
            "VALKEY_PASSWORD_CACHE_TEST=passord",
        )

    private fun start(vararg egenskaper: String): ConfigurableApplicationContext = start(App::class.java, *egenskaper)

    private fun start(
        app: Class<*>,
        vararg egenskaper: String,
    ): ConfigurableApplicationContext = SpringApplicationBuilder(app)
        .web(WebApplicationType.NONE)
        .properties("spring.application.name=test", *egenskaper)
        .run()

    @Test
    fun `skal slå av helsesjekk for redis som standard`() {
        start(*valkeyEgenskaper).use { kontekst ->
            kontekst.containsBean(REDIS_HELSESJEKK_BEAN) shouldBe false
        }
    }

    @Test
    fun `skal la appen slå på helsesjekk for redis`() {
        SpringApplicationBuilder(App::class.java)
            .web(WebApplicationType.NONE)
            .properties("spring.application.name=test", *valkeyEgenskaper)
            // Som i application.yaml må overstyringen ligge foran defaultProperties, som Spring Boot legger bakerst.
            .run("--management.health.redis.enabled=true")
            .use { kontekst ->
                kontekst.containsBean(REDIS_HELSESJEKK_BEAN) shouldBe true
            }
    }

    @Test
    fun `skal cache med KortCache uten valkey`() {
        start().use { kontekst ->
            val tjeneste = kontekst.getBean<Tjeneste>()

            tjeneste.kort("1")
            tjeneste.kort("1")

            tjeneste.kall shouldBe 1
            kontekst.getBeanNamesForType(RedisConnectionFactory::class.java).toList().shouldBeEmpty()
        }
    }

    @Test
    fun `skal cache LangCache bare i caffeine uten valkey`() {
        start().use { kontekst ->
            val tjeneste = kontekst.getBean<Tjeneste>()

            tjeneste.lang("1")
            tjeneste.lang("1")

            tjeneste.kall shouldBe 1
            kontekst.getBean("langCacheManager").shouldBeInstanceOf<CaffeineCacheManager>()
        }
    }

    @Test
    fun `skal hente på nytt etter tømming`() {
        start().use { kontekst ->
            val tjeneste = kontekst.getBean<Tjeneste>()
            tjeneste.kort("1")

            kontekst.getBean<CacheTømmer>().tøm(null)
            tjeneste.kort("1")

            tjeneste.kall shouldBe 2
        }
    }

    @Test
    fun `skal bruke valkey når instansen er satt`() {
        start(*valkeyEgenskaper).use { kontekst ->
            kontekst.getBean("langCacheManager").shouldBeInstanceOf<ToNivåCacheManager>()
            kontekst.getBeanNamesForType(RedisConnectionFactory::class.java).toList() shouldBe listOf("valkeyConnectionFactory")
        }
    }

    @Test
    fun `skal bruke appens egen CacheManager for vanlig Cacheable`() {
        start(AppMedEgenCacheManager::class.java).use { kontekst ->
            kontekst.getBean<TjenesteMedEgenCache>().hent("1")

            kontekst.getBean<CacheManager>("egenCacheManager").cacheNames shouldBe listOf(EGEN_CACHE)
        }
    }

    @Test
    fun `skal bruke appens egen CacheManager for vanlig Cacheable med valkey`() {
        start(AppMedEgenCacheManager::class.java, *valkeyEgenskaper).use { kontekst ->
            kontekst.getBean<TjenesteMedEgenCache>().hent("1")

            kontekst.getBean<CacheManager>("egenCacheManager").cacheNames shouldBe listOf(EGEN_CACHE)
            kontekst.getBean("langCacheManager").shouldBeInstanceOf<ToNivåCacheManager>()
        }
    }

    @Test
    fun `skal bruke caffeine for vanlig Cacheable med valkey`() {
        start(AppUtenEgenCacheManager::class.java, *valkeyEgenskaper).use { kontekst ->
            kontekst.getBean<TjenesteMedEgenCache>().hent("1")

            kontekst.getBean<CacheManager>().shouldBeInstanceOf<CaffeineCacheManager>().cacheNames shouldBe listOf(EGEN_CACHE)
        }
    }

    @Test
    fun `skal la appen velge cachetype for vanlig Cacheable`() {
        SpringApplicationBuilder(AppUtenEgenCacheManager::class.java)
            .web(WebApplicationType.NONE)
            .properties("spring.application.name=test", *valkeyEgenskaper)
            .run("--spring.cache.type=redis")
            .use { kontekst ->
                kontekst.getBean<CacheManager>().shouldBeInstanceOf<RedisCacheManager>()
            }
    }
}

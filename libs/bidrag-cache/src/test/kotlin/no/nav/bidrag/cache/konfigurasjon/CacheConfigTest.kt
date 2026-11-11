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
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory

class CacheConfigTest {
    private companion object {
        const val REDIS_HELSESJEKK_BEAN = "redisHealthContributor"
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

    private val valkeyEgenskaper =
        arrayOf(
            "CACHE_VALKEY_INSTANCE=cache-test",
            "VALKEY_HOST_CACHE_TEST=127.0.0.1",
            "VALKEY_PORT_CACHE_TEST=1",
            "VALKEY_USERNAME_CACHE_TEST=bruker",
            "VALKEY_PASSWORD_CACHE_TEST=passord",
        )

    private fun start(vararg egenskaper: String): ConfigurableApplicationContext = SpringApplicationBuilder(App::class.java)
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
}

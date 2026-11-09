package no.nav.bidrag.cache.konfigurasjon

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.getBean

class BidragCacheConfigurationTest {
    private val klokke = Clock.fixed(Instant.parse("2026-01-02T05:30:00Z"), ZoneId.of("UTC"))
    private val regler = mapOf(
        "SHORT" to CacheExpiry(ttl = Duration.ofSeconds(2)),
        "LONG" to CacheExpiry(expireAt = LocalTime.of(6, 0)),
    )
    private val settings = CacheSettings("test", regler, { "Z999999" })
    private val kontekst = ApplicationContextRunner()
        .withClassLoader(FilteredClassLoader("no.nav.bidrag.commons", "no.nav.bidrag.domene", "no.nav.bidrag.transport"))
        .withConfiguration(
            AutoConfigurations.of(
                BidragCacheConfiguration::class.java,
                DataRedisAutoConfiguration::class.java,
                CacheAutoConfiguration::class.java,
            ),
        )
        .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
        .withPropertyValues(
            "spring.application.name=test-app",
            "VALKEY_HOST_TEST=localhost",
            "VALKEY_PORT_TEST=26483",
            "VALKEY_USERNAME_TEST=test",
            "VALKEY_PASSWORD_TEST=testhemmelighet",
        )

    @Test
    fun `skal aktivere hele bidrag-cache biblioteket med en settings bean`() {
        kontekst.withBean(CacheSettings::class.java, { settings }).run { app ->
            app.startupFailure shouldBe null
            app.getBean<LettuceConnectionFactory>().also {
                it.hostName shouldBe "localhost"
                it.clientConfiguration.commandTimeout shouldBe Duration.ofMillis(500)
                it.clientConfiguration.isUseSsl shouldBe true
            }
            app.getBeansOfType(CacheSettings::class.java).values.toList() shouldBe listOf(settings)

            val manager = app.getBean<CacheManager>().shouldBeInstanceOf<TwoLevelCacheManager>().also {
                it.getCache("LONG").nativeCache.shouldBeInstanceOf<com.github.benmanes.caffeine.cache.Cache<*, *>>()
                    .policy().eviction().orElseThrow().maximum shouldBe 10_000L
            }

            val keyGenerator = app.getBean<KeyGenerator>(BrukerCacheKeyGenerator.BEAN_NAME).shouldBeInstanceOf<BrukerCacheKeyGenerator>()
            val configurer = app.getBean<CachingConfigurer>()
            configurer.cacheManager() shouldBe manager
            configurer.keyGenerator() shouldBe keyGenerator
            configurer.errorHandler() shouldBe app.getBean(CacheErrorHandler::class.java)
        }
    }

    @Test
    fun `skal stoppe oppstart hvis Valkey mangler config`() {
        kontekst.withBean(CacheSettings::class.java, { settings.copy(valkeyInstans = "manglende config") }).run { app ->
            app.startupFailure.shouldNotBeNull()
        }
    }

    @Test
    fun `skal kaste feil ved ugyldige cache utløpsverdier`() {
        shouldThrow<IllegalArgumentException> { CacheExpiry() }
        shouldThrow<IllegalArgumentException> { CacheExpiry(ttl = Duration.ZERO) }
        shouldThrow<IllegalArgumentException> { CacheExpiry(ttl = Duration.ofSeconds(-1)) }
        shouldThrow<IllegalArgumentException> { CacheExpiry(ttl = Duration.ofSeconds(1), expireAt = LocalTime.NOON) }
    }

    @Test
    fun `skal opprette lokale cacher ved første bruk og avvise ukjente cacher`() {
        val writer = valkeySkriver()
        val manager = cachebehandler(writer)

        manager.cacheNames shouldBe emptySet()
        listOf("SHORT", "LONG").forEach { navn ->
            val cache = manager.getCache(navn)
            (manager.getCache(navn) === cache) shouldBe true
            cache.put("key", "value")
            cache.get("key").shouldNotBeNull().get() shouldBe "value"
        }
        manager.cacheNames shouldBe setOf("SHORT", "LONG")
        shouldThrow<IllegalArgumentException> { manager.getCache("UKJENT") }.message shouldBe "Cache mangler utløpsregel"
        manager.cacheNames shouldBe setOf("SHORT", "LONG")
        verify(exactly = 0) { writer.get(any(), any()) }
        verify { writer.put("SHORT", any(), any(), Duration.ofSeconds(2)) }
        verify { writer.put("LONG", any(), any(), Duration.ofHours(24).plusMinutes(30)) }
    }

    @Test
    fun `skal dele samme lokale cache ved samtidige førstegangsoppslag`() {
        val registry = SimpleMeterRegistry()
        val writer = valkeySkriver()
        val manager = TwoLevelCacheManager(settings, "test", registry, writer, clock = klokke)
        val start = CountDownLatch(1)
        val antallTråder = 32
        val caches = Executors.newFixedThreadPool(8).use { threads ->
            val futures = List(antallTråder) {
                threads.submit<Cache> {
                    start.await()
                    manager.getCache("LONG")
                }
            }
            start.countDown()
            futures.map { it.get(10, TimeUnit.SECONDS) }
        }
        val cache = caches.first()
        cache.put("key", "value")
        caches.forEach {
            (it === cache) shouldBe true
            it.get("key").shouldNotBeNull().get() shouldBe "value"
        }
        registry.get("cache.gets").tag("cache", "LONG-caffeine").tag("result", "hit").functionCounter().count() shouldBe antallTråder.toLong()
        verify(exactly = 0) { writer.get(any(), any()) }
    }

    @Test
    fun `skal lage forskjellige nøkler for strenger, tall, boolske verdier, enum og null`() {
        val generator = BrukerCacheKeyGenerator("testhemmelighet", settings.brukerId)
        val other = BrukerCacheKeyGenerator("testhemmelighet", settings.brukerId)
        val method = TestKilde::class.java.getMethod("hent", Any::class.java)
        val values = listOf("1", 1, 1L, true, TestVerdi.FØRSTE, null, emptyList<String>())
        val keys = values.map { generator.generate(TestKilde(), method, it) }
        keys.toSet().size shouldBe values.size
        keys shouldBe values.map { other.generate(TestKilde(), method, it) }
    }

    @Test
    fun `skal lese brukerident ved hvert oppslag og avvise manglende identitet før cacheoppslag`() {
        var ident: String? = null
        kontekst.withBean(CacheSettings::class.java, { settings.copy(brukerId = { ident }) }).run { app ->
            app.startupFailure shouldBe null
            val generator = app.getBean<KeyGenerator>(BrukerCacheKeyGenerator.BEAN_NAME)
            val method = TestKilde::class.java.getMethod("hent", Any::class.java)
            ident = "Z999999"
            val first = generator.generate(TestKilde(), method, "verdi")
            ident = "Z888888"
            first shouldNotBe generator.generate(TestKilde(), method, "verdi")
            listOf(null, "", " ", "\t").forEach {
                ident = it
                shouldThrow<IllegalStateException> { generator.generate(TestKilde(), method, "verdi") }
                    .message shouldBe "Mangler brukeridentitet for brukerbasert cache"
            }
        }
    }

    @Test
    fun `skal avvise ukjente nøkkelparametere uten å vise innholdet`() {
        val generator = BrukerCacheKeyGenerator("testhemmelighet", settings.brukerId)
        val method = TestKilde::class.java.getMethod("hent", Any::class.java)
        shouldThrow<IllegalArgumentException> { generator.generate(TestKilde(), method, EgenParameter("sensitiv verdi")) }
            .message shouldBe "Ukjent type for cache-nøkkel: ${EgenParameter::class.java.name}"
    }

    private fun cachebehandler(writer: RedisCacheWriter) = TwoLevelCacheManager(settings, "test", SimpleMeterRegistry(), writer, clock = klokke)

    private fun valkeySkriver(): RedisCacheWriter {
        val writer = mockk<RedisCacheWriter>(relaxed = true)
        val values = mutableMapOf<String, ByteArray>()
        every { writer.put(any(), any(), any(), any()) } answers { values[secondArg<ByteArray>().decodeToString()] = thirdArg() }
        every { writer.get(any(), any()) } answers { values[secondArg<ByteArray>().decodeToString()] }
        return writer
    }

    class TestKilde {
        fun hent(verdi: Any?): Any? = verdi
    }

    enum class TestVerdi { FØRSTE }

    data class EgenParameter(val verdi: String)
}

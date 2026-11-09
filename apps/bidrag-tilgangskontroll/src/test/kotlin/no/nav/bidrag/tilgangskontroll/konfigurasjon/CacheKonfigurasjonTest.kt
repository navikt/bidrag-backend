package no.nav.bidrag.tilgangskontroll.konfigurasjon

import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.bidrag.cache.konfigurasjon.BidragCacheConfiguration
import no.nav.bidrag.cache.konfigurasjon.CacheSettings
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.cache.CacheManager
import java.time.Duration
import java.time.LocalTime
import org.springframework.beans.factory.getBean

class CacheKonfigurasjonTest {

    @Test
    fun `skal aktivere cache fra kode i begge Nais profiler`() {
        listOf("nais", "lokal-nais").forEach { profile ->
            ApplicationContextRunner()
                .withInitializer(ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(BidragCacheConfiguration::class.java))
                .withUserConfiguration(CacheKonfigurasjon::class.java)
                .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
                .withPropertyValues(
                    "spring.profiles.active=$profile",
                    "CACHE_VALKEY_INSTANCE=test",
                    "VALKEY_HOST_TEST=localhost",
                    "VALKEY_PORT_TEST=26483",
                    "VALKEY_USERNAME_TEST=test",
                    "VALKEY_PASSWORD_TEST=testhemmelighet",
                )
                .run { app ->
                    app.startupFailure shouldBe null

                    val settings = app.getBean<CacheSettings>()
                    settings.valkeyInstans shouldBe "test"
                    settings.cacher shouldBe Cache.innstillinger

                    val manager = app.getBean<CacheManager>()
                    (cacherMedArbeidsdagsutløp + Cache.PIP_SAK).forEach { manager.getCache(it) }
                    manager.cacheNames.toSet() shouldBe cacherMedArbeidsdagsutløp + Cache.PIP_SAK
                }
        }
    }

    @Test
    fun `skal bevare alle utløpsregler i Cache uten globale overstyringer`() {
        val settings = CacheKonfigurasjon().cacheSettings("egen-instans")
        settings.valkeyInstans shouldBe "egen-instans"
        settings.cacher.keys shouldBe cacherMedArbeidsdagsutløp + Cache.PIP_SAK
        settings.cacher.getValue(Cache.PIP_SAK).ttl shouldBe Duration.ofSeconds(10)
        settings.cacher.getValue(Cache.PIP_SAK).expireAt shouldBe null
        cacherMedArbeidsdagsutløp.forEach {
            settings.cacher.getValue(it).expireAt shouldBe LocalTime.of(6, 0)
            settings.cacher.getValue(it).ttl shouldBe null
        }
    }
}

// Forventningslisten hører hjemme i testkode, ikke som en ekstra cacheoversikt i produksjonskoden.
internal val cacherMedArbeidsdagsutløp = setOf(
    Cache.TILGANGSMASKIN_KJERNEREGLER,
    Cache.TILGANGSMASKIN_KJERNEREGLER_BULK,
    Cache.TILGANGSMASKIN_KOMPLETTEREGLER,
    Cache.TILGANGSMASKIN_KOMPLETTEREGLER_BULK,
    Cache.BRUKERINFO,
    Cache.BRUKERGRUPPER,
    Cache.BRUKERENHETER,
    Cache.GRUPPE_DETALJER,
    Cache.BRUKERE_FOR_ENHET,
    Cache.GRUPPE_DETALJER_TEMA,
)

package no.nav.bidrag.tilgangskontroll.konfigurasjon

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import no.nav.bidrag.cache.konfigurasjon.BidragCacheConfiguration
import no.nav.bidrag.cache.konfigurasjon.BrukerCacheKeyGenerator
import no.nav.bidrag.cache.konfigurasjon.CacheExpiry
import no.nav.bidrag.cache.konfigurasjon.CacheSettings
import no.nav.bidrag.cache.konfigurasjon.TwoLevelCacheManager
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.generer.testdata.sak.genererSaksnummer
import no.nav.bidrag.tilgangskontroll.model.graph.BrukerEnheterRespons
import no.nav.bidrag.tilgangskontroll.model.graph.BrukerGrupperResponse
import no.nav.bidrag.tilgangskontroll.model.graph.Brukerinformasjon
import no.nav.bidrag.tilgangskontroll.model.graph.BrukerinformasjonResponse
import no.nav.bidrag.tilgangskontroll.model.graph.Gruppe
import no.nav.bidrag.tilgangskontroll.model.tilgangsmaskin.TilgangsmaskinBulkResponse
import no.nav.bidrag.tilgangskontroll.model.tilgangsmaskin.TilgangsmaskinResultat
import no.nav.bidrag.tilgangskontroll.model.tilgangsmaskin.TilgangsmaskinResultatDetaljer
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.ProxyFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.AnnotationCacheOperationSource
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.interceptor.CacheInterceptor
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class ValkeyCacheTest {
    private val verdier = mutableMapOf<String, ByteArray>()
    private val valkeySkriver = mockk<RedisCacheWriter>(relaxed = true)
    private val settings = CacheKonfigurasjon().cacheSettings("test")
    private val klokke = Clock.fixed(Instant.parse("2026-01-02T05:30:00Z"), ZoneId.of("UTC"))

    init {
        every { valkeySkriver.get(any(), any()) } answers { verdier[secondArg<ByteArray>().decodeToString()] }
        every { valkeySkriver.put(any(), any(), any(), any()) } answers {
            verdier[secondArg<ByteArray>().decodeToString()] = thirdArg()
        }
    }

    @Test
    fun `skal koble appens Valkey-innstillinger til bibliotekets beans`() {
        AnnotationConfigApplicationContext().use { kontekst ->
            kontekst.environment.setActiveProfiles("nais")
            kontekst.environment.propertySources.addFirst(
                MapPropertySource(
                    "valkey",
                    mapOf(
                        "CACHE_VALKEY_INSTANCE" to "tilgangskontrollq1",
                        "VALKEY_HOST_TILGANGSKONTROLLQ1" to "localhost",
                        "VALKEY_PORT_TILGANGSKONTROLLQ1" to "26483",
                        "VALKEY_USERNAME_TILGANGSKONTROLLQ1" to "cache",
                        "VALKEY_PASSWORD_TILGANGSKONTROLLQ1" to "testhemmelighet",
                        "spring.application.name" to "test",
                    ),
                ),
            )
            kontekst.beanFactory.registerSingleton("meterRegistry", SimpleMeterRegistry())
            kontekst.register(CacheKonfigurasjon::class.java, BidragCacheConfiguration::class.java)
            kontekst.refresh()

            kontekst.getBean(CacheSettings::class.java).valkeyInstans shouldBe "tilgangskontrollq1"
            kontekst.getBean(LettuceConnectionFactory::class.java).hostName shouldBe "localhost"
            kontekst.getBean(CacheManager::class.java).getCache(Cache.PIP_SAK).shouldNotBeNull()
            kontekst.getBean(CachingConfigurer::class.java).keyGenerator() shouldBe
                kontekst.getBean(BrukerCacheKeyGenerator.BEAN_NAME, KeyGenerator::class.java)
        }
    }

    @Test
    fun `skal bruke appens utløpsregler og kode domenetyper til cache-nøkler`() {
        settings.cacher.keys shouldBe Cache.innstillinger.keys
        settings.cacher[Cache.PIP_SAK] shouldBe CacheExpiry(ttl = Duration.ofSeconds(10))
        settings.cacher.filterKeys { it != Cache.PIP_SAK }.values.toSet() shouldBe
            setOf(CacheExpiry(expireAt = LocalTime.of(6, 0)))
        val saksnummer = genererSaksnummer()
        settings.nøkkelprefix(Saksnummer(saksnummer)) shouldBe "saksnummer:$saksnummer"
        val fnr = genererFødselsnummer()
        settings.nøkkelprefix(Personident(fnr)) shouldBe "personident:$fnr"
        settings.nøkkelprefix(1) shouldBe null
    }

    @Test
    fun `skal lese appens DTO-er fra delt cache i en ny cache-instans`() {
        val skriver = cachebehandler().getCache(Cache.BRUKERGRUPPER).shouldNotBeNull()
        val leser = cachebehandler().getCache(Cache.BRUKERGRUPPER).shouldNotBeNull()
        val fnr = genererFødselsnummer()
        val cacheverdier =
            listOf(
                BrukerGrupperResponse(listOf(Gruppe("id", "beskrivelse", "navn"))),
                BrukerinformasjonResponse(
                    null,
                    listOf(Brukerinformasjon("Navn", "Fornavn", "Tittel", "epost", "avdeling", "Etternavn", "id", "Z999999")),
                ),
                BrukerEnheterRespons(listOf("0000")),
                TilgangsmaskinBulkResponse(
                    null,
                    listOf(TilgangsmaskinResultat("bruker", 200, TilgangsmaskinResultatDetaljer(null, null, 200, null, fnr, "Z999999", null, null, true))),
                ),
                ResponseEntity.status(HttpStatus.ACCEPTED).header("X-Cache-Test", "header").body(mapOf("harTilgang" to true)),
            )
        cacheverdier.forEachIndexed { index, verdi -> skriver.put("key-$index", verdi) }

        (leser.get("key-0").shouldNotBeNull().get() as BrukerGrupperResponse).value?.first()?.navn shouldBe "navn"
        (leser.get("key-1").shouldNotBeNull().get() as BrukerinformasjonResponse).value?.first()?.fulltNavn shouldBe "Navn"
        (leser.get("key-2").shouldNotBeNull().get() as BrukerEnheterRespons).enhetIder shouldBe listOf("0000")
        (leser.get("key-3").shouldNotBeNull().get() as TilgangsmaskinBulkResponse).resultater.first().detaljer?.brukerIdent shouldBe fnr
        val respons = leser.get("key-4").shouldNotBeNull().get() as ResponseEntity<*>
        respons.body shouldBe mapOf("harTilgang" to true)
        respons.statusCode shouldBe HttpStatus.ACCEPTED
        respons.headers.getFirst("X-Cache-Test") shouldBe "header"
    }

    @Test
    fun `skal skille saksbehandlere og applikasjonsbruker ved Cacheable-oppslag`() = mockkStatic(TokenUtils::class) {
        every { TokenUtils.erApplikasjonsbruker() } returns false
        every { TokenUtils.hentSaksbehandlerIdent() } returns "Z999999"
        val førstePod = proxy(TestKilde())
        val andreKilde = TestKilde()
        val andrePod = proxy(andreKilde)
        val person = genererPersonident()

        førstePod.hentPerson(person) shouldBe "${person.verdi}-1"
        every { TokenUtils.hentSaksbehandlerIdent() } returns "Z888888"
        førstePod.hentPerson(person) shouldBe "${person.verdi}-2"
        every { TokenUtils.erApplikasjonsbruker() } returns true
        førstePod.hentPerson(person) shouldBe "${person.verdi}-3"

        every { TokenUtils.erApplikasjonsbruker() } returns false
        every { TokenUtils.hentSaksbehandlerIdent() } returns "Z999999"
        andrePod.hentPerson(person) shouldBe "${person.verdi}-1"
        every { TokenUtils.hentSaksbehandlerIdent() } returns "Z888888"
        andrePod.hentPerson(person) shouldBe "${person.verdi}-2"
        every { TokenUtils.erApplikasjonsbruker() } returns true
        andrePod.hentPerson(person) shouldBe "${person.verdi}-3"

        andreKilde.antallKall shouldBe 0
        verdier.size shouldBe 3
        verdier.keys.none { it.contains(person.verdi) || it.contains("Z999999") || it.contains("Z888888") } shouldBe true
    }

    @Test
    fun `skal avvise Cacheable-oppslag uten saksbehandlerident`() = mockkStatic(TokenUtils::class) {
        every { TokenUtils.erApplikasjonsbruker() } returns false
        val kilde = TestKilde()
        val proxy = proxy(kilde)
        val person = genererPersonident()

        listOf(null, "", " ", "\t").forEach { ident ->
            every { TokenUtils.hentSaksbehandlerIdent() } returns ident
            shouldThrow<IllegalStateException> {
                proxy.hentPerson(person)
            }.message shouldBe "Mangler brukeridentitet for brukerbasert cache"
        }
        kilde.antallKall shouldBe 0
        verify(exactly = 0) { valkeySkriver.get(any(), any()) }
        verify(exactly = 0) { valkeySkriver.put(any(), any(), any(), any()) }
    }

    private fun cachebehandler() = TwoLevelCacheManager(settings, "test", SimpleMeterRegistry(), valkeySkriver, clock = klokke)

    private fun proxy(kilde: TestKilde): TestKilde {
        val interceptor = CacheInterceptor().apply {
            setCacheManager(cachebehandler())
            keyGenerator = BrukerCacheKeyGenerator("testhemmelighet", settings.brukerId, settings.nøkkelprefix)
            cacheOperationSource = AnnotationCacheOperationSource()
            afterPropertiesSet()
            afterSingletonsInstantiated()
        }
        return ProxyFactory(kilde).apply { addAdvice(interceptor) }.proxy as TestKilde
    }

    open class TestKilde {
        var antallKall = 0

        @Cacheable(Cache.TILGANGSMASKIN_KJERNEREGLER)
        open fun hentPerson(personIdent: Personident): String {
            antallKall++
            return "${personIdent.verdi}-$antallKall"
        }
    }
}

package no.nav.bidrag.tilgangskontroll.konfigurasjon

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.cache.konfigurasjon.CacheConfig
import no.nav.bidrag.cache.nøkkelgenerering.CacheNøkkel
import no.nav.bidrag.tilgangskontroll.model.graph.BrukerEnheterRespons
import no.nav.bidrag.tilgangskontroll.model.graph.BrukerGrupperResponse
import no.nav.bidrag.tilgangskontroll.model.graph.Brukerinformasjon
import no.nav.bidrag.tilgangskontroll.model.graph.BrukerinformasjonResponse
import no.nav.bidrag.tilgangskontroll.model.graph.Gruppe
import no.nav.bidrag.tilgangskontroll.model.tilgangsmaskin.TilgangsmaskinBulkResponse
import no.nav.bidrag.tilgangskontroll.model.tilgangsmaskin.TilgangsmaskinResultat
import no.nav.bidrag.tilgangskontroll.model.tilgangsmaskin.TilgangsmaskinResultatDetaljer
import org.junit.jupiter.api.Test
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter

/** Sjekker at returtypene til @LangCache-metodene kan leses tilbake fra Valkey, for eksempel på en annen pod. */
class ValkeySerialiseringTest {
    private val lagring = mutableMapOf<String, ByteArray>()
    private val skriver =
        mockk<RedisCacheWriter>(relaxed = true) {
            every { get(any(), any()) } answers { lagring[secondArg<ByteArray>().decodeToString()] }
            every { get(any(), any(), any()) } answers { lagring[secondArg<ByteArray>().decodeToString()] }
            every { put(any(), any(), any(), any()) } answers { lagring[secondArg<ByteArray>().decodeToString()] = thirdArg() }
        }

    private fun rundtur(verdi: Any?): Any? {
        val nøkkel = CacheNøkkel("Test.hent", "Z999999", listOf("id"))
        nyValkeyCache().put(nøkkel, verdi)
        return nyValkeyCache().get(nøkkel).shouldNotBeNull().get()
    }

    private fun nyValkeyCache() = RedisCacheManager
        .builder(skriver)
        .cacheDefaults(CacheConfig.Valkey().redisCacheConfig())
        .build()
        .getCache("test")
        .shouldNotBeNull()

    @Test
    fun `skal lese brukergrupper tilbake`() {
        val verdi = BrukerGrupperResponse(listOf(Gruppe("id", "beskrivelse", "0000-GA-ENHET_4806")), "neste")

        rundtur(verdi) shouldBe verdi
    }

    @Test
    fun `skal lese brukerinformasjon tilbake`() {
        val bruker = Brukerinformasjon("Navn", "Fornavn", "Tittel", "epost", "avdeling", "Etternavn", "id", "Z999999")

        val lest = rundtur(BrukerinformasjonResponse("neste", listOf(bruker))).shouldBeInstanceOf<BrukerinformasjonResponse>()

        lest.nextLink shouldBe "neste"
        lest.value shouldBe listOf(bruker)
    }

    @Test
    fun `skal lese brukerenheter tilbake`() {
        val verdi = BrukerEnheterRespons(listOf("4806", "4833"))

        rundtur(verdi) shouldBe verdi
    }

    @Test
    fun `skal lese bulksvar fra tilgangsmaskinen tilbake`() {
        val detaljer = TilgangsmaskinResultatDetaljer("type", "tittel", 403, "instans", "12345678901", "Z999999", "begrunnelse", "trace", true)
        val verdi = TilgangsmaskinBulkResponse("antatt", listOf(TilgangsmaskinResultat("12345678901", 403, detaljer)))

        rundtur(verdi) shouldBe verdi
    }

    @Test
    fun `skal lese enhetsid som String tilbake`() {
        rundtur("enhet-id").shouldBeInstanceOf<String>() shouldBe "enhet-id"
    }

    @Test
    fun `skal lese null tilbake som null`() {
        rundtur(null) shouldBe null
    }
}

package no.nav.bidrag.regnskap.consumer

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.sak.BidragssakDto
import no.nav.bidrag.transport.sak.RolleDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.ResponseEntity
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class SakConsumerTest {

    companion object {
        private const val SAK_PATH = "/bidrag-sak/sak"
        private const val DUMMY_NUMMER = "22222222226"
    }

    @MockK
    private lateinit var restTemplate: RestTemplate

    @InjectMockKs
    private lateinit var bidragSakConsumer: BidragSakConsumer

    private val sakUrl = "localhost:8080"

    @BeforeEach
    fun setup() {
        ReflectionTestUtils.setField(bidragSakConsumer, "sakUrl", sakUrl)
    }

    @Test
    fun `skal hente ut fødelsnummer fra bm`() {
        every { restTemplate.getForEntity("$sakUrl$SAK_PATH/123", BidragssakDto::class.java) } returns ResponseEntity.ok(
            opprettBidragSak(Rolletype.BIDRAGSMOTTAKER),
        )

        val fødelsnummer = bidragSakConsumer.hentBmFraSak("123")

        fødelsnummer shouldNotBe DUMMY_NUMMER
    }

    @Test
    fun `skal bruke dummynr om det ikke finnes en bm på sak`() {
        every { restTemplate.getForEntity("$sakUrl$SAK_PATH/123", BidragssakDto::class.java) } returns ResponseEntity.ok(
            opprettBidragSak(Rolletype.BIDRAGSPLIKTIG),
        )

        val fødelsnummer = bidragSakConsumer.hentBmFraSak("123")

        fødelsnummer shouldBe DUMMY_NUMMER
    }

    private fun opprettBidragSak(rolletype: Rolletype): BidragssakDto = BidragssakDto(
        Enhetsnummer("eierfogd"),
        Saksnummer("123"),
        Bidragssakstatus.NY,
        Sakskategori.NASJONAL,
        begrensetTilgang = false,
        opprettetDato = LocalDate.now(),
        levdeAdskilt = false,
        ukjentPart = false,
        roller = listOf(
            RolleDto(
                Personident(genererFødselsnummer()),
                rolletype,
            ),
        ),
    )
}

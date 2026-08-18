package no.nav.bidrag.organisasjon.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.commons.web.HttpResponse
import no.nav.bidrag.domene.enums.diverse.Enhetsstatus
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.organisasjon.BidragOrganisasjonTest
import no.nav.bidrag.organisasjon.TEST
import no.nav.bidrag.organisasjon.TestRestTemplateConfiguration
import no.nav.bidrag.organisasjon.WEB_ENV_TEST
import no.nav.bidrag.organisasjon.dto.SaksbehandlerDto
import no.nav.bidrag.organisasjon.service.OrganisasjonService
import no.nav.bidrag.transport.organisasjon.EnhetDto
import no.nav.bidrag.transport.organisasjon.JournalførendeEnhetDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(TEST, WEB_ENV_TEST)
@SpringBootTest(classes = [BidragOrganisasjonTest::class], webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(MockKExtension::class)
@EnableMockOAuth2Server
@AutoConfigureTestRestTemplate
internal class OrganisasjonControllerMockTest {
    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var testRestTemplateConfiguration: TestRestTemplateConfiguration

    @LocalServerPort
    private val port = 0

    @Value($$"${server.servlet.context-path}")
    private val contextPath: String? = null

    @MockkBean
    private lateinit var organisasjonServiceMock: OrganisasjonService

    @Test
    fun `skal bruke context path fra application yaml`() {
        contextPath shouldBe "/bidrag-organisasjon"
    }

    @Test
    fun `skal hente saksbehandler med navn`() {
        every { organisasjonServiceMock.hentSaksbehandlerInfo(SB_IDENT) } returns SaksbehandlerDto(SB_IDENT, SB_NAVN)
        val saksbehandlerResponse =
            testRestTemplate.exchange<SaksbehandlerDto>(
                "http://localhost:" + port + "/bidrag-organisasjon" + OrganisasjonController.ENDPOINT_SAKSBEHANDLERINFO + "/" + SB_IDENT,
                HttpMethod.GET,
                testRestTemplateConfiguration.initHttpEntity<Any>(),
            )
        assertSoftly {
            saksbehandlerResponse.statusCode shouldBe HttpStatus.OK
            val saksbehandler = saksbehandlerResponse.body
            saksbehandler?.ident shouldBe SB_IDENT
            saksbehandler?.navn shouldBe SB_NAVN
        }
    }

    @Test
    fun `skal hente liste over enheter en saksbehandler har tilgang til`() {
        val enhetsliste =
            listOf(
                EnhetDto(nummer = Enhetsnummer("100001596"), navn = "Bærum", status = Enhetsstatus.AKTIV),
                EnhetDto(nummer = Enhetsnummer("100001796"), navn = "Drammen", status = Enhetsstatus.AKTIV),
            )
        every { organisasjonServiceMock.hentSaksbehandlerEnheter(SB_IDENT) } returns enhetsliste
        val enhetslisteResponse =
            testRestTemplate.exchange<List<EnhetDto>>(
                "http://localhost:" + port + "/bidrag-organisasjon" + OrganisasjonController.ENDPOINT_SAKSBEHANDLERENHETER + "/" + SB_IDENT,
                HttpMethod.GET,
                testRestTemplateConfiguration.initHttpEntity<Any>(),
            )
        assertSoftly {
            enhetslisteResponse.statusCode shouldBe HttpStatus.OK
            enhetslisteResponse.body?.size shouldBe 2
            enhetslisteResponse.body?.get(0)?.nummer?.verdi shouldBe "100001596"
            enhetslisteResponse.body?.get(1)?.navn shouldBe "Drammen"
        }
    }

    @Test
    fun `skal hente liste over journalførende enheter fra arbeidsfordeling`() {
        val enhetsliste: MutableList<JournalførendeEnhetDto> = ArrayList()
        enhetsliste.add(JournalførendeEnhetDto(nummer = Enhetsnummer("100001596"), navn = "Bærum", type = "Klage"))
        enhetsliste.add(JournalførendeEnhetDto(nummer = Enhetsnummer("100001796"), navn = "Drammen", type = "Forvaltning"))
        every { organisasjonServiceMock.hentArbeidsfordelingJournalforendeEnheter() } returns HttpResponse.from(HttpStatus.OK, enhetsliste)
        val enhetslisteResponse =
            testRestTemplate.exchange<List<JournalførendeEnhetDto>>(
                "http://localhost:" + port + "/bidrag-organisasjon" + OrganisasjonController.ENDPOINT_ARBEIDSFORDELING_JF,
                HttpMethod.GET,
                testRestTemplateConfiguration.initHttpEntity<Any>(),
            )
        assertSoftly {
            enhetslisteResponse.statusCode shouldBe HttpStatus.OK
            enhetslisteResponse.body?.size shouldBe 2
            enhetslisteResponse.body?.get(0)?.nummer?.verdi shouldBe "100001596"
            enhetslisteResponse.body?.get(0)?.navn shouldBe "Bærum"
            enhetslisteResponse.body?.get(0)?.type shouldBe "Klage"
            enhetslisteResponse.body?.get(1)?.nummer?.verdi shouldBe "100001796"
            enhetslisteResponse.body?.get(1)?.navn shouldBe "Drammen"
            enhetslisteResponse.body?.get(1)?.type shouldBe "Forvaltning"
        }
    }

    @Test
    fun `skal hente liste over enheter fra arbeidsfordeling basert på geografisk tilknytning for ident`() {
        val enhetDto = EnhetDto(nummer = Enhetsnummer("100001596"), navn = "Bærum", status = Enhetsstatus.AKTIV)
        every {
            organisasjonServiceMock.hentArbeidsfordelingGeografiskTilknytningEnheter(
                eq(IDENT),
                any(),
                any(),
            )
        } returns enhetDto
        val enhetResponse =
            testRestTemplate.exchange<EnhetDto>(
                "http://localhost:" + port + "/bidrag-organisasjon" + OrganisasjonController.ENDPOINT_ARBEIDSFORDELING_GT + "/" + IDENT.verdi,
                HttpMethod.GET,
                testRestTemplateConfiguration.initHttpEntity<Any>(),
            )
        assertSoftly {
            enhetResponse.statusCode shouldBe HttpStatus.OK
            enhetResponse.body shouldNotBe null
            enhetResponse.body?.nummer?.verdi shouldBe "100001596"
            enhetResponse.body?.navn shouldBe "Bærum"
            verify {
                organisasjonServiceMock.hentArbeidsfordelingGeografiskTilknytningEnheter(
                    eq(IDENT),
                    null,
                    null,
                )
            }
        }
    }

    @Test
    fun `skal hente liste over enheter fra arbeidsfordeling basert på geografisk tilknytning for ident for tema BAR`() {
        val enhetDto = EnhetDto(nummer = Enhetsnummer("100001596"), navn = "Bærum", status = Enhetsstatus.AKTIV)
        every {
            organisasjonServiceMock.hentArbeidsfordelingGeografiskTilknytningEnheter(
                eq(IDENT),
                any(),
                any(),
            )
        } returns enhetDto
        val enhetResponse: ResponseEntity<EnhetDto> =
            testRestTemplate.exchange(
                "http://localhost:$port/bidrag-organisasjon${OrganisasjonController.ENDPOINT_ARBEIDSFORDELING_GT}/${IDENT.verdi}?tema=BAR",
                HttpMethod.GET,
                testRestTemplateConfiguration.initHttpEntity<Any>(),
                EnhetDto::class.java,
            )
        assertSoftly {
            enhetResponse.statusCode shouldBe HttpStatus.OK
            enhetResponse.body shouldNotBe null
            enhetResponse.body?.nummer?.verdi shouldBe "100001596"
            enhetResponse.body?.navn shouldBe "Bærum"
            verify {
                organisasjonServiceMock.hentArbeidsfordelingGeografiskTilknytningEnheter(
                    eq(IDENT),
                    eq("BAR"),
                    null,
                )
            }
        }
    }

    companion object {
        private val SB_IDENT = "X123456"
        private val SB_NAVN = "Sylfest Strutle"
        private val IDENT = Personident("12345678900")
    }
}

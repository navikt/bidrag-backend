package no.nav.bidrag.dokument.journalpost.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.bidrag.dokument.journalpost.AvvikshendelseBuilder
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles
import no.nav.bidrag.dokument.journalpost.TestDataManager
import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager
import no.nav.bidrag.dokument.journalpost.dto.Saksbehandler
import no.nav.bidrag.dokument.journalpost.entity.Journalpost
import no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostKafkaEventProducer
import no.nav.bidrag.dokument.journalpost.model.Journalstatus
import no.nav.bidrag.dokument.journalpost.repository.JournalHendelseRepository
import no.nav.bidrag.dokument.journalpost.service.TilgangskontrollService
import no.nav.bidrag.dokument.journalpost.service.TokenInformationService
import no.nav.bidrag.dokument.journalpost.utils.CustomHeader
import no.nav.bidrag.dokument.journalpost.utils.initHttpEntity
import no.nav.bidrag.dokument.journalpost.utils.prefixId
import no.nav.bidrag.transport.dokument.AvvikType
import no.nav.bidrag.transport.dokument.BehandleAvvikshendelseResponse
import no.nav.bidrag.transport.dokument.FARSKAP_UTELUKKET_PREFIKS
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.net.URI

@ActiveProfiles(BidragDokumentJournalpostProfiles.TEST, BidragDokumentJournalpostProfiles.SECURED_TEST)
@DisplayName("JournalpostController og avvik")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [BidragDokumentJournalpostLocalTest::class],
    properties = ["STS_URL=junit"],
)
@EnableWireMock(value = [ConfigureWireMock(port = 0)])
@EnableMockOAuth2Server
class JournalpostAvvikTestKt {

    companion object {
        private const val AVVIK_PA_JOURNALPOST = "/journal/%s/avvik"
        private const val AVVIK_PA_JP_MED_SAK = "/journal/%s/avvik?saksnummer=%s"
    }

    private fun listeMedAvvikTyper(): ParameterizedTypeReference<List<AvvikType>> = object : ParameterizedTypeReference<List<AvvikType>>() {}

    @Autowired
    private lateinit var testDataManager: TestDataManager

    @Autowired
    private lateinit var httpHeaderTestRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var journalHendelseRepository: JournalHendelseRepository

    @MockkBean
    private lateinit var tilgangskontrollServiceMock: TilgangskontrollService

    @MockkBean(relaxed = true)
    private lateinit var httpHeaderRestTemplateMock: HttpHeaderRestTemplate

    @MockkBean
    private lateinit var journalpostKafkaEventProducerMock: JournalpostKafkaEventProducer

    @MockkBean
    private lateinit var saksbehandlerOidcTokenManagerMock: SaksbehandlerOidcTokenManager

    @MockkBean
    private lateinit var tokenInformationServiceMock: TokenInformationService

    @BeforeEach
    fun resetMocks() {
        clearAllMocks()
    }

    @BeforeEach
    fun opprettKodeJournalstatusForVisning() {
        every {
            httpHeaderRestTemplateMock.exchange(
                ArgumentMatchers.any<URI>(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(Saksbehandler::class.java),
            )
        } returns ResponseEntity(HttpStatus.OK)
        testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.JOURNALFORT)
        every { tokenInformationServiceMock.hentSaksbehandlersBrukerid() } returns "Z9999"
    }

    @Test
    fun `skal hente avvik farskap utelukket`() {
        testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.EKSPEDERT)
        val journalpost =
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalpost()
                    .medBeskrivelse("Tittel på dokument")
                    .medJournalforendeEnhet("4860")
                    .medJournalstatus(Journalstatus.EKSPEDERT),
            )

        val response =
            httpHeaderTestRestTemplate.exchange(
                String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                HttpMethod.GET,
                null,
                listeMedAvvikTyper(),
            )

        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            response.body!! shouldContain AvvikType.FARSKAP_UTELUKKET
        }
    }

    @Test
    fun `skal ikke hente avvik farskap utelukket hvis allerede utelukket`() {
        testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.EKSPEDERT)
        val journalpost =
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalpost()
                    .medBeskrivelse("$FARSKAP_UTELUKKET_PREFIKS: Tittel på dokument")
                    .medJournalforendeEnhet("4860")
                    .medFagomrade("FAR")
                    .medJournalstatus(Journalstatus.EKSPEDERT),
            )

        val response =
            httpHeaderTestRestTemplate.exchange(
                String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                HttpMethod.GET,
                null,
                listeMedAvvikTyper(),
            )

        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            response.body!! shouldNotContain AvvikType.FARSKAP_UTELUKKET
        }
    }

    @Test
    fun `skal ikke hente avvik farskap utelukket hvis status er under produksjon`() {
        testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.EKSPEDERT)
        val journalpost =
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalpost()
                    .medBeskrivelse("Tittel på dokument")
                    .medJournalforendeEnhet("4860")
                    .medJournalstatus(Journalstatus.UNDER_PRODUKSJON),
            )

        val response =
            httpHeaderTestRestTemplate.exchange(
                String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                HttpMethod.GET,
                null,
                listeMedAvvikTyper(),
            )

        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            response.body!! shouldNotContain AvvikType.FARSKAP_UTELUKKET
        }
    }

    @Test
    fun `skal ikke hente avvik farskap utelukket hvis enhet ikke er farskap`() {
        testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.EKSPEDERT)
        val journalpost =
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalpost()
                    .medBeskrivelse("Tittel på dokument")
                    .medJournalforendeEnhet("4806")
                    .medJournalstatus(Journalstatus.EKSPEDERT),
            )

        val response =
            httpHeaderTestRestTemplate.exchange(
                String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                HttpMethod.GET,
                null,
                listeMedAvvikTyper(),
            )

        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            response.body!! shouldNotContain AvvikType.FARSKAP_UTELUKKET
        }
    }

    @Test
    fun `skal ikke utføre avvik farskap utelukket hvis allerede utelukket`() {
        testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.EKSPEDERT)
        val journalpost =
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalpost()
                    .medBeskrivelse("$FARSKAP_UTELUKKET_PREFIKS: Tittel på dokument")
                    .medJournalforendeEnhet("4860")
                    .medFagomrade("FAR")
                    .medJournalstatus(Journalstatus.EKSPEDERT),
            )

        val response =
            httpHeaderTestRestTemplate.exchange(
                String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                HttpMethod.POST,
                initHttpEntity(
                    AvvikshendelseBuilder
                        .enAvvikshendelse()
                        .med(AvvikType.FARSKAP_UTELUKKET)
                        .bygg(),
                    CustomHeader(EnhetFilter.X_ENHET_HEADER, "4860"),
                ),
                BehandleAvvikshendelseResponse::class.java,
            )
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `skal utføre avvik farskap utelukket`() {
        testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.EKSPEDERT)
        val journalpost =
            testDataManager.opprett(
                JournalpostBygger
                    .enJournalpost()
                    .medBeskrivelse("Tittel på dokument")
                    .medJournalforendeEnhet("4860")
                    .medFagomrade("BID")
                    .medJournalstatus(Journalstatus.EKSPEDERT),
            )

        val response =
            httpHeaderTestRestTemplate.exchange(
                String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                HttpMethod.POST,
                initHttpEntity(
                    AvvikshendelseBuilder
                        .enAvvikshendelse()
                        .med(AvvikType.FARSKAP_UTELUKKET)
                        .bygg(),
                    CustomHeader(EnhetFilter.X_ENHET_HEADER, "4860"),
                ),
                BehandleAvvikshendelseResponse::class.java,
            )

        val journalpostEtter = testDataManager.hent(journalpost.journalpostId, Journalpost::class.java).get()
        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            journalpostEtter.erFarskapUtelukket() shouldBe true
            journalpostEtter.fagomrade shouldBe "FAR"
            journalpostEtter.beskrivelse shouldBe "$FARSKAP_UTELUKKET_PREFIKS: Tittel på dokument"
        }

        val responseAvvik =
            httpHeaderTestRestTemplate.exchange(
                String.format(AVVIK_PA_JOURNALPOST, prefixId(journalpost)),
                HttpMethod.GET,
                null,
                listeMedAvvikTyper(),
            )

        assertSoftly {
            responseAvvik.statusCode shouldBe HttpStatus.OK
            responseAvvik.body!! shouldNotContain AvvikType.FARSKAP_UTELUKKET
        }
    }
}

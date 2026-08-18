package no.nav.bidrag.dokument.arkivering.controller

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.bidrag.dokument.arkivering.BidragDokumentArkivering
import no.nav.bidrag.dokument.arkivering.BidragDokumentArkiveringLocal
import no.nav.bidrag.dokument.arkivering.consumer.rest.Stubs
import no.nav.bidrag.dokument.arkivering.dto.ArkivereJournalpostResponse
import no.nav.bidrag.dokument.arkivering.dto.AvvikHendelseIntern.JournalStatusIntern
import no.nav.bidrag.dokument.arkivering.dto.JournalStatus
import no.nav.bidrag.dokument.arkivering.testutil.TestdataUtil.mockJournalpostResponse
import no.nav.bidrag.transport.dokument.JournalpostStatus
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.resttestclient.postForEntity
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock

@ExtendWith(MockitoExtension::class)
@DisplayName("ArkiveringController")
@ActiveProfiles(
    BidragDokumentArkivering.PROFILE_TEST,
    BidragDokumentArkivering.PROFILE_SECURED_TEST,
)
@SpringBootTest(
    classes = [BidragDokumentArkiveringLocal::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@EnableWireMock(ConfigureWireMock(port = 0))
@AutoConfigureTestRestTemplate
@EnableMockOAuth2Server
class ArkiveringControllerTest {
    @LocalServerPort
    private val localServerPort = 0

    @Autowired
    private lateinit var httpHeaderTestRestTemplate: HttpHeaderTestRestTemplate

    @Autowired
    private lateinit var stubs: Stubs

    @BeforeEach
    fun setupMocks() {
        stubs.runSecurityTokenServiceStub("")
    }

    @AfterEach
    fun cleanUp() {
        WireMock.reset()
    }

    @Test
    @DisplayName("Skal arkivere journalpost hvis dokument returneres fra midlertidig brevlager")
    fun skalArkivereJournalpostHvisDokumentReturneresFraBrevlager() {
        val journalpostResponse = mockJournalpostResponse()
        val jpIdBidrag = removeSourcePrefix(journalpostResponse.journalpost!!.journalpostId)
        val jpIdJoark = "467018752"
        val dokIdJoark = "485235560"
        val meldingJoark = "Journalposten dreier seg om noe"
        val journalstatus = JournalStatus.UGAAENDE_MED_DOKUMENTVARIANTER
        val saksnummer = journalpostResponse.sakstilknytninger[0]
        // Stubbe retur fra bidrag-dokument-journalpost
        stubs.runBidragDokumentHentJournalpostStub(journalpostResponse, HttpStatus.OK)
        stubs.runBidragSendAvvik("BID-$jpIdBidrag", HttpStatus.OK)
        stubs.runKanArkivereJournalpostStub(jpIdBidrag, HttpStatus.OK)
        stubs.runBidragDokumentEndreJournalpostStub(jpIdJoark, HttpStatus.OK)
        stubs.runHentDokumentStub(HttpStatus.OK)
        stubs.runSecurityTokenServiceStub("eyAklakj")
        // Stubbe retur fra Joark
        stubs.runArkiverJournalpostStub(
            HttpStatus.OK,
            jpIdJoark,
            journalstatus,
            meldingJoark,
            dokIdJoark,
            false,
        )

        // Kaller bidrag-dokument-arkivering-endepunkt med journalpost-id for å lagre journalpost i joark
        val bidragArkiveringRespons =
            httpHeaderTestRestTemplate.postForEntity<ArkivereJournalpostResponse>(
                initArkivereJournalpostUrl(jpIdBidrag),
            )
        val enhet = journalpostResponse.journalpost!!.journalforendeEnhet
        assertAll(
            {
                assertThat(bidragArkiveringRespons)
                    .extracting { it.statusCode }
                    .`as`("Statuskode")
                    .withFailMessage(
                        "Feil statuskode mottatt, forventet <%s>, fikk <%s>",
                        HttpStatus.OK,
                        bidragArkiveringRespons.statusCode,
                    ).isEqualTo(HttpStatus.OK)
            },
            {
                assertThat(bidragArkiveringRespons.body)
                    .extracting { it?.melding }
                    .`as`("Melding")
                    .withFailMessage(
                        "Feil melding mottatt, forventet <%s>, fikk <%s>",
                        meldingJoark,
                        bidragArkiveringRespons.body!!.melding,
                    ).isEqualTo(meldingJoark)
            },
            {
                assertThat(bidragArkiveringRespons.body)
                    .extracting { it?.jpIdJoark }
                    .`as`("Journalpostid Joark")
                    .withFailMessage(
                        "Feil journalpost-id mottatt, forventet <%s>, fikk <%s>",
                        jpIdJoark,
                        bidragArkiveringRespons.body!!.jpIdJoark,
                    ).isEqualTo(jpIdJoark)
            },
            {
                assertThat(bidragArkiveringRespons.body)
                    .extracting { it?.jpIdBidrag }
                    .`as`("Journalpostid Bidrag")
                    .withFailMessage(
                        "Feil journalpost-id mottatt, forventet <%s>, fikk <%s>",
                        journalpostResponse.journalpost!!.journalpostId,
                        bidragArkiveringRespons.body!!.jpIdBidrag,
                    ).isEqualTo(journalpostResponse.journalpost!!.journalpostId)
            },
            {
                assertThat(bidragArkiveringRespons.body)
                    .extracting { it?.journalpostFerdigstilt }
                    .`as`("Er journalposten ferdigstilt?")
                    .withFailMessage(
                        "Feil ferdigstiltstatus mottatt, forventet <%s>, fikk <%s>",
                        false,
                        bidragArkiveringRespons.body!!.journalpostFerdigstilt,
                    ).isEqualTo(false)
            },
            {
                assertThat(bidragArkiveringRespons.body)
                    .extracting { it!!.journalstatus }
                    .`as`("Journalstatus")
                    .withFailMessage(
                        "Feil journalstatus mottatt, forventet <%s>, fikk <%s>",
                        journalstatus,
                        bidragArkiveringRespons.body!!.journalstatus,
                    ).isEqualTo(journalstatus.kode)
            },
            {
                assertThat(bidragArkiveringRespons.body!!.dokumentInfo!![0].dokumentInfoId)
                    .`as`("Dokumentid Joark")
                    .withFailMessage(
                        "Feil Joark dokumentid mottatt, forventet <%s>, fikk <%s>",
                        dokIdJoark,
                        bidragArkiveringRespons.body!!.dokumentInfo!![0].dokumentInfoId,
                    ).isEqualTo(dokIdJoark)
            },
            {
                stubs.verify().avvikCalledWith(
                    "BID-$jpIdBidrag",
                    enhet,
                    JournalStatusIntern.STARTET.toString(),
                    saksnummer,
                )
            },
            {
                stubs.verify().avvikCalledWith(
                    "BID-$jpIdBidrag",
                    enhet,
                    JournalStatusIntern.FULLFORT.toString(),
                    jpIdJoark,
                    saksnummer,
                )
            },
            { stubs.verify().arkiverJournalpostCalledWith() },
        )
    }

    @Test
    @DisplayName("Skal ikke arkivere journalpost hvis dokument allerede er arkivert")
    fun skalIkkeArkivereJournalpostHvisDokumentAlleredeErArkivert() {
        val journalpostResponse = mockJournalpostResponse()
        val jpIdBidrag = removeSourcePrefix(journalpostResponse.journalpost!!.journalpostId)
        val jpIdJoark = "467018752"
        val dokIdJoark = "485235560"
        val meldingJoark = "Journalposten dreier seg om noe"
        val journalstatus = JournalStatus.UGAAENDE_MED_DOKUMENTVARIANTER
        val saksnummer = journalpostResponse.sakstilknytninger[0]
        stubs.runBidragDokumentHentJournalpostStub(journalpostResponse, HttpStatus.OK)
        stubs.runBidragSendAvvik("BID-$jpIdBidrag", HttpStatus.OK)
        stubs.runKanArkivereJournalpostStub(jpIdBidrag, HttpStatus.OK)
        stubs.runBidragDokumentEndreJournalpostStub(jpIdJoark, HttpStatus.OK)
        stubs.runSecurityTokenServiceStub("eyAklakj")
        stubs.runArkiverJournalpostStub(
            HttpStatus.CONFLICT,
            jpIdJoark,
            journalstatus,
            meldingJoark,
            dokIdJoark,
            false,
        )
        stubs.runHentDokumentStub(HttpStatus.OK)

        // Kaller bidrag-dokument-arkivering-endepunkt med journalpost-id for å lagre journalpost i joark
        httpHeaderTestRestTemplate.postForEntity<ArkivereJournalpostResponse>(
            initArkivereJournalpostUrl(jpIdBidrag),
        )
        val enhet = journalpostResponse.journalpost!!.journalforendeEnhet
        assertAll(
            {
                stubs.verify().avvikCalledWith(
                    "BID-$jpIdBidrag",
                    enhet,
                    JournalStatusIntern.STARTET.toString(),
                    saksnummer,
                )
            },
            {
                stubs.verify().avvikCalledWith(
                    "BID-$jpIdBidrag",
                    enhet,
                    JournalStatusIntern.FULLFORT.toString(),
                    jpIdJoark,
                    saksnummer,
                )
            },
            {
                stubs.verify().avvikNotCalledWith(
                    jpIdBidrag,
                    enhet,
                    JournalStatusIntern.FEILET.toString(),
                )
            },
            { stubs.verify().arkiverJournalpostCalledWith() },
        )
    }

    @Test
    @DisplayName("Skal ikke arkivere journalpost hvis journalpost har joark journalpostid")
    fun skalIkkeArkivereJournalpostHarJoarkJournalpostid() {
        val jpIdJoark = "467018752"
        val journalpostResponse = mockJournalpostResponse(jpIdJoark)
        val jpIdBidrag = removeSourcePrefix(journalpostResponse.journalpost!!.journalpostId)
        stubs.runBidragDokumentHentJournalpostStub(journalpostResponse, HttpStatus.OK)
        stubs.runSecurityTokenServiceStub("eyAklakj")
        stubs.runBidragSendAvvik("BID-$jpIdBidrag", HttpStatus.OK)

        // Kaller bidrag-dokument-arkivering-endepunkt med journalpost-id for å lagre journalpost i joark
        val response =
            httpHeaderTestRestTemplate.postForEntity<ArkivereJournalpostResponse>(
                initArkivereJournalpostUrl(jpIdBidrag),
            )
        assertAll(
            { assertThat(response.body!!.jpIdJoark).isEqualTo(jpIdJoark) },
            { assertThat(response.body!!.jpIdBidrag).isEqualTo(jpIdBidrag) },
            { stubs.verify().arkiverJournalpostNotCalled() },
        )
    }

    @Test
    @Disabled
    @DisplayName("Skal sende avvik feilet hvis tilknytt saker til journalpost feiler")
    fun skalSendeAvvikFeiletHvisTilknyttSakerTilJournalpostFeiler() {
        val journalpostResponse = mockJournalpostResponse()
        val jpIdBidrag = removeSourcePrefix(journalpostResponse.journalpost!!.journalpostId)
        val jpIdJoark = "467018752"
        val dokIdJoark = "485235560"
        val meldingJoark = "Journalposten dreier seg om noe"
        val journalstatus = JournalStatus.UGAAENDE_MED_DOKUMENTVARIANTER
        stubs.runHentDokumentStub(HttpStatus.OK)
        stubs.runBidragDokumentHentJournalpostStub(journalpostResponse, HttpStatus.OK)
        stubs.runBidragSendAvvik("BID-$jpIdBidrag", HttpStatus.OK)
        stubs.runKanArkivereJournalpostStub(jpIdBidrag, HttpStatus.OK)
        stubs.runBidragDokumentEndreJournalpostStub(jpIdJoark, HttpStatus.BAD_REQUEST)
        stubs.runSecurityTokenServiceStub("eyAklakj")
        stubs.runArkiverJournalpostStub(
            HttpStatus.OK,
            jpIdJoark,
            journalstatus,
            meldingJoark,
            dokIdJoark,
            false,
        )

        // Kaller bidrag-dokument-arkivering-endepunkt med journalpost-id for å lagre journalpost i joark
        val bidragArkiveringRespons =
            httpHeaderTestRestTemplate.postForEntity<ArkivereJournalpostResponse>(
                initArkivereJournalpostUrl(jpIdBidrag),
            )
        val enhet = journalpostResponse.journalpost!!.journalforendeEnhet
        val saker = journalpostResponse.sakstilknytninger
        assertAll(
            {
                assertThat(bidragArkiveringRespons.statusCode)
                    .isEqualTo(HttpStatus.BAD_REQUEST)
            },
            {
                stubs.verify().avvikCalledWith(
                    "BID-$jpIdBidrag",
                    enhet,
                    JournalStatusIntern.FEILET.toString(),
                    saker[0],
                )
            },
        )
    }

    @Test
    @DisplayName("Skal sende avvik feilet hvis opprett journalpost feiler")
    fun skalSendeAvvikFeiletHvisOpprettJournalpostFeiler() {
        val journalpostResponse = mockJournalpostResponse()
        val jpIdBidrag = removeSourcePrefix(journalpostResponse.journalpost!!.journalpostId)
        val jpIdJoark = "467018752"
        val dokIdJoark = "485235560"
        val meldingJoark = "Journalposten dreier seg om noe"
        val journalstatus = JournalStatus.UGAAENDE_MED_DOKUMENTVARIANTER
        val saksnummer = journalpostResponse.sakstilknytninger[0]
        // Stubbe retur fra bidrag-dokument-journalpost
        stubs.runBidragDokumentHentJournalpostStub(journalpostResponse, HttpStatus.OK)
        stubs.runBidragSendAvvik("BID-$jpIdBidrag", HttpStatus.OK)
        stubs.runKanArkivereJournalpostStub(jpIdBidrag, HttpStatus.OK)
        stubs.runHentDokumentStub(HttpStatus.OK)
        // Stubbe tjeneste for å hente id-token for servicebruker, benyttes i kall mot Joark
        stubs.runSecurityTokenServiceStub("eyAklakj")
        // Stubbe retur fra Joark
        stubs.runArkiverJournalpostStub(
            HttpStatus.BAD_REQUEST,
            jpIdJoark,
            journalstatus,
            meldingJoark,
            dokIdJoark,
            false,
        )

        // Kaller bidrag-dokument-arkivering-endepunkt med journalpost-id for å lagre journalpost i joark
        val bidragArkiveringRespons =
            httpHeaderTestRestTemplate.postForEntity<ArkivereJournalpostResponse>(
                initArkivereJournalpostUrl(jpIdBidrag),
            )
        val enhet = journalpostResponse.journalpost!!.journalforendeEnhet
        assertAll(
            {
                assertThat(bidragArkiveringRespons.statusCode)
                    .isEqualTo(HttpStatus.BAD_REQUEST)
            },
            {
                stubs.verify().avvikCalledWith(
                    "BID-$jpIdBidrag",
                    enhet,
                    JournalStatusIntern.STARTET.toString(),
                    saksnummer,
                )
            },
            {
                stubs.verify().avvikCalledWith(
                    "BID-$jpIdBidrag",
                    enhet,
                    JournalStatusIntern.FEILET.toString(),
                    saksnummer,
                )
            },
        )
    }

    @Test
    @DisplayName("Skal ikke arkivere journalpost dersom dokument mangler")
    fun skalIkkeArkivereJournalpostDersonDokumentMangler() {
        val journalpostResponse = mockJournalpostResponse()
        val jpIdBidrag = removeSourcePrefix(journalpostResponse.journalpost!!.journalpostId)
        val jpIdJoark = "467018752"
        val dokIdJoark = "485235560"
        val meldingJoark = "Journalposten dreier seg om noe"
        val journalstatus = JournalStatus.UGAAENDE_MED_DOKUMENTVARIANTER

        // Stubbe retur fra bidrag-dokument-journalpost
        stubs.runBidragDokumentHentJournalpostStub(journalpostResponse, HttpStatus.OK)
        // Mocker retur fra Midlertidig brevlager i stedet for å benytte stub pga problemer med bruk av Wiremock for stubbing av Soap-endepunkt
        stubs.runBidragSendAvvik("BID-$jpIdBidrag", HttpStatus.OK)
        stubs.runKanArkivereJournalpostStub(jpIdBidrag, HttpStatus.OK)
        stubs.runHentDokumentStub(HttpStatus.INTERNAL_SERVER_ERROR)

        // Stubbe tjeneste for å hente id-token for servicebruker, benyttes i kall mot Joark
        stubs.runSecurityTokenServiceStub("eyAklakj")
        // Stubbe retur fra Joark
        stubs.runArkiverJournalpostStub(
            HttpStatus.OK,
            jpIdJoark,
            journalstatus,
            meldingJoark,
            dokIdJoark,
            false,
        )
        val respons = // Kaller bidrag-dokument-arkivering-endepunkt med journalpost-id for å lagre journalpost i joark
            httpHeaderTestRestTemplate.postForEntity<ArkivereJournalpostResponse>(
                initArkivereJournalpostUrl(jpIdBidrag),
            )
        assertAll(
            { assertTrue(respons.statusCode.is5xxServerError) },
            {
                stubs
                    .verify()
                    .avvikCalledWith(
                        "BID-$jpIdBidrag",
                        journalpostResponse.journalpost!!.journalforendeEnhet,
                        JournalStatusIntern.FEILET.toString(),
                    )
            },
        )
    }

    @Test
    @DisplayName("Skal ikke arkivere journalpost dersom tittel inneholder spesialtegn")
    fun skalIkkeArkivereJournalpostDersomTittelInneholdSpesialTegn() {
        val journalpostResponse = mockJournalpostResponse(tittel = "\u001A" + "Test" + "\u001A")
        val jpIdBidrag = removeSourcePrefix(journalpostResponse.journalpost!!.journalpostId)

        // Stubbe retur fra bidrag-dokument-journalpost
        stubs.runBidragDokumentHentJournalpostStub(journalpostResponse, HttpStatus.OK)
        stubs.runKanArkivereJournalpostStub(jpIdBidrag, HttpStatus.OK)

        // Stubbe tjeneste for å hente id-token for servicebruker, benyttes i kall mot Joark
        stubs.runSecurityTokenServiceStub("eyAklakj")
        val respons = // Kaller bidrag-dokument-arkivering-endepunkt med journalpost-id for å lagre journalpost i joark
            httpHeaderTestRestTemplate.postForEntity<ArkivereJournalpostResponse>(
                initArkivereJournalpostUrl(jpIdBidrag),
            )
        assertAll(
            { assertTrue(respons.statusCode.is4xxClientError) },
        )
    }

    @Test
    @DisplayName(
        "Skal gi 400 BAD_REQUEST dersom journalpostId det spørres om inneholder noe annet enn bare tall",
    )
    fun skalGi400DersomJpIdInneholderNoeAnnetEnnBareTall() {
        val (journalpost) = mockJournalpostResponse()
        val jpIdBidrag = removeSourcePrefix(journalpost!!.journalpostId)

        // Stubbe forbidden-respons fra bidrag-dokument-journalpost
        stubs.runBidragDokumentHentJournalpostStub(
            journalpost.journalpostId!!,
            HttpStatus.FORBIDDEN,
        )

        // Kaller bidrag-dokument-arkivering-endepunkt med journalpost-id for å lagre journalpost i joark
        val bidragArkiveringRespons =
            httpHeaderTestRestTemplate
                .postForEntity<ArkivereJournalpostResponse>(
                    initArkivereJournalpostUrl(jpIdBidrag + "bokstaver"),
                )
        assertThat(bidragArkiveringRespons.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    @DisplayName("Skal gi 400 BAD_REQUEST dersom kan journalpost arkiveres sjekk feiler")
    fun skalGi400DersomKanArkivereKalletFeiler() {
        val journalpostResponse = mockJournalpostResponse()
        val jpIdBidrag = removeSourcePrefix(journalpostResponse.journalpost!!.journalpostId)
        val jpIdJoark = "467018752"
        val dokIdJoark = "485235560"
        val meldingJoark = "Journalposten dreier seg om noe"
        val journalstatus = JournalStatus.UGAAENDE_MED_DOKUMENTVARIANTER
        // Stubbe retur fra bidrag-dokument-journalpost
        stubs.runHentDokumentStub(HttpStatus.OK)
        stubs.runBidragDokumentHentJournalpostStub(journalpostResponse, HttpStatus.OK)
        stubs.runBidragSendAvvik("BID-$jpIdBidrag", HttpStatus.OK)
        stubs.runKanArkivereJournalpostStub(jpIdBidrag, HttpStatus.NOT_ACCEPTABLE)
        stubs.runBidragDokumentEndreJournalpostStub(jpIdJoark, HttpStatus.OK)
        // Stubbe tjeneste for å hente id-token for servicebruker, benyttes i kall mot Joark
        stubs.runSecurityTokenServiceStub("eyAklakj")
        // Stubbe retur fra Joark
        stubs.runArkiverJournalpostStub(
            HttpStatus.OK,
            jpIdJoark,
            journalstatus,
            meldingJoark,
            dokIdJoark,
            false,
        )

        // Kaller bidrag-dokument-arkivering-endepunkt med journalpost-id for å lagre journalpost i joark
        val respons =
            httpHeaderTestRestTemplate.postForEntity<ArkivereJournalpostResponse>(
                initArkivereJournalpostUrl(jpIdBidrag),
            )
        assertAll(
            { assertTrue(respons.statusCode == HttpStatus.BAD_REQUEST) },
            { stubs.verify().arkiverJournalpostNotCalled() },
        )
    }

    @Test
    @DisplayName("Skal gi httpstatus 401 UNAUTHORIZED hvis id-token mangler")
    fun skalGiStatus401HvisIDTokenMangler() {
        val testRestTemplate = TestRestTemplate()

        // Kaller bidrag-dokument-arkivering-endepunkt med journalpost-id for å lagre journalpost i joark
        val bidragArkiveringRespons =
            testRestTemplate.postForEntity<ArkivereJournalpostResponse>(
                initArkivereJournalpostUrl("1"),
            )
        assertThat(bidragArkiveringRespons.statusCode).isEqualTo(
            HttpStatus.UNAUTHORIZED,
        )
    }

    @Test
    @DisplayName("Skal gi httpstatus 403 FORBIDDEN hvis tilgang ikke gis til journalpost")
    fun skalGi403HvisManglendeTilgang() {
        val (journalpost) = mockJournalpostResponse()
        val jpIdBidrag = removeSourcePrefix(journalpost!!.journalpostId)
        stubs.runSecurityTokenServiceStub("")
        // Stubbe forbidden-respons fra bidrag-dokument-journalpost
        stubs.runBidragDokumentHentJournalpostStub(jpIdBidrag, HttpStatus.FORBIDDEN)

        // Kaller bidrag-dokument-arkivering-endepunkt med journalpost-id for å lagre journalpost i joark
        val bidragArkiveringRespons =
            httpHeaderTestRestTemplate.postForEntity<ArkivereJournalpostResponse>(
                initArkivereJournalpostUrl(jpIdBidrag),
            )
        assertThat(bidragArkiveringRespons.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    @DisplayName("Skal gi 404 NOT_FOUND dersom journalpost ikke finnes")
    fun skalGiNotFoundHvisJournalpostIkkeFinnes() {
        val (journalpost) = mockJournalpostResponse()
        val jpIdBidrag = removeSourcePrefix(journalpost!!.journalpostId)

        // Stubbe tom retur fra bidrag-dokument-journalpost
        stubs.runBidragDokumentHentJournalpostStub(jpIdBidrag, HttpStatus.NOT_FOUND)

        // Kaller bidrag-dokument-arkivering-endepunkt med journalpost-id for å lagre journalpost i joark
        val bidragArkiveringRespons =
            httpHeaderTestRestTemplate.postForEntity<ArkivereJournalpostResponse>(
                initArkivereJournalpostUrl(jpIdBidrag),
            )
        assertAll(
            { assertThat(bidragArkiveringRespons.statusCode.is4xxClientError) },
            { assertThat(bidragArkiveringRespons.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
        )
    }

    @Test
    @DisplayName("Skal gi 400 BAD_REQUEST dersom journalpost ikke har status klar til print")
    fun skalGiBadRequestHvisJournalpostIkkeHarStatusKlarTilPrint() {
        val journalpostResponse =
            mockJournalpostResponse(journalstatus = JournalpostStatus.MOTTAKSREGISTRERT)
        val jpIdBidrag = removeSourcePrefix(journalpostResponse.journalpost!!.journalpostId)

        // Stubbe retur fra bidrag-dokument-journalpost
        stubs.runBidragDokumentHentJournalpostStub(journalpostResponse, HttpStatus.OK)

        // Kaller bidrag-dokument-arkivering-endepunkt med journalpost-id for å lagre journalpost i joark
        val bidragArkiveringRespons =
            httpHeaderTestRestTemplate.postForEntity<ArkivereJournalpostResponse>(
                initArkivereJournalpostUrl(jpIdBidrag),
            )
        assertAll(
            { assertThat(bidragArkiveringRespons.statusCode.is4xxClientError) },
            { assertThat(bidragArkiveringRespons.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
        )
    }

    @Test
    @DisplayName(
        "Skal gi 500 Internal Server Error dersom henting av dokument fra midlertidig brevlager feiler",
    )
    fun skalGi500DersomHentingAvDokumentFeiler() {
        val journalpostResponse = mockJournalpostResponse()
        val jpIdBidrag = removeSourcePrefix(journalpostResponse.journalpost!!.journalpostId)

        // Stubbe retur fra bidrag-dokument-journalpost
        stubs.runHentDokumentStub(HttpStatus.INTERNAL_SERVER_ERROR)
        stubs.runBidragDokumentHentJournalpostStub(journalpostResponse, HttpStatus.OK)
        stubs.runBidragSendAvvik("BID-$jpIdBidrag", HttpStatus.OK)
        stubs.runKanArkivereJournalpostStub(jpIdBidrag, HttpStatus.OK)
        stubs.runSecurityTokenServiceStub("")
        stubs.runHentDokumentStub(HttpStatus.INTERNAL_SERVER_ERROR)

        // Kaller bidrag-dokument-arkivering-endepunkt med journalpost-id for å lagre journalpost i joark
        val bidragArkiveringRespons =
            httpHeaderTestRestTemplate.postForEntity<ArkivereJournalpostResponse>(
                initArkivereJournalpostUrl(jpIdBidrag),
            )
        assertThat(bidragArkiveringRespons.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    private fun initArkivereJournalpostUrl(journalpostId: String): String = "$baseUrlForStubs/api/v1/arkivere/journalpost/$journalpostId"

    private val baseUrlForStubs: String get() = "http://localhost:$localServerPort"

    private fun removeSourcePrefix(jpid: String?): String = jpid!!.replace("BID-", "")
}

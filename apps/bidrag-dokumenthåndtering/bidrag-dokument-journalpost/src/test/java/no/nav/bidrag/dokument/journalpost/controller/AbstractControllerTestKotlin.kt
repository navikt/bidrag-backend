package no.nav.bidrag.dokument.journalpost.controller

import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles
import no.nav.bidrag.dokument.journalpost.TestDataManager
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostHendelseListener
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository
import no.nav.bidrag.dokument.journalpost.repository.JournalsakReposistory
import no.nav.bidrag.dokument.journalpost.service.TilgangskontrollService
import no.nav.bidrag.dokument.journalpost.stubs.StubUtils
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock

@ActiveProfiles(BidragDokumentJournalpostProfiles.TEST, BidragDokumentJournalpostProfiles.SECURED_TEST)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [BidragDokumentJournalpostLocalTest::class],
    properties = ["STS_URL=junit"],
)
@EnableWireMock(value = [ConfigureWireMock(port = 0)])
@EnableMockOAuth2Server
abstract class AbstractControllerTestKotlin {
    @LocalServerPort
    protected var port = 0

    var stubUtils: StubUtils = StubUtils()

    @Autowired
    protected lateinit var journalsakReposistory: JournalsakReposistory

    @Autowired
    protected lateinit var journalpostRepository: JournalpostRepository

    @Autowired
    protected lateinit var testDataManager: TestDataManager

    @Autowired
    protected lateinit var httpHeaderTestRestTemplate: TestRestTemplate

    @MockitoBean
    protected lateinit var tilgangskontrollServiceMock: TilgangskontrollService

    @MockitoBean
    protected lateinit var journalpostHendelseListenerMock: JournalpostHendelseListener

    protected fun baseUrl(): String = "http://localhost:$port/bidrag-dokument-journalpost/"

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(
            tilgangskontrollServiceMock,
            journalpostHendelseListenerMock,
        )
        testDataManager.slettAlt()
    }
}

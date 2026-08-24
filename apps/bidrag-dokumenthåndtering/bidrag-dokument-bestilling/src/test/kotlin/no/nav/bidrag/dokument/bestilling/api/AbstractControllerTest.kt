package no.nav.bidrag.dokument.bestilling.api

import StubUtils
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import jakarta.jms.Queue
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.bidrag.dokument.bestilling.BidragDokumentBestillingLocalTest
import no.nav.bidrag.dokument.bestilling.JmsTestConfig
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMal
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.BrevBestilling
import no.nav.bidrag.dokument.bestilling.consumer.KodeverkConsumer
import no.nav.bidrag.dokument.bestilling.consumer.SjablonConsumer
import no.nav.bidrag.dokument.bestilling.consumer.dto.KodeverkResponse
import no.nav.bidrag.dokument.bestilling.consumer.dto.SjablongerDto
import no.nav.bidrag.dokument.bestilling.model.typeRef
import no.nav.bidrag.dokument.bestilling.persistence.bucket.GcpCloudStorage
import no.nav.bidrag.dokument.bestilling.utils.ANNEN_MOTTAKER
import no.nav.bidrag.dokument.bestilling.utils.BARN1
import no.nav.bidrag.dokument.bestilling.utils.BARN2
import no.nav.bidrag.dokument.bestilling.utils.BM1
import no.nav.bidrag.dokument.bestilling.utils.BP1
import no.nav.bidrag.dokument.bestilling.utils.JmsTestConsumer
import no.nav.bidrag.dokument.bestilling.utils.SAKSBEHANDLER_IDENT
import no.nav.bidrag.dokument.bestilling.utils.TEST_DOKUMENT
import no.nav.bidrag.dokument.bestilling.utils.createEnhetKontaktInformasjon
import no.nav.bidrag.dokument.bestilling.utils.createPostAdresseResponse
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.io.ClassPathResource
import org.springframework.jms.core.JmsTemplate
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock

@ActiveProfiles("test")
@SpringBootTest(
    classes = [BidragDokumentBestillingLocalTest::class, StubUtils::class, JmsTestConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@EnableWireMock(ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
@Disabled
abstract class AbstractControllerTest {
    @LocalServerPort
    private val port = 0

    @MockkBean
    lateinit var sjablonConsumer: SjablonConsumer

    @MockkBean
    lateinit var kodeverkConsumer: KodeverkConsumer

    @MockkBean
    lateinit var gcpCloudStorage: GcpCloudStorage

    @Autowired
    lateinit var stubUtils: StubUtils

    @Autowired
    lateinit var httpHeaderTestRestTemplate: TestRestTemplate

    @Autowired
    lateinit var onlineBrevQueue: Queue

    @Autowired
    lateinit var onlinebrevTemplate: JmsTemplate

    @Autowired
    lateinit var jmsTestConsumer: JmsTestConsumer

    @BeforeEach
    fun initMocks() {
        stubUtils.stubEnhetInfo()
        stubUtils.stubEnhetKontaktInfo()
        stubUtils.stubHentSaksbehandlerInfo()
        stubUtils.stubHentPersonSpraak()
        val kodeverkResponse =
            ObjectMapper()
                .findAndRegisterModules()
                .readValue(readFile("api/landkoder.json"), KodeverkResponse::class.java)
        val sjablonResponse =
            ObjectMapper()
                .findAndRegisterModules()
                .readValue(readFile("api/sjablon_all.json"), typeRef<SjablongerDto>())
        every { kodeverkConsumer.hentLandkoder() } returns kodeverkResponse
        every { sjablonConsumer.hentSjablonger() } returns sjablonResponse
        every { gcpCloudStorage.hentFil(any()) } returns TEST_DOKUMENT
    }

    @AfterEach
    fun resetMocks() {
        WireMock.reset()
        clearAllMocks()
        jmsTestConsumer.withOnlinebrev { purge() }
    }

    fun rootUri(): String = "http://localhost:$port"

    fun readFile(filePath: String): String = String(ClassPathResource("__files/$filePath").inputStream.readAllBytes())

    fun stubDefaultValues() {
        stubUtils.stubHentPerson(BP1.ident.verdi, BP1)
        stubUtils.stubHentPerson(BM1.ident.verdi, BM1)
        stubUtils.stubHentPerson(BARN1.ident.verdi, BARN1)
        stubUtils.stubHentPerson(BARN2.ident.verdi, BARN2)
        stubUtils.stubHentPerson(ANNEN_MOTTAKER.ident.verdi, ANNEN_MOTTAKER)
        stubUtils.stubHentSak()
        stubUtils.stubHentPersonSpraak()
        stubUtils.stubHentAdresse(postAdresse = createPostAdresseResponse())
        stubUtils.stubEnhetKontaktInfo(createEnhetKontaktInformasjon())
    }

    fun verifyBrevbestillingHeaders(
        bestilling: BrevBestilling,
        dokumentMalEnum: DokumentMal,
    ) {
        bestilling.passord shouldBe "pass"
        bestilling.sysid shouldBe "BI12"
        bestilling.arkiver shouldBe "JA"
        bestilling.format shouldBe "ENSIDIG"
        bestilling.skrivertype shouldBe "LOKAL"
        bestilling.skuff shouldBe ""
        bestilling.skriver shouldBe ""
        bestilling.saksbehandler shouldBe SAKSBEHANDLER_IDENT
        bestilling.malpakke shouldBe "BI01.${dokumentMalEnum.kode}"
    }
}

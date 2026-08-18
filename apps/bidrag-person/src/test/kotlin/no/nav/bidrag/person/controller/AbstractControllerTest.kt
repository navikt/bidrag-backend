package no.nav.bidrag.person.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario
import no.nav.bidrag.commons.service.KodeverkKoderBetydningerResponse
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.bidrag.generer.testdata.person.IdentType
import no.nav.bidrag.generer.testdata.person.IdentTyper
import no.nav.bidrag.generer.testdata.person.TestPersonBuilder
import no.nav.bidrag.generer.testdata.person.TestPersonIdent
import no.nav.bidrag.person.BidragPersonTest
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.io.IOException
import java.time.LocalDateTime

@ActiveProfiles(BidragPersonTest.LOCAL, BidragPersonTest.OIDC_TOKEN_TEST)
@SpringBootTest(classes = [BidragPersonTest::class], webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableWireMock(ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
class AbstractControllerTest {
    @LocalServerPort
    protected var port = 0

    @Autowired
    protected lateinit var httpHeaderTestRestTemplate: TestRestTemplate

    @BeforeEach
    @Throws(IOException::class)
    fun initStubs() {
        stubKodeverkPostnummerEndepunkt()
        WireMock.stubFor(
            WireMock.post(WireMock.urlMatching("/sts/token/([a-z]*)"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("sts/sts_token_response.json"),
                ),
        )
    }

    protected fun baseURL(): String = "http://localhost:$port/bidrag-person"

    private fun createGenericResponse() = WireMock.aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
        .withStatus(HttpStatus.OK.value())

    fun stubKodeverkPostnummerEndepunkt(response: KodeverkKoderBetydningerResponse? = null, status: HttpStatus = HttpStatus.OK) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*/kodeverk/Postnummer.*")).willReturn(
                if (response != null) {
                    createGenericResponse().withStatus(status.value()).withBody(
                        ObjectMapper().findAndRegisterModules().writeValueAsString(response),
                    )
                } else {
                    createGenericResponse()
                        .withBodyFile("kodeverk/kodeverk_postnummer.json")
                },
            ),
        )
    }

    protected fun stubPDLEndpoint(
        responseFile: String?,
        httpStatus: HttpStatus = HttpStatus.OK,
        scenariostate: String? = null,
        nextScenario: String? = null,
    ) {
        WireMock.stubFor(
            WireMock.post(WireMock.urlMatching("/pdl/graphql"))
                .inScenario("PDL response")
                .whenScenarioStateIs(scenariostate ?: Scenario.STARTED)
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(httpStatus.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseFile?.let { erstattVariablerITestFil(responseFile) }),
                ).willSetStateTo(nextScenario),
        )
    }

    fun erstattVariablerITestFil(filnavn: String): String {
        val fil =
            no.nav.bidrag.commons.web.mock
                .hentFil("/__files/$filnavn")
        var stringValue = fil.readText().replace("{ident}", PERSON_FNR)
        stringValue = stringValue.replace("{ident2}", PERSON_FNR_2)
        stringValue = stringValue.replace("{ident3}", PERSON_FNR_3)
        stringValue = stringValue.replace("{npid}", PERSON_NPID)
        stringValue = stringValue.replace("{aktørId}", PERSON_AKTORID)
        stringValue = stringValue.replace("{aktørId2}", PERSON_AKTORID_2)
        return stringValue
    }
    protected fun stubPDLEndpoint(responseFile: String?, scenariostate: String?, nextScenario: String?) {
        stubPDLEndpoint(responseFile, HttpStatus.OK, scenariostate, nextScenario)
    }

    companion object {
        val PERSON_NPID = TestPersonBuilder().identType(IdentTyper.NPID).opprett().personIdent!!
        val PERSON_FNR = TestPersonBuilder().identType(IdentTyper.FNR).opprett().personIdent!!
        val PERSON_FNR_2 = TestPersonBuilder().identType(IdentTyper.FNR).opprett().personIdent!!
        val PERSON_FNR_3 = TestPersonBuilder().identType(IdentTyper.FNR).opprett().personIdent!!
        val PERSON_AKTORID = TestPersonBuilder().identType(IdentTyper.BNR).opprett().personIdent!!
        val PERSON_AKTORID_2 = TestPersonBuilder().identType(IdentTyper.BNR).opprett().personIdent!!
    }
}

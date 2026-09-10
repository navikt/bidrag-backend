package no.nav.bidrag.henvendelse

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.ConsoleAppender
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.http.Fault
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import org.wiremock.spring.InjectWireMock
import java.util.UUID

/**
 * Starter hele appen med wiremock i rollen som sf-henvendelse-api-proxy og bidrag-person, og
 * mock-oauth2-server i rollen som Azure.
 *
 * Grunnen til at denne finnes: de andre testene bygger objektene selv og går derfor rundt
 * produksjonsoppsettet. Det skjulte to feil - at feilhåndteringen ikke fant fram til
 * 502-handleren, og at `X-Correlation-ID` ble sendt med to verdier fordi
 * `MdcValuesPropagatingClientInterceptor` i bidrag-commons legger på sin egen. Begge kunne
 * bare avdekkes gjennom den faktiske RestTemplaten og Spring sin exception-resolver.
 *
 * Wiremock-oppsettet her er samtidig mock-serveren man kan kjøre appen mot lokalt.
 */
@SpringBootTest(classes = [BidragHenvendelse::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnableMockOAuth2Server
@EnableWireMock(ConfigureWireMock(port = 0))
class HenvendelseIntegrasjonTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @InjectWireMock
    private lateinit var wireMockServer: WireMockServer

    /** Kaster ikke på feilstatus, slik at vi kan se på både status og body. */
    private val klient = RestTemplate().apply {
        errorHandler = object : DefaultResponseErrorHandler() {
            override fun hasError(statusCode: HttpStatusCode) = false
        }
    }

    @BeforeEach
    fun stubTilgangOgPerson() {
        wireMockServer.resetAll()
        stubTilgang(harTilgang = true)
        wireMockServer.stubFor(
            post(urlPathEqualTo("/person/personidenter")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""[ { "ident": "$AKTØRID", "historisk": false, "gruppe": "AKTORID" } ]"""),
            ),
        )
    }

    @Test
    fun `skal svare 403 ProblemDetail når saksbehandleren ikke har tilgang til personen`() {
        stubTilgang(harTilgang = false)

        val respons = hentHenvendelser(FNR)

        respons.statusCode shouldBe HttpStatus.FORBIDDEN
        respons.body!! shouldContain "Ingen tilgang"
        // Uten tilgang skal vi verken ha vekslet ident eller spurt kilden.
        wireMockServer.findAll(getRequestedFor(urlPathEqualTo(HENVENDELSESTI))) shouldHaveSize 0
        wireMockServer.findAll(postRequestedFor(urlPathEqualTo("/person/personidenter"))) shouldHaveSize 0
    }

    /**
     * Formen her er den kilden faktisk svarer med, slik `CRM_HenvendelseInfoListRestService` i
     * navikt/crm-henvendelse serialiserer den - konvolutt med alle fem feltene, og henvendelser
     * med feltene vi ikke leser.
     */
    @Test
    fun `skal hente henvendelser gjennom hele kjeden`() {
        stubHenvendelser(
            """
            {
              "data": [
                {
                  "henvendelseType": "MELDINGSKJEDE",
                  "fnr": "$FNR",
                  "aktorId": "$AKTØRID",
                  "kjedeId": "a0J3N000004dUBJUA2",
                  "gjeldendeTemagruppe": "FMLI",
                  "gjeldendeTema": "BID",
                  "opprettetDato": "2026-06-27T12:00:00.000Z",
                  "feilsendt": false,
                  "journalposter": [],
                  "markeringer": [],
                  "meldinger": [
                    { "sendtDato": "2026-06-27T12:00:00.000Z", "fritekst": "hei", "kanal": "DIGITAL" },
                    { "sendtDato": "2026-06-28T09:30:00.000Z", "fritekst": "hei igjen", "kanal": "DIGITAL" }
                  ]
                }
              ],
              "currentPage": 1,
              "pageSize": 100,
              "totalPages": 1,
              "hasNextPage": false
            }
            """.trimIndent(),
        )

        val respons = hentHenvendelser(FNR)

        respons.statusCode shouldBe HttpStatus.OK
        respons.body!! shouldContainJson "\"kjedeId\":\"a0J3N000004dUBJUA2\""
        respons.body!! shouldContainJson "\"henvendelsestype\":\"MELDINGSKJEDE\""
        respons.body!! shouldContainJson "\"sisteMeldingSendt\":\"2026-06-28T09:30:00Z\""
        // Feltene vi ikke leser skal ikke velte deserialiseringen, og ikke havne i svaret vårt
        respons.body!! shouldNotContain "fritekst"
        respons.body!! shouldNotContain "journalposter"
    }

    @Test
    fun `skal sende pageSize framfor å arve kildens default`() {
        stubHenvendelser("""{ "data": [], "currentPage": 1, "pageSize": 100, "totalPages": 0, "hasNextPage": false }""")

        hentHenvendelser(FNR).statusCode shouldBe HttpStatus.OK

        val forespørsel = wireMockServer.findAll(getRequestedFor(urlPathEqualTo(HENVENDELSESTI))).single()
        forespørsel.queryParameter("pageSize").firstValue() shouldBe "100"
    }

    @Test
    fun `skal takle at svaret er en ren liste`() {
        // Proxyens swagger dokumenterer denne formen. Vi tåler den selv om kilden svarer med
        // konvolutt i dag.
        stubHenvendelser("""[ { "henvendelseType": "CHAT", "kjedeId": "kjede-1", "meldinger": [] } ]""")

        val respons = hentHenvendelser(FNR)

        respons.statusCode shouldBe HttpStatus.OK
        respons.body!! shouldContainJson "\"kjedeId\":\"kjede-1\""
    }

    @Test
    fun `skal gi tom liste når personen ikke har henvendelser`() {
        // Kilden svarer 200 med tom data og totalPages 0, ikke 404.
        stubHenvendelser("""{ "data": [], "currentPage": 1, "pageSize": 100, "totalPages": 0, "hasNextPage": false }""")

        val respons = hentHenvendelser(FNR)

        respons.statusCode shouldBe HttpStatus.OK
        respons.body!! shouldContainJson "\"henvendelser\":[]"
    }

    @Test
    fun `skal sende presis én X-Correlation-ID, og den skal være en UUID`() {
        stubHenvendelser("[]")

        hentHenvendelser(FNR).statusCode shouldBe HttpStatus.OK

        val forespørsler = wireMockServer.findAll(getRequestedFor(urlPathEqualTo(HENVENDELSESTI)))
        forespørsler shouldHaveSize 1
        val verdier = forespørsler.single().headers.getHeader("X-Correlation-ID").values()
        verdier shouldHaveSize 1
        UUID.fromString(verdier.single())
    }

    @Test
    fun `skal gi 502 når henvendelsestjenesten bryter forbindelsen`() {
        wireMockServer.stubFor(
            get(urlPathEqualTo(HENVENDELSESTI)).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)),
        )

        val respons = hentHenvendelser(FNR)

        respons.statusCode shouldBe HttpStatus.BAD_GATEWAY
        respons.body!! shouldContainJson "\"status\":502"
        respons.body!!.utenLekkasje()
    }

    @Test
    fun `skal gi tom liste når kilden ikke kjenner aktøren`() {
        // Swaggeren dokumenterer 404 som "Could not find actor" - normalt utfall for en person
        // som aldri har kontaktet Nav, ikke en feil.
        wireMockServer.stubFor(
            get(urlPathEqualTo(HENVENDELSESTI)).willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{ "statusCode": 404, "message": "Could not find actor" }"""),
            ),
        )

        val respons = hentHenvendelser(FNR)

        respons.statusCode shouldBe HttpStatus.OK
        respons.body!! shouldContainJson "\"henvendelser\":[]"
    }

    @Test
    fun `skal gi 502 når henvendelsestjenesten svarer med feilstatus`() {
        wireMockServer.stubFor(
            get(urlPathEqualTo(HENVENDELSESTI)).willReturn(
                aResponse().withStatus(403).withBody("Machine token authorization not sufficient"),
            ),
        )

        val respons = hentHenvendelser(FNR)

        respons.statusCode shouldBe HttpStatus.BAD_GATEWAY
        respons.body!!.utenLekkasje()
    }

    @Test
    fun `skal gi 400 for ugyldig ident, uten å gjenta identen`() {
        val respons = hentHenvendelser("+1749011234")

        respons.statusCode shouldBe HttpStatus.BAD_REQUEST
        respons.body!! shouldNotContain "1749011234"
    }

    @Test
    fun `skal gi 400 for ugyldig request-body, uten å gjenta innholdet`() {
        val respons = kall("""{ "ident": 17490123474 }""", medToken = true)

        respons.statusCode shouldBe HttpStatus.BAD_REQUEST
        respons.body!! shouldNotContain "17490123474"
    }

    @Test
    fun `skal gi 401 uten token`() {
        val respons = kall("""{ "ident": "$FNR" }""", medToken = false)

        respons.statusCode shouldBe HttpStatus.UNAUTHORIZED
    }

    /**
     * Maskeringen i logback-spring.xml er det eneste som står mellom en aktørid og loggen: hver
     * URL appen kaller har `?aktorid=...`, og AbstractRestClient logger URL-en når kallet feiler.
     *
     * Testen sender loggposten gjennom den faktiske encoderen framfor å se på XML-en, fordi feilen
     * den skal fange er en tagg logback ikke kjenner - `<jsonGeneratorDecorator>` framfor
     * `<decorator>` - og ukjente tagger overses i stillhet. Oppsettet så riktig ut og
     * maskerte ingenting.
     */
    @Test
    fun `skal maskere aktørid i loggen`() {
        val appender = rotloggeren.getAppender("stdout_json") as ConsoleAppender<ILoggingEvent>

        val linje = String(appender.encoder.encode(loggpost("Kall mot /henvendelseliste?aktorid=$AKTØRID feilet")))

        linje shouldNotContain AKTØRID
        linje shouldContain "*".repeat(AKTØRID.length)
    }

    private fun stubTilgang(harTilgang: Boolean) {
        wireMockServer.stubFor(
            post(urlPathEqualTo("/tilgang/v2/api/tilgang/person")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{ "harTilgang": $harTilgang }"""),
            ),
        )
    }

    private fun stubHenvendelser(respons: String) {
        wireMockServer.stubFor(
            get(urlPathEqualTo(HENVENDELSESTI)).willReturn(
                aResponse().withHeader("Content-Type", "application/json").withBody(respons),
            ),
        )
    }

    private fun hentHenvendelser(ident: String) = kall("""{ "ident": "$ident" }""", medToken = true)

    private fun kall(
        body: String,
        medToken: Boolean,
    ) = klient.exchange(
        "http://localhost:$port/henvendelser",
        HttpMethod.POST,
        HttpEntity(
            body,
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                if (medToken) setBearerAuth(token())
            },
        ),
        String::class.java,
    )

    private val rotloggeren get() = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

    private fun loggpost(melding: String) = LoggingEvent(
        HenvendelseIntegrasjonTest::class.java.name,
        rotloggeren,
        Level.WARN,
        melding,
        null,
        null,
    )

    private fun token() = mockOAuth2Server
        .issueToken("aad", "Z999999", "test-client-id")
        .serialize()

    /** Sammenligner uten mellomrom, slik at testen ikke avhenger av formateringen av JSON-en. */
    private infix fun String.shouldContainJson(forventet: String) {
        this.replace(" ", "") shouldContain forventet
    }

    private fun String.utenLekkasje() {
        this shouldNotContain AKTØRID
        this shouldNotContain "aktorid"
        this shouldNotContain "localhost"
    }

    companion object {
        private const val FNR = "17490123474"
        private const val AKTØRID = "2000012345678"
        private const val HENVENDELSESTI = "/henvendelse/henvendelseinfo/henvendelseliste"
    }
}

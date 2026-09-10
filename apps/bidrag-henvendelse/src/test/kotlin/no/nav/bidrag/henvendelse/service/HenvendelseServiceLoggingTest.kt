package no.nav.bidrag.henvendelse.service

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.henvendelse.consumer.BidragPersonConsumer
import no.nav.bidrag.henvendelse.consumer.HenvendelseConsumer
import org.hamcrest.CoreMatchers.startsWith
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * Dekker loggingen som skal hindre at rare rader fra kilden fyller loggen: avvik logges
 * samlet én gang per kall, ikke én linje per henvendelse.
 */
class HenvendelseServiceLoggingTest {
    private val personident = Personident("17490123474")
    private val aktørid = "2000012345678"

    private val bidragPersonConsumer = mockk<BidragPersonConsumer>()
    private val restTemplate = RestTemplate()
    private val mockServer = MockRestServiceServer.bindTo(restTemplate).build()
    private val service = HenvendelseService(
        bidragPersonConsumer,
        HenvendelseConsumer(URI.create("http://sf-henvendelse"), restTemplate),
        mockk(relaxed = true),
    )

    private val logg = ListAppender<ILoggingEvent>()
    private val rotlogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

    @BeforeEach
    fun start() {
        every { bidragPersonConsumer.hentAktørid(personident) } returns aktørid
        logg.start()
        rotlogger.addAppender(logg)
    }

    @AfterEach
    fun stopp() {
        rotlogger.detachAppender(logg)
        logg.stop()
    }

    @Test
    fun `skal logge ukjente henvendelsestyper samlet, én linje uansett hvor mange rader`() {
        stub(
            """
            [
              { "henvendelseType": "HELT_NY_TYPE", "kjedeId": "kjede-1", "meldinger": [] },
              { "henvendelseType": "HELT_NY_TYPE", "kjedeId": "kjede-2", "meldinger": [] },
              { "henvendelseType": "EN_ANNEN_NY", "kjedeId": "kjede-3", "meldinger": [] },
              { "henvendelseType": "CHAT", "kjedeId": "kjede-4", "meldinger": [] }
            ]
            """.trimIndent(),
        )

        service.hentHenvendelser(personident).henvendelser shouldHaveSize 4

        val linjer = linjerSom { it.contains("Ukjente henvendelsestyper") }
        linjer shouldHaveSize 1
        linjer.single() shouldContain "HELT_NY_TYPE"
        linjer.single() shouldContain "EN_ANNEN_NY"
        linjer.single() shouldNotContain "CHAT"
    }

    @Test
    fun `skal logge manglende henvendelsestype som mangler-markør`() {
        stub("""[ { "kjedeId": "kjede-1", "meldinger": [] } ]""")

        service.hentHenvendelser(personident).henvendelser shouldHaveSize 1

        linjerSom { it.contains("Ukjente henvendelsestyper") }.single() shouldContain "<mangler>"
    }

    @Test
    fun `skal logge forkastede henvendelser samlet per kall, med andel`() {
        stub(
            """
            [
              { "henvendelseType": "CHAT", "meldinger": [] },
              { "henvendelseType": "CHAT", "meldinger": [] },
              { "henvendelseType": "CHAT", "kjedeId": "kjede-1", "meldinger": [] }
            ]
            """.trimIndent(),
        )

        service.hentHenvendelser(personident).henvendelser shouldHaveSize 1

        val linjer = linjerSom { it.contains("uten kjedeId") }
        linjer shouldHaveSize 1
        linjer.single() shouldContain "Hoppet over 2 av 3"
    }

    /**
     * Vi ber om pageSize=100 og henter bare første side. Advarselen er det eneste signalet vi får
     * om at 100 ikke holder for en person, og dermed grunnlaget for å ta stilling til paginering.
     */
    @Test
    fun `skal varsle når kilden har flere sider enn den vi henter`() {
        stub(
            """
            {
              "data": [ { "henvendelseType": "CHAT", "kjedeId": "kjede-1", "meldinger": [] } ],
              "currentPage": 1,
              "pageSize": 100,
              "totalPages": 3,
              "hasNextPage": true
            }
            """.trimIndent(),
        )

        service.hentHenvendelser(personident).henvendelser shouldHaveSize 1

        linjerSom { it.contains("flere sider") }.single() shouldContain "currentPage=1"
    }

    @Test
    fun `skal ikke varsle om sider når konvolutten sier at det ikke er flere`() {
        stub(
            """
            {
              "data": [ { "henvendelseType": "CHAT", "kjedeId": "kjede-1", "meldinger": [] } ],
              "currentPage": 1,
              "pageSize": 100,
              "totalPages": 1,
              "hasNextPage": false
            }
            """.trimIndent(),
        )

        service.hentHenvendelser(personident).henvendelser shouldHaveSize 1

        linjerFraTjenesten().shouldBeEmpty()
    }

    @Test
    fun `skal ikke logge noe når responsen er som forventet`() {
        stub("""[ { "henvendelseType": "CHAT", "kjedeId": "kjede-1", "meldinger": [] } ]""")

        service.hentHenvendelser(personident).henvendelser shouldHaveSize 1

        linjerFraTjenesten().shouldBeEmpty()
    }

    @Test
    fun `skal ikke logge aktørid eller fødselsnummer`() {
        stub("""[ { "henvendelseType": "HELT_NY_TYPE", "meldinger": [] } ]""")

        service.hentHenvendelser(personident)

        linjerFraTjenesten().forEach {
            it shouldNotContain aktørid
            it shouldNotContain personident.verdi
        }
    }

    private fun stub(respons: String) {
        mockServer
            .expect(requestTo(startsWith("http://sf-henvendelse/")))
            .andRespond(withSuccess(respons, MediaType.APPLICATION_JSON))
    }

    private fun linjerFraTjenesten() = logg.list
        .filter { it.loggerName.startsWith("no.nav.bidrag.henvendelse") }
        .map { it.formattedMessage }

    private fun linjerSom(predikat: (String) -> Boolean) = linjerFraTjenesten().filter(predikat)
}
